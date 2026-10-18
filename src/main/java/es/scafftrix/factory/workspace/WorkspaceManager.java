package es.scafftrix.factory.workspace;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.exception.DownloadException;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.exception.WorkspaceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Owns every filesystem path the application touches.
 *
 * <p><strong>Layout</strong> (nothing outside this tree is ever read or written):
 * <pre>
 * &lt;workspace.path&gt;/
 *   &lt;projectId&gt;/
 *     source/     generated project as produced by the generator
 *     logs/       generation.log, generation-error.log
 *     dist/       &lt;artifactId&gt;.zip
 * </pre>
 *
 * <p><strong>Security model.</strong> Every method takes a {@link UUID} that must match a
 * strict canonical format. No user-supplied string is ever concatenated into a path.
 * On top of that, each resolved path is verified with {@link #assertInsideWorkspace(Path)}
 * so that even a hypothetical {@code UUID} containing traversal sequences would be
 * rejected. Two independent controls, because this is the boundary that decides where
 * the server reads and writes bytes.
 */
@Component
public class WorkspaceManager {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceManager.class);

    /** Canonical UUID string form: 8-4-4-4-12 lowercase hex digits. */
    private static final Pattern CANONICAL_UUID =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final String SOURCE_DIR = "source";
    private static final String LOGS_DIR = "logs";
    private static final String DIST_DIR = "dist";
    private static final String ZIP_SUFFIX = ".zip";

    /** Guards against zip bombs / runaway artifacts by capping the number of entries. */
    private static final int MAX_ZIP_ENTRIES = 100_000;

    private final Path workspaceRoot;
    private final String projectFolderName;
    private final long maxArtifactSizeBytes;

    public WorkspaceManager(ScafftrixProperties properties) {
        this.workspaceRoot = Path.of(properties.getWorkspace().getPath())
                .toAbsolutePath()
                .normalize();
        this.projectFolderName = properties.getWorkspace().getProjectFolderName();
        this.maxArtifactSizeBytes = properties.getGeneration().getMaxArtifactSizeMb() * 1024L * 1024L;
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    /** Verifies the workspace exists and is writable, creating it when missing. */
    public void ensureWorkspaceAvailable() {
        try {
            if (!Files.exists(workspaceRoot)) {
                Files.createDirectories(workspaceRoot);
                LOG.info("Created project factory workspace at {}", workspaceRoot);
            }
            if (!Files.isDirectory(workspaceRoot)) {
                throw new WorkspaceUnavailableException(
                        "Configured workspace path exists but is not a directory: " + workspaceRoot, null);
            }
            if (!Files.isWritable(workspaceRoot)) {
                throw new WorkspaceUnavailableException(
                        "Configured workspace path is not writable: " + workspaceRoot, null);
            }
        } catch (IOException e) {
            throw new WorkspaceUnavailableException(
                    "Could not create workspace directory: " + workspaceRoot, e);
        }
    }

    public Path projectRoot(UUID projectId) {
        String id = requireCanonicalUuid(projectId);
        return assertInsideWorkspace(workspaceRoot.resolve(id));
    }

    /** Directory handed to the generator process. Recreated empty on every generation. */
    public Path sourceDirectory(UUID projectId) {
        return ensureDirectory(projectRoot(projectId).resolve(SOURCE_DIR));
    }

    public Path logDirectory(UUID projectId) {
        return ensureDirectory(projectRoot(projectId).resolve(LOGS_DIR));
    }

    public Path distDirectory(UUID projectId) {
        return ensureDirectory(projectRoot(projectId).resolve(DIST_DIR));
    }

    /** Absolute path of the ZIP for a project; the artifact id is validated, not trusted. */
    public Path artifactPath(UUID projectId, String artifactId) {
        if (artifactId == null || !artifactId.matches("[a-z][a-z0-9-]{0,62}")) {
            throw new InvalidConfigurationException("El Artifact ID registrado no es válido.",
                    "Refusing to build an artifact path from an invalid artifactId: " + artifactId);
        }
        return assertInsideWorkspace(distDirectory(projectId).resolve(artifactId + ZIP_SUFFIX));
    }

    /** Deletes the previous source tree so a re-generation never mixes with stale files. */
    public void resetSourceDirectory(UUID projectId) {
        Path source = projectRoot(projectId).resolve(SOURCE_DIR);
        deleteRecursively(source);
        ensureDirectory(source);
    }

    public Path logFile(UUID projectId, String name) {
        if (!name.matches("[a-zA-Z0-9._-]{1,64}")) {
            throw new InvalidConfigurationException("Nombre de log no válido.", "Invalid log file name: " + name);
        }
        return assertInsideWorkspace(logDirectory(projectId).resolve(name));
    }

    /**
     * Packs a directory into a ZIP inside {@code dist/}.
     *
     * <p>The ZIP always contains a single top-level folder named after the artifact id,
     * which avoids the "zip slip" problem entirely: extraction cannot escape the target
     * directory because no entry name contains a path traversal sequence.
     *
     * @return absolute path of the created ZIP
     */
    public Path compress(UUID projectId, String artifactId, Path sourceDirectory) {
        Path zipPath = artifactPath(projectId, artifactId);
        assertInsideWorkspace(sourceDirectory);
        if (!Files.isDirectory(sourceDirectory)) {
            throw new DownloadException("No se ha encontrado el contenido generado del proyecto.",
                    "Source directory does not exist for projectId=" + projectId + ": " + sourceDirectory);
        }
        try (OutputStream fileOut = Files.newOutputStream(zipPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
             ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {

            String rootFolder = artifactId;
            int entryCount = 0;
            try (var walk = Files.walk(sourceDirectory)) {
                for (Path path : walk.toList()) {
                    if (entryCount++ > MAX_ZIP_ENTRIES) {
                        throw new IOException("Generated project exceeds the maximum number of entries (" + MAX_ZIP_ENTRIES + ")");
                    }
                    if (path.equals(sourceDirectory)) {
                        continue;
                    }
                    Path relative = sourceDirectory.relativize(path);
                    // Defensive: refuse to write an entry that escapes the root.
                    String entryName = rootFolder + "/" + relative.toString().replace('\\', '/');
                    if (entryName.contains("..")) {
                        throw new IOException("Refusing to add unsafe zip entry: " + entryName);
                    }
                    if (Files.isDirectory(path)) {
                        zipOut.putNextEntry(new ZipEntry(entryName + "/"));
                        zipOut.closeEntry();
                    } else {
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zipOut);
                        zipOut.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new es.scafftrix.factory.exception.CompressionException(
                    "Could not create ZIP for projectId=" + projectId, e);
        }

        long size;
        try {
            size = Files.size(zipPath);
        } catch (IOException e) {
            throw new es.scafftrix.factory.exception.CompressionException(
                    "Could not read generated artifact size for projectId=" + projectId, e);
        }
        if (size > maxArtifactSizeBytes) {
            try {
                Files.deleteIfExists(zipPath);
            } catch (IOException ignored) {
                LOG.warn("Could not delete oversized artifact {}", zipPath);
            }
            throw new DownloadException("El proyecto generado supera el tamaño máximo permitido para descarga.",
                    "Generated artifact for projectId=" + projectId + " is " + size + " bytes, limit is "
                            + maxArtifactSizeBytes);
        }
        LOG.info("Created artifact {} ({} bytes)", zipPath, size);
        return zipPath;
    }

    /** Removes the whole project folder. Used when a project record is deleted. */
    public void deleteProject(UUID projectId) {
        deleteRecursively(projectRoot(projectId));
    }

    public void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    LOG.warn("Could not delete {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            LOG.warn("Could not walk {} for deletion: {}", path, e.getMessage());
        }
    }

    private Path ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new WorkspaceUnavailableException("Could not create directory: " + path, e);
        }
    }

    private String requireCanonicalUuid(UUID projectId) {
        if (projectId == null) {
            throw new InvalidConfigurationException("Identificador de proyecto inválido.",
                    "A null project id was used to resolve a workspace path");
        }
        String value = projectId.toString();
        if (!CANONICAL_UUID.matcher(value).matches()) {
            throw new InvalidConfigurationException("Identificador de proyecto inválido.",
                    "Non-canonical project id rejected for path resolution: " + value);
        }
        return value;
    }

    /**
     * Final containment check: the path must be inside the workspace root.
     *
     * <p>This is intentionally a separate method rather than being folded into path
     * construction, so that every single path the application hands out has been
     * verified against the root by the time it leaves this class.
     */
    public Path assertInsideWorkspace(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot)) {
            LOG.error("Blocked path traversal attempt. workspaceRoot={} candidate={}", workspaceRoot, normalized);
            throw new InvalidConfigurationException("Ruta no permitida.",
                    "Blocked path outside workspace. workspaceRoot=" + workspaceRoot + " candidate=" + normalized);
        }
        return normalized;
    }

    /** Convenience for tests and diagnostics. */
    public List<Path> resolveAll(UUID projectId) {
        Path root = projectRoot(projectId);
        return List.of(root.resolve(SOURCE_DIR), root.resolve(LOGS_DIR), root.resolve(DIST_DIR));
    }

    /**
     * Path of an artifact relative to the workspace root.
     *
     * <p>Relative paths are what get persisted on the entity: they survive a move of the
     * workspace directory, and they cannot be used by the download endpoint as-is to
     * escape the workspace, because the endpoint re-resolves and re-validates anyway.
     */
    public String relativePath(Path path) {
        Path normalized = assertInsideWorkspace(path);
        return workspaceRoot.relativize(normalized).toString().replace('\\', '/');
    }

    /** Absolute path of a previously stored relative artifact path, re-validated. */
    public Path resolveArtifact(String relativeArtifactPath) {
        if (relativeArtifactPath == null || relativeArtifactPath.isBlank()) {
            throw new DownloadException("No hay ningún paquete disponible para descargar.",
                    "Stored artifact path is null or blank");
        }
        Path resolved = assertInsideWorkspace(workspaceRoot.resolve(relativeArtifactPath).normalize());
        if (!Files.isRegularFile(resolved)) {
            throw new DownloadException("El paquete del proyecto ya no está disponible en el servidor.",
                    "Stored artifact path does not resolve to a file: " + resolved);
        }
        return resolved;
    }
}
