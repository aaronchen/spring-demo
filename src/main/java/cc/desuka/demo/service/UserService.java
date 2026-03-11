package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import cc.desuka.demo.model.Role;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, TaskRepository taskRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
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

    public User updateRole(Long userId, Role role) {
        User user = getUserById(userId);
        user.setRole(role);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_ROLE_CHANGED, User.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
                AuditDetails.toJson(Map.of(User.FIELD_NAME, saved.getName(), User.FIELD_ROLE, role.name()))));
        return saved;
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        String snapshot = AuditDetails.toJson(user.toAuditSnapshot());
        // Unassign all tasks before deleting — prevents FK constraint violation.
        // The @OneToMany collection is LAZY, so we query via TaskRepository instead.
        List<Task> tasks = taskRepository.findByUser(user);
        for (Task task : tasks) {
            task.setUser(null);
        }
        taskRepository.saveAll(tasks);
        userRepository.delete(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_DELETED, User.class, id, SecurityUtils.getCurrentPrincipal(),
                snapshot));
    }
}
