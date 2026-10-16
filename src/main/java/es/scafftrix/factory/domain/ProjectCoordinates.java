package es.scafftrix.factory.domain;

/**
 * The derived identity of a project: JHipster base name plus the Maven coordinates.
 *
 * <p>Grouped into one value object so {@link Project}'s constructor stays within a readable
 * number of arguments. These values are produced by {@code NamingService} from user text and
 * are immutable for the lifetime of the project: {@code updateMetadata} never touches them.
 */
public record ProjectCoordinates(String baseName, String groupId, String artifactId,
                                 String packageName, String version) {
}