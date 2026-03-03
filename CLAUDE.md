# CLAUDE.md - Project Context for Claude Code

## Project Overview

**Spring Workshop** - A growing full-stack learning project demonstrating modern Spring Boot 4.0 development with both REST API and server-side rendered UI. New features are added incrementally as we explore Spring Boot patterns together.

- **Package**: `cc.desuka.demo`
- **Java Version**: 25
- **Spring Boot**: 4.0.3
- **Security**: Spring Security 7.0 (form login, BCrypt, role-based access)
- **Database**: H2 in-memory database
- **Template Engine**: Thymeleaf 3.x
- **Frontend**: Bootstrap 5.3.3 + HTMX 2.0.4

## Architecture

### Layered Architecture
```
       Spring Security Filter Chain
       (authentication, CSRF, role checks)
                   ↓
      Controller Layer (REST API + Web)
          ↓                  ↓
   OwnershipGuard      AuthExpressions
   (server-side)       (template-side)
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
   - Accepts `TaskRequest` DTO for input; returns `TaskResponse` DTO for output
   - `TaskMapper` (MapStruct) handles all entity ↔ DTO conversion; `TaskMapperImpl` is generated at compile time

2. **Web UI** (`TaskWebController`) - `/tasks/*`
   - Server-side rendered with Thymeleaf
   - HTMX for dynamic updates without full page reloads
   - Bootstrap for responsive styling

## Key Files and Structure

### Java Source Files

#### Model Layer
- `model/Task.java` - Entity class with JPA annotations; implements `OwnedEntity`
  - Fields: id, title, description, completed, createdAt, tags, user
  - `@ManyToMany(fetch = LAZY)` + `@JoinTable(name = "task_tags")` — Task is the owning side
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — Task owns the FK column; user is optional (nullable)
  - Validation: `@NotBlank`, `@Size` constraints
  - Manual getters/setters (no Lombok on entities)

- `model/OwnedEntity.java` - Marker interface for entities that have an owner
  - Single method: `User getUser()` — returns owner or null if unassigned
  - Implemented by `Task`; enables generic ownership checks via `AuthExpressions` and `OwnershipGuard`
  - Future entities with ownership can implement this for automatic access control

- `model/Role.java` - Enum with two values: `USER`, `ADMIN`
  - Stored as string in database via `@Enumerated(EnumType.STRING)` on User
  - Defaults to `USER` for new registrations and API-created users

- `model/Tag.java` - Tag entity
  - Fields: id, name (unique, max 50 chars)
  - `@ManyToMany(mappedBy = "tags", fetch = LAZY)` — Tag is the inverse side (no @JoinTable here)
  - Manual getters/setters; `equals()`/`hashCode()` on `id` only

- `model/User.java` - User entity with authentication fields
  - Fields: id, name (max 100), email (max 150, unique), password (max 72, nullable), role (Role enum, defaults to USER)
  - `password` — BCrypt hash; nullable for API-created users (who cannot log in)
  - `role` — `@Enumerated(EnumType.STRING)`, stored as "USER" or "ADMIN" in the database
  - `@OneToMany(mappedBy = "user", fetch = LAZY)` — inverse side; no cascade (service handles task reassignment on delete)
  - Manual getters/setters; `equals()`/`hashCode()` on `id` only

#### Repository Layer
- `repository/TaskRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Task, Long>` and `JpaSpecificationExecutor<Task>`
  - Active query methods:
    - `findByCompleted(boolean)` - used by `getIncompleteTasks()`
    - `findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String, String)` - used by `searchTasks()`
    - `findByUser(User)` - used by `UserService.deleteUser()` to reassign tasks before deleting a user
  - `@EntityGraph(attributePaths = {"tags", "user"})` on the paginated query — loads both associations in one LEFT JOIN to prevent N+1
  - `JpaSpecificationExecutor` used by `searchAndFilterTasks()` for paginated filtering

- `repository/TaskSpecifications.java` - JPA Specifications for dynamic queries
  - `build(keyword, filter)` - builds a combined search + filter specification

- `repository/UserRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<User, Long>`
  - `findByEmail(String)` — used by `CustomUserDetailsService` for login and `RegistrationController` for duplicate checks

#### DTO Layer
- `dto/TaskRequest.java` - API input DTO (create and update operations)
  - Fields: `title` (required, 1–100 chars), `description` (optional, max 500 chars), `tagIds` (optional `List<Long>`), `userId` (optional `Long`)
  - Validation annotations used by `@Valid` in the controller
  - Lombok `@Data` for getters/setters/equals/hashCode

- `dto/TaskResponse.java` - API output DTO (returned by all read/write endpoints)
  - Fields: `id`, `title`, `description`, `completed`, `createdAt`, `tags` (`List<TagResponse>`), `user` (`UserResponse`, nullable)
  - Lombok `@Data`

- `dto/TagResponse.java` - Tag output DTO
  - Fields: `id`, `name`
  - Lombok `@Data`

- `dto/UserRequest.java` - User input DTO
  - Fields: `name` (required, max 100), `email` (required, max 150)
  - Lombok `@Data`

- `dto/UserResponse.java` - User output DTO
  - Fields: `id`, `name`, `email`
  - Lombok `@Data`

- `dto/RegistrationRequest.java` - Registration form DTO
  - Fields: `name` (required, max 100), `email` (required, max 150), `password` (required, 8–72 chars), `confirmPassword` (required)
  - Cross-field validation (password match) handled programmatically in `RegistrationController`
  - Lombok `@Data`

#### Mapper Layer
- `mapper/TaskMapper.java` - MapStruct mapper interface
  - `@Mapper(componentModel = "spring", uses = {TagMapper.class, UserMapper.class})` — auto-discovers nested converters
  - `toResponse(Task)` — MapStruct auto-calls `TagMapper` and `UserMapper` for relationship fields
  - `toResponseList(List<Task>)` — generated automatically
  - `toEntity(TaskRequest)` — `id`, `completed`, `createdAt`, `tags`, `user` explicitly ignored (service resolves relationships)
  - Implementation `TaskMapperImpl` generated into `target/generated-sources/` at compile time

- `mapper/TagMapper.java` - MapStruct mapper for Tag ↔ TagResponse

- `mapper/UserMapper.java` - MapStruct mapper for User ↔ UserResponse / UserRequest

#### Service Layer
- `service/TaskService.java` - Business logic layer
  - Constructor injection (preferred Spring pattern)
  - Active methods: `getAllTasks`, `getTaskById`, `createTask(task, tagIds, userId)`, `updateTask(id, task, tagIds, userId)`, `deleteTask`, `getIncompleteTasks`, `searchTasks`, `searchAndFilterTasks(keyword, filter, pageable)`, `toggleComplete`
  - `resolveUser(Long userId)` helper — returns null for null input, otherwise looks up user (silent no-op if ID not found)

- `service/UserService.java` - User business logic
  - `getAllUsers`, `getUserById`, `findByEmail`, `createUser`, `updateRole`, `deleteUser`
  - `findByEmail(String)` — returns `Optional<User>`; used by registration duplicate check and `CustomUserDetailsService`
  - `updateRole(Long userId, Role role)` — loads user, sets role, saves; called by `AdminController`
  - `deleteUser` first reassigns all that user's tasks to null (via `taskRepository.findByUser`), then deletes — prevents FK constraint failure

#### Controller Layer
- `controller/api/TaskApiController.java` - Task REST API endpoints
  - `@RestController` with `/api/tasks` base path
  - Standard HTTP methods: GET, POST, PUT, PATCH, DELETE
  - Accepts `TaskRequest` (includes `tagIds`, `userId`), returns `TaskResponse` — no raw entity exposure
  - Injects `TaskMapper` for all DTO ↔ entity conversion
  - **Security**: injects `OwnershipGuard`; uses `@AuthenticationPrincipal CustomUserDetails` on POST, PUT, DELETE
  - POST: auto-assigns task to caller; admins can override via `request.getUserId()`
  - PUT/DELETE: calls `ownershipGuard.requireAccess()` — owner or admin only
  - PATCH toggle: open to all authenticated users (matches web UI behavior)

- `controller/api/UserApiController.java` - User REST API endpoints
  - `@RestController` with `/api/users` base path
  - `GET /api/users` — list all; `GET /api/users/{id}` — get by id; `POST /api/users` (201) — create; `DELETE /api/users/{id}` (204) — delete
  - **Security**: POST and DELETE restricted to admins via `SecurityConfig` URL matchers (no code changes needed here)

- `controller/api/TagApiController.java` - Tag REST API endpoints
  - `@RestController` with `/api/tags` base path
  - `GET /api/tags` — list all; `GET /api/tags/{id}` — get by id; `POST /api/tags` (201) — create; `DELETE /api/tags/{id}` (204) — delete (join table rows cleaned up by Hibernate; tasks are not deleted)
  - **Security**: POST and DELETE restricted to admins via `SecurityConfig` URL matchers (no code changes needed here)

- `controller/HomeController.java` - Home page
  - `@Controller` — single `GET /` mapping, returns `"home"` template

- `controller/LoginController.java` - Login page
  - `@Controller` — single `GET /login` mapping, returns `"login"` template
  - Spring Security handles `POST /login` automatically via `UsernamePasswordAuthenticationFilter`

- `controller/RegistrationController.java` - User self-registration
  - `GET /register` — serves registration form with empty `RegistrationRequest`
  - `POST /register` — validates form, checks password match, checks email uniqueness, creates user with `Role.USER`
  - Encodes password via `PasswordEncoder.encode()` before persisting
  - Redirects to `/login?registered` on success

- `controller/AdminController.java` - Admin panel
  - `@Controller` with `/admin` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `GET /admin/users` — lists all users with role dropdown
  - `POST /admin/users/{id}/role` — changes a user's role via `UserService.updateRole()`

- `controller/TaskController.java` - Task web UI endpoints
  - `@Controller` with `/tasks` base path
  - Returns Thymeleaf template names or fragment selectors
  - HTMX support: detects `HX-Request` header via `HtmxUtils.isHtmxRequest()`
  - `Object` return type on POST methods to allow returning either a String view name or `ResponseEntity`
  - Fires `HX-Trigger` events (`taskSaved`, `taskDeleted`) via `HtmxUtils.triggerEvent()`
  - Injects `UserService` and `TagService`; adds `users` and `tags` lists to all form-serving methods
  - **Security**: uses `OwnershipGuard` for edit/delete; auto-assigns new tasks to current user

- `controller/FrontendConfigController.java` - Serves `/config.js` (JS route config)
  - `@RestController` producing `application/javascript`
  - Reads `AppRoutesProperties` and emits `window.APP_CONFIG = { routes: { tasks, api } };`
  - `escapeJs()` helper sanitizes property values before embedding in JS output
  - Loaded by the `scripts` fragment on every page; `APP_CONFIG` is available globally to all page scripts

#### Security Layer
- `security/CustomUserDetails.java` - Implements Spring Security's `UserDetails`
  - Wraps the `User` entity; exposes it via `getUser()` for controllers and templates
  - `getUsername()` returns `user.getEmail()` (email is the login identifier)
  - `getAuthorities()` returns single authority: `ROLE_USER` or `ROLE_ADMIN`
  - Account status methods all return `true` (no expiry/lock features yet)

- `security/CustomUserDetailsService.java` - Implements Spring Security's `UserDetailsService`
  - `loadUserByUsername(String email)` — looks up user via `UserRepository.findByEmail()`
  - Throws `UsernameNotFoundException` if not found
  - Wraps result in `CustomUserDetails`

- `security/OwnershipGuard.java` - Reusable access control component
  - `requireAccess(OwnedEntity entity, CustomUserDetails currentDetails)` — throws `AccessDeniedException` if caller is neither admin nor owner
  - Admins get unrestricted access; unassigned entities are admin-only
  - Used by both `TaskApiController` and `TaskController` (web)

#### Configuration

- `config/SecurityConfig.java` - Spring Security configuration
  - `PasswordEncoder` bean — `BCryptPasswordEncoder` (default strength)
  - `SecurityFilterChain` bean — HTTP security rules:
    - Public: `/login`, `/register`, static assets, `/h2-console/**`
    - Admin-only: `/admin/**`, `POST /api/tags`, `DELETE /api/tags/**`, `POST /api/users`, `DELETE /api/users/**`
    - Everything else: `authenticated()`
  - Form login: custom login page at `/login`, success → `/`, failure → `/login?error`
  - Logout: `POST /logout` → `/login?logout`, invalidates session, deletes JSESSIONID
  - CSRF: enabled for web forms (Thymeleaf auto-injects); disabled for `/api/**` and `/h2-console/**`
  - Headers: `X-Frame-Options: SAMEORIGIN` (for H2 console)

- `config/AppRoutesProperties.java` - `@ConfigurationProperties(prefix = "app.routes")`
  - Fields: `tasks` (default `/tasks`), `api` (default `/api`)
  - Single source of truth for the two base paths used by both Thymeleaf templates and frontend JS

- `config/GlobalModelAttributes.java` - `@ControllerAdvice` that injects shared attributes into every Thymeleaf model
  - `@ModelAttribute("appRoutes")` — exposes the `AppRoutesProperties` bean as `${appRoutes}` in all templates
  - `@ModelAttribute("currentUser")` — resolves authenticated `User` from `SecurityContextHolder`; null for anonymous
  - Used by HTMX attributes (`th:attr="hx-get=${appRoutes.tasks + ...}"`) where `@{}` URL syntax cannot be used

#### Exception Handling
- `exception/EntityNotFoundException.java` - Custom unchecked exception for missing entities

- `exception/ApiExceptionHandler.java` - `@RestControllerAdvice` scoped to `controller.api`
  - Ordered at `HIGHEST_PRECEDENCE` to win over `WebExceptionHandler`
  - Handles: `MethodArgumentNotValidException` (400), `EntityNotFoundException` (404), `AccessDeniedException` (403), catch-all `Exception` (500)
  - All responses are JSON maps with `timestamp`, `status`, `error` fields

- `exception/WebExceptionHandler.java` - `@ControllerAdvice` for Thymeleaf web controllers
  - Handles: `EntityNotFoundException` → `error/404.html`, catch-all → `error/500.html`
  - **403 not handled here** — Spring Security's `ExceptionTranslationFilter` intercepts `AccessDeniedException` before `@ControllerAdvice` runs; `BasicErrorController` resolves `templates/error/403.html` automatically

#### Utilities
- `util/HtmxUtils.java` - HTMX helper methods
  - `isHtmxRequest(HttpServletRequest)` - checks for `HX-Request: true` header
  - `triggerEvent(String eventName)` - returns `ResponseEntity` with `HX-Trigger` header set

- `util/AuthExpressions.java` - Ownership and role check logic (shared between templates and Java)
  - Exposed as `${#auth}` in Thymeleaf templates via `AuthDialect`
  - Instance methods (template use): `isOwner(OwnedEntity)`, `isAdmin()`, `canEdit(OwnedEntity)` (admin OR owner)
  - Static methods (Java use): `isOwner(User, OwnedEntity)`, `isAdmin(User)` — reused by `OwnershipGuard`
  - Unassigned entities (`entity.getUser() == null`): `isOwner()` returns false → only admins can edit

- `util/AuthDialect.java` - Thymeleaf `IExpressionObjectDialect` implementation
  - Registers `${#auth}` expression object, built per-request from `SecurityContextHolder`
  - Auto-discovered by Spring Boot; no manual configuration needed

#### Bootstrap
- `DataLoader.java` - Seeds database on startup: **50 users**, **8 tags**, **300 tasks**
  - First user (Alice Johnson) gets `Role.ADMIN`; all others get `Role.USER`
  - All passwords: `"password"` (BCrypt-encoded once, reused for all 50 users for speed)
  - Dev credentials: `alice.johnson@example.com` / `password` (admin), `bob.smith@example.com` / `password` (regular)
  - Tags use orthogonal dimensions: domain (Work/Personal/Home), priority (Urgent/Someday), type (Meeting/Research/Errand)
  - Each task gets 1–2 tags drawn from different dimensions for natural combos (e.g. "Work + Urgent")
  - ~80% of tasks are assigned to a user (every 5th task is unassigned)

### Thymeleaf Templates

#### Layouts
- `templates/layouts/base.html` - Base layout with reusable fragments
  - `head(title, cssFile)` - two-parameter head fragment; `cssFile` is nullable for pages without page-specific CSS
  - `navbar` - navigation bar with auth-aware elements:
    - Anonymous: shows Register link
    - Authenticated: user dropdown with name, email, role badge, logout button
    - Admin: additional "Manage Users" link in dropdown
    - Uses `sec:authorize` (Spring Security Thymeleaf dialect) and `${#auth}` for conditional rendering
  - `footer` - footer
  - `scripts` - Bootstrap + HTMX + `/config.js` + `utils.js` (in that order — `APP_CONFIG` must be set before page scripts run)

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
  - `fields` fragment — title, description, user `<select>` (one value, @ManyToOne), tag checkboxes (multiple, @ManyToMany), completed toggle (edit only)
  - The user/tag widgets side-by-side illustrate the difference: `<select>` for single FK vs checkboxes for join table
  - No `<form>` tag; `th:object` is set by the including template
  - Used by both `task.html` and `task-modal.html`

- `templates/tasks/task.html` - Full-page create/edit form
  - Standalone page with full layout (navbar, footer)
  - Includes `task-form :: fields` inside its own `<form th:object="${task}">`

- `templates/tasks/task-modal.html` - HTMX modal content (bare file, no HTML wrapper)
  - Returned by controller as `"tasks/task-modal"` (whole file is the response)
  - Includes `task-form :: fields` inside its own `<form>` with `hx-post`
  - Swapped into `#task-modal-content` by HTMX

#### Auth Views
- `templates/login.html` - Login page
  - Card-styled form with email and password fields
  - `th:action="@{/login}"` — Spring Security handles POST automatically and injects CSRF token
  - Alert messages via query params: `?error` (bad credentials), `?logout` (logged out), `?registered` (just registered)
  - Dev credentials hint (removable for production)

- `templates/register.html` - Registration page
  - Form bound to `RegistrationRequest` via `th:object`
  - Fields: name, email, password, confirmPassword with per-field validation errors
  - Success redirects to `/login?registered`

- `templates/error/403.html` - Access Denied page
  - Resolved automatically by Spring Boot's `BasicErrorController` (not via `@ControllerAdvice`)
  - Shows "Access Denied" message with link back to task list

- `templates/admin/users.html` - Admin user management page
  - Table of all users: name, email, role badge, role-change dropdown
  - Form submits `POST /admin/users/{id}/role` with selected role
  - Role badges: green for ADMIN, gray for USER

### Static Resources

- `static/css/base.css` - Global styles (body, card hover, btn transitions, validation, navbar, footer, HTMX indicator)
- `static/css/tasks.css` - Task page styles (filter button active states, table action button overrides, search clear button overlay)
- `static/js/utils.js` - Shared browser utilities (`getCookie`, `setCookie`); CSRF token injection for HTMX requests via `htmx:configRequest` listener; loaded globally via base layout
- `static/js/tasks.js` - Task list page logic (sort, filter, search, pagination, modal wiring, search clear button); loaded only by `tasks.html`; reads `APP_CONFIG.routes.tasks` for URL construction
- `static/bootstrap-icons/` - Bootstrap Icons (locally hosted)

### Resource Files

- `resources/messages.properties` - UI display strings (field labels, button text, page titles, status badges, etc.)
  - Spring Boot auto-discovers this file; no extra configuration needed
  - Reference with `#{key}` in Thymeleaf templates
  - Namespace conventions (see Message Source Pattern section for full detail):
    - `action.*` — generic actions reusable across features (`action.cancel`, `action.delete`, ...)
    - `pagination.*` — pagination controls, reusable for any paginated list
    - `nav.*`, `footer.*`, `page.title.*` — layout and browser title strings
    - `task.*` — everything specific to the Task feature (`task.field.*`, `task.sort.*`, `task.filter.*`, `task.table.column.*`, `task.view.*`, `task.search.*`, ...)
    - `tag.*` — Tag feature strings (`tag.field.*`)
    - `user.*` — User feature strings (`user.field.*`)
    - `login.*` — Login page strings (`login.heading`, `login.field.*`, `login.error`, `login.loggedOut`)
    - `register.*` — Registration page strings (`register.heading`, `register.field.*`, `register.error.*`, `register.success`)
    - `admin.*` — Admin panel strings (`admin.users.heading`, `admin.users.table.*`, `admin.users.role.change`)
    - `role.*` — Role display names (`role.user`, `role.admin`)
    - `error.*` — Error page strings (`error.403.heading`, `error.403.message`)

- `resources/META-INF/additional-spring-configuration-metadata.json` - IDE metadata for custom properties
  - Provides descriptions and types for `app.routes.tasks` and `app.routes.api`
  - Enables autocomplete and documentation in IntelliJ / VS Code for `application.properties`
  - No runtime effect — purely a developer-experience file

- `resources/ValidationMessages.properties` - Bean Validation error messages
  - Used by Hibernate Validator for `@NotBlank`, `@Size`, `@NotNull`, etc.
  - Reference with `{key}` syntax (curly braces) inside constraint annotation `message` attribute
  - `{min}`, `{max}`, `{value}` placeholders are interpolated from the constraint's own attributes
  - Referenced by `Task.java`, `TaskRequest.java`, `User.java`, and `RegistrationRequest.java`

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

### Message Source Pattern

All user-facing UI strings are externalized to `messages.properties`. Reference them in Thymeleaf with `#{key}`:

```html
<label th:text="#{task.field.title}">Title</label>
<span th:text="#{task.status.completed}">Completed</span>
```

The static fallback text inside the tag (e.g., `Title`) is shown in IDE preview but replaced at runtime.

**Parameterized messages** use `{0}`, `{1}`, ... placeholders (Java `MessageFormat`):

```properties
# messages.properties
pagination.showing=Showing {0}–{1} of {2} tasks
pagination.perPage={0} / page
```

```html
<small th:text="#{pagination.showing(${start}, ${end}, ${total})}">Showing 1–25 of 300</small>
<option th:text="#{pagination.perPage(10)}">10 / page</option>
```

**Conditional with message keys** — when a ternary picks between two `#{...}` expressions, use the *outer* conditional form (not inside `${}`):

```html
<!-- Correct: #{...} expressions go outside ${} in a conditional -->
<span th:text="${isEdit} ? #{task.edit.heading} : #{action.newTask}">Task</span>

<!-- Only use the inner form for string literals inside SpEL -->
<div th:classappend="${task.completed ? 'border-success' : 'border-warning'}">
```

**Validation error messages** reference `ValidationMessages.properties` with `{key}` (curly braces, not `${key}`) in constraint annotations. Hibernate Validator resolves `{min}` / `{max}` from the annotation's own attributes:

```java
@NotBlank(message = "{task.title.notBlank}")
@Size(min = 1, max = 100, message = "{task.title.size}")
```

```properties
# ValidationMessages.properties
task.title.notBlank=Title is required
task.title.size=Title must be between {min} and {max} characters
```

### Thymeleaf Ternary Operator Syntax

**Correct:**
```html
th:classappend="${task.completed ? 'border-success' : 'border-warning'}"
```

**Incorrect (will fail to parse):**
```html
th:classappend="${task.completed} ? 'border-success' : 'border-warning'"
```

The ternary operator `? :` must be **inside** the `${}` expression when both branches are string literals. For message key branches, use the outer conditional form shown in the Message Source Pattern section above.

### `th:object` Propagation to Fragments

`th:object` set on a `<form>` propagates into included fragments, so `*{field}` expressions work inside `task-form :: fields` even though the `<form>` tag is in the parent template. This enables the shared fields fragment pattern.

### HTMX Fragment Pattern

Controller detects HTMX request and returns a fragment:
```java
if (HtmxUtils.isHtmxRequest(request)) {
    model.addAttribute("task", task);
    return "tasks/task-card :: card";
}
return "redirect:/tasks";
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
<button hx-get="/tasks/new" hx-target="#task-modal-content" hx-swap="innerHTML">
```
`htmx:afterSwap` fires Bootstrap `modal.show()` when `#task-modal-content` is populated.

**Delete confirmation modal** — content populated via JS:
```javascript
document.getElementById('task-delete-modal').addEventListener('show.bs.modal', function(e) {
    const btn = e.relatedTarget; // the triggering delete button
    document.getElementById('task-delete-modal-title').textContent = btn.dataset.taskTitle;
    const confirmBtn = document.getElementById('delete-confirm-btn');
    confirmBtn.setAttribute('hx-post', '/tasks/' + btn.dataset.taskId + '/delete');
    htmx.process(confirmBtn); // re-process after dynamic hx-post assignment
});
```
Delete buttons carry `data-task-id` and `data-task-title` attributes.

### Frontend Route Configuration Pattern

Route base paths are defined once in `application.properties` and flow to two consumers:

**Thymeleaf templates** — via `GlobalModelAttributes` which exposes `appRoutes` as a model attribute on every request:
```html
<!-- Use @{} for th:href and th:action — it's context-path-aware -->
<a th:href="@{/tasks}">Tasks</a>
<a th:href="@{/tasks/{id}/edit(id=${task.id})}">Edit</a>
<form th:action="${isEdit} ? @{/tasks/{id}(id=${task.id})} : @{/tasks}">

<!-- Use ${appRoutes.tasks + ...} only for HTMX th:attr — @{} doesn't work there -->
<button th:attr="hx-get=${appRoutes.tasks + '/new'}">New</button>
<button th:attr="hx-post=${appRoutes.tasks + '/' + task.id + '/toggle'}">Toggle</button>
```

**JavaScript** — via `FrontendConfigController` which serves `/config.js`, loaded before all page scripts:
```js
// In the browser after /config.js loads:
window.APP_CONFIG = { routes: { tasks: '/tasks', api: '/api' } };

// Page scripts read it as a plain global:
const TASKS_BASE = APP_CONFIG.routes.tasks;
htmx.ajax('GET', `${TASKS_BASE}?${params}`, ...);
```

**Key rule:** use `@{}` for `th:href` / `th:action` — it adds the server's context path automatically. Use `${appRoutes.tasks + ...}` only where `@{}` cannot be used (HTMX `th:attr` values). Never use `${appRoutes.tasks}` inside a `th:href` — it bypasses context-path handling.

### Constructor Injection Pattern

Always use constructor injection:
```java
private final TaskService taskService;

public TaskWebController(TaskService taskService) {
    this.taskService = taskService;
}
```
Not `@Autowired` field injection.

### Security Authorization Patterns

Security is enforced at **three layers** — any one can block access:

**1. URL-level (SecurityConfig)** — coarse-grained, role-based:
```java
.requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
.requestMatchers(HttpMethod.POST, "/api/tags").hasRole(Role.ADMIN.name())
.anyRequest().authenticated()
```
Use for endpoints where the entire operation is role-restricted (admin panel, tag/user mutations).

**2. Controller-level (OwnershipGuard)** — fine-grained, ownership-based:
```java
Task task = taskService.getTaskById(id);
ownershipGuard.requireAccess(task, currentDetails); // throws AccessDeniedException
```
Use for endpoints where access depends on who owns the entity (task edit/delete).

**3. Template-level (AuthExpressions)** — UI visibility only (not a security boundary):
```html
<button th:if="${#auth.canEdit(task)}">Edit</button>
<li sec:authorize="hasRole('ADMIN')">Admin Panel</li>
```
`${#auth}` is the custom expression object; `sec:authorize` is Spring Security's Thymeleaf dialect.

### OwnedEntity Pattern

Entities that have an owner implement `OwnedEntity`:
```java
public interface OwnedEntity {
    User getUser(); // null = unassigned (admin-only access)
}
```
`Task implements OwnedEntity` — enables generic ownership checks via `OwnershipGuard.requireAccess()` and `AuthExpressions.canEdit()`. Future owned entities just implement the interface.

### CSRF Token Pattern for HTMX

Thymeleaf auto-injects CSRF into `<form th:action>` tags. For standalone HTMX buttons (not inside a form), `utils.js` reads `<meta>` tags and adds the token header:
```javascript
document.body.addEventListener('htmx:configRequest', function(e) {
    e.detail.headers['X-CSRF-TOKEN'] = document.querySelector('meta[name="_csrf"]').content;
});
```
The `<meta>` tags are set in `base.html`'s `<head>`. REST API (`/api/**`) is CSRF-exempt.

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

<!-- tasks.html passes a message key for the title -->
<head th:replace="~{layouts/base :: head(#{page.title.tasks}, '/css/tasks.css')}"></head>

<!-- task.html: conditional title using message keys -->
<head th:replace="~{layouts/base :: head(${isEdit} ? #{page.title.task.edit} : #{page.title.task.create}, '/css/tasks.css')}"></head>

<!-- Future non-task pages pass null for no page-specific CSS -->
<head th:replace="~{layouts/base :: head(#{page.title.some.page}, null)}"></head>
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

`app.routes.*` properties are **not** listed here — their defaults live in `AppRoutesProperties.java` (the single source of truth). Only add them to `application.properties` when overriding the defaults.

### Maven Dependencies (pom.xml)

Key dependencies:
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-jpa` - JPA/Hibernate
- `spring-boot-starter-thymeleaf` - Template engine
- `spring-boot-starter-security` - Authentication and authorization
- `thymeleaf-extras-springsecurity7` - `sec:authorize` attributes in Thymeleaf templates
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-actuator` - Health and metrics endpoints
- `spring-boot-devtools` - Hot reload during development
- `bootstrap` (WebJar 5.3.3) - CSS framework
- `htmx.org` (WebJar 2.0.4) - AJAX library
- `h2` (2.4.240) - In-memory database
- `lombok` - Boilerplate reduction (not used on entities)
- `mapstruct` (1.6.3) - Compile-time DTO mapping code generation
  - `mapstruct-processor` in `annotationProcessorPaths` (after Lombok — order matters)

## Development Workflow

### Running the Application

```bash
./mvnw spring-boot:run
```
Application runs on: `http://localhost:8080`

### Development Workflow with MapStruct

MapStruct generates `TaskMapperImpl` at **compile time** via the annotation processor.
DevTools hot-reload handles most code changes automatically, but not annotation processor output.

| Change type | Action needed |
|---|---|
| Controller, service, template, JS, CSS | Nothing — DevTools hot-reloads automatically |
| `TaskMapper.java` (the interface) | Run `./mvnw compile` in a second terminal |
| Added a new dependency to `pom.xml` | Restart the app (`Ctrl+C`, then `./mvnw spring-boot:run`) |

**Two-terminal dev setup (recommended when actively editing mappers):**
```bash
# Terminal 1 — keep running
./mvnw spring-boot:run

# Terminal 2 — run after changing TaskMapper.java
./mvnw compile
```
DevTools detects the new `.class` files from `target/` and automatically restarts the context.

### Available URLs

**Authentication:**
- `http://localhost:8080/login` - Login page (email + password)
- `http://localhost:8080/register` - Self-registration (creates `USER` role)
- Dev credentials: `alice.johnson@example.com` / `password` (admin), `bob.smith@example.com` / `password` (regular)

**Web UI** (requires login):
- `http://localhost:8080/` - Home page
- `http://localhost:8080/tasks` - Task list (cards or table view)
- `http://localhost:8080/tasks/new` - Create task (full page; modal preferred)
- `http://localhost:8080/tasks/{id}/edit` - Edit task (full page; modal preferred)
- `http://localhost:8080/admin/users` - User management (admin only)

**REST API — Tasks** (requires login; CSRF exempt):
- `GET /api/tasks` - List all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create task (auto-assigned to caller; admins can specify `userId`)
- `PUT /api/tasks/{id}` - Update task (owner or admin)
- `DELETE /api/tasks/{id}` - Delete task (owner or admin)
- `PATCH /api/tasks/{id}/toggle` - Toggle completion (any authenticated user)
- `GET /api/tasks/search?keyword=...` - Search by title/description
- `GET /api/tasks/incomplete` - Get incomplete tasks only

**REST API — Tags** (requires login):
- `GET /api/tags` - List all tags
- `GET /api/tags/{id}` - Get tag by ID
- `POST /api/tags` - Create tag (admin only, 201 Created)
- `DELETE /api/tags/{id}` - Delete tag (admin only, 204 No Content)

**REST API — Users** (requires login):
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user (admin only, 201 Created)
- `DELETE /api/users/{id}` - Delete user (admin only, 204 No Content)

**Dev Tools:**
- `http://localhost:8080/h2-console` - H2 database console
  - JDBC URL: `jdbc:h2:mem:taskdb` / Username: `sa` / Password: (empty)

### Testing with rest.http

The project includes `rest.http` for testing REST API endpoints with VS Code REST Client extension. CSRF is disabled for `/api/**`, so only a valid session cookie is needed. Log in via browser, copy your `JSESSIONID` from DevTools, and paste it in the `@sessionId` variable.

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
CREATE TABLE users (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(72),                       -- BCrypt hash; nullable for API-created users
    role     VARCHAR(255) NOT NULL DEFAULT 'USER' -- 'USER' or 'ADMIN' (@Enumerated STRING)
);

CREATE TABLE tasks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    completed   BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP,
    user_id     BIGINT REFERENCES users(id)   -- nullable FK; @ManyToOne owning side
);

CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Join table for the @ManyToMany between Task and Tag.
-- Task is the owning side (@JoinTable lives on Task); Tag is the inverse side (mappedBy = "tags").
CREATE TABLE task_tags (
    task_id BIGINT NOT NULL REFERENCES tasks(id),
    tag_id  BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (task_id, tag_id)
);
```

### Join Table Naming Convention

`task_tags` follows the pattern **singular owning entity + plural inverse entity**.

- Owning side (`Task`) → singular: `task`
- Inverse side (`Tag`) → plural: `tags`
- Result: `task_tags` — reads naturally as "a task's tags"

This is the most common Spring/JPA community style. Hibernate's auto-generated name (without `@JoinTable(name=...)`) would be `task_tag` (both singular). Rails uses alphabetical + both plural. There is no enforced standard — consistency within the project is what matters.

## Git Workflow

- `main` - Production-ready code
- `feature/*` - New features
- `fix/*` - Bug fixes
- `refactor/*` - Code improvements

Always ask before committing. Never auto-commit.

## Future Enhancement Ideas

- Add due dates and reminders
- Support task priority levels
- Implement dark mode toggle
- Export tasks to CSV/PDF
- Add task comments/notes
- Implement recurring tasks
