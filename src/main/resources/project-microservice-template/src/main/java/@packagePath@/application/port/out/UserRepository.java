package @packageName@.application.port.out;

import @packageName@.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de persistencia de usuarios.
 *
 * <p>Habla en terminos del modelo de dominio, no de entidades JPA. La implementacion real
 * vive en {@code infrastructure.persistence.adapter}.
 */
public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(Long id);

    User save(User user);

    boolean existsById(Long id);

    void deleteById(Long id);
}
