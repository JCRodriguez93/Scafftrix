package es.scafftrix.factory.domain;

/**
 * Corporate technology standards that are <strong>pinned</strong> by the factory.
 *
 * <p>These values are never taken from user input. They come from the {@link ProjectType}
 * manifest of the selected profile and are copied onto the project at creation time. Keeping
 * them in an immutable value object makes it obvious at a glance which parts of the stack are
 * negotiable (they are not) and prevents them from drifting between the form, the generator
 * and the database.
 *
 * <p>{@code frontend()} may be blank: profiles without a view layer (batch, microservice)
 * have no frontend.
 */
public record TechnologyStack(String database, String frontend, String buildTool) {

    public TechnologyStack(String database, String frontend, String buildTool) {
        this.database = requireText(database, "database");
        this.frontend = frontend == null ? "" : frontend.trim();
        this.buildTool = requireText(buildTool, "buildTool");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pinned technology stack field must not be blank: " + field);
        }
        return value;
    }
}