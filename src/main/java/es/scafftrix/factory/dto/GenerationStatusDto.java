package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ProjectStatus;

import java.util.List;
import java.util.UUID;

/**
 * Polling/SSE payload describing the state of a generation job.
 *
 * @param finished true once the job reached a terminal state
 */
public record GenerationStatusDto(UUID projectId,
                                  ProjectStatus status,
                                  boolean finished,
                                  int logCount,
                                  String failureReason,
                                  List<GenerationLogEntry> entries) {
}