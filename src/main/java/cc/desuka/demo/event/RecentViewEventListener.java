package cc.desuka.demo.event;

import cc.desuka.demo.service.RecentViewService;
import cc.desuka.demo.util.EntityTypes;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecentViewEventListener {

    private final RecentViewService recentViewService;
    private final SimpMessagingTemplate messagingTemplate;

    public RecentViewEventListener(
            RecentViewService recentViewService, SimpMessagingTemplate messagingTemplate) {
        this.recentViewService = recentViewService;
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onTaskUpdated(TaskUpdatedEvent event) {
        recentViewService.updateTitle(
                EntityTypes.TASK, event.task().getId(), event.task().getTitle(), event.actor());
    }

    @TransactionalEventListener
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        recentViewService.updateTitle(
                EntityTypes.PROJECT,
                event.project().getId(),
                event.project().getName(),
                event.actor());
    }

    @TransactionalEventListener
    public void onRecentViewPush(RecentViewPushEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.userEmail(), "/queue/recent-views", event.payload());
    }
}
