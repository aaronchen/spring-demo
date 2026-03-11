# CLAUDE-reference.md - Detailed File Reference

This file contains per-file documentation for every Java class, template, static resource, and configuration file in the project. It is NOT loaded automatically — Claude reads it on demand when it needs detailed information about a specific file.

For architecture, patterns, conventions, and workflow, see [CLAUDE.md](CLAUDE.md).

## Java Source Files

### Model Layer
- `model/Task.java` - Entity class with JPA annotations; implements `OwnedEntity`
  - Fields: id, version, title, description, status, priority, priorityOrder, dueDate, createdAt, tags, user
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_VERSION`, `FIELD_TITLE`, `FIELD_DESCRIPTION`, `FIELD_STATUS`, `FIELD_PRIORITY`, `FIELD_PRIORITY_ORDER`, `FIELD_DUE_DATE`, `FIELD_CREATED_AT`, `FIELD_TAGS`, `FIELD_USER`) — used in mappers, specifications, and `toAuditSnapshot()`
  - `@Version` on `version` field — JPA optimistic locking; Hibernate auto-increments on each update and throws `OptimisticLockException` on stale writes
  - `status` — `@Enumerated(EnumType.STRING)`, `TaskStatus` enum (OPEN, IN_PROGRESS, COMPLETED), defaults to `OPEN`
  - `isCompleted()` — derived convenience method, returns `status == TaskStatus.COMPLETED` (no backing field)
  - `priority` — `@Enumerated(EnumType.STRING)`, defaults to `MEDIUM`
  - `priorityOrder` — `@Formula("CASE priority WHEN 'LOW' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'HIGH' THEN 2 END")` virtual column for correct sort order (STRING enums sort alphabetically without this)
  - `dueDate` — `LocalDate`, `@DateTimeFormat(iso = ISO.DATE)` for HTML5 `<input type="date">` binding
  - `@ManyToMany(fetch = LAZY)` + `@JoinTable(name = "task_tags")` — Task is the owning side
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — Task owns the FK column; user is optional (nullable)
  - Validation: `@NotBlank`, `@Size` constraints
  - Manual getters/setters (no Lombok on entities)

- `model/TaskStatus.java` - Enum for task lifecycle states: `OPEN`, `IN_PROGRESS`, `COMPLETED`
  - Stored as string via `@Enumerated(EnumType.STRING)` on Task
  - Status advances cyclically: OPEN -> IN_PROGRESS -> COMPLETED -> OPEN

- `model/Priority.java` - Enum for task priority levels: `LOW`, `MEDIUM`, `HIGH`
  - Stored as string via `@Enumerated(EnumType.STRING)` on Task

- `model/Comment.java` - Comment entity; implements `OwnedEntity` and `Auditable`
  - Fields: id, text, createdAt, task, user
  - `FIELD_*` constants (`FIELD_TEXT`, `FIELD_TASK`, `FIELD_USER`)
  - `text` — `@NotBlank`, `@Size(max = 500)`
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "task_id")` — owning side to Task
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — owning side to User
  - Manual getters/setters (no Lombok on entities)

- `model/OwnedEntity.java` - Marker interface for entities that have an owner
  - Single method: `User getUser()` — returns owner or null if unassigned
  - Implemented by `Task` and `Comment`; enables generic ownership checks via `AuthExpressions` and `OwnershipGuard`
  - Future entities with ownership can implement this for automatic access control

- `model/Role.java` - Enum with two values: `USER`, `ADMIN`
  - Stored as string in database via `@Enumerated(EnumType.STRING)` on User
  - Defaults to `USER` for new registrations and API-created users

- `model/Tag.java` - Tag entity
  - Fields: id, name (unique, max 50 chars)
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_NAME`)
  - `@ManyToMany(mappedBy = "tags", fetch = LAZY)` — Tag is the inverse side (no @JoinTable here)
  - Manual getters/setters; `equals()`/`hashCode()` use `getId()` (not field access) for Hibernate proxy safety

- `model/User.java` - User entity with authentication fields
  - Fields: id, name (max 100), email (max 150, unique), password (max 72, nullable), role (Role enum, defaults to USER)
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_NAME`, `FIELD_EMAIL`, `FIELD_ROLE`)
  - `password` — BCrypt hash; nullable for API-created users (who cannot log in)
  - `role` — `@Enumerated(EnumType.STRING)`, stored as "USER" or "ADMIN" in the database
  - `@OneToMany(mappedBy = "user", fetch = LAZY)` — inverse side; no cascade (service handles task reassignment on delete)
  - Manual getters/setters; `equals()`/`hashCode()` use `getId()` (not field access) for Hibernate proxy safety

- `model/AuditLog.java` - Audit log entity
  - Fields: id, action (String), entityType (String), entityId (Long), principal (String), details (String/JSON), timestamp (Instant)
  - `FIELD_*` constants (`FIELD_ACTION`, `FIELD_PRINCIPAL`, `FIELD_DETAILS`, `FIELD_TIMESTAMP`)
  - `@Transient detailsMap` — parsed JSON details for template rendering; populated by `AuditLogService`
  - `toAuditSnapshot()` — entities provide snapshot maps for audit diffing

### Audit Package
- `audit/AuditEvent.java` - Event class published via `ApplicationEventPublisher`
  - Constants: `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`, `USER_CREATED`, `USER_DELETED`, `USER_ROLE_CHANGED`, `USER_REGISTERED`, `TAG_CREATED`, `TAG_DELETED`, `COMMENT_CREATED`, `COMMENT_DELETED`, `LOGIN_SUCCESS`, `LOGIN_FAILURE`
  - Fields: action, entityType, entityId, principal, details

- `audit/AuditDetails.java` - Audit detail utilities
  - `toJson(Map)` — serializes snapshot to JSON string
  - `diff(Map before, Map after)` — computes field-level changes as `{ field: { old: ..., new: ... } }`
  - `resolveDisplayNames(Map, MessageSource, Locale)` — maps raw field keys to human-readable names via `audit.field.{key}` message keys; falls back to raw key if no message found

- `audit/AuditListener.java` - `@EventListener` that persists `AuditEvent` → `AuditLog`

### Repository Layer
- `repository/TaskRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Task, Long>` and `JpaSpecificationExecutor<Task>`
  - Active query methods:
    - `findByStatusNot(TaskStatus)` - used by `getIncompleteTasks()` (finds tasks where status is not COMPLETED)
    - `findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String, String)` - used by `searchTasks()`
    - `findByUser(User)` - used by `UserService.deleteUser()` to reassign tasks before deleting a user
  - `@EntityGraph(attributePaths = {"tags", "user"})` on the paginated query — loads both associations in one LEFT JOIN to prevent N+1
  - `JpaSpecificationExecutor` used by `searchAndFilterTasks()` for paginated filtering

- `repository/TaskSpecifications.java` - JPA Specifications for dynamic queries
  - Uses entity `FIELD_*` constants everywhere (e.g. `Task.FIELD_STATUS`, `User.FIELD_ID`, `Tag.FIELD_ID`)
  - `build(keyword, statusFilter, overdue, priority, userId, tagIds)` - builds a combined search + status + overdue + priority + user + tag specification
  - `withStatusFilter(TaskStatusFilter)` — maps filter enum name directly to `TaskStatus` enum (ALL returns all; OPEN/IN_PROGRESS/COMPLETED filter by matching status)
  - `withOverdue(boolean)` — filters to non-completed tasks with past due dates
  - `withPriority(Priority)` — filters by priority level
  - `withUserId(Long)` — filters tasks by assigned user
  - `withTagIds(List<Long>)` — filters tasks having any of the given tags (OR logic, uses INNER JOIN + distinct)

- `model/TaskStatusFilter.java` - Enum for task status filtering (ALL, OPEN, IN_PROGRESS, COMPLETED)
  - `DEFAULT` constant (`"ALL"`) — for use in `@RequestParam` default value annotations
  - Inner `StringConverter` auto-converts URL params to enum values

- `repository/TagRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Tag, Long>`
  - `findByName(String)` — exact name lookup
  - `findAllByOrderByNameAsc()` — sorted tag list for tag page and task form checkboxes

- `repository/UserRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<User, Long>`
  - `findByEmail(String)` — used by `CustomUserDetailsService` for login and `RegistrationController` for duplicate checks
  - `findAllByOrderByNameAsc()` — sorted user list for dropdowns and admin panel
  - `findByNameContainingIgnoreCaseOrderByNameAsc(String)` — server-side user search for remote searchable-select
  - `findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(String, String)` — user page search (name or email)

- `repository/CommentRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Comment, Long>`
  - `findByTaskIdOrderByCreatedAtAsc(Long)` — chronological comment list; `@EntityGraph(attributePaths = {"user"})` to prevent N+1 on user names
  - `deleteByTaskId(Long)` — `@Modifying` `@Transactional` bulk delete; called by `CommentService.deleteByTaskId()` which is called by `TaskService.deleteTask()` before removing the task

- `repository/AuditLogRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<AuditLog, Long>` and `JpaSpecificationExecutor<AuditLog>`
  - `findByEntityTypeAndEntityIdOrderByTimestampDesc(String, Long)` — entity-specific audit history (used by task detail page)

- `repository/AuditLogSpecifications.java` - JPA Specifications for dynamic audit queries
  - `withCategory(String)` — maps category to action prefix LIKE pattern (`"AUTH"` → `LOGIN_%`, `"COMMENT"` → `COMMENT_%`)
  - `withSearch(String)` — case-insensitive LIKE on principal and details
  - `withFrom(Instant)` / `withTo(Instant)` — timestamp range
  - `build(category, search, from, to)` — combines all specs

### DTO Layer
- `dto/TaskRequest.java` - API input DTO (create and update operations)
  - Fields: `title` (required, 1–100 chars), `description` (optional, max 500 chars), `priority` (optional `Priority`, defaults to MEDIUM), `dueDate` (optional `LocalDate`), `tagIds` (optional `List<Long>`), `userId` (optional `Long`), `version` (null on create, required on update for optimistic locking)
  - Validation annotations used by `@Valid` in the controller
  - Lombok `@Data` for getters/setters/equals/hashCode

- `dto/TaskResponse.java` - API output DTO (returned by all read/write endpoints)
  - Fields: `id`, `title`, `description`, `status` (`TaskStatus`), `priority` (`Priority`), `dueDate` (`LocalDate`), `createdAt`, `tags` (`List<TagResponse>`), `user` (`UserResponse`, nullable), `version`
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

- `dto/CommentResponse.java` - Comment output DTO
  - Fields: `id`, `text`, `taskId`, `user` (`UserResponse`), `createdAt`
  - Lombok `@Data`

- `dto/AdminUserRequest.java` - Admin user creation form DTO
  - Fields: `name` (required, max 100), `email` (required, max 150, @Email), `password` (required, 8–72 chars), `role` (required, defaults to USER)
  - Duplicate email check handled in `UserManagementController`
  - Lombok `@Data`

### Mapper Layer
- `mapper/TaskMapper.java` - MapStruct mapper interface
  - `@Mapper(componentModel = "spring", uses = {TagMapper.class, UserMapper.class})` — auto-discovers nested converters
  - `toResponse(Task)` — MapStruct auto-calls `TagMapper` and `UserMapper` for relationship fields
  - `toResponseList(List<Task>)` — generated automatically
  - `toEntity(TaskRequest)` — `id`, `status`, `createdAt`, `tags`, `user`, `version` explicitly ignored via `Task.FIELD_*` constants (service resolves relationships)
  - Implementation `TaskMapperImpl` generated into `target/generated-sources/` at compile time

- `mapper/TagMapper.java` - MapStruct mapper for Tag ↔ TagResponse

- `mapper/CommentMapper.java` - MapStruct mapper for Comment ↔ CommentResponse
  - `@Mapping(source = "task.id", target = "taskId")` — flattens task association to ID
  - `uses = {UserMapper.class}` — delegates nested user mapping

- `mapper/UserMapper.java` - MapStruct mapper for User ↔ UserResponse / UserRequest

### Service Layer
- `service/CommentService.java` - Comment business logic with audit event publishing
  - Uses `TaskRepository` directly for task lookups (not `TaskService`) to avoid circular dependency
  - `createComment(text, taskId, userId)` — creates comment, publishes `COMMENT_CREATED` audit event
  - `getCommentById(id)` — single comment lookup
  - `getCommentsByTaskId(taskId)` — chronological comment list for a task
  - `deleteByTaskId(taskId)` — bulk deletes all comments for a task; called by `TaskService.deleteTask()`
  - `deleteComment(id)` — deletes comment, publishes `COMMENT_DELETED` audit event

- `service/TaskService.java` - Business logic layer
  - Constructor injection: `TaskRepository`, `TagService`, `UserService`, `CommentService`, `ApplicationEventPublisher` (uses service layer instead of direct repository access for tags, users, and comments)
  - Active methods: `getAllTasks`, `getTaskById`, `createTask(task, tagIds, userId)`, `updateTask(id, task, tagIds, userId, version)`, `deleteTask`, `getIncompleteTasks`, `searchTasks`, `searchAndFilterTasks(keyword, statusFilter, overdue, priority, userId, tagIds, pageable)`, `advanceStatus`
  - `advanceStatus(id)` — cycles OPEN -> IN_PROGRESS -> COMPLETED -> OPEN (replaces `toggleComplete`)
  - `updateTask` — reassigning an IN_PROGRESS task to a different user resets status to OPEN (new assignee hasn't started)
  - `deleteTask` — calls `commentService.deleteByTaskId()` before deleting the task to avoid FK constraint failure

- `service/UserService.java` - User business logic
  - `getAllUsers`, `getUserById`, `findUserById`, `findByEmail`, `searchUsers`, `createUser`, `updateRole`, `deleteUser`
  - `findUserById(Long id)` — returns null if id is null or not found (vs `getUserById` which throws `EntityNotFoundException`); used by `TaskService` for user resolution
  - `searchUsers(String query)` — returns all users if query is blank, otherwise searches by name or email (case-insensitive substring); used by `GET /api/users?q=` and `UserController`
  - `findByEmail(String)` — returns `Optional<User>`; used by registration duplicate check and `CustomUserDetailsService`
  - `updateRole(Long userId, Role role)` — loads user, sets role, saves; called by `UserManagementController`
  - `deleteUser` first reassigns all that user's tasks to null (via `taskRepository.findByUser`), then deletes — prevents FK constraint failure

- `service/TagService.java` - Tag business logic with audit event publishing
  - `getAllTags`, `getTagById`, `findAllByIds(List<Long>)`, `createTag`, `deleteTag`
  - `findAllByIds(ids)` — returns tags matching the given IDs; returns empty list for null/empty input; used by `TaskService` for tag resolution

- `service/AuditLogService.java` - Audit log business logic
  - `searchAuditLogs(category, search, from, to, pageable)` — paginated search with JPA Specifications
  - `getEntityHistory(entityType, entityId)` — entity-specific audit trail (used by task detail/modal)
  - Both methods resolve field display names via `AuditDetails.resolveDisplayNames()` before returning

### Controller Layer
- `controller/api/TaskApiController.java` - Task REST API endpoints
  - `@RestController` with `/api/tasks` base path
  - Standard HTTP methods: GET, POST, PUT, PATCH, DELETE
  - Accepts `TaskRequest` (includes `tagIds`, `userId`), returns `TaskResponse` — no raw entity exposure
  - Injects `TaskMapper` for all DTO ↔ entity conversion
  - **Security**: injects `OwnershipGuard`; uses `@AuthenticationPrincipal CustomUserDetails` on POST, PUT, DELETE
  - POST: auto-assigns task to caller; admins can override via `request.getUserId()`
  - PUT/DELETE: calls `ownershipGuard.requireAccess()` — owner or admin only
  - PATCH advance status: open to all authenticated users (matches web UI behavior)

- `controller/api/UserApiController.java` - User REST API endpoints
  - `@RestController` with `/api/users` base path
  - `GET /api/users` — list all; `GET /api/users?q=ali` — search by name; `GET /api/users/{id}` — get by id; `POST /api/users` (201) — create; `DELETE /api/users/{id}` (204) — delete
  - **Security**: POST and DELETE restricted to admins via `SecurityConfig` URL matchers (no code changes needed here)

- `controller/api/CommentApiController.java` - Comment REST API endpoints
  - `@RestController` with `/api/tasks/{taskId}/comments` base path
  - `GET /api/tasks/{taskId}/comments` — list comments for a task
  - `POST /api/tasks/{taskId}/comments` (201) — create comment; auto-assigned to caller
  - `DELETE /api/tasks/{taskId}/comments/{commentId}` (204) — delete comment; owner or admin only via `OwnershipGuard`
  - Uses `CommentMapper` for DTO conversion

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

- `controller/admin/UserManagementController.java` - Admin user management
  - `@Controller` with `/admin` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `GET /admin/users` — lists all users with role dropdown and collapsible create user form
  - `POST /admin/users` — creates a new user; validates `AdminUserRequest`, checks duplicate email, encodes password; flash attribute `userCreated` triggers toast on redirect
  - `POST /admin/users/{id}/role` — changes a user's role via `UserService.updateRole()`

- `controller/admin/AuditController.java` - Audit log page
  - `@Controller` with `/admin/audit` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `GET /admin/audit` — paginated audit log with category, search, and date range filters
  - Params: `category` (Task/User/Tag/Auth), `search` (principal/details text), `from`/`to` (LocalDate → Instant)
  - HTMX requests → `"admin/audit-table"` (bare fragment); full requests → `"admin/audit"`

- `controller/TagController.java` - Tag web UI
  - `@Controller` with `/tags` base path
  - `GET /tags` — lists all tags sorted A-Z in a table; tag names link to `/tasks?tags={id}&userId=` (all users)

- `controller/UserController.java` - User web UI
  - `@Controller` with `/users` base path
  - `GET /users` — lists all users sorted A-Z in a table with HTMX live search (name/email)
  - User names link to `/tasks?userId={id}` to show that user's tasks
  - HTMX requests return `users/user-table` fragment; full requests return `users/users`

- `controller/TaskController.java` - Task web UI endpoints
  - `@Controller` with `/tasks` base path
  - Returns Thymeleaf template names or fragment selectors
  - HTMX support: detects `HX-Request` header via `HtmxUtils.isHtmxRequest()`
  - `Object` return type on POST methods to allow returning either a String view name or `ResponseEntity`
  - Fires `HX-Trigger` events (`taskSaved`, `taskDeleted`) via `HtmxUtils.triggerEvent()`
  - Injects `TagService`, `UserService`, `CommentService`, and `OwnershipGuard`; adds `tags` list to all form-serving methods (user list fetched remotely by `<searchable-select>`)
  - Task list defaults to current user's tasks on first visit; explicit empty `userId=` param means "All Users"
  - Resolves `filterUserName` when filtering by another user's ID (passed to template for user filter button label)
  - `POST /{id}/comments` — add comment to task; returns `task-comments` template (whole file for hx-swap-oob comment count updates)
  - `DELETE /{id}/comments/{commentId}` — delete comment (owner or admin); returns `task-comments` template
  - Task delete changed from `@PostMapping("/{id}/delete")` to `@DeleteMapping("/{id}")`
  - **Security**: uses `OwnershipGuard` for edit/delete/comment-delete; new tasks default to current user (changeable via dropdown)

- `controller/FrontendConfigController.java` - Serves `/config.js` (JS runtime config)
  - `@RestController` producing `application/javascript`
  - Emits `window.APP_CONFIG = { routes: { ... }, messages: { ... } };`
  - `routes` — from `AppRoutesProperties`; `messages` — all keys from `messages.properties` via `ResourceBundle`
  - `escapeJs()` / `buildMessagesJson()` helpers sanitize values before embedding in JS output
  - Loaded by the `scripts` fragment on every page; `APP_CONFIG` is available globally to all page scripts
  - NOTE: Uses JVM default locale; for i18n, would need `MessageSource` with request `Locale` (conflicts with content-hash caching)

### Security Layer
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
  - Does NOT handle unassigned entities — callers should check `entity.getUser() == null` before calling if unassigned entities should be open
  - Used by both `TaskApiController` and `TaskController` (web)

- `security/AuthExpressions.java` - Ownership and role check logic (shared between templates and Java)
  - Exposed as `${#auth}` in Thymeleaf templates via `AuthDialect`
  - Instance methods (template use): `isOwner(OwnedEntity)`, `isAdmin()`, `canEdit(OwnedEntity)` (admin OR owner)
  - Static methods (Java use): `isOwner(User, OwnedEntity)`, `isAdmin(User)` — reused by `OwnershipGuard`
  - Unassigned entities (`entity.getUser() == null`): `isOwner()` and `canEdit()` return false — business rules for unassigned entities belong in the controller/template, not here

- `security/AuthDialect.java` - Thymeleaf `IExpressionObjectDialect` implementation
  - Registers `${#auth}` expression object, built per-request from `SecurityContextHolder`
  - Auto-discovered by Spring Boot; no manual configuration needed

- `security/SecurityUtils.java` - Static utility for getting current principal
  - `getCurrentPrincipal()` — returns username from `SecurityContextHolder` or `"system"` if unauthenticated
  - Used by services when publishing audit events

### Configuration
- `config/SecurityConfig.java` - Spring Security configuration
  - `PasswordEncoder` bean — `BCryptPasswordEncoder` (default strength)
  - `SecurityFilterChain` bean — HTTP security rules:
    - Public: `/login`, `/register`, static assets, `/favicon.svg`, `/h2-console/**`
    - Admin-only: `/admin/**`, `POST /api/tags`, `DELETE /api/tags/**`, `POST /api/users`, `DELETE /api/users/**`
    - Everything else: `authenticated()`
  - Form login: custom login page at `/login`, success → `/`, failure → `/login?error`
  - Logout: `POST /logout` → `/login?logout`, invalidates session, deletes JSESSIONID
  - CSRF: enabled for web forms (Thymeleaf auto-injects); disabled for `/api/**` and `/h2-console/**`
  - Headers: `X-Frame-Options: SAMEORIGIN` (for H2 console)

- `config/AppRoutesProperties.java` - `@ConfigurationProperties(prefix = "app.routes")`
  - Fields: `tasks` (default `/tasks`), `api` (default `/api`), `audit` (default `/admin/audit`)
  - Single source of truth for base paths used by both Thymeleaf templates and frontend JS

- `config/GlobalModelAttributes.java` - `@ControllerAdvice` that injects shared attributes into every Thymeleaf model
  - `@ModelAttribute("appRoutes")` — exposes the `AppRoutesProperties` bean as `${appRoutes}` in all templates
  - `@ModelAttribute("currentUser")` — resolves authenticated `User` from `SecurityContextHolder`; null for anonymous
  - Used by HTMX attributes (`th:attr="hx-get=${appRoutes.tasks + ...}"`) where `@{}` URL syntax cannot be used

### Exception Handling
- `exception/EntityNotFoundException.java` - Custom unchecked exception for missing entities

- `exception/StaleDataException.java` - Custom unchecked exception for optimistic locking conflicts (409)

- `exception/ApiExceptionHandler.java` - `@RestControllerAdvice` scoped to `controller.api`
  - Ordered at `HIGHEST_PRECEDENCE` to win over `WebExceptionHandler`
  - Handles: `MethodArgumentNotValidException` (400), `EntityNotFoundException` (404), `AccessDeniedException` (403), `StaleDataException` (409), catch-all `Exception` (500)
  - All responses are JSON maps with `timestamp`, `status`, `error` fields

- `exception/WebExceptionHandler.java` - `@ControllerAdvice` for Thymeleaf web controllers
  - Handles: `EntityNotFoundException` and `NoResourceFoundException` → `error/404.html`, `StaleDataException` → `error/409.html`, catch-all → `error/500.html`
  - `AccessDeniedException` is explicitly re-thrown so Spring Security's `ExceptionTranslationFilter` can handle it → `error/403.html` (without this, the catch-all `Exception` handler would swallow it as a 500)

### Utilities
- `util/HtmxUtils.java` - HTMX helper methods
  - `isHtmxRequest(HttpServletRequest)` - checks for `HX-Request: true` header
  - `triggerEvent(String eventName)` - returns `ResponseEntity` with `HX-Trigger` header set

### Bootstrap
- `DataLoader.java` - Seeds database on startup: **50 users**, **8 tags**, **300 tasks**
  - First user (Alice Johnson) gets `Role.ADMIN`; all others get `Role.USER`
  - All passwords: `"password"` (BCrypt-encoded once, reused for all 50 users for speed)
  - Dev credentials: `alice.johnson@example.com` / `password` (admin), `bob.smith@example.com` / `password` (regular)
  - Tags use orthogonal dimensions: domain (Work/Personal/Home), priority (Urgent/Someday), type (Meeting/Research/Errand)
  - Each task gets 1–2 tags drawn from different dimensions for natural combos (e.g. "Work + Urgent")
  - `seedTask` takes `TaskStatus` (OPEN, IN_PROGRESS, COMPLETED) instead of boolean `completed`
  - ~80% of tasks are assigned to a user (every 5th task is unassigned)
  - Priority distribution: ~20% HIGH, ~40% MEDIUM, ~40% LOW
  - Due dates: ~80% of tasks get a due date spread -10 to +30 days from today (creates a mix of overdue and upcoming)

## Thymeleaf Templates

### Layouts
- `templates/layouts/base.html` - Base layout with reusable fragments
  - `head(title, cssFile)` - two-parameter head fragment; `cssFile` is nullable for pages without page-specific CSS; includes `<link rel="icon">` for SVG favicon
  - `navbar` - navigation bar with auth-aware elements:
    - Left nav links: Tasks, Tags, Users
    - Anonymous: shows Register link
    - Authenticated: user dropdown with name, email, role badge, logout button
    - Admin: additional "User Management", "Audit Log", and "Settings" links in dropdown
    - Uses `sec:authorize` (Spring Security Thymeleaf dialect) and `${#auth}` for conditional rendering
  - `footer` - footer
  - `scripts` - Bootstrap + HTMX + `/config.js` + `utils.js` (in that order — `APP_CONFIG` must be set before page scripts run)

- `templates/layouts/pagination.html` - Reusable pagination control bar
  - `controlBar(page, position, label)` — `page` is `Page<?>`, `position` is `'top'`/`'bottom'`, `label` is item noun (e.g. "tasks", "entries")
  - Renders: result count, page navigation with ellipsis (±2 window), per-page selector (10/25/50/100)
  - Dispatches custom DOM events (`pagination:navigate`, `pagination:resize`) instead of calling named JS functions
  - `th:selected` on `<option>` elements auto-syncs per-page selector after HTMX swaps

### Task Views
- `templates/tasks/tasks.html` - Main task list page
  - Live search (JS-debounced, 300ms), status filter buttons (All/Open/In Progress/Completed/Overdue, all btn-sm), priority dropdown filter, user filter (All Users / Mine), tag filter dropdown with pills, sort dropdown (includes priority and due date), view toggle (cards/table)
  - All state managed in JS (`tasks.js`) — synced to URL params and cookies
  - Contains two shared modal shells loaded once per page:
    - `#task-modal` — create/edit form, content loaded via HTMX
    - `#task-delete-modal` — delete confirmation, populated via `show.bs.modal` JS event

- `templates/tasks/task-cards.html` - Card grid fragment (`grid` fragment)
- `templates/tasks/task-card.html` - Individual task card fragment (`card` fragment, reads `${task}` from context); 3-state status badge and toggle button
- `templates/tasks/task-table.html` - Table view fragment (`grid` fragment)
- `templates/tasks/task-table-row.html` - Single table row fragment (`row` fragment); 3-state status badge and toggle button
- `templates/tasks/task-comments.html` - Shared comments fragment with dual usage
  - `:: list` fragment selector — returns comment list only (used by `task.html` and `task-modal.html` during page render)
  - Whole-file return — includes comment list + `hx-swap-oob` spans for comment count updates (used by controller for HTMX add/delete responses)
  - Delete buttons use `hx-delete` with `hx-confirm` and `data-confirm-*` attributes for styled Bootstrap confirmation dialog
- `templates/tasks/task-audit.html` - Shared audit history entries fragment (used by both `task.html` and `task-modal.html`)

- `templates/tasks/task-form.html` - **Shared form fields fragment only**
  - `fields` fragment — title, description, status radio buttons (3-state: Open/In Progress/Completed, shown on edit only, hidden on create since new tasks default to OPEN), priority radio buttons (with reception bar icons), due date picker, user `<searchable-select>` (remote, one value, @ManyToOne), tag checkboxes (multiple, @ManyToMany)
  - No `<form>` tag; `th:object` is set by the including template
  - Used by both `task.html` and `task-modal.html`

- `templates/tasks/task.html` - Full-page create/edit form; includes comments card section between form and audit history (edit mode)
- `templates/tasks/task-modal.html` - HTMX modal content (bare file, split-panel layout); comments and history as exclusive side panels toggled via `toggleTaskPanel(name)`; footer moved outside `d-flex` for full-width; submit button uses `form="task-form"` attribute

### Home, Tag, User, Auth, Admin, Error Views
- `templates/home.html` - Home page with hero section and 6 feature cards (REST API, Spring Security, Dynamic UI, Data & Persistence, Task Lifecycle, Admin & Audit); all strings from `messages.properties` (`home.feature.*` keys)
- `templates/tags/tags.html` - Tag list page (table, badge links to task filter)
- `templates/users/users.html` - User list page (HTMX live search)
- `templates/users/user-table.html` - User table fragment (bare file)
- `templates/login.html` - Login page (Spring Security handles POST)
- `templates/register.html` - Registration page
- `templates/error/403.html` - Access Denied page
- `templates/error/404.html` - Not Found page
- `templates/error/409.html` - Conflict page (optimistic locking, rendered by `WebExceptionHandler`)
- `templates/error/500.html` - Server Error page
- `templates/admin/users.html` - Admin user management page (collapsible create user form, role change table, toast on creation)
- `templates/admin/audit.html` - Audit log page (admin only)
- `templates/admin/audit-table.html` - Audit table fragment (HTMX partial)

## Static Resources

- `static/favicon.svg` - SVG favicon (blue rounded square with white "S")
- `static/css/base.css` - Global styles (body, btn transitions, validation, navbar, footer, HTMX indicator, toast container/animations); `.card-clip` for overflow clipping; `.card-lift` opt-in hover lift; `#confirm-modal` z-index and width styles
- `static/css/tasks.css` - Task page styles (filters, search clear button, tag badges, `.task-side-panel` and `.task-side-panel-body` for exclusive side panels, active state styles for 3 status filter buttons)
- `static/css/audit.css` - Audit page styles (category buttons, search clear button)
- `static/css/components/searchable-select-bootstrap5.css` - Bootstrap 5 theme for `<searchable-select>`
- `static/js/utils.js` - Shared utilities (`getCookie`, `setCookie`); `showToast(message, type)` for toast notifications; `showConfirm(options, onConfirm)` for styled Bootstrap confirm dialogs; CSRF injection for HTMX; `htmx:confirm` integration with `data-confirm-*` attributes; 409 conflict handler
- `static/js/tasks.js` - Task list page logic (sort, filters, search, pagination, modal wiring); `setStatusFilter` uses enum names (OPEN, IN_PROGRESS, COMPLETED); filter button IDs use enum names; `toggleTaskPanel(name)` for exclusive side panel toggle (comments OR history); task delete uses `hx-delete`
- `static/js/audit.js` - Audit page logic (category filter, search, date range, pagination)
- `static/js/components/searchable-select.js` - Reusable `<searchable-select>` Web Component
- `static/bootstrap-icons/` - Bootstrap Icons (locally hosted)

## Resource Files

- `resources/messages.properties` - UI display strings
  - Namespace conventions:
    - `action.*` — generic actions; `pagination.*` — pagination controls
    - `nav.*`, `footer.*`, `page.title.*` — layout strings
    - `task.*` — Task feature (includes `task.status.open`, `task.status.inProgress`, `task.status.completed`); `tag.*` — Tag feature; `user.*` — User feature
    - `login.*`, `register.*` — Auth pages
    - `admin.*` — Admin panel; `audit.*` — Audit feature (includes `audit.field.status`)
    - `home.feature.*` — Home page feature cards (rest, security, ui, data, lifecycle, admin)
    - `role.*` — Role display names; `error.*` — Error pages; `toast.*` — Toast notifications

- `resources/META-INF/additional-spring-configuration-metadata.json` - IDE metadata for custom `app.routes.*` properties

- `resources/ValidationMessages.properties` - Bean Validation error messages
  - Used by Hibernate Validator; reference with `{key}` syntax in constraint annotations
  - `{min}`, `{max}` placeholders interpolated from annotation attributes
