package cc.desuka.demo.controller;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.DashboardService;
import cc.desuka.demo.service.ProjectQueryService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProjectQueryService projectQueryService;

    public DashboardController(
            DashboardService dashboardService, ProjectQueryService projectQueryService) {
        this.dashboardService = dashboardService;
        this.projectQueryService = projectQueryService;
    }

    // GET /dashboard - Personal dashboard
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetails currentDetails, Model model) {
        User currentUser = currentDetails.getUser();
        List<Long> accessibleProjectIds =
                AuthExpressions.isAdmin(currentUser)
                        ? null
                        : projectQueryService.getAccessibleProjectIds(currentUser.getId());
        model.addAttribute("stats", dashboardService.buildStats(currentUser, accessibleProjectIds));
        return "dashboard/dashboard";
    }

    // GET /dashboard/stats - HTMX fragment for real-time refresh
    @GetMapping("/dashboard/stats")
    public String dashboardStats(
            @AuthenticationPrincipal CustomUserDetails currentDetails, Model model) {
        User currentUser = currentDetails.getUser();
        List<Long> accessibleProjectIds =
                AuthExpressions.isAdmin(currentUser)
                        ? null
                        : projectQueryService.getAccessibleProjectIds(currentUser.getId());
        model.addAttribute("stats", dashboardService.buildStats(currentUser, accessibleProjectIds));
        return "dashboard/dashboard-stats";
    }
}
