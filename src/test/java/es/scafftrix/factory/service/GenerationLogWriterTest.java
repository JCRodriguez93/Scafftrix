package es.scafftrix.factory.service;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerationLogWriterTest {

    @TempDir
    Path workspace;

    private GenerationLogWriter writer;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        ScafftrixProperties properties = new ScafftrixProperties();
        properties.getWorkspace().setPath(workspace.toString());
        writer = new GenerationLogWriter(new WorkspaceManager(properties));
        projectId = UUID.randomUUID();
    }

    @Test
    void appendsAndReadsBackProgressLines() {
        writer.appendProgress(projectId, "primera");
        writer.appendProgress(projectId, "segunda");

        String log = writer.readProgress(projectId);

        assertThat(log).contains("primera").contains("segunda");
    }

    @Test
    void returnsEmptyStringWhenNothingHasBeenWritten() {
        assertThat(writer.readProgress(projectId)).isEmpty();
    }

    @Test
    void writesTheTechnicalErrorLogSeparately() {
        writer.appendProgress(projectId, "visible");
        writer.appendError(projectId, "interno");

        String progress = writer.readProgress(projectId);

        assertThat(progress).contains("visible").doesNotContain("interno");
    }

    @Test
    void swallowsAnAppendFailureInsteadOfBreakingTheGeneration() {
        WorkspaceManager failing = mock(WorkspaceManager.class);
        when(failing.logFile(any(), any())).thenThrow(new IllegalStateException("disco lleno"));
        GenerationLogWriter resilient = new GenerationLogWriter(failing);

        assertThatCode(() -> resilient.appendProgress(projectId, "linea")).doesNotThrowAnyException();
        assertThatCode(() -> resilient.appendError(projectId, "detalle")).doesNotThrowAnyException();
    }
}
