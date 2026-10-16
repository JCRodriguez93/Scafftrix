package es.scafftrix.factory.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigurationTest {

    @Test
    void sizesThePoolFromTheConfiguredConcurrency() {
        ScafftrixProperties properties = new ScafftrixProperties();
        properties.getGeneration().setConcurrency(3);
        AsyncConfiguration configuration = new AsyncConfiguration(properties);

        Executor executor = configuration.generationExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPool.getCorePoolSize()).isEqualTo(3);
        assertThat(threadPool.getMaxPoolSize()).isEqualTo(3);
        threadPool.shutdown();
    }

    @Test
    void neverConfiguresFewerThanOneThread() {
        ScafftrixProperties properties = new ScafftrixProperties();
        properties.getGeneration().setConcurrency(0);
        AsyncConfiguration configuration = new AsyncConfiguration(properties);

        ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) configuration.generationExecutor();

        assertThat(threadPool.getCorePoolSize()).isEqualTo(1);
        threadPool.shutdown();
    }

    @Test
    void exposesAnAsyncTaskExecutor() {
        AsyncConfiguration configuration = new AsyncConfiguration(new ScafftrixProperties());

        TaskExecutor taskExecutor = configuration.asyncTaskExecutor();

        assertThat(taskExecutor).isNotNull();
        ((ThreadPoolTaskExecutor) taskExecutor).shutdown();
    }
}
