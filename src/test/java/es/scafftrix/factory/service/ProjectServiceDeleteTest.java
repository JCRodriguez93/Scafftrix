package es.scafftrix.factory.service;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.exception.GenerationInProgressException;
import es.scafftrix.factory.exception.ProjectNotFoundException;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.naming.NamingService;
import es.scafftrix.factory.repository.ProjectRepository;
import es.scafftrix.factory.security.CurrentUserProvider;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceDeleteTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);

    private final ProjectService service = new ProjectService(
            repository, new ProjectMapper(), new NamingService(), currentUserProvider,
            new ScafftrixProperties(), workspaceManager);

    private UUID id;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser())
                .thenReturn(new CurrentUserProvider.CurrentUser("developer", "Developer"));
        id = UUID.randomUUID();
        Project project = new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
        when(repository.findById(id)).thenReturn(Optional.of(project));
    }

    @Test
    void deletesRecordAndWorkspace() {
        service.delete(id);

        verify(repository).delete(anyProject());
        verify(workspaceManager).deleteProject(id);
    }

    @Test
    void removesWorkspaceOnlyAfterTheTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.delete(id);
            verify(workspaceManager, never()).deleteProject(id);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(workspaceManager).deleteProject(id);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectsUnknownProjectAndDoesNotTouchWorkspace() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(missing))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(workspaceManager, never()).deleteProject(missing);
    }

    @Test
    void rejectsDeleteWhileGenerating() {
        Project generating = new Project("Batch",
                new ProjectCoordinates("batch", "es.scafftrix", "batch", "es.scafftrix.batch", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BATCH,
                new TechnologyStack("H2", "", "Maven"), "4.1.1", ConfigFormat.PROPERTIES);
        generating.markGenerating();
        when(repository.findById(id)).thenReturn(Optional.of(generating));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(GenerationInProgressException.class);

        verify(repository, never()).delete(anyProject());
        verify(workspaceManager, never()).deleteProject(id);
    }

    private static Project anyProject() {
        return org.mockito.ArgumentMatchers.any(Project.class);
    }
}
