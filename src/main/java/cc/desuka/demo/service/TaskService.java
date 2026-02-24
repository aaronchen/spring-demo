package cc.desuka.demo.service;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskFilter;
import cc.desuka.demo.repository.TaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

  private final TaskRepository taskRepository;

  // Constructor injection (the Spring way)
  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public List<Task> getAllTasks() {
    return taskRepository.findAll();
  }

  public List<Task> getAllTasks(Sort sort) {
    return taskRepository.findAll(sort);
  }

  public Task getTaskById(Long id) {
    return taskRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
  }

  public Task createTask(Task task) {
    return taskRepository.save(task);
  }

  public Task updateTask(Long id, Task taskDetails) {
    Task task = getTaskById(id);
    task.setTitle(taskDetails.getTitle());
    task.setDescription(taskDetails.getDescription());
    task.setCompleted(taskDetails.isCompleted());
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
    // If keyword is null or empty, return all tasks
    if (keyword == null || keyword.trim().isEmpty()) {
      return taskRepository.findAll();
    }
    // Search in both title and description (case-insensitive)
    return taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
  }

  public List<Task> searchAndFilterTasks(String keyword, TaskFilter filter, Sort sort) {
    List<Task> tasks = taskRepository.findAll(sort);

    if (filter == TaskFilter.COMPLETED) {
      tasks = tasks.stream().filter(Task::isCompleted).collect(Collectors.toList());
    } else if (filter == TaskFilter.INCOMPLETE) {
      tasks = tasks.stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
    }

    if (keyword != null && !keyword.trim().isEmpty()) {
      String lower = keyword.toLowerCase();
      tasks = tasks.stream()
          .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(lower))
              || (t.getDescription() != null && t.getDescription().toLowerCase().contains(lower)))
          .collect(Collectors.toList());
    }

    return tasks;
  }

  public List<Task> searchAndFilterTasks(String keyword, TaskFilter filter) {
    return searchAndFilterTasks(keyword, filter, Sort.by(Sort.Direction.DESC, Task.FIELD_CREATED_AT));
  }

  public Task toggleComplete(Long id) {
    Task task = getTaskById(id);
    task.setCompleted(!task.isCompleted());
    return taskRepository.save(task);
  }
}