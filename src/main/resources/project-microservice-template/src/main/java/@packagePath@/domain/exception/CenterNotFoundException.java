package @packageName@.domain.exception;

/**
 * Se lanza cuando se pide un centro que no existe.
 */
public class CenterNotFoundException extends RuntimeException {

    public CenterNotFoundException(Long id) {
        super("Centro " + id + " no encontrado");
    }
}
