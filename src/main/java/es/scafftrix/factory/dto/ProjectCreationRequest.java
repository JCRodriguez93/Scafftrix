package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form binding target for the "create project" screen.
 *
 * <p>Only fields the user is allowed to change appear here. The pinned technology
 * (database, frontend, build tool) is deliberately absent: it is not accepted from the
 * request at all, so it cannot be tampered with by posting extra parameters. The two
 * selectable technical options (Spring Boot version and configuration format) are present
 * and are re-validated against the configured allow-list in {@code ProjectService}.
 *
 * <p>The bean validation below is a first line of defence for fast feedback; the
 * authoritative checks live in {@code NamingService} because they must also apply to
 * values derived automatically.
 */
public class ProjectCreationRequest {

    /** Human readable project name, e.g. {@code Customer Management}. */
    @NotBlank(message = "El nombre del proyecto es obligatorio.")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
    private String name;

    /** Project profile. */
    @NotNull(message = "Debe seleccionar un tipo de proyecto.")
    private ProjectType projectType;

    /** Optional; auto-derived from the name when left blank. */
    @Pattern(regexp = "^$|^[a-z][a-zA-Z0-9]{0,39}$",
            message = "El base name debe ser camelCase y empezar por minúscula, por ejemplo customerManagement.")
    private String baseName;

    /** Optional; auto-derived from the name when left blank. */
    @Pattern(regexp = "^$|^[a-z][a-z0-9-]{0,62}$",
            message = "El Artifact ID debe estar en kebab-case, por ejemplo customer-management.")
    private String artifactId;

    /** Optional; auto-derived from group id and base name when left blank. */
    @Pattern(regexp = "^$|^[a-z][a-z0-9]*+(\\.[a-z][a-z0-9]*+)++$",
            message = "El package name debe ser un paquete Java válido en minúsculas.")
    private String packageName;

    @NotBlank(message = "El Group ID es obligatorio.")
    @Pattern(regexp = "^$|^[A-Za-z]\\w*+(\\.[A-Za-z]\\w++)*+$",
            message = "El Group ID no es válido. Formato esperado: es.scafftrix")
    private String groupId;

    @NotBlank(message = "La versión del proyecto es obligatoria.")
    @Pattern(regexp = "^$|^[0-9A-Za-z][0-9A-Za-z._-]*$",
            message = "La versión del proyecto no es válida. Ejemplos: 1.0.0, 1.0.0-SNAPSHOT, 2.1.")
    private String version;

    /** Optional; defaults to the configured Spring Boot version when left blank. */
    @Size(max = 40, message = "La versión de Spring Boot no puede superar los 40 caracteres.")
    @Pattern(regexp = "^$|^[0-9A-Za-z][0-9A-Za-z._-]*$",
            message = "La versión de Spring Boot no es válida, por ejemplo 4.1.1.")
    private String springBootVersion;

    /** Optional; defaults to the selected profile's format when left blank. */
    private ConfigFormat configFormat;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public void setSpringBootVersion(String springBootVersion) {
        this.springBootVersion = springBootVersion;
    }

    public ConfigFormat getConfigFormat() {
        return configFormat;
    }

    public void setConfigFormat(ConfigFormat configFormat) {
        this.configFormat = configFormat;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
