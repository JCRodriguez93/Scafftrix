package es.scafftrix.factory.service;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.exception.GenerationInProgressException;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.naming.NamingService;
import es.scafftrix.factory.repository.ProjectRepository;
import es.scafftrix.factory.security.CurrentUserProvider;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresion sobre el disparador de generacion.
 *
 * <p>El fallo que cubre: {@code ProjectGenerationService#requestGeneration} reclama la
 * ranura con {@code tryRegister} y a continuacion llama a
 * {@code ProjectService#beginGeneration}. Si este ultimo volviera a consultar el
 * registro, encontraria su propia reclamacion y rechazaria la peticion con 409, dejando
 * la generacion permanentemente imposible de arrancar. Ningun test anterior lo cubria
 * porque todos exercisedan por debajo del borde web.
 */
class ProjectServiceBeginGenerationTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final NamingService namingService = mock(NamingService.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final GenerationJobRegistry jobRegistry = new GenerationJobRegistry(mock(GenerationLogWriter.class));

    private final ProjectService projectService = new ProjectService(
            repository, new ProjectMapper(), namingService, currentUserProvider, new ScafftrixProperties(),
            mock(WorkspaceManager.class));

    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
    }

    @Test
    void acceptsGenerationWhenTheCallerAlreadyHoldsTheRegistryClaim() {
        assertEquals(true, jobRegistry.tryRegister(projectId));

        assertDoesNotThrow(() -> projectService.beginGeneration(projectId));

        assertEquals(ProjectStatus.GENERATING, project.getStatus());
        verify(repository).save(project);
    }

    @Test
    void rejectsGenerationWhenPersistedStatusIsGenerating() {
        project.markGenerating();

        assertThrows(GenerationInProgressException.class, () -> projectService.beginGeneration(projectId));

        verify(repository, never()).save(project);
    }

    @Test
    void replacesThePreviousArtifactWhenRegeneratingAFailedProject() {
        project.markFailed("Fallo anterior");

        projectService.beginGeneration(projectId);

        assertEquals(ProjectStatus.GENERATING, project.getStatus());
        assertEquals(null, project.getFailureReason());
        assertEquals(false, project.isArtifactAvailable());
    }
}