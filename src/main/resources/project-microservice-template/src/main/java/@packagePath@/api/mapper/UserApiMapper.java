package @packageName@.api.mapper;

import @packageName@.api.model.User;
import org.mapstruct.Mapper;

/**
 * Traduce entre el modelo generado por OpenAPI y el modelo de dominio.
 *
 * <p>Vive en la capa api a proposito: es la frontera la que conoce ambos mundos, ni el
 * dominio conoce OpenAPI ni el contrato conoce el dominio.
 */
@Mapper(componentModel = "spring")
public interface UserApiMapper {

    User toApi(@packageName@.domain.model.User user);

    @packageName@.domain.model.User toDomain(User user);
}
