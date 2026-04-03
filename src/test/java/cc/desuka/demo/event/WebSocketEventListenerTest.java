package cc.desuka.demo.event;

import static org.mockito.Mockito.verify;

import cc.desuka.demo.config.AppRoutesProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    @Spy private AppRoutesProperties appRoutes = new AppRoutesProperties();

    @InjectMocks private WebSocketEventListener listener;

    @Test
    void onTaskChange_broadcastsToProjectTopic() {
        TaskChangeEvent event = new TaskChangeEvent("created", 1L, 10L, 2L);

        listener.onTaskChange(event);

        verify(messagingTemplate).convertAndSend("/topic/projects/10/tasks", event);
    }

    @Test
    void onCommentChange_broadcastsToTaskCommentsTopic() {
        CommentChangeEvent event = new CommentChangeEvent("created", 5L, 10L, 2L);

        listener.onCommentChange(event);

        verify(messagingTemplate).convertAndSend("/topic/tasks/5/comments", event);
    }
}
