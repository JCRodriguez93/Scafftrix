package es.scafftrix.factory.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationLogEntryTest {

    @Test
    void factoriesBuildEveryLevelWithATimestamp() {
        assertThat(GenerationLogEntry.info("i").level()).isEqualTo(GenerationLogEntry.Level.INFO);
        assertThat(GenerationLogEntry.success("s").level()).isEqualTo(GenerationLogEntry.Level.SUCCESS);
        assertThat(GenerationLogEntry.warn("w").level()).isEqualTo(GenerationLogEntry.Level.WARN);
        assertThat(GenerationLogEntry.error("e").level()).isEqualTo(GenerationLogEntry.Level.ERROR);

        GenerationLogEntry entry = GenerationLogEntry.info("mensaje");
        assertThat(entry.message()).isEqualTo("mensaje");
        assertThat(entry.instant()).isNotNull();
    }

    @Test
    void exposesTheExplicitRecordComponents() {
        Instant when = Instant.parse("2026-01-01T00:00:00Z");
        GenerationLogEntry entry = new GenerationLogEntry(GenerationLogEntry.Level.WARN, "aviso", when);

        assertThat(entry.level()).isEqualTo(GenerationLogEntry.Level.WARN);
        assertThat(entry.message()).isEqualTo("aviso");
        assertThat(entry.instant()).isEqualTo(when);
    }
}
