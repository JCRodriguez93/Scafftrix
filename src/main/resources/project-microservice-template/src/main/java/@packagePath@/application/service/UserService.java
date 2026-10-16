package @packageName@.application.service;

import @packageName@.application.port.in.UserUseCase;
import @packageName@.application.port.out.UserRepository;
import @packageName@.domain.exception.UserNotFoundException;
import @packageName@.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion del caso de uso de usuarios.
 *
 * <p>Orquesta el dominio y el puerto de persistencia. No conoce HTTP ni JPA: depende de la
 * interfaz {@link UserRepository}, que Spring inyecta con el adaptador de infraestructura.
 */
@Service
@Transactional
public class UserService implements UserUseCase {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public User create(User user) {
        // El id lo asigna la base de datos; el que llegue en el payload se ignora.
        return repository.save(new User(null, user.username(), user.email(),
                user.firstName(), user.lastName()));
    }

    @Override
    public User update(Long id, User user) {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        return repository.save(new User(id, user.username(), user.email(),
                user.firstName(), user.lastName()));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
