package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import cc.desuka.demo.model.Role;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Uses TaskRepository and CommentRepository directly (not TaskService/CommentService)
// to break circular dependency: TaskService → UserService → TaskService.
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, TaskRepository taskRepository,
                       CommentRepository commentRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByNameAsc();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));
    }

    public User findUserById(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) return getAllUsers();
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(query, query);
    }

    public List<User> getEnabledUsers() {
        return userRepository.findByEnabledTrueOrderByNameAsc();
    }

    public List<User> searchEnabledUsers(String query) {
        if (query == null || query.isBlank()) return getEnabledUsers();
        return userRepository.findByEnabledTrueAndNameContainingIgnoreCaseOrEnabledTrueAndEmailContainingIgnoreCaseOrderByNameAsc(query, query);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_CREATED, User.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public User updateUser(Long userId, String name, String email, Role role) {
        User user = getUserById(userId);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_UPDATED, User.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public long countCompletedTasks(Long userId) {
        User user = getUserById(userId);
        return taskRepository.countByUserAndStatus(user, TaskStatus.COMPLETED);
    }

    public long countComments(Long userId) {
        return commentRepository.countByUserId(userId);
    }

    public long countAssignedTasks(Long userId) {
        User user = getUserById(userId);
        return taskRepository.findByUser(user).size();
    }

    public boolean canDelete(Long userId) {
        return countCompletedTasks(userId) == 0 && countComments(userId) == 0;
    }

    public User disableUser(Long userId) {
        User user = getUserById(userId);
        user.setEnabled(false);
        // Unassign open/in-progress tasks and reset to OPEN
        unassignTasks(user);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_DISABLED, User.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public User enableUser(Long userId) {
        User user = getUserById(userId);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_ENABLED, User.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public void resetPassword(Long userId, String encodedPassword) {
        User user = getUserById(userId);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_PASSWORD_RESET, User.class, userId, SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(Map.of(User.FIELD_NAME, user.getName()))));
    }

    public User updateRole(Long userId, Role role) {
        User user = getUserById(userId);
        user.setRole(role);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_ROLE_CHANGED, User.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(Map.of(User.FIELD_NAME, saved.getName(), User.FIELD_ROLE, role.name()))));
        return saved;
    }

    private void unassignTasks(User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        for (Task task : tasks) {
            task.setUser(null);
            // Reset non-completed tasks to OPEN — new owner hasn't started work
            if (task.getStatus() != TaskStatus.COMPLETED) {
                task.setStatus(TaskStatus.OPEN);
            }
        }
        taskRepository.saveAll(tasks);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        String snapshot = AuditDetails.toJson(user.toAuditSnapshot());
        // Unassign all tasks before deleting — prevents FK constraint violation.
        unassignTasks(user);
        userRepository.delete(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_DELETED, User.class, id, SecurityUtils.getCurrentPrincipal(),
                snapshot));
    }
}
