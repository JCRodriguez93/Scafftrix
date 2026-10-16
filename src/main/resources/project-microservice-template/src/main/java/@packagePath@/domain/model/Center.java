package @packageName@.domain.model;

/**
 * Modelo de dominio de un centro de trabajo.
 *
 * <p>Igual que {@link User}, es un record inmutable e independiente de la tecnologia.
 */
public record Center(Long id, String name, String city) {
}
