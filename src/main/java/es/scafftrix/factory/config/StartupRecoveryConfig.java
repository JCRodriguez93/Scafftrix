package es.scafftrix.factory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.repository.ProjectRepository;

/**
 * Runs at startup to make the system consistent after an unexpected restart.
 *
 * <p>If the application was killed mid-generation, projects left in {@code GENERATING}
 * are moved to {@code FAILED} with a clear explanation and the workspace for that
 * project is reset, preventing the UI from showing a "generation in progress" that will
 * never finish.
 */
@Configuration
public class StartupRecoveryConfig {

    private static final Logger LOG = LoggerFactory.getLogger(StartupRecoveryConfig.class);

    /**
     * Wraps the recovery in an explicit transaction. Using a {@link TransactionTemplate}
     * instead of a {@code @Transactional} method avoids the self-invocation that would bypass
     * the proxy (and therefore the transaction) when the runner calls back into this class.
     */
    @Bean
    public CommandLineRunner recoveryRunner(ProjectRepository repository,
                                            PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return args -> transactionTemplate.executeWithoutResult(status -> recover(repository));
    }

    private void recover(ProjectRepository repository) {
        var stuck = repository.findAllGenerating();
        for (Project project : stuck) {
            project.markFailed("Generación interrumpida por un reinicio de la aplicación. Vuelva a generar el proyecto.");
            repository.save(project);
        }
        if (!stuck.isEmpty()) {
            LOG.warn("Recovered {} project(s) left in {} state", stuck.size(), ProjectStatus.GENERATING);
        }
    }
}