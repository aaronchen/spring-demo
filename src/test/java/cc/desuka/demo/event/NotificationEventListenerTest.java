package cc.desuka.demo.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.model.*;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.NotificationService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.Messages;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Mock private NotificationService notificationService;
    @Mock private CommentService commentService;
    @Mock private UserService userService;
    @Mock private Messages messages;
    @Spy private AppRoutesProperties appRoutes = new AppRoutesProperties();

    @InjectMocks private NotificationEventListener listener;

    private User alice;
    private User bob;
    private User charlie;
    private Task task;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(ID_2);
        charlie = new User("Charlie", "charlie@example.com", "password", Role.USER);
        charlie.setId(ID_3);

        task = new Task("Test Task", "Description");
        task.setId(TASK_ID);
        task.setUser(alice);
    }

    // ── onTaskAssigned ───────────────────────────────────────────────────

    @Test
    void onTaskAssigned_notifiesAssignee() {
        when(messages.get(eq("notification.task.assigned"), any(Object[].class)))
                .thenReturn("Bob assigned you Test Task");

        listener.onTaskAssigned(new TaskAssignedEvent(task, bob));

        verify(notificationService)
                .create(
                        eq(alice),
                        eq(bob),
                        eq(NotificationType.TASK_ASSIGNED),
                        anyString(),
                        anyString());
    }

    @Test
    void onTaskAssigned_selfAssign_doesNotNotify() {
        task.setUser(alice);

        listener.onTaskAssigned(new TaskAssignedEvent(task, alice));

        verifyNoInteractions(notificationService);
    }

    @Test
    void onTaskAssigned_unassigned_doesNotNotify() {
        task.setUser(null);

        listener.onTaskAssigned(new TaskAssignedEvent(task, bob));

        verifyNoInteractions(notificationService);
    }

    // ── onTaskUpdated ────────────────────────────────────────────────────

    @Test
    void onTaskUpdated_notifiesOwnerAndSubscribers() {
        when(messages.get(eq("notification.task.updated"), any(Object[].class)))
                .thenReturn("Bob updated Test Task");
        when(commentService.getSubscriberIds(TASK_ID)).thenReturn(Set.of(ID_3));
        when(userService.findUserById(ID_3)).thenReturn(charlie);

        listener.onTaskUpdated(new TaskUpdatedEvent(task, bob));

        // Notifies owner (alice) and subscriber (charlie), not actor (bob)
        verify(notificationService)
                .create(
                        eq(alice),
                        eq(bob),
                        eq(NotificationType.TASK_UPDATED),
                        anyString(),
                        anyString());
        verify(notificationService)
                .create(
                        eq(charlie),
                        eq(bob),
                        eq(NotificationType.TASK_UPDATED),
                        anyString(),
                        anyString());
        verify(notificationService, times(2)).create(any(), any(), any(), any(), any());
    }

    @Test
    void onTaskUpdated_actorIsOwner_doesNotSelfNotify() {
        task.setUser(alice);
        when(messages.get(eq("notification.task.updated"), any(Object[].class)))
                .thenReturn("Alice updated Test Task");
        when(commentService.getSubscriberIds(TASK_ID)).thenReturn(Set.of());

        listener.onTaskUpdated(new TaskUpdatedEvent(task, alice));

        // Alice is both actor and owner — should not be notified
        verifyNoInteractions(notificationService);
    }

    @Test
    void onTaskUpdated_nullActor_doesNothing() {
        listener.onTaskUpdated(new TaskUpdatedEvent(task, null));

        verifyNoInteractions(notificationService);
    }

    @Test
    void onTaskUpdated_subscriberAlsoOwner_notifiedOnlyOnce() {
        // Alice is owner AND subscriber (she commented before)
        when(messages.get(eq("notification.task.updated"), any(Object[].class)))
                .thenReturn("Bob updated Test Task");
        when(commentService.getSubscriberIds(TASK_ID)).thenReturn(Set.of(ID_1));

        listener.onTaskUpdated(new TaskUpdatedEvent(task, bob));

        // Alice notified once (as owner), not again as subscriber
        verify(notificationService, times(1)).create(any(), any(), any(), any(), any());
    }

    // ── onCommentAdded ───────────────────────────────────────────────────

    @Test
    void onCommentAdded_notifiesOwnerCommentersAndMentioned() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("Hey @[Charlie](userId:" + ID_3 + ")");
        comment.setTask(task);
        comment.setUser(bob);

        when(messages.get(eq("notification.comment.added"), any(Object[].class)))
                .thenReturn("Bob commented on Test Task");
        when(messages.get(eq("notification.comment.mentioned"), any(Object[].class)))
                .thenReturn("Bob mentioned you on Test Task");
        when(commentService.getCommenterIds(TASK_ID)).thenReturn(Set.of());
        when(commentService.getPreviouslyMentionedUserIds(TASK_ID)).thenReturn(Set.of());
        when(userService.findUserById(ID_3)).thenReturn(charlie);

        listener.onCommentAdded(new CommentAddedEvent(comment, task, bob));

        // Owner (alice) gets COMMENT_ADDED, mentioned (charlie) gets COMMENT_MENTIONED
        verify(notificationService)
                .create(
                        eq(alice),
                        eq(bob),
                        eq(NotificationType.COMMENT_ADDED),
                        anyString(),
                        anyString());
        verify(notificationService)
                .create(
                        eq(charlie),
                        eq(bob),
                        eq(NotificationType.COMMENT_MENTIONED),
                        anyString(),
                        anyString());
        verify(notificationService, times(2)).create(any(), any(), any(), any(), any());
    }

    @Test
    void onCommentAdded_actorIsOwner_doesNotSelfNotify() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("My own comment");
        comment.setTask(task);
        comment.setUser(alice);

        task.setUser(alice);
        when(messages.get(eq("notification.comment.added"), any(Object[].class)))
                .thenReturn("Alice commented on Test Task");
        when(commentService.getCommenterIds(TASK_ID)).thenReturn(Set.of());
        when(commentService.getPreviouslyMentionedUserIds(TASK_ID)).thenReturn(Set.of());

        listener.onCommentAdded(new CommentAddedEvent(comment, task, alice));

        // Alice is actor and owner — no notification
        verifyNoInteractions(notificationService);
    }

    @Test
    void onCommentAdded_deduplicatesAcrossAllGroups() {
        // Charlie is owner, commenter, AND mentioned — should only get one notification
        task.setUser(charlie);

        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("@[Charlie](userId:" + ID_3 + ")");
        comment.setTask(task);
        comment.setUser(bob);

        when(messages.get(eq("notification.comment.added"), any(Object[].class)))
                .thenReturn("Bob commented on Test Task");
        when(commentService.getCommenterIds(TASK_ID)).thenReturn(Set.of(ID_3));
        when(commentService.getPreviouslyMentionedUserIds(TASK_ID)).thenReturn(Set.of(ID_3));

        listener.onCommentAdded(new CommentAddedEvent(comment, task, bob));

        // Charlie notified once as owner, deduped from commenter + mentioned
        verify(notificationService, times(1)).create(any(), any(), any(), any(), any());
    }
}
