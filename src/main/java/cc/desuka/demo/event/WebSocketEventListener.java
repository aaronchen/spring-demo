package cc.desuka.demo.event;

import cc.desuka.demo.config.AppRoutesProperties;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles all WebSocket broadcasting — ephemeral messages that power stale-data banners and live
 * comment refresh. No DB persistence. Same pattern as {@code AuditEventListener} and {@code
 * NotificationEventListener}.
 */
@Component
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final AppRoutesProperties appRoutes;

    public WebSocketEventListener(
            SimpMessagingTemplate messagingTemplate, AppRoutesProperties appRoutes) {
        this.messagingTemplate = messagingTemplate;
        this.appRoutes = appRoutes;
    }

    @TransactionalEventListener
    public void onTaskChange(TaskChangeEvent event) {
        messagingTemplate.convertAndSend(
                appRoutes.getTopicProjectTasks().resolve("projectId", event.projectId()), event);
    }

    @TransactionalEventListener
    public void onCommentChange(CommentChangeEvent event) {
        messagingTemplate.convertAndSend(
                appRoutes.getTopicTaskComments().resolve("taskId", event.taskId()), event);
    }
}
