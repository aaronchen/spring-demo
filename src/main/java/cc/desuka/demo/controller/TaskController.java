package cc.desuka.demo.controller;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.AuditLogService;
import cc.desuka.demo.service.CommentService;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.CsvWriter;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tasks")
public class TaskController {

  private final TaskService taskService;
  private final TagService tagService;
  private final UserService userService;
  private final CommentService commentService;
  private final OwnershipGuard ownershipGuard;
  private final AuditLogService auditLogService;
  private final MessageSource messageSource;

  public TaskController(TaskService taskService, TagService tagService,
      UserService userService, CommentService commentService,
      OwnershipGuard ownershipGuard,
      AuditLogService auditLogService,
      MessageSource messageSource) {
    this.taskService = taskService;
    this.tagService = tagService;
    this.userService = userService;
    this.commentService = commentService;
    this.ownershipGuard = ownershipGuard;
    this.auditLogService = auditLogService;
    this.messageSource = messageSource;
  }

  // GET /tasks - Display task list (full page or HTMX fragment)
  @GetMapping
  public String listTasks(
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = TaskStatusFilter.DEFAULT) TaskStatusFilter statusFilter,
      @RequestParam(required = false, defaultValue = "false") boolean overdue,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) Long selectedUserId,
      @RequestParam(required = false) List<Long> tags,
      @RequestParam(required = false) String view,
      @CookieValue(name = "pageSize", required = false, defaultValue = "25") int pageSizeCookie,
      @PageableDefault(size = 25, sort = Task.FIELD_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request,
      Model model) {
    // userPreferences is already in the model via GlobalModelAttributes
    UserPreferences userPreferences =
        (UserPreferences) model.getAttribute("userPreferences");
    // Default user filter on first visit (no userId param):
    // "mine" preference → current user's tasks; "all" preference → all users.
    if (selectedUserId == null && !request.getParameterMap().containsKey("selectedUserId") && currentDetails != null) {
      if (userPreferences == null
          || UserPreferences.FILTER_MINE.equals(userPreferences.getDefaultUserFilter())) {
        selectedUserId = currentDetails.getUser().getId();
      }
    }
    // View mode: URL param overrides preference default
    String resolvedView = (view != null) ? view
        : (userPreferences != null ? userPreferences.getTaskView() : UserPreferences.VIEW_CARDS);
    if (!request.getParameterMap().containsKey("size")) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageSizeCookie, pageable.getSort());
    }
    Page<Task> taskPage = taskService.searchAndFilterTasks(search, statusFilter, overdue, priority, selectedUserId, tags, pageable);
    model.addAttribute("taskPage", taskPage);
    model.addAttribute("allTags", tagService.getAllTags());
    model.addAttribute("view", resolvedView);
    model.addAttribute("selectedUserId", selectedUserId);

    // Resolve filtered user's name for the user filter button label
    Long currentId = currentDetails != null ? currentDetails.getUser().getId() : null;
    if (selectedUserId != null && !selectedUserId.equals(currentId)) {
      try {
        model.addAttribute("filterUserName", userService.getUserById(selectedUserId).getName());
      } catch (Exception ignored) {}
    }

    if (HtmxUtils.isHtmxRequest(request)) {
      return "table".equals(resolvedView) ? "tasks/task-table :: grid" : "tasks/task-cards :: grid";
    }
    return "tasks/tasks";
  }

  // GET /tasks/export - Download CSV of filtered tasks
  // GET /tasks/export - Download CSV of filtered tasks (same filters as listTasks, unpaged)
  @GetMapping("/export")
  public void exportTasks(
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = TaskStatusFilter.DEFAULT) TaskStatusFilter statusFilter,
      @RequestParam(required = false, defaultValue = "false") boolean overdue,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) Long selectedUserId,
      @RequestParam(required = false) List<Long> tags,
      @PageableDefault(sort = Task.FIELD_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable,
      HttpServletResponse response) throws IOException {

    Pageable unpaged = Pageable.unpaged(pageable.getSort());
    List<Task> tasks = taskService.searchAndFilterTasks(
        search, statusFilter, overdue, priority, selectedUserId, tags, unpaged).getContent();

    Locale locale = Locale.getDefault();
    String[] headers = {
        messageSource.getMessage("task.field.title", null, locale),
        messageSource.getMessage("task.field.status", null, locale),
        messageSource.getMessage("task.field.priority", null, locale),
        messageSource.getMessage("task.field.dueDate", null, locale),
        messageSource.getMessage("task.field.user", null, locale),
        messageSource.getMessage("task.field.tags", null, locale),
        messageSource.getMessage("task.field.createdAt", null, locale),
    };
    CsvWriter.write(response, "tasks.csv", headers, tasks, task -> new String[]{
        task.getTitle(),
        task.getStatus() != null ? task.getStatus().name() : "",
        task.getPriority() != null ? task.getPriority().name() : "",
        task.getDueDate() != null ? task.getDueDate().toString() : "",
        task.getUser() != null ? task.getUser().getName() : "",
        task.getTags() != null
            ? task.getTags().stream().map(t -> t.getName()).collect(Collectors.joining("; "))
            : "",
        task.getCreatedAt() != null ? task.getCreatedAt().toLocalDate().toString() : ""
    });
  }

  // GET /tasks/{id} - Show task in view (read-only) mode
  @GetMapping("/{id}")
  public String showTask(@PathVariable Long id, Model model, HttpServletRequest request) {
    Task task = taskService.getTaskById(id);
    model.addAttribute("task", task);
    model.addAttribute("mode", "view");
    model.addAttribute("tags", tagService.getAllTags());
    model.addAttribute("comments", commentService.getCommentsByTaskId(id));
    model.addAttribute("auditHistory",
        auditLogService.getEntityHistory(Task.class, id));
    if (HtmxUtils.isHtmxRequest(request)) {
      return "tasks/task-modal";
    }
    return "tasks/task";
  }

  // GET /tasks/new - Show create form (full page or modal fragment)
  // Default user assignment is the current user (can be changed via dropdown).
  @GetMapping("/new")
  public String showCreateForm(Model model, HttpServletRequest request,
      @AuthenticationPrincipal CustomUserDetails currentDetails) {
    Task task = new Task();
    task.setUser(currentDetails.getUser());
    model.addAttribute("task", task);
    model.addAttribute("mode", "create");
    model.addAttribute("tags", tagService.getAllTags());
    model.addAttribute("comments", Collections.emptyList());
    model.addAttribute("auditHistory", Collections.emptyList());
    if (HtmxUtils.isHtmxRequest(request)) {
      return "tasks/task-modal";
    }
    return "tasks/task";
  }

  // POST /tasks - Create new task
  // Defaults to current user; user can pick a different assignee via the dropdown.
  // tagIds: list of selected tag IDs from form checkboxes. null when no checkbox is checked.
  @PostMapping
  public Object createTask(
      @Valid @ModelAttribute Task task, BindingResult result,
      @RequestParam(required = false) List<Long> tagIds,
      @RequestParam(required = false) Long assigneeId,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request, Model model) {
    if (result.hasErrors()) {
      model.addAttribute("mode", "create");
      model.addAttribute("tags", tagService.getAllTags());
      if (HtmxUtils.isHtmxRequest(request)) {
        return "tasks/task-modal";
      }
      return "tasks/task";
    }
    Task created = taskService.createTask(task, tagIds, assigneeId);
    if (HtmxUtils.isHtmxRequest(request)) {
      return HtmxUtils.triggerEvent("taskSaved");
    }
    return new RedirectView("/tasks/" + created.getId());
  }

  // GET /tasks/{id}/edit - Show edit form (full page or modal fragment)
  // Owner or admin may edit. Unassigned tasks are open to any user.
  @GetMapping("/{id}/edit")
  public String showEditForm(@PathVariable Long id, Model model, HttpServletRequest request,
      @AuthenticationPrincipal CustomUserDetails currentDetails) {
    Task task = taskService.getTaskById(id);
    if (task.getUser() != null) {
      ownershipGuard.requireAccess(task, currentDetails);
    }
    model.addAttribute("task", task);
    model.addAttribute("mode", "edit");
    model.addAttribute("tags", tagService.getAllTags());
    model.addAttribute("comments", commentService.getCommentsByTaskId(id));
    model.addAttribute("auditHistory",
        auditLogService.getEntityHistory(Task.class, id));
    if (HtmxUtils.isHtmxRequest(request)) {
      return "tasks/task-modal";
    }
    return "tasks/task";
  }

  // POST /tasks/{id} - Update task
  // Owner or admin may update. Unassigned tasks are open to any user.
  // tagIds: null means remove all tags. assigneeId comes from the user select dropdown.
  @PostMapping("/{id}")
  public Object updateTask(
      @PathVariable Long id,
      @Valid @ModelAttribute Task task, BindingResult result,
      @RequestParam(required = false) List<Long> tagIds,
      @RequestParam(required = false) Long assigneeId,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request, Model model) {
    Task existing = taskService.getTaskById(id);
    if (existing.getUser() != null) {
      ownershipGuard.requireAccess(existing, currentDetails);
    }
    if (result.hasErrors()) {
      model.addAttribute("mode", "edit");
      model.addAttribute("tags", tagService.getAllTags());
      if (HtmxUtils.isHtmxRequest(request)) {
        return "tasks/task-modal";
      }
      return "tasks/task";
    }
    taskService.updateTask(id, task, tagIds, assigneeId, task.getVersion());
    if (HtmxUtils.isHtmxRequest(request)) {
      return HtmxUtils.triggerEvent("taskSaved");
    }
    return new RedirectView("/tasks/" + id);
  }

  // DELETE /tasks/{id} - Delete task
  // Owner or admin may delete. Unassigned tasks are open to any user.
  @DeleteMapping("/{id}")
  public Object deleteTask(@PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request, Model model) {
    Task task = taskService.getTaskById(id);
    if (task.getUser() != null) {
      ownershipGuard.requireAccess(task, currentDetails);
    }
    taskService.deleteTask(id);
    if (HtmxUtils.isHtmxRequest(request)) {
      return HtmxUtils.triggerEvent("taskDeleted");
    }
    return new RedirectView("/tasks");
  }

  // GET /tasks/{id}/comments - Fetch comment list fragment (HTMX live refresh)
  @GetMapping("/{id}/comments")
  public String getComments(@PathVariable Long id, Model model) {
    model.addAttribute("task", taskService.getTaskById(id));
    model.addAttribute("comments", commentService.getCommentsByTaskId(id));
    return "tasks/task-comments";
  }

  // POST /tasks/{id}/comments - Add a comment to a task
  // Any authenticated user may comment on any task.
  @PostMapping("/{id}/comments")
  public Object addComment(@PathVariable Long id,
      @RequestParam String text,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request, Model model) {
    commentService.createComment(text, id, currentDetails.getUser().getId());
    if (HtmxUtils.isHtmxRequest(request)) {
      model.addAttribute("task", taskService.getTaskById(id));
      model.addAttribute("comments", commentService.getCommentsByTaskId(id));
      return "tasks/task-comments";
    }
    return new RedirectView("/tasks/" + id);
  }

  // DELETE /tasks/{id}/comments/{commentId} - Delete a comment
  // Owner of the comment or admin may delete.
  @DeleteMapping("/{id}/comments/{commentId}")
  public Object deleteComment(@PathVariable Long id, @PathVariable Long commentId,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request, Model model) {
    Comment comment = commentService.getCommentById(commentId);
    if (comment.getUser() != null) {
      ownershipGuard.requireAccess(comment, currentDetails);
    }
    commentService.deleteComment(commentId);
    if (HtmxUtils.isHtmxRequest(request)) {
      model.addAttribute("task", taskService.getTaskById(id));
      model.addAttribute("comments", commentService.getCommentsByTaskId(id));
      return "tasks/task-comments";
    }
    return new RedirectView("/tasks/" + id);
  }

  // POST /tasks/{id}/toggle - Advance status (OPEN → IN_PROGRESS → COMPLETED → OPEN)
  // Available to all authenticated users — no ownership restriction.
  @PostMapping("/{id}/toggle")
  public String advanceStatus(
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = "cards") String view,
      HttpServletRequest request,
      Model model) {
    Task task = taskService.advanceStatus(id);

    if (HtmxUtils.isHtmxRequest(request)) {
      model.addAttribute("task", task);
      return "table".equals(view) ? "tasks/task-table-row :: row" : "tasks/task-card :: card";
    }
    return "redirect:/tasks";
  }
}
