package @packageName@.infrastructure.persistence.mapper;

import @packageName@.domain.model.User;
import @packageName@.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

/**
 * Traduce entre la entidad JPA y el modelo de dominio. MapStruct genera la implementacion.
 */
@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User user);
}
