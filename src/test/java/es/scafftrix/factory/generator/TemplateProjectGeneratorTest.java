package es.scafftrix.factory.generator;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.GenerationProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cubre la consolidacion de la interfaz en un unico perfil de Spring Batch.
 *
 * <p>Los tres fallos que cubre:
 *
 * <ol>
 *   <li>La UI ofrezca dos opciones de batch. Con {@code SPRING_BATCH_PROCESS} en el enum,
 *       el desplegable iteraba tres valores. Ahora {@code ProjectType} solo declara los dos
 *       perfiles que la interfaz debe mostrar.</li>
 *   <li>Los dos perfiles compartieran raiz de plantilla. Si ambos resolvieran
 *       {@code project-template}, un proyecto batch saldria con codigo de API en lugar del
 *       proceso de lectura/escritura.</li>
 *   <li>El proyecto batch heredara un directorio {@code templates/} vacio de Thymeleaf, que
 *       no pertenece a un proceso sin capa web.</li>
 * </ol>
 */
class TemplateProjectGeneratorTest {

    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final TemplateProjectGenerator generator = new TemplateProjectGenerator(resolver);

    private static GenerationProfile profile(ProjectType type, String baseName) {
        return profile(type, baseName, "4.1.1");
    }

    private static GenerationProfile profile(ProjectType type, String baseName, String springBootVersion) {
        ConfigFormat configFormat = type == ProjectType.SPRING_MICROSERVICE
                ? ConfigFormat.YAML : ConfigFormat.PROPERTIES;
        return new GenerationProfile(
                type,
                baseName,
                baseName,
                "es.scafftrix",
                baseName,
                "es.scafftrix." + baseName,
                "1.0.0-SNAPSHOT",
                springBootVersion,
                configFormat,
                "");
    }

    private static GenerationProfile profile(ProjectType type, String baseName, ConfigFormat format) {
        return new GenerationProfile(
                type,
                baseName,
                baseName,
                "es.scafftrix",
                baseName,
                "es.scafftrix." + baseName,
                "1.0.0-SNAPSHOT",
                "4.1.1",
                format,
                "");
    }

    private GenerationProfile batchProfile(String baseName) {
        return profile(ProjectType.SPRING_BATCH, baseName);
    }

    private GenerationProfile monolithProfile(String baseName) {
        return profile(ProjectType.SPRING_BOOT, baseName);
    }

    private GenerationProfile microserviceProfile(String baseName) {
        return profile(ProjectType.SPRING_MICROSERVICE, baseName);
    }

    @Test
    void theInterfaceExposesExactlyThreeTypes() {
        assertEquals(3, ProjectType.values().length,
                "La interfaz debe ofrecer monolito, proceso batch y microservicio");
        assertEquals(List.of(ProjectType.SPRING_BOOT, ProjectType.SPRING_BATCH,
                        ProjectType.SPRING_MICROSERVICE),
                List.of(ProjectType.values()),
                "SPRING_BOOT, SPRING_BATCH y SPRING_MICROSERVICE, en ese orden");
    }

    @Test
    void springBootGeneratesTheMonolithContract(@TempDir Path output) {
        GenerationProfile profile = monolithProfile("monolitoCheck");

        generator.generate(profile, output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/contract/api.yaml")),
                "El monolito debe traer el contrato OpenAPI");
    }

