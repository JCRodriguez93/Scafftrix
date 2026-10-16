package es.scafftrix.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form binding target for the "edit project" screen.
 *
 * <p>Only the user editable metadata is present: the display name, the Maven project
 * version and the description. The technical configuration that defines the generated
 * layout is deliberately absent, exactly like in {@link ProjectCreationRequest}, so a
 * crafted POST cannot reassign the project type, the pinned stack, the package or the
 * artifact coordinates. The version format is re-validated in {@code ProjectService}
 * against the same allow-list used on creation.
 */
public class ProjectUpdateRequest {

    @NotBlank(message = "El nombre del proyecto es obligatorio.")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
    private String name;

    @NotBlank(message = "La versión del proyecto es obligatoria.")
    @Pattern(regexp = "^$|^[0-9A-Za-z][0-9A-Za-z._-]*$",
            message = "La versión del proyecto no es válida. Ejemplos: 1.0.0, 1.0.0-SNAPSHOT, 2.1.")
    private String version;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
