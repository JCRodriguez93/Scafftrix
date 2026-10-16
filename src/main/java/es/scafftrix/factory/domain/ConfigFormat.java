package es.scafftrix.factory.domain;

/**
 * Format of the Spring Boot configuration files shipped by the generated project.
 *
 * <p>This is a genuine choice, not a pinned standard: the factory writes <em>both</em>
 * variants of every {@code application*} file and keeps only the selected one, so a
 * developer can work with {@code application.properties} or {@code application.yml}
 * without touching the architecture of the generated project. The default per profile
 * preserves the historical behaviour (properties for the monolith and the batch process,
 * YAML for the contract-first microservice).
 */
public enum ConfigFormat {

    PROPERTIES("properties", ".properties", "Properties (application.properties)"),

    YAML("yml", ".yml", "YAML (application.yml)");

    private final String id;
    private final String extension;
    private final String displayName;

    ConfigFormat(String id, String extension, String displayName) {
        this.id = id;
        this.extension = extension;
        this.displayName = displayName;
    }

    /** Short, stable identifier used in views and data attributes. */
    public String getId() {
        return id;
    }

    /** File extension, used by the generator to keep only the selected variant. */
    public String getExtension() {
        return extension;
    }

    /** Label shown in the configuration format selector. */
    public String getDisplayName() {
        return displayName;
    }

    /** Name of a configuration file for this format, e.g. {@code application-local.yml}. */
    public String fileName(String baseName) {
        return baseName + extension;
    }

    /**
     * Whether a template configuration file (for example {@code application-local.yml})
     * belongs to this format.
     */
    public boolean matches(String fileName) {
        return fileName != null && fileName.startsWith("application") && fileName.endsWith(extension);
    }
}
