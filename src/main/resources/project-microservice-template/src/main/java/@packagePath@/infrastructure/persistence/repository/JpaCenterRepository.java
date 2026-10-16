package @packageName@.infrastructure.persistence.repository;

import @packageName@.infrastructure.persistence.entity.CenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data de centros.
 */
public interface JpaCenterRepository extends JpaRepository<CenterEntity, Long> {
}
