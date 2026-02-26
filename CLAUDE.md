# CLAUDE.md - Project Context for Claude Code

## Project Overview

**Spring Workshop** - A growing full-stack learning project demonstrating modern Spring Boot 4.0 development with both REST API and server-side rendered UI. New features are added incrementally as we explore Spring Boot patterns together.

- **Package**: `cc.desuka.demo`
- **Java Version**: 25
- **Spring Boot**: 4.0.3
- **Database**: H2 in-memory database
- **Template Engine**: Thymeleaf 3.x
- **Frontend**: Bootstrap 5.3.3 + HTMX 2.0.4

## Architecture

### Layered Architecture
```
Controller Layer (REST API + Web)
         ↓
  Service Layer
         ↓
Repository Layer (Spring Data JPA)
         ↓
   Database (H2)
```

### Dual Interface Pattern
The application provides **two interfaces** for the same backend:

1. **REST API** (`TaskApiController`) - `/api/tasks/*`
   - JSON-based CRUD operations
   - Returns `Task` entity directly (no DTO layer in use)
   - DTOs exist (`TaskRequest`, `TaskResponse`) but are not yet wired in

2. **Web UI** (`TaskWebController`) - `/web/tasks/*`
   - Server-side rendered with Thymeleaf
   - HTMX for dynamic updates without full page reloads
   - Bootstrap for responsive styling

## Key Files and Structure

### Java Source Files

#### Model Layer
- `model/Task.java` - Entity class with JPA annotations
  - Fields: id, title, description, completed, createdAt
  - Validation: `@NotBlank`, `@Size` constraints
  - Manual getters/setters (no Lombok on entities)

#### Repository Layer
- `repository/TaskRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Task, Long>` and `JpaSpecificationExecutor<Task>`
  - Active query methods:
    - `findByCompleted(boolean)` - used by `getIncompleteTasks()`
    - `findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String, String)` - used by `searchTasks()`
  - `JpaSpecificationExecutor` used by `searchAndFilterTasks()` for paginated filtering

- `repository/TaskSpecifications.java` - JPA Specifications for dynamic queries
  - `build(keyword, filter)` - builds a combined search + filter specification

#### Service Layer
- `service/TaskService.java` - Business logic layer
  - Constructor injection (preferred Spring pattern)
  - Active methods: `getAllTasks`, `getTaskById`, `createTask`, `updateTask`, `deleteTask`, `getIncompleteTasks`, `searchTasks`, `searchAndFilterTasks(keyword, filter, pageable)`, `toggleComplete`

#### Controller Layer
- `controller/TaskApiController.java` - REST API endpoints
  - `@RestController` with `/api/tasks` base path
  - Standard HTTP methods: GET, POST, PUT, PATCH, DELETE
  - Returns JSON automatically via entity serialization

- `controller/TaskWebController.java` - Web UI endpoints
  - `@Controller` with `/web/tasks` base path
  - Returns Thymeleaf template names or fragment selectors
  - HTMX support: detects `HX-Request` header via `HtmxUtils.isHtmxRequest()`
  - `Object` return type on POST methods to allow returning either a String view name or `ResponseEntity`
  - Fires `HX-Trigger` events (`taskSaved`, `taskDeleted`) via `HtmxUtils.triggerEvent()`

#### Utilities
- `util/HtmxUtils.java` - HTMX helper methods
  - `isHtmxRequest(HttpServletRequest)` - checks for `HX-Request: true` header
  - `triggerEvent(String eventName)` - returns `ResponseEntity` with `HX-Trigger` header set

#### Bootstrap
- `DataLoader.java` - Seeds database with 100+ realistic tasks on startup

### Thymeleaf Templates

#### Layouts
- `templates/layouts/base.html` - Base layout with reusable fragments
  - `head(title, cssFile)` - two-parameter head fragment; `cssFile` is nullable for pages without page-specific CSS
  - `navbar` - navigation bar
  - `footer` - footer
  - `scripts` - Bootstrap + HTMX + `utils.js`

#### Task Views
- `templates/tasks/tasks.html` - Main task list page
  - Live search (JS-debounced, 300ms), filter buttons, sort dropdown, view toggle (cards/table)
  - All state managed in JS (`tasks.js`) — synced to URL params and cookies
  - Contains two shared modal shells loaded once per page:
    - `#task-modal` — create/edit form, content loaded via HTMX
    - `#task-delete-modal` — delete confirmation, populated via `show.bs.modal` JS event

- `templates/tasks/task-cards.html` - Card grid fragment
  - `grid` fragment — renders task cards + top/bottom pagination bars

- `templates/tasks/task-card.html` - Individual task card fragment
  - `card` fragment — reads `${task}` from context (non-parameterized)
  - Color-coded: green = completed, yellow = pending

- `templates/tasks/task-table.html` - Table view fragment
  - `grid` fragment — sortable columns, renders rows via `task-table-row :: row`

