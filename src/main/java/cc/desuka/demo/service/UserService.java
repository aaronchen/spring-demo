package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TaskQueryService taskQueryService;
    private final TaskCommandService taskAssignmentService;
    private final CommentQueryService commentQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(
            UserRepository userRepository,
            TaskQueryService taskQueryService,
            TaskCommandService taskAssignmentService,
            CommentQueryService commentQueryService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.taskQueryService = taskQueryService;
        this.taskAssignmentService = taskAssignmentService;
        this.commentQueryService = commentQueryService;
        this.eventPublisher = eventPublisher;
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByNameAsc();
    }

    public Map<UUID, String> getNamesByIds(List<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    public User getUserById(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));
    }

    public User findUserById(UUID id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) return getAllUsers();
        return userRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
                        query, query);
    }

    public List<User> getEnabledUsers() {
        return userRepository.findByEnabledTrueOrderByNameAsc();
    }

    public List<User> searchEnabledUsers(String query) {
        if (query == null || query.isBlank()) return getEnabledUsers();
        return userRepository
                .findByEnabledTrueAndNameContainingIgnoreCaseOrEnabledTrueAndEmailContainingIgnoreCaseOrderByNameAsc(
                        query, query);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

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

    public User updateUser(UUID userId, String name, String email, Role role) {
        User user = getUserById(userId);
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
        User user = getUserById(userId);
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

    public void changePassword(UUID userId, String encodedPassword) {
        User user = getUserById(userId);
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

    public long countCompletedTasks(UUID userId) {
        User user = getUserById(userId);
        return taskQueryService.countByUserAndStatus(user, TaskStatus.COMPLETED);
    }

    public long countComments(UUID userId) {
        return commentQueryService.countByUserId(userId);
    }

    public long countAssignedTasks(UUID userId) {
        User user = getUserById(userId);
        return taskQueryService.countAssignedTasks(user);
    }

    public boolean canDelete(UUID userId) {
        return countCompletedTasks(userId) == 0 && countComments(userId) == 0;
    }

    public User disableUser(UUID userId) {
        User user = getUserById(userId);
        user.setEnabled(false);
        taskAssignmentService.unassignTasks(user);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.USER_DISABLED,
                        User.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public User enableUser(UUID userId) {
        User user = getUserById(userId);
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

    public void resetPassword(UUID userId, String encodedPassword) {
        User user = getUserById(userId);
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

    public User updateRole(UUID userId, Role role) {
        User user = getUserById(userId);
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

    public void deleteUser(UUID id) {
        User user = getUserById(id);
        String snapshot = AuditDetails.toJson(user.toAuditSnapshot());
        taskAssignmentService.unassignTasks(user);
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
