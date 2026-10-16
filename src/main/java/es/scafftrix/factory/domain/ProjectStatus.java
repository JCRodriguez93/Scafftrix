package es.scafftrix.factory.domain;

/**
 * Lifecycle of a project inside the factory.
 *
 * <p>The transition rules are enforced in the service layer
 * ({@code ProjectService} / {@code ProjectGenerationService}) so that an illegal
 * transition is impossible to persist by accident.
 */
public enum ProjectStatus {

    /** The project has been created from the form but not generated yet. */
    CREATED,

    /** A generation job has been accepted and is currently running. */
    GENERATING,

    /** Generation finished successfully and a ZIP artifact is available. */
    GENERATED,

    /** Generation failed; the reason is available in the generation log. */
    FAILED,

    /** The ZIP artifact has been downloaded at least once. */
    DOWNLOADED;

    /** A project can only be (re)generated from these states. */
    public boolean isGenerationAllowed() {
        return this == CREATED || this == FAILED;
    }

    /** Generation finished successfully (regardless of later downloads). */
    public boolean isArtifactAvailable() {
        return this == GENERATED || this == DOWNLOADED;
    }

    /** No new generation job may be started while the project is in this state. */
    public boolean isTerminalForGeneration() {
        return this == GENERATING;
    }
}