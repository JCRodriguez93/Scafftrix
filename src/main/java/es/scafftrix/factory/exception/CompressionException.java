package es.scafftrix.factory.exception;

/** The generated project could not be packaged into a ZIP artifact. */
public class CompressionException extends ScafftrixException {

    public CompressionException(String technicalMessage, Throwable cause) {
        super("El proyecto se ha generado pero no ha sido posible empaquetarlo para su descarga.",
                "Failed to compress generated project: " + technicalMessage, cause);
    }
}