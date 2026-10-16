package es.scafftrix.factory.service;

import es.scafftrix.factory.dto.GenerationLogEntry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationJobRegistryTest {

    private final GenerationLogWriter logWriter = mock(GenerationLogWriter.class);
    private final GenerationJobRegistry registry = new GenerationJobRegistry(logWriter);
    private final UUID projectId = UUID.randomUUID();

    @Test
    void claimsTheSlotOnlyOnceUntilReleased() {
        assertThat(registry.tryRegister(projectId)).isTrue();
        assertThat(registry.tryRegister(projectId)).isFalse();
        assertThat(registry.isRunning(projectId)).isTrue();

        registry.release(projectId);

        assertThat(registry.isRunning(projectId)).isFalse();
        assertThat(registry.tryRegister(projectId)).isTrue();
    }

    @Test
    void appendsToTheLiveBufferAndThePersistedLog() {
        registry.tryRegister(projectId);
        GenerationLogEntry entry = GenerationLogEntry.info("Paso 1");

        registry.append(projectId, entry);

        assertThat(registry.liveSnapshot(projectId)).containsExactly(entry);
        verify(logWriter).appendProgress(projectId, "[" + entry.instant() + "] [INFO] Paso 1");
    }

    @Test
    void appendsStillReachesTheLogWhenNoLiveBufferExists() {
        registry.append(projectId, GenerationLogEntry.warn("Sin buffer"));

        assertThat(registry.liveSnapshot(projectId)).isEmpty();
        verify(logWriter).appendProgress(eq(projectId), org.mockito.ArgumentMatchers.contains("Sin buffer"));
    }

    @Test
    void snapshotIsEmptyWhenNotRunning() {
        assertThat(registry.liveSnapshot(projectId)).isEmpty();
    }

    @Test
    void delegatesPersistedReadAndTechnicalErrorsToTheWriter() {
        when(logWriter.readProgress(projectId)).thenReturn("persisted");

        assertThat(registry.readPersistedLog(projectId)).isEqualTo("persisted");

        registry.appendTechnicalError(projectId, "detalle");
        verify(logWriter).appendError(projectId, "detalle");
    }
}
