package cc.desuka.demo.controller.api;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.RecentViewResponse;
import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.RecentViewQueryService;
import cc.desuka.demo.util.EntityTypes;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recent-views")
public class RecentViewApiController {

    private final RecentViewQueryService recentViewQueryService;
    private final AppRoutesProperties appRoutes;

    public RecentViewApiController(
            RecentViewQueryService recentViewQueryService, AppRoutesProperties appRoutes) {
        this.recentViewQueryService = recentViewQueryService;
        this.appRoutes = appRoutes;
    }

    @GetMapping
    public List<RecentViewResponse> getRecentViews(
            @AuthenticationPrincipal CustomUserDetails user) {
        return recentViewQueryService.getRecentViews(user.getUser().getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private RecentViewResponse toResponse(RecentView rv) {
        String href = EntityTypes.resolveHref(appRoutes, rv.getEntityType(), rv.getEntityId());
        return new RecentViewResponse(
                rv.getEntityType(),
                rv.getEntityId(),
                rv.getEntityTitle(),
                href,
                rv.getViewedAt(),
                false);
    }
}
