package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.CalendarDay;
import cc.desuka.demo.dto.ProjectListQuery;
import cc.desuka.demo.dto.ProjectRequest;
import cc.desuka.demo.dto.TaskListQuery;
import cc.desuka.demo.dto.TaskSearchCriteria;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.report.TaskReport;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.ProjectQueryService;
import cc.desuka.demo.service.ProjectService;
import cc.desuka.demo.service.SprintQueryService;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.service.TaskQueryService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectQueryService projectQueryService;
    private final TaskQueryService taskQueryService;
    private final SprintQueryService sprintQueryService;
    private final TagService tagService;
    private final UserService userService;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskReport taskReport;
    private final AppRoutesProperties appRoutes;

    public ProjectController(
            ProjectService projectService,
            ProjectQueryService projectQueryService,
            TaskQueryService taskQueryService,
            SprintQueryService sprintQueryService,
            TagService tagService,
            UserService userService,
            ProjectAccessGuard projectAccessGuard,
            TaskReport taskReport,
            AppRoutesProperties appRoutes) {
        this.projectService = projectService;
        this.projectQueryService = projectQueryService;
        this.taskQueryService = taskQueryService;
        this.sprintQueryService = sprintQueryService;
        this.tagService = tagService;
        this.userService = userService;
        this.projectAccessGuard = projectAccessGuard;
        this.taskReport = taskReport;
        this.appRoutes = appRoutes;
    }

    // GET /projects - List projects the current user belongs to
    @GetMapping
    public String listProjects(
            @ModelAttribute ProjectListQuery query,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        List<Project> projects;
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            projects =
                    projectQueryService.getAdminProjects(query.isShowArchived(), query.getSort());
        } else {
            projects =
                    projectQueryService.getProjectsForUser(
                            currentDetails.getUser().getId(),
                            query.isShowArchived(),
                            query.getSort());
        }
        model.addAttribute("projects", projects);
        model.addAttribute("sort", query.getSort());
        model.addAttribute("showArchived", query.isShowArchived());

        if (HtmxUtils.isHtmxRequest(request)) {
            return "projects/project-grid :: grid";
        }
        return "projects/projects";
    }

    // GET /projects/new - Show create project form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("projectRequest", new ProjectRequest());
        return "projects/project-form";
    }

    // POST /projects - Create a new project
    @PostMapping
    public Object createProject(
            @Valid @ModelAttribute ProjectRequest projectRequest,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        if (result.hasErrors()) {
            return "projects/project-form";
        }
        Project saved =
                projectService.createProject(projectRequest.toEntity(), currentDetails.getUser());
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("projectSaved");
        }
        return new RedirectView(appRoutes.getProjectDetail().resolve("projectId", saved.getId()));
    }

    // GET /projects/{id} - Project home with task list
    @GetMapping("/{id}")
    public String showProject(
            @PathVariable Long id,
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
        projectAccessGuard.requireViewAccess(id, currentDetails);

        Project project = projectQueryService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("projectMembers", projectQueryService.getMembers(id));

        // Check if current user can edit in this project
        boolean canEditProject =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        || projectQueryService.isEditor(id, currentDetails.getUser().getId());
        model.addAttribute("canEditProject", canEditProject);
        boolean isProjectOwner =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        || projectQueryService.isOwner(id, currentDetails.getUser().getId());
        model.addAttribute("isProjectOwner", isProjectOwner);

        // Resolve view mode from user preferences
        UserPreferences userPreferences = (UserPreferences) model.getAttribute("userPreferences");
        String resolvedView =
                (view != null)
                        ? view
                        : (userPreferences != null
                                ? userPreferences.getTaskView()
                                : UserPreferences.VIEW_CARDS);
        model.addAttribute("allTags", tagService.getAllTags());
        model.addAttribute("view", resolvedView);
        model.addAttribute("selectedUserId", query.getSelectedUserId());

        // Sprint support
        if (project.isSprintEnabled()) {
            List<Sprint> sprints = sprintQueryService.getSprintsByProject(id);
            model.addAttribute("sprints", sprints);
            Optional<Sprint> activeSprint = sprintQueryService.getActiveSprint(id);
            activeSprint.ifPresent(s -> model.addAttribute("activeSprint", s));

            // Default to active sprint only on initial page load (direct navigation).
            // HTMX requests always carry an explicit value from the dropdown.
            if (query.getSprintId() == null
                    && activeSprint.isPresent()
                    && !HtmxUtils.isHtmxRequest(request)) {
                query.setSprintId(activeSprint.get().getId());
            }

            model.addAttribute("sprintId", query.getSprintId());

            // Resolve sprint filter label for the dropdown button
            Long sid = query.getSprintId();
            if (sid != null && sid > 0) {
                sprints.stream()
                        .filter(s -> s.getId().equals(sid))
                        .findFirst()
                        .ifPresent(s -> model.addAttribute("sprintFilterLabel", s.getName()));
            }
        }

        // Resolve filtered user's name
        Long currentId = currentDetails.getUser().getId();
        Long selectedUserId = query.getSelectedUserId();
        if (selectedUserId != null && !selectedUserId.equals(currentId)) {
            try {
                model.addAttribute(
                        "filterUserName", userService.getUserById(selectedUserId).getName());
            } catch (Exception ignored) {
            }
        }

        TaskSearchCriteria criteria = query.toCriteria(id);

        if (UserPreferences.VIEW_CALENDAR.equals(resolvedView)) {
            YearMonth calendarMonth = (month != null) ? month : YearMonth.now();
            List<List<CalendarDay>> calendarWeeks =
                    buildCalendarWeeks(calendarMonth, criteria, model);
            model.addAttribute("calendarWeeks", calendarWeeks);
            model.addAttribute("calendarMonth", calendarMonth);
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-calendar :: grid";
            }
            return "projects/project";
        }

        if (UserPreferences.VIEW_BOARD.equals(resolvedView)) {
            Pageable unpaged = Pageable.unpaged(Sort.by(Sort.Direction.ASC, Task.FIELD_CREATED_AT));
            List<Task> tasks = taskQueryService.searchTasks(criteria, unpaged).getContent();
            model.addAttribute("tasksByStatus", taskQueryService.groupByStatus(tasks));
            model.addAttribute("statuses", TaskStatus.values());
            if (HtmxUtils.isHtmxRequest(request)) {
                return "tasks/task-board :: grid";
            }
            return "projects/project";
        }

        if (!request.getParameterMap().containsKey("size")) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageSizeCookie, pageable.getSort());
        }
        Page<Task> taskPage = taskQueryService.searchTasks(criteria, pageable);
        model.addAttribute("taskPage", taskPage);

        if (HtmxUtils.isHtmxRequest(request)) {
            return UserPreferences.VIEW_TABLE.equals(resolvedView)
                    ? "tasks/task-table :: grid"
                    : "tasks/task-cards :: grid";
        }
        return "projects/project";
    }

    // GET /projects/{id}/export - Download CSV of project tasks (same filters as project view)
    @GetMapping("/{id}/export")
    public void exportProjectTasks(
            @PathVariable Long id,
            @ModelAttribute TaskListQuery query,
            @PageableDefault(sort = Task.FIELD_CREATED_AT, direction = Sort.Direction.DESC)
                    Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletResponse response)
            throws IOException {
        projectAccessGuard.requireViewAccess(id, currentDetails);
        Project project = projectQueryService.getProjectById(id);
        TaskSearchCriteria criteria = query.toCriteria(id);

        Pageable unpaged = Pageable.unpaged(pageable.getSort());
        List<Task> tasks = taskQueryService.searchTasks(criteria, unpaged).getContent();

        taskReport.exportCsv(response, project.getName() + "-tasks.csv", tasks);
    }

    // GET /projects/{id}/settings - Project settings (owner or admin only)
    @GetMapping("/{id}/settings")
    public String showSettings(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        Project project = projectQueryService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("projectRequest", ProjectRequest.fromEntity(project));
        model.addAttribute("projectMembers", projectQueryService.getMembers(id));
        model.addAttribute("projectRoles", ProjectRole.values());
        if (project.isSprintEnabled()) {
            model.addAttribute("sprints", sprintQueryService.getSprintsByProject(id));
        }

        return "projects/project-settings";
    }

    // POST /projects/{id} - Update project
    @PostMapping("/{id}")
    public Object updateProject(
            @PathVariable Long id,
            @Valid @ModelAttribute ProjectRequest projectRequest,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        if (result.hasErrors()) {
            model.addAttribute("project", projectQueryService.getProjectById(id));
            model.addAttribute("projectMembers", projectQueryService.getMembers(id));
            model.addAttribute("projectRoles", ProjectRole.values());
            return "projects/project-settings";
        }
        projectService.updateProject(id, projectRequest.toEntity());
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("projectSaved");
        }
        return new RedirectView(appRoutes.getProjectSettings().resolve("projectId", id));
    }

    // POST /projects/{id}/archive - Archive project
    @PostMapping("/{id}/archive")
    public Object archiveProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        projectService.archiveProject(id);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("projectArchived");
        }
        return new RedirectView(appRoutes.getProjects().toString());
    }

    // POST /projects/{id}/unarchive - Restore archived project
    @PostMapping("/{id}/unarchive")
    public Object unarchiveProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        projectService.unarchiveProject(id);
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("projectUnarchived");
        }
        return new RedirectView(appRoutes.getProjectDetail().resolve("projectId", id));
    }

    // POST /projects/{id}/members - Add member
    @PostMapping("/{id}/members")
    public String addMember(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam ProjectRole role,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        try {
            projectService.addMember(id, userId, role);
            model.addAttribute("memberAdded", true);
        } catch (IllegalStateException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        populateMemberModel(id, model);
        return "projects/member-table";
    }

    // PATCH /projects/{id}/members/{userId}/role - Change member role
    @PatchMapping("/{id}/members/{userId}/role")
    public String updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam ProjectRole role,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        try {
            projectService.updateMemberRole(id, userId, role);
            model.addAttribute("roleChanged", true);
        } catch (IllegalStateException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        populateMemberModel(id, model);
        return "projects/member-table";
    }

    // DELETE /projects/{id}/members/{userId} - Remove member
    @DeleteMapping("/{id}/members/{userId}")
    public String removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        try {
            projectService.removeMember(id, userId);
            model.addAttribute("memberRemoved", true);
        } catch (IllegalStateException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        populateMemberModel(id, model);
        return "projects/member-table";
    }

    // GET /projects/{id}/analytics - Project analytics page
    @GetMapping("/{id}/analytics")
    public String projectAnalytics(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        projectAccessGuard.requireViewAccess(id, currentDetails);
        Project project = projectQueryService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("apiUrl", appRoutes.getApiProjectAnalytics().resolve("projectId", id));
        if (project.isSprintEnabled()) {
            List<Sprint> sprints = sprintQueryService.getSprintsByProject(id);
            model.addAttribute("sprints", sprints);
            sprintQueryService
                    .getActiveSprint(id)
                    .ifPresent(s -> model.addAttribute("activeSprint", s));
        }
        return "analytics/analytics";
    }

    private void populateMemberModel(Long projectId, Model model) {
        model.addAttribute("project", projectQueryService.getProjectById(projectId));
        model.addAttribute("projectMembers", projectQueryService.getMembers(projectId));
        model.addAttribute("projectRoles", ProjectRole.values());
    }

    private List<List<CalendarDay>> buildCalendarWeeks(
            YearMonth month, TaskSearchCriteria criteria, Model model) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate lastOfMonth = month.atEndOfMonth();
        LocalDate gridStart = firstOfMonth.with(DayOfWeek.MONDAY);
        LocalDate gridEnd = lastOfMonth.with(DayOfWeek.SUNDAY);

        criteria.setDueDateFrom(gridStart);
        criteria.setDueDateTo(gridEnd);
        Pageable unpaged = Pageable.unpaged(Sort.by(Sort.Direction.ASC, Task.FIELD_DUE_DATE));
        List<Task> tasks = taskQueryService.searchTasks(criteria, unpaged).getContent();

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
}
