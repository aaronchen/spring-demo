package cc.desuka.demo.controller;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.DashboardService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /dashboard - Personal dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("stats", dashboardService.buildStats(currentUser));
        }
        return "dashboard/dashboard";
    }

    // GET /dashboard/stats - HTMX fragment for real-time refresh
    @GetMapping("/dashboard/stats")
    public String dashboardStats(Model model, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("stats", dashboardService.buildStats(currentUser));
        }
        return "dashboard/dashboard-stats";
    }
}
