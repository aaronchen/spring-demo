package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.CommentAddedEvent;
import cc.desuka.demo.event.CommentChangeEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Mock private CommentRepository commentRepository;
    @Mock private TaskQueryService taskQueryService;
    @Mock private UserService userService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private CommentService commentService;

    private User alice;
    private User bob;
    private Task task;
    private Comment comment;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(ID_2);

        task = new Task("Test Task", "Description");
        task.setId(TASK_ID);

        comment = new Comment();
        comment.setId(1L);
        comment.setText("A comment");
        comment.setTask(task);
        comment.setUser(alice);
    }

    // ── getCommentById ───────────────────────────────────────────────────

    @Test
    void getCommentById_found() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        Comment result = commentService.getCommentById(1L);

        assertThat(result).isEqualTo(comment);
    }

    @Test
    void getCommentById_notFound_throwsEntityNotFoundException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getCommentsByTaskId ──────────────────────────────────────────────

    @Test
    void getCommentsByTaskId_delegatesToRepository() {
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID))
                .thenReturn(List.of(comment));

        List<Comment> result = commentService.getCommentsByTaskId(TASK_ID);

        assertThat(result).containsExactly(comment);
    }

    // ── createComment ────────────────────────────────────────────────────

    @Test
    void createComment_savesAndPublishesEvents() {
        when(taskQueryService.getTaskById(TASK_ID)).thenReturn(task);
        when(userService.getUserById(ID_1)).thenReturn(alice);
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(
                        inv -> {
                            Comment c = inv.getArgument(0);
                            c.setId(1L);
                            return c;
                        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            Comment result = commentService.createComment("New comment", TASK_ID, ID_1);

            assertThat(result.getText()).isEqualTo("New comment");
            assertThat(result.getTask()).isEqualTo(task);
            assertThat(result.getUser()).isEqualTo(alice);

            // Verify 3 events: AuditEvent, CommentAddedEvent, CommentChangeEvent
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, times(3)).publishEvent(captor.capture());
            List<Object> events = captor.getAllValues();
            assertThat(events.get(0)).isInstanceOf(AuditEvent.class);
            assertThat(events.get(1)).isInstanceOf(CommentAddedEvent.class);
            assertThat(events.get(2)).isInstanceOf(CommentChangeEvent.class);
        }
    }

    // ── deleteComment ────────────────────────────────────────────────────

    @Test
    void deleteComment_deletesAndPublishesEvents() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(alice);

            commentService.deleteComment(1L);

            verify(commentRepository).delete(comment);

            // Verify 2 events: AuditEvent, CommentChangeEvent
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, times(2)).publishEvent(captor.capture());
            List<Object> events = captor.getAllValues();
            assertThat(events.get(0)).isInstanceOf(AuditEvent.class);
            assertThat(events.get(1)).isInstanceOf(CommentChangeEvent.class);
        }
    }

    @Test
    void deleteComment_notFound_throwsEntityNotFoundException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── deleteByTaskId ───────────────────────────────────────────────────

    @Test
    void deleteByTaskId_delegatesToRepository() {
        commentService.deleteByTaskId(TASK_ID);

        verify(commentRepository).deleteByTaskId(TASK_ID);
    }

    // ── getSubscriberIds ─────────────────────────────────────────────────

    @Test
    void getSubscriberIds_mergesCommentersAndMentionedUsers() {
        when(commentRepository.findDistinctUsersByTaskId(TASK_ID)).thenReturn(List.of(alice));
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(List.of("Hello @[Bob](userId:" + ID_2 + ")"));

        Set<UUID> result = commentService.getSubscriberIds(TASK_ID);

        assertThat(result).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    @Test
    void getSubscriberIds_deduplicates() {
        // Alice is both a commenter and mentioned
        when(commentRepository.findDistinctUsersByTaskId(TASK_ID)).thenReturn(List.of(alice));
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(List.of("@[Alice](userId:" + ID_1 + ") see this"));

        Set<UUID> result = commentService.getSubscriberIds(TASK_ID);

        assertThat(result).containsExactly(ID_1);
    }

    // ── getCommenterIds ──────────────────────────────────────────────────

    @Test
    void getCommenterIds_returnsDistinctUserIds() {
        when(commentRepository.findDistinctUsersByTaskId(TASK_ID)).thenReturn(List.of(alice, bob));

        Set<UUID> result = commentService.getCommenterIds(TASK_ID);

        assertThat(result).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    // ── getPreviouslyMentionedUserIds ────────────────────────────────────

    @Test
    void getPreviouslyMentionedUserIds_parsesEncodedMentions() {
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(
                        List.of(
                                "Hey @[Alice](userId:" + ID_1 + ")",
                                "cc @[Bob](userId:"
                                        + ID_2
                                        + ") and @[Alice](userId:"
                                        + ID_1
                                        + ")"));

        Set<UUID> result = commentService.getPreviouslyMentionedUserIds(TASK_ID);

        assertThat(result).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    @Test
    void getPreviouslyMentionedUserIds_noMentions_returnsEmpty() {
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(List.of("Plain text comment"));

        Set<UUID> result = commentService.getPreviouslyMentionedUserIds(TASK_ID);

        assertThat(result).isEmpty();
    }
}
