package es.scafftrix.factory.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScafftrixPropertiesTest {

    @Test
    void workspaceDefaultsAndOverrides() {
        ScafftrixProperties properties = new ScafftrixProperties();

        assertThat(properties.getWorkspace().getPath()).isEqualTo("./workspace");
        assertThat(properties.getWorkspace().getProjectFolderName()).isEqualTo("project");

        properties.getWorkspace().setPath("/tmp/factory");
        properties.getWorkspace().setProjectFolderName("pf");

        assertThat(properties.getWorkspace().getPath()).isEqualTo("/tmp/factory");
        assertThat(properties.getWorkspace().getProjectFolderName()).isEqualTo("pf");
    }

    @Test
    void defaultsDefaultsAndOverrides() {
        ScafftrixProperties properties = new ScafftrixProperties();

        assertThat(properties.getDefaults().getGroupId()).isEqualTo("es.scafftrix");
        assertThat(properties.getDefaults().getVersion()).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(properties.getDefaults().getSpringBootVersion()).isEqualTo("4.1.1");

        properties.getDefaults().setGroupId("com.acme");
        properties.getDefaults().setVersion("2.0.0");
        properties.getDefaults().setSpringBootVersion("4.0.8");

        assertThat(properties.getDefaults().getGroupId()).isEqualTo("com.acme");
        assertThat(properties.getDefaults().getVersion()).isEqualTo("2.0.0");
        assertThat(properties.getDefaults().getSpringBootVersion()).isEqualTo("4.0.8");
    }

    @Test
    void versionsListIsImmutableAndOfferable() {
        ScafftrixProperties properties = new ScafftrixProperties();

        assertThat(properties.getVersions().getSpringBoot()).contains("4.1.1");
        assertThat(properties.getVersions().offers("4.1.1")).isTrue();
        assertThat(properties.getVersions().offers("9.9.9")).isFalse();
        assertThat(properties.getVersions().offers(null)).isFalse();

        properties.getVersions().setSpringBoot(List.of("4.1.1"));
        assertThat(properties.getVersions().getSpringBoot()).containsExactly("4.1.1");

        properties.getVersions().setSpringBoot(null);
        assertThat(properties.getVersions().getSpringBoot()).isEmpty();
    }

    @Test
    void generationDefaultsAndOverrides() {
        ScafftrixProperties properties = new ScafftrixProperties();

        assertThat(properties.getGeneration().getTimeout()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getGeneration().getConcurrency()).isEqualTo(2);
        assertThat(properties.getGeneration().getMaxArtifactSizeMb()).isEqualTo(200);

        properties.getGeneration().setTimeout(Duration.ofMinutes(5));
        properties.getGeneration().setConcurrency(4);
        properties.getGeneration().setMaxArtifactSizeMb(50);

        assertThat(properties.getGeneration().getTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getGeneration().getConcurrency()).isEqualTo(4);
        assertThat(properties.getGeneration().getMaxArtifactSizeMb()).isEqualTo(50);
    }

    @Test
    void rootExposesAllNestedGroups() {
        ScafftrixProperties properties = new ScafftrixProperties();

        assertThat(properties.getVersions()).isNotNull();
        assertThat(properties.getGeneration()).isNotNull();
    }
}
