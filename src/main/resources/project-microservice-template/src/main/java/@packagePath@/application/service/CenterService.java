package @packageName@.application.service;

import @packageName@.application.port.in.CenterUseCase;
import @packageName@.application.port.out.CenterRepository;
import @packageName@.domain.exception.CenterNotFoundException;
import @packageName@.domain.model.Center;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion del caso de uso de centros.
 */
@Service
@Transactional
public class CenterService implements CenterUseCase {

    private final CenterRepository repository;

    public CenterService(CenterRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Center> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Center findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new CenterNotFoundException(id));
    }

    @Override
    public Center create(Center center) {
        return repository.save(new Center(null, center.name(), center.city()));
    }

    @Override
    public Center update(Long id, Center center) {
        if (!repository.existsById(id)) {
            throw new CenterNotFoundException(id);
        }
        return repository.save(new Center(id, center.name(), center.city()));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new CenterNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
