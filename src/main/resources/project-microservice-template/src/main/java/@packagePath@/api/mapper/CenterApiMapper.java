package @packageName@.api.mapper;

import @packageName@.api.model.Center;
import org.mapstruct.Mapper;

/**
 * Traduce entre el modelo generado por OpenAPI y el modelo de dominio de centros.
 */
@Mapper(componentModel = "spring")
public interface CenterApiMapper {

    Center toApi(@packageName@.domain.model.Center center);

    @packageName@.domain.model.Center toDomain(Center center);
}
