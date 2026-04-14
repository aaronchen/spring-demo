package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Mock private UserRepository userRepository;
    @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;
    @Mock private TaskQueryService taskQueryService;
    @Mock private CommentQueryService commentQueryService;
    @Mock private ProjectQueryService projectQueryService;

    @InjectMocks private UserQueryService userQueryService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(ID_1);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(ID_2);
    }

    // ── getUserById ──────────────────────────────────────────────────────

    @Test
    void getUserById_found() {
        when(userRepository.findById(ID_1)).thenReturn(Optional.of(alice));

        User result = userQueryService.getUserById(ID_1);

        assertThat(result).isEqualTo(alice);
    }

    @Test
    void getUserById_notFound_throwsEntityNotFoundException() {
        when(userRepository.findById(ID_99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getUserById(ID_99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── findUserById ─────────────────────────────────────────────────────

    @Test
    void findUserById_found() {
        when(userRepository.findById(ID_1)).thenReturn(Optional.of(alice));

        assertThat(userQueryService.findUserById(ID_1)).isEqualTo(alice);
    }

    @Test
    void findUserById_null_returnsNull() {
        assertThat(userQueryService.findUserById(null)).isNull();
    }

    @Test
    void findUserById_notFound_returnsNull() {
        when(userRepository.findById(ID_99)).thenReturn(Optional.empty());

        assertThat(userQueryService.findUserById(ID_99)).isNull();
    }

    // ── searchUsers ──────────────────────────────────────────────────────

    @Test
    void searchUsers_blankQuery_returnsAll() {
        when(userRepository.findAllByOrderByNameAsc()).thenReturn(List.of(alice, bob));

        List<User> result = userQueryService.searchUsers("  ");

        assertThat(result).containsExactly(alice, bob);
    }

    @Test
    void searchUsers_withQuery_filtersResults() {
        when(userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
                        "ali", "ali"))
                .thenReturn(List.of(alice));

        List<User> result = userQueryService.searchUsers("ali");

        assertThat(result).containsExactly(alice);
    }

    // ── searchEnabledUsers ───────────────────────────────────────────────

    @Test
    void searchEnabledUsers_blankQuery_returnsAllEnabled() {
        when(userRepository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(alice));

        List<User> result = userQueryService.searchEnabledUsers("");

        assertThat(result).containsExactly(alice);
    }

    // ── canDelete ────────────────────────────────────────────────────────

    @Test
    void canDelete_noBlockers_returnsTrue() {
        when(taskQueryService.countByUserIdAndStatus(ID_2, TaskStatus.COMPLETED)).thenReturn(0L);
        when(commentQueryService.countByUserId(ID_2)).thenReturn(0L);
        when(recurringTaskTemplateRepository.countByCreatedById(ID_2)).thenReturn(0L);
        when(projectQueryService.isSoleOwnerOfAnyProject(ID_2)).thenReturn(false);

        assertThat(userQueryService.canDelete(ID_2)).isTrue();
    }

    @Test
    void canDelete_hasCompletedTasks_returnsFalse() {
        when(taskQueryService.countByUserIdAndStatus(ID_2, TaskStatus.COMPLETED)).thenReturn(3L);

        assertThat(userQueryService.canDelete(ID_2)).isFalse();
    }

    @Test
    void canDelete_hasComments_returnsFalse() {
        when(taskQueryService.countByUserIdAndStatus(ID_2, TaskStatus.COMPLETED)).thenReturn(0L);
        when(commentQueryService.countByUserId(ID_2)).thenReturn(5L);

        assertThat(userQueryService.canDelete(ID_2)).isFalse();
    }

    @Test
    void canDelete_hasRecurringTemplates_returnsFalse() {
        when(taskQueryService.countByUserIdAndStatus(ID_2, TaskStatus.COMPLETED)).thenReturn(0L);
        when(commentQueryService.countByUserId(ID_2)).thenReturn(0L);
        when(recurringTaskTemplateRepository.countByCreatedById(ID_2)).thenReturn(2L);

        assertThat(userQueryService.canDelete(ID_2)).isFalse();
    }

    @Test
    void canDelete_isSoleOwner_returnsFalse() {
        when(taskQueryService.countByUserIdAndStatus(ID_2, TaskStatus.COMPLETED)).thenReturn(0L);
        when(commentQueryService.countByUserId(ID_2)).thenReturn(0L);
        when(recurringTaskTemplateRepository.countByCreatedById(ID_2)).thenReturn(0L);
        when(projectQueryService.isSoleOwnerOfAnyProject(ID_2)).thenReturn(true);

        assertThat(userQueryService.canDelete(ID_2)).isFalse();
    }
}
