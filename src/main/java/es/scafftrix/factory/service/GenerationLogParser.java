package es.scafftrix.factory.service;

import es.scafftrix.factory.dto.GenerationLogEntry;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a persisted {@code generation.log} back into structured entries.
 *
 * <p>Needed because the live in-memory buffer does not survive an application restart,
 * but the user should still see their full generation history. The format written by
 * {@code GenerationJobRegistry} is {@code [timestamp] [LEVEL] message}, and the parser is
 * deliberately lenient: an unparseable line is still shown, as INFO, so the log is never
 * silently truncated.
 */
final class GenerationLogParser {

    private static final Pattern LINE =
            Pattern.compile("^\\[(?<ts>[^]]++)]\\s*+\\[(?<level>[A-Z]++)]\\s*+(?<msg>.*)$");

    private GenerationLogParser() {
    }

    static List<GenerationLogEntry> parse(String raw) {
        List<GenerationLogEntry> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return entries;
        }
        for (String line : raw.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            entries.add(parseLine(line));
        }
        return entries;
    }

    private static GenerationLogEntry parseLine(String line) {
        Matcher matcher = LINE.matcher(line);
        if (!matcher.matches()) {
            return new GenerationLogEntry(GenerationLogEntry.Level.INFO, line, Instant.EPOCH);
        }
        return new GenerationLogEntry(
                toLevel(matcher.group("level")),
                matcher.group("msg"),
                toInstant(matcher.group("ts")));
    }

    private static GenerationLogEntry.Level toLevel(String raw) {
        try {
            return GenerationLogEntry.Level.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return GenerationLogEntry.Level.INFO;
        }
    }

    private static Instant toInstant(String raw) {
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }
}