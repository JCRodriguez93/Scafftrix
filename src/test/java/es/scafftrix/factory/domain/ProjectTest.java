package es.scafftrix.factory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTest {

    private Project newProject() {
        return new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                "inicial", ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
    }

    @Test
    void updateMetadataChangesOnlyTheEditableFields() {
        Project project = newProject();

        project.updateMetadata("Billing", "2.0.0", "nueva");

        assertThat(project.getName()).isEqualTo("Billing");
        assertThat(project.getVersion()).isEqualTo("2.0.0");
        assertThat(project.getDescription()).isEqualTo("nueva");
        assertThat(project.getProjectType()).isEqualTo(ProjectType.SPRING_BOOT);
        assertThat(project.getBaseName()).isEqualTo("customerManagement");
        assertThat(project.getGroupId()).isEqualTo("es.scafftrix");
        assertThat(project.getArtifactId()).isEqualTo("customer-management");
        assertThat(project.getPackageName()).isEqualTo("es.scafftrix.customermanagement");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CREATED);
    }
}
