package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.TaskRequest;
import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.mapper.TaskMapper;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.ProjectService;
import cc.desuka.demo.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final TaskMapper taskMapper;
    private final ProjectAccessGuard projectAccessGuard;

    public TaskApiController(
            TaskService taskService,
            ProjectService projectService,
            TaskMapper taskMapper,
            ProjectAccessGuard projectAccessGuard) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.taskMapper = taskMapper;
        this.projectAccessGuard = projectAccessGuard;
    }

    // GET /api/tasks
    // GET /api/tasks?page=0&size=20&sort=createdAt,desc
    // GET /api/tasks?search=spring&status=OPEN&priority=HIGH&overdue=true&userId=1&tags=1,2
    @GetMapping
    public Page<TaskResponse> getTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = TaskStatusFilter.DEFAULT) String status,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<Long> tags,
            @ParameterObject
                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        TaskStatusFilter statusFilter = TaskStatusFilter.from(status);
        List<Long> accessibleProjectIds =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        ? null
                        : projectService.getAccessibleProjectIds(currentDetails.getUser().getId());
        return taskService
                .searchAndFilterTasksForProjects(
                        accessibleProjectIds,
                        search,
                        statusFilter,
                        overdue,
                        priority,
                        userId,
                        tags,
                        pageable)
                .map(taskMapper::toResponse);
    }

    // GET /api/tasks/5
    @GetMapping("/{id}")
    public TaskResponse getTaskById(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskService.getTaskById(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        return taskMapper.toResponse(task);
    }

    // POST /api/tasks
    // projectId is required. Regular users: task auto-assigned to caller.
    // Admins: can optionally specify userId to assign task to another user.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireEditAccess(request.getProjectId(), currentDetails);
        Long assigneeId = currentDetails.getUser().getId();
        if (request.getUserId() != null && AuthExpressions.isAdmin(currentDetails.getUser())) {
            assigneeId = request.getUserId();
        }
        Task task = taskMapper.toEntity(request);
        task.setProject(projectService.getProjectById(request.getProjectId()));
        return taskMapper.toResponse(taskService.createTask(task, request.getTagIds(), assigneeId));
    }

    // PUT /api/tasks/5
    // Project EDITOR or OWNER (or admin) may update.
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task existing = taskService.getTaskById(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        return taskMapper.toResponse(
                taskService.updateTask(
                        id,
                        taskMapper.toEntity(request),
                        request.getTagIds(),
                        request.getUserId(),
                        request.getVersion()));
    }

    // DELETE /api/tasks/5
    // Task creator, project OWNER, or system admin may delete.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskService.getTaskById(id);
        requireDeleteAccess(task, currentDetails);
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
    @PatchMapping("/{id}/toggle")
    public TaskResponse advanceStatus(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskService.getTaskById(id);
        projectAccessGuard.requireEditAccess(task.getProject().getId(), currentDetails);
        return taskMapper.toResponse(taskService.advanceStatus(id));
    }

    private void requireDeleteAccess(Task task, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        if (task.getUser() != null
                && task.getUser().getId().equals(currentDetails.getUser().getId())) {
            return;
        }
        projectAccessGuard.requireOwnerAccess(task.getProject().getId(), currentDetails);
    }
}
