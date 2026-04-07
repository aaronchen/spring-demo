package cc.desuka.demo.controller.api;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.RecentViewResponse;
import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.RecentViewService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recent-views")
public class RecentViewApiController {

    private final RecentViewService recentViewService;
    private final AppRoutesProperties appRoutes;

    public RecentViewApiController(
            RecentViewService recentViewService, AppRoutesProperties appRoutes) {
        this.recentViewService = recentViewService;
        this.appRoutes = appRoutes;
    }

    @GetMapping
    public List<RecentViewResponse> getRecentViews(
            @AuthenticationPrincipal CustomUserDetails user) {
        return recentViewService.getRecentViews(user.getUser().getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private RecentViewResponse toResponse(RecentView rv) {
        String href =
                RecentView.TYPE_TASK.equals(rv.getEntityType())
                        ? appRoutes.getTaskDetail().params("taskId", rv.getEntityId()).build()
                        : appRoutes
                                .getProjectDetail()
                                .params("projectId", rv.getEntityId())
                                .build();
        return new RecentViewResponse(
                rv.getEntityType(),
                rv.getEntityId(),
                rv.getEntityTitle(),
                href,
                rv.getViewedAt(),
                false);
    }
}
