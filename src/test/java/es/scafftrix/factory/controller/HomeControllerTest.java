package es.scafftrix.factory.controller;

import es.scafftrix.factory.dto.DashboardStatsDto;
import es.scafftrix.factory.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    private final DashboardService dashboardService = mock(DashboardService.class);
    private final HomeController controller = new HomeController(dashboardService);

    @Test
    void indexAddsStats() {
        DashboardStatsDto stats = new DashboardStatsDto(1, 1, 0, 0, 1);
        when(dashboardService.getStats()).thenReturn(stats);

        Model model = new ConcurrentModel();
        String view = controller.index(model);

        assertEquals("index", view);
        assertEquals(stats, model.getAttribute("stats"));
    }
}
