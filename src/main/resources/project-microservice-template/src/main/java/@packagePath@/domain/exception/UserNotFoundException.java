package @packageName@.domain.exception;

/**
 * Se lanza cuando se pide un usuario que no existe. Al vivir en el dominio, la capa de
 * aplicacion puede comunicar el fallo sin conocer el framework web.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Usuario " + id + " no encontrado");
    }
}
