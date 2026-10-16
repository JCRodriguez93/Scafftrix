package es.scafftrix.factory.generator;

import java.time.Duration;

/**
 * Outcome of a generation attempt.
 *
 * <p>The {@code exitCode}, {@code timedOut}, {@code detectedVersion}, {@code commandLine} and
 * {@code outputTail} fields were removed with the JHipster CLI: they described an external
 * process. The template generator either produced the project or threw, so the exit code and
 * the captured process output never carried information.
 *
 * @param generator    name of the generator that ran
 * @param success      whether the project was produced
 * @param duration     wall clock time spent generating
 * @param projectRoot  directory containing the generated project, {@code null} on failure
 * @param errorSummary condensed technical failure detail, {@code null} on success
 */
public record GenerationResult(String generator,
                               boolean success,
                               Duration duration,
                               java.nio.file.Path projectRoot,
                               String errorSummary) {

    public static GenerationResult success(String generator,
                                           Duration duration,
                                           java.nio.file.Path projectRoot) {
        return new GenerationResult(generator, true, duration, projectRoot, null);
    }

    public static GenerationResult failure(String generator,
                                           Duration duration,
                                           String errorSummary) {
        return new GenerationResult(generator, false, duration, null, errorSummary);
    }
}