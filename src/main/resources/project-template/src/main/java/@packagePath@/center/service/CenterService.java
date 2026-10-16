package @packageName@.center.service;

import @packageName@.center.entity.CenterEntity;
import @packageName@.center.exception.CenterException;
import @packageName@.center.mapper.CenterMapper;
import @packageName@.center.repository.CenterRepository;
import org.openapitools.model.Centro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Transactional
@Service
public class CenterService {

    private final CenterRepository repository;
    private final CenterMapper mapper;

    public CenterService(CenterRepository repository, CenterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Centro> findAll() {
        return repository.findAll().stream().map(mapper::toModel).toList();
    }

    @Transactional(readOnly = true)
    public Centro findById(Integer numCentro) {
        return repository.findById(numCentro)
                .map(mapper::toModel)
                .orElseThrow(() -> new CenterException("Centro " + numCentro + " no encontrado"));
    }

    public Centro create(Centro centro) {
        return mapper.toModel(repository.save(mapper.toEntity(centro)));
    }

    public Centro update(Integer numCentro, Centro centro) {
        CenterEntity existing = repository.findById(numCentro)
                .orElseThrow(() -> new CenterException("Centro " + numCentro + " no encontrado"));
        CenterEntity updated = mapper.toEntity(centro);
        updated.setNumCentro(existing.getNumCentro());
        return mapper.toModel(repository.save(updated));
    }

    public void delete(Integer numCentro) {
        if (!repository.existsById(numCentro)) {
            throw new CenterException("Centro " + numCentro + " no encontrado");
        }
        repository.deleteById(numCentro);
    }
}
