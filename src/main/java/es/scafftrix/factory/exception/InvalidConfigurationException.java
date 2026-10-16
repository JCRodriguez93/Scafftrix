package es.scafftrix.factory.exception;

/** The submitted configuration is invalid (bad group id, invalid Java package, ...). */
public class InvalidConfigurationException extends ScafftrixException {

    public InvalidConfigurationException(String userMessage, String technicalMessage) {
        super(userMessage, technicalMessage);
    }

    public InvalidConfigurationException(String userMessage, String technicalMessage, Throwable cause) {
        super(userMessage, technicalMessage, cause);
    }
}