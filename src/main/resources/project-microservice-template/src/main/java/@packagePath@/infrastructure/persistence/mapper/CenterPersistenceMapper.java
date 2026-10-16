package @packageName@.infrastructure.persistence.mapper;

import @packageName@.domain.model.Center;
import @packageName@.infrastructure.persistence.entity.CenterEntity;
import org.mapstruct.Mapper;

/**
 * Traduce entre la entidad JPA y el modelo de dominio de centros.
 */
@Mapper(componentModel = "spring")
public interface CenterPersistenceMapper {

    Center toDomain(CenterEntity entity);

    CenterEntity toEntity(Center center);
}
