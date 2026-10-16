package @packageName@.infrastructure.persistence.adapter;

import @packageName@.application.port.out.CenterRepository;
import @packageName@.domain.model.Center;
import @packageName@.infrastructure.persistence.entity.CenterEntity;
import @packageName@.infrastructure.persistence.mapper.CenterPersistenceMapper;
import @packageName@.infrastructure.persistence.repository.JpaCenterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa el puerto {@link CenterRepository} con Spring Data + JPA.
 */
@Component
public class CenterRepositoryAdapter implements CenterRepository {

    private final JpaCenterRepository jpaCenterRepository;
    private final CenterPersistenceMapper mapper;

    public CenterRepositoryAdapter(JpaCenterRepository jpaCenterRepository, CenterPersistenceMapper mapper) {
        this.jpaCenterRepository = jpaCenterRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Center> findAll() {
        return jpaCenterRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Center> findById(Long id) {
        return jpaCenterRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Center save(Center center) {
        CenterEntity saved = jpaCenterRepository.save(mapper.toEntity(center));
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaCenterRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaCenterRepository.deleteById(id);
    }
}
