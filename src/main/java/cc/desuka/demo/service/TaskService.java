package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.exception.StaleDataException;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import cc.desuka.demo.repository.UserRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

  private final TaskRepository taskRepository;
  private final TagRepository tagRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TaskService(TaskRepository taskRepository, TagRepository tagRepository,
                     UserRepository userRepository, CommentRepository commentRepository,
                     ApplicationEventPublisher eventPublisher) {
    this.taskRepository = taskRepository;
    this.tagRepository = tagRepository;
    this.userRepository = userRepository;
    this.commentRepository = commentRepository;
    this.eventPublisher = eventPublisher;
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
    task.setTags(resolveTags(tagIds));
    task.setUser(resolveUser(userId));
    Task saved = taskRepository.save(task);
    eventPublisher.publishEvent(new AuditEvent(
        AuditEvent.TASK_CREATED, Task.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
        AuditDetails.toJson(saved.toAuditSnapshot())));
    return saved;
  }

  public Task updateTask(Long id, Task taskDetails, List<Long> tagIds, Long userId, Long expectedVersion) {
    Task task = getTaskById(id);
    if (expectedVersion != null && !expectedVersion.equals(task.getVersion())) {
      throw new StaleDataException(Task.class, id);
    }
    Map<String, Object> before = task.toAuditSnapshot();

    task.setTitle(taskDetails.getTitle());
    task.setDescription(taskDetails.getDescription());
    task.setCompleted(taskDetails.isCompleted());
    task.setPriority(taskDetails.getPriority());
    task.setDueDate(taskDetails.getDueDate());
    task.setTags(resolveTags(tagIds));
    task.setUser(resolveUser(userId));

    Task saved = taskRepository.save(task);

    Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
    if (!changes.isEmpty()) {
      eventPublisher.publishEvent(new AuditEvent(
          AuditEvent.TASK_UPDATED, Task.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
          AuditDetails.toJson(changes)));
    }
    return saved;
  }

  public void deleteTask(Long id) {
    Task task = getTaskById(id);
    String snapshot = AuditDetails.toJson(task.toAuditSnapshot());
    commentRepository.deleteByTaskId(id);
    taskRepository.delete(task);
    eventPublisher.publishEvent(new AuditEvent(
        AuditEvent.TASK_DELETED, Task.class, id, SecurityUtils.getCurrentPrincipal(),
        snapshot));
  }

  public List<Task> getIncompleteTasks() {
    return taskRepository.findByCompleted(false);
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

  public Task toggleComplete(Long id) {
    Task task = getTaskById(id);
    Map<String, Object> before = task.toAuditSnapshot();
    task.setCompleted(!task.isCompleted());
    Task saved = taskRepository.save(task);
    eventPublisher.publishEvent(new AuditEvent(
        AuditEvent.TASK_UPDATED, Task.class, saved.getId(), SecurityUtils.getCurrentPrincipal(),
        AuditDetails.toJson(AuditDetails.diff(before, saved.toAuditSnapshot()))));
    return saved;
  }

  // Resolves a list of tag IDs to Tag entities.
  // null or empty input → empty list (all tags removed from the task).
  // Unknown IDs are silently ignored by findAllById.
  // This keeps the mapper pure (no DB access) and controllers thin (no lookups).
  private List<Tag> resolveTags(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) return new ArrayList<>();
    return tagRepository.findAllById(tagIds);
  }

  // Resolves a user ID to a User entity.
  // null input → null (task becomes unassigned).
  // Unknown ID → null (silently ignored — same pattern as resolveTags).
  private User resolveUser(Long userId) {
    if (userId == null) return null;
    return userRepository.findById(userId).orElse(null);
  }
}
