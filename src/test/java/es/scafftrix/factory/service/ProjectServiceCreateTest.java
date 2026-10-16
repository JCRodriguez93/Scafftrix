package es.scafftrix.factory.service;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.GenerationProfile;
import es.scafftrix.factory.dto.ProjectCreationRequest;
import es.scafftrix.factory.dto.ProjectSearchCriteria;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.exception.ProjectNotFoundException;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.naming.NamingService;
import es.scafftrix.factory.repository.ProjectRepository;
import es.scafftrix.factory.security.CurrentUserProvider;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceCreateTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
    private final ScafftrixProperties properties = new ScafftrixProperties();

    private final ProjectService service = new ProjectService(
            repository, new ProjectMapper(), new NamingService(), currentUserProvider, properties, workspaceManager);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser())
                .thenReturn(new CurrentUserProvider.CurrentUser("developer", "Developer"));
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ProjectCreationRequest request() {
        ProjectCreationRequest request = new ProjectCreationRequest();
        request.setName("Customer Management");
        request.setProjectType(ProjectType.SPRING_BOOT);
        request.setGroupId("es.scafftrix");
        request.setVersion("1.0.0-SNAPSHOT");
        return request;
    }

    @Test
    void derivesAllTechnicalIdentifiersWhenLeftBlank() {
        Project project = service.create(request());

        assertThat(project.getBaseName()).isEqualTo("customerManagement");
        assertThat(project.getArtifactId()).isEqualTo("customer-management");
        assertThat(project.getPackageName()).isEqualTo("es.scafftrix.customermanagement");
        assertThat(project.getGroupId()).isEqualTo("es.scafftrix");
        assertThat(project.getVersion()).isEqualTo("1.0.0-SNAPSHOT");
    }

    @Test
    void usesProvidedIdentifiersWhenPresent() {
        ProjectCreationRequest request = request();
        request.setBaseName("billingApp");
        request.setArtifactId("billing-app");
        request.setPackageName("com.acme.billing");
        request.setGroupId("com.acme");
        request.setVersion("2.0.0");

        Project project = service.create(request);

        assertThat(project.getBaseName()).isEqualTo("billingApp");
        assertThat(project.getArtifactId()).isEqualTo("billing-app");
        assertThat(project.getPackageName()).isEqualTo("com.acme.billing");
        assertThat(project.getGroupId()).isEqualTo("com.acme");
        assertThat(project.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void appliesThePinnedStackOfTheSelectedType() {
        Project project = service.create(request());

        assertThat(project.getProjectType()).isEqualTo(ProjectType.SPRING_BOOT);
        assertThat(project.getDatabaseType()).isEqualTo("H2");
        assertThat(project.getFrontend()).isEqualTo("Thymeleaf");
        assertThat(project.getBuildTool()).isEqualTo("Maven");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CREATED);
    }

    @Test
    void defaultsTheSpringBootVersionAndConfigFormatWhenBlank() {
        Project project = service.create(request());

        assertThat(project.getSpringBootVersion()).isEqualTo("4.1.1");
        assertThat(project.getConfigFormat()).isEqualTo(ConfigFormat.PROPERTIES);
    }

    @Test
    void acceptsAnOfferedSpringBootVersionAndConfigFormat() {
        ProjectCreationRequest request = request();
        request.setSpringBootVersion("4.0.8");
        request.setConfigFormat(ConfigFormat.YAML);

        Project project = service.create(request);

        assertThat(project.getSpringBootVersion()).isEqualTo("4.0.8");
        assertThat(project.getConfigFormat()).isEqualTo(ConfigFormat.YAML);
    }

    @Test
    void rejectsASpringBootVersionThatIsNotOffered() {
        ProjectCreationRequest request = request();
        request.setSpringBootVersion("3.4.5");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsAMissingProjectType() {
        ProjectCreationRequest request = request();
        request.setProjectType(null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void blankDescriptionBecomesNullAndTextIsTrimmed() {
        ProjectCreationRequest blank = request();
        blank.setDescription("   ");
        assertThat(service.create(blank).getDescription()).isNull();

        ProjectCreationRequest filled = request();
        filled.setDescription("  Gestión de clientes  ");
        assertThat(service.create(filled).getDescription()).isEqualTo("Gestión de clientes");
    }

    @Test
    void searchWithNullCriteriaUsesDefaults() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.search(null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        assertThat(captor.getValue().getPageSize()).isEqualTo(ProjectSearchCriteria.DEFAULT_SIZE);
    }

    @Test
    void searchResolvesEveryAllowedSort() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.search(new ProjectSearchCriteria(null, null, null, ProjectSearchCriteria.SORT_OLDEST, 1, 5));
        service.search(new ProjectSearchCriteria(null, null, null, ProjectSearchCriteria.SORT_NAME_ASC, 1, 5));
        service.search(new ProjectSearchCriteria(null, null, null, ProjectSearchCriteria.SORT_STATUS, 1, 5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(3)).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getAllValues().get(0).getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
        assertThat(captor.getAllValues().get(1).getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
        assertThat(captor.getAllValues().get(2).getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "status").and(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void buildsAGenerationProfileFromStoredValues() {
        Project project = service.create(request());

        GenerationProfile profile = service.toGenerationProfile(project);

        assertThat(profile.projectType()).isEqualTo(ProjectType.SPRING_BOOT);
        assertThat(profile.groupId()).isEqualTo("es.scafftrix");
        assertThat(profile.artifactId()).isEqualTo("customer-management");
        assertThat(profile.packageName()).isEqualTo("es.scafftrix.customermanagement");
        assertThat(profile.version()).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(profile.springBootVersion()).isEqualTo("4.1.1");
        assertThat(profile.configFormat()).isEqualTo(ConfigFormat.PROPERTIES);
    }

    @Test
    void getEntityLoadsTheManagedProjectOrFails() {
        UUID id = UUID.randomUUID();
        Project project = service.create(request());
        when(repository.findById(id)).thenReturn(Optional.of(project));
        assertThat(service.getEntity(id)).isSameAs(project);

        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getEntity(missing))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void regenerationOfAGeneratedProjectLogsAndResetsArtifact() {
        UUID id = UUID.randomUUID();
        Project project = service.create(request());
        project.markGenerated("some/artifact.zip");
        when(repository.findById(id)).thenReturn(Optional.of(project));

        service.beginGeneration(id);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.GENERATING);
        assertThat(project.getArtifactPath()).isNull();
    }

    @Test
    void markGeneratedFailAndDownloadedDriveTheStateMachine() {
        UUID id = UUID.randomUUID();
        Project project = service.create(request());
        assignId(project, id);
        when(repository.findById(id)).thenReturn(Optional.of(project));

        service.markGenerated(project, "x/artifact.zip");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.GENERATED);
        assertThat(project.isArtifactAvailable()).isTrue();

        service.markFailed(project, "boom");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.FAILED);
        assertThat(project.getFailureReason()).isEqualTo("boom");

        service.markDownloaded(id);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DOWNLOADED);
    }

    private static void assignId(Project project, UUID id) {
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void exposesPinnedStackAndConfiguredDefaults() {
        assertThat(service.technologyStack(ProjectType.SPRING_BATCH))
                .isEqualTo(new TechnologyStack("H2", "", "Maven"));
        assertThat(service.offeredSpringBootVersions()).contains("4.1.1");
        assertThat(service.defaultSpringBootVersion()).isEqualTo("4.1.1");
        assertThat(service.defaultGroupId()).isEqualTo("es.scafftrix");
        assertThat(service.defaultVersion()).isEqualTo("1.0.0-SNAPSHOT");
    }
}
