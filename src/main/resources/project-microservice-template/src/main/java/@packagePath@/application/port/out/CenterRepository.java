package @packageName@.application.port.out;

import @packageName@.domain.model.Center;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de persistencia de centros.
 */
public interface CenterRepository {

    List<Center> findAll();

    Optional<Center> findById(Long id);

    Center save(Center center);

    boolean existsById(Long id);

    void deleteById(Long id);
}
