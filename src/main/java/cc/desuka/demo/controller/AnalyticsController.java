package cc.desuka.demo.controller;

import cc.desuka.demo.model.Project;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.ProjectService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final ProjectService projectService;

    public AnalyticsController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public String crossProjectAnalytics(
            @AuthenticationPrincipal CustomUserDetails currentDetails, Model model) {
        model.addAttribute("apiUrl", "/api/analytics");

        boolean isAdmin = AuthExpressions.isAdmin(currentDetails.getUser());
        List<Project> projects;
        if (isAdmin) {
            projects = projectService.getActiveProjects();
        } else {
            projects = projectService.getProjectsForUser(currentDetails.getUser().getId());
        }
        model.addAttribute("projects", projects);
        return "analytics/analytics";
    }
}
