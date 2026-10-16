package @packageName@.infrastructure.persistence.repository;

import @packageName@.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data. Solo habla de entidades JPA; el adaptador lo usa para
 * implementar el puerto de aplicacion.
 */
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
}
