package cc.desuka.demo.controller;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

  private final TaskService taskService;

  public TaskApiController(TaskService taskService) {
    this.taskService = taskService;
  }

  // GET /api/tasks
  @GetMapping
  public List<Task> getAllTasks() {
    return taskService.getAllTasks();
  }

  // GET /api/tasks/5
  @GetMapping("/{id}")
  public Task getTaskById(@PathVariable Long id) {
    return taskService.getTaskById(id);
  }

  // POST /api/tasks
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Task createTask(@Valid @RequestBody Task task) {
    return taskService.createTask(task);
  }

  // PUT /api/tasks/5
  @PutMapping("/{id}")
  public Task updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
    return taskService.updateTask(id, task);
  }

  // DELETE /api/tasks/5
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
  }

  // GET /api/tasks/search?keyword=spring
  @GetMapping("/search")
  public List<Task> searchTasks(@RequestParam String keyword) {
    return taskService.searchTasks(keyword);
  }

  // GET /api/tasks/incomplete
  @GetMapping("/incomplete")
  public List<Task> getIncompleteTasks() {
    return taskService.getIncompleteTasks();
  }

  // PATCH /api/tasks/5/toggle
  @PatchMapping("/{id}/toggle")
  public Task toggleComplete(@PathVariable Long id) {
    return taskService.toggleComplete(id);
  }
}