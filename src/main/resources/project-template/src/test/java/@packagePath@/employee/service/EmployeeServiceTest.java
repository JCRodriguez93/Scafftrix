package @packageName@.employee.service;

import @packageName@.employee.entity.EmployeeEntity;
import @packageName@.employee.exception.EmployeeException;
import @packageName@.employee.mapper.EmployeeMapper;
import @packageName@.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.Empleado;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private EmployeeMapper mapper;

    @InjectMocks
    private EmployeeService service;

    @Test
    void findAllReturnsEveryEmployeeAsModel() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setIdEmpleado(1);
        Empleado model = new Empleado().idEmpleado(1);

        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toModel(entity)).thenReturn(model);

        assertThat(service.findAll()).containsExactly(model);
    }

    @Test
    void findByIdThrowsWhenTheEmployeeDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99)).isInstanceOf(EmployeeException.class);
    }

    @Test
    void createSavesTheMappedEntityAndReturnsTheModel() {
        Empleado model = new Empleado().idEmpleado(7).nombre("Ana Ruiz");
        EmployeeEntity entity = new EmployeeEntity();
        entity.setIdEmpleado(7);
        Empleado saved = new Empleado().idEmpleado(7).nombre("Ana Ruiz");

        when(mapper.toEntity(any(Empleado.class))).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toModel(entity)).thenReturn(saved);

        assertThat(service.create(model)).isEqualTo(saved);
    }
}
