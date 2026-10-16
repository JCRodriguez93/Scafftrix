package es.scafftrix.factory.config;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupRecoveryConfigTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final StartupRecoveryConfig config = new StartupRecoveryConfig();

    @BeforeEach
    void setUp() {
        TransactionStatus status = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(status);
    }

    private Project generatingProject() {
        Project project = new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, ProjectType.SPRING_BOOT.technologyStack(),
                "4.1.1", ConfigFormat.PROPERTIES);
        project.markGenerating();
        return project;
    }

    @Test
    void marksInterruptedGenerationsAsFailed() throws Exception {
        Project stuck = generatingProject();
        when(repository.findAllGenerating()).thenReturn(List.of(stuck));
        CommandLineRunner runner = config.recoveryRunner(repository, transactionManager);

        runner.run();

        assertThat(stuck.getStatus()).isEqualTo(ProjectStatus.FAILED);
        assertThat(stuck.getFailureReason()).contains("reinicio");
        verify(repository).save(stuck);
        verify(transactionManager).commit(any());
    }

    @Test
    void doesNothingWhenNoProjectWasLeftGenerating() throws Exception {
        when(repository.findAllGenerating()).thenReturn(List.of());
        CommandLineRunner runner = config.recoveryRunner(repository, transactionManager);

        runner.run();

        verify(repository, never()).save(any(Project.class));
    }
}
