package es.scafftrix.factory.repository;

import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link Project}.
 *
 * <p>{@link JpaSpecificationExecutor} is used for the searchable/filterable history
 * screen so new filter criteria can be added without changing the repository signature.
 */
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    List<Project> findTop10ByOrderByCreatedAtDesc();

    long countByProjectType(ProjectType projectType);

    long countByCreatedAtGreaterThanEqual(Instant threshold);

    List<Project> findByStatusOrderByUpdatedAtAsc(ProjectStatus status);

    /** Used at startup to detect jobs that were interrupted by a restart. */
    @Query("select p from Project p where p.status = es.scafftrix.factory.domain.ProjectStatus.GENERATING")
    List<Project> findAllGenerating();

    @Query("select p from Project p where p.status = es.scafftrix.factory.domain.ProjectStatus.GENERATING")
    Page<Project> findAllGenerating(Pageable pageable);
}