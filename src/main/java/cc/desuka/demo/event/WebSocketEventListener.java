package cc.desuka.demo.event;

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

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onTaskChange(TaskChangeEvent event) {
        messagingTemplate.convertAndSend("/topic/tasks", event);
    }

    @TransactionalEventListener
    public void onCommentChange(CommentChangeEvent event) {
        messagingTemplate.convertAndSend("/topic/tasks/" + event.taskId() + "/comments", event);
    }
}
