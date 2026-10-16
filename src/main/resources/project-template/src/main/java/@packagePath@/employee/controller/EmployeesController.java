package @packageName@.employee.controller;

import @packageName@.employee.service.EmployeeService;
import org.openapitools.api.EmpleadosApi;
import org.openapitools.model.Empleado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EmployeesController implements EmpleadosApi {

    private final EmployeeService service;

    public EmployeesController(EmployeeService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<Empleado>> getAllEmpleados() {
        return ResponseEntity.ok(service.findAll());
    }

    @Override
    public ResponseEntity<Empleado> getEmpleado(Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Override
    public ResponseEntity<Empleado> createEmpleado(Empleado empleado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(empleado));
    }

    @Override
    public ResponseEntity<Empleado> updateEmpleado(Integer id, Empleado empleado) {
        return ResponseEntity.ok(service.update(id, empleado));
    }

    @Override
    public ResponseEntity<Void> deleteEmpleado(Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
