package es.scafftrix.factory.exception;

/** The configured workspace directory is missing, not writable, or cannot be created. */
public class WorkspaceUnavailableException extends ScafftrixException {

    public WorkspaceUnavailableException(String technicalMessage, Throwable cause) {
        super("El espacio de trabajo de la fábrica no está disponible. Contacte con el equipo de plataforma.",
                "Workspace unavailable: " + technicalMessage, cause);
    }
}