package cc.desuka.demo.controller;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.CalendarDay;
import cc.desuka.demo.dto.ProjectRequest;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.security.AuthExpressions;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.ProjectAccessGuard;
import cc.desuka.demo.service.ProjectService;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final TaskService taskService;
    private final TagService tagService;
    private final UserService userService;
    private final ProjectAccessGuard projectAccessGuard;

    public ProjectController(
            ProjectService projectService,
            TaskService taskService,
            TagService tagService,
            UserService userService,
            ProjectAccessGuard projectAccessGuard) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.tagService = tagService;
        this.userService = userService;
        this.projectAccessGuard = projectAccessGuard;
    }

    // GET /projects - List projects the current user belongs to
    @GetMapping
    public String listProjects(
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "false") boolean showArchived,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            HttpServletRequest request,
            Model model) {
        List<Project> projects;
        if (AuthExpressions.isAdmin(currentDetails.getUser())) {
            if (showArchived) {
                projects =
                        "newest".equals(sort)
                                ? projectService.getAllProjectsByNewest()
                                : projectService.getAllProjects();
            } else {
                projects =
                        "newest".equals(sort)
                                ? projectService.getActiveProjectsByNewest()
                                : projectService.getActiveProjects();
            }
        } else {
            projects =
                    projectService.getProjectsForUser(
                            currentDetails.getUser().getId(), showArchived, sort);
        }
        model.addAttribute("projects", projects);
        model.addAttribute("sort", sort);
        model.addAttribute("showArchived", showArchived);

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
        return new RedirectView("/projects/" + saved.getId());
    }

    // GET /projects/{id} - Project home with task list
    @GetMapping("/{id}")
    public String showProject(
            @PathVariable Long id,
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
        projectAccessGuard.requireViewAccess(id, currentDetails);

        Project project = projectService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("projectMembers", projectService.getMembers(id));

        // Check if current user can edit in this project
        boolean canEditProject =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        || projectService.isEditor(id, currentDetails.getUser().getId());
        model.addAttribute("canEditProject", canEditProject);
        boolean isProjectOwner =
                AuthExpressions.isAdmin(currentDetails.getUser())
                        || projectService.isOwner(id, currentDetails.getUser().getId());
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
        model.addAttribute("selectedUserId", selectedUserId);

        // Resolve filtered user's name
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
                            id,
                            calendarMonth,
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
            return "projects/project";
        }

        if (!request.getParameterMap().containsKey("size")) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageSizeCookie, pageable.getSort());
        }
        Page<Task> taskPage =
                taskService.searchAndFilterTasksForProject(
                        id,
                        search,
                        statusFilter,
                        overdue,
                        priority,
                        selectedUserId,
                        tags,
                        pageable);
        model.addAttribute("taskPage", taskPage);

        if (HtmxUtils.isHtmxRequest(request)) {
            return UserPreferences.VIEW_TABLE.equals(resolvedView)
                    ? "tasks/task-table :: grid"
                    : "tasks/task-cards :: grid";
        }
        return "projects/project";
    }

    // GET /projects/{id}/settings - Project settings (owner or admin only)
    @GetMapping("/{id}/settings")
    public String showSettings(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model) {
        projectAccessGuard.requireOwnerAccess(id, currentDetails);
        Project project = projectService.getProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("projectRequest", ProjectRequest.fromEntity(project));
        model.addAttribute("projectMembers", projectService.getMembers(id));
        model.addAttribute("projectRoles", ProjectRole.values());

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
            model.addAttribute("project", projectService.getProjectById(id));
            model.addAttribute("projectMembers", projectService.getMembers(id));
            model.addAttribute("projectRoles", ProjectRole.values());
            return "projects/project-settings";
        }
        projectService.updateProject(id, projectRequest.toEntity());
        if (HtmxUtils.isHtmxRequest(request)) {
            return HtmxUtils.triggerEvent("projectSaved");
        }
        return new RedirectView("/projects/" + id);
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
        return new RedirectView("/projects");
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
        return new RedirectView("/projects/" + id);
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

    private void populateMemberModel(Long projectId, Model model) {
        model.addAttribute("project", projectService.getProjectById(projectId));
        model.addAttribute("projectMembers", projectService.getMembers(projectId));
        model.addAttribute("projectRoles", ProjectRole.values());
    }

    private List<List<CalendarDay>> buildCalendarWeeks(
            Long projectId,
            YearMonth month,
            String search,
            TaskStatusFilter statusFilter,
            boolean overdue,
            Priority priority,
            Long selectedUserId,
            List<Long> tags,
            Model model) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate lastOfMonth = month.atEndOfMonth();
        LocalDate gridStart = firstOfMonth.with(DayOfWeek.MONDAY);
        LocalDate gridEnd = lastOfMonth.with(DayOfWeek.SUNDAY);

        Pageable unpaged = Pageable.unpaged(Sort.by(Sort.Direction.ASC, Task.FIELD_DUE_DATE));
        List<Task> tasks =
                taskService
                        .searchAndFilterTasksForProject(
                                projectId,
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
