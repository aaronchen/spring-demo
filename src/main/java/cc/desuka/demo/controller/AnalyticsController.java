package cc.desuka.demo.controller;

import cc.desuka.demo.model.Project;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.ProjectQueryService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final ProjectQueryService projectQueryService;

    public AnalyticsController(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    @GetMapping
    public String crossProjectAnalytics(
            @AuthenticationPrincipal CustomUserDetails currentDetails, Model model) {
        model.addAttribute("apiUrl", "/api/analytics");

        boolean isAdmin = AuthExpressions.isAdmin(currentDetails.getUser());
        List<Project> projects;
        if (isAdmin) {
            projects = projectQueryService.getActiveProjects();
        } else {
            projects = projectQueryService.getProjectsForUser(currentDetails.getUser().getId());
        }
        model.addAttribute("projects", projects);
        return "analytics/analytics";
    }
}
