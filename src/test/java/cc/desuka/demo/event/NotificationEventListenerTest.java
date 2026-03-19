package cc.desuka.demo.event;

import cc.desuka.demo.model.*;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.NotificationService;
import cc.desuka.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private CommentService commentService;
    @Mock private UserService userService;
    @Mock private MessageSource messageSource;

    @InjectMocks private NotificationEventListener listener;

    private User alice;
    private User bob;
    private User charlie;
    private Task task;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(2L);
        charlie = new User("Charlie", "charlie@example.com", "password", Role.USER);
        charlie.setId(3L);

        task = new Task("Test Task", "Description");
        task.setId(1L);
        task.setUser(alice);
    }

    // ── onTaskAssigned ───────────────────────────────────────────────────

    @Test
    void onTaskAssigned_notifiesAssignee() {
        when(messageSource.getMessage(eq("notification.task.assigned"), any(), any(Locale.class)))
                .thenReturn("Bob assigned you Test Task");

        listener.onTaskAssigned(new TaskAssignedEvent(task, bob));

        verify(notificationService).create(
                eq(alice), eq(bob), eq(NotificationType.TASK_ASSIGNED), anyString(), anyString());
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
        when(messageSource.getMessage(eq("notification.task.updated"), any(), any(Locale.class)))
                .thenReturn("Bob updated Test Task");
        when(commentService.getSubscriberIds(1L)).thenReturn(Set.of(3L));
        when(userService.findUserById(3L)).thenReturn(charlie);

        listener.onTaskUpdated(new TaskUpdatedEvent(task, bob));

        // Notifies owner (alice) and subscriber (charlie), not actor (bob)
        verify(notificationService).create(
                eq(alice), eq(bob), eq(NotificationType.TASK_UPDATED), anyString(), anyString());
        verify(notificationService).create(
                eq(charlie), eq(bob), eq(NotificationType.TASK_UPDATED), anyString(), anyString());
        verify(notificationService, times(2)).create(any(), any(), any(), any(), any());
    }

    @Test
    void onTaskUpdated_actorIsOwner_doesNotSelfNotify() {
        task.setUser(alice);
        when(messageSource.getMessage(eq("notification.task.updated"), any(), any(Locale.class)))
                .thenReturn("Alice updated Test Task");
        when(commentService.getSubscriberIds(1L)).thenReturn(Set.of());

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
        when(messageSource.getMessage(eq("notification.task.updated"), any(), any(Locale.class)))
                .thenReturn("Bob updated Test Task");
        when(commentService.getSubscriberIds(1L)).thenReturn(Set.of(1L));

        listener.onTaskUpdated(new TaskUpdatedEvent(task, bob));

        // Alice notified once (as owner), not again as subscriber
        verify(notificationService, times(1)).create(any(), any(), any(), any(), any());
    }

    // ── onCommentAdded ───────────────────────────────────────────────────

    @Test
    void onCommentAdded_notifiesOwnerCommentersAndMentioned() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("Hey @[Charlie](userId:3)");
        comment.setTask(task);
        comment.setUser(bob);

        when(messageSource.getMessage(eq("notification.comment.added"), any(), any(Locale.class)))
                .thenReturn("Bob commented on Test Task");
        when(messageSource.getMessage(eq("notification.comment.mentioned"), any(), any(Locale.class)))
                .thenReturn("Bob mentioned you on Test Task");
        when(commentService.getCommenterIds(1L)).thenReturn(Set.of());
        when(commentService.getPreviouslyMentionedUserIds(1L)).thenReturn(Set.of());
        when(userService.findUserById(3L)).thenReturn(charlie);

        listener.onCommentAdded(new CommentAddedEvent(comment, task, bob));

        // Owner (alice) gets COMMENT_ADDED, mentioned (charlie) gets COMMENT_MENTIONED
        verify(notificationService).create(
                eq(alice), eq(bob), eq(NotificationType.COMMENT_ADDED), anyString(), anyString());
        verify(notificationService).create(
                eq(charlie), eq(bob), eq(NotificationType.COMMENT_MENTIONED), anyString(), anyString());
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
        when(messageSource.getMessage(eq("notification.comment.added"), any(), any(Locale.class)))
                .thenReturn("Alice commented on Test Task");
        when(commentService.getCommenterIds(1L)).thenReturn(Set.of());
        when(commentService.getPreviouslyMentionedUserIds(1L)).thenReturn(Set.of());

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
        comment.setText("@[Charlie](userId:3)");
        comment.setTask(task);
        comment.setUser(bob);

        when(messageSource.getMessage(eq("notification.comment.added"), any(), any(Locale.class)))
                .thenReturn("Bob commented on Test Task");
        when(commentService.getCommenterIds(1L)).thenReturn(Set.of(3L));
        when(commentService.getPreviouslyMentionedUserIds(1L)).thenReturn(Set.of(3L));

        listener.onCommentAdded(new CommentAddedEvent(comment, task, bob));

        // Charlie notified once as owner, deduped from commenter + mentioned
        verify(notificationService, times(1)).create(any(), any(), any(), any(), any());
    }
}
