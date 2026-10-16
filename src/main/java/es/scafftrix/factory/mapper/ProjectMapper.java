package es.scafftrix.factory.mapper;

import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.dto.ProjectDetailDto;
import es.scafftrix.factory.dto.ProjectSummaryDto;
import es.scafftrix.factory.dto.ProjectUpdateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts the persistence model into view models.
 *
 * <p>Kept as an explicit hand-written mapper rather than a framework: the project
 * deliberately avoids Lombok and MapStruct, so a new teammate only needs the JDK to
 * read and change this class.
 */
@Component
public class ProjectMapper {

    public ProjectSummaryDto toSummary(Project project) {
        return new ProjectSummaryDto(
                project.getId(),
                project.getName(),
                project.getProjectType(),
                project.getGroupId(),
                project.getArtifactId(),
                project.getPackageName(),
                project.getStatus(),
                project.getCreatedAt(),
                project.isArtifactAvailable());
    }

    public List<ProjectSummaryDto> toSummaries(List<Project> projects) {
        return projects.stream().map(this::toSummary).toList();
    }

    public ProjectDetailDto toDetail(Project project) {
        return new ProjectDetailDto(
                project.getId(),
                project.getName(),
                project.getBaseName(),
                project.getGroupId(),
                project.getArtifactId(),
                project.getPackageName(),
                project.getVersion(),
                project.getDescription(),
                project.getProjectType(),
                project.getStatus(),
                project.getDatabaseType(),
                project.getFrontend(),
                project.getBuildTool(),
                project.getSpringBootVersion(),
                project.getConfigFormat(),
                project.getFailureReason(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.isArtifactAvailable());
    }

    /** Prefills the edit form with the current, editable values of a project. */
    public ProjectUpdateRequest toUpdateRequest(Project project) {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName(project.getName());
        request.setVersion(project.getVersion());
        request.setDescription(project.getDescription());
        return request;
    }
}