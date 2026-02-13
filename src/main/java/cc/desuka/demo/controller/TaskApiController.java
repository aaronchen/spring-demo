package cc.desuka.demo.controller;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
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

  // @GetMapping("/{id}")
  // public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
  // try {
  // Task task = taskService.getTaskById(id);
  // return ResponseEntity.ok(task); // 200 OK
  // } catch (RuntimeException e) {
  // return ResponseEntity.notFound().build(); // 404
  // }
  // }

  // POST /api/tasks
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Task createTask(@Valid @RequestBody Task task) {
    return taskService.createTask(task);
  }

  // @PostMapping
  // public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
  // Task created = taskService.createTask(task);
  // return ResponseEntity
  // .status(HttpStatus.CREATED) // 201
  // .body(created);
  // }

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