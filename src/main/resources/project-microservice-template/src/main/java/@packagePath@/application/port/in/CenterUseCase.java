package @packageName@.application.port.in;

import @packageName@.domain.model.Center;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) de centros.
 */
public interface CenterUseCase {

    List<Center> findAll();

    Center findById(Long id);

    Center create(Center center);

    Center update(Long id, Center center);

    void delete(Long id);
}
