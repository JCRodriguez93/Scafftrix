package es.scafftrix.factory.service;

import es.scafftrix.factory.workspace.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Appends the generation console to per-project files inside the workspace.
 *
 * <p>Two files are kept per project:
 * <ul>
 *   <li>{@code generation.log} &mdash; the user facing progress, and</li>
 *   <li>{@code generation-error.log} &mdash; technical detail for support, containing
 *       generator output that is not shown in the UI.</li>
 * </ul>
 * Keeping the technical log out of the user log is deliberate: the UI can then show the
 * whole of {@code generation.log} without risking the disclosure of paths or internals.
 *
 * <p>Logging failures are swallowed and logged: losing a log line must never abort a
 * generation that is otherwise fine.
 */
@Component
public class GenerationLogWriter {

    private static final Logger LOG = LoggerFactory.getLogger(GenerationLogWriter.class);

    static final String LOG_FILE_NAME = "generation.log";
    static final String ERROR_LOG_FILE_NAME = "generation-error.log";

    private final WorkspaceManager workspaceManager;

    public GenerationLogWriter(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    /** Appends one line to the user facing log. */
    public void appendProgress(UUID projectId, String line) {
        append(projectId, LOG_FILE_NAME, line);
    }

    /** Appends one line to the technical error log. */
    public void appendError(UUID projectId, String line) {
        append(projectId, ERROR_LOG_FILE_NAME, line);
    }

    /** Reads the user facing log; empty string when the project has never been generated. */
    public String readProgress(UUID projectId) {
        return read(projectId, LOG_FILE_NAME);
    }

    private void append(UUID projectId, String fileName, String line) {
        try {
            Path file = workspaceManager.logFile(projectId, fileName);
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not append to generation log for project {}: {}", projectId, e.getMessage());
        }
    }

    private String read(UUID projectId, String fileName) {
        Path file = workspaceManager.logFile(projectId, fileName);
        if (!Files.isRegularFile(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Could not read generation log {}: {}", file, e.getMessage());
            return "";
        }
    }
}