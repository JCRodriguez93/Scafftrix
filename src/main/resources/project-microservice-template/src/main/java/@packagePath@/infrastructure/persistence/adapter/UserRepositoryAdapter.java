package @packageName@.infrastructure.persistence.adapter;

import @packageName@.application.port.out.UserRepository;
import @packageName@.domain.model.User;
import @packageName@.infrastructure.persistence.entity.UserEntity;
import @packageName@.infrastructure.persistence.mapper.UserPersistenceMapper;
import @packageName@.infrastructure.persistence.repository.JpaUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa el puerto {@link UserRepository} con Spring Data + JPA.
 *
 * <p>Es el unico punto donde se cruzan dominio y persistencia: traduce con MapStruct y
 * delega en el repositorio JPA.
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository, UserPersistenceMapper mapper) {
        this.jpaUserRepository = jpaUserRepository;
        this.mapper = mapper;
    }

    @Override
    public List<User> findAll() {
        return jpaUserRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaUserRepository.save(mapper.toEntity(user));
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaUserRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaUserRepository.deleteById(id);
    }
}
