package es.scafftrix.factory.exception;

import java.util.UUID;

/** A generation job is already running for this project. */
public class GenerationInProgressException extends ScafftrixException {

    public GenerationInProgressException(UUID projectId) {
        super("Este proyecto ya tiene una generación en curso. Espere a que finalice.",
                "Rejected generation request, job already running for projectId=" + projectId);
    }
}