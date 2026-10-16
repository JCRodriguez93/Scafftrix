package @packageName@.center.repository;

import @packageName@.center.entity.CenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterRepository extends JpaRepository<CenterEntity, Integer> {
}
