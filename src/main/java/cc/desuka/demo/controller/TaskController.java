package cc.desuka.demo.controller;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.CalendarDay;
import cc.desuka.demo.dto.TaskFormRequest;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.ProjectService;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.service.TimelineService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.CsvWriter;
import cc.desuka.demo.util.HtmxUtils;
import cc.desuka.demo.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    private final ProjectService projectService;
    private final TagService tagService;
    private final UserService userService;
    private final CommentService commentService;
    private final TimelineService timelineService;
    private final OwnershipGuard ownershipGuard;
    private final ProjectAccessGuard projectAccessGuard;
    private final Messages messages;

    public TaskController(
            TaskService taskService,
            ProjectService projectService,
            TagService tagService,
            UserService userService,
            CommentService commentService,
            TimelineService timelineService,
            OwnershipGuard ownershipGuard,
            ProjectAccessGuard projectAccessGuard,
            Messages messages) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.userService = userService;
        this.commentService = commentService;
        this.timelineService = timelineService;
        this.ownershipGuard = ownershipGuard;
        this.projectAccessGuard = projectAccessGuard;
        this.messages = messages;
    }

    // GET /tasks - Display task list (full page or HTMX fragment)
    @GetMapping
    public String listTasks(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = TaskStatusFilter.DEFAULT)
                    TaskStatusFilter statusFilter,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long selectedUserId,
            @RequestParam(required = false) List<Long> tags,
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
        if (selectedUserId == null && !request.getParameterMap().containsKey("selectedUserId")) {
            if (userPreferences == null
                    || UserPreferences.FILTER_MINE.equals(userPreferences.getDefaultUserFilter())) {
                selectedUserId = currentDetails.getUser().getId();
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
        List<Long> accessibleProjectIds =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        ? null
                        : projectService.getAccessibleProjectIds(currentDetails.getUser().getId());

        model.addAttribute("allTags", tagService.getAllTags());
        model.addAttribute("view", resolvedView);
        model.addAttribute("selectedUserId", selectedUserId);
        addEditableProjects(model, currentDetails);

        // Resolve filtered user's name for the user filter button label
        Long currentId = currentDetails.getUser().getId();
        if (selectedUserId != null && !selectedUserId.equals(currentId)) {
            try {
                model.addAttribute(
                        "filterUserName", userService.getUserById(selectedUserId).getName());
            } catch (Exception ignored) {
            }
        }

        if (UserPreferences.VIEW_CALENDAR.equals(resolvedView)) {
            YearMonth calendarMonth = (month != null) ? month : YearMonth.now();
            List<List<CalendarDay>> calendarWeeks =
                    buildCalendarWeeks(
                            calendarMonth,
                            accessibleProjectIds,
                            search,
                            statusFilter,
                            overdue,
                            priority,
                            selectedUserId,
                            tags,
                            model);
            model.addAttribute("calendarWeeks", calendarWeeks);
            model.addAttribute("calendarMonth", calendarMonth);
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-calendar :: grid";
            }
            return "tasks/tasks";
        }

        if (!request.getParameterMap().containsKey("size")) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageSizeCookie, pageable.getSort());
        }
        Page<Task> taskPage =
                taskService.searchAndFilterTasksForProjects(
                        accessibleProjectIds,
                        search,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tags,
                        pageable);
        model.addAttribute("taskPage", taskPage);
        addProjectEditPermissions(taskPage.getContent(), currentDetails, model);

        if (HtmxUtils.isHtmxRequest(request)) {
            return UserPreferences.VIEW_TABLE.equals(resolvedView)
                    ? "tasks/task-table :: grid"
                    : "tasks/task-cards :: grid";
        }
        return "tasks/tasks";
    }

    // GET /tasks/export - Download CSV of filtered tasks
    // GET /tasks/export - Download CSV of filtered tasks (same filters as listTasks, unpaged)
    @GetMapping("/export")
    public void exportTasks(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = TaskStatusFilter.DEFAULT)
                    TaskStatusFilter statusFilter,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long selectedUserId,
            @RequestParam(required = false) List<Long> tags,
            @PageableDefault(sort = Task.FIELD_CREATED_AT, direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletResponse response)
            throws IOException {

        List<Long> accessibleProjectIds =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        ? null
                        : projectService.getAccessibleProjectIds(currentDetails.getUser().getId());
        Pageable unpaged = Pageable.unpaged(pageable.getSort());
        List<Task> tasks =
                taskService
                        .searchAndFilterTasksForProjects(
                                accessibleProjectIds,
                                search,
                                statusFilter,
                                overdue,
                                priority,
                                selectedUserId,
                                tags,
                                unpaged)
                        .getContent();

        String[] headers = {
            messages.get("task.field.title"),
            messages.get("task.field.status"),
            messages.get("task.field.priority"),
            messages.get("task.field.startDate"),
            messages.get("task.field.dueDate"),
            messages.get("task.field.completedAt"),
            messages.get("task.field.user"),
            messages.get("task.field.tags"),
            messages.get("task.field.createdAt"),
            messages.get("task.field.updatedAt"),
        };
        CsvWriter.write(
                response,
                "tasks.csv",
                headers,
                tasks,
                task ->
                        new String[] {
                            task.getTitle(),
                            task.getStatus() != null ? task.getStatus().name() : "",
                            task.getPriority() != null ? task.getPriority().name() : "",
                            task.getStartDate() != null ? task.getStartDate().toString() : "",
                            task.getDueDate() != null ? task.getDueDate().toString() : "",
                            task.getCompletedAt() != null
                                    ? task.getCompletedAt().toLocalDate().toString()
                                    : "",
                            task.getUser() != null ? task.getUser().getName() : "",
                            task.getTags() != null
                                    ? task.getTags().stream()
                                            .map(t -> t.getName())
                                            .collect(Collectors.joining("; "))
                                    : "",
                            task.getCreatedAt() != null
                                    ? task.getCreatedAt().toLocalDate().toString()
                                    : "",
                            task.getUpdatedAt() != null
                                    ? task.getUpdatedAt().toLocalDate().toString()
                                    : ""
                        });
    }

    // GET /tasks/{id} - Show task in view (read-only) mode
    @GetMapping("/{id}")
    public String showTask(
            @PathVariable Long id,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskService.getTaskById(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        model.addAttribute("task", task);
        model.addAttribute("taskFormRequest", TaskFormRequest.fromEntity(task));
        model.addAttribute("mode", "view");
        model.addAttribute("tags", tagService.getAllTags());
        addTimelineAttributes(model, id, currentDetails);
        if (HtmxUtils.isHtmxRequest(request)) {
            return "tasks/task-modal";
        }
        return "tasks/task";
    }

    // GET /tasks/new - Show create form (full page or modal fragment)
    // Default user assignment is the current user (can be changed via dropdown).
    // projectId is optional — if omitted, user picks from a dropdown of editable projects.
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) Long projectId,
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
            Project project = projectService.getProjectById(projectId);
            task.setProject(project);
        } else {
            addEditableProjects(model, currentDetails);
        }
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }
        model.addAttribute("task", task);
        model.addAttribute("taskFormRequest", TaskFormRequest.fromEntity(task));
        model.addAttribute("mode", "create");
        model.addAttribute("tags", tagService.getAllTags());
        model.addAttribute("timeline", Collections.emptyList());
        model.addAttribute("activityCount", 0);
        if (HtmxUtils.isHtmxRequest(request)) {
            return "tasks/task-modal";
        }
        return "tasks/task";
    }

    // POST /tasks - Create new task
    // Defaults to current user; user can pick a different assignee via the dropdown.
    // tagIds: list of selected tag IDs from form checkboxes. null when no checkbox is checked.
    // projectId comes from a hidden or select form field — every task must belong to a project.
    @PostMapping
    public Object createTask(
            @Valid @ModelAttribute TaskFormRequest taskFormRequest,
            BindingResult result,
            @RequestParam Long projectId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) List<String> checklistTexts,
            @RequestParam(required = false) List<Boolean> checklistChecked,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        projectAccessGuard.requireEditAccess(projectId, currentDetails);
        Project project = projectService.getProjectById(projectId);
        if (result.hasErrors()) {
            Task task = new Task();
            task.setProject(project);
            task.setUser(currentDetails.getUser());
            model.addAttribute("task", task);
            model.addAttribute("mode", "create");
            model.addAttribute("tags", tagService.getAllTags());
            addEditableProjects(model, currentDetails);
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-modal";
            }
            return "tasks/task";
        }
        Task task = taskFormRequest.toEntity();
        task.setProject(project);
        Task created =
                taskService.createTask(task, tagIds, assigneeId, checklistTexts, checklistChecked);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskSaved");
        }
        return new RedirectView("/tasks/" + created.getId());
    }

    // GET /tasks/{id}/edit - Show edit form (full page or modal fragment)
    // Project EDITOR or OWNER (or admin) may edit.
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskService.getTaskById(id);
        projectAccessGuard.requireEditAccess(task.getProject().getId(), currentDetails);
        model.addAttribute("task", task);
        model.addAttribute("taskFormRequest", TaskFormRequest.fromEntity(task));
        model.addAttribute("mode", "edit");
        model.addAttribute("tags", tagService.getAllTags());
        addTimelineAttributes(model, id, currentDetails);
        if (HtmxUtils.isHtmxRequest(request)) {
            return "tasks/task-modal";
        }
        return "tasks/task";
    }

    // POST /tasks/{id} - Update task
    // Project EDITOR or OWNER (or admin) may update.
    // tagIds: null means remove all tags. assigneeId comes from the user select dropdown.
    @PostMapping("/{id}")
    public Object updateTask(
            @PathVariable Long id,
            @Valid @ModelAttribute TaskFormRequest taskFormRequest,
            BindingResult result,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) List<String> checklistTexts,
            @RequestParam(required = false) List<Boolean> checklistChecked,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task existing = taskService.getTaskById(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        if (result.hasErrors()) {
            model.addAttribute("task", existing);
            model.addAttribute("mode", "edit");
            model.addAttribute("tags", tagService.getAllTags());
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-modal";
            }
            return "tasks/task";
        }
        Task taskDetails = taskFormRequest.toEntity();
        taskService.updateTask(
                id,
                taskDetails,
                tagIds,
                assigneeId,
                taskFormRequest.getVersion(),
                checklistTexts,
                checklistChecked);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskSaved");
        }
        return new RedirectView("/tasks/" + id);
    }

    // DELETE /tasks/{id} - Delete task
    // Task creator, project OWNER, or system admin may delete.
    @DeleteMapping("/{id}")
    public Object deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task task = taskService.getTaskById(id);
        requireDeleteAccess(task, currentDetails);
        taskService.deleteTask(id);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("taskDeleted");
        }
        return new RedirectView("/tasks");
    }

    // GET /tasks/{id}/activity - Fetch activity timeline fragment (HTMX live refresh)
    @GetMapping("/{id}/activity")
    public String getActivity(
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails currentDetails) {
        Task task = taskService.getTaskById(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        model.addAttribute("task", task);
        addTimelineAttributes(model, id, currentDetails);
        return "tasks/task-activity";
    }

    // POST /tasks/{id}/comments - Add a comment to a task
    // Any project member may comment on tasks in their project.
    @PostMapping("/{id}/comments")
    public Object addComment(
            @PathVariable Long id,
            @RequestParam String text,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task task = taskService.getTaskById(id);
        projectAccessGuard.requireViewAccess(task.getProject().getId(), currentDetails);
        commentService.createComment(text, id, currentDetails.getUser().getId());
        if (HtmxUtils.isHtmxRequest(request)) {
            model.addAttribute("task", taskService.getTaskById(id));
            addTimelineAttributes(model, id, currentDetails);
            return "tasks/task-activity";
        }
        return new RedirectView("/tasks/" + id);
    }

    // DELETE /tasks/{id}/comments/{commentId} - Delete a comment
    // Owner of the comment or admin may delete.
    @DeleteMapping("/{id}/comments/{commentId}")
    public Object deleteComment(
            @PathVariable Long id,
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
            model.addAttribute("task", taskService.getTaskById(id));
            addTimelineAttributes(model, id, currentDetails);
            return "tasks/task-activity";
        }
        return new RedirectView("/tasks/" + id);
    }

    // POST /tasks/{id}/toggle - Advance status (OPEN → IN_PROGRESS → COMPLETED → OPEN)
    @PostMapping("/{id}/toggle")
    public Object advanceStatus(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "cards") String view,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        Task existing = taskService.getTaskById(id);
        projectAccessGuard.requireEditAccess(existing.getProject().getId(), currentDetails);
        Task task = taskService.advanceStatus(id);

        if (HtmxUtils.isHtmxRequest(request)) {
            if (UserPreferences.VIEW_CALENDAR.equals(view)) {
                return HtmxUtils.triggerEvent("taskSaved");
            }
            model.addAttribute("task", task);
            return UserPreferences.VIEW_TABLE.equals(view)
                    ? "tasks/task-table-row :: row"
                    : "tasks/task-card :: card";
        }
        return "redirect:/tasks";
    }

    private void addProjectEditPermissions(
            List<Task> tasks, CustomUserDetails currentDetails, Model model) {
        boolean isAdmin = AuthExpressions.isAdmin(currentDetails.getUser());
        if (isAdmin) {
            model.addAttribute("canEditProject", true);
            return;
        }
        Long userId = currentDetails.getUser().getId();
        Map<Long, Boolean> editByProject = new java.util.HashMap<>();
        for (Task task : tasks) {
            Long projectId = task.getProject().getId();
            editByProject.computeIfAbsent(projectId, pid -> projectService.isEditor(pid, userId));
        }
        model.addAttribute("projectEditMap", editByProject);
    }

    private void addTimelineAttributes(Model model, Long taskId, CustomUserDetails currentDetails) {
        var currentUser = currentDetails.getUser();
        var timeline = timelineService.getTimeline(taskId, currentUser);
        model.addAttribute("timeline", timeline);
        model.addAttribute("activityCount", timeline.size());
    }

    private List<List<CalendarDay>> buildCalendarWeeks(
            YearMonth month,
            List<Long> accessibleProjectIds,
            String search,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tags,
            Model model) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate lastOfMonth = month.atEndOfMonth();

        // Grid starts on Monday of the week containing the 1st
        LocalDate gridStart = firstOfMonth.with(DayOfWeek.MONDAY);
        // Grid ends on Sunday of the week containing the last day
        LocalDate gridEnd = lastOfMonth.with(DayOfWeek.SUNDAY);

        // Query tasks with dates overlapping the visible grid range
        Pageable unpaged = Pageable.unpaged(Sort.by(Sort.Direction.ASC, Task.FIELD_DUE_DATE));
        List<Task> tasks =
                taskService
                        .searchAndFilterTasksForProjects(
                                accessibleProjectIds,
                                search,
                                statusFilter,
                                overdue,
                                priority,
                                selectedUserId,
                                tags,
                                unpaged,
                                gridStart,
                                gridEnd)
                        .getContent();

        // Place each task on one date: due date if present, otherwise start date.
        // Tasks with neither date are counted but not shown on the grid.
        Map<LocalDate, List<Task>> tasksByDate = new java.util.LinkedHashMap<>();
        long undatedCount = 0;

        for (Task task : tasks) {
            LocalDate date = (task.getDueDate() != null) ? task.getDueDate() : task.getStartDate();
            if (date == null) {
                undatedCount++;
                continue;
            }
            tasksByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        }

        model.addAttribute("undatedCount", undatedCount);

        LocalDate today = LocalDate.now();
        List<List<CalendarDay>> weeks = new ArrayList<>();
        LocalDate cursor = gridStart;

        while (!cursor.isAfter(gridEnd)) {
            List<CalendarDay> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                week.add(
                        new CalendarDay(
                                cursor,
                                !cursor.isBefore(firstOfMonth) && !cursor.isAfter(lastOfMonth),
                                cursor.equals(today),
                                tasksByDate.getOrDefault(cursor, List.of())));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return weeks;
    }

    private void addEditableProjects(Model model, CustomUserDetails currentDetails) {
        List<Project> editableProjects;
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            editableProjects = projectService.getActiveProjects();
        } else {
            editableProjects =
                    projectService.getEditableProjectsForUser(currentDetails.getUser().getId());
        }
        model.addAttribute("editableProjects", editableProjects);
    }

    /** Task delete access: task creator, project OWNER, or system admin. */
    private void requireDeleteAccess(Task task, CustomUserDetails currentDetails) {
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            return;
        }
        // Task creator can delete their own task
        if (task.getUser() != null
                && task.getUser().getId().equals(currentDetails.getUser().getId())) {
            return;
        }
        // Project OWNER can delete any task in the project
        projectAccessGuard.requireOwnerAccess(task.getProject().getId(), currentDetails);
    }
}
