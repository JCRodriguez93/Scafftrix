package @packageName@.center.mapper;

import @packageName@.center.entity.CenterEntity;
import org.mapstruct.Mapper;
import org.openapitools.model.Centro;

@Mapper(componentModel = "spring")
public interface CenterMapper {

    Centro toModel(CenterEntity entity);

    CenterEntity toEntity(Centro model);
}
