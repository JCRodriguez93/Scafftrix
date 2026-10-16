package es.scafftrix.factory.service;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.GenerationLogEntry;
import es.scafftrix.factory.dto.GenerationStatusDto;
import es.scafftrix.factory.generator.ProjectGenerator;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectGenerationServiceStatusTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
    private final GenerationJobRegistry jobRegistry = mock(GenerationJobRegistry.class);
    private final Executor executor = Runnable::run;

    private final ProjectGenerationService service = new ProjectGenerationService(
            projectService, List.<ProjectGenerator>of(), workspaceManager, jobRegistry, executor);

    @Test
    void whenJobIsNotRunningStatusFallsBackToThePersistedLog() {
        UUID id = UUID.randomUUID();
        when(projectService.getEntity(id)).thenReturn(sampleProject());
        when(jobRegistry.isRunning(id)).thenReturn(false);
        when(jobRegistry.readPersistedLog(id))
                .thenReturn("[2026-01-01T00:00:00Z] [SUCCESS] Proyecto generado correctamente.");

        GenerationStatusDto dto = service.status(id);

        assertThat(dto.finished()).isTrue();
        assertThat(dto.entries()).hasSize(1);
        assertThat(dto.entries().get(0).message()).isEqualTo("Proyecto generado correctamente.");
        verify(jobRegistry, never()).liveSnapshot(id);
    }

    @Test
    void whenJobIsRunningStatusUsesTheLiveBuffer() {
        UUID id = UUID.randomUUID();
        when(projectService.getEntity(id)).thenReturn(sampleProject());
        when(jobRegistry.isRunning(id)).thenReturn(true);
        when(jobRegistry.liveSnapshot(id)).thenReturn(List.of(GenerationLogEntry.info("Generación en curso.")));

        GenerationStatusDto dto = service.status(id);

        assertThat(dto.finished()).isFalse();
        assertThat(dto.entries()).hasSize(1);
        verify(jobRegistry, never()).readPersistedLog(id);
    }

    private Project sampleProject() {
        return new Project("Batch test",
                new ProjectCoordinates("batchTest", "es.scafftrix", "batch-test",
                        "es.scafftrix.batchtest", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BATCH,
                new TechnologyStack("H2", "", "Maven"), "4.1.1", ConfigFormat.PROPERTIES);
    }
}
