package @packageName@.application.port.in;

import @packageName@.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) de usuarios.
 *
 * <p>La capa api depende de esta interfaz y no de la implementacion, de modo que el
 * framework HTTP y el dominio no se conocen entre si.
 */
public interface UserUseCase {

    List<User> findAll();

    User findById(Long id);

    User create(User user);

    User update(Long id, User user);

    void delete(Long id);
}
