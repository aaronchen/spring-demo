package cc.desuka.demo.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private WebSocketEventListener listener;

    @Test
    void onTaskChange_broadcastsToTasksTopic() {
        TaskChangeEvent event = new TaskChangeEvent("created", 1L, 2L);

        listener.onTaskChange(event);

        verify(messagingTemplate).convertAndSend("/topic/tasks", event);
    }

    @Test
    void onCommentChange_broadcastsToTaskCommentsTopic() {
        CommentChangeEvent event = new CommentChangeEvent("created", 5L, 10L, 2L);

        listener.onCommentChange(event);

        verify(messagingTemplate).convertAndSend("/topic/tasks/5/comments", event);
    }
}
