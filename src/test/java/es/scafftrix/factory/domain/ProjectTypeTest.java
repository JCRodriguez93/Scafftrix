package es.scafftrix.factory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTypeTest {

    @Test
    void exposesThePinnedManifestOfEveryProfile() {
        for (ProjectType type : ProjectType.values()) {
            assertThat(type.getDisplayName()).isNotBlank();
            assertThat(type.getDescription()).isNotBlank();
            assertThat(type.getAllowedDatabases()).isNotEmpty();
            assertThat(type.getDatabaseLabel()).isNotBlank();
            assertThat(type.getBuildToolLabel()).isEqualTo("Maven");
            assertThat(type.getTechnologyNote()).isNotBlank();
            assertThat(type.technologyStack()).isNotNull();
            assertThat(type.defaultConfigFormat()).isNotNull();
        }
    }

    @Test
    void describesTheFrontendOnlyWhenTheProfileHasOne() {
        assertThat(ProjectType.SPRING_BOOT.isContractFirst()).isTrue();
        assertThat(ProjectType.SPRING_BOOT.getFrontendLabel()).isEqualTo("Thymeleaf");

        assertThat(ProjectType.SPRING_BATCH.isContractFirst()).isFalse();
        assertThat(ProjectType.SPRING_BATCH.getFrontendLabel()).isEmpty();

        assertThat(ProjectType.SPRING_MICROSERVICE.isContractFirst()).isTrue();
        assertThat(ProjectType.SPRING_MICROSERVICE.getFrontendLabel()).isEmpty();
    }

    @Test
    void supportsOnlyItsAllowedDatabases() {
        assertThat(ProjectType.SPRING_BOOT.supports(DatabaseType.H2)).isTrue();
        assertThat(ProjectType.SPRING_BOOT.supports(DatabaseType.POSTGRESQL)).isTrue();
        assertThat(ProjectType.SPRING_BATCH.supports(DatabaseType.POSTGRESQL)).isFalse();
        assertThat(ProjectType.SPRING_BATCH.supports(DatabaseType.H2)).isTrue();
        assertThat(ProjectType.SPRING_MICROSERVICE.supports(DatabaseType.POSTGRESQL)).isTrue();
        assertThat(ProjectType.SPRING_BOOT.supports(null)).isFalse();
    }
}
