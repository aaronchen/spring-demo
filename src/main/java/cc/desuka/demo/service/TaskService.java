package cc.desuka.demo.service;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.util.List;

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

  public List<Task> searchAndFilterTasks(String keyword, String filter) {
    List<Task> tasks;

    // First, get filtered tasks based on completion status
    if ("completed".equals(filter)) {
      tasks = taskRepository.findByCompleted(true);
    } else if ("incomplete".equals(filter)) {
      tasks = taskRepository.findByCompleted(false);
    } else {
      // "all" or any other value
      tasks = taskRepository.findAll();
    }

    // Then apply search filter if keyword is provided
    if (keyword != null && !keyword.trim().isEmpty()) {
      String lowerKeyword = keyword.toLowerCase();
      tasks = tasks.stream()
          .filter(task ->
              (task.getTitle() != null && task.getTitle().toLowerCase().contains(lowerKeyword)) ||
              (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerKeyword))
          )
          .toList();
    }

    return tasks;
  }

  public Task toggleComplete(Long id) {
    Task task = getTaskById(id);
    task.setCompleted(!task.isCompleted());
    return taskRepository.save(task);
  }
}