package es.scafftrix.factory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;

/**
 * Root of the {@code scafftrix.*} configuration tree.
 *
 * <p>Everything that could differ between corporate environments lives here: no
 * corporate decision is hard-coded in Java. Each value can be overridden with an
 * environment variable using Spring Boot's relaxed binding, e.g.
 * {@code SCAFFTRIX_WORKSPACE_PATH} or
 * {@code SCAFFTRIX_DEFAULTS_GROUP_ID}.
 */
@ConfigurationProperties(prefix = "scafftrix")
public class ScafftrixProperties {

    @NestedConfigurationProperty
    private final Workspace workspace = new Workspace();

    @NestedConfigurationProperty
    private final Defaults defaults = new Defaults();

    @NestedConfigurationProperty
    private final Versions versions = new Versions();

    @NestedConfigurationProperty
    private final Generation generation = new Generation();

    public Workspace getWorkspace() {
        return workspace;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public Versions getVersions() {
        return versions;
    }

    public Generation getGeneration() {
        return generation;
    }

    /** Filesystem layout configuration. */
    public static class Workspace {

        /** Root folder where every generated project is stored. */
        private String path = "./workspace";

        /**
         * Name of the folder holding a single project's files. The project UUID is
         * appended to it, so this value is never user controlled.
         */
        private String projectFolderName = "project";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getProjectFolderName() {
            return projectFolderName;
        }

        public void setProjectFolderName(String projectFolderName) {
            this.projectFolderName = projectFolderName;
        }
    }

    public static class Defaults {

        private String groupId = "es.scafftrix";

        private String version = "1.0.0-SNAPSHOT";

        /** Spring Boot version preselected in the form and used when none is chosen. */
        private String springBootVersion = "4.1.1";

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSpringBootVersion() {
            return springBootVersion;
        }

        public void setSpringBootVersion(String springBootVersion) {
            this.springBootVersion = springBootVersion;
        }
    }

    /**
     * Versions the portal may offer. The pinned corporate stack is not selectable, but the
     * Spring Boot version the generated project is built against is, so it lives here.
     */
    public static class Versions {

        /**
         * Offered Spring Boot versions, newest first. Pre-release versions (SNAPSHOT, M2)
         * automatically get the Spring snapshot and milestone repositories in the generated
         * pom.xml, so this list is the whole playground.
         */
        private java.util.List<String> springBoot = java.util.List.of(
                "4.2.0-SNAPSHOT",
                "4.2.0-M2",
                "4.1.2-SNAPSHOT",
                "4.1.1",
                "4.0.9-SNAPSHOT",
                "4.0.8");

        public java.util.List<String> getSpringBoot() {
            return springBoot;
        }

        public void setSpringBoot(java.util.List<String> springBoot) {
            this.springBoot = springBoot == null ? java.util.List.of() : java.util.List.copyOf(springBoot);
        }

        /** Whether a project version is one the portal actually offers. */
        public boolean offers(String springBootVersion) {
            return springBootVersion != null && springBoot.contains(springBootVersion);
        }
    }

    /** Generation job execution limits. */
    public static class Generation {

        /** Maximum wall-clock time for one generation. */
        private Duration timeout = Duration.ofMinutes(15);

        /** Maximum number of concurrent generation jobs. */
        private int concurrency = 2;

        /** Maximum size of a generated ZIP artifact, in megabytes. */
        private long maxArtifactSizeMb = 200;

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public long getMaxArtifactSizeMb() {
            return maxArtifactSizeMb;
        }

        public void setMaxArtifactSizeMb(long maxArtifactSizeMb) {
            this.maxArtifactSizeMb = maxArtifactSizeMb;
        }
    }
}
