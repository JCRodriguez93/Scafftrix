package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;

import java.time.Instant;
import java.util.UUID;

/** Row view of the project history table. */
public record ProjectSummaryDto(UUID id,
                                String name,
                                ProjectType projectType,
                                String groupId,
                                String artifactId,
                                String packageName,
                                ProjectStatus status,
                                Instant createdAt,
                                boolean artifactAvailable) {
}