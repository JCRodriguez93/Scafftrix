package es.scafftrix.factory.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectLifecycleTest {

    private Project newProject() {
        return new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                "inicial", ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
    }

    private static Project withId(Project project, UUID id) {
        try {
            Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return project;
    }

    @Test
    void drivesEveryLifecycleTransition() {
        Project project = newProject();

        project.markGenerating();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.GENERATING);
        assertThat(project.getFailureReason()).isNull();

        project.markFailed("motivo");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.FAILED);
        assertThat(project.getFailureReason()).isEqualTo("motivo");

        project.markGenerated("rel/artifact.zip");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.GENERATED);
        assertThat(project.isArtifactAvailable()).isTrue();
        assertThat(project.getFailureReason()).isNull();

        project.markDownloaded();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DOWNLOADED);

        project.clearArtifact();
        assertThat(project.getArtifactPath()).isNull();
        assertThat(project.isArtifactAvailable()).isFalse();
    }

    @Test
    void exposesThePinnedTechnologyStack() {
        Project project = newProject();

        assertThat(project.technologyStack())
                .isEqualTo(new TechnologyStack("H2", "Thymeleaf", "Maven"));
    }

    @Test
    void equalityIsBasedOnTheGeneratedId() {
        UUID id = UUID.randomUUID();
        Project a = withId(newProject(), id);
        Project b = withId(newProject(), id);
        Project c = withId(newProject(), UUID.randomUUID());

        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .hasSameHashCodeAs(b)
                .isNotEqualTo(c)
                .isNotEqualTo(null)
                .isNotEqualTo("not a project");
    }

    @Test
    void transientProjectsAreNeverEqual() {
        Project a = newProject();
        Project b = newProject();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringDescribesTheAggregate() {
        assertThat(newProject().toString()).contains("Customer Management").contains("CREATED");
    }
}
