package es.scafftrix.factory.service;

import es.scafftrix.factory.dto.GenerationLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks the state of in-flight generation jobs and guards against duplicates.
 *
 * <p><strong>Division of responsibility.</strong> The authoritative status of a project
 * lives in the database ({@code Project.status}); this registry only holds what the
 * database is a bad fit for:
 * <ul>
 *   <li>a <em>live</em> buffer of log lines, appended by the worker thread while it runs
 *       and streamed to the browser over SSE, and</li>
 *   <li>a claim flag so a second request cannot start a concurrent generation for the
 *       same project.</li>
 * </ul>
 * Every line is also handed to {@link GenerationLogWriter} so it is persisted and
 * survives an application restart.
 *
 * <p>The claim flag is process-local, which is correct for the single-node deployment the
 * MVP targets. Moving to multiple nodes requires a shared lock; the service layer talks
 * to this class through {@link #tryRegister(UUID)} / {@link #release(UUID)}, so that
 * change stays confined to this file.
 */
@Component
public class GenerationJobRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(GenerationJobRegistry.class);

    private final Map<UUID, List<GenerationLogEntry>> liveBuffers = new ConcurrentHashMap<>();
    private final GenerationLogWriter logWriter;

    public GenerationJobRegistry(GenerationLogWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Attempts to claim the generation slot for a project.
     *
     * @return {@code false} when a job is already running for that project
     */
    public boolean tryRegister(UUID projectId) {
        boolean claimed = liveBuffers.putIfAbsent(projectId, new CopyOnWriteArrayList<>()) == null;
        if (!claimed) {
            LOG.info("Generation already running for project {}", projectId);
        }
        return claimed;
    }

    public void release(UUID projectId) {
        liveBuffers.remove(projectId);
    }

    public boolean isRunning(UUID projectId) {
        return liveBuffers.containsKey(projectId);
    }

    /** Records a user facing line, both live and on disk. */
    public void append(UUID projectId, GenerationLogEntry entry) {
        List<GenerationLogEntry> buffer = liveBuffers.get(projectId);
        if (buffer != null) {
            buffer.add(entry);
        }
        logWriter.appendProgress(projectId, format(entry));
    }

    /**
     * Snapshot of the lines recorded so far in this process.
     *
     * <p>Used by the SSE stream. When the job is not in memory (for example right after a
     * restart) the caller falls back to the persisted log.
     */
    public List<GenerationLogEntry> liveSnapshot(UUID projectId) {
        List<GenerationLogEntry> buffer = liveBuffers.get(projectId);
        return buffer == null ? List.of() : List.copyOf(buffer);
    }

    /** Full persisted user facing log, available even after a restart. */
    public String readPersistedLog(UUID projectId) {
        return logWriter.readProgress(projectId);
    }

    /** Records a technical detail that must not be shown in the UI console. */
    public void appendTechnicalError(UUID projectId, String line) {
        logWriter.appendError(projectId, line);
    }

    private String format(GenerationLogEntry entry) {
        return "[" + entry.instant() + "] [" + entry.level() + "] " + entry.message();
    }
}