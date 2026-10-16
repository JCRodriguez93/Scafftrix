package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;

import java.time.Instant;
import java.util.UUID;

/** Full detail view of a single project. */
public record ProjectDetailDto(UUID id,
                               String name,
                               String baseName,
                               String groupId,
                               String artifactId,
                               String packageName,
                               String version,
                               String description,
                               ProjectType projectType,
                               ProjectStatus status,
                               String databaseType,
                               String frontend,
                               String buildTool,
                               String springBootVersion,
                               ConfigFormat configFormat,
                               String failureReason,
                               Instant createdAt,
                               Instant updatedAt,
                               boolean artifactAvailable) {
}