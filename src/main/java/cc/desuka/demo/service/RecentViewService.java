package cc.desuka.demo.service;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.RecentViewResponse;
import cc.desuka.demo.model.RecentView;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.RecentViewRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecentViewService {

    private static final int MAX_ENTRIES = 10;

    private final RecentViewRepository recentViewRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AppRoutesProperties appRoutes;

    public RecentViewService(
            RecentViewRepository recentViewRepository,
            SimpMessagingTemplate messagingTemplate,
            AppRoutesProperties appRoutes) {
        this.recentViewRepository = recentViewRepository;
        this.messagingTemplate = messagingTemplate;
        this.appRoutes = appRoutes;
    }

    public void recordView(User user, String entityType, Long entityId, String entityTitle) {
        Optional<RecentView> existing =
                recentViewRepository.findByUserIdAndEntityTypeAndEntityId(
                        user.getId(), entityType, entityId);

        if (existing.isPresent()) {
            RecentView view = existing.get();
            view.setViewedAt(LocalDateTime.now());
            view.setEntityTitle(entityTitle);
        } else {
            recentViewRepository.save(new RecentView(user, entityType, entityId, entityTitle));
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

        String href = resolveHref(entityType, entityId);
        RecentViewResponse payload =
                new RecentViewResponse(
                        entityType, entityId, entityTitle, href, LocalDateTime.now(), false);
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/recent-views", payload);
    }

    @Transactional(readOnly = true)
    public List<RecentView> getRecentViews(Long userId) {
        return recentViewRepository.findTop10ByUserIdOrderByViewedAtDesc(userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTitle(String entityType, Long entityId, String title, User actor) {
        recentViewRepository.updateTitle(entityType, entityId, title);

        // Push title change to all users who have this entity in their recent views
        // Actor: bump viewedAt (they just interacted) and prepend to top
        // Others: update title in place without moving
        LocalDateTime now = LocalDateTime.now();
        for (RecentView rv :
                recentViewRepository.findByEntityTypeAndEntityId(entityType, entityId)) {
            boolean isActor = rv.getUser().getId().equals(actor.getId());
            if (isActor) {
                rv.setViewedAt(now);
            }
            String href = resolveHref(entityType, entityId);
            RecentViewResponse payload =
                    new RecentViewResponse(
                            entityType,
                            entityId,
                            title,
                            href,
                            isActor ? now : rv.getViewedAt(),
                            !isActor);
            messagingTemplate.convertAndSendToUser(
                    rv.getUser().getEmail(), "/queue/recent-views", payload);
        }
    }

    public void deleteByEntity(String entityType, Long entityId) {
        recentViewRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }

    public void deleteByUserId(Long userId) {
        recentViewRepository.deleteByUserId(userId);
    }

    private String resolveHref(String entityType, Long entityId) {
        if (RecentView.TYPE_TASK.equals(entityType)) {
            return appRoutes.getTaskDetail().resolve("taskId", entityId);
        }
        return appRoutes.getProjectDetail().resolve("projectId", entityId);
    }
}
