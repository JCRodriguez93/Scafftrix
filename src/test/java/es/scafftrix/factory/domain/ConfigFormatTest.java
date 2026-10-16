package es.scafftrix.factory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigFormatTest {

    @Test
    void exposesItsIdentifierExtensionAndLabel() {
        assertThat(ConfigFormat.PROPERTIES.getId()).isEqualTo("properties");
        assertThat(ConfigFormat.PROPERTIES.getExtension()).isEqualTo(".properties");
        assertThat(ConfigFormat.PROPERTIES.getDisplayName()).contains("Properties");

        assertThat(ConfigFormat.YAML.getId()).isEqualTo("yml");
        assertThat(ConfigFormat.YAML.getExtension()).isEqualTo(".yml");
        assertThat(ConfigFormat.YAML.getDisplayName()).contains("YAML");
    }

    @Test
    void buildsConfigurationFileNames() {
        assertThat(ConfigFormat.PROPERTIES.fileName("application-local")).isEqualTo("application-local.properties");
        assertThat(ConfigFormat.YAML.fileName("application")).isEqualTo("application.yml");
    }

    @Test
    void matchesOnlyItsOwnApplicationFiles() {
        assertThat(ConfigFormat.YAML.matches("application.yml")).isTrue();
        assertThat(ConfigFormat.YAML.matches("application-local.yml")).isTrue();
        assertThat(ConfigFormat.YAML.matches("application.properties")).isFalse();
        assertThat(ConfigFormat.PROPERTIES.matches("application.properties")).isTrue();
        assertThat(ConfigFormat.PROPERTIES.matches("bootstrap.properties")).isFalse();
        assertThat(ConfigFormat.PROPERTIES.matches(null)).isFalse();
    }
}
