package @packageName@.config;

import @packageName@.listener.JobCompletionNotificationListener;
import @packageName@.model.Employee;
import @packageName@.processor.EmployeeItemProcessor;
import javax.sql.DataSource;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public FlatFileItemReader<Employee> reader() {
        return new FlatFileItemReaderBuilder<Employee>()
                .name("employeeItemReader")
                .resource(new ClassPathResource("sample-data.csv"))
                .delimited()
                .names("id", "firstName", "lastName", "email", "department", "salary")
                .targetType(Employee.class)
                .build();
    }

    @Bean
    public EmployeeItemProcessor processor() {
        return new EmployeeItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Employee> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Employee>()
                .sql("INSERT INTO employees (id, first_name, last_name, email, department, salary) " +
                     "VALUES (:id, :firstName, :lastName, :email, :department, :salary)")
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, 
                      PlatformTransactionManager transactionManager,
                      FlatFileItemReader<Employee> reader,
                      EmployeeItemProcessor processor,
                      JdbcBatchItemWriter<Employee> writer) {
        return new StepBuilder("step1", jobRepository)
                .<Employee, Employee>chunk(3)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skip(FlatFileParseException.class) // Omitir registros del CSV con errores de parseo
                .skipLimit(5)
                .build();
    }

    @Bean
    public Job importUserJob(JobRepository jobRepository,
                             Step step1,
                             JobCompletionNotificationListener listener) {
        return new JobBuilder("importEmployeeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1)
                .end()
                .build();
    }
}