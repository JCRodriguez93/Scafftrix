package es.scafftrix.factory.service;

import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.DashboardStatsDto;
import es.scafftrix.factory.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final DashboardService service = new DashboardService(repository);

    @Test
    void getStatsAggregatesCounts() {
        when(repository.count()).thenReturn(10L);
        when(repository.countByProjectType(ProjectType.SPRING_BOOT)).thenReturn(6L);
        when(repository.countByProjectType(ProjectType.SPRING_BATCH)).thenReturn(2L);
        when(repository.countByProjectType(ProjectType.SPRING_MICROSERVICE)).thenReturn(2L);
        when(repository.countByCreatedAtGreaterThanEqual(any(Instant.class))).thenReturn(3L);

        DashboardStatsDto stats = service.getStats();
        assertEquals(10L, stats.totalProjects());
        assertEquals(6L, stats.springBootProjects());
        assertEquals(2L, stats.springBatchProjects());
        assertEquals(2L, stats.springMicroserviceProjects());
        assertEquals(3L, stats.generatedToday());
    }
}
