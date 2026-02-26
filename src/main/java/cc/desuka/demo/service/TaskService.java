package cc.desuka.demo.service;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskFilter;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.TaskSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TaskService {

  private final TaskRepository taskRepository;

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
}