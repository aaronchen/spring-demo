package cc.desuka.demo.controller;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.OwnershipGuard;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskController {

  private final TaskService taskService;
  private final TagService tagService;
  private final OwnershipGuard ownershipGuard;

  public TaskController(TaskService taskService, TagService tagService,
      OwnershipGuard ownershipGuard) {
    this.taskService = taskService;
    this.tagService = tagService;
    this.ownershipGuard = ownershipGuard;
  }

  // GET /tasks - Display task list (full page or HTMX fragment)
  @GetMapping
  public String listTasks(
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = "all") TaskStatusFilter statusFilter,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) List<Long> tags,
      @RequestParam(required = false) String view,
      @CookieValue(name = "view", required = false, defaultValue = "cards") String viewCookie,
      @CookieValue(name = "pageSize", required = false, defaultValue = "25") int pageSizeCookie,
      @PageableDefault(size = 25, sort = Task.FIELD_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails currentDetails,
      HttpServletRequest request,
      Model model) {
    // Default to current user's tasks when userId param is absent (first visit).
    // An explicit empty userId= (from "All Users" click) is present in the param map
    // but binds as null — distinguished by checking the raw param map.
    if (userId == null && !request.getParameterMap().containsKey("userId") && currentDetails != null) {
      userId = currentDetails.getUser().getId();
    }
    String resolvedView = (view != null) ? view : viewCookie;
    if (!request.getParameterMap().containsKey("size")) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageSizeCookie, pageable.getSort());
    }
    Page<Task> taskPage = taskService.searchAndFilterTasks(search, statusFilter, userId, tags, pageable);
    model.addAttribute("taskPage", taskPage);
    model.addAttribute("allTags", tagService.getAllTags());
    model.addAttribute("view", resolvedView);

    if (HtmxUtils.isHtmxRequest(request)) {
      return "table".equals(resolvedView) ? "tasks/task-table :: grid" : "tasks/task-cards :: grid";
    }
    return "tasks/tasks";
  }

  // GET /tasks/{id} - Show task in view (read-only) mode
  @GetMapping("/{id}")
  public String showTask(@PathVariable Long id, Model model, HttpServletRequest request) {
    Task task = taskService.getTaskById(id);
    model.addAttribute("task", task);
    model.addAttribute("mode", "view");
    model.addAttribute("tags", tagService.getAllTags());
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
      @RequestParam(required = false) Long userId,
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
    Task created = taskService.createTask(task, tagIds, userId);
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
    if (HtmxUtils.isHtmxRequest(request)) {
      return "tasks/task-modal";
    }
    return "tasks/task";
  }

  // POST /tasks/{id} - Update task
  // Owner or admin may update. Unassigned tasks are open to any user.
  // tagIds: null means remove all tags. userId comes from the user select dropdown.
  @PostMapping("/{id}")
  public Object updateTask(
      @PathVariable Long id,
      @Valid @ModelAttribute Task task, BindingResult result,
      @RequestParam(required = false) List<Long> tagIds,
      @RequestParam(required = false) Long userId,
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
    taskService.updateTask(id, task, tagIds, userId);
    if (HtmxUtils.isHtmxRequest(request)) {
      return HtmxUtils.triggerEvent("taskSaved");
    }
    return new RedirectView("/tasks/" + id);
  }

  // POST /tasks/{id}/delete - Delete task
  // Owner or admin may delete. Unassigned tasks are open to any user.
  @PostMapping("/{id}/delete")
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

  // POST /tasks/{id}/toggle - Toggle completion
  // Available to all authenticated users — no ownership restriction on toggling.
  @PostMapping("/{id}/toggle")
  public String toggleComplete(
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = "cards") String view,
      HttpServletRequest request,
      Model model) {
    Task task = taskService.toggleComplete(id);

    if (HtmxUtils.isHtmxRequest(request)) {
      model.addAttribute("task", task);
      return "table".equals(view) ? "tasks/task-table-row :: row" : "tasks/task-card :: card";
    }
    return "redirect:/tasks";
  }
}
