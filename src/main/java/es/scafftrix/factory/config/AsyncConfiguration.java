package es.scafftrix.factory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread pool configuration for asynchronous generation jobs.
 *
 * <p>Generating a project can take several minutes, so the task is submitted to a
 * dedicated pool rather than the default Spring MVC pool. The configuration is bound to
 * {@code scafftrix.generation.concurrency} so it can be tuned without a code change.
 */
@Configuration
public class AsyncConfiguration {

    private final ScafftrixProperties properties;

    public AsyncConfiguration(ScafftrixProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "generationExecutor")
    public Executor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int concurrency = Math.max(1, properties.getGeneration().getConcurrency());
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("factory-gen-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "asyncTaskExecutor")
    public TaskExecutor asyncTaskExecutor() {
        return (ThreadPoolTaskExecutor) generationExecutor();
    }
}