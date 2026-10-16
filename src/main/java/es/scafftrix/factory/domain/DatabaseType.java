package es.scafftrix.factory.domain;

import es.scafftrix.factory.exception.InvalidConfigurationException;

import java.util.Locale;

/**
 * Databases the factory knows how to express in the generated JHipster configuration.
 */
public enum DatabaseType {

    H2("H2", "h2Disk"),
    POSTGRESQL("PostgreSQL", "postgresql");

    private final String displayName;
    private final String jhipsterValue;

    DatabaseType(String displayName, String jhipsterValue) {
        this.displayName = displayName;
        this.jhipsterValue = jhipsterValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getJhipsterValue() {
        return jhipsterValue;
    }

    public static DatabaseType fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidConfigurationException("La base de datos del proyecto no es válida.",
                    "Project database was null or blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DatabaseType databaseType : values()) {
            if (databaseType.name().equals(normalized)
                    || databaseType.displayName.toUpperCase(Locale.ROOT).equals(normalized)
                    || databaseType.jhipsterValue.toUpperCase(Locale.ROOT).equals(normalized)) {
                return databaseType;
            }
        }
        throw new InvalidConfigurationException("La base de datos del proyecto no está soportada.",
                "Unsupported project database: " + value);
    }
}
