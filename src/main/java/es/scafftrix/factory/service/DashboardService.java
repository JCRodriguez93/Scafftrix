package es.scafftrix.factory.service;

import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.DashboardStatsDto;
import es.scafftrix.factory.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Compute dashboard statistics.
 *
 * <p>All counts are obtained through the repository, avoiding N+1 queries and keeping
 * the query surface explicit. The timezone used for "generated today" is the system
 * default; for a corporate network this is typically UTC or the company's standard zone,
 * both are acceptable for the dashboard counters.
 */
@Service
public class DashboardService {

    /**
     * Zone used to decide what "generated today" means. The portal runs inside the corporate
     * network, so the host's system zone (typically UTC) is the single reference; making it
     * explicit keeps the day boundary from depending on an implicit default.
     */
    private static final ZoneId DASHBOARD_ZONE = ZoneId.systemDefault();

    private final ProjectRepository repository;

    public DashboardService(ProjectRepository repository) {
        this.repository = repository;
    }

    public DashboardStatsDto getStats() {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        long total = repository.count();
        long springBoot = repository.countByProjectType(ProjectType.SPRING_BOOT);
        long springBatch = repository.countByProjectType(ProjectType.SPRING_BATCH);
        long springMicroservice = repository.countByProjectType(ProjectType.SPRING_MICROSERVICE);
        long todayCount = repository.countByCreatedAtGreaterThanEqual(today.atStartOfDay(DASHBOARD_ZONE).toInstant());
        return new DashboardStatsDto(total, springBoot, springBatch, springMicroservice, todayCount);
    }
}
