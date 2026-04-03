package cc.desuka.demo.event;

import static org.mockito.Mockito.verify;

import cc.desuka.demo.config.AppRoutesProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ID_10 = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Mock private SimpMessagingTemplate messagingTemplate;

    @Spy private AppRoutesProperties appRoutes = new AppRoutesProperties();

    @InjectMocks private WebSocketEventListener listener;

    @Test
    void onTaskChange_broadcastsToProjectTopic() {
        TaskChangeEvent event = new TaskChangeEvent("created", ID_1, ID_10, ID_2);

        listener.onTaskChange(event);

        verify(messagingTemplate).convertAndSend("/topic/projects/" + ID_10 + "/tasks", event);
    }

    @Test
    void onCommentChange_broadcastsToTaskCommentsTopic() {
        CommentChangeEvent event = new CommentChangeEvent("created", ID_5, 10L, ID_2);

        listener.onCommentChange(event);

        verify(messagingTemplate).convertAndSend("/topic/tasks/" + ID_5 + "/comments", event);
    }
}
