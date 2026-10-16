package @packageName@.listener;

import @packageName@.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
    private final JdbcTemplate jdbcTemplate;

    public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINALIZADO. Verificando resultados en BD:");

            jdbcTemplate.query("SELECT id, first_name, last_name, salary FROM employees",
                    (rs, rowNum) -> new Employee(
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            null, null,
                            rs.getDouble("salary")
                    )
            ).forEach(emp -> log.info("Insertado en BD: {} {}, Salario: {}", emp.getFirstName(), emp.getLastName(), emp.getSalary()));
        }
    }
}