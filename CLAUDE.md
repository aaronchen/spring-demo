# CLAUDE.md - Project Context for Claude Code

## Project Overview

**Spring Boot Task Manager** - A full-stack task management application demonstrating modern Spring Boot 4.0 development with both REST API and server-side rendered UI.

- **Package**: `cc.desuka.demo`
- **Java Version**: 25
- **Spring Boot**: 4.0.2
- **Database**: H2 in-memory database
- **Template Engine**: Thymeleaf 3.x
- **Frontend**: Bootstrap 5.3.3 + HTMX 2.0.4

## Architecture

### Layered Architecture
The application follows standard Spring Boot layered architecture:

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
   - Follows RESTful conventions
   - Suitable for client applications, mobile apps, etc.

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
  - Extends `JpaRepository<Task, Long>`
  - Custom query methods using Spring Data naming conventions
  - Key methods: `findByCompleted`, `findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase`

#### Service Layer
- `service/TaskService.java` - Business logic layer
  - Constructor injection (preferred Spring pattern)
  - Methods: `getAllTasks`, `getTaskById`, `createTask`, `updateTask`, `deleteTask`, `toggleComplete`, `searchAndFilterTasks`
  - Filter logic: all, completed, incomplete
  - Search logic: case-insensitive search in title and description

#### Controller Layer
- `controller/TaskApiController.java` - REST API endpoints
  - `@RestController` with `/api/tasks` base path
  - Standard HTTP methods: GET, POST, PUT, PATCH, DELETE
  - Returns JSON automatically
  - Uses `@Valid` for validation

- `controller/TaskWebController.java` - Web UI endpoints
  - `@Controller` with `/web/tasks` base path
  - Returns Thymeleaf template names
  - HTMX support: detects `HX-Request` header and returns fragments
  - Uses `RedirectView` for non-HTMX delete requests

#### Utilities
- `util/HtmxUtils.java` - HTMX helper methods
  - `isHtmxRequest(HttpServletRequest)` - checks for `HX-Request: true` header
  - Helps controllers return fragments vs full pages

#### Bootstrap
- `DataLoader.java` - Seeds database with 100+ realistic tasks
  - Implements `CommandLineRunner`
  - Runs on application startup
  - Creates tasks with varied completion status and creation dates

### Thymeleaf Templates

#### Layouts
- `templates/layouts/base.html` - Base layout with reusable fragments
  - `head(title)` - parameterized head fragment
  - `navbar` - navigation bar
  - `footer` - footer
  - `scripts` - Bootstrap + HTMX scripts

#### Task Views
- `templates/tasks/tasks.html` - Main task list page
  - Search bar with HTMX live search (`input changed delay:300ms`)
  - Filter buttons: All, Completed, Pending
  - Grid container targeted by HTMX for dynamic updates
  - JavaScript for filter button active state management

- `templates/tasks/task-card-grid.html` - Grid layout fragment
  - `grid` fragment - renders task cards in responsive grid
  - Empty state when no tasks found
  - Bootstrap grid: `row-cols-1 row-cols-md-2 row-cols-lg-3`

- `templates/tasks/task-card.html` - Individual task card fragment
  - `card` fragment - single task card (non-parameterized, reads from context)
  - Color-coded by status:
    - **Completed**: Green border (`border-success`), green header (`bg-success`)
    - **Incomplete**: Yellow border (`border-warning`), yellow header (`bg-warning`)
  - HTMX actions:
    - Toggle button: `hx-post`, `hx-target`, `hx-swap="outerHTML"`
    - Delete button: Opens Bootstrap modal, then `hx-swap="delete"`
  - Bootstrap modal for delete confirmation

- `templates/tasks/task-form.html` - Create/Edit form
  - Dual-purpose form: create and edit
  - Uses `${isEdit}` flag to customize behavior
  - Validation error display with `is-invalid` class
  - Completion checkbox only shown in edit mode

### Static Resources

- `static/css/custom.css` - Custom styles
  - Card hover effects (`transform: translateY(-5px)`)
  - HTMX indicator styles (hidden by default, shown on `.htmx-request`)
  - Button outline customization for filter buttons
  - Footer styling

- `static/bootstrap-icons/` - Bootstrap Icons (locally hosted)
  - Font files and CSS
  - Used throughout UI for icons

## Important Patterns and Conventions

### Thymeleaf Fragment Pattern

**Non-parameterized fragments reading from context:**
```html
<!-- Fragment definition (no parameters) -->
<div th:fragment="card" class="col">
    <!-- Reads ${task} from context -->
</div>

<!-- Fragment call (no parameters) -->
<div th:replace="~{tasks/task-card :: card}"></div>
```

**Why this pattern?**
- Thymeleaf fragments can read variables from context (th:each loop, model attributes)
- Controller return strings cannot use `${}` expressions in fragment parameters
- Simpler than parameterized fragments for most use cases

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

### HTMX Integration Pattern

