package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.TaskListQuery;
import cc.desuka.demo.dto.TaskRequest;
import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.dto.TaskSearchCriteria;
import cc.desuka.demo.mapper.TaskMapper;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.ProjectQueryService;
import cc.desuka.demo.service.TaskQueryService;
import cc.desuka.demo.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
    private final TaskQueryService taskQueryService;
    private final ProjectQueryService projectQueryService;
    private final TaskMapper taskMapper;
    private final ProjectAccessGuard projectAccessGuard;

    public TaskApiController(
            TaskService taskService,
            TaskQueryService taskQueryService,
            ProjectQueryService projectQueryService,
            TaskMapper taskMapper,
            ProjectAccessGuard projectAccessGuard) {
        this.taskService = taskService;
        this.taskQueryService = taskQueryService;
        this.projectQueryService = projectQueryService;
        this.taskMapper = taskMapper;
        this.projectAccessGuard = projectAccessGuard;
    }

    // GET /api/tasks
    // GET /api/tasks?page=0&size=20&sort=createdAt,desc
    // GET
    // /api/tasks?search=spring&statusFilter=OPEN&priority=HIGH&overdue=true&selectedUserId=1&tags=1,2
    @GetMapping
    public Page<TaskResponse> getTasks(
            @ModelAttribute TaskListQuery query,
            @ParameterObject
                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        List<Long> accessibleProjectIds =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        ? null
                        : projectQueryService.getAccessibleProjectIds(
                                currentDetails.getUser().getId());
        TaskSearchCriteria criteria = query.toCriteria(accessibleProjectIds);
        return taskQueryService.searchTasks(criteria, pageable).map(taskMapper::toResponse);
    }

    // GET /api/tasks/5
    @GetMapping("/{id}")
    public TaskResponse getTaskById(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskById(id);
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
        task.setProject(projectQueryService.getProjectById(request.getProjectId()));
        return taskMapper.toResponse(taskService.createTask(task, request.getTagIds(), assigneeId));
    }

    // PUT /api/tasks/5
    // Project EDITOR or OWNER (or admin) may update.
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task existing = taskQueryService.getTaskById(id);
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
        Task task = taskQueryService.getTaskById(id);
        requireDeleteAccess(task, currentDetails);
        taskService.deleteTask(id);
    }

    // GET /api/tasks/search?keyword=spring
    @GetMapping("/search")
    public List<TaskResponse> searchTasks(@RequestParam String keyword) {
        return taskMapper.toResponseList(taskQueryService.searchTasks(keyword));
    }

    // GET /api/tasks/search-for-dependency?projectId=1&q=deploy&excludeTaskIds=5,10,12
    // Lightweight search for the dependency picker — returns id + title only.
    // excludeTaskIds: self + existing blockedBy + existing blocks (prevents duplicates/cycles in
    // UI).
    @GetMapping("/search-for-dependency")
    public List<Map<String, Object>> searchForDependency(
            @RequestParam Long projectId,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam List<Long> excludeTaskIds,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        projectAccessGuard.requireViewAccess(projectId, currentDetails);
        return taskQueryService.searchForDependency(projectId, q, excludeTaskIds);
    }

    // GET /api/tasks/incomplete
    @GetMapping("/incomplete")
    public List<TaskResponse> getIncompleteTasks() {
        return taskMapper.toResponseList(taskQueryService.getIncompleteTasks());
    }

    // PATCH /api/tasks/5/toggle
    // Advances status: OPEN → IN_PROGRESS → COMPLETED → OPEN.
    @PatchMapping("/{id}/toggle")
    public TaskResponse advanceStatus(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskById(id);
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
