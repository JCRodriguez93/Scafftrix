package es.scafftrix.factory.exception;

import java.time.Duration;

/** The generator did not finish within the configured timeout. */
public class GenerationTimeoutException extends ScafftrixException {

    public GenerationTimeoutException(Duration timeout, String technicalDetail) {
        super("La generación del proyecto ha excedido el tiempo máximo permitido. Inténtelo de nuevo más tarde.",
                "Template generation timed out after " + timeout + ". " + technicalDetail);
    }
}