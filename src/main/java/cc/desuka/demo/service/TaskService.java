package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.TaskAssignedEvent;
import cc.desuka.demo.event.TaskChangeEvent;
import cc.desuka.demo.event.TaskUpdatedEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.model.ChecklistItem;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagService tagService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public TaskService(
            TaskRepository taskRepository,
            TagService tagService,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.taskRepository = taskRepository;
        this.tagService = tagService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Task.class, id));
    }

    // tagIds and assigneeId come from the caller (API or web controller).
    // The mapper cannot do DB lookups, so that responsibility lives here.
    public Task createTask(Task task, List<Long> tagIds, Long assigneeId) {
        return createTask(task, tagIds, assigneeId, null, null);
    }

    public Task createTask(
            Task task,
            List<Long> tagIds,
            Long assigneeId,
            List<String> checklistTexts,
            List<Boolean> checklistChecked) {
        if (task.getProject() == null) {
            throw new IllegalStateException("Task must belong to a project");
        }
        task.setTags(tagService.findAllByIds(tagIds));
        task.setUser(userService.findUserById(assigneeId));
        updateCompletedAt(task, null);
        applyChecklist(task, checklistTexts, checklistChecked);
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        User actor = SecurityUtils.getCurrentUser();
        eventPublisher.publishEvent(new TaskAssignedEvent(saved, actor));
        eventPublisher.publishEvent(new TaskChangeEvent("created", saved.getId(), actorId(actor)));
        return saved;
    }

    public Task updateTask(
            Long id, Task taskDetails, List<Long> tagIds, Long assigneeId, Long expectedVersion) {
        return updateTask(id, taskDetails, tagIds, assigneeId, expectedVersion, null, null);
    }

    public Task updateTask(
            Long id,
            Task taskDetails,
            List<Long> tagIds,
            Long assigneeId,
            Long expectedVersion,
            List<String> checklistTexts,
            List<Boolean> checklistChecked) {
        Task task = getTaskById(id);
        if (expectedVersion != null && !expectedVersion.equals(task.getVersion())) {
            throw new StaleDataException(Task.class, id);
        }
        Map<String, Object> before = task.toAuditSnapshot();
        User previousUser = task.getUser();

        TaskStatus previousStatus = task.getStatus();
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setStatus(taskDetails.getStatus());
        task.setPriority(taskDetails.getPriority());
        task.setStartDate(taskDetails.getStartDate());
        task.setDueDate(taskDetails.getDueDate());
        task.setTags(tagService.findAllByIds(tagIds));
        updateCompletedAt(task, previousStatus);
        // Reassigning an in-progress task resets status to OPEN — new assignee hasn't started
        User newUser = userService.findUserById(assigneeId);
        if (task.getStatus() == TaskStatus.IN_PROGRESS
                && newUser != null
                && (task.getUser() == null || !task.getUser().getId().equals(newUser.getId()))) {
            task.setStatus(TaskStatus.OPEN);
        }
        task.setUser(newUser);
        applyChecklist(task, checklistTexts, checklistChecked);

        Task saved = taskRepository.save(task);

        Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.TASK_UPDATED,
                            Task.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
        }

        User actor = SecurityUtils.getCurrentUser();
        boolean assignmentChanged =
                newUser != null
                        && (previousUser == null || !previousUser.getId().equals(newUser.getId()));
        if (assignmentChanged) {
            eventPublisher.publishEvent(new TaskAssignedEvent(saved, actor));
        }
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(new TaskUpdatedEvent(saved, actor));
        }
        eventPublisher.publishEvent(new TaskChangeEvent("updated", saved.getId(), actorId(actor)));
        return saved;
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException(messages.get("task.delete.completed"));
        }

        String snapshot = AuditDetails.toJson(task.toAuditSnapshot());
        taskRepository.delete(task);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.TASK_DELETED,
                        Task.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
        eventPublisher.publishEvent(
                new TaskChangeEvent("deleted", id, actorId(SecurityUtils.getCurrentUser())));
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

    // ── Single-project queries (for /projects/{id}) ─────────────────────

    public Page<Task> searchAndFilterTasks(
            Long projectId,
            String keyword,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tagIds,
            Pageable pageable) {
        return taskRepository.findAll(
                TaskSpecifications.build(
                        projectId,
                        keyword,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tagIds),
                pageable);
    }

    public Page<Task> searchAndFilterTasks(
            Long projectId,
            String keyword,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tagIds,
            Pageable pageable,
            LocalDate dueDateFrom,
            LocalDate dueDateTo) {
        return taskRepository.findAll(
                TaskSpecifications.build(
                        projectId,
                        keyword,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tagIds,
                        dueDateFrom,
                        dueDateTo),
                pageable);
    }

    // ── Single-project queries (for /projects/{id}) ────────────────────

    public Page<Task> searchAndFilterTasksForProject(
            Long projectId,
            String keyword,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tagIds,
            Pageable pageable) {
        return taskRepository.findAll(
                TaskSpecifications.build(
                        projectId,
                        keyword,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tagIds),
                pageable);
    }

    public Page<Task> searchAndFilterTasksForProject(
            Long projectId,
            String keyword,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tagIds,
            Pageable pageable,
            LocalDate dueDateFrom,
            LocalDate dueDateTo) {
        return taskRepository.findAll(
                TaskSpecifications.build(
                        projectId,
                        keyword,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tagIds,
                        dueDateFrom,
                        dueDateTo),
                pageable);
    }

    // ── Cross-project queries (for /tasks, /api/tasks) ─────────────────

    public Page<Task> searchAndFilterTasksForProjects(
            List<Long> accessibleProjectIds,
            String keyword,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tagIds,
            Pageable pageable) {
        return taskRepository.findAll(
                TaskSpecifications.buildForProjects(
                        accessibleProjectIds,
                        keyword,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tagIds),
                pageable);
    }

    public Page<Task> searchAndFilterTasksForProjects(
            List<Long> accessibleProjectIds,
            String keyword,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tagIds,
            Pageable pageable,
            LocalDate dueDateFrom,
            LocalDate dueDateTo) {
        return taskRepository.findAll(
                TaskSpecifications.buildForProjects(
                        accessibleProjectIds,
                        keyword,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tagIds,
                        dueDateFrom,
                        dueDateTo),
                pageable);
    }

    public long countByUserAndStatus(User user, TaskStatus status) {
        return taskRepository.countByUserAndStatus(user, status);
    }

    public long countByUserOverdue(User user) {
        return taskRepository.countByUserAndDueDateBeforeAndStatusNotIn(
                user, LocalDate.now(), TaskStatus.terminalStatuses());
    }

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

    // ── Project-scoped counts (for dashboard team stats) ─────────────────

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

    // Advance status: BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED → OPEN
    // CANCELLED is not part of the cycle — it's a separate action.
    public Task advanceStatus(Long id) {
        Task task = getTaskById(id);
        Map<String, Object> before = task.toAuditSnapshot();
        TaskStatus previousStatus = task.getStatus();
        TaskStatus next =
                switch (previousStatus) {
                    case BACKLOG -> TaskStatus.OPEN;
                    case OPEN -> TaskStatus.IN_PROGRESS;
                    case IN_PROGRESS -> TaskStatus.IN_REVIEW;
                    case IN_REVIEW -> TaskStatus.COMPLETED;
                    case COMPLETED -> TaskStatus.OPEN;
                    case CANCELLED -> TaskStatus.OPEN;
                };
        task.setStatus(next);
        updateCompletedAt(task, previousStatus);
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.TASK_UPDATED,
                        Task.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(AuditDetails.diff(before, saved.toAuditSnapshot()))));
        User actor = SecurityUtils.getCurrentUser();
        eventPublisher.publishEvent(new TaskUpdatedEvent(saved, actor));
        eventPublisher.publishEvent(new TaskChangeEvent("updated", saved.getId(), actorId(actor)));
        return saved;
    }

    private static long actorId(User user) {
        return user != null ? user.getId() : 0L;
    }

    private void applyChecklist(Task task, List<String> texts, List<Boolean> checked) {
        task.getChecklistItems().clear();
        if (texts == null) return;
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.isBlank()) continue;
            ChecklistItem item = new ChecklistItem(text.trim(), i);
            item.setChecked(
                    checked != null && i < checked.size() && Boolean.TRUE.equals(checked.get(i)));
            item.setTask(task);
            task.getChecklistItems().add(item);
        }
    }

    private void updateCompletedAt(Task task, TaskStatus previousStatus) {
        if (task.getStatus() == TaskStatus.COMPLETED && previousStatus != TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (task.getStatus() != TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
        }
    }
}
