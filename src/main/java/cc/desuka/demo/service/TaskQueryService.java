package cc.desuka.demo.service;

import cc.desuka.demo.dto.TaskItem;
import cc.desuka.demo.dto.TaskSearchCriteria;
import cc.desuka.demo.dto.UserTaskCounts;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only task lookups, counts, and dependency queries. Counterpart to {@link TaskService}
 * (writes).
 */
@Service
@Transactional(readOnly = true)
public class TaskQueryService {

    private final TaskRepository taskRepository;

    public TaskQueryService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // ── Single-entity lookups ─────────────────────────────────────────────

    public Task getTaskById(UUID id) {
        return taskRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Task.class, id));
    }

    public List<Task> getTasksByIds(List<UUID> ids) {
        return taskRepository.findAllById(ids);
    }

    public Task getTaskWithDependencies(UUID id) {
        Task task =
                taskRepository
                        .findWithDependenciesById(id)
                        .orElseThrow(() -> new EntityNotFoundException(Task.class, id));
        // Force-initialize checklistItems — not in the entity graph to avoid
        // MultipleBagFetchException
        task.getChecklistItems().size();
        return task;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getIncompleteTasks() {
        return taskRepository.findByStatusNotIn(TaskStatus.terminalStatuses());
    }

    public List<Task> getIncompleteTasks(List<UUID> accessibleProjectIds) {
        if (accessibleProjectIds == null) {
            return taskRepository.findByStatusNotIn(TaskStatus.terminalStatuses());
        }
        return taskRepository.findByProjectIdInAndStatusNotIn(
                accessibleProjectIds, TaskStatus.terminalStatuses());
    }

    public List<Task> searchTasks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return taskRepository.findAll();
        }
        return taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword);
    }

    public List<Task> searchTasks(String keyword, List<UUID> accessibleProjectIds) {
        List<Task> tasks = searchTasks(keyword);
        return filterByAccessibleProjects(tasks, accessibleProjectIds);
    }

    // ── Search and filter ─────────────────────────────────────────────────

    public Page<Task> searchTasks(TaskSearchCriteria criteria, Pageable pageable) {
        return taskRepository.findAll(TaskSpecifications.build(criteria), pageable);
    }

    // ── Counts (user-scoped) ──────────────────────────────────────────────

    public long countByUserAndStatus(User user, TaskStatus status) {
        return taskRepository.countByUserAndStatus(user, status);
    }

    public long countByUserIdAndStatus(UUID userId, TaskStatus status) {
        return taskRepository.countByUserIdAndStatus(userId, status);
    }

    public long countAssignedTasks(User user) {
        return taskRepository.countByUser(user);
    }

    public long countAssignedTasks(UUID userId) {
        return taskRepository.countByUserId(userId);
    }

    public long countByUserOverdue(User user) {
        return taskRepository.countByUserAndDueDateBeforeAndStatusNotIn(
                user, LocalDate.now(), TaskStatus.terminalStatuses());
    }

    /**
     * Dashboard aggregate: status counts + overdue for a user in 2 queries (one group-by + one
     * overdue count) instead of 5 individual counts.
     */
    public UserTaskCounts countsByUser(User user) {
        Map<TaskStatus, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : taskRepository.countGroupedByStatusForUser(user)) {
            byStatus.put((TaskStatus) row[0], (Long) row[1]);
        }

        return new UserTaskCounts(
                byStatus.getOrDefault(TaskStatus.OPEN, 0L),
                byStatus.getOrDefault(TaskStatus.IN_PROGRESS, 0L),
                byStatus.getOrDefault(TaskStatus.IN_REVIEW, 0L),
                byStatus.getOrDefault(TaskStatus.COMPLETED, 0L),
                countByUserOverdue(user));
    }

    // ── Counts (global) ───────────────────────────────────────────────────

    public long countByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    public long countOverdue() {
        return taskRepository.countByDueDateBeforeAndStatusNotIn(
                LocalDate.now(), TaskStatus.terminalStatuses());
    }

    public long countAll() {
        return taskRepository.count();
    }

    // ── Counts (project-scoped) ───────────────────────────────────────────

    public long countForProjects(List<UUID> projectIds) {
        return taskRepository.count(TaskSpecifications.withProjectIds(projectIds));
    }

    public long countByStatusForProjects(List<UUID> projectIds, TaskStatus status) {
        return taskRepository.count(
                TaskSpecifications.withProjectIds(projectIds)
                        .and(
                                TaskSpecifications.withStatusFilter(
                                        TaskStatusFilter.valueOf(status.name()))));
    }

    public long countOverdueForProjects(List<UUID> projectIds) {
        return taskRepository.count(
                TaskSpecifications.withProjectIds(projectIds)
                        .and(TaskSpecifications.withOverdue(true)));
    }

    public long countForProject(UUID projectId) {
        return taskRepository.count(TaskSpecifications.withProjectId(projectId));
    }

    public long countByStatusForProject(UUID projectId, TaskStatus status) {
        return taskRepository.count(
                TaskSpecifications.withProjectId(projectId)
                        .and(
                                TaskSpecifications.withStatusFilter(
                                        TaskStatusFilter.valueOf(status.name()))));
    }

    public long countOverdueForProject(UUID projectId) {
        return taskRepository.count(
                TaskSpecifications.withProjectId(projectId)
                        .and(TaskSpecifications.withOverdue(true)));
    }

    // ── Dashboard helpers ─────────────────────────────────────────────────

    public List<Task> getRecentTasksByUser(User user) {
        return taskRepository.findTop5ByUserOrderByCreatedAtDesc(user);
    }

    public List<Task> getDueSoon(User user) {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        return taskRepository.findByUserAndDueDateBetweenAndStatusNotIn(
                user, today, endOfWeek, TaskStatus.terminalStatuses());
    }

    public List<Task> getTasksDueOn(LocalDate date) {
        return taskRepository.findByDueDateAndStatusNotIn(date, TaskStatus.terminalStatuses());
    }

    public Map<UUID, String> getTitlesByIds(List<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return taskRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Task::getId, Task::getTitle));
    }

    // ── Dependency picker search ──────────────────────────────────────────

    public List<TaskItem> searchByTitleForDependency(
            UUID projectId, String query, List<UUID> excludeTaskIds) {
        Specification<Task> spec = TaskSpecifications.withProjectId(projectId);
        if (query != null && !query.isBlank()) {
            spec = spec.and(TaskSpecifications.withTitleContaining(query));
        }
        if (excludeTaskIds != null && !excludeTaskIds.isEmpty()) {
            spec = spec.and((root, q, cb) -> cb.not(root.get(Task.FIELD_ID).in(excludeTaskIds)));
        }
        Sort sort = Sort.by(Sort.Direction.DESC, Task.FIELD_CREATED_AT);
        return taskRepository.findAll(spec, sort).stream()
                .map(t -> new TaskItem(t.getId(), t.getTitle(), t.getStatus().name()))
                .toList();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private List<Task> filterByAccessibleProjects(
            List<Task> tasks, List<UUID> accessibleProjectIds) {
        if (accessibleProjectIds == null) return tasks;
        return tasks.stream()
                .filter(t -> accessibleProjectIds.contains(t.getProject().getId()))
                .toList();
    }

    // ── Dependency queries ─────────────────────────────────────────────

    /** Returns non-terminal tasks that block the given task. */
    public List<Task> getActiveBlockers(UUID taskId) {
        Task task = getTaskWithDependencies(taskId);
        return task.getBlockedBy().stream().filter(t -> !t.getStatus().isTerminal()).toList();
    }

    /** Returns true if the task has at least one non-terminal blocker. */
    public boolean hasActiveBlockers(UUID taskId) {
        Task task = getTaskWithDependencies(taskId);
        return task.getBlockedBy().stream().anyMatch(t -> !t.getStatus().isTerminal());
    }

    // ── Grouping ──────────────────────────────────────────────────────────

    public Map<TaskStatus, List<Task>> groupByStatus(List<Task> tasks) {
        Map<TaskStatus, List<Task>> map = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            map.put(status, new ArrayList<>());
        }
        for (Task task : tasks) {
            map.get(task.getStatus()).add(task);
        }
        return map;
    }
}
