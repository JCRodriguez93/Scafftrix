package es.scafftrix.factory.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.scafftrix.factory.service.ProjectGenerationService;
import es.scafftrix.factory.service.ProjectService;

/**
 * Controller that drives generation and artifact download.
 *
 * <p>{@link #generate(UUID)} returns immediately (202-style redirect) and starts the
 * asynchronous job; the live console is served via {@link #streamLog(UUID)} (Server-Sent
 * Events). The download endpoint reads only from the workspace, and after successful
 * delivery marks the project as {@code DOWNLOADED}.
 */
@Controller
@RequestMapping("/projects/{id}/generation")
public class GenerationController {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1 hour

    private static final Logger LOG = LoggerFactory.getLogger(GenerationController.class);

    private final ProjectGenerationService generationService;
    private final ProjectService projectService;
    private final es.scafftrix.factory.workspace.WorkspaceManager workspaceManager;
    private final ExecutorService sseExecutor = Executors.newSingleThreadExecutor();

    public GenerationController(ProjectGenerationService generationService,
                                ProjectService projectService,
                                es.scafftrix.factory.workspace.WorkspaceManager workspaceManager) {
        this.generationService = generationService;
        this.projectService = projectService;
        this.workspaceManager = workspaceManager;
    }

    @PostMapping
    public String generate(@PathVariable UUID id) {
        generationService.requestGeneration(id);
        return "redirect:/projects/" + id + "?tab=logs";
    }

    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<es.scafftrix.factory.dto.GenerationStatusDto> status(@PathVariable UUID id) {
        return ResponseEntity.ok(generationService.status(id));
    }

    /**
     * Streams the generation log via Server-Sent Events.
     *
     * <p>The emitter sends a heartbeat every second while the job runs so the browser
     * keeps the connection open. When the job finishes the stream completes.
     */
    @GetMapping(value = "/log/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLog(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        sseExecutor.submit(() -> {
            while (true) {
                var status = generationService.status(id);
                try {
                    emitter.send(SseEmitter.event()
                            .name("status")
                            .data(status));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    return;
                }
                if (status.finished()) {
                    emitter.complete();
                    return;
                }
                // heartbeat every 1s
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    emitter.completeWithError(e);
                    return;
                }
            }
        });
        emitter.onCompletion(() -> LOG.debug("SSE completed for project {}", id));
        emitter.onTimeout(() -> LOG.debug("SSE timeout for project {}", id));
        emitter.onError(throwable -> LOG.debug("SSE error for project {}: {}", id, throwable));
        return emitter;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) throws IOException {
        String relative = generationService.artifactPath(id);
        Path file = workspaceManager.resolveArtifact(relative);
        byte[] bytes = Files.readAllBytes(file);
        projectService.markDownloaded(id);
        String filename = file.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
