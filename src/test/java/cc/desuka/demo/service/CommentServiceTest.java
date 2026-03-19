package cc.desuka.demo.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

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
        alice.setId(1L);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(2L);

        task = new Task("Test Task", "Description");
        task.setId(1L);

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
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(comment));

        List<Comment> result = commentService.getCommentsByTaskId(1L);

        assertThat(result).containsExactly(comment);
    }

    // ── createComment ────────────────────────────────────────────────────

    @Test
    void createComment_savesAndPublishesEvents() {
        when(taskQueryService.getTaskById(1L)).thenReturn(task);
        when(userService.getUserById(1L)).thenReturn(alice);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            Comment result = commentService.createComment("New comment", 1L, 1L);

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
        commentService.deleteByTaskId(1L);

        verify(commentRepository).deleteByTaskId(1L);
    }

    // ── countByUserId ────────────────────────────────────────────────────

    @Test
    void countByUserId_delegatesToRepository() {
        when(commentRepository.countByUserId(1L)).thenReturn(5L);

        long count = commentService.countByUserId(1L);

        assertThat(count).isEqualTo(5L);
    }

    // ── getSubscriberIds ─────────────────────────────────────────────────

    @Test
    void getSubscriberIds_mergesCommentersAndMentionedUsers() {
        when(commentRepository.findDistinctUsersByTaskId(1L)).thenReturn(List.of(alice));
        when(commentRepository.findCommentTextsByTaskId(1L))
                .thenReturn(List.of("Hello @[Bob](userId:2)"));

        Set<Long> result = commentService.getSubscriberIds(1L);

        assertThat(result).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void getSubscriberIds_deduplicates() {
        // Alice is both a commenter and mentioned
        when(commentRepository.findDistinctUsersByTaskId(1L)).thenReturn(List.of(alice));
        when(commentRepository.findCommentTextsByTaskId(1L))
                .thenReturn(List.of("@[Alice](userId:1) see this"));

        Set<Long> result = commentService.getSubscriberIds(1L);

        assertThat(result).containsExactly(1L);
    }

    // ── getCommenterIds ──────────────────────────────────────────────────

    @Test
    void getCommenterIds_returnsDistinctUserIds() {
        when(commentRepository.findDistinctUsersByTaskId(1L))
                .thenReturn(List.of(alice, bob));

        Set<Long> result = commentService.getCommenterIds(1L);

        assertThat(result).containsExactlyInAnyOrder(1L, 2L);
    }

    // ── getPreviouslyMentionedUserIds ────────────────────────────────────

    @Test
    void getPreviouslyMentionedUserIds_parsesEncodedMentions() {
        when(commentRepository.findCommentTextsByTaskId(1L))
                .thenReturn(List.of(
                        "Hey @[Alice](userId:1)",
                        "cc @[Bob](userId:2) and @[Alice](userId:1)"));

        Set<Long> result = commentService.getPreviouslyMentionedUserIds(1L);

        assertThat(result).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void getPreviouslyMentionedUserIds_noMentions_returnsEmpty() {
        when(commentRepository.findCommentTextsByTaskId(1L))
                .thenReturn(List.of("Plain text comment"));

        Set<Long> result = commentService.getPreviouslyMentionedUserIds(1L);

        assertThat(result).isEmpty();
    }
}
