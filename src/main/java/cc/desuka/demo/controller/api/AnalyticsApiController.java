package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.AnalyticsResponse;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.AnalyticsService;
import cc.desuka.demo.service.ProjectQueryService;
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
    private final ProjectQueryService projectQueryService;

    public AnalyticsApiController(
            AnalyticsService analyticsService, ProjectQueryService projectQueryService) {
        this.analyticsService = analyticsService;
        this.projectQueryService = projectQueryService;
    }

    @GetMapping
    public AnalyticsResponse getCrossProjectAnalytics(
            @RequestParam(required = false) List<Long> projectIds,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        boolean isAdmin = AuthExpressions.isAdmin(currentDetails.getUser());

        // If caller specified projectIds, intersect with accessible set for security
        List<Long> effectiveProjectIds;
        if (isAdmin) {
            // Admin: use caller's filter or null (all projects)
            effectiveProjectIds = (projectIds != null && !projectIds.isEmpty()) ? projectIds : null;
        } else {
            List<Long> accessibleProjectIds =
                    projectQueryService.getAccessibleProjectIds(currentDetails.getUser().getId());
            effectiveProjectIds =
                    (projectIds != null && !projectIds.isEmpty())
                            ? projectIds.stream().filter(accessibleProjectIds::contains).toList()
                            : accessibleProjectIds;
        }

        return analyticsService.getCrossProjectAnalytics(effectiveProjectIds);
    }
}
