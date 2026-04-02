package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.AnalyticsResponse;
import cc.desuka.demo.dto.UserResponse;
import cc.desuka.demo.mapper.UserMapper;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.AnalyticsService;
import cc.desuka.demo.service.ProjectQueryService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {

    private final ProjectQueryService projectQueryService;
    private final UserMapper userMapper;
    private final AnalyticsService analyticsService;
    private final ProjectAccessGuard projectAccessGuard;

    public ProjectApiController(
            ProjectQueryService projectQueryService,
            UserMapper userMapper,
            AnalyticsService analyticsService,
            ProjectAccessGuard projectAccessGuard) {
        this.projectQueryService = projectQueryService;
        this.userMapper = userMapper;
        this.analyticsService = analyticsService;
        this.projectAccessGuard = projectAccessGuard;
    }

    // GET /api/projects/{id}/members — all enabled members
    @GetMapping("/{id}/members")
    public List<UserResponse> getProjectMembers(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(id, currentDetails);
        List<User> members =
                projectQueryService.getMembers(id).stream()
                        .map(m -> m.getUser())
                        .filter(User::isEnabled)
                        .toList();
        return userMapper.toResponseList(members);
    }

    // GET /api/projects/{id}/members/assignable — editors and owners only (for task assignment)
    @GetMapping("/{id}/members/assignable")
    public List<UserResponse> getAssignableMembers(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(id, currentDetails);
        List<User> members =
                projectQueryService.getMembers(id).stream()
                        .filter(m -> m.getRole() != ProjectRole.VIEWER)
                        .map(m -> m.getUser())
                        .filter(User::isEnabled)
                        .toList();
        return userMapper.toResponseList(members);
    }

    // GET /api/projects/{id}/analytics — project analytics data
    @GetMapping("/{id}/analytics")
    public AnalyticsResponse getProjectAnalytics(
            @PathVariable Long id,
            @RequestParam(required = false) Long sprintId,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(id, currentDetails);
        if (sprintId != null) {
            return analyticsService.getProjectAnalytics(id, sprintId);
        }
        return analyticsService.getProjectAnalytics(id);
    }
}
