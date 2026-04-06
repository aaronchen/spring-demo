package cc.desuka.demo.service;

import cc.desuka.demo.dto.TaskSearchCriteria;
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
 * Read-only task lookups and cross-service task operations. Breaks circular dependency: TaskService
 * → UserService/CommentService → TaskService.
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

    public long countAssignedTasks(User user) {
        return taskRepository.countByUser(user);
    }

    public long countByUserOverdue(User user) {
        return taskRepository.countByUserAndDueDateBeforeAndStatusNotIn(
                user, LocalDate.now(), TaskStatus.terminalStatuses());
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

    public List<Map<String, Object>> searchByTitleForDependency(
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
                .map(
                        t ->
                                Map.<String, Object>of(
                                        "id", t.getId(),
                                        "title", t.getTitle(),
                                        "status", t.getStatus().name()))
                .toList();
    }

    // ── Grouping ──────────────────────────────────────────────────────────

    private List<Task> filterByAccessibleProjects(
            List<Task> tasks, List<UUID> accessibleProjectIds) {
        if (accessibleProjectIds == null) return tasks;
        return tasks.stream()
                .filter(t -> accessibleProjectIds.contains(t.getProject().getId()))
                .toList();
    }

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
