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
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public Task getTaskById(Long id) {
        return taskRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Task.class, id));
    }

    public Task getTaskWithDependencies(Long id) {
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

    public List<Task> searchTasks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return taskRepository.findAll();
        }
        return taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword);
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
        return taskRepository.findByUser(user).size();
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

    public long countForProjects(List<Long> projectIds) {
        return taskRepository.count(TaskSpecifications.withProjectIds(projectIds));
    }

    public long countByStatusForProjects(List<Long> projectIds, TaskStatus status) {
        return taskRepository.count(
                TaskSpecifications.withProjectIds(projectIds)
                        .and(
                                TaskSpecifications.withStatusFilter(
                                        TaskStatusFilter.valueOf(status.name()))));
    }

    public long countOverdueForProjects(List<Long> projectIds) {
        return taskRepository.count(
                TaskSpecifications.withProjectIds(projectIds)
                        .and(TaskSpecifications.withOverdue(true)));
    }

    public long countForProject(Long projectId) {
        return taskRepository.count(TaskSpecifications.withProjectId(projectId));
    }

    public long countByStatusForProject(Long projectId, TaskStatus status) {
        return taskRepository.count(
                TaskSpecifications.withProjectId(projectId)
                        .and(
                                TaskSpecifications.withStatusFilter(
                                        TaskStatusFilter.valueOf(status.name()))));
    }

    public long countOverdueForProject(Long projectId) {
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

    public Map<Long, String> getTitlesByIds(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return taskRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Task::getId, Task::getTitle));
    }

    // ── Dependency picker search ──────────────────────────────────────────

    public List<Map<String, Object>> searchForDependency(
            Long projectId, String query, List<Long> excludeTaskIds) {
        return taskRepository
                .findAll(
                        TaskSpecifications.withProjectId(projectId),
                        Sort.by(Sort.Direction.DESC, Task.FIELD_CREATED_AT))
                .stream()
                .filter(t -> !excludeTaskIds.contains(t.getId()))
                .filter(
                        t ->
                                query == null
                                        || query.isBlank()
                                        || t.getTitle().toLowerCase().contains(query.toLowerCase()))
                .limit(20)
                .map(
                        t ->
                                Map.<String, Object>of(
                                        "id", t.getId(),
                                        "title", t.getTitle(),
                                        "status", t.getStatus().name()))
                .toList();
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

    // ── Write operations (cross-service) ──────────────────────────────────

    @Transactional
    public void unassignTasks(User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        for (Task task : tasks) {
            task.setUser(null);
            if (!task.getStatus().isTerminal()) {
                task.setStatus(TaskStatus.OPEN);
            }
        }
        taskRepository.saveAll(tasks);
    }

    @Transactional
    public void unassignTasksInProject(User user, Long projectId) {
        List<Task> tasks =
                taskRepository.findByUserAndProjectIdAndStatusNotIn(
                        user, projectId, TaskStatus.terminalStatuses());
        for (Task task : tasks) {
            task.setUser(null);
            task.setStatus(TaskStatus.OPEN);
        }
        taskRepository.saveAll(tasks);
    }
}
