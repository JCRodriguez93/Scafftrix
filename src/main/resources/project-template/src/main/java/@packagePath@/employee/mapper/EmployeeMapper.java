package @packageName@.employee.mapper;

import @packageName@.employee.entity.EmployeeEntity;
import org.mapstruct.Mapper;
import org.openapitools.model.Empleado;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    Empleado toModel(EmployeeEntity entity);

    EmployeeEntity toEntity(Empleado model);
}
