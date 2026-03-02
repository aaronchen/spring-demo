package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskFilter;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import cc.desuka.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

  private final TaskRepository taskRepository;
  private final TagRepository tagRepository;
  private final UserRepository userRepository;

  public TaskService(TaskRepository taskRepository, TagRepository tagRepository, UserRepository userRepository) {
    this.taskRepository = taskRepository;
    this.tagRepository = tagRepository;
    this.userRepository = userRepository;
  }

  public List<Task> getAllTasks() {
    return taskRepository.findAll();
  }

  public Task getTaskById(Long id) {
    return taskRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Task", id));
  }

  // tagIds and userId come from the caller (API or web controller).
  // The mapper cannot do DB lookups, so that responsibility lives here.
  public Task createTask(Task task, List<Long> tagIds, Long userId) {
    task.setTags(resolveTags(tagIds));
    task.setUser(resolveUser(userId));
    return taskRepository.save(task);
  }

  public Task updateTask(Long id, Task taskDetails, List<Long> tagIds, Long userId) {
    Task task = getTaskById(id);
    task.setTitle(taskDetails.getTitle());
    task.setDescription(taskDetails.getDescription());
    task.setCompleted(taskDetails.isCompleted());
    task.setTags(resolveTags(tagIds));
    task.setUser(resolveUser(userId));
    return taskRepository.save(task);
  }

  public void deleteTask(Long id) {
    Task task = getTaskById(id);
    taskRepository.delete(task);
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

  public Page<Task> searchAndFilterTasks(String keyword, TaskFilter filter, Pageable pageable) {
    return taskRepository.findAll(TaskSpecifications.build(keyword, filter), pageable);
  }

  public Task toggleComplete(Long id) {
    Task task = getTaskById(id);
    task.setCompleted(!task.isCompleted());
    return taskRepository.save(task);
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
