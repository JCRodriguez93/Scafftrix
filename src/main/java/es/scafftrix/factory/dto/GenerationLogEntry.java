package es.scafftrix.factory.dto;

import java.time.Instant;

/**
 * One line of the generation console shown to the user.
 *
 * @param level    {@code INFO}, {@code SUCCESS}, {@code ERROR} or {@code WARN}
 * @param message  sanitised text, safe to render in a browser
 * @param instant  when the step was recorded
 */
public record GenerationLogEntry(Level level, String message, Instant instant) {

    public enum Level { INFO, SUCCESS, WARN, ERROR }

    public static GenerationLogEntry info(String message) {
        return new GenerationLogEntry(Level.INFO, message, Instant.now());
    }

    public static GenerationLogEntry success(String message) {
        return new GenerationLogEntry(Level.SUCCESS, message, Instant.now());
    }

    public static GenerationLogEntry warn(String message) {
        return new GenerationLogEntry(Level.WARN, message, Instant.now());
    }

    public static GenerationLogEntry error(String message) {
        return new GenerationLogEntry(Level.ERROR, message, Instant.now());
    }
}