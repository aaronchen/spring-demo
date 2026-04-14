package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User write operations (create, update, delete, enable/disable, password, role). Counterpart to
 * {@link UserQueryService} (reads).
 */
@Service
@Transactional
public class UserService {

    private final UserQueryService userQueryService;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final PinnedItemService pinnedItemService;
    private final RecentViewService recentViewService;
    private final SavedViewService savedViewService;
    private final UserPreferenceService userPreferenceService;
    private final ProjectMemberRepository memberRepository;
    private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(
            UserQueryService userQueryService,
            UserRepository userRepository,
            TaskService taskService,
            NotificationService notificationService,
            PinnedItemService pinnedItemService,
            RecentViewService recentViewService,
            SavedViewService savedViewService,
            UserPreferenceService userPreferenceService,
            ProjectMemberRepository memberRepository,
            RecurringTaskTemplateRepository recurringTaskTemplateRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userQueryService = userQueryService;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.notificationService = notificationService;
        this.pinnedItemService = pinnedItemService;
        this.recentViewService = recentViewService;
        this.savedViewService = savedViewService;
        this.userPreferenceService = userPreferenceService;
        this.memberRepository = memberRepository;
        this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
        this.eventPublisher = eventPublisher;
    }

    // ── Create ───────────────────────────────────────────────────────────

    public User createUser(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_CREATED,
                        User.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public User registerUser(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_REGISTERED,
                        User.class,
                        saved.getId(),
                        saved.getEmail(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    // ── Update ───────────────────────────────────────────────────────────

    public User updateUser(UUID userId, String name, String email, Role role) {
        User user = userQueryService.getUserById(userId);
        Map<String, AuditField> before = user.toAuditSnapshot();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        User saved = userRepository.save(user);
        Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.USER_UPDATED,
                            User.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
        }
        return saved;
    }

    public User updateProfile(UUID userId, String name, String email) {
        User user = userQueryService.getUserById(userId);
        Map<String, AuditField> before = user.toAuditSnapshot();
        user.setName(name);
        user.setEmail(email);
        User saved = userRepository.save(user);
        Map<String, AuditField> after = saved.toAuditSnapshot();
        Map<String, Object> diff = AuditDetails.diff(before, after);
        if (!diff.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.PROFILE_UPDATED,
                            User.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(diff)));
        }
        return saved;
    }

    public User updateRole(UUID userId, Role role) {
        User user = userQueryService.getUserById(userId);
        user.setRole(role);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_ROLE_CHANGED,
                        User.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(
                                Map.of(
                                        User.FIELD_NAME,
                                        saved.getName(),
                                        User.FIELD_ROLE,
                                        role.name()))));
        return saved;
    }

    // ── Password ─────────────────────────────────────────────────────────

    public void changePassword(UUID userId, String encodedPassword) {
        User user = userQueryService.getUserById(userId);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.PROFILE_PASSWORD_CHANGED,
                        User.class,
                        userId,
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(Map.of(User.FIELD_NAME, user.getName()))));
    }

    public void resetPassword(UUID userId, String encodedPassword) {
        User user = userQueryService.getUserById(userId);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_PASSWORD_RESET,
                        User.class,
                        userId,
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(Map.of(User.FIELD_NAME, user.getName()))));
    }

    // ── Enable / Disable ─────────────────────────────────────────────────

    public User enableUser(UUID userId) {
        User user = userQueryService.getUserById(userId);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_ENABLED,
                        User.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    /**
     * Disables a user and unassigns all their tasks. Disabled users cannot log in and are hidden
     * from assignment dropdowns.
     */
    public User disableUser(UUID userId) {
        User user = userQueryService.getUserById(userId);
        user.setEnabled(false);
        User saved = userRepository.save(user);

        taskService.unassignTasks(user);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_DISABLED,
                        User.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    // ── Delete ───────────────────────────────────────────────────────────

    /**
     * Deletes a user after cleaning up all cross-domain references: unassigns tasks, nulls
     * notification actors and recurring template assignees, deletes notifications, project
     * memberships, pins, recent views, saved views, and preferences.
     */
    public void deleteUser(UUID userId) {
        User user = userQueryService.getUserById(userId);
        UUID id = user.getId();
        String snapshot = AuditDetails.toJson(user.toAuditSnapshot());

        // Cross-domain cleanup
        taskService.unassignTasks(user);
        notificationService.nullActorByUserId(id);
        recurringTaskTemplateRepository.nullAssigneeByUserId(id);
        notificationService.clearAll(id);
        memberRepository.deleteByUserId(id);
        pinnedItemService.deleteByUserId(id);
        recentViewService.deleteByUserId(id);
        savedViewService.deleteByUserId(id);
        userPreferenceService.deleteByUserId(id);

        // Delete the user entity
        userRepository.delete(user);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_DELETED,
                        User.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }
}
