package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.event.TaskAssignedEvent;
import cc.desuka.demo.event.TaskChangeEvent;
import cc.desuka.demo.event.TaskUpdatedEvent;
import cc.desuka.demo.exception.BlockedTaskException;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.model.ChecklistItem;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskQueryService taskQueryService;
    private final TaskDependencyService taskDependencyService;
    private final TagService tagService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public TaskService(
            TaskRepository taskRepository,
            TaskQueryService taskQueryService,
            TaskDependencyService taskDependencyService,
            TagService tagService,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.taskRepository = taskRepository;
        this.taskQueryService = taskQueryService;
        this.taskDependencyService = taskDependencyService;
        this.tagService = tagService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
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
        eventPublisher.publishEvent(
                new TaskChangeEvent(TaskChangeEvent.ACTION_CREATED, saved.getId(), actorId(actor)));
        return saved;
    }

    public Task updateTask(
            Long id, Task taskDetails, List<Long> tagIds, Long assigneeId, Long expectedVersion) {
        return updateTask(
                id, taskDetails, tagIds, assigneeId, expectedVersion, null, null, null, null);
    }

    public Task updateTask(
            Long id,
            Task taskDetails,
            List<Long> tagIds,
            Long assigneeId,
            Long expectedVersion,
            List<String> checklistTexts,
            List<Boolean> checklistChecked) {
        return updateTask(
                id,
                taskDetails,
                tagIds,
                assigneeId,
                expectedVersion,
                checklistTexts,
                checklistChecked,
                null,
                null);
    }

    public Task updateTask(
            Long id,
            Task taskDetails,
            List<Long> tagIds,
            Long assigneeId,
            Long expectedVersion,
            List<String> checklistTexts,
            List<Boolean> checklistChecked,
            List<Long> blockedByIds,
            List<Long> blocksIds) {
        Task task = taskQueryService.getTaskById(id);
        if (expectedVersion != null && !expectedVersion.equals(task.getVersion())) {
            throw new StaleDataException(Task.class, id);
        }
        Map<String, Object> before = task.toAuditSnapshot();
        User previousUser = task.getUser();

        TaskStatus previousStatus = task.getStatus();
        if (taskDetails.getStatus() != previousStatus) {
            requireNotBlocked(id, taskDetails.getStatus());
        }
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
        taskDependencyService.reconcile(task, blockedByIds, blocksIds);

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
        eventPublisher.publishEvent(
                new TaskChangeEvent(TaskChangeEvent.ACTION_UPDATED, saved.getId(), actorId(actor)));
        return saved;
    }

    public void deleteTask(Long id) {
        Task task = taskQueryService.getTaskById(id);

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
                new TaskChangeEvent(
                        TaskChangeEvent.ACTION_DELETED,
                        id,
                        actorId(SecurityUtils.getCurrentUser())));
    }

    public Task updateField(Long id, String field, String value) {
        Task task = taskQueryService.getTaskById(id);
        Map<String, Object> before = task.toAuditSnapshot();

        switch (field) {
            case Task.FIELD_TITLE -> task.setTitle(value);
            case Task.FIELD_DESCRIPTION ->
                    task.setDescription(value != null && !value.isBlank() ? value : null);
            case Task.FIELD_PRIORITY -> task.setPriority(Priority.valueOf(value));
            case Task.FIELD_STATUS -> {
                TaskStatus newStatus = TaskStatus.valueOf(value);
                requireNotBlocked(id, newStatus);
                TaskStatus previousStatus = task.getStatus();
                task.setStatus(newStatus);
                updateCompletedAt(task, previousStatus);
            }
            case Task.FIELD_DUE_DATE ->
                    task.setDueDate(
                            value != null && !value.isBlank() ? LocalDate.parse(value) : null);
            case Task.FIELD_USER_ID ->
                    task.setUser(
                            value != null && !value.isBlank()
                                    ? userService.findUserById(Long.valueOf(value))
                                    : null);
            default ->
                    throw new IllegalArgumentException(
                            messages.get("task.field.notEditable", field));
        }

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
            User actor = SecurityUtils.getCurrentUser();
            eventPublisher.publishEvent(new TaskUpdatedEvent(saved, actor));
            eventPublisher.publishEvent(
                    new TaskChangeEvent(
                            TaskChangeEvent.ACTION_UPDATED, saved.getId(), actorId(actor)));
        }
        return saved;
    }

    public Task setStatus(Long id, TaskStatus newStatus) {
        Task task = taskQueryService.getTaskById(id);
        if (task.getStatus() == newStatus) {
            return task;
        }
        requireNotBlocked(id, newStatus);
        Map<String, Object> before = task.toAuditSnapshot();
        TaskStatus previousStatus = task.getStatus();
        task.setStatus(newStatus);
        updateCompletedAt(task, previousStatus);
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
        eventPublisher.publishEvent(new TaskUpdatedEvent(saved, actor));
        eventPublisher.publishEvent(
                new TaskChangeEvent(TaskChangeEvent.ACTION_UPDATED, saved.getId(), actorId(actor)));
        return saved;
    }

    // Advance status: BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED → OPEN
    // CANCELLED is not part of the cycle — it's a separate action.
    public Task advanceStatus(Long id) {
        Task task = taskQueryService.getTaskById(id);
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
        requireNotBlocked(id, next);
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
        eventPublisher.publishEvent(
                new TaskChangeEvent(TaskChangeEvent.ACTION_UPDATED, saved.getId(), actorId(actor)));
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

    private void requireNotBlocked(Long taskId, TaskStatus newStatus) {
        // Only block completion — other transitions are allowed even when blocked
        if (newStatus != TaskStatus.COMPLETED) {
            return;
        }
        if (taskDependencyService.hasActiveBlockers(taskId)) {
            List<String> blockerNames =
                    taskDependencyService.getActiveBlockers(taskId).stream()
                            .map(Task::getTitle)
                            .toList();
            throw new BlockedTaskException(
                    messages.get(
                            "task.dependency.blocked.transition", String.join(", ", blockerNames)),
                    blockerNames);
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
