package es.scafftrix.factory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root: a project request created in the portal.
 *
 * <p>The {@code id} is a database-generated UUID and is the <strong>only</strong> value
 * ever used to build filesystem paths (see {@code WorkspaceManager}). User supplied
 * text is stored for display and for the generator configuration, but never used as a
 * path segment.
 */
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Human readable project name, e.g. {@code Customer Management}. */
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** JHipster {@code baseName}, camelCase, e.g. {@code customerManagement}. */
    @Column(name = "base_name", nullable = false, length = 60)
    private String baseName;

    @Column(name = "group_id", nullable = false, length = 120)
    private String groupId;

    @Column(name = "artifact_id", nullable = false, length = 120)
    private String artifactId;

    @Column(name = "package_name", nullable = false, length = 200)
    private String packageName;

    @Column(name = "version", nullable = false, length = 40)
    private String version;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 40, columnDefinition = "varchar(40)")
    private ProjectType projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status;

    /** Pinned corporate standards, copied at creation time for auditability. */
    @Column(name = "database_type", nullable = false, length = 40)
    private String databaseType;

    @Column(name = "frontend", nullable = false, length = 40)
    private String frontend;

    @Column(name = "build_tool", nullable = false, length = 40)
    private String buildTool;

    /**
     * Spring Boot version chosen for the generated project, e.g. {@code 4.1.1}.
     *
     * <p>Nullable so that projects persisted before the selector existed can still be
     * read: {@code ddl-auto=update} cannot add a NOT NULL column to a populated table,
     * and those rows legitimately have no value (the UI shows a dash).
     */
    @Column(name = "spring_boot_version", length = 40)
    private String springBootVersion;

    /**
     * Configuration file format chosen for the generated project.
     *
     * <p>Nullable for the same reason as {@link #springBootVersion}: historic rows
     * predate the selector and must survive the schema update.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "config_format", length = 20)
    private ConfigFormat configFormat;

    /** Relative path of the ZIP artifact inside the workspace root. Never absolute. */
    @Column(name = "artifact_path", length = 400)
    private String artifactPath;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: two concurrent generation triggers cannot both win. */
    @Version
    @Column(name = "entity_version", nullable = false)
    private long entityVersion;

    protected Project() {
        // for JPA
    }

    public Project(String name,
                   ProjectCoordinates coordinates,
                   String description,
                   ProjectType projectType,
                   TechnologyStack technologyStack,
                   String springBootVersion,
                   ConfigFormat configFormat) {
        this.name = name;
        this.baseName = coordinates.baseName();
        this.groupId = coordinates.groupId();
        this.artifactId = coordinates.artifactId();
        this.packageName = coordinates.packageName();
        this.version = coordinates.version();
        this.description = description;
        this.projectType = projectType;
        this.status = ProjectStatus.CREATED;
        this.databaseType = technologyStack.database();
        this.frontend = technologyStack.frontend();
        this.buildTool = technologyStack.buildTool();
        this.springBootVersion = springBootVersion;
        this.configFormat = configFormat;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public TechnologyStack technologyStack() {
        return new TechnologyStack(databaseType, frontend, buildTool);
    }

    public void markGenerating() {
        this.status = ProjectStatus.GENERATING;
        this.failureReason = null;
    }

    public void markGenerated(String artifactPath) {
        this.status = ProjectStatus.GENERATED;
        this.artifactPath = artifactPath;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = ProjectStatus.FAILED;
        this.failureReason = reason;
    }

    public void markDownloaded() {
        this.status = ProjectStatus.DOWNLOADED;
    }

    public void clearArtifact() {
        this.artifactPath = null;
    }

    /**
     * Updates the user editable metadata of the project.
     *
     * <p>Only display/coordinate fields are touched. The values that define the project's
     * identity and generated layout ({@code projectType}, pinned technology,
     * {@code baseName}, {@code artifactId}, {@code packageName}, {@code groupId}) are
     * deliberately absent: changing them after creation would desynchronise the record
     * from the artifact it already produced. The lifecycle fields ({@code status},
     * {@code artifactPath}, timestamps) are managed by the state machine, not by the user.
     */
    public void updateMetadata(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public boolean isArtifactAvailable() {
        return status.isArtifactAvailable() && artifactPath != null;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getFrontend() {
        return frontend;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public ConfigFormat getConfigFormat() {
        return configFormat;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', status=" + status + '}';
    }
}
