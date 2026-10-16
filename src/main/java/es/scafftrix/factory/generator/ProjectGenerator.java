package es.scafftrix.factory.generator;

import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.GenerationProfile;

import java.nio.file.Path;

/**
 * Strategy that turns a validated {@link GenerationProfile} into a project on disk.
 *
 * <p>This is the seam that keeps the rest of the application independent from how a project
 * is actually produced. Today {@link TemplateProjectGenerator} is the only implementation, but
 * the same contract could later be served by a different generator, a remote service client or
 * an internal service. Selecting an implementation is a configuration concern, never a change in
 * the service or controller layer.
 *
 * <p>Implementations must be safe to call concurrently and must be idempotent for a given
 * output directory: the caller provides a freshly reset, empty directory.
 */
public interface ProjectGenerator {

    /**
     * @return the project types this generator is able to produce
     */
    ProjectType[] appliesTo();

    /**
     * Human readable name of the generator, shown in the UI and logs.
     */
    String name();

    /**
     * Generates a project.
     *
     * @param profile        fully validated and resolved configuration; contains no unvalidated input
     * @param outputDirectory empty, existing, inside the workspace; becomes the project root
     * @param progress       sink for human readable progress messages
     * @return the result of the generation, never {@code null}
     * @throws es.scafftrix.factory.exception.ScafftrixException on any failure
     */
    GenerationResult generate(GenerationProfile profile, Path outputDirectory, ProgressListener progress);

    /**
     * Sink for user facing progress lines (rendered as the live console).
     */
    @FunctionalInterface
    interface ProgressListener {

        void onStep(String step);

        ProgressListener NOOP = step -> {
        };
    }
}