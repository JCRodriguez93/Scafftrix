package es.scafftrix.factory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TechnologyManifestTest {

    @Test
    void mavenFactoryPinsTheBuildTool() {
        TechnologyManifest manifest = TechnologyManifest.maven(
                "H2", "Thymeleaf", true, ConfigFormat.PROPERTIES, "nota");

        assertThat(manifest.database()).isEqualTo("H2");
        assertThat(manifest.frontend()).isEqualTo("Thymeleaf");
        assertThat(manifest.buildTool()).isEqualTo("Maven");
        assertThat(manifest.contractFirst()).isTrue();
        assertThat(manifest.configFormat()).isEqualTo(ConfigFormat.PROPERTIES);
        assertThat(manifest.note()).isEqualTo("nota");
    }

    @Test
    void blankFrontendBecomesAnEmptyString() {
        TechnologyManifest manifest = new TechnologyManifest(
                "H2", null, "Maven", false, ConfigFormat.PROPERTIES, "nota");

        assertThat(manifest.frontend()).isEmpty();
        assertThat(manifest.toTechnologyStack()).isEqualTo(new TechnologyStack("H2", "", "Maven"));
    }

    @Test
    void rejectsInvalidManifests() {
        assertThatThrownBy(() -> new TechnologyManifest(
                " ", "Thymeleaf", "Maven", false, ConfigFormat.PROPERTIES, "nota"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TechnologyManifest(
                "H2", "Thymeleaf", " ", false, ConfigFormat.PROPERTIES, "nota"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TechnologyManifest(
                "H2", "Thymeleaf", "Maven", false, null, "nota"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
