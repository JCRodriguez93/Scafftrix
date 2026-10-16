package @packageName@.domain.model;

/**
 * Modelo de dominio de un usuario.
 *
 * <p>No depende de OpenAPI ni de JPA: la API y la persistencia se adaptan a este modelo,
 * nunca al reves. Es un record para que sea inmutable y para que MapStruct pueda construirlo
 * a partir de sus componentes.
 */
public record User(Long id, String username, String email, String firstName, String lastName) {
}
