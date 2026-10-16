package es.scafftrix.factory.service;

import es.scafftrix.factory.dto.GenerationLogEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationLogParserTest {

    @Test
    void returnsEmptyForNullOrBlankInput() {
        assertThat(GenerationLogParser.parse(null)).isEmpty();
        assertThat(GenerationLogParser.parse("   \n  ")).isEmpty();
    }

    @Test
    void parsesWellFormedLinesAndSkipsBlankOnes() {
        String raw = """
                [2026-01-01T10:00:00Z] [SUCCESS] Proyecto generado.

                [2026-01-01T10:00:01Z] [INFO] Descargable.""";

        List<GenerationLogEntry> entries = GenerationLogParser.parse(raw);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).level()).isEqualTo(GenerationLogEntry.Level.SUCCESS);
        assertThat(entries.get(0).message()).isEqualTo("Proyecto generado.");
        assertThat(entries.get(0).instant()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void fallsBackToInfoAndEpochForUnparseableContent() {
        List<GenerationLogEntry> entries = GenerationLogParser.parse("linea suelta sin formato");

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).level()).isEqualTo(GenerationLogEntry.Level.INFO);
        assertThat(entries.get(0).message()).isEqualTo("linea suelta sin formato");
        assertThat(entries.get(0).instant()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void fallsBackWhenTheLevelOrTimestampAreUnknown() {
        List<GenerationLogEntry> entries =
                GenerationLogParser.parse("[not-an-instant] [TRACE] detalle");

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).level()).isEqualTo(GenerationLogEntry.Level.INFO);
        assertThat(entries.get(0).message()).isEqualTo("detalle");
        assertThat(entries.get(0).instant()).isEqualTo(Instant.EPOCH);
    }
}
