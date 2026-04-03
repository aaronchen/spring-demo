package cc.desuka.demo.service;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.RecentViewResponse;
import cc.desuka.demo.event.RecentViewPushEvent;
import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.RecentViewRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecentViewService {

    private static final int MAX_ENTRIES = 10;

    private final RecentViewRepository recentViewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppRoutesProperties appRoutes;

    public RecentViewService(
            RecentViewRepository recentViewRepository,
            ApplicationEventPublisher eventPublisher,
            AppRoutesProperties appRoutes) {
        this.recentViewRepository = recentViewRepository;
        this.eventPublisher = eventPublisher;
        this.appRoutes = appRoutes;
    }

    public void recordView(User user, String entityType, Object entityId, String entityTitle) {
        String idStr = entityId.toString();
        Optional<RecentView> existing =
                recentViewRepository.findByUserIdAndEntityTypeAndEntityId(
                        user.getId(), entityType, idStr);

        if (existing.isPresent()) {
            RecentView view = existing.get();
            view.setViewedAt(LocalDateTime.now());
            view.setEntityTitle(entityTitle);
        } else {
            recentViewRepository.save(new RecentView(user, entityType, idStr, entityTitle));
            if (recentViewRepository.countByUserId(user.getId()) > MAX_ENTRIES) {
                List<Long> keepIds =
                        recentViewRepository
                                .findTop10ByUserIdOrderByViewedAtDesc(user.getId())
                                .stream()
                                .map(RecentView::getId)
                                .toList();
                recentViewRepository.deleteByUserIdAndIdNotIn(user.getId(), keepIds);
            }
        }

        String href = resolveHref(entityType, idStr);
        RecentViewResponse payload =
                new RecentViewResponse(
                        entityType, idStr, entityTitle, href, LocalDateTime.now(), false);
        eventPublisher.publishEvent(new RecentViewPushEvent(user.getEmail(), payload));
    }

    @Transactional(readOnly = true)
    public List<RecentView> getRecentViews(UUID userId) {
        return recentViewRepository.findTop10ByUserIdOrderByViewedAtDesc(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTitle(String entityType, Object entityId, String title, User actor) {
        String idStr = entityId.toString();
        recentViewRepository.updateTitle(entityType, idStr, title);

        // Push title change to all users who have this entity in their recent views
        // Actor: bump viewedAt (they just interacted) and prepend to top
        // Others: update title in place without moving
        LocalDateTime now = LocalDateTime.now();
        for (RecentView rv : recentViewRepository.findByEntityTypeAndEntityId(entityType, idStr)) {
            boolean isActor = rv.getUser().getId().equals(actor.getId());
            if (isActor) {
                rv.setViewedAt(now);
            }
            String href = resolveHref(entityType, idStr);
            RecentViewResponse payload =
                    new RecentViewResponse(
                            entityType,
                            idStr,
                            title,
                            href,
                            isActor ? now : rv.getViewedAt(),
                            !isActor);
            eventPublisher.publishEvent(new RecentViewPushEvent(rv.getUser().getEmail(), payload));
        }
    }

    public void deleteByEntity(String entityType, Object entityId) {
        recentViewRepository.deleteByEntityTypeAndEntityId(entityType, entityId.toString());
    }

    public void deleteByUserId(UUID userId) {
        recentViewRepository.deleteByUserId(userId);
    }

    private String resolveHref(String entityType, String entityId) {
        if (RecentView.TYPE_TASK.equals(entityType)) {
            return appRoutes.getTaskDetail().resolve("taskId", entityId);
        }
        return appRoutes.getProjectDetail().resolve("projectId", entityId);
    }
}
