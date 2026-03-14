package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.dto.TaskChangeEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TaskService {



  private final TaskRepository taskRepository;
  private final TagService tagService;
  private final UserService userService;
  private final CommentService commentService;
  private final NotificationService notificationService;
  private final ApplicationEventPublisher eventPublisher;
  private final MessageSource messageSource;
  private final SimpMessagingTemplate messagingTemplate;

  public TaskService(TaskRepository taskRepository, 
                     TagService tagService,
                     UserService userService, CommentService commentService,
                     NotificationService notificationService,
                     ApplicationEventPublisher eventPublisher,
                     MessageSource messageSource,
                     SimpMessagingTemplate messagingTemplate) {
    this.taskRepository = taskRepository;
    this.tagService = tagService;
    this.userService = userService;
    this.commentService = commentService;
    this.notificationService = notificationService;
    this.eventPublisher = eventPublisher;
    this.messageSource = messageSource;
    this.messagingTemplate = messagingTemplate;
  }

  public List<Task> getAllTasks() {
    return taskRepository.findAll();
  }

  public Task getTaskById(Long id) {
    return taskRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(Task.class, id));
  }

  // tagIds and userId come from the caller (API or web controller).
  // The mapper cannot do DB lookups, so that responsibility lives here.
  public Task createTask(Task task, List<Long> tagIds, Long userId) {
    task.setTags(tagService.findAllByIds(tagIds));
    task.setUser(userService.findUserById(userId));
    Task saved = taskRepository.save(task);
    eventPublisher.publishEvent(new AuditEvent(
        AuditEvent.TASK_CREATED, Task.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
        AuditDetails.toJson(saved.toAuditSnapshot())));
    notifyAssignment(saved, SecurityUtils.getCurrentUser());
    broadcastTaskChange("created", saved.getId());
    return saved;
  }

  public Task updateTask(Long id, Task taskDetails, List<Long> tagIds, Long userId, Long expectedVersion) {
    Task task = getTaskById(id);
    if (expectedVersion != null && !expectedVersion.equals(task.getVersion())) {
      throw new StaleDataException(Task.class, id);
    }
    Map<String, Object> before = task.toAuditSnapshot();
    User previousUser = task.getUser();

    task.setTitle(taskDetails.getTitle());
    task.setDescription(taskDetails.getDescription());
    task.setStatus(taskDetails.getStatus());
    task.setPriority(taskDetails.getPriority());
    task.setDueDate(taskDetails.getDueDate());
    task.setTags(tagService.findAllByIds(tagIds));
    // Reassigning an in-progress task resets status to OPEN — new assignee hasn't started
    User newUser = userService.findUserById(userId);
    if (task.getStatus() == TaskStatus.IN_PROGRESS && newUser != null
        && (task.getUser() == null || !task.getUser().getId().equals(newUser.getId()))) {
      task.setStatus(TaskStatus.OPEN);
    }
    task.setUser(newUser);

    Task saved = taskRepository.save(task);

    Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
    if (!changes.isEmpty()) {
      eventPublisher.publishEvent(new AuditEvent(
          AuditEvent.TASK_UPDATED, Task.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
          AuditDetails.toJson(changes)));
    }

    // Notify if assignment changed to a different user
    boolean assignmentChanged = newUser != null
        && (previousUser == null || !previousUser.getId().equals(newUser.getId()));
    if (assignmentChanged) {
      notifyAssignment(saved, SecurityUtils.getCurrentUser());
    }
    broadcastTaskChange("updated", saved.getId());
    return saved;
  }

  public void deleteTask(Long id) {
    Task task = getTaskById(id);
    String snapshot = AuditDetails.toJson(task.toAuditSnapshot());
    commentService.deleteByTaskId(id);
    taskRepository.delete(task);
    eventPublisher.publishEvent(new AuditEvent(
        AuditEvent.TASK_DELETED, Task.class, id, SecurityUtils.getCurrentPrincipal(),
        snapshot));
    broadcastTaskChange("deleted", id);
  }

  public List<Task> getIncompleteTasks() {
    return taskRepository.findByStatusNot(TaskStatus.COMPLETED);
  }

  public List<Task> searchTasks(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return taskRepository.findAll();
    }
    return taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
  }

  public Page<Task> searchAndFilterTasks(String keyword, TaskStatusFilter statusFilter,
                                         boolean overdue, Priority priority,
                                         Long userId, List<Long> tagIds,
                                         Pageable pageable) {
    return taskRepository.findAll(TaskSpecifications.build(keyword, statusFilter, overdue, priority, userId, tagIds), pageable);
  }

  public long countByUserAndStatus(User user, TaskStatus status) {
    return taskRepository.countByUserAndStatus(user, status);
  }

  public long countByUserOverdue(User user) {
    return taskRepository.countByUserAndDueDateBeforeAndStatusNot(user, java.time.LocalDate.now(), TaskStatus.COMPLETED);
  }

  public long countByStatus(TaskStatus status) {
    return taskRepository.countByStatus(status);
  }

  public long countOverdue() {
    return taskRepository.countByDueDateBeforeAndStatusNot(java.time.LocalDate.now(), TaskStatus.COMPLETED);
  }

  public long countAll() {
    return taskRepository.count();
  }

  public List<Task> getRecentTasksByUser(User user) {
    return taskRepository.findTop5ByUserOrderByCreatedAtDesc(user);
  }

  public Map<Long, String> getTitlesByIds(List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    return taskRepository.findAllById(ids).stream()
        .collect(java.util.stream.Collectors.toMap(Task::getId, Task::getTitle));
  }

  // Advance status: OPEN → IN_PROGRESS → COMPLETED → OPEN
  public Task advanceStatus(Long id) {
    Task task = getTaskById(id);
    Map<String, Object> before = task.toAuditSnapshot();
    TaskStatus next = switch (task.getStatus()) {
      case OPEN -> TaskStatus.IN_PROGRESS;
      case IN_PROGRESS -> TaskStatus.COMPLETED;
      case COMPLETED -> TaskStatus.OPEN;
    };
    task.setStatus(next);
    Task saved = taskRepository.save(task);
    eventPublisher.publishEvent(new AuditEvent(
        AuditEvent.TASK_UPDATED, Task.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
        AuditDetails.toJson(AuditDetails.diff(before, saved.toAuditSnapshot()))));
    broadcastTaskChange("updated", saved.getId());
    return saved;
  }

  private void broadcastTaskChange(String action, Long taskId) {
    User current = SecurityUtils.getCurrentUser();
    long actorId = current != null ? current.getId() : 0L;
    messagingTemplate.convertAndSend("/topic/tasks",
        new TaskChangeEvent(action, taskId, actorId));
  }

  private void notifyAssignment(Task task, User actor) {
    User assignee = task.getUser();
    // Don't notify if assigning to self or unassigned
    if (assignee == null || (actor != null && actor.getId().equals(assignee.getId()))) {
      return;
    }
    String message = messageSource.getMessage("notification.task.assigned",
        new Object[]{actor != null ? actor.getName() : "System", task.getTitle()}, Locale.getDefault());
    notificationService.create(assignee, actor, NotificationType.TASK_ASSIGNED,
        message, "/tasks/" + task.getId() + "/edit");
  }
}
