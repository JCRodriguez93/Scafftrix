package es.scafftrix.factory.controller;

import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.dto.GenerationStatusDto;
import es.scafftrix.factory.service.ProjectGenerationService;
import es.scafftrix.factory.service.ProjectService;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationControllerCoverageTest {

    private final ProjectGenerationService generationService = mock(ProjectGenerationService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
    private final GenerationController controller =
            new GenerationController(generationService, projectService, workspaceManager);

    @Test
    void generateStartsTheJobAndRedirectsToTheConsole() {
        UUID id = UUID.randomUUID();

        String view = controller.generate(id);

        assertThat(view).isEqualTo("redirect:/projects/" + id + "?tab=logs");
        verify(generationService).requestGeneration(id);
    }

    @Test
    void statusReturnsTheCurrentJobState() {
        UUID id = UUID.randomUUID();
        GenerationStatusDto dto = new GenerationStatusDto(id, ProjectStatus.GENERATING, false, 0, null, List.of());
        when(generationService.status(id)).thenReturn(dto);

        ResponseEntity<GenerationStatusDto> response = controller.status(id);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void streamLogCompletesOnceTheJobIsFinished() {
        UUID id = UUID.randomUUID();
        GenerationStatusDto dto = new GenerationStatusDto(id, ProjectStatus.GENERATED, true, 1, null, List.of());
        when(generationService.status(id)).thenReturn(dto);

        SseEmitter emitter = controller.streamLog(id);

        assertThat(emitter).isNotNull();
        verify(generationService, timeout(2000)).status(id);
    }

    @Test
    void downloadServesTheArtifactAndMarksItDownloaded(@TempDir Path tempDir) throws IOException {
        UUID id = UUID.randomUUID();
        Path artifact = tempDir.resolve("customer-management.zip");
        byte[] content = "zip-bytes".getBytes();
        Files.write(artifact, content);
        when(generationService.artifactPath(id)).thenReturn("customerManagement/dist/customer-management.zip");
        when(workspaceManager.resolveArtifact(any())).thenReturn(artifact);

        ResponseEntity<byte[]> response = controller.download(id);

        assertThat(response.getBody()).isEqualTo(content);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(content.length);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .contains("customer-management.zip");
        verify(projectService).markDownloaded(id);
    }
}
