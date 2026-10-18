package es.scafftrix.factory.workspace;

import es.scafftrix.factory.config.ScafftrixProperties;
import es.scafftrix.factory.exception.DownloadException;
import es.scafftrix.factory.exception.InvalidConfigurationException;
import es.scafftrix.factory.exception.WorkspaceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceManagerTest {

    @TempDir
    Path workspace;

    private ScafftrixProperties properties;
    private WorkspaceManager manager;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        properties = new ScafftrixProperties();
        properties.getWorkspace().setPath(workspace.toString());
        properties.getWorkspace().setProjectFolderName("project");
        manager = new WorkspaceManager(properties);
        projectId = UUID.randomUUID();
    }

    @Test
    void exposesTheNormalisedWorkspaceRoot() {
        assertThat(manager.getWorkspaceRoot()).isEqualTo(workspace.toAbsolutePath().normalize());
    }

    @Test
    void createsTheWorkspaceWhenItDoesNotExist() {
        Path missing = workspace.resolve("nested/created");
        properties.getWorkspace().setPath(missing.toString());
        WorkspaceManager fresh = new WorkspaceManager(properties);

        fresh.ensureWorkspaceAvailable();

        assertThat(Files.isDirectory(missing)).isTrue();
    }

    @Test
    void rejectsAWorkspaceThatIsAFile() throws IOException {
        Path file = workspace.resolve("not-a-directory");
        Files.writeString(file, "x");
        properties.getWorkspace().setPath(file.toString());
        WorkspaceManager fresh = new WorkspaceManager(properties);

        assertThatThrownBy(fresh::ensureWorkspaceAvailable)
                .isInstanceOf(WorkspaceUnavailableException.class);
    }

    @Test
    void acceptsAnExistingWritableWorkspace() {
        manager.ensureWorkspaceAvailable();

        assertThat(Files.isDirectory(workspace)).isTrue();
    }

    @Test
    void resolvesProjectRootInsideTheWorkspace() {
        Path root = manager.projectRoot(projectId);

        assertThat(root).isEqualTo(workspace.toAbsolutePath().normalize().resolve(projectId.toString()));
    }

    @Test
    void rejectsANullProjectId() {
        assertThatThrownBy(() -> manager.projectRoot(null))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void createsTheThreePerProjectDirectories() {
        assertThat(Files.isDirectory(manager.sourceDirectory(projectId))).isTrue();
        assertThat(Files.isDirectory(manager.logDirectory(projectId))).isTrue();
        assertThat(Files.isDirectory(manager.distDirectory(projectId))).isTrue();
    }

    @Test
    void buildsTheArtifactPathFromAValidArtifactId() {
        Path artifact = manager.artifactPath(projectId, "customer-management");

        assertThat(artifact.getFileName().toString()).isEqualTo("customer-management.zip");
        assertThat(artifact).isEqualTo(manager.distDirectory(projectId).resolve("customer-management.zip"));
    }

    @Test
    void rejectsInvalidArtifactIds() {
        assertThatThrownBy(() -> manager.artifactPath(projectId, null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> manager.artifactPath(projectId, "Customer"))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> manager.artifactPath(projectId, "1customer"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void resetSourceDirectoryEmptiesAnExistingTree() throws IOException {
        Path source = manager.sourceDirectory(projectId);
        Files.writeString(source.resolve("stale.txt"), "old");
        Files.createDirectories(source.resolve("nested"));

        manager.resetSourceDirectory(projectId);

        try (var entries = Files.list(manager.sourceDirectory(projectId))) {
            assertThat(entries).isEmpty();
        }
    }

    @Test
    void buildsALogFilePathForAValidName() {
        Path log = manager.logFile(projectId, "generation.log");

        assertThat(log.getFileName().toString()).isEqualTo("generation.log");
    }

    @Test
    void rejectsInvalidLogNames() {
        assertThatThrownBy(() -> manager.logFile(projectId, "../escape.log"))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> manager.logFile(projectId, "with space.log"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void compressesASourceTreeIntoAZipRootedAtTheArtifactId() throws IOException {
        Path source = manager.sourceDirectory(projectId);
        Files.writeString(source.resolve("pom.xml"), "<project/>");
        Files.createDirectories(source.resolve("src/main/java"));
        Files.writeString(source.resolve("src/main/java/App.java"), "class App {}");

        Path zip = manager.compress(projectId, "customer-management", source);

        assertThat(Files.isRegularFile(zip)).isTrue();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            List<String> names = zipFile.stream().map(e -> e.getName()).toList();
            assertThat(names).contains("customer-management/pom.xml",
                    "customer-management/src/main/java/App.java");
            assertThat(names).allMatch(name -> name.startsWith("customer-management/"));
            assertThat(names).noneMatch(name -> name.startsWith("source/"));
        }
    }

    @Test
    void refusesToCompressWhenTheSourceDirectoryIsMissing() {
        Path missing = manager.projectRoot(projectId).resolve("source");

        assertThatThrownBy(() -> manager.compress(projectId, "customer-management", missing))
                .isInstanceOf(DownloadException.class);
    }

    @Test
    void deletesAnOversizedArtifactAndFails() throws IOException {
        properties.getGeneration().setMaxArtifactSizeMb(0);
        WorkspaceManager smallLimit = new WorkspaceManager(properties);
        Path source = smallLimit.sourceDirectory(projectId);
        Files.writeString(source.resolve("pom.xml"), "<project/>");

        assertThatThrownBy(() -> smallLimit.compress(projectId, "customer-management", source))
                .isInstanceOf(DownloadException.class);

        try (var dist = Files.list(smallLimit.distDirectory(projectId))) {
            assertThat(dist).isEmpty();
        }
    }

    @Test
    void deletesAWholeProjectFolder() throws IOException {
        Path source = manager.sourceDirectory(projectId);
        Files.writeString(source.resolve("pom.xml"), "<project/>");

        manager.deleteProject(projectId);

        assertThat(Files.exists(manager.projectRoot(projectId))).isFalse();
    }

    @Test
    void deleteRecursivelyIgnoresNullAndMissingPaths() {
        manager.deleteRecursively(null);
        manager.deleteRecursively(workspace.resolve("does-not-exist"));

        assertThat(true).isTrue();
    }

    @Test
    void deleteRecursivelyRemovesAnExistingTree() throws IOException {
        Path tree = workspace.resolve("tree/deep");
        Files.createDirectories(tree);
        Files.writeString(tree.resolve("file.txt"), "x");

        manager.deleteRecursively(workspace.resolve("tree"));

        assertThat(Files.exists(workspace.resolve("tree"))).isFalse();
    }

    @Test
    void blocksPathsOutsideTheWorkspace() {
        Path outside = workspace.getParent().resolve("outside.txt");

        assertThatThrownBy(() -> manager.assertInsideWorkspace(outside))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void returnsAllPerProjectPaths() {
        List<Path> all = manager.resolveAll(projectId);

        assertThat(all).hasSize(3)
                .allSatisfy(path -> assertThat(path.startsWith(manager.projectRoot(projectId))).isTrue());
    }

    @Test
    void computesARelativeArtifactPath() {
        Path artifact = manager.artifactPath(projectId, "customer-management");

        String relative = manager.relativePath(artifact);

        assertThat(relative).isEqualTo(projectId + "/dist/customer-management.zip");
    }

    @Test
    void resolvesAPreviouslyStoredArtifactPath() throws IOException {
        Path artifact = manager.artifactPath(projectId, "customer-management");
        Files.writeString(artifact, "zip-bytes");
        String relative = manager.relativePath(artifact);

        assertThat(manager.resolveArtifact(relative)).isEqualTo(manager.assertInsideWorkspace(artifact));
    }

    @Test
    void rejectsBlankStoredArtifactPaths() {
        assertThatThrownBy(() -> manager.resolveArtifact(null))
                .isInstanceOf(DownloadException.class);
        assertThatThrownBy(() -> manager.resolveArtifact("   "))
                .isInstanceOf(DownloadException.class);
    }

    @Test
    void rejectsAStoredArtifactPathThatIsNotAFile() {
        assertThatThrownBy(() -> manager.resolveArtifact(projectId + "/dist/missing.zip"))
                .isInstanceOf(DownloadException.class);
    }
}
