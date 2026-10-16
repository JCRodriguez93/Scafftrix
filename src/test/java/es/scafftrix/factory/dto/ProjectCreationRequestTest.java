package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectCreationRequestTest {

    @Test
    void readsBackEveryValueItWasGiven() {
        ProjectCreationRequest request = new ProjectCreationRequest();
        request.setName("Customer Management");
        request.setProjectType(ProjectType.SPRING_MICROSERVICE);
        request.setBaseName("customerManagement");
        request.setArtifactId("customer-management");
        request.setPackageName("es.scafftrix.customermanagement");
        request.setGroupId("es.scafftrix");
        request.setVersion("2.0.0");
        request.setSpringBootVersion("4.0.8");
        request.setConfigFormat(ConfigFormat.YAML);
        request.setDescription("descripción");

        assertThat(request.getName()).isEqualTo("Customer Management");
        assertThat(request.getProjectType()).isEqualTo(ProjectType.SPRING_MICROSERVICE);
        assertThat(request.getBaseName()).isEqualTo("customerManagement");
        assertThat(request.getArtifactId()).isEqualTo("customer-management");
        assertThat(request.getPackageName()).isEqualTo("es.scafftrix.customermanagement");
        assertThat(request.getGroupId()).isEqualTo("es.scafftrix");
        assertThat(request.getVersion()).isEqualTo("2.0.0");
        assertThat(request.getSpringBootVersion()).isEqualTo("4.0.8");
        assertThat(request.getConfigFormat()).isEqualTo(ConfigFormat.YAML);
        assertThat(request.getDescription()).isEqualTo("descripción");
    }
}
