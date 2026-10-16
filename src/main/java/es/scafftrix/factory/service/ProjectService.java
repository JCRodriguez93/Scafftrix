package es.scafftrix.factory.service;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.GenerationProfile;
import es.scafftrix.factory.dto.ProjectCreationRequest;
import es.scafftrix.factory.dto.ProjectSearchCriteria;
import es.scafftrix.factory.dto.ProjectUpdateRequest;
import es.scafftrix.factory.exception.GenerationInProgressException;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.exception.ProjectNotFoundException;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.naming.NamingService;
import es.scafftrix.factory.repository.ProjectRepository;
import es.scafftrix.factory.repository.ProjectSpecifications;
import es.scafftrix.factory.security.CurrentUserProvider;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * Use cases around the project aggregate: creation, history, detail and the
 * generation trigger's preconditions.
 *
 * <p>This class owns the naming rules and the pinned technology stack. It does not run
 * the generator; that is {@link ProjectGenerationService}'s job.
 */
@Service
@Transactional
public class ProjectService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectService.class);

    private static final String PROPERTY_CREATED_AT = "createdAt";

    private final ProjectRepository repository;
    private final ProjectMapper mapper;
    private final NamingService namingService;
    private final CurrentUserProvider currentUserProvider;
    private final ScafftrixProperties properties;
    private final WorkspaceManager workspaceManager;

    public ProjectService(ProjectRepository repository,
                          ProjectMapper mapper,
                          NamingService namingService,
                          CurrentUserProvider currentUserProvider,
                          ScafftrixProperties properties,
                          WorkspaceManager workspaceManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.namingService = namingService;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
        this.workspaceManager = workspaceManager;
    }

    /**
     * Creates a project from the form, deriving any technical identifier the user left
     * blank and applying the pinned technology stack.
     */
    public Project create(ProjectCreationRequest request) {
        // Everything below is validated/derived by NamingService; nothing unvalidated
        // is ever persisted.
        String name = request.getName().trim();
        String groupId = namingService.validateGroupId(request.getGroupId());
        String version = namingService.validateVersion(request.getVersion());

        String baseName = isBlank(request.getBaseName())
                ? namingService.deriveBaseName(name)
                : namingService.validateBaseName(request.getBaseName().trim());

        String artifactId = isBlank(request.getArtifactId())
                ? namingService.deriveArtifactId(name)
                : namingService.validateArtifactId(request.getArtifactId().trim());

        String packageName = isBlank(request.getPackageName())
                ? namingService.derivePackageName(groupId, baseName)
                : namingService.validatePackageName(request.getPackageName().trim());

        ProjectType projectType = request.getProjectType();
        if (projectType == null) {
            throw new InvalidConfigurationException(
                    "Debe seleccionar el tipo de proyecto.",
                    "Project type was null in creation request for name=" + name);
        }

        String description = isBlank(request.getDescription()) ? null : request.getDescription().trim();

        TechnologyStack stack = projectType.technologyStack();
        String springBootVersion = resolveSpringBootVersion(request.getSpringBootVersion());
        ConfigFormat configFormat = request.getConfigFormat() == null
                ? projectType.defaultConfigFormat()
                : request.getConfigFormat();

        Project project = new Project(name,
                new ProjectCoordinates(baseName, groupId, artifactId, packageName, version),
                description, projectType, stack, springBootVersion, configFormat);

        CurrentUserProvider.CurrentUser user = currentUserProvider.currentUser();
        Project saved = repository.save(project);
        LOG.info("Project {} created by '{}': type={} coordinates={}:{} package={} boot={} format={}",
                saved.getId(), user.username(), saved.getProjectType(),
                saved.getGroupId(), saved.getArtifactId(), saved.getPackageName(),
                saved.getSpringBootVersion(), saved.getConfigFormat());
        return saved;
    }

    /**
     * Applies the user editable metadata of a project.
     *
     * <p>The project must exist and must not be mid-generation: editing while a job is
     * running would race with the worker that saves the final status, and the user would
     * not know which values survived. The name and version are normalised the same way as
     * on creation ({@link NamingService}); the version is re-validated so a hand-crafted
     * request cannot store a value the factory would never mint.
     *
     * @throws ProjectNotFoundException      if no project has that id
     * @throws GenerationInProgressException if a generation is currently running
     */
    public Project update(UUID id, ProjectUpdateRequest request) {
        Project project = load(id);
        requireNotGenerating(project);
        String name = request.getName().trim();
        String version = namingService.validateVersion(request.getVersion());
        String description = isBlank(request.getDescription()) ? null : request.getDescription().trim();
        project.updateMetadata(name, version, description);
        Project saved = repository.save(project);
        LOG.info("Project {} updated by '{}': name='{}' version={}",
                saved.getId(), currentUserProvider.currentUser().username(), name, version);
        return saved;
    }

    /**
     * Deletes a project: both its database record and the files it generated.
     *
     * <p>The persisted row is removed inside the transaction; the workspace folder is
     * removed only <strong>after</strong> the commit. Doing it the other way round would
     * leave a surviving record pointing at nothing if the commit failed, and a rollback
     * would already have destroyed files the record still references. If the folder cannot
     * be removed, {@link WorkspaceManager} logs it and the operation still succeeds: an
     * orphan folder is recoverable, a half-deleted resource is not.
     *
     * @throws ProjectNotFoundException      if no project has that id
     * @throws GenerationInProgressException if a generation is currently running
     */
    public void delete(UUID id) {
        Project project = load(id);
        requireNotGenerating(project);
        repository.delete(project);
        deleteWorkspaceAfterCommit(id);
        LOG.info("Project {} deleted by '{}'", id, currentUserProvider.currentUser().username());
    }

    /** Rejects metadata changes while a generation job owns the project. */
    private void requireNotGenerating(Project project) {
        if (project.getStatus().isTerminalForGeneration()) {
            throw new GenerationInProgressException(project.getId());
        }
    }

    /**
     * Schedules the workspace removal for after the transaction commits, or performs it
     * immediately when no transaction is active (direct service calls and unit tests).
     */
    private void deleteWorkspaceAfterCommit(UUID id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workspaceManager.deleteProject(id);
                }
            });
        } else {
            workspaceManager.deleteProject(id);
        }
    }

    /**
     * Builds the generator specification for an existing project.
     *
     * <p>Only the values stored on the project are carried, so the specification is the
     * same object the generator receives and the form cannot influence it.
     */
    public GenerationProfile toGenerationProfile(Project project) {
        return new GenerationProfile(
                project.getProjectType(),
                project.getName(),
                project.getBaseName(),
                project.getGroupId(),
                project.getArtifactId(),
                project.getPackageName(),
                project.getVersion(),
                project.getSpringBootVersion(),
                project.getConfigFormat(),
                project.getDescription());
    }

    @Transactional(readOnly = true)
    public Project getEntity(UUID id) {
        return load(id);
    }

    private Project load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Project> search(ProjectSearchCriteria criteria) {
        criteria = criteria == null ? ProjectSearchCriteria.empty() : criteria.normalized();
        Pageable pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                resolveSort(criteria));
        return repository.findAll(ProjectSpecifications.filter(criteria.search(), criteria.type(), criteria.status()), pageable);
    }

    /**
     * Constrains ordering to an allow-list.
     *
     * <p>The raw {@code sort} parameter never reaches {@link Sort} directly; an
     * unrecognised value falls back to newest first rather than being rejected, so a
     * hand-edited URL cannot inject a property name.
     */
    private Sort resolveSort(ProjectSearchCriteria criteria) {
        if (criteria.isSort(ProjectSearchCriteria.SORT_OLDEST)) {
            return Sort.by(Sort.Direction.ASC, PROPERTY_CREATED_AT);
        }
        if (criteria.isSort(ProjectSearchCriteria.SORT_NAME_ASC)) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        if (criteria.isSort(ProjectSearchCriteria.SORT_STATUS)) {
            return Sort.by(Sort.Direction.ASC, "status").and(Sort.by(Sort.Direction.DESC, PROPERTY_CREATED_AT));
        }
        return Sort.by(Sort.Direction.DESC, PROPERTY_CREATED_AT);
    }

    /**
     * Marks a project as generating after checking the persisted status allows it.
     *
     * <p>This method deliberately does <strong>not</strong> consult
     * {@link GenerationJobRegistry}. The caller
     * ({@code ProjectGenerationService#requestGeneration}) claims the slot with
     * {@code tryRegister} <em>before</em> calling in, so asking the registry here would
     * always find the project "already running" by the caller's own hand and reject every
     * request with 409. The registry claim is the concurrency guard; this method owns the
     * state-machine guard.
     *
     * @throws GenerationInProgressException if the persisted status forbids a new job
     */
    public void beginGeneration(UUID projectId) {
        Project project = load(projectId);
        if (project.getStatus().isTerminalForGeneration()) {
            throw new GenerationInProgressException(projectId);
        }
        if (!project.getStatus().isGenerationAllowed()) {
            LOG.info("Regeneration requested for project {} in status {}; allowed (artifact will be replaced)",
                    projectId, project.getStatus());
        }
        project.markGenerating();
        project.clearArtifact();
        repository.save(project);
    }

    public void markGenerated(Project project, String artifactPath) {
        Project managed = load(project.getId());
        managed.markGenerated(artifactPath);
        repository.save(managed);
    }

    public void markFailed(Project project, String reason) {
        Project managed = load(project.getId());
        managed.markFailed(reason);
        repository.save(managed);
    }

    public void markDownloaded(UUID projectId) {
        Project project = load(projectId);
        project.markDownloaded();
        repository.save(project);
    }

    /** The pinned corporate stack of a profile, read from the type manifest and never from the request. */
    public TechnologyStack technologyStack(ProjectType projectType) {
        return projectType.technologyStack();
    }

    /** The Spring Boot versions the portal offers, from configuration. */
    @Transactional(readOnly = true)
    public List<String> offeredSpringBootVersions() {
        return properties.getVersions().getSpringBoot();
    }

    /** Spring Boot version preselected in the form, from configuration. */
    @Transactional(readOnly = true)
    public String defaultSpringBootVersion() {
        return properties.getDefaults().getSpringBootVersion();
    }

    /** Default Group ID prefilled in the form, from configuration. */
    @Transactional(readOnly = true)
    public String defaultGroupId() {
        return properties.getDefaults().getGroupId();
    }

    /** Default project version prefilled in the form, from configuration. */
    @Transactional(readOnly = true)
    public String defaultVersion() {
        return properties.getDefaults().getVersion();
    }

    /**
     * Resolves the Spring Boot version: the configured default when the user left it blank,
     * otherwise one of the offered versions. Arbitrary versions are rejected so the portal
     * never mints a pom.xml the factory does not stand behind.
     */
    private String resolveSpringBootVersion(String requested) {
        ScafftrixProperties.Versions versions = properties.getVersions();
        String defaultVersion = properties.getDefaults().getSpringBootVersion();
        if (isBlank(requested)) {
            return defaultVersion;
        }
        String normalized = requested.trim();
        if (!versions.offers(normalized)) {
            throw new InvalidConfigurationException(
                    "La versión de Spring Boot '" + normalized + "' no es una de las versiones ofrecidas.",
                    "Requested Spring Boot version " + normalized
                            + " is not in the offered list " + versions.getSpringBoot());
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
