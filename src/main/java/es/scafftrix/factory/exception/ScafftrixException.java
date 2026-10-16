package es.scafftrix.factory.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base type for every error the factory raises deliberately.
 *
 * <p>Two distinct messages are carried on purpose:
 * <ul>
 *   <li>{@code userMessage} &mdash; safe to display in the UI. Never contains paths,
 *       stack traces or generator internals.</li>
 *   <li>{@code technicalMessage} &mdash; only ever written to the application log.</li>
 * </ul>
 */
public abstract class ScafftrixException extends RuntimeException {

    private static final Logger LOG = LoggerFactory.getLogger(ScafftrixException.class);

    private final String userMessage;
    private final String technicalMessage;

    protected ScafftrixException(String userMessage, String technicalMessage, Throwable cause) {
        super(technicalMessage, cause);
        this.userMessage = userMessage;
        this.technicalMessage = technicalMessage;
        log();
    }

    protected ScafftrixException(String userMessage, String technicalMessage) {
        this(userMessage, technicalMessage, null);
    }

    private void log() {
        if (getCause() != null) {
            LOG.error("[{}] {}", getClass().getSimpleName(), technicalMessage, getCause());
        } else {
            LOG.error("[{}] {}", getClass().getSimpleName(), technicalMessage);
        }
    }

    /** Message safe to render to the end user. */
    public String getUserMessage() {
        return userMessage;
    }

    /** Message written to the technical log. */
    public String getTechnicalMessage() {
        return technicalMessage;
    }
}