    @Test
    void springBatchGeneratesTheJobAndNotTheWebLayer(@TempDir Path output) {
        GenerationProfile profile = batchProfile("batchCheck");

        generator.generate(profile, output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/schema-all.sql")),
                "El proceso batch necesita su esquema");
        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/sample-data.csv")),
                "El proceso batch necesita su entrada CSV");
        assertTrue(Files.isRegularFile(
                output.resolve("src/main/java/es/scafftrix/batchcheck/config/BatchConfig.java")),
                "El proceso batch necesita su configuracion de job");
        assertFalse(Files.exists(output.resolve("src/main/resources/contract")),
                "Un proceso batch no debe traer contrato OpenAPI");
    }

    @Test
    void springBatchDoesNotGetAnEmptyThymeleafDirectory(@TempDir Path output) {
        generator.generate(batchProfile("batchSinWeb"), output, step -> { });

        assertFalse(Files.exists(output.resolve("src/main/resources/templates")),
                "El proceso batch no tiene capa web y no debe heredar templates/");
    }

    @Test
    void springBootKeepsTheThymeleafDirectory(@TempDir Path output) {
        generator.generate(monolithProfile("monolitoConWeb"), output, step -> { });

        assertTrue(Files.isDirectory(output.resolve("src/main/resources/templates")),
                "El monolito si debe conservar el directorio de plantillas");
    }

    @Test
    void eachProfileShipsItsOwnTemplateTree() {
        assertTrue(resolver.getResource("classpath:project-template/pom.xml").exists(),
                "La raiz del monolito debe existir");
        assertTrue(resolver.getResource("classpath:project-template/src/main/resources/contract/api.yaml").exists(),
                "La plantilla del monolito debe traer el contrato OpenAPI");
        assertTrue(resolver.getResource("classpath:project-batch-template/pom.xml").exists(),
                "La raiz del proceso batch debe existir");
        
        assertFalse(resolver.getResource("classpath:project-batch-template/src/main/resources/contract/api.yaml").exists(),
                "La plantilla batch no debe traer contrato OpenAPI");
    }

    @Test
    void theProgressStepsDoNotClaimAMonolithForABatchJob(@TempDir Path output) {
        List<String> steps = new ArrayList<>();

        generator.generate(batchProfile("batchProgreso"), output, steps::add);

        assertTrue(steps.stream().anyMatch(s -> s.contains("proceso Spring Batch")),
                "El progreso debe describir el proceso batch: " + steps);
        assertFalse(steps.stream().anyMatch(s -> s.contains("monolito")),
                "El progreso no debe llamar 'monolito' a un proceso batch: " + steps);
    }

    @Test
    void springMicroserviceGeneratesItsContractAndContainerArtefacts(@TempDir Path output) {
        generator.generate(microserviceProfile("microCheck"), output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/contract/api.yaml")),
                "El microservicio es contract-first: necesita su contrato OpenAPI");
        assertTrue(Files.isRegularFile(output.resolve("Dockerfile")),
                "El microservicio debe poder empaquetarse en contenedor");
        assertTrue(Files.isRegularFile(output.resolve("docker-compose.yml")),
                "El microservicio debe traer su orquestacion local");
        assertTrue(Files.isRegularFile(
                        output.resolve("src/main/resources/db/migration/V1__create_user.sql")),
                "El microservicio debe arrancar desde un esquema gestionado por migraciones");
    }

    @Test
    void springMicroserviceShipsTheLayeredPackages(@TempDir Path output) {
        generator.generate(microserviceProfile("microLayers"), output, step -> { });

        String base = "src/main/java/es/scafftrix/microlayers/";
        assertTrue(Files.isRegularFile(output.resolve(base + "api/controller/UserController.java")),
                "El microservicio necesita la capa api");
        assertTrue(Files.isRegularFile(output.resolve(base + "application/service/UserService.java")),
                "El microservicio necesita la capa application");
        assertTrue(Files.isRegularFile(output.resolve(base + "domain/model/User.java")),
                "El microservicio necesita la capa domain");
        assertTrue(Files.isRegularFile(output.resolve(
                        base + "infrastructure/persistence/adapter/UserRepositoryAdapter.java")),
                "El microservicio necesita la capa infrastructure");
    }

    @Test
    void springMicroserviceDoesNotGetAnEmptyThymeleafDirectory(@TempDir Path output) {
        generator.generate(microserviceProfile("microSinWeb"), output, step -> { });

        assertFalse(Files.exists(output.resolve("src/main/resources/templates")),
                "El microservicio expone JSON, no vistas Thymeleaf");
    }

    @Test
    void pomCarriesTheChosenSpringBootVersion(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_BOOT, "versionCheck", "4.0.8"), output, step -> { });

        String pom = readString(output.resolve("pom.xml"));
        assertTrue(pom.contains("<version>4.0.8</version>"),
                "El pom debe usar la versión elegida, no una fija");
    }

    @Test
    void pomCarriesTheDescriptionTypedInTheForm(@TempDir Path output) {
        GenerationProfile profile = new GenerationProfile(ProjectType.SPRING_BATCH, "batchDesc", "batchDesc",
                "es.scafftrix", "batchDesc", "es.scafftrix.batchdesc", "1.0.0-SNAPSHOT", "4.1.1",
                ConfigFormat.PROPERTIES, "Gestión de clientes del centro");

        generator.generate(profile, output, step -> { });

        assertTrue(readString(output.resolve("pom.xml"))
                        .contains("<description>Gestión de clientes del centro</description>"),
                "La descripción escrita en el formulario debe llegar al pom.xml generado");
    }

    @Test
    void pomFallsBackToTheProfileDefaultDescription(@TempDir Path output) {
        generator.generate(batchProfile("batchSinDesc"), output, step -> { });

        String pom = readString(output.resolve("pom.xml"));
        assertTrue(pom.contains("<description>Proceso Spring Batch autónomo"),
                "Sin descripción, el pom debe llevar la del perfil");
        assertFalse(pom.contains("Plantilla base"),
                "El pom no debe arrastrar el texto de la plantilla");
    }

    @Test
    void preReleaseVersionsAddTheSpringRepositories(@TempDir Path snapshotDir, @TempDir Path milestoneDir) {
        generator.generate(profile(ProjectType.SPRING_BOOT, "snapshotCheck", "4.2.0-SNAPSHOT"), snapshotDir, step -> { });
        generator.generate(profile(ProjectType.SPRING_BOOT, "milestoneCheck", "4.2.0-M2"), milestoneDir, step -> { });

        String snapshotPom = readString(snapshotDir.resolve("pom.xml"));
        assertTrue(snapshotPom.contains("repo.spring.io/snapshot"),
                "Una SNAPSHOT debe declarar el repositorio de snapshots de Spring");
        assertTrue(snapshotPom.contains("repo.spring.io/milestone"),
                "Una SNAPSHOT también debe declarar el repositorio de milestones");

        String milestonePom = readString(milestoneDir.resolve("pom.xml"));
        assertTrue(milestonePom.contains("repo.spring.io/milestone"),
                "Un hito debe declarar el repositorio de milestones");
    }

    @Test
    void gaVersionsSkipTheSpringRepositories(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_BOOT, "gaCheck", "4.1.1"), output, step -> { });

        String pom = readString(output.resolve("pom.xml"));
        assertFalse(pom.contains("repo.spring.io"),
                "Una versión estable no debe arrastrar repositorios pre-release");
    }

    @Test
    void propertiesFormatKeepsOnlyPropertiesFiles(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_BOOT, "propCheck", ConfigFormat.PROPERTIES), output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/application.properties")),
                "El formato PROPERTIES debe conservar application.properties");
        assertFalse(Files.exists(output.resolve("src/main/resources/application.yml")),
                "El formato PROPERTIES debe descartar application.yml");
    }

    @Test
    void yamlFormatKeepsOnlyYamlFiles(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_BOOT, "yamlCheck", ConfigFormat.YAML), output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/application.yml")),
                "El formato YAML debe conservar application.yml");
        assertFalse(Files.exists(output.resolve("src/main/resources/application.properties")),
                "El formato YAML debe descartar application.properties");
    }

    @Test
    void yamlFormatKeepsTheProfileAndTestVariants(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_MICROSERVICE, "yamlMicro", ConfigFormat.YAML), output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/application.yml")),
                "El microservicio YAML conserva su configuración principal");
        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/application-local.yml")),
                "El microservicio YAML conserva el perfil local");
        assertTrue(Files.isRegularFile(output.resolve("src/test/resources/application-test.yml")),
                "El microservicio YAML conserva la configuración de tests");
        assertFalse(Files.exists(output.resolve("src/main/resources/application.properties")),
                "El microservicio YAML descarta las variantes .properties");
    }

    @Test
    void microservicePropertiesFormatWorksToo(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_MICROSERVICE, "propMicro", ConfigFormat.PROPERTIES), output, step -> { });

        assertTrue(Files.isRegularFile(output.resolve("src/main/resources/application.properties")),
                "El microservicio puede configurarse en .properties");
        assertFalse(Files.exists(output.resolve("src/main/resources/application.yml")),
                "La variante .properties descarta los ficheros YAML");
    }

    @Test
    void batchProfileHonoursTheChosenConfigFormat(@TempDir Path propertiesDir, @TempDir Path yamlDir) {
        generator.generate(profile(ProjectType.SPRING_BATCH, "batchProps", ConfigFormat.PROPERTIES),
                propertiesDir, step -> { });
        generator.generate(profile(ProjectType.SPRING_BATCH, "batchYaml", ConfigFormat.YAML),
                yamlDir, step -> { });

        assertTrue(Files.isRegularFile(propertiesDir.resolve("src/main/resources/application.properties")),
                "El batch en PROPERTIES conserva application.properties");
        assertFalse(Files.exists(propertiesDir.resolve("src/main/resources/application.yml")),
                "El batch en PROPERTIES descarta application.yml");

        assertTrue(Files.isRegularFile(yamlDir.resolve("src/main/resources/application.yml")),
                "El batch en YAML conserva application.yml");
        assertFalse(Files.exists(yamlDir.resolve("src/main/resources/application.properties")),
                "El batch en YAML descarta application.properties");
    }

    @Test
    void bootVersionTokenIsReplacedInEveryProfilePom(@TempDir Path output) {
        generator.generate(profile(ProjectType.SPRING_BATCH, "batchVersion", "4.0.8"), output, step -> { });
        generator.generate(profile(ProjectType.SPRING_MICROSERVICE, "microVersion", "4.0.8"),
                output.resolve("micro"), step -> { });

        assertTrue(readString(output.resolve("pom.xml")).contains("<version>4.0.8</version>"),
                "El pom batch debe usar la versión elegida");
        assertTrue(readString(output.resolve("micro/pom.xml")).contains("<version>4.0.8</version>"),
                "El pom del microservicio debe usar la versión elegida");
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new AssertionError("No se pudo leer " + path, e);
        }
    }
}