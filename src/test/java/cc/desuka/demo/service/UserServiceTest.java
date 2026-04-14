package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
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

    @Mock private UserQueryService userQueryService;
    @Mock private UserRepository userRepository;
    @Mock private TaskService taskService;
    @Mock private NotificationService notificationService;
    @Mock private PinnedItemService pinnedItemService;
    @Mock private RecentViewService recentViewService;
    @Mock private SavedViewService savedViewService;
    @Mock private UserPreferenceService userPreferenceService;
    @Mock private ProjectMemberRepository memberRepository;
    @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;
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
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);
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
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);
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
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);
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
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("bob@example.com");

            userService.changePassword(ID_2, "$2a$10$encoded");

            assertThat(bob.getPassword()).isEqualTo("$2a$10$encoded");
            verify(userRepository).save(bob);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── enableUser ───────────────────────────────────────────────────────

    @Test
    void enableUser_enablesUser() {
        bob.setEnabled(false);
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            User result = userService.enableUser(ID_2);

            assertThat(result.isEnabled()).isTrue();
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── disableUser ──────────────────────────────────────────────────────

    @Test
    void disableUser_disablesAndUnassignsTasks() {
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            User result = userService.disableUser(ID_2);

            assertThat(result.isEnabled()).isFalse();
            verify(taskService).unassignTasks(bob);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── deleteUser ───────────────────────────────────────────────────────

    @Test
    void deleteUser_cleansUpAndDeletes() {
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("alice@example.com");

            userService.deleteUser(ID_2);

            // Cross-domain cleanup
            verify(taskService).unassignTasks(bob);
            verify(notificationService).nullActorByUserId(ID_2);
            verify(recurringTaskTemplateRepository).nullAssigneeByUserId(ID_2);
            verify(notificationService).clearAll(ID_2);
            verify(memberRepository).deleteByUserId(ID_2);
            verify(pinnedItemService).deleteByUserId(ID_2);
            verify(recentViewService).deleteByUserId(ID_2);
            verify(savedViewService).deleteByUserId(ID_2);
            verify(userPreferenceService).deleteByUserId(ID_2);

            // Entity deletion + audit
            verify(userRepository).delete(bob);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    // ── updateRole ───────────────────────────────────────────────────────

    @Test
    void updateRole_changesRoleAndPublishesEvent() {
        when(userQueryService.getUserById(ID_2)).thenReturn(bob);
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
