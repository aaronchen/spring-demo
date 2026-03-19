package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.TaskRequest;
import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.mapper.TaskMapper;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.security.AuthExpressions;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

  private final TaskService taskService;
  private final TaskMapper taskMapper;
  private final OwnershipGuard ownershipGuard;

  public TaskApiController(TaskService taskService,
                           TaskMapper taskMapper,
                           OwnershipGuard ownershipGuard) {
    this.taskService = taskService;
    this.taskMapper = taskMapper;
    this.ownershipGuard = ownershipGuard;
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
  // Regular users: task auto-assigned to caller (userId in body is ignored).
  // Admins: can optionally specify userId to assign task to another user.
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse createTask(@Valid @RequestBody TaskRequest request,
      @AuthenticationPrincipal CustomUserDetails currentDetails) {
    Long assigneeId = currentDetails.getUser().getId();
    if (request.getUserId() != null && AuthExpressions.isAdmin(currentDetails.getUser())) {
      assigneeId = request.getUserId();
    }
    return taskMapper.toResponse(
        taskService.createTask(taskMapper.toEntity(request), request.getTagIds(), assigneeId));
  }

  // PUT /api/tasks/5
  // Owner or admin only.
  @PutMapping("/{id}")
  public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request,
      @AuthenticationPrincipal CustomUserDetails currentDetails) {
    Task existing = taskService.getTaskById(id);
    ownershipGuard.requireAccess(existing, currentDetails);
    return taskMapper.toResponse(
        taskService.updateTask(id, taskMapper.toEntity(request), request.getTagIds(), request.getUserId(), request.getVersion()));
  }

  // DELETE /api/tasks/5
  // Owner or admin only.
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTask(@PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails currentDetails) {
    Task task = taskService.getTaskById(id);
    ownershipGuard.requireAccess(task, currentDetails);
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
  // Advances status: OPEN → IN_PROGRESS → COMPLETED → OPEN.
  // Open to all authenticated users (matches web UI behavior).
  @PatchMapping("/{id}/toggle")
  public TaskResponse advanceStatus(@PathVariable Long id) {
    return taskMapper.toResponse(taskService.advanceStatus(id));
  }
}
