package cc.desuka.demo.controller;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.service.TaskService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/web/tasks")
public class TaskWebController {

  private final TaskService taskService;

  public TaskWebController(TaskService taskService) {
    this.taskService = taskService;
  }

  // GET /web/tasks - Display all tasks
  @GetMapping
  public String listTasks(Model model) {
    model.addAttribute("tasks", taskService.getAllTasks());
    return "tasks/tasks";
  }

  // GET /web/tasks/search - Search tasks (HTMX endpoint)
  @GetMapping("/search")
  public String searchTasks(
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = "all") String filter,
      Model model) {
    List<Task> tasks = taskService.searchAndFilterTasks(search, filter);
    model.addAttribute("tasks", tasks);
    return "tasks/task-card-grid :: grid";
  }

  // GET /web/tasks/new - Show create form
  @GetMapping("/new")
  public String showCreateForm(Model model) {
    model.addAttribute("task", new Task());
    model.addAttribute("isEdit", false);
    return "tasks/task-form";
  }

  // POST /web/tasks - Create new task
  @PostMapping
  public String createTask(@Valid @ModelAttribute Task task, BindingResult result) {
    if (result.hasErrors()) {
      return "tasks/task-form";
    }
    taskService.createTask(task);
    return "redirect:/web/tasks";
  }

  // GET /web/tasks/{id}/edit - Show edit form
  @GetMapping("/{id}/edit")
  public String showEditForm(@PathVariable Long id, Model model) {
    Task task = taskService.getTaskById(id);
    model.addAttribute("task", task);
    model.addAttribute("isEdit", true);
    return "tasks/task-form";
  }

  // POST /web/tasks/{id} - Update task
  @PostMapping("/{id}")
  public String updateTask(@PathVariable Long id, @Valid @ModelAttribute Task task, BindingResult result) {
    if (result.hasErrors()) {
      return "tasks/task-form";
    }
    taskService.updateTask(id, task);
    return "redirect:/web/tasks";
  }

  // POST /web/tasks/{id}/delete - Delete task
  // Supports both HTMX (returns empty response to remove element) and regular form (redirects)
  @PostMapping("/{id}/delete")
  public Object deleteTask(@PathVariable Long id, HttpServletRequest request, Model model) {
    taskService.deleteTask(id);

    if (HtmxUtils.isHtmxRequest(request)) {
      return ResponseEntity.ok().build(); // HTTP 200 with empty body removes the element
    }
    return new RedirectView("/web/tasks");
  }

  // POST /web/tasks/{id}/toggle - Toggle completion
  // Supports both HTMX (returns updated card) and regular form (redirects)
  @PostMapping("/{id}/toggle")
  public String toggleComplete(@PathVariable Long id, HttpServletRequest request, Model model) {
    Task task = taskService.toggleComplete(id);

    if (HtmxUtils.isHtmxRequest(request)) {
      model.addAttribute("task", task);
      return "tasks/task-card :: card"; // Return just the updated card (reads task from model)
    }
    return "redirect:/web/tasks";
  }
}
