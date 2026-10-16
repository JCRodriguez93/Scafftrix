package es.scafftrix.factory.domain;

/**
 * Truthful, per-profile description of the technology a generated project really uses.
 *
 * <p>It replaces the single global default that was shown for every profile, which claimed
 * a Thymeleaf frontend even for the batch process and the microservice. A manifest is the
 * single source of both the technical panel in the UI and the pinned {@link TechnologyStack}
 * stored on the project.
 *
 * <p>{@code frontend} may be blank: profiles without a view layer (batch, microservice) have
 * no frontend at all, and the UI renders a dash instead of inventing one.
 */
public record TechnologyManifest(String database,
                                 String frontend,
                                 String buildTool,
                                 boolean contractFirst,
                                 ConfigFormat configFormat,
                                 String note) {

    /** The corporate build tool every profile uses today. */
    public static final String BUILD_TOOL_MAVEN = "Maven";

    public TechnologyManifest {
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("Technology manifest database must not be blank");
        }
        if (buildTool == null || buildTool.isBlank()) {
            throw new IllegalArgumentException("Technology manifest build tool must not be blank");
        }
        if (configFormat == null) {
            throw new IllegalArgumentException("Technology manifest config format must not be null");
        }
        frontend = frontend == null ? "" : frontend.trim();
    }

    /** A Maven-built manifest, so profiles do not repeat the build tool literal. */
    public static TechnologyManifest maven(String database,
                                           String frontend,
                                           boolean contractFirst,
                                           ConfigFormat configFormat,
                                           String note) {
        return new TechnologyManifest(database, frontend, BUILD_TOOL_MAVEN, contractFirst, configFormat, note);
    }

    /** The pinned stack copied onto the project at creation time for auditability. */
    public TechnologyStack toTechnologyStack() {
        return new TechnologyStack(database, frontend, buildTool);
    }
}
