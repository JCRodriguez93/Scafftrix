package es.scafftrix.factory.service;

import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.GenerationLogEntry;
import es.scafftrix.factory.dto.GenerationProfile;
import es.scafftrix.factory.dto.GenerationStatusDto;
import es.scafftrix.factory.exception.GenerationInProgressException;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.exception.ScafftrixException;
import es.scafftrix.factory.generator.GenerationResult;
import es.scafftrix.factory.generator.ProjectGenerator;
import es.scafftrix.factory.workspace.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * Orchestrates a generation: workspace, generator, packaging, status.
 *
 * <p>Generation is <strong>asynchronous</strong>. The HTTP request that triggers it
 * returns as soon as the job is accepted, and the browser follows the progress over SSE.
 * This keeps the servlet thread free: copying and archiving a template legitimately takes
 * tens of seconds and would otherwise exhaust the request thread pool.
 *
 * <p>Status is written to the database at each transition, so a user who reloads the page,
 * or opens the project in another browser tab, always sees the truth.
 */
@Service
public class ProjectGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectGenerationService.class);

    private final ProjectService projectService;
    private final List<ProjectGenerator> generators;
    private final WorkspaceManager workspaceManager;
    private final GenerationJobRegistry jobRegistry;
    private final Executor generationExecutor;

    public ProjectGenerationService(ProjectService projectService,
                                    List<ProjectGenerator> generators,
                                    WorkspaceManager workspaceManager,
                                    GenerationJobRegistry jobRegistry,
                                    Executor generationExecutor) {
        this.projectService = projectService;
        this.generators = List.copyOf(generators);
        this.workspaceManager = workspaceManager;
        this.jobRegistry = jobRegistry;
        this.generationExecutor = generationExecutor;
    }

    /** Picks the generator that declares support for the requested profile. */
    private ProjectGenerator generatorFor(GenerationProfile profile) {
        return generators.stream()
                .filter(generator -> Stream.of(generator.appliesTo())
                        .anyMatch(type -> type == profile.projectType()))
                .findFirst()
                .orElseThrow(() -> new InvalidConfigurationException(
                        "No hay ningún generador disponible para el tipo de proyecto seleccionado.",
                        "No ProjectGenerator supports projectType=" + profile.projectType()));
    }

    /**
     * Accepts a generation request and schedules the work.
     *
     * @return a future that completes when the job finishes; used by tests and by the
     *         optional synchronous trigger
     */
    public CompletableFuture<Void> requestGeneration(UUID projectId) {
        if (!jobRegistry.tryRegister(projectId)) {
            throw new GenerationInProgressException(projectId);
        }
        // Claim persisted first so a concurrent reader immediately sees GENERATING.
        try {
            projectService.beginGeneration(projectId);
        } catch (RuntimeException e) {
            jobRegistry.release(projectId);
            throw e;
        }

        Project project = projectService.getEntity(projectId);
        GenerationProfile profile = projectService.toGenerationProfile(project);
        ProjectGenerator generator = generatorFor(profile);

        log(projectId, GenerationLogEntry.info("Proyecto '" + project.getName() + "' en cola de generación."));
        log(projectId, GenerationLogEntry.info("Perfil: " + profile.projectType().getDisplayName()
                + " | Generador: " + generator.name()));

        CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> execute(project, profile), generationExecutor)
                .whenComplete((ignored, error) -> {
                    jobRegistry.release(projectId);
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(null);
                    }
                });
        return future;
    }

    /** Runs the whole pipeline. Never throws: failures are recorded on the project. */
    private void execute(Project project, GenerationProfile profile) {
        UUID projectId = project.getId();
        Instant start = Instant.now();
        try {
            workspaceManager.ensureWorkspaceAvailable();
            workspaceManager.resetSourceDirectory(projectId);
            Path outputDirectory = workspaceManager.sourceDirectory(projectId);

            log(projectId, GenerationLogEntry.info("Creando el directorio de trabajo del proyecto."));
            TechnologyStack stack = project.getProjectType().technologyStack();
            log(projectId, GenerationLogEntry.info("Stack fijo: " + stack.database()
                    + (stack.frontend().isEmpty() ? "" : " | Frontend: " + stack.frontend())
                    + " | Build: " + stack.buildTool()));
            log(projectId, GenerationLogEntry.info("Spring Boot " + project.getSpringBootVersion()
                    + " | Formato de configuración: " + project.getConfigFormat().getDisplayName()));

            GenerationResult result = generatorFor(profile).generate(profile, outputDirectory, step -> {
                if (step != null && !step.isBlank()) {
                    log(projectId, GenerationLogEntry.info(step));
                }
            });

            Duration duration = result.duration();
            log(projectId, GenerationLogEntry.info("Comprimiendo el proyecto generado."));

            Path zip = workspaceManager.compress(projectId, profile.artifactId(), result.projectRoot());
            projectService.markGenerated(project, workspaceManager.relativePath(zip));

            log(projectId, GenerationLogEntry.success("Proyecto generado correctamente en "
                    + formatDuration(duration) + "."));
            log(projectId, GenerationLogEntry.success("Descargable como " + zip.getFileName() + "."));

        } catch (ScafftrixException e) {
            handleFailure(project, e.getUserMessage(), e.getTechnicalMessage());
        } catch (Exception e) {
            LOG.error("Unexpected failure while generating project {}", projectId, e);
            handleFailure(project,
                    "Se ha producido un error inesperado durante la generación del proyecto.",
                    "Unexpected exception: " + e);
        } finally {
            LOG.info("Generation finished for project {} after {}", projectId, Duration.between(start, Instant.now()));
        }
    }

    private void handleFailure(Project project, String userMessage, String technicalMessage) {
        try {
            projectService.markFailed(project, truncate(userMessage));
        } catch (RuntimeException e) {
            LOG.error("Could not persist FAILED status for project {}", project.getId(), e);
        }
        log(project.getId(), GenerationLogEntry.error(userMessage));
        logTechnical(project.getId(), technicalMessage == null ? "unknown technical failure" : technicalMessage);
    }

    /**
     * Current status plus the log, used by the detail page and by polling.
     *
     * <p>While a job is running the live in-memory buffer is authoritative (it is updated
     * before the line is flushed to disk). Once the job is no longer running the buffer
     * has been released, so the persisted log is used instead: this keeps the console
     * populated for a project opened after generation finished, and lets the final SSE
     * frame carry the complete log.
     */
    public GenerationStatusDto status(UUID projectId) {
        Project project = projectService.getEntity(projectId);
        boolean running = jobRegistry.isRunning(projectId);
        List<GenerationLogEntry> entries = running
                ? new ArrayList<>(jobRegistry.liveSnapshot(projectId))
                : persistedLog(projectId);
        return new GenerationStatusDto(
                projectId,
                project.getStatus(),
                !running,
                entries.size(),
                project.getFailureReason(),
                entries);
    }

    /** The persisted user facing log, replayed when the live buffer is gone. */
    public List<GenerationLogEntry> persistedLog(UUID projectId) {
        return GenerationLogParser.parse(jobRegistry.readPersistedLog(projectId));
    }

    /** Relative path of the ZIP inside the workspace, stored on the project. */
    public String artifactPath(UUID projectId) {
        Project project = projectService.getEntity(projectId);
        if (!project.isArtifactAvailable()) {
            throw new es.scafftrix.factory.exception.DownloadException(
                    "Este proyecto todavía no tiene un paquete descargable.",
                    "No artifact available for projectId=" + projectId + " (status=" + project.getStatus() + ")");
        }
        Path artifact = workspaceManager.artifactPath(projectId, project.getArtifactId());
        return workspaceManager.relativePath(artifact);
    }

    private void log(UUID projectId, GenerationLogEntry entry) {
        jobRegistry.append(projectId, entry);
    }

    private void logTechnical(UUID projectId, String line) {
        jobRegistry.appendTechnicalError(projectId, "[" + Instant.now() + "] " + line);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        return seconds < 60 ? seconds + " s" : (seconds / 60) + " min " + (seconds % 60) + " s";
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 900 ? value : value.substring(0, 900) + "...";
    }
}