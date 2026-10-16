package @packageName@.employee.service;

import @packageName@.employee.entity.EmployeeEntity;
import @packageName@.employee.exception.EmployeeException;
import @packageName@.employee.mapper.EmployeeMapper;
import @packageName@.employee.repository.EmployeeRepository;
import org.openapitools.model.Empleado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Transactional
@Service
public class EmployeeService {

    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;

    public EmployeeService(EmployeeRepository repository, EmployeeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Empleado> findAll() {
        return repository.findAll().stream().map(mapper::toModel).toList();
    }

    @Transactional(readOnly = true)
    public Empleado findById(Integer idEmpleado) {
        return repository.findById(idEmpleado)
                .map(mapper::toModel)
                .orElseThrow(() -> new EmployeeException("Empleado " + idEmpleado + " no encontrado"));
    }

    public Empleado create(Empleado empleado) {
        return mapper.toModel(repository.save(mapper.toEntity(empleado)));
    }

    public Empleado update(Integer idEmpleado, Empleado empleado) {
        EmployeeEntity existing = repository.findById(idEmpleado)
                .orElseThrow(() -> new EmployeeException("Empleado " + idEmpleado + " no encontrado"));
        EmployeeEntity updated = mapper.toEntity(empleado);
        updated.setIdEmpleado(existing.getIdEmpleado());
        return mapper.toModel(repository.save(updated));
    }

    public void delete(Integer idEmpleado) {
        if (!repository.existsById(idEmpleado)) {
            throw new EmployeeException("Empleado " + idEmpleado + " no encontrado");
        }
        repository.deleteById(idEmpleado);
    }
}
