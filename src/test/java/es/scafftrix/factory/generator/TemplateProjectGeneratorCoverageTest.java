package es.scafftrix.factory.generator;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.GenerationProfile;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateProjectGeneratorCoverageTest {

    private static final class FakeResource extends AbstractResource {
        private final String uri;
        private final String content;
        private final boolean brokenStream;
        private final boolean brokenUri;

        private FakeResource(String uri, String content, boolean brokenStream, boolean brokenUri) {
            this.uri = uri;
            this.content = content;
            this.brokenStream = brokenStream;
            this.brokenUri = brokenUri;
        }

        @Override
        public String getDescription() {
            return uri;
        }

        @Override
        public URI getURI() throws IOException {
            if (brokenUri) {
                throw new IOException("boom");
            }
            return URI.create(uri);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (brokenStream) {
                throw new FileNotFoundException("boom");
            }
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class FakeResolver implements ResourcePatternResolver {
        private final Resource[] resources;

        private FakeResolver(Resource... resources) {
            this.resources = resources;
        }

        @Override
        public Resource[] getResources(String locationPattern) {
            return resources;
        }

        @Override
        public Resource getResource(String location) {
            return resources.length == 0 ? null : resources[0];
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }

    private static Resource resource(String uri, String content) {
        return new FakeResource(uri, content, false, false);
    }

    private static Resource unreadable(String uri) {
        return new FakeResource(uri, "", true, false);
    }

    private static Resource badLocation(String uri) {
        return new FakeResource(uri, "", false, true);
    }

    private static GenerationProfile profile(ProjectType type, ConfigFormat format, String bootVersion) {
        return new GenerationProfile(type, "coverage", "coverage", "es.scafftrix", "coverage",
                "es.scafftrix.coverage", "1.0.0-SNAPSHOT", bootVersion, format, "");
    }

    private final GenerationProfile bootProfile = profile(ProjectType.SPRING_BOOT, null, null);

    private final GenerationProfile batchProfile = profile(ProjectType.SPRING_BATCH, null, null);

    private final GenerationProfile microProfile = profile(ProjectType.SPRING_MICROSERVICE, null, null);

    private static Resource[] monolithResources() {
        return new Resource[]{
                resource("file:/tmp/project-template/pom.xml", "<version>@version@</version>"),
                resource("file:/tmp/project-template/src/main/java/es/scafftrix/coverage/App.java", "class App {}"),
                resource("file:/tmp/project-template/src/main/resources/contract/api.yaml", "openapi: 3.0.0")
        };
    }

    @Test
    void generatesAFullMonolithFromInjectedResources(@TempDir Path output) throws IOException {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(monolithResources()));

        generator.generate(bootProfile, output, step -> { });

        assertThat(output.resolve("pom.xml")).exists();
        assertThat(output.resolve("src/main/resources/templates")).isDirectory();
        assertThat(Files.readString(output.resolve("pom.xml"))).contains("<version>1.0.0-SNAPSHOT</version>");
    }

    @Test
    void fallsBackToTheDefaultBootVersionAndConfigFormat(@TempDir Path output) throws IOException {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-template/pom.xml", "@springBootVersion@ @springRepositoriesBlock@"),
                resource("file:/tmp/project-template/src/main/java/es/scafftrix/coverage/App.java", "class App {}"),
                resource("file:/tmp/project-template/src/main/resources/contract/api.yaml", "openapi: 3.0.0")));

        generator.generate(profile(ProjectType.SPRING_BOOT, null, "  "), output, step -> { });

        assertThat(Files.readString(output.resolve("pom.xml")))
                .startsWith("4.1.1")
                .doesNotContain("repo.spring.io");
    }

    @Test
    void nonPreReleaseMarkerWithoutDigitIsNotMilestone(@TempDir Path output) throws IOException {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-template/pom.xml", "<version>@springBootVersion@</version>@springRepositoriesBlock@"),
                resource("file:/tmp/project-template/src/main/java/es/scafftrix/coverage/App.java", "class App {}"),
                resource("file:/tmp/project-template/src/main/resources/contract/api.yaml", "openapi: 3.0.0")));

        generator.generate(profile(ProjectType.SPRING_BOOT, ConfigFormat.PROPERTIES, "4.2.0-M"), output, step -> { });

        assertThat(Files.readString(output.resolve("pom.xml"))).doesNotContain("repo.spring.io");
    }

    @Test
    void failsWhenNoTemplateFilesAreFound(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver());

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("No template files were found");
    }

    @Test
    void skipsResourcesOutsideTheTemplateRoot(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/other/pom.xml", "<version>@version@</version>")));

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void skipsDirectoryLikeResources(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-template/subdir/", "x")));

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void failsWhenTheTemplateLocationCannotBeResolved(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                badLocation("file:/tmp/project-template/pom.xml")));

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("Could not resolve the location");
    }

    @Test
    void failsWhenATemplateCannotBeRead(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                unreadable("file:/tmp/project-template/pom.xml")));

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("Could not read template resource");
    }

    @Test
    void failsOnAnUnknownTemplateToken(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-template/pom.xml", "@unknown@")));

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("Unknown template token");
    }

    @Test
    void failsWhenTheOutputDirectoryCannotBeCreated(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("not-a-directory");
        Files.writeString(file, "x");
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(monolithResources()));

        assertThatThrownBy(() -> generator.generate(bootProfile, file, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("Could not create directory");
    }

    @Test
    void failsWhenTheMonolithHasNoContract(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-template/pom.xml", "@version@"),
                resource("file:/tmp/project-template/src/main/java/es/scafftrix/coverage/App.java", "class App {}")));

        assertThatThrownBy(() -> generator.generate(bootProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("src/main/resources/contract/api.yaml");
    }

    @Test
    void failsWhenTheBatchHasNoSchema(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-batch-template/pom.xml", "@version@"),
                resource("file:/tmp/project-batch-template/src/main/java/es/scafftrix/coverage/App.java", "class App {}")));

        assertThatThrownBy(() -> generator.generate(batchProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("src/main/resources/schema-all.sql");
    }

    @Test
    void failsWhenTheMicroserviceHasNoMigrations(@TempDir Path output) {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver(
                resource("file:/tmp/project-microservice-template/pom.xml", "@version@"),
                resource("file:/tmp/project-microservice-template/src/main/java/es/scafftrix/coverage/App.java", "class App {}"),
                resource("file:/tmp/project-microservice-template/src/main/resources/contract/api.yaml", "openapi: 3.0.0"),
                resource("file:/tmp/project-microservice-template/Dockerfile", "FROM scratch"),
                resource("file:/tmp/project-microservice-template/docker-compose.yml", "services: {}")));

        assertThatThrownBy(() -> generator.generate(microProfile, output, step -> { }))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("src/main/resources/db/migration");
    }

    @Test
    void exposesItsIdentityAndAcceptedTypes() {
        TemplateProjectGenerator generator = new TemplateProjectGenerator(new FakeResolver());

        assertThat(generator.name()).isEqualTo("Plantilla propia");
        assertThat(generator.appliesTo()).containsExactly(
                ProjectType.SPRING_BOOT, ProjectType.SPRING_BATCH, ProjectType.SPRING_MICROSERVICE);
    }
}