**Controller method:**
```java
@PostMapping("/{id}/toggle")
public String toggleComplete(@PathVariable Long id, HttpServletRequest request, Model model) {
    Task task = taskService.toggleComplete(id);

    if (HtmxUtils.isHtmxRequest(request)) {
        model.addAttribute("task", task);
        return "tasks/task-card :: card";  // Return fragment
    }
    return "redirect:/web/tasks";  // Full page redirect for non-HTMX
}
```

**Template:**
```html
<button hx-post="/web/tasks/5/toggle"
        hx-target="#task-card-5"
        hx-swap="outerHTML">
    Toggle
</button>
```

### Constructor Injection Pattern

Always use constructor injection (Spring's preferred pattern):

```java
private final TaskService taskService;

public TaskWebController(TaskService taskService) {
    this.taskService = taskService;
}
```

**Not** `@Autowired` field injection.

### Color-Coded UI Pattern

Task cards use Bootstrap color utilities for status indication:

- **Completed tasks**: `border-success`, `bg-success`, `text-success`
- **Incomplete tasks**: `border-warning`, `bg-warning`, `text-warning`
- Filter buttons match task card colors for visual consistency

### Filter Button Styling

Filter buttons use:
- `btn-outline-light` base class
- `border-secondary` utility class for visible borders
- `fw-bold` for better text visibility
- JavaScript to manage `active` class on click

## Configuration

### Application Properties (`application.properties`)

```properties
# H2 in-memory database (data lost on restart)
spring.datasource.url=jdbc:h2:mem:taskdb
spring.jpa.hibernate.ddl-auto=create-drop  # Recreates schema on startup

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
- `spring-boot-devtools` - Hot reload during development
- `bootstrap` (WebJar 5.3.3) - CSS framework
- `htmx.org` (WebJar 2.0.4) - AJAX library
- `h2` (2.4.240) - In-memory database
- `lombok` - Boilerplate reduction (optional, not used on entities)

## Development Workflow

### Running the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Application runs on: `http://localhost:8080`

### Available URLs

**Web UI:**
- `http://localhost:8080/web/tasks` - Task list
- `http://localhost:8080/web/tasks/new` - Create task
- `http://localhost:8080/web/tasks/{id}/edit` - Edit task

**REST API:**
- `GET /api/tasks` - List all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create task
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `PATCH /api/tasks/{id}/toggle` - Toggle completion
- `GET /api/tasks/search?keyword=...` - Search tasks
- `GET /api/tasks/incomplete` - Get incomplete tasks

**Admin:**
- `http://localhost:8080/h2-console` - H2 database console
  - JDBC URL: `jdbc:h2:mem:taskdb`
  - Username: `sa`
  - Password: (empty)

### Testing with rest.http

The project includes `rest.http` file for testing REST API endpoints in VS Code with REST Client extension.

## Common Issues and Solutions

### Thymeleaf Template Parsing Errors

**Problem:** `Could not parse as expression`

**Solution:** Ensure ternary operators are inside `${}`:
```html
<!-- Correct -->
th:classappend="${condition ? 'class1' : 'class2'}"

<!-- Wrong -->
th:classappend="${condition} ? 'class1' : 'class2'"
```

### Fragment Parameter Errors

**Problem:** `Parameters in a view specification must be named (non-synthetic)`

**Solution:** Don't use `${}` in controller return strings. Use context-based fragments:
```java
// Correct
model.addAttribute("task", task);
return "tasks/task-card :: card";

// Wrong
return "tasks/task-card :: card(${task})";
```

### HTMX Not Working

**Problem:** HTMX requests returning full page instead of fragment

**Solution:** Check controller uses `HtmxUtils.isHtmxRequest(request)` to detect HTMX and return fragment instead of redirect.

### Bootstrap Styles Not Visible

**Problem:** Custom colors or utility classes not working

**Solution:**
1. Use Bootstrap utility classes where possible (`border-secondary`, `fw-bold`)
2. For btn-outline variants, override CSS variables in custom.css
3. Don't override global CSS variables unless necessary

## Database Schema

### tasks table
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

### Recommended Branching Strategy

- `main` - Production-ready code
- `feature/*` - New features (e.g., `feature/add-tags`)
- `fix/*` - Bug fixes (e.g., `fix/search-bug`)
- `refactor/*` - Code improvements (e.g., `refactor/service-layer`)

### Git Ignore

Standard Spring Boot `.gitignore` includes:
- `target/` - Build artifacts
- `.mvn/wrapper/maven-wrapper.jar`
- IDE files (`.idea`, `.vscode`, `.classpath`)

## Future Enhancement Ideas

- Add user authentication and authorization
- Implement task tags/categories
- Add due dates and reminders
- Support task priority levels
- Add pagination for large task lists
- Implement dark mode toggle
- Add task assignment to users
- Export tasks to CSV/PDF
- Add task comments/notes
- Implement recurring tasks
