package es.scafftrix.factory.exception;

import java.util.UUID;

/** The requested project does not exist. */
public class ProjectNotFoundException extends ScafftrixException {

    public ProjectNotFoundException(UUID projectId) {
        super("El proyecto solicitado no existe.",
                "Project not found: id=" + projectId);
    }
}