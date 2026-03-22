package cc.desuka.demo.controller;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.DashboardService;
import cc.desuka.demo.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProjectService projectService;

    public DashboardController(DashboardService dashboardService, ProjectService projectService) {
        this.dashboardService = dashboardService;
        this.projectService = projectService;
    }

    // GET /dashboard - Personal dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            List<Long> accessibleProjectIds =
                    AuthExpressions.isAdmin(currentUser)
                            ? null
                            : projectService.getAccessibleProjectIds(currentUser.getId());
            model.addAttribute(
                    "stats", dashboardService.buildStats(currentUser, accessibleProjectIds));
        }
        return "dashboard/dashboard";
    }

    // GET /dashboard/stats - HTMX fragment for real-time refresh
    @GetMapping("/dashboard/stats")
    public String dashboardStats(Model model, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            List<Long> accessibleProjectIds =
                    AuthExpressions.isAdmin(currentUser)
                            ? null
                            : projectService.getAccessibleProjectIds(currentUser.getId());
            model.addAttribute(
                    "stats", dashboardService.buildStats(currentUser, accessibleProjectIds));
        }
        return "dashboard/dashboard-stats";
    }
}
