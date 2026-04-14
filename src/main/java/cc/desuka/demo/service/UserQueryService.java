package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only user lookups. Counterpart to {@link UserService} (writes). */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
    private final TaskQueryService taskQueryService;
    private final CommentQueryService commentQueryService;
    private final ProjectQueryService projectQueryService;

    public UserQueryService(
            UserRepository userRepository,
            RecurringTaskTemplateRepository recurringTaskTemplateRepository,
            TaskQueryService taskQueryService,
            CommentQueryService commentQueryService,
            ProjectQueryService projectQueryService) {
        this.userRepository = userRepository;
        this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
        this.taskQueryService = taskQueryService;
        this.commentQueryService = commentQueryService;
        this.projectQueryService = projectQueryService;
    }

    // ── Lookups ──────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByNameAsc();
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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Map<UUID, User> findAllByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    public Map<UUID, String> getNamesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    // ── Search ───────────────────────────────────────────────────────────

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

    // ── Decision queries (guard checks for write operations) ─────────────

    /** Aggregated user info for admin panel — counts + deletion/disable eligibility in one pass. */
    public record UserDeletionInfo(
            long completedTasks,
            long comments,
            long assignedTasks,
            long recurringTemplates,
            boolean soleOwner) {

        public boolean canDelete() {
            return completedTasks == 0 && comments == 0 && recurringTemplates == 0 && !soleOwner;
        }

        public boolean canDisable() {
            return !soleOwner;
        }
    }

    public UserDeletionInfo getDeletionInfo(UUID userId) {
        return new UserDeletionInfo(
                taskQueryService.countByUserIdAndStatus(userId, TaskStatus.COMPLETED),
                commentQueryService.countByUserId(userId),
                taskQueryService.countAssignedTasks(userId),
                recurringTaskTemplateRepository.countByCreatedById(userId),
                projectQueryService.isSoleOwnerOfAnyProject(userId));
    }

    public boolean canDelete(UUID userId) {
        return getDeletionInfo(userId).canDelete();
    }

    public boolean canDisable(UUID userId) {
        return getDeletionInfo(userId).canDisable();
    }
}