- `templates/tasks/task-table-row.html` - Single table row fragment
  - `row` fragment — reads `${task}` from context

- `templates/tasks/task-pagination.html` - Pagination control bar
  - `controlBar(position)` fragment — top/bottom bars with page nav and page-size selector

- `templates/tasks/task-form.html` - **Shared form fields fragment only**
  - `fields` fragment — title, description, completed checkbox (edit only)
  - No `<form>` tag; `th:object` is set by the including template
  - Used by both `task.html` and `task-modal.html`

- `templates/tasks/task.html` - Full-page create/edit form
  - Standalone page with full layout (navbar, footer)
  - Includes `task-form :: fields` inside its own `<form th:object="${task}">`

- `templates/tasks/task-modal.html` - HTMX modal content (bare file, no HTML wrapper)
  - Returned by controller as `"tasks/task-modal"` (whole file is the response)
  - Includes `task-form :: fields` inside its own `<form>` with `hx-post`
  - Swapped into `#task-modal-content` by HTMX

### Static Resources

- `static/css/base.css` - Global styles (body, card hover, btn transitions, validation, navbar, footer, HTMX indicator)
- `static/css/tasks.css` - Task page styles (filter button active states, table action button overrides)
- `static/js/utils.js` - Shared browser utilities (`getCookie`, `setCookie`); loaded globally via base layout
- `static/js/tasks.js` - Task list page logic (sort, filter, search, pagination, modal wiring); loaded only by `tasks.html`
- `static/bootstrap-icons/` - Bootstrap Icons (locally hosted)

## Important Patterns and Conventions

### Thymeleaf Fragment Pattern

**Bare fragment files (no HTML wrapper):**
```html
<!-- task-modal.html — whole file is the response content -->
<div class="modal-header" xmlns:th="http://www.thymeleaf.org">...</div>
<form ...>...</form>
```
Controller returns `"tasks/task-modal"` — no `::` fragment selector needed.

**Fragment files with HTML wrapper (need `::` selector):**
```html
<!-- task-card.html — full HTML document, fragment is an inner element -->
<div th:fragment="card" ...>...</div>
```
Controller returns `"tasks/task-card :: card"`.

**Non-parameterized fragments reading from context:**
```html
<div th:fragment="card" class="col">
    <!-- Reads ${task} from context set by th:each or model -->
</div>
```
Use this pattern because controller return strings cannot use `${}` expressions in fragment parameters.

### Thymeleaf Ternary Operator Syntax

**Correct:**
```html
th:classappend="${task.completed ? 'border-success' : 'border-warning'}"
```

**Incorrect (will fail to parse):**
```html
th:classappend="${task.completed} ? 'border-success' : 'border-warning'"
```

The ternary operator `? :` must be **inside** the `${}` expression.

### `th:object` Propagation to Fragments

`th:object` set on a `<form>` propagates into included fragments, so `*{field}` expressions work inside `task-form :: fields` even though the `<form>` tag is in the parent template. This enables the shared fields fragment pattern.

### HTMX Fragment Pattern

Controller detects HTMX request and returns a fragment:
```java
if (HtmxUtils.isHtmxRequest(request)) {
    model.addAttribute("task", task);
    return "tasks/task-card :: card";
}
return "redirect:/web/tasks";
```

### HTMX Event Trigger Pattern

For actions that need client-side side effects after success (close modal, refresh grid):
```java
// Controller fires a named event
return HtmxUtils.triggerEvent("taskSaved");
// → ResponseEntity 200 with header: HX-Trigger: taskSaved
```

```javascript
// JavaScript listens for the event
document.body.addEventListener('taskSaved', function() {
    bootstrap.Modal.getInstance(...).hide();
    doSearch(false);
});
```
Active events: `taskSaved` (create/update), `taskDeleted` (delete).

### Single Shared Modal Pattern

One modal shell in `tasks.html` serves all tasks. Populated dynamically:

**Task form modal** — content loaded via HTMX:
```html
<button hx-get="/web/tasks/new" hx-target="#task-modal-content" hx-swap="innerHTML">
```
`htmx:afterSwap` fires Bootstrap `modal.show()` when `#task-modal-content` is populated.

**Delete confirmation modal** — content populated via JS:
```javascript
document.getElementById('task-delete-modal').addEventListener('show.bs.modal', function(e) {
    const btn = e.relatedTarget; // the triggering delete button
    document.getElementById('task-delete-modal-title').textContent = btn.dataset.taskTitle;
    const confirmBtn = document.getElementById('delete-confirm-btn');
    confirmBtn.setAttribute('hx-post', '/web/tasks/' + btn.dataset.taskId + '/delete');
    htmx.process(confirmBtn); // re-process after dynamic hx-post assignment
});
```
Delete buttons carry `data-task-id` and `data-task-title` attributes.

### Constructor Injection Pattern

