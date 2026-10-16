package es.scafftrix.factory.generator;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.GenerationProfile;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Produces the project's own seeds from the templates shipped in {@code resources}:
 * the minimal Spring Boot monolith ({@code project-template}), the Spring Batch process
 * ({@code project-batch-template}) and the contract-first microservice
 * ({@code project-microservice-template}).
 *
 * <p>Selected through {@link #appliesTo()} for every {@link ProjectType}, it needs no external
 * process: every file is copied from the classpath and its {@code @token@} placeholders are
 * replaced with the values already resolved by {@code NamingService} into the
 * {@link GenerationProfile}. Each type is described by a {@link TemplateDefinition} (template
 * root, human label, whether it carries a Thymeleaf directory and how to verify the result), so
 * the seeds share one implementation and adding a fourth type is a single entry in
 * {@link #definitionFor(ProjectType)} instead of a new pair of {@code if}s.
 *
 * <p>{@code @} is used as the delimiter, not {@code ${}}, because the templates are Maven
 * files that must keep Maven's own {@code ${...}} expressions untouched.
 */
@Component
public class TemplateProjectGenerator implements ProjectGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateProjectGenerator.class);

    private static final String TEMPLATE_ROOT = "project-template";

    private static final String BATCH_TEMPLATE_ROOT = "project-batch-template";

    private static final String MICROSERVICE_TEMPLATE_ROOT = "project-microservice-template";

    private static final String BATCH_SCHEMA = "src/main/resources/schema-all.sql";

    private static final String BATCH_INPUT = "src/main/resources/sample-data.csv";

    private static final String CONTRACT = "src/main/resources/contract/api.yaml";

    private static final String MICROSERVICE_MIGRATIONS = "src/main/resources/db/migration";

    /**
     * Created for the monolith only: no view is generated, but the directory must exist so
     * Thymeleaf is usable right away. The batch and microservice templates have no Thymeleaf
     * view layer and do not get it.
     */
    private static final String EMPTY_TEMPLATES_DIRECTORY = "src/main/resources/templates";

    private static final Pattern PLACEHOLDER = Pattern.compile("@(\\w+)@");

    private final ResourcePatternResolver resourceResolver;

    /**
     * Production constructor Spring must use. The other constructor exists only so tests can
     * inject an in-memory resolver; marking this one removes the ambiguity between them.
     */
    @Autowired
    public TemplateProjectGenerator() {
        this(new PathMatchingResourcePatternResolver());
    }

    public TemplateProjectGenerator(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public ProjectType[] appliesTo() {
        return new ProjectType[]{ProjectType.SPRING_BOOT, ProjectType.SPRING_BATCH,
                ProjectType.SPRING_MICROSERVICE};
    }

    @Override
    public String name() {
        return "Plantilla propia";
    }

    /**
     * Everything that varies between the seeded profiles lives here, so the generation flow
     * itself is type agnostic. Adding a new project type means adding one branch to
     * {@link #definitionFor(ProjectType)} and one verification method.
     */
    private TemplateDefinition definitionFor(ProjectType projectType) {
        return switch (projectType) {
            case SPRING_BOOT -> new TemplateDefinition(TEMPLATE_ROOT, "monolito Spring Boot mínimo",
                    true, this::verifyMonolithContract);
            case SPRING_BATCH -> new TemplateDefinition(BATCH_TEMPLATE_ROOT, "proceso Spring Batch",
                    false, this::verifyBatchProject);
            case SPRING_MICROSERVICE -> new TemplateDefinition(MICROSERVICE_TEMPLATE_ROOT,
                    "microservicio Spring Boot contract-first", false, this::verifyMicroserviceProject);
        };
    }

    @Override
    public GenerationResult generate(GenerationProfile profile, Path outputDirectory, ProgressListener progress) {
        Instant start = Instant.now();
        Map<String, String> tokens = tokens(profile);
        TemplateDefinition definition = definitionFor(profile.projectType());
        ConfigFormat configFormat = profile.configFormat() != null
                ? profile.configFormat()
                : profile.projectType().defaultConfigFormat();
        if (LOG.isInfoEnabled()) {
            LOG.info("Writing {} template. profile={} boot={} format={} output={}", definition.label(),
                    profile.mavenCoordinates(), profile.springBootVersion(), configFormat, outputDirectory);
        }

        progress.onStep("Escribiendo la plantilla del " + definition.label());
        int written = writeTemplates(definition.root(), outputDirectory, tokens);

        // Every template ships both configuration variants; keep only the selected one so
        // the generated project never carries two competing application files.
        pruneConfigFormatVariants(outputDirectory, configFormat);

        // Only the monolith template carries a Thymeleaf view layer; the batch and microservice
        // templates must not get a stray empty templates/ directory.
        if (definition.thymeleafDirectory()) {
            progress.onStep("Creando el directorio de plantillas Thymeleaf");
            createDirectory(outputDirectory.resolve(EMPTY_TEMPLATES_DIRECTORY));
        }

        progress.onStep("Verificando el proyecto generado");
        verifyGeneratedProject(outputDirectory, profile, definition);

        progress.onStep(written + " ficheros escritos");
        return GenerationResult.success(definition.label(),
                Duration.between(start, Instant.now()), outputDirectory);
    }

    private int writeTemplates(String templateRoot, Path outputDirectory, Map<String, String> tokens) {
        int written = 0;
        Resource[] resources;
        try {
            resources = resourceResolver.getResources("classpath*:" + templateRoot + "/**");
        } catch (IOException e) {
            throw new InvalidConfigurationException(
                    "No se ha podido leer la plantilla del proyecto.",
                    "Could not resolve the template resources under " + templateRoot, e);
        }
        for (Resource resource : resources) {
            String relativePath = relativePathOf(resource, templateRoot);
            if (relativePath.isEmpty()) {
                continue;
            }
            Path target = outputDirectory.resolve(render(relativePath, tokens));
            createDirectory(target.getParent());
            try {
                Files.writeString(target, render(read(resource), tokens), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new InvalidConfigurationException(
                        "No se ha podido escribir el proyecto generado.",
                        "Could not write template file " + relativePath + " to " + target, e);
            }
            written++;
        }
        if (written == 0) {
            throw new InvalidConfigurationException(
                    "No se ha podido generar el proyecto.",
                    "No template files were found under the classpath at " + templateRoot);
        }
        return written;
    }

    /** Tokens resolved from the profile; the project name entered in the UI is the artifact. */
    private Map<String, String> tokens(GenerationProfile profile) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("name", profile.applicationName());
        tokens.put("groupId", profile.groupId());
        tokens.put("artifactId", profile.artifactId());
        tokens.put("version", profile.version());
        tokens.put("packageName", profile.packageName());
        tokens.put("packagePath", profile.packageName().replace('.', '/'));
        tokens.put("applicationClass", applicationClassName(profile.baseName()));
        tokens.put("springBootVersion", bootVersion(profile));
        tokens.put("springRepositoriesBlock", springRepositoriesBlock(bootVersion(profile)));
        tokens.put("description", descriptionFor(profile));
        return Map.copyOf(tokens);
    }

    /**
     * Text placed in the {@code <description>} of the generated {@code pom.xml}. What the user
     * typed in the form wins; an empty description falls back to the profile's own default, so
     * the pom never advertises the template's placeholder text.
     */
    private String descriptionFor(GenerationProfile profile) {
        String typed = profile.description();
        if (typed != null && !typed.isBlank()) {
            return typed.trim();
        }
        return switch (profile.projectType()) {
            case SPRING_BOOT -> "Monolito Spring Boot mínimo con contrato OpenAPI y Thymeleaf";
            case SPRING_BATCH -> "Proceso Spring Batch autónomo (lector CSV, procesador y writer JDBC)";
            case SPRING_MICROSERVICE -> "Microservicio Spring Boot contract-first generado por Scafftrix";
        };
    }

    /**
     * Spring Boot version substituted into every pom.xml. The profile always carries it (it is
     * a project attribute); the fallback only protects direct generator calls.
     */
    private String bootVersion(GenerationProfile profile) {
        String version = profile.springBootVersion();
        return version == null || version.isBlank() ? "4.1.1" : version.trim();
    }

    /**
     * Adds the Spring snapshot and milestone repositories when the chosen Spring Boot version
     * is a pre-release. A GA version resolves from Maven Central and must not advertise
     * repositories the corporate build does not need.
     */
    private String springRepositoriesBlock(String springBootVersion) {
        if (!isPreRelease(springBootVersion)) {
            return "";
        }
        return """
                <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>https://repo.spring.io/milestone</url>
                        <snapshots>
                            <enabled>false</enabled>
                        </snapshots>
                    </repository>
                    <repository>
                        <id>spring-snapshots</id>
                        <name>Spring Snapshots</name>
                        <url>https://repo.spring.io/snapshot</url>
                        <releases>
                            <enabled>false</enabled>
                        </releases>
                    </repository>
                </repositories>""";
    }

    private boolean isPreRelease(String springBootVersion) {
        return springBootVersion != null
                && (springBootVersion.contains("SNAPSHOT")
                || startsMilestone(springBootVersion, "-M")
                || startsMilestone(springBootVersion, "-RC"));
    }

    /** True when {@code marker} appears followed by at least one digit, e.g. {@code -M2} or {@code -RC1}. */
    private static boolean startsMilestone(String version, String marker) {
        int from = 0;
        int index;
        while ((index = version.indexOf(marker, from)) >= 0) {
            int afterMarker = index + marker.length();
            if (afterMarker < version.length() && Character.isDigit(version.charAt(afterMarker))) {
                return true;
            }
            from = index + 1;
        }
        return false;
    }

    /**
     * Removes the configuration files that do not match the selected format. The templates
     * ship both {@code application.properties} and {@code application.yml} (and their profile
     * variants) so the choice is real; only the selected family survives in the output.
     */
    private void pruneConfigFormatVariants(Path outputDirectory, ConfigFormat configFormat) {
        ConfigFormat effective = configFormat == null ? ConfigFormat.PROPERTIES : configFormat;
        try (Stream<Path> paths = Files.walk(outputDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> isSpringConfigFile(path.getFileName().toString()))
                    .filter(path -> !effective.matches(path.getFileName().toString()))
                    .forEach(this::deleteTemplateFile);
        } catch (IOException e) {
            throw new InvalidConfigurationException(
                    "No se ha podido preparar el formato de configuración del proyecto generado.",
                    "Could not prune configuration variants under " + outputDirectory, e);
        }
    }

    private boolean isSpringConfigFile(String fileName) {
        return fileName.startsWith("application")
                && (fileName.endsWith(".properties") || fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
    }

    private void deleteTemplateFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new InvalidConfigurationException(
                    "No se ha podido preparar el formato de configuración del proyecto generado.",
                    "Could not remove unselected configuration file " + path, e);
        }
    }

    private String applicationClassName(String baseName) {
        return Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1) + "Application";
    }

    private String render(String content, Map<String, String> tokens) {
        Matcher matcher = PLACEHOLDER.matcher(content);
        StringBuilder rendered = new StringBuilder(content.length());
        while (matcher.find()) {
            String token = matcher.group(1);
            String value = tokens.get(token);
            if (value == null) {
                throw new InvalidConfigurationException(
                        "La plantilla del proyecto no es válida.",
                        "Unknown template token @" + token + "@ in the project templates");
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    /**
     * Path of a template resource relative to the template root, using {@code /} separators
     * so it works both from a directory and from inside the packaged jar.
     */
    private String relativePathOf(Resource resource, String templateRoot) {
        try {
            String uri = resource.getURI().toString();
            int root = uri.lastIndexOf(templateRoot + "/");
            if (root < 0) {
                return "";
            }
            String relative = uri.substring(root + templateRoot.length() + 1);
            return relative.endsWith("/") ? "" : relative;
        } catch (IOException e) {
            throw new InvalidConfigurationException(
                    "No se ha podido leer la plantilla del proyecto.",
                    "Could not resolve the location of a template resource", e);
        }
    }

    private String read(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidConfigurationException(
                    "No se ha podido leer la plantilla del proyecto.",
                    "Could not read template resource " + resource.getDescription(), e);
        }
    }

    private void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new InvalidConfigurationException(
                    "No se ha podido crear el directorio del proyecto generado.",
                    "Could not create directory " + directory, e);
        }
    }

    /**
     * Fails the generation when the expected Maven artefacts are absent, so an incomplete
     * template can never be handed to the user as a successful download.
     */
    private void verifyGeneratedProject(Path outputDirectory, GenerationProfile profile,
                                        TemplateDefinition definition) {
        if (!Files.isRegularFile(outputDirectory.resolve("pom.xml"))) {
            throw new InvalidConfigurationException(
                    "El generador no ha producido un proyecto Maven válido.",
                    "Expected pom.xml in " + outputDirectory + " for " + profile.mavenCoordinates());
        }
        Path expectedPackageDir = outputDirectory.resolve(
                "src/main/java/" + profile.packageName().replace('.', '/'));
        if (!Files.isDirectory(expectedPackageDir)) {
            throw new InvalidConfigurationException(
                    "El proyecto generado no contiene el paquete esperado.",
                    "Expected package directory " + expectedPackageDir + " for " + profile.packageName());
        }
        definition.verification().verify(outputDirectory);
        if (LOG.isInfoEnabled()) {
            LOG.info("{} template written for {} into {}", definition.label(),
                    profile.mavenCoordinates(), outputDirectory);
        }
    }

    /** A batch process has no OpenAPI contract; it must instead be able to read and write rows. */
    private void verifyBatchProject(Path outputDirectory) {
        if (!Files.isRegularFile(outputDirectory.resolve(BATCH_SCHEMA))) {
            throw new InvalidConfigurationException(
                    "El generador no ha producido un proyecto Spring Batch válido.",
                    "Expected " + BATCH_SCHEMA + " in " + outputDirectory);
        }
        if (!Files.isRegularFile(outputDirectory.resolve(BATCH_INPUT))) {
            throw new InvalidConfigurationException(
                    "El proyecto generado no contiene el fichero de entrada del proceso.",
                    "Expected " + BATCH_INPUT + " in " + outputDirectory);
        }
    }

    private void verifyMonolithContract(Path outputDirectory) {
        if (!Files.isRegularFile(outputDirectory.resolve(CONTRACT))) {
            throw new InvalidConfigurationException(
                    "El proyecto generado no contiene el contrato OpenAPI.",
                    "Expected " + CONTRACT + " in " + outputDirectory);
        }
    }

    /**
     * A microservice is only real when the contract, the container artefacts and the database
     * migrations are present: without them the project would compile but could not be run or
     * deployed, which is worse than a failed generation.
     */
    private void verifyMicroserviceProject(Path outputDirectory) {
        verifyMonolithContract(outputDirectory);
        if (!Files.isRegularFile(outputDirectory.resolve("Dockerfile"))) {
            throw new InvalidConfigurationException(
                    "El microservicio generado no contiene el Dockerfile.",
                    "Expected Dockerfile in " + outputDirectory);
        }
        if (!Files.isRegularFile(outputDirectory.resolve("docker-compose.yml"))) {
            throw new InvalidConfigurationException(
                    "El microservicio generado no contiene docker-compose.yml.",
                    "Expected docker-compose.yml in " + outputDirectory);
        }
        Path migrations = outputDirectory.resolve(MICROSERVICE_MIGRATIONS);
        if (!Files.isDirectory(migrations)) {
            throw new InvalidConfigurationException(
                    "El microservicio generado no contiene migraciones de base de datos.",
                    "Expected " + MICROSERVICE_MIGRATIONS + " in " + outputDirectory);
        }
    }

    /** Immutable description of one seeded profile. */
    private record TemplateDefinition(String root, String label, boolean thymeleafDirectory,
                                      Verification verification) {
    }

    /** Post-write check that the produced project is complete for its type. */
    @FunctionalInterface
    private interface Verification {
        void verify(Path outputDirectory);
    }
}
