package cc.desuka.demo.event;

import cc.desuka.demo.service.PinnedItemService;
import cc.desuka.demo.util.EntityTypes;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PinnedItemEventListener {

    private final PinnedItemService pinnedItemService;
    private final SimpMessagingTemplate messagingTemplate;

    public PinnedItemEventListener(
            PinnedItemService pinnedItemService, SimpMessagingTemplate messagingTemplate) {
        this.pinnedItemService = pinnedItemService;
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onTaskUpdated(TaskUpdatedEvent event) {
        pinnedItemService.updateTitle(
                EntityTypes.TASK, event.task().getId(), event.task().getTitle());
    }

    @TransactionalEventListener
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        pinnedItemService.updateTitle(
                EntityTypes.PROJECT, event.project().getId(), event.project().getName());
    }

    @TransactionalEventListener
    public void onPinnedItemPush(PinnedItemPushEvent event) {
        messagingTemplate.convertAndSendToUser(event.userEmail(), "/queue/pins", event.payload());
    }
}