Always use constructor injection:
```java
private final TaskService taskService;

public TaskWebController(TaskService taskService) {
    this.taskService = taskService;
}
```
Not `@Autowired` field injection.

### CSS Organization

- **`base.css`** — styles used on every page; included by `base.html`
- **`tasks.css`** — styles only needed on task pages; passed to `head(title, cssFile)` parameter
- For btn-outline active state overrides, use Bootstrap CSS custom properties (`--bs-btn-active-bg`, etc.) rather than class overrides
- Bootstrap utility classes use `!important` — override with `!important` if needed

### Page-Specific CSS via Head Fragment

```html
<!-- base.html head fragment accepts optional cssFile -->
<head th:fragment="head(title, cssFile)">
    <link rel="stylesheet" th:href="@{/css/base.css}">
    <link th:if="${cssFile != null}" rel="stylesheet" th:href="${cssFile}">
</head>

<!-- tasks.html and task.html pass tasks.css -->
<head th:replace="~{layouts/base :: head('Tasks', '/css/tasks.css')}"></head>

<!-- Future non-task pages pass null -->
<head th:replace="~{layouts/base :: head('Some Page', null)}"></head>
```

## Configuration

### Application Properties (`application.properties`)

```properties
spring.application.name=demo

# H2 in-memory database (data lost on restart)
spring.datasource.url=jdbc:h2:mem:taskdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# H2 Console: http://localhost:8080/h2-console
spring.h2.console.enabled=true

# SQL logging (helpful for debugging)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Maven Dependencies (pom.xml)

Key dependencies:
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-jpa` - JPA/Hibernate
- `spring-boot-starter-thymeleaf` - Template engine
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-actuator` - Health and metrics endpoints
- `spring-boot-devtools` - Hot reload during development
- `bootstrap` (WebJar 5.3.3) - CSS framework
- `htmx.org` (WebJar 2.0.4) - AJAX library
- `h2` (2.4.240) - In-memory database
- `lombok` - Boilerplate reduction (not used on entities)

## Development Workflow

### Running the Application

```bash
./mvnw spring-boot:run
```
Application runs on: `http://localhost:8080`

### Available URLs

**Web UI:**
- `http://localhost:8080/web/tasks` - Task list (cards or table view)
- `http://localhost:8080/web/tasks/new` - Create task (full page; modal preferred)
- `http://localhost:8080/web/tasks/{id}/edit` - Edit task (full page; modal preferred)

**REST API:**
- `GET /api/tasks` - List all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create task
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `PATCH /api/tasks/{id}/toggle` - Toggle completion
- `GET /api/tasks/search?keyword=...` - Search by title/description
- `GET /api/tasks/incomplete` - Get incomplete tasks only

**Admin:**
- `http://localhost:8080/h2-console` - H2 database console
  - JDBC URL: `jdbc:h2:mem:taskdb` / Username: `sa` / Password: (empty)

### Testing with rest.http

The project includes `rest.http` for testing REST API endpoints with VS Code REST Client extension.

## Common Issues and Solutions

### Thymeleaf Template Parsing Errors

**Problem:** `Could not parse as expression`

**Solution:** Ensure ternary operators are inside `${}`:
```html
th:classappend="${condition ? 'class1' : 'class2'}"   <!-- correct -->
th:classappend="${condition} ? 'class1' : 'class2'"   <!-- wrong -->
```

### Fragment Parameter Errors

**Problem:** `Parameters in a view specification must be named (non-synthetic)`

**Solution:** Don't use `${}` in controller return strings. Put data in model:
```java
model.addAttribute("task", task);
return "tasks/task-card :: card";   // correct
return "tasks/task-card :: card(${task})";  // wrong
```

### HTMX dynamic attribute not firing

**Problem:** Dynamically set `hx-post` (via `setAttribute`) doesn't fire

**Solution:** Call `htmx.process(element)` after setting the attribute to re-initialize HTMX on that element.

### HTMX Not Working

**Problem:** HTMX requests returning full page instead of fragment

**Solution:** Check controller uses `HtmxUtils.isHtmxRequest(request)` to detect HTMX and return fragment instead of redirect.

### Bootstrap Active State Color Not Applying

**Problem:** CSS custom property override for `--bs-btn-active-color` ignored

**Solution:** Bootstrap utility classes use `!important`. Override with `!important` on the active selector:
```css
#filter-completed.active { color: #fff !important; }
```

## Database Schema

```sql
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);
```

## Git Workflow

- `main` - Production-ready code
- `feature/*` - New features
- `fix/*` - Bug fixes
- `refactor/*` - Code improvements

Always ask before committing. Never auto-commit.

## Future Enhancement Ideas

- Add user authentication and authorization
- Implement task tags/categories
- Add due dates and reminders
- Support task priority levels
- Implement dark mode toggle
- Add task assignment to users
- Export tasks to CSV/PDF
- Add task comments/notes
- Implement recurring tasks
