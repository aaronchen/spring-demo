package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.AnalyticsResponse;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.AnalyticsService;
import cc.desuka.demo.service.ProjectService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsApiController {

    private final AnalyticsService analyticsService;
    private final ProjectService projectService;

    public AnalyticsApiController(
            AnalyticsService analyticsService, ProjectService projectService) {
        this.analyticsService = analyticsService;
        this.projectService = projectService;
    }

    @GetMapping
    public AnalyticsResponse getCrossProjectAnalytics(
            @RequestParam(required = false) List<Long> projectIds,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        boolean isAdmin = AuthExpressions.isAdmin(currentDetails.getUser());

        // Determine the user's full accessible set
        List<Long> accessibleProjectIds =
                isAdmin
                        ? null
                        : projectService.getAccessibleProjectIds(currentDetails.getUser().getId());

        // If caller specified projectIds, intersect with accessible set for security
        List<Long> effectiveProjectIds;
        if (projectIds != null && !projectIds.isEmpty()) {
            if (isAdmin) {
                // Admin can filter to any projects
                effectiveProjectIds = projectIds;
            } else {
                // Non-admin: only keep IDs the user actually has access to
                effectiveProjectIds =
                        projectIds.stream().filter(accessibleProjectIds::contains).toList();
            }
        } else {
            effectiveProjectIds = accessibleProjectIds;
        }

        return analyticsService.getCrossProjectAnalytics(effectiveProjectIds);
    }
}
