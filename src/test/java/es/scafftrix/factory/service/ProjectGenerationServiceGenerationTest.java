package es.scafftrix.factory.service;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.GenerationProfile;
import es.scafftrix.factory.exception.DownloadException;
import es.scafftrix.factory.exception.GenerationInProgressException;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.generator.GenerationResult;
import es.scafftrix.factory.generator.ProjectGenerator;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectGenerationServiceGenerationTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
    private final GenerationLogWriter logWriter = mock(GenerationLogWriter.class);
    private final GenerationJobRegistry jobRegistry = new GenerationJobRegistry(logWriter);
    private final Executor executor = Runnable::run;

    @TempDir
    Path sourceDir;

    private UUID projectId;
    private Project project;
    private GenerationProfile profile;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
        profile = new GenerationProfile(ProjectType.SPRING_BOOT, "Customer Management", "customerManagement",
                "es.scafftrix", "customer-management", "es.scafftrix.customermanagement",
                "1.0.0-SNAPSHOT", "4.1.1", ConfigFormat.PROPERTIES, "");
        assignId(project, projectId);

        when(projectService.getEntity(projectId)).thenReturn(project);
        when(projectService.toGenerationProfile(project)).thenReturn(profile);
        when(workspaceManager.sourceDirectory(projectId)).thenReturn(sourceDir);
    }

    private static void assignId(Project project, UUID id) {
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private ProjectGenerationService serviceWith(ProjectGenerator... generators) {
        return new ProjectGenerationService(projectService, List.of(generators),
                workspaceManager, jobRegistry, executor);
    }

    private static final class FakeGenerator implements ProjectGenerator {
        private final ProjectType[] types;
        private final String name;
        private RuntimeException failure;
        private Duration duration = Duration.ofSeconds(5);

        FakeGenerator(ProjectType[] types, String name) {
            this.types = types;
            this.name = name;
        }

        FakeGenerator failingWith(RuntimeException failure) {
            this.failure = failure;
            return this;
        }

        FakeGenerator taking(Duration duration) {
            this.duration = duration;
            return this;
        }

        @Override
        public ProjectType[] appliesTo() {
            return types;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public GenerationResult generate(GenerationProfile profile, Path outputDirectory, ProgressListener progress) {
            progress.onStep(null);
            progress.onStep("   ");
            progress.onStep("Paso real");
            if (failure != null) {
                throw failure;
            }
            return GenerationResult.success(name, duration, outputDirectory);
        }
    }

    @Test
    void runsTheWholePipelineAndMarksTheProjectGenerated() {
        Path zip = sourceDir.resolve("customer-management.zip");
        when(workspaceManager.compress(eq(projectId), eq("customer-management"), any(Path.class))).thenReturn(zip);
        when(workspaceManager.relativePath(zip)).thenReturn("rel/artifact.zip");
        ProjectGenerationService service =
                serviceWith(new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake"));

        CompletableFuture<Void> future = service.requestGeneration(projectId);

        assertThat(future).isCompleted();
        verify(projectService).beginGeneration(projectId);
        verify(workspaceManager).ensureWorkspaceAvailable();
        verify(workspaceManager).resetSourceDirectory(projectId);
        verify(projectService).markGenerated(project, "rel/artifact.zip");
        assertThat(jobRegistry.isRunning(projectId)).isFalse();
    }

    @Test
    void formatsLongDurationsInMinutes() {
        Path zip = sourceDir.resolve("z.zip");
        when(workspaceManager.compress(eq(projectId), eq("customer-management"), any(Path.class))).thenReturn(zip);
        when(workspaceManager.relativePath(zip)).thenReturn("rel/z.zip");
        FakeGenerator generator = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake")
                .taking(Duration.ofSeconds(90));
        ProjectGenerationService service = serviceWith(generator);

        service.requestGeneration(projectId);

        verify(projectService).markGenerated(project, "rel/z.zip");
    }

    @Test
    void selectsTheGeneratorThatSupportsTheProfile() {
        Path zip = sourceDir.resolve("z.zip");
        when(workspaceManager.compress(eq(projectId), eq("customer-management"), any(Path.class))).thenReturn(zip);
        when(workspaceManager.relativePath(zip)).thenReturn("rel/z.zip");
        FakeGenerator batch = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BATCH}, "Batch");
        FakeGenerator boot = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Boot");
        ProjectGenerationService service = serviceWith(batch, boot);

        service.requestGeneration(projectId);

        verify(projectService).markGenerated(project, "rel/z.zip");
    }

    @Test
    void rejectsWhenNoGeneratorSupportsTheProfile() {
        ProjectGenerationService service = serviceWith(
                new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BATCH}, "Batch"));

        assertThatThrownBy(() -> service.requestGeneration(projectId))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsAConcurrentGenerationForTheSameProject() {
        assertThat(jobRegistry.tryRegister(projectId)).isTrue();
        ProjectGenerationService service =
                serviceWith(new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake"));

        assertThatThrownBy(() -> service.requestGeneration(projectId))
                .isInstanceOf(GenerationInProgressException.class);
    }

    @Test
    void releasesTheRegistryClaimWhenBeginGenerationFails() {
        doThrow(new IllegalStateException("db down")).when(projectService).beginGeneration(projectId);
        ProjectGenerationService service =
                serviceWith(new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake"));

        assertThatThrownBy(() -> service.requestGeneration(projectId))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jobRegistry.isRunning(projectId)).isFalse();
    }

    @Test
    void recordsAFailedStatusWhenTheGeneratorFails() {
        FakeGenerator generator = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake")
                .failingWith(new DownloadException("No se pudo comprimir.", "technical boom"));
        ProjectGenerationService service = serviceWith(generator);

        service.requestGeneration(projectId).join();

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(projectService).markFailed(eq(project), reason.capture());
        assertThat(reason.getValue()).isEqualTo("No se pudo comprimir.");
        verify(logWriter).appendError(eq(projectId), any(String.class));
    }

    @Test
    void recordsAGenericFailureForUnexpectedExceptions() {
        FakeGenerator generator = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake")
                .failingWith(new IllegalStateException("kaboom"));
        ProjectGenerationService service = serviceWith(generator);

        service.requestGeneration(projectId).join();

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(projectService).markFailed(eq(project), reason.capture());
        assertThat(reason.getValue()).contains("error inesperado");
    }

    @Test
    void truncatesAVeryLongFailureReason() {
        String longMessage = "x".repeat(1200);
        FakeGenerator generator = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake")
                .failingWith(new DownloadException(longMessage, "technical"));
        ProjectGenerationService service = serviceWith(generator);

        service.requestGeneration(projectId).join();

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(projectService).markFailed(eq(project), reason.capture());
        assertThat(reason.getValue()).hasSize(903).endsWith("...");
    }

    @Test
    void stillRecordsFailureWhenPersistingTheFailedStatusAlsoFails() {
        doThrow(new IllegalStateException("db down")).when(projectService)
                .markFailed(eq(project), any(String.class));
        FakeGenerator generator = new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake")
                .failingWith(new DownloadException("fallo", "technical"));
        ProjectGenerationService service = serviceWith(generator);

        service.requestGeneration(projectId).join();

        verify(logWriter).appendError(eq(projectId), any(String.class));
    }

    @Test
    void exposesTheArtifactPathOnlyWhenAnArtifactExists() {
        ProjectGenerationService service =
                serviceWith(new FakeGenerator(new ProjectType[]{ProjectType.SPRING_BOOT}, "Fake"));
        Path artifact = sourceDir.resolve("a.zip");
        when(workspaceManager.artifactPath(projectId, "customer-management")).thenReturn(artifact);
        when(workspaceManager.relativePath(artifact)).thenReturn("rel/a.zip");

        assertThatThrownBy(() -> service.artifactPath(projectId))
                .isInstanceOf(DownloadException.class);

        project.markGenerated("rel/a.zip");
        assertThat(service.artifactPath(projectId)).isEqualTo("rel/a.zip");
    }
}
