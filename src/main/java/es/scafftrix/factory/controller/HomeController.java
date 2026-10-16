package es.scafftrix.factory.controller;

import es.scafftrix.factory.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home and dashboard controller.
 *
 * <p>The portal is read mostly by developers, so the dashboard is the first place they
 * land to see the overall state and to jump straight to creating a new project.
 */
@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "index";
    }
}