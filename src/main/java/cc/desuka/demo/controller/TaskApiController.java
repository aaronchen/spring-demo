package cc.desuka.demo.controller;

import cc.desuka.demo.dto.TaskRequest;
import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.mapper.TaskMapper;
import cc.desuka.demo.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

  private final TaskService taskService;
  private final TaskMapper taskMapper;

  public TaskApiController(TaskService taskService, TaskMapper taskMapper) {
    this.taskService = taskService;
    this.taskMapper = taskMapper;
  }

  // GET /api/tasks
  @GetMapping
  public List<TaskResponse> getAllTasks() {
    return taskMapper.toResponseList(taskService.getAllTasks());
  }

  // GET /api/tasks/5
  @GetMapping("/{id}")
  public TaskResponse getTaskById(@PathVariable Long id) {
    return taskMapper.toResponse(taskService.getTaskById(id));
  }

  // POST /api/tasks
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
    return taskMapper.toResponse(taskService.createTask(taskMapper.toEntity(request)));
  }

  // PUT /api/tasks/5
  @PutMapping("/{id}")
  public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
    return taskMapper.toResponse(taskService.updateTask(id, taskMapper.toEntity(request)));
  }

  // DELETE /api/tasks/5
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
  }

  // GET /api/tasks/search?keyword=spring
  @GetMapping("/search")
  public List<TaskResponse> searchTasks(@RequestParam String keyword) {
    return taskMapper.toResponseList(taskService.searchTasks(keyword));
  }

  // GET /api/tasks/incomplete
  @GetMapping("/incomplete")
  public List<TaskResponse> getIncompleteTasks() {
    return taskMapper.toResponseList(taskService.getIncompleteTasks());
  }

  // PATCH /api/tasks/5/toggle
  @PatchMapping("/{id}/toggle")
  public TaskResponse toggleComplete(@PathVariable Long id) {
    return taskMapper.toResponse(taskService.toggleComplete(id));
  }
}