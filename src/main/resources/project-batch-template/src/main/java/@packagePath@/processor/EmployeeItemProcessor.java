package @packageName@.processor;

import @packageName@.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class EmployeeItemProcessor implements ItemProcessor<Employee, Employee> {

    private static final Logger log = LoggerFactory.getLogger(EmployeeItemProcessor.class);

    @Override
    public Employee process(Employee employee) throws Exception {
        // Filtrar registros con salario inválido (descarte en el processor)
        if (employee.getSalary() == null || employee.getSalary() <= 0) {
            log.warn("Registro descartado por salario inválido: ID {}", employee.getId());
            return null;
        }

        // Normalización de datos (ejemplo: nombres en mayúsculas)
        String upperFirstName = employee.getFirstName().toUpperCase();
        String upperLastName = employee.getLastName().toUpperCase();

        return new Employee(
                employee.getId(),
                upperFirstName,
                upperLastName,
                employee.getEmail().toLowerCase(),
                employee.getDepartment(),
                employee.getSalary()
        );
    }
}