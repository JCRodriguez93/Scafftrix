package es.scafftrix.factory.repository;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProjectSpecificationsTest {

    @Autowired
    private ProjectRepository repository;

    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void seed() {
        Project customer = project(ProjectType.SPRING_BOOT, "Customer Management",
                "customerManagement", "customer-management");
        Project billing = project(ProjectType.SPRING_BATCH, "Billing", "billing", "billing");
        billing.markFailed("generation failed");
        Project inventory = project(ProjectType.SPRING_MICROSERVICE, "Inventory", "inventory", "inventory");
        inventory.markGenerated("inventory/dist/inventory.zip");
        repository.saveAndFlush(customer);
        repository.saveAndFlush(billing);
        repository.saveAndFlush(inventory);
    }

    private Project project(ProjectType type, String name, String baseName, String artifactId) {
        return new Project(name,
                new ProjectCoordinates(baseName, "es.scafftrix", artifactId,
                        "es.scafftrix." + baseName, "1.0.0-SNAPSHOT"),
                "desc", type, type.technologyStack(), "4.1.1", ConfigFormat.PROPERTIES);
    }

    @Test
    void withoutFiltersReturnsEverything() {
        assertThat(repository.findAll(ProjectSpecifications.filter(null, null, null), pageable))
                .hasSize(3);
    }

    @Test
    void blankSearchIsIgnored() {
        assertThat(repository.findAll(ProjectSpecifications.filter("   ", null, null), pageable))
                .hasSize(3);
    }

    @Test
    void searchMatchesNameBaseNameArtifactGroupIdAndPackageIgnoringCase() {
        assertThat(repository.findAll(ProjectSpecifications.filter("CUSTOMER", null, null), pageable))
                .extracting(Project::getArtifactId)
                .containsExactly("customer-management");

        assertThat(repository.findAll(ProjectSpecifications.filter("es.scafftrix.billing", null, null), pageable))
                .extracting(Project::getArtifactId)
                .containsExactly("billing");
    }

    @Test
    void filtersByType() {
        assertThat(repository.findAll(ProjectSpecifications.filter(null, ProjectType.SPRING_BATCH, null), pageable))
                .extracting(Project::getProjectType)
                .containsExactly(ProjectType.SPRING_BATCH);
    }

    @Test
    void filtersByStatus() {
        assertThat(repository.findAll(ProjectSpecifications.filter(null, null, ProjectStatus.FAILED), pageable))
                .extracting(Project::getStatus)
                .containsExactly(ProjectStatus.FAILED);
    }

    @Test
    void combinesSearchTypeAndStatus() {
        assertThat(repository.findAll(
                ProjectSpecifications.filter("inventory", ProjectType.SPRING_MICROSERVICE,
                        ProjectStatus.GENERATED), pageable))
                .extracting(Project::getArtifactId)
                .containsExactly("inventory");

        assertThat(repository.findAll(
                ProjectSpecifications.filter("inventory", ProjectType.SPRING_BOOT,
                        ProjectStatus.GENERATED), pageable))
                .isEmpty();
    }
}
