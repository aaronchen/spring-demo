package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Mock private CommentRepository commentRepository;

    @InjectMocks private CommentQueryService commentQueryService;

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

        Comment result = commentQueryService.getCommentById(1L);

        assertThat(result).isEqualTo(comment);
    }

    @Test
    void getCommentById_notFound_throwsEntityNotFoundException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentQueryService.getCommentById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── getCommentsByTaskId ──────────────────────────────────────────────

    @Test
    void getCommentsByTaskId_delegatesToRepository() {
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID))
                .thenReturn(List.of(comment));

        List<Comment> result = commentQueryService.getCommentsByTaskId(TASK_ID);

        assertThat(result).containsExactly(comment);
    }

    // ── getSubscriberIds ─────────────────────────────────────────────────

    @Test
    void getSubscriberIds_mergesCommentersAndMentionedUsers() {
        when(commentRepository.findDistinctUsersByTaskId(TASK_ID)).thenReturn(List.of(alice));
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(List.of("Hello @[Bob](userId:" + ID_2 + ")"));

        Set<UUID> result = commentQueryService.getSubscriberIds(TASK_ID);

        assertThat(result).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    @Test
    void getSubscriberIds_deduplicates() {
        when(commentRepository.findDistinctUsersByTaskId(TASK_ID)).thenReturn(List.of(alice));
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(List.of("@[Alice](userId:" + ID_1 + ") see this"));

        Set<UUID> result = commentQueryService.getSubscriberIds(TASK_ID);

        assertThat(result).containsExactly(ID_1);
    }

    // ── getCommenterIds ──────────────────────────────────────────────────

    @Test
    void getCommenterIds_returnsDistinctUserIds() {
        when(commentRepository.findDistinctUsersByTaskId(TASK_ID)).thenReturn(List.of(alice, bob));

        Set<UUID> result = commentQueryService.getCommenterIds(TASK_ID);

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

        Set<UUID> result = commentQueryService.getPreviouslyMentionedUserIds(TASK_ID);

        assertThat(result).containsExactlyInAnyOrder(ID_1, ID_2);
    }

    @Test
    void getPreviouslyMentionedUserIds_noMentions_returnsEmpty() {
        when(commentRepository.findCommentTextsByTaskId(TASK_ID))
                .thenReturn(List.of("Plain text comment"));

        Set<UUID> result = commentQueryService.getPreviouslyMentionedUserIds(TASK_ID);

        assertThat(result).isEmpty();
    }
}
