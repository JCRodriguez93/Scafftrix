package es.scafftrix.factory.exception;

/** The requested artifact is missing or can no longer be served. */
public class DownloadException extends ScafftrixException {

    public DownloadException(String userMessage, String technicalMessage) {
        super(userMessage, technicalMessage);
    }

    public DownloadException(String userMessage, String technicalMessage, Throwable cause) {
        super(userMessage, technicalMessage, cause);
    }
}