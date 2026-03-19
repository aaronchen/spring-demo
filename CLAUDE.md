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
- **Real-Time**: WebSocket + STOMP (via STOMP.js 7.1)

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

Detailed per-file documentation is in [CLAUDE-reference.md](CLAUDE-reference.md). Read it when you need specifics about a particular class, template, or resource file.

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

### Template Comment Convention

Two types of HTML comments serve different purposes:

**Regular comments (`<!-- -->`)** — structural landmarks visible in browser DevTools. Use for short section/component labels that help navigate the DOM:
```html
<!-- Navigation -->
<!-- Card Header -->
<!-- Footer -->
<!-- Empty State -->
```

**Parser comments (`<!--/* */-->`)** — developer documentation stripped by Thymeleaf before rendering. Use for implementation notes, usage instructions, and explanations:
```html
<!--/* Shared fragment. Reads ${task} from model context.
     Two usage modes: ... */-->
<!--/* th:action causes Thymeleaf to inject the _csrf hidden input automatically */-->
```

**Rule of thumb:** if it's a 1–4 word label for a section, keep it regular so it shows in DevTools. If it explains *why* or *how*, make it a parser comment so it doesn't bloat the response.

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
pagination.showing=Showing {0}–{1} of {2} {3}
pagination.perPage={0} / page
```

```html
<small th:text="#{pagination.showing(${start}, ${end}, ${total}, #{task.pagination.label})}">Showing 1–25 of 300 tasks</small>
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
    showToast(APP_CONFIG.messages['toast.task.saved'], 'success');
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
    confirmBtn.setAttribute('hx-delete', TASKS_BASE + '/' + btn.dataset.taskId);
    htmx.process(confirmBtn); // re-process after dynamic hx-delete assignment
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
window.APP_CONFIG = {
  routes: { tasks: '/tasks', api: '/api', audit: '/admin/audit' },
  messages: { "toast.task.saved": "Task saved successfully.", ... }  // all messages.properties keys
};

// Page scripts read routes and messages as plain globals:
const TASKS_BASE = APP_CONFIG.routes.tasks;
showToast(APP_CONFIG.messages['toast.task.saved'], 'success');
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

### `@Unique` Validation Pattern

Generic class-level annotation for field uniqueness, used on DTOs (not entities):
```java
@Unique(entity = User.class, field = User.FIELD_EMAIL, message = "{user.email.unique}")
public class AdminUserRequest {
    private Long id;  // null on create, set on edit — used to exclude self
    // ...
}
```
- Lives on DTOs only — Spring MVC `@Valid` has full Spring DI for the `EntityManager`
- Entities use `@Column(unique = true)` for DB-level enforcement instead
- `@Repeatable` — supports multiple unique fields on one DTO
- `idField` defaults to `"id"` — validator reads it from the validated object to exclude self on update

### Global String Trimming

`GlobalBindingConfig` (`@ControllerAdvice`) registers `StringTrimmerEditor(true)` — trims all form-bound strings and converts blank to null:
- Applies to `@ModelAttribute`, `@RequestParam`, `@PathVariable`
- Does NOT apply to `@RequestBody` (JSON)
- `@NotBlank` catches null values naturally — no manual trim calls needed

### User Enable/Disable Pattern

Users can be disabled instead of deleted when they have completed tasks or comments:
- `User.enabled` field (default true); disabled users can't log in (`CustomUserDetails.isEnabled()`)
- Disabled users hidden from assignment dropdowns and public user lists (`searchEnabledUsers()`)
- `UserService.canDelete(id)` — true only if no completed tasks and no comments
- Disabling unassigns open/in-progress tasks (resets to OPEN)
- Admin user management shows delete vs disable confirmation based on `canDelete` result

### Site Settings Pattern

Admin-managed settings stored in the `settings` table as key/value rows. The system has three layers:

- **`Setting`** (entity in `model/`) — JPA entity for DB row (`setting_key` / `setting_value`)
- **`Settings`** (typed POJO in `config/`) — single source of truth for all settings with defaults. DB keys must match field names exactly. `BeanWrapper` auto-maps DB rows to fields with type conversion (String → boolean, etc.)
- **`SettingService.load()`** — reads all DB rows into a `Settings` object; missing keys keep field defaults

`GlobalModelAttributes` exposes `${settings}` to all templates. To add a new setting:
1. Add a field with its default in `Settings.java`
2. Add a `KEY_*` constant whose value matches the field name
3. Add `audit.field.<key>` to `messages.properties`

### User Preferences Pattern

Per-user preferences stored in the `user_preferences` table as key/value rows. Mirrors the Site Settings pattern:

- **`UserPreference`** (entity in `model/`) — JPA entity for DB row (`pref_key` / `pref_value`), unique per user+key
- **`UserPreferences`** (typed POJO in `config/`) — defaults for all preferences. `BeanWrapper` auto-maps DB rows to fields
- **`UserPreferenceService.load(userId)`** — reads all rows for a user into a `UserPreferences` object; missing keys keep defaults

`GlobalModelAttributes` exposes `${userPreferences}` to all templates. Current preferences:
- `taskView` — `"cards"` (default), `"table"`, or `"calendar"` — default view mode for task list
- `defaultUserFilter` — `"mine"` (default) or `"all"` — default user filter on task list

### Profile Controller Pattern

Three controllers handle User-related concerns:
- **`UserController`** (`/users`) — public user list, visible to all authenticated users
- **`ProfileController`** (`/profile`) — self-service: edit name/email, change password, manage preferences
- **`UserManagementController`** (`/admin/users`) — admin: create/edit/delete/disable/enable users

`ProfileRequest` DTO uses `@Unique` validation (same as admin user forms) with a hidden `id` field for self-exclusion on update.

### Theme System

Custom color schemes activated via `data-theme` attribute on `<html>`. Without it, stock Bootstrap renders.

- **`Settings.java`** — `THEME_DEFAULT`, `THEME_WORKSHOP`, `THEME_INDIGO` constants; default theme stored as `"default"` (not null)
- **`theme.css`** — palette tokens (`--theme-*`) per theme, mapped to Bootstrap `--bs-*` variables in shared `[data-theme]` rules
- **`SettingsController`** — theme picker UI with color swatch cards; `ThemeOption` record holds preview colors (duplicated from CSS — unavoidable since CSS is client-side); validates theme against known `THEMES` list
- FOUC prevention: `<meta name="_theme">` + inline JS in `<head>` sets `data-theme` before CSS renders
- Adding a new theme: (1) add `THEME_*` constant in `Settings.java`, (2) add `[data-theme="name"]` palette in `theme.css`, (3) add `ThemeOption` in `SettingsController.THEMES`, (4) add `admin.settings.theme.<name>.{name,description}` to `messages.properties`

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
if (task.getUser() != null) {
    ownershipGuard.requireAccess(task, currentDetails); // throws AccessDeniedException
}
```
Use for endpoints where access depends on who owns the entity (task edit/delete). Unassigned-entity handling is a business rule — callers skip the guard when `getUser() == null`.

**3. Template-level** — UI visibility only (not a security boundary):
```html
<!-- canEdit: owner OR admin OR unassigned (task-specific business rule) -->
<div th:with="canEdit=${#auth.canEdit(task) || task.user == null}">
    <button th:if="${canEdit}">Edit</button>
</div>
<li sec:authorize="hasRole('ADMIN')">Admin Panel</li>
```
`${#auth}` is the custom expression object; `sec:authorize` is Spring Security's Thymeleaf dialect.

### OwnedEntity Pattern

Entities that have an owner implement `OwnedEntity`:
```java
public interface OwnedEntity {
    User getUser(); // null = unassigned
}
```
Unassigned-entity rules are business decisions — handled in controllers (skip `requireAccess()` when `getUser() == null`) and templates (`${#auth.canEdit(task) || task.user == null}`), not in the generic auth utilities.
`Task` and `Comment` implement `OwnedEntity` — enables generic ownership checks via `OwnershipGuard.requireAccess()` and `AuthExpressions.canEdit()`. Future owned entities just implement the interface.

### HTMX Out-of-Band Swap Pattern

For updating multiple areas of the page from a single HTMX response (e.g., refreshing comment counts after add/delete), use `hx-swap-oob`:

**Template with dual usage** — fragment selector for page renders, whole-file return for HTMX responses:
```html
<th:block>
    <!-- Primary swap target — selected by :: list -->
    <div th:fragment="list" id="task-activity">
        ...activity timeline...
    </div>
    <!-- OOB spans — only included when whole file is returned (HTMX response) -->
    <span id="task-activity-panel-heading" hx-swap-oob="true"
          th:text="#{activity.heading(${activityCount})}">Activity (0)</span>
</th:block>
```

- `th:replace="~{tasks/task-activity :: list}"` — returns only the fragment (page render)
- Controller returns `"tasks/task-activity"` (no `::`) — returns fragment + OOB spans (HTMX response)

This replaces JS-based count updates with server-driven updates. No client-side counting logic needed.

### Confirm Dialog Pattern

`showConfirm(options, onConfirm)` in `utils.js` — reusable styled Bootstrap modal replacing native `window.confirm()`. The modal is created fresh each call and destroyed on hide (avoids Bootstrap backdrop stacking issues with nested modals).

**Options** (only `message` is required):
```javascript
showConfirm({
    message: 'Do you want to delete this?',  // body text
    title: 'Delete Item',                     // header title
    confirmText: 'Delete',                    // confirm button label
    cancelText: 'Cancel',                     // cancel button label
    headerClass: 'bg-danger text-white',      // header CSS classes
    confirmClass: 'btn btn-danger',           // confirm button CSS classes
    width: '420px',                           // modal width
}, () => { /* on confirm */ });
```

**HTMX integration** — automatically intercepts `htmx:confirm` events. Use `data-confirm-*` attributes for per-element customization:
```html
<button hx-delete="/tasks/1/comments/5"
        hx-confirm="Do you want to delete this comment?"
        data-confirm-title="Delete Comment"
        data-confirm-text="Delete">
```

### WebSocket + STOMP Pattern

Real-time features use Spring WebSocket with STOMP protocol:

- **`WebSocketConfig`** — `/ws` endpoint with SockJS fallback; simple broker on `/topic` (broadcast) and `/queue` (user-specific)
- **`presence/PresenceService`** — `ConcurrentHashMap` tracks online users by WebSocket session
- **`presence/PresenceEventListener`** — listens for `SessionConnectEvent`/`SessionDisconnectEvent`, broadcasts presence changes to `/topic/presence`
- **`NotificationService.create()`** — saves to DB first, then pushes to user via `SimpMessagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload)`
- **`event/WebSocketEventListener`** — broadcasts `TaskChangeEvent` to `/topic/tasks` and `CommentChangeEvent` to `/topic/tasks/{id}/comments`; clients show stale-data banners

**Live task update banner** — when another user modifies a task, viewers see an info banner with a "Refresh" link:
- Task list page: subscribes to `/topic/tasks`, shows banner for any task change, "Refresh" re-runs current filters via `doSearch()`
- Task detail page: subscribes to `/topic/tasks`, filters by specific task ID, "Refresh" reloads page
- Task modal: subscribes on open, unsubscribes on close, "Refresh" re-fetches modal content via HTMX
- Self-filtering: `<meta name="_userId">` exposes current user ID; own changes are ignored

**Client-side connection** (`websocket.js`):
```javascript
// Shared connection — multiple scripts register callbacks before connect
window.stompClient = { onConnect: function(cb) { callbacks.push(cb); } };
// After STOMP connects, each callback receives the client for subscribing
```

**Notification event bus** (`notifications.js`) — custom DOM events decouple producers from consumers:
```javascript
// Events: notification:received, notification:read, notification:allRead, notification:cleared
// Three consumers: badge, dropdown, notifications page — all listen independently
document.dispatchEvent(new CustomEvent('notification:received', { detail: data }));
```

`window.notificationHelpers` exposes `getIcon()`, `formatTime()`, `escapeHtml()`, `fire()` for the notifications page to reuse.

**WebSocket payload convention:** payloads shared by multiple classes (e.g., `PresenceResponse` used by both REST API and WebSocket broadcast) go in `dto/`. Event records that are both Spring events and WebSocket payloads go in `event/` (e.g., `TaskChangeEvent`, `CommentChangeEvent`). Naming: `*Response` for data returned to clients, `*Event` for push-only broadcasts.

### Event-Driven Side Effects Pattern

Services publish domain events via `ApplicationEventPublisher`; three independent listeners handle all side effects:

| Listener | Package | What it does | Persistence |
|---|---|---|---|
| `AuditEventListener` | `audit/` | Persists audit logs | DB |
| `NotificationEventListener` | `event/` | Routes persistent notifications to recipients | DB + WebSocket push |
| `WebSocketEventListener` | `event/` | Broadcasts ephemeral stale-data/comment events | WebSocket only |

**Domain events** (consumed by Java listeners only, `actor` field naming):
- `TaskAssignedEvent(task, actor)` — task assigned to someone new
- `TaskUpdatedEvent(task, actor)` — task fields changed
- `CommentAddedEvent(comment, task, actor)` — comment created

**WebSocket events** (serialized to JSON for JS clients, `userId` field naming):
- `TaskChangeEvent(action, taskId, userId)` — task created/updated/deleted
- `CommentChangeEvent(action, taskId, commentId, userId)` — comment created/deleted

Services only do business logic + `eventPublisher.publishEvent()` — no `SimpMessagingTemplate`, `NotificationService`, or `MessageSource` dependencies. Scheduled jobs (`ScheduledTaskService`) still call `NotificationService` directly since cron-triggered actions don't fit the event pattern.

### Package-by-Concern Organization

Event-related code is organized by domain concern, not by technical role:
- `audit/` — audit events, listeners, service, utilities
- `event/` — domain events, notification listener, WebSocket listener
- `presence/` — presence service and session lifecycle listener

Controllers always stay in `controller/` (layer package) — never move them into feature packages. Feature packages are for internal infrastructure only.

### Cross-Service Dependency Rule

A service uses its own repository for its own domain and delegates to other services for other domains. Never call another domain's repository directly — business logic in the owning service would be bypassed.

`TaskQueryService` and `CommentQueryService` exist to break circular dependencies while respecting this rule:
- `TaskQueryService` — read-only task lookups + `unassignTasks()`; used by `CommentService` and `UserService`
- `CommentQueryService` — read-only comment lookups; used by `UserService`

### Transactional Boundaries

**OSIV is disabled** (`spring.jpa.open-in-view=false`). Lazy associations must be loaded within a transaction — either via `@EntityGraph` on repository methods or within a `@Transactional` service method.

**Class-level `@Transactional`** on services with write methods (`TaskService`, `UserService`, `CommentService`, `TagService`, `SettingService`, `UserPreferenceService`). Method-level `@Transactional` for isolated write methods in otherwise read-only services (e.g., `ScheduledTaskService.purgeOldNotifications()`).

**`@EntityGraph`** on repository methods whose results are used outside the service transaction (e.g., mapped by MapStruct in controllers):
```java
@EntityGraph(attributePaths = {"tags", "user", "checklistItems"})
Optional<Task> findById(Long id);

@EntityGraph(attributePaths = {"tags", "user"})
List<Task> findAll();
```
Only include the associations that callers actually access. `findById` includes `checklistItems` (needed by edit form); list queries include only `tags` and `user` (used by mapper/cards).

**`@TransactionalEventListener`** on all event listeners (replaces `@EventListener`). Fires after the publishing transaction commits. `AuditEventListener` uses `@Transactional(propagation = Propagation.REQUIRES_NEW)` because it writes to its own table in a new transaction.

### SecurityUtils Pattern

Central utility for resolving the current user from `SecurityContextHolder`:
```java
SecurityUtils.getCurrentUser()        // → User entity or null
SecurityUtils.getCurrentUserDetails() // → CustomUserDetails or null
SecurityUtils.getUserFrom(principal)  // → User entity from Principal (for WebSocket events)
```
All other classes (`GlobalModelAttributes`, `AuthDialect`, `PresenceEventListener`, services) delegate here instead of duplicating resolution logic.

### CSRF Token Pattern for HTMX

Thymeleaf auto-injects CSRF into `<form th:action>` tags. For standalone HTMX buttons (not inside a form), `utils.js` reads `<meta>` tags and adds the token header:
```javascript
document.body.addEventListener('htmx:configRequest', function(e) {
    e.detail.headers['X-CSRF-TOKEN'] = document.querySelector('meta[name="_csrf"]').content;
});
```
The `<meta>` tags are set in `base.html`'s `<head>`. REST API (`/api/**`) is CSRF-exempt.

### Audit Event Categories

`AuditEvent.CATEGORIES` is the single source of truth for audit filter categories. Every event constant must be prefixed with one of these categories (e.g., `TASK_CREATED`, `AUTH_SUCCESS`, `PROFILE_UPDATED`).

- **`AuditLogSpecifications.withCategory()`** — derives `LIKE` prefix from `CATEGORIES` list (no hardcoded switch)
- **`AuditController`** — passes `CATEGORIES` to model
- **`audit.html`** — generates filter buttons dynamically via `th:each`

To add a new audit category: add the prefix to `CATEGORIES`, define event constants with that prefix, add `admin.audit.filter.<name>` message key. Filter and UI update automatically.

### CSV Export Pattern

`CsvWriter` utility in `util/` — generic CSV writer that takes headers, a list of any type, and a row mapper lambda:
```java
CsvWriter.write(response, "tasks.csv", headers, tasks, task -> new String[]{...});
```
Handles escaping (commas, quotes, newlines), sets `Content-Type` and `Content-Disposition` headers. Reusable for any entity export.

The task export endpoint (`GET /tasks/export`) accepts the same filter params as the task list, uses `Pageable.unpaged(sort)` to bypass pagination, and delegates to the same `searchAndFilterTasks` service method.

### @Mention Pattern

Comments support @mentions via Tribute.js autocomplete. The system has three layers:

**Client-side (`mentions.js`):**
- Attaches to any element with `[data-mention]` attribute (input or textarea)
- Tribute.js with `positionMenu: false` — CSS-only positioning (above input via `bottom: 100%`)
- Shows clean `@Bob Smith` in the input; tracks name→ID mapping in a `WeakMap`
- Encodes to `@[Bob Smith](userId:2)` before submission via `htmx:configRequest` and form `submit` listeners
- Atomic backspace/delete — `@Bob Smith` is deleted as a single token
- `isMentionMenuActive()` — prevents Enter-to-post while dropdown is open

**Server-side (`MentionUtils`):**
- `@Component("mentionUtils")` for Thymeleaf `@beanName` access
- `extractMentionedUserIds(text)` — static, returns `List<Long>` from encoded tokens
- `renderHtml(text)` — instance method, converts tokens to `<span class="mention">` with HTML escaping
- Pattern: `@[Display Name](userId:N)`

**Notification subscription:**
- @Mentioned users get `COMMENT_MENTIONED` notification on the mentioning comment
- Being mentioned subscribes you to the conversation — all future comments on that task notify you
- `CommentService.findPreviouslyMentionedUserIds()` parses all comment texts for a task

To add @mentions to a new field: add `data-mention` attribute to the input/textarea element.

### Shared Task Layout Pattern

`task-layout.html` contains the two-column layout fragment (`columns`) shared by both `task-modal.html` and `task.html`:
- Left column: form fields (scrollable)
- Right column: checklist (top 50%) + activity timeline (bottom 50%)
- Checklist add button in header (always visible above scroll)
- Activity comment input pinned at bottom via flexbox

The comment input uses a `<div>` instead of `<form>` to avoid invalid nested forms (the outer task `<form>` wraps the layout). HTMX `hx-post` on the button with `hx-include="#comment-text"` replaces the inner form.

### Checklist Pattern

Tasks have an embedded checklist via `@ElementCollection`:
```java
@ElementCollection
@CollectionTable(name = "task_checklist_items", joinColumns = @JoinColumn(name = "task_id"))
@OrderColumn(name = "position")
private List<ChecklistItem> checklistItems;
```

`ChecklistItem` is an `@Embeddable` with `text` and `checked` fields. Form binding uses parallel arrays (`checklistTexts[]` + `checklistChecked[]`). Checklist progress shown on cards/table rows via `@Formula` computed columns.

Checklist items support drag-and-drop reordering via native HTML Drag and Drop API. Each item has a grip handle (`bi-grip-vertical`). Reordering DOM elements naturally updates the parallel arrays on form submission — no extra position tracking needed.

Checklist is audited in `Task.toAuditSnapshot()` using `[x]/[ ]` format for readable diffs.

### Activity Timeline Pattern

`TimelineService` merges comments and audit history into a single chronological stream of `TimelineEntry` records. Each entry has a `type()` ("comment" or "audit") and type-specific accessor methods.

`task-activity.html` renders the timeline with visual timeline dots (colored by action type) and connecting lines. Uses the dual-usage template pattern: `:: list` for page includes, whole-file for HTMX responses with OOB count swaps.

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
    <link th:if="${cssFile != null}" rel="stylesheet" th:href="@{${cssFile}}">
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
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=create-drop

# H2 Console: http://localhost:8080/h2-console
spring.h2.console.enabled=true

# SQL logging (helpful for debugging)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Cache busting: content hash appended to static resource URLs
spring.web.resources.chain.strategy.content.enabled=true
spring.web.resources.chain.strategy.content.paths=/**
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
- `spring-boot-starter-websocket` - WebSocket + STOMP support
- `stomp-websocket` (WebJar 2.3.4) - STOMP.js client library

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
- `http://localhost:8080/tasks` - Task list (cards, table, or calendar view)
- `http://localhost:8080/tasks/new` - Create task (full page; modal preferred)
- `http://localhost:8080/tasks/{id}/edit` - Edit task (full page; modal preferred)
- `http://localhost:8080/tasks/export` - CSV export of filtered tasks (respects current filters/sort)
- `http://localhost:8080/dashboard` - Personal dashboard (real-time stats, recent tasks, activity feed)
- `http://localhost:8080/tags` - Tag list
- `http://localhost:8080/users` - User list with search
- `http://localhost:8080/admin/users` - User management: create/edit/delete/disable/enable (admin only)
- `http://localhost:8080/admin/tags` - Tag management: create/delete with task counts (admin only)
- `http://localhost:8080/admin/audit` - Audit log with search/filters (admin only)
- `http://localhost:8080/admin/settings` - Site settings: theme, site name, registration, maintenance banner (admin only)
- `http://localhost:8080/notifications` - Notification inbox (paginated, mark-read, clear-all)
- `http://localhost:8080/profile` - User profile: edit name/email, change password, preferences

**REST API — Tasks** (requires login; CSRF exempt):
- `GET /api/tasks` - List all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create task (auto-assigned to caller; admins can specify `userId`)
- `PUT /api/tasks/{id}` - Update task (owner or admin; requires `version` for optimistic locking)
- `DELETE /api/tasks/{id}` - Delete task (owner or admin)
- `PATCH /api/tasks/{id}/toggle` - Toggle completion (any authenticated user)
- `GET /api/tasks/search?keyword=...` - Search by title/description
- `GET /api/tasks/incomplete` - Get incomplete tasks only

**REST API — Comments** (requires login; CSRF exempt):
- `GET /api/tasks/{taskId}/comments` - List comments for a task
- `POST /api/tasks/{taskId}/comments` - Add comment (201 Created; body: `{"text": "..."}`)
- `DELETE /api/tasks/{taskId}/comments/{id}` - Delete comment (owner or admin, 204 No Content)

**REST API — Tags** (requires login):
- `GET /api/tags` - List all tags
- `GET /api/tags/{id}` - Get tag by ID
- `POST /api/tags` - Create tag (admin only, 201 Created)
- `DELETE /api/tags/{id}` - Delete tag (admin only, 204 No Content)

**REST API — Users** (requires login):
- `GET /api/users` - List all users (sorted A-Z)
- `GET /api/users?q=ali` - Search users by name (case-insensitive substring)
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user (admin only, 201 Created)
- `DELETE /api/users/{id}` - Delete user (admin only, 204 No Content)

**REST API — Notifications** (requires login; CSRF exempt):
- `GET /api/notifications` - Paginated list (default: page=0, size=10)
- `GET /api/notifications/unread-count` - Unread badge count (`{"count": n}`)
- `PATCH /api/notifications/{id}/read` - Mark one as read (204 No Content)
- `PATCH /api/notifications/read-all` - Mark all as read (204 No Content)
- `DELETE /api/notifications` - Clear all notifications (204 No Content)

**REST API — Presence** (no authentication required):
- `GET /api/presence` - Online users (`{"users": [...], "count": n}`)

**WebSocket** (STOMP over SockJS):
- Endpoint: `ws://localhost:8080/ws`
- Subscribe: `/user/queue/notifications` — real-time notification push
- Subscribe: `/topic/presence` — online user list broadcast

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
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(72),                       -- BCrypt hash; nullable for API-created users
    role     VARCHAR(255) NOT NULL DEFAULT 'USER', -- 'USER' or 'ADMIN' (@Enumerated STRING)
    enabled  BOOLEAN NOT NULL DEFAULT TRUE      -- disabled users cannot log in
);

CREATE TABLE tasks (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    version      BIGINT,                         -- @Version optimistic locking
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    status       VARCHAR(255) DEFAULT 'OPEN',   -- OPEN / IN_PROGRESS / COMPLETED (@Enumerated STRING)
    priority     VARCHAR(255) DEFAULT 'MEDIUM',  -- LOW / MEDIUM / HIGH (@Enumerated STRING)
    start_date   DATE,                           -- nullable; when work begins
    due_date     DATE,                           -- nullable; overdue = incomplete + past due
    completed_at TIMESTAMP,                      -- set automatically when status → COMPLETED
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    user_id      BIGINT REFERENCES users(id)   -- nullable FK; @ManyToOne owning side
);

CREATE TABLE task_checklist_items (
    task_id  BIGINT NOT NULL REFERENCES tasks(id),
    text     VARCHAR(255) NOT NULL,
    checked  BOOLEAN NOT NULL DEFAULT FALSE,
    position INT NOT NULL                       -- @OrderColumn preserves list order
);

CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    action      VARCHAR(255) NOT NULL,          -- e.g. TASK_CREATED, LOGIN_SUCCESS
    entity_type VARCHAR(255),                   -- e.g. Task, User, Tag
    entity_id   BIGINT,
    principal   VARCHAR(255),                   -- username who performed the action
    details     TEXT,                            -- JSON snapshot or diff
    timestamp   TIMESTAMP
);

CREATE TABLE settings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL UNIQUE,    -- e.g. 'theme', 'siteName'
    setting_value VARCHAR(500)                     -- nullable; null = use default
);

CREATE TABLE notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),  -- recipient
    actor_id   BIGINT REFERENCES users(id),           -- who triggered it (nullable for system)
    type       VARCHAR(255) NOT NULL,                 -- TASK_ASSIGNED / COMMENT_ADDED / TASK_OVERDUE / SYSTEM
    message    VARCHAR(500) NOT NULL,
    link       VARCHAR(500),                          -- e.g. /tasks/5
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,        -- 'read' is SQL reserved word
    created_at TIMESTAMP
);

CREATE TABLE comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    text       VARCHAR(500) NOT NULL,
    created_at TIMESTAMP,
    task_id    BIGINT NOT NULL REFERENCES tasks(id),  -- @ManyToOne; cascade delete via service
    user_id    BIGINT NOT NULL REFERENCES users(id)   -- @ManyToOne; comment author
);

CREATE TABLE user_preferences (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    pref_key   VARCHAR(100) NOT NULL,
    pref_value VARCHAR(500),
    UNIQUE (user_id, pref_key)
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

- Add reminders for due dates
- Implement dark mode toggle
- Export tasks to CSV/PDF
- Implement recurring tasks
