package es.scafftrix.factory.service;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.ProjectUpdateRequest;
import es.scafftrix.factory.exception.GenerationInProgressException;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.exception.ProjectNotFoundException;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.naming.NamingService;
import es.scafftrix.factory.repository.ProjectRepository;
import es.scafftrix.factory.security.CurrentUserProvider;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceUpdateTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);

    private final ProjectService service = new ProjectService(
            repository, new ProjectMapper(), new NamingService(), currentUserProvider,
            new ScafftrixProperties(), workspaceManager);

    private UUID id;
    private Project project;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser())
                .thenReturn(new CurrentUserProvider.CurrentUser("developer", "Developer"));
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        id = UUID.randomUUID();
        project = new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                "desc", ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
        when(repository.findById(id)).thenReturn(Optional.of(project));
    }

    private ProjectUpdateRequest request(String name, String version, String description) {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName(name);
        request.setVersion(version);
        request.setDescription(description);
        return request;
    }

    @Test
    void updatesEditableMetadataAndPersists() {
        Project saved = service.update(id, request("  Billing  ", "2.0.0", "  Nueva descripción  "));

        assertThat(saved.getName()).isEqualTo("Billing");
        assertThat(saved.getVersion()).isEqualTo("2.0.0");
        assertThat(saved.getDescription()).isEqualTo("Nueva descripción");
        verify(repository).save(project);
    }

    @Test
    void doesNotTouchTechnicalConfiguration() {
        service.update(id, request("Otro nombre", "2.0.0", null));

        assertThat(project.getProjectType()).isEqualTo(ProjectType.SPRING_BOOT);
        assertThat(project.getGroupId()).isEqualTo("es.scafftrix");
        assertThat(project.getArtifactId()).isEqualTo("customer-management");
        assertThat(project.getPackageName()).isEqualTo("es.scafftrix.customermanagement");
        assertThat(project.getBaseName()).isEqualTo("customerManagement");
        assertThat(project.getDatabaseType()).isEqualTo("H2");
    }

    @Test
    void blankDescriptionBecomesNull() {
        service.update(id, request("Billing", "1.0.0", "   "));

        assertThat(project.getDescription()).isNull();
    }

    @Test
    void rejectsInvalidVersion() {
        ProjectUpdateRequest invalid = request("Billing", "not a version", null);

        assertThatThrownBy(() -> service.update(id, invalid))
                .isInstanceOf(InvalidConfigurationException.class);

        verify(repository, never()).save(any(Project.class));
    }

    @Test
    void rejectsUnknownProject() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        ProjectUpdateRequest valid = request("Billing", "1.0.0", null);

        assertThatThrownBy(() -> service.update(missing, valid))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void rejectsUpdateWhileGenerating() {
        project.markGenerating();
        ProjectUpdateRequest valid = request("Billing", "1.0.0", null);

        assertThatThrownBy(() -> service.update(id, valid))
                .isInstanceOf(GenerationInProgressException.class);

        verify(repository, never()).save(any(Project.class));
    }
}
