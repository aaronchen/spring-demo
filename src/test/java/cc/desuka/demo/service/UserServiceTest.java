package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.List;
import java.util.Optional;
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
class UserServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Mock private UserRepository userRepository;
    @Mock private TaskQueryService taskQueryService;
    @Mock private TaskCommandService taskAssignmentService;
    @Mock private CommentQueryService commentQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private UserService userService;

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

        User result = userService.getUserById(ID_1);

        assertThat(result).isEqualTo(alice);
    }

    @Test
    void getUserById_notFound_throwsEntityNotFoundException() {
        when(userRepository.findById(ID_99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(ID_99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── findUserById ─────────────────────────────────────────────────────

    @Test
    void findUserById_found() {
        when(userRepository.findById(ID_1)).thenReturn(Optional.of(alice));

        assertThat(userService.findUserById(ID_1)).isEqualTo(alice);
    }

    @Test
    void findUserById_null_returnsNull() {
        assertThat(userService.findUserById(null)).isNull();
    }

    @Test
    void findUserById_notFound_returnsNull() {
        when(userRepository.findById(ID_99)).thenReturn(Optional.empty());

        assertThat(userService.findUserById(ID_99)).isNull();
    }

    // ── searchUsers ──────────────────────────────────────────────────────

    @Test
    void searchUsers_blankQuery_returnsAll() {
        when(userRepository.findAllByOrderByNameAsc()).thenReturn(List.of(alice, bob));

        List<User> result = userService.searchUsers("  ");

        assertThat(result).containsExactly(alice, bob);
    }

    @Test
    void searchUsers_withQuery_filtersResults() {
        when(userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
                        "ali", "ali"))
                .thenReturn(List.of(alice));

        List<User> result = userService.searchUsers("ali");

        assertThat(result).containsExactly(alice);
    }

    // ── searchEnabledUsers ───────────────────────────────────────────────

    @Test
    void searchEnabledUsers_blankQuery_returnsAllEnabled() {
        when(userRepository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(alice));

        List<User> result = userService.searchEnabledUsers("");

        assertThat(result).containsExactly(alice);
    }

    // ── createUser ───────────────────────────────────────────────────────

    @Test
    void createUser_savesAndPublishesAuditEvent() {
        when(userRepository.save(any(User.class))).thenReturn(alice);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("admin@example.com");

            User result = userService.createUser(alice);

            assertThat(result).isEqualTo(alice);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── updateUser ───────────────────────────────────────────────────────

    @Test
    void updateUser_updatesFieldsAndPublishesEvent() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            User result = userService.updateUser(ID_2, "Robert", "robert@example.com", Role.ADMIN);

            assertThat(result.getName()).isEqualTo("Robert");
            assertThat(result.getEmail()).isEqualTo("robert@example.com");
            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── updateProfile ────────────────────────────────────────────────────

    @Test
    void updateProfile_changedFields_publishesAuditWithDiff() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("bob@example.com");

            User result = userService.updateProfile(ID_2, "Robert", "bob@example.com");

            assertThat(result.getName()).isEqualTo("Robert");
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void updateProfile_noChanges_doesNotPublishEvent() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("bob@example.com");

            userService.updateProfile(ID_2, "Bob", "bob@example.com");

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // ── changePassword ───────────────────────────────────────────────────

    @Test
    void changePassword_updatesAndPublishesEvent() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("bob@example.com");

            userService.changePassword(ID_2, "$2a$10$encoded");

            assertThat(bob.getPassword()).isEqualTo("$2a$10$encoded");
            verify(userRepository).save(bob);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── canDelete ────────────────────────────────────────────────────────

    @Test
    void canDelete_noCompletedTasksNoComments_returnsTrue() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(taskQueryService.countByUserAndStatus(bob, TaskStatus.COMPLETED)).thenReturn(0L);
        when(commentQueryService.countByUserId(ID_2)).thenReturn(0L);

        assertThat(userService.canDelete(ID_2)).isTrue();
    }

    @Test
    void canDelete_hasCompletedTasks_returnsFalse() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(taskQueryService.countByUserAndStatus(bob, TaskStatus.COMPLETED)).thenReturn(3L);

        assertThat(userService.canDelete(ID_2)).isFalse();
    }

    @Test
    void canDelete_hasComments_returnsFalse() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(taskQueryService.countByUserAndStatus(bob, TaskStatus.COMPLETED)).thenReturn(0L);
        when(commentQueryService.countByUserId(ID_2)).thenReturn(5L);

        assertThat(userService.canDelete(ID_2)).isFalse();
    }

    // ── disableUser ──────────────────────────────────────────────────────

    @Test
    void disableUser_disablesAndUnassignsTasks() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            User result = userService.disableUser(ID_2);

            assertThat(result.isEnabled()).isFalse();
            verify(taskAssignmentService).unassignTasks(bob);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── enableUser ───────────────────────────────────────────────────────

    @Test
    void enableUser_enablesUser() {
        bob.setEnabled(false);
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            User result = userService.enableUser(ID_2);

            assertThat(result.isEnabled()).isTrue();
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── deleteUser ───────────────────────────────────────────────────────

    @Test
    void deleteUser_unassignsTasksAndDeletes() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            userService.deleteUser(ID_2);

            var inOrder = inOrder(taskAssignmentService, userRepository);
            inOrder.verify(taskAssignmentService).unassignTasks(bob);
            inOrder.verify(userRepository).delete(bob);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── updateRole ───────────────────────────────────────────────────────

    @Test
    void updateRole_changesRoleAndPublishesEvent() {
        when(userRepository.findById(ID_2)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            User result = userService.updateRole(ID_2, Role.ADMIN);

            assertThat(result.getRole()).isEqualTo(Role.ADMIN);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditEvent.USER_ROLE_CHANGED);
        }
    }
}
