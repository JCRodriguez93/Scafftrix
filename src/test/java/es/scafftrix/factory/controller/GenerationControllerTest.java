package es.scafftrix.factory.controller;

import es.scafftrix.factory.exception.DownloadException;
import es.scafftrix.factory.service.ProjectGenerationService;
import es.scafftrix.factory.service.ProjectService;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationControllerTest {

    private final ProjectGenerationService generationService = mock(ProjectGenerationService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);

    private final GenerationController controller =
            new GenerationController(generationService, projectService, workspaceManager);

    @Test
    void downloadDoesNotMarkProjectAsDownloadedWhenArtifactCannotBeResolved() {
        UUID projectId = UUID.randomUUID();
        String relativeArtifact = projectId + "/dist/customer-management.zip";

        when(generationService.artifactPath(projectId)).thenReturn(relativeArtifact);
        when(workspaceManager.resolveArtifact(relativeArtifact)).thenThrow(new DownloadException(
                "El paquete del proyecto ya no está disponible en el servidor.",
                "Stored artifact path does not resolve to a file"));

        assertThrows(DownloadException.class, () -> controller.download(projectId));

        verify(projectService, never()).markDownloaded(projectId);
    }

    @Test
    void downloadMarksProjectAsDownloadedAfterReadingArtifact(@TempDir Path tempDir) throws IOException {
        UUID projectId = UUID.randomUUID();
        String relativeArtifact = projectId + "/dist/customer-management.zip";
        byte[] zipBytes = new byte[]{'P', 'K', 3, 4};
        Path zip = tempDir.resolve("customer-management.zip");
        Files.write(zip, zipBytes);

        when(generationService.artifactPath(projectId)).thenReturn(relativeArtifact);
        when(workspaceManager.resolveArtifact(relativeArtifact)).thenReturn(zip);

        ResponseEntity<byte[]> response = controller.download(projectId);

        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(zipBytes, response.getBody());
        verify(projectService).markDownloaded(projectId);
    }
}
