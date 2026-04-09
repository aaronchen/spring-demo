package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.BulkTaskRequest;
import cc.desuka.demo.dto.TaskFormRequest;
import cc.desuka.demo.dto.TaskListQuery;
import cc.desuka.demo.dto.TaskSearchCriteria;
import cc.desuka.demo.dto.TaskUpdateCriteria;
import cc.desuka.demo.dto.TimelineEntry;
import cc.desuka.demo.exception.BlockedTaskException;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.mapper.TaskFormMapper;
import cc.desuka.demo.model.ChecklistItem;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.report.TaskReport;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.ProjectQueryService;
import cc.desuka.demo.service.RecentViewService;
import cc.desuka.demo.service.SprintQueryService;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.service.TaskQueryService;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.service.TimelineService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.CalendarHelper;
import cc.desuka.demo.util.EntityTypes;
import cc.desuka.demo.util.FormMode;
import cc.desuka.demo.util.HtmxUtils;
import cc.desuka.demo.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskQueryService taskQueryService;
    private final ProjectQueryService projectQueryService;
    private final SprintQueryService sprintQueryService;
    private final TagService tagService;
    private final UserService userService;
    private final CommentService commentService;
    private final TimelineService timelineService;
    private final RecentViewService recentViewService;
    private final CalendarHelper calendarHelper;
    private final OwnershipGuard ownershipGuard;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskReport taskReport;
    private final TaskFormMapper taskFormMapper;
    private final AppRoutesProperties appRoutes;
    private final Messages messages;

    public TaskController(
            TaskService taskService,
            TaskQueryService taskQueryService,
            ProjectQueryService projectQueryService,
            SprintQueryService sprintQueryService,
            TagService tagService,
            UserService userService,
            CommentService commentService,
            TimelineService timelineService,
            RecentViewService recentViewService,
            CalendarHelper calendarHelper,
            OwnershipGuard ownershipGuard,
            ProjectAccessGuard projectAccessGuard,
            TaskReport taskReport,
            TaskFormMapper taskFormMapper,
            AppRoutesProperties appRoutes,
            Messages messages) {
        this.taskService = taskService;
        this.taskQueryService = taskQueryService;
        this.projectQueryService = projectQueryService;
        this.sprintQueryService = sprintQueryService;
        this.tagService = tagService;
        this.userService = userService;
        this.commentService = commentService;
        this.timelineService = timelineService;
        this.recentViewService = recentViewService;
        this.calendarHelper = calendarHelper;
        this.ownershipGuard = ownershipGuard;
        this.projectAccessGuard = projectAccessGuard;
        this.taskReport = taskReport;
        this.taskFormMapper = taskFormMapper;
        this.appRoutes = appRoutes;
        this.messages = messages;
    }

    // GET /tasks - Display task list (full page or HTMX fragment)
    @GetMapping
    public String listTasks(
            @ModelAttribute TaskListQuery query,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) YearMonth month,
            @CookieValue(name = "pageSize", required = false, defaultValue = "25")
                    int pageSizeCookie,
            @PageableDefault(
                            size = 25,
                            sort = Task.FIELD_CREATED_AT,
                            direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        // userPreferences is already in the model via GlobalModelAttributes
        UserPreferences userPreferences = (UserPreferences) model.getAttribute("userPreferences");
        // Default user filter on first visit (no userId param):
        // "mine" preference → current user's tasks; "all" preference → all users.
        if (query.getSelectedUserId() == null
                && !request.getParameterMap().containsKey("selectedUserId")) {
            if (userPreferences == null
                    || UserPreferences.FILTER_MINE.equals(userPreferences.getDefaultUserFilter())) {
                query.setSelectedUserId(currentDetails.getUser().getId());
            }
        }
        // View mode: URL param overrides preference default
        String resolvedView =
                (view != null)
                        ? view
                        : (userPreferences != null
                                ? userPreferences.getTaskView()
                                : UserPreferences.VIEW_CARDS);
        // Scope to user's accessible projects (admins see all)
        List<UUID> accessibleProjectIds =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        ? null
                        : projectQueryService.getAccessibleProjectIds(
                                currentDetails.getUser().getId());

        // Project IDs for WebSocket subscriptions (always a concrete list, never null)
        List<UUID> wsProjectIds =
                accessibleProjectIds != null
                        ? accessibleProjectIds
                        : projectQueryService.getAllActiveProjectIds();
        model.addAttribute("wsProjectIds", wsProjectIds);

        model.addAttribute("allTags", tagService.getAllTags());
        model.addAttribute("view", resolvedView);
        model.addAttribute("selectedUserId", query.getSelectedUserId());
        addEditableProjects(model, currentDetails);

        // Resolve filtered user's name for the user filter button label
        UUID currentId = currentDetails.getUser().getId();
        UUID selectedUserId = query.getSelectedUserId();
        if (selectedUserId != null && !selectedUserId.equals(currentId)) {
            try {
                model.addAttribute(
                        "filterUserName", userService.getUserById(selectedUserId).getName());
            } catch (EntityNotFoundException ignored) {
            }
        }

        TaskSearchCriteria criteria = query.toCriteria(accessibleProjectIds);

        if (UserPreferences.VIEW_CALENDAR.equals(resolvedView)) {
            YearMonth calendarMonth = (month != null) ? month : YearMonth.now();
            CalendarHelper.CalendarResult calendarResult =
                    calendarHelper.buildCalendarWeeks(calendarMonth, criteria);
            model.addAttribute("calendarWeeks", calendarResult.weeks());
            model.addAttribute("calendarMonth", calendarMonth);
            model.addAttribute("undatedCount", calendarResult.undatedCount());
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-calendar :: grid";
            }
            return "tasks/tasks";
        }

        if (UserPreferences.VIEW_BOARD.equals(resolvedView)) {
            Pageable unpaged = Pageable.unpaged(Sort.by(Sort.Direction.ASC, Task.FIELD_CREATED_AT));
            List<Task> tasks = taskQueryService.searchTasks(criteria, unpaged).getContent();
            model.addAttribute("tasksByStatus", taskQueryService.groupByStatus(tasks));
            model.addAttribute("statuses", TaskStatus.values());
            addProjectEditPermissions(tasks, currentDetails, model);
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-board :: grid";
            }
            return "tasks/tasks";
        }

        if (!request.getParameterMap().containsKey("size")) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageSizeCookie, pageable.getSort());
        }
        Page<Task> taskPage = taskQueryService.searchTasks(criteria, pageable);
        model.addAttribute("taskPage", taskPage);
        addProjectEditPermissions(taskPage.getContent(), currentDetails, model);

        if (HtmxUtils.isHtmxRequest(request)) {
            return UserPreferences.VIEW_TABLE.equals(resolvedView)
                    ? "tasks/task-table :: grid"
                    : "tasks/task-cards :: grid";
        }
        return "tasks/tasks";
    }

    // GET /tasks/export - Download CSV of filtered tasks (same filters as listTasks, unpaged)
    @GetMapping("/export")
    public void exportTasks(
            @ModelAttribute TaskListQuery query,
            @PageableDefault(sort = Task.FIELD_CREATED_AT, direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletResponse response)
            throws IOException {

        List<UUID> accessibleProjectIds =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        ? null
                        : projectQueryService.getAccessibleProjectIds(
                                currentDetails.getUser().getId());
        TaskSearchCriteria criteria = query.toCriteria(accessibleProjectIds);

        Pageable unpaged = Pageable.unpaged(pageable.getSort());
        List<Task> tasks = taskQueryService.searchTasks(criteria, unpaged).getContent();

        taskReport.exportCsv(response, "tasks.csv", tasks);
    }

    // GET /tasks/{id} - Show task in view (read-only) mode
    @GetMapping("/{id}")
    public String showTask(
            @PathVariable UUID id,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskWithDependencies(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        recentViewService.recordView(
                currentDetails.getUser(), EntityTypes.TASK, task.getId(), task.getTitle());
        populateFormModel(task, FormMode.VIEW, currentDetails, model);
        return taskFormView(request);
    }

    // GET /tasks/new - Show create form (full page or modal fragment)
    // Default user assignment is the current user (can be changed via dropdown).
    // projectId is optional — if omitted, user picks from a dropdown of editable projects.
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    LocalDate dueDate,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = new Task();
        task.setUser(currentDetails.getUser());
        if (projectId != null) {
            projectAccessGuard.requireEditAccess(projectId, currentDetails);
            Project project = projectQueryService.getProjectById(projectId);
            task.setProject(project);
        } else {
            addEditableProjects(model, currentDetails);
        }
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }
        populateFormModel(task, FormMode.CREATE, currentDetails, model);
        return taskFormView(request);
    }

    // POST /tasks - Create new task
    // Defaults to current user; user can pick a different assignee via the dropdown.
    // tagIds: list of selected tag IDs from form checkboxes. null when no checkbox is checked.
    // projectId comes from a hidden or select form field — every task must belong to a project.
    @PostMapping
    public Object createTask(
            @Valid @ModelAttribute TaskFormRequest taskFormRequest,
            BindingResult result,
            @RequestParam UUID projectId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) List<String> checklistTexts,
            @RequestParam(required = false) List<Boolean> checklistChecked,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        Project project = projectQueryService.getProjectById(projectId);
        if (result.hasErrors()) {
            Task task = new Task();
            task.setProject(project);
            restoreFormSelections(
                    task,
                    taskFormRequest,
                    assigneeId,
                    tagIds,
                    checklistTexts,
                    checklistChecked,
                    null,
                    null);
            populateFormModel(task, FormMode.CREATE, currentDetails, model);
            addEditableProjects(model, currentDetails);
            return taskFormView(request);
        }
        Task task = taskFormMapper.toEntity(taskFormRequest);
        task.setProject(project);
        restoreSprint(task, taskFormRequest.getSprintId());
        Task created =
                taskService.createTask(task, tagIds, assigneeId, checklistTexts, checklistChecked);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskSaved");
        }
        return new RedirectView(
                appRoutes.getTaskDetail().params("taskId", created.getId()).build());
    }

    // GET /tasks/{id}/edit - Show edit form (full page or modal fragment)
    // Project EDITOR or OWNER (or admin) may edit.
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable UUID id,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskWithDependencies(id);
        projectAccessGuard.requireEditAccess(task.getProject().getId(), currentDetails);
        recentViewService.recordView(
                currentDetails.getUser(), EntityTypes.TASK, task.getId(), task.getTitle());
        populateFormModel(task, FormMode.EDIT, currentDetails, model);
        return taskFormView(request);
    }

    // POST /tasks/{id} - Update task
    // Project EDITOR or OWNER (or admin) may update.
    // tagIds: null means remove all tags. assigneeId comes from the user select dropdown.
    @PostMapping("/{id}")
    public Object updateTask(
            @PathVariable UUID id,
            @Valid @ModelAttribute TaskFormRequest taskFormRequest,
            BindingResult result,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) List<String> checklistTexts,
            @RequestParam(required = false) List<Boolean> checklistChecked,
            @RequestParam(required = false) List<UUID> blockedByIds,
            @RequestParam(required = false) List<UUID> blocksIds,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task existing = taskQueryService.getTaskWithDependencies(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        if (result.hasErrors()) {
            restoreFormSelections(
                    existing,
                    taskFormRequest,
                    assigneeId,
                    tagIds,
                    checklistTexts,
                    checklistChecked,
                    blockedByIds,
                    blocksIds);
            populateFormModel(existing, FormMode.EDIT, currentDetails, model);
            return taskFormView(request);
        }
        Task taskDetails = taskFormMapper.toEntity(taskFormRequest);
        restoreSprint(taskDetails, taskFormRequest.getSprintId());
        taskService.updateTask(
                id,
                taskDetails,
                new TaskUpdateCriteria(
                        tagIds,
                        assigneeId,
                        taskFormRequest.getVersion(),
                        checklistTexts,
                        checklistChecked,
                        blockedByIds,
                        blocksIds));
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskSaved");
        }
        return new RedirectView(appRoutes.getTaskDetail().params("taskId", id).build());
    }

    // DELETE /tasks/{id} - Delete task
    // Task creator, project OWNER, or system admin may delete.
    @DeleteMapping("/{id}")
    public Object deleteTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task task = taskQueryService.getTaskById(id);
        projectAccessGuard.requireDeleteAccess(task, task.getProject().getId(), currentDetails);
        taskService.deleteTask(id);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskDeleted");
        }
        return new RedirectView(appRoutes.getTasks().toString());
    }

    // GET /tasks/{id}/activity - Fetch activity timeline fragment (HTMX live refresh)
    @GetMapping("/{id}/activity")
    public String getActivity(
            @PathVariable UUID id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskQueryService.getTaskById(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        model.addAttribute("task", task);
        addTimelineAttributes(model, id, currentDetails);
        return "tasks/task-activity";
    }

    // POST /tasks/{id}/comments - Add a comment to a task
    // Any project member may comment on tasks in their project.
    @PostMapping("/{id}/comments")
    public Object addComment(
            @PathVariable UUID id,
            @RequestParam String text,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task task = taskQueryService.getTaskById(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        commentService.createComment(text, id, currentDetails.getUser().getId());
        if (HtmxUtils.isHtmxRequest(request)) {
            model.addAttribute("task", taskQueryService.getTaskById(id));
            addTimelineAttributes(model, id, currentDetails);
            return "tasks/task-activity";
        }
        return new RedirectView(appRoutes.getTaskDetail().params("taskId", id).build());
    }

    // DELETE /tasks/{id}/comments/{commentId} - Delete a comment
    // Owner of the comment or admin may delete.
    @DeleteMapping("/{id}/comments/{commentId}")
    public Object deleteComment(
            @PathVariable UUID id,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Comment comment = commentService.getCommentById(commentId);
        if (comment.getUser() != null) {
            ownershipGuard.requireAccess(comment, currentDetails);
        }
        commentService.deleteComment(commentId);
        if (HtmxUtils.isHtmxRequest(request)) {
            model.addAttribute("task", taskQueryService.getTaskById(id));
            addTimelineAttributes(model, id, currentDetails);
            return "tasks/task-activity";
        }
        return new RedirectView(appRoutes.getTaskDetail().params("taskId", id).build());
    }

    // PATCH /tasks/{id}/field - Inline edit a single field (used by table view inline editing)
    @PatchMapping("/{id}/field")
    public String updateField(
            @PathVariable UUID id,
            @RequestParam String field,
            @RequestParam(required = false) String value,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        Task existing = taskQueryService.getTaskById(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        Task task = taskService.updateField(id, field, value);
        model.addAttribute("task", task);
        addProjectEditPermissions(List.of(task), currentDetails, model);
        return "tasks/task-table-row :: row";
    }

    // POST /tasks/{id}/status - Set explicit status (used by Kanban drag-and-drop)
    @PostMapping("/{id}/status")
    public Object setStatus(
            @PathVariable UUID id,
            @RequestParam TaskStatus status,
            @RequestParam(required = false, defaultValue = "board") String view,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task existing = taskQueryService.getTaskById(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        taskService.setStatus(id, status);

        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskSaved");
        }
        return "redirect:" + appRoutes.getTasks();
    }

    // POST /tasks/{id}/toggle - Advance status (OPEN → IN_PROGRESS → COMPLETED → OPEN)
    @PostMapping("/{id}/toggle")
    public Object advanceStatus(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "cards") String view,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task existing = taskQueryService.getTaskById(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        Task task = taskService.advanceStatus(id);

        if (HtmxUtils.isHtmxRequest(request)) {
            if (UserPreferences.VIEW_CALENDAR.equals(view)) {
                return HtmxUtils.triggerEvent("taskSaved");
            }
            model.addAttribute("task", task);
            // Set canEditProject so the card/row link opens in edit mode (not view)
            model.addAttribute(
                    "canEditProject",
                    projectAccessGuard.canEdit(task.getProject().getId(), currentDetails));
            return UserPreferences.VIEW_TABLE.equals(view)
                    ? "tasks/task-table-row :: row"
                    : "tasks/task-card :: card";
        }
        return "redirect:" + appRoutes.getTasks();
    }

    // POST /tasks/bulk - Bulk update or delete tasks (JSON request/response)
    @PostMapping("/bulk")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkAction(
            @Valid @RequestBody BulkTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {

        List<UUID> taskIds = request.getTaskIds();
        String action = request.getAction();
        String value = request.getValue();

        // Load all tasks and verify access (fail-fast)
        List<Task> tasks = new ArrayList<>(taskIds.size());
        Set<UUID> checkedProjectIds = new HashSet<>();
        for (UUID taskId : taskIds) {
            Task task = taskQueryService.getTaskById(taskId);
            UUID projectId = task.getProject().getId();
            if (BulkTaskRequest.ACTION_DELETE.equals(action)) {
                // Delete requires per-task check (creator, project OWNER, or admin)
                projectAccessGuard.requireDeleteAccess(
                        task, task.getProject().getId(), currentDetails);
            } else if (checkedProjectIds.add(projectId)) {
                // Edit requires project-level EDITOR/OWNER check (cache per project)
                projectAccessGuard.requireEditAccess(projectId, currentDetails);
            }
            tasks.add(task);
        }

        int count = 0;
        int skipped = 0;
        switch (action) {
            case BulkTaskRequest.ACTION_STATUS -> {
                TaskStatus newStatus = TaskStatus.valueOf(value);
                for (Task task : tasks) {
                    try {
                        taskService.setStatus(task.getId(), newStatus);
                        count++;
                    } catch (BlockedTaskException e) {
                        skipped++;
                    }
                }
            }
            case BulkTaskRequest.ACTION_PRIORITY -> {
                Priority newPriority = Priority.valueOf(value);
                for (Task task : tasks) {
                    taskService.updateField(task.getId(), "priority", newPriority.name());
                    count++;
                }
            }
            case BulkTaskRequest.ACTION_ASSIGN -> {
                for (Task task : tasks) {
                    taskService.updateField(task.getId(), "userId", value);
                    count++;
                }
            }
            case BulkTaskRequest.ACTION_EFFORT -> {
                for (Task task : tasks) {
                    taskService.updateField(task.getId(), "effort", value);
                    count++;
                }
            }
            case BulkTaskRequest.ACTION_SPRINT -> {
                Long sprintId = value != null && !value.isBlank() ? Long.valueOf(value) : null;
                for (Task task : tasks) {
                    taskService.assignSprint(task.getId(), sprintId);
                    count++;
                }
            }
            case BulkTaskRequest.ACTION_DELETE -> {
                for (Task task : tasks) {
                    try {
                        taskService.deleteTask(task.getId());
                        count++;
                    } catch (IllegalStateException ignored) {
                        // Skip completed tasks that cannot be deleted
                    }
                }
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", messages.get("task.bulk.invalidAction")));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        if (skipped > 0) {
            result.put("skipped", skipped);
            result.put("skippedMessage", messages.get("task.bulk.skippedBlocked", skipped));
        }
        return ResponseEntity.ok(result);
    }

    private void addProjectEditPermissions(
            List<Task> tasks, CustomUserDetails currentDetails, Model model) {
        boolean isAdmin = AuthExpressions.isAdmin(currentDetails.getUser());
        if (isAdmin) {
            model.addAttribute("canEditProject", true);
            return;
        }
        UUID userId = currentDetails.getUser().getId();
        Map<UUID, Boolean> editByProject = new HashMap<>();
        for (Task task : tasks) {
            UUID projectId = task.getProject().getId();
            editByProject.computeIfAbsent(
                    projectId, pid -> projectQueryService.isEditor(pid, userId));
        }
        model.addAttribute("projectEditMap", editByProject);
    }

    private void populateFormModel(
            Task task, FormMode mode, CustomUserDetails currentDetails, Model model) {
        model.addAttribute("task", task);
        // On validation error, Spring's @ModelAttribute has already placed the user's
        // submitted TaskFormRequest in the model — preserve it so form fields retain values.
        if (!model.containsAttribute("taskFormRequest")) {
            model.addAttribute("taskFormRequest", taskFormMapper.toRequest(task));
        }
        model.addAttribute("mode", mode.getValue());
        model.addAttribute("tags", tagService.getAllTags());
        addSprintAttributes(task.getProject(), model);
        model.addAttribute("canEditDependencies", mode == FormMode.EDIT);
        addTimelineAttributes(model, task.getId(), currentDetails);
    }

    private String taskFormView(HttpServletRequest request) {
        return HtmxUtils.isHtmxRequest(request) ? "tasks/task-modal" : "tasks/task";
    }

    private void restoreFormSelections(
            Task task,
            TaskFormRequest formRequest,
            UUID assigneeId,
            List<Long> tagIds,
            List<String> checklistTexts,
            List<Boolean> checklistChecked,
            List<UUID> blockedByIds,
            List<UUID> blocksIds) {
        restoreAssignee(task, assigneeId);
        restoreSprint(task, formRequest.getSprintId());
        restoreTags(task, tagIds);
        restoreDependencies(task, blockedByIds, blocksIds);
        rebuildChecklistFromForm(task, checklistTexts, checklistChecked);
    }

    private void restoreAssignee(Task task, UUID assigneeId) {
        task.setUser(userService.findUserById(assigneeId));
    }

    private void restoreSprint(Task task, Long sprintId) {
        if (sprintId != null && sprintId > 0) {
            task.setSprint(sprintQueryService.getSprintById(sprintId));
        } else {
            task.setSprint(null);
        }
    }

    // Tags use checkboxes: null means none checked = clear all tags
    private void restoreTags(Task task, List<Long> tagIds) {
        if (tagIds != null) {
            task.setTags(tagService.findAllByIds(tagIds));
        } else {
            task.setTags(new LinkedHashSet<>());
        }
    }

    // Dependencies use hidden fields: null means not submitted (create mode) = keep DB values
    private void restoreDependencies(Task task, List<UUID> blockedByIds, List<UUID> blocksIds) {
        if (blockedByIds != null) {
            task.setBlockedBy(new LinkedHashSet<>(taskQueryService.getTasksByIds(blockedByIds)));
        }
        if (blocksIds != null) {
            task.setBlocks(new LinkedHashSet<>(taskQueryService.getTasksByIds(blocksIds)));
        }
    }

    private void rebuildChecklistFromForm(
            Task task, List<String> checklistTexts, List<Boolean> checklistChecked) {
        if (checklistTexts == null) {
            return;
        }
        task.getChecklistItems().clear();
        for (int i = 0; i < checklistTexts.size(); i++) {
            ChecklistItem item = new ChecklistItem(checklistTexts.get(i), i);
            item.setChecked(
                    checklistChecked != null
                            && i < checklistChecked.size()
                            && Boolean.TRUE.equals(checklistChecked.get(i)));
            task.getChecklistItems().add(item);
        }
    }

    private void addTimelineAttributes(Model model, UUID taskId, CustomUserDetails currentDetails) {
        if (taskId == null) {
            model.addAttribute("timeline", List.of());
            model.addAttribute("activityCount", 0);
            return;
        }
        List<TimelineEntry> timeline =
                timelineService.getTimeline(taskId, currentDetails.getUser());
        model.addAttribute("timeline", timeline);
        model.addAttribute("activityCount", timeline.size());
    }

    private void addSprintAttributes(Project project, Model model) {
        if (project != null && project.isSprintEnabled()) {
            model.addAttribute(
                    "projectSprints", sprintQueryService.getSprintsByProject(project.getId()));
        }
    }

    private void addEditableProjects(Model model, CustomUserDetails currentDetails) {
        List<Project> editableProjects;
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            editableProjects = projectQueryService.getActiveProjects();
        } else {
            editableProjects =
                    projectQueryService.getEditableProjectsForUser(
                            currentDetails.getUser().getId());
        }
        model.addAttribute("editableProjects", editableProjects);
    }
}
