# CLAUDE-reference.md - Detailed File Reference

This file contains per-file documentation for every Java class, template, static resource, and configuration file in the project. It is NOT loaded automatically — Claude reads it on demand when it needs detailed information about a specific file.

For architecture, patterns, conventions, and workflow, see [CLAUDE.md](CLAUDE.md).

## Java Source Files

### Model Layer
- `model/Task.java` - Entity class with JPA annotations; implements `OwnedEntity`
  - Fields: id, version, title, description, status, priority, priorityOrder, startDate, dueDate, effort, createdAt, completedAt, updatedAt, project, tags, user, checklistItems, checklistTotal, checklistChecked, blocks, blockedBy, blocked
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_VERSION`, `FIELD_TITLE`, `FIELD_DESCRIPTION`, `FIELD_STATUS`, `FIELD_PRIORITY`, `FIELD_PRIORITY_ORDER`, `FIELD_DUE_DATE`, `FIELD_START_DATE`, `FIELD_EFFORT`, `FIELD_CREATED_AT`, `FIELD_COMPLETED_AT`, `FIELD_UPDATED_AT`, `FIELD_PROJECT`, `FIELD_TAGS`, `FIELD_USER`, `FIELD_COMMENTS`, `FIELD_CHECKLIST_ITEMS`, `FIELD_CHECKLIST_TOTAL`, `FIELD_CHECKLIST_CHECKED`, `FIELD_BLOCKS`, `FIELD_BLOCKED_BY`, `FIELD_BLOCKED`) — used in mappers, specifications, and `toAuditSnapshot()`
  - `@Version` on `version` field — JPA optimistic locking; Hibernate auto-increments on each update and throws `OptimisticLockException` on stale writes
  - `status` — `@Enumerated(EnumType.STRING)`, `TaskStatus` enum (BACKLOG, OPEN, IN_PROGRESS, IN_REVIEW, COMPLETED, CANCELLED), defaults to `OPEN`
  - `isCompleted()` — derived convenience method, returns `status == TaskStatus.COMPLETED` (no backing field)
  - `priority` — `@Enumerated(EnumType.STRING)`, defaults to `MEDIUM`
  - `priorityOrder` — `@Formula("CASE priority WHEN 'LOW' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'HIGH' THEN 2 END")` virtual column for correct sort order (STRING enums sort alphabetically without this)
  - `startDate` — `LocalDate`, `@DateTimeFormat(iso = ISO.DATE)` for HTML5 `<input type="date">` binding
  - `dueDate` — `LocalDate`, `@DateTimeFormat(iso = ISO.DATE)` for HTML5 `<input type="date">` binding
  - `completedAt` — `LocalDateTime`, set automatically by `TaskService` when status changes to COMPLETED (cleared when un-completed)
  - `updatedAt` — `LocalDateTime`, set by `@PrePersist` / `@PreUpdate` lifecycle callbacks
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "project_id", nullable = false)` — every task belongs to a project
  - `@ManyToMany(fetch = LAZY)` + `@JoinTable(name = "task_tags")` — Task is the owning side; `Set<Tag>`
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — Task owns the FK column; user is optional (nullable)
  - `@OneToMany(mappedBy = "task", cascade = ALL, orphanRemoval = true)` + `@OrderBy("sortOrder ASC")` — checklist items owned by task; cascade delete; `List<ChecklistItem>` (uses `@OrderColumn` for drag-and-drop ordering)
  - `@OneToMany(mappedBy = "task")` — comments; `Set<Comment>`
  - `blocks` (`Set<Task>`) — `@ManyToMany(fetch = LAZY)` + `@JoinTable(name = "task_dependencies", joinColumns = "blocking_task_id", inverseJoinColumns = "blocked_task_id")` — tasks this task blocks; owning side
  - `blockedBy` (`Set<Task>`) — `@ManyToMany(mappedBy = "blocks", fetch = LAZY)` — tasks that block this task; inverse side
  - `blocked` (boolean) — `@Formula` virtual column, true when at least one non-terminal task in `blockedBy` exists
  - `checklistTotal` — `@Formula` subquery counting all checklist items (virtual column, avoids loading collection on list views)
  - `checklistChecked` — `@Formula` subquery counting checked items (virtual column for progress display)
  - Validation: `@NotBlank`, `@Size` constraints
  - Manual getters/setters (no Lombok on entities)

- `model/TaskStatus.java` - Enum for task lifecycle states: `BACKLOG`, `OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `COMPLETED`, `CANCELLED`
  - Stored as string via `@Enumerated(EnumType.STRING)` on Task
  - Status advance cycle: BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED → OPEN; CANCELLED → OPEN
  - `isTerminal()` — returns true for COMPLETED and CANCELLED (done states)
  - `terminalStatuses()` — returns `List.of(COMPLETED, CANCELLED)`; used by overdue checks, incomplete counts, and due reminders
  - CANCELLED is not part of the advance cycle — it's set explicitly via the status radio buttons
  - Implements `Translatable`; `getMessageKey()` returns corresponding `task.status.*` message key

- `model/Priority.java` - Enum for task priority levels: `LOW`, `MEDIUM`, `HIGH`
  - Stored as string via `@Enumerated(EnumType.STRING)` on Task
  - Implements `Translatable`; `getMessageKey()` returns corresponding `task.priority.*` message key

- `model/Comment.java` - Comment entity; implements `OwnedEntity` and `Auditable`
  - Fields: id, text, createdAt, task, user
  - `FIELD_*` constants (`FIELD_TEXT`, `FIELD_TASK`, `FIELD_USER`)
  - `text` — `@NotBlank`, `@Size(max = 500)`
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "task_id")` — owning side to Task
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — owning side to User
  - Manual getters/setters (no Lombok on entities)

- `model/OwnedEntity.java` - Marker interface for entities that have an owner
  - Single method: `User getUser()` — returns owner or null if unassigned
  - Implemented by `Task`, `Comment`, and `SavedView`; enables generic ownership checks via `AuthExpressions` and `OwnershipGuard`
  - Future entities with ownership can implement this for automatic access control

- `model/Translatable.java` - Interface for enums with externalized display names
  - Single method: `String getMessageKey()` — returns the `messages.properties` key for this enum value
  - Implemented by `TaskStatus`, `Priority`, `ProjectRole`, `ProjectStatus`, `Role`
  - Enables generic translation via `Messages.get(Translatable)` without enum-specific switch logic

- `model/SavedView.java` - Entity for user-saved filter views; implements `OwnedEntity`
  - Fields: id, user (ManyToOne), name (max 100), filters (JSON string, max 2000), createdAt
  - `filters` — serialized JSON string of filter state (search, status, priority, etc.); opaque to the backend
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — owning side to User
  - `@PrePersist` sets `createdAt`
  - Manual getters/setters (no Lombok on entities)

- `model/Project.java` - Project entity; implements `Auditable`
  - Fields: id, name, description, status, createdBy, createdAt, updatedAt, members, tasks
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_NAME`, `FIELD_DESCRIPTION`, `FIELD_STATUS`, `FIELD_CREATED_BY`, `FIELD_CREATED_AT`, `FIELD_UPDATED_AT`, `FIELD_MEMBERS`, `FIELD_MEMBER`, `FIELD_ROLE`)
  - `name` — `@NotBlank`, `@Size(min = 1, max = 100)`
  - `description` — `@Size(max = 500)`
  - `status` — `@Enumerated(EnumType.STRING)`, `ProjectStatus` enum (ACTIVE, ARCHIVED), defaults to `ACTIVE`
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "created_by")` — project creator
  - `@OneToMany(mappedBy = "project", cascade = ALL, orphanRemoval = true)` — members; `Set<ProjectMember>`
  - `@OneToMany(mappedBy = "project", cascade = ALL, orphanRemoval = true)` — tasks; `Set<Task>`
  - `@PrePersist` / `@PreUpdate` lifecycle callbacks set `updatedAt`
  - `toAuditSnapshot()` — captures name, description, status
  - Manual getters/setters (no Lombok on entities)

- `model/ProjectStatus.java` - Enum for project lifecycle: `ACTIVE`, `ARCHIVED`
  - Stored as string via `@Enumerated(EnumType.STRING)` on Project
  - Implements `Translatable`; `getMessageKey()` returns corresponding `project.status.*` message key

- `model/ProjectRole.java` - Enum for project membership roles: `VIEWER`, `EDITOR`, `OWNER`
  - Stored as string via `@Enumerated(EnumType.STRING)` on ProjectMember
  - VIEWER = read-only; EDITOR = read/write tasks; OWNER = full project control
  - Implements `Translatable`; `getMessageKey()` returns corresponding `project.role.*` message key

- `model/ProjectMember.java` - Project membership entity (user + role per project)
  - Fields: id, project, user, role, createdAt
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_PROJECT`, `FIELD_USER`, `FIELD_ROLE`, `FIELD_CREATED_AT`)
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))` — one membership per user per project
  - `@ManyToOne(fetch = LAZY)` to both Project and User
  - `role` defaults to `EDITOR`
  - Convenience constructor: `ProjectMember(Project, User, ProjectRole)`
  - Manual getters/setters (no Lombok on entities)

- `model/Role.java` - Enum with two values: `USER`, `ADMIN`
  - Stored as string in database via `@Enumerated(EnumType.STRING)` on User
  - Defaults to `USER` for new registrations and API-created users
  - Implements `Translatable`; `getMessageKey()` returns corresponding `role.*` message key

- `model/Tag.java` - Tag entity
  - Fields: id, name (unique, max 50 chars)
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_NAME`)
  - `@ManyToMany(mappedBy = "tags", fetch = LAZY)` — Tag is the inverse side (no @JoinTable here); `Set<Task>`
  - Manual getters/setters; `equals()`/`hashCode()` use `getId()` (not field access) for Hibernate proxy safety

- `model/User.java` - User entity with authentication fields
  - Fields: id, name (max 100), email (max 150, unique), password (max 72, nullable), role (Role enum, defaults to USER), enabled (boolean, defaults to true)
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_NAME`, `FIELD_EMAIL`, `FIELD_ROLE`, `FIELD_ENABLED`)
  - `password` — BCrypt hash; nullable for API-created users (who cannot log in)
  - `role` — `@Enumerated(EnumType.STRING)`, stored as "USER" or "ADMIN" in the database
  - `enabled` — disabled users cannot log in and are hidden from assignment dropdowns
  - `@OneToMany(mappedBy = "user", fetch = LAZY)` — inverse side; no cascade (service handles task reassignment on delete); `Set<Task>`
  - Manual getters/setters; `equals()`/`hashCode()` use `getId()` (not field access) for Hibernate proxy safety

- `model/ChecklistItem.java` - Checklist item entity for task sub-items
  - Fields: id, text, checked, sortOrder, task
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_TEXT`, `FIELD_CHECKED`, `FIELD_SORT_ORDER`, `FIELD_TASK`)
  - `text` — `@NotBlank`, `@Size(max = 200)`
  - `checked` — boolean, defaults to `false`
  - `sortOrder` — int, defaults to `0`; used by `@OrderBy` on `Task.checklistItems`
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "task_id")` — owning side to Task
  - Convenience constructor: `ChecklistItem(String text, int sortOrder)`
  - Manual getters/setters (no Lombok on entities)

- `model/UserPreference.java` - Per-user preference entity (key/value rows per user)
  - Fields: id, user, key, value
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_USER`, `FIELD_KEY`, `FIELD_VALUE`)
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "pref_key"}))` — one row per user+key
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — owning side to User
  - `key` mapped to `pref_key` column, `value` mapped to `pref_value` column (avoids SQL reserved words)
  - Manual getters/setters (no Lombok on entities); `equals()`/`hashCode()` use `getId()`

- `model/Notification.java` - Notification entity
  - Fields: id, user (recipient), actor, type, message, link, read, createdAt
  - `FIELD_*` constants (`FIELD_ID`, `FIELD_USER`, `FIELD_ACTOR`, `FIELD_TYPE`, `FIELD_MESSAGE`, `FIELD_LINK`, `FIELD_READ`, `FIELD_CREATED_AT`)
  - `@ManyToOne(fetch = LAZY)` to User for both `user` (recipient, non-null) and `actor` (nullable)
  - `type` — `@Enumerated(EnumType.STRING)`, `NotificationType` enum
  - `read` — `@Column(name = "is_read")` to avoid SQL reserved word conflict
  - Convenience constructor: `Notification(user, actor, type, message, link)` — sets `read = false` and `createdAt = now()`
  - Manual getters/setters (no Lombok on entities)

- `model/NotificationType.java` - Enum for notification types: `TASK_ASSIGNED`, `COMMENT_ADDED`, `COMMENT_MENTIONED`, `TASK_DUE_REMINDER`, `TASK_OVERDUE`, `SYSTEM`
  - Stored as string via `@Enumerated(EnumType.STRING)` on Notification

- `model/Sprint.java` - JPA entity for time-boxed iterations
  - Fields: id, name (max 100), goal (max 500), startDate, endDate, createdAt, updatedAt, project (ManyToOne LAZY)
  - Status derived from date ranges: `isPast()` (endDate < today), `isActive()` (startDate <= today <= endDate), `isFuture()` (startDate > today)
  - `FIELD_*` constants, `Auditable` interface
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "project_id", nullable = false)` — every sprint belongs to a project
  - Manual getters/setters (no Lombok on entities)

- `model/Recurrence.java` - Enum for recurring task frequency
  - Values: `DAILY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`
  - Implements `Translatable` with `recurring.recurrence.*` message keys

- `model/RecurringTaskTemplate.java` - JPA entity for recurring task blueprints
  - Fields: id, title (max 100), description (max 500), priority, effort, recurrence (Recurrence enum), dayOfWeek, dayOfMonth, dueDaysAfter, nextRunDate, endDate (nullable = open-ended), enabled (boolean), lastGeneratedAt, createdAt, updatedAt, project (ManyToOne LAZY), assignee (ManyToOne LAZY, nullable), createdBy (ManyToOne LAZY), tags (ManyToMany via `recurring_template_tags`)
  - `calculateNextRunDate(LocalDate)` — advances date based on recurrence pattern
  - `FIELD_*` constants, `Auditable` interface, `toAuditSnapshot()`
  - Only available on non-sprint-enabled projects

- `model/RecentView.java` - JPA entity for recently viewed items
  - Fields: id, user (ManyToOne LAZY), entityType (String), entityId (Long), entityTitle (max 200), viewedAt (LocalDateTime)
  - `TYPE_TASK`, `TYPE_PROJECT` — entity type constants
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"}))` — one row per user+type+entity
  - `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")` — owning side to User
  - Manual getters/setters (no Lombok on entities)

- `model/AuditLog.java` - Audit log entity
  - Fields: id, action (String), entityType (String), entityId (Long), principal (String), details (String/JSON), timestamp (Instant)
  - `FIELD_*` constants (`FIELD_ACTION`, `FIELD_PRINCIPAL`, `FIELD_DETAILS`, `FIELD_TIMESTAMP`)
  - `@Transient detailsMap` — parsed JSON details for template rendering; populated by `AuditLogService`
  - `toAuditSnapshot()` — entities provide snapshot maps for audit diffing

### Audit Package
- `audit/AuditEvent.java` - Event class published via `ApplicationEventPublisher`
  - `CATEGORIES` — `List.of("PROJECT", "TASK", "USER", "PROFILE", "COMMENT", "TAG", "AUTH", "SETTING")` — single source of truth for filter UI and query logic; each event constant must be prefixed with one of these
  - Project constants: `PROJECT_CREATED`, `PROJECT_UPDATED`, `PROJECT_ARCHIVED`, `PROJECT_UNARCHIVED`, `PROJECT_DELETED`, `PROJECT_MEMBER_ADDED`, `PROJECT_MEMBER_REMOVED`, `PROJECT_MEMBER_ROLE_CHANGED`
  - Task constants: `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`
  - User constants: `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`, `USER_DISABLED`, `USER_ENABLED`, `USER_PASSWORD_RESET`, `USER_ROLE_CHANGED`, `USER_REGISTERED`
  - Other constants: `PROFILE_UPDATED`, `PROFILE_PASSWORD_CHANGED`, `COMMENT_CREATED`, `COMMENT_DELETED`, `TAG_CREATED`, `TAG_DELETED`, `SETTING_UPDATED`, `AUTH_SUCCESS`, `AUTH_FAILURE`
  - Fields: action, entityType, entityId, principal, details

- `audit/AuditField.java` - Typed audit snapshot value record
  - `FieldType` enum: `TEXT`, `ENUM`, `DATE`, `NUMBER`, `BOOLEAN`, `REFERENCE`, `COLLECTION`, `CHECKLIST`
  - Static factory methods: `text()`, `enumValue()`, `date()`, `number()`, `bool()`, `ref()`, `collection()`, `checklist()`
  - Convenience overloads: `enumValue(Enum<?>)`, `ref(T, Function<T,Long>, Function<T,String>, String)`, `collection(Collection<T>, Function<T,String>)`, `checklist(Collection<T>, Predicate<T>, Function<T,String>)`
  - `valueEquals(AuditField, AuditField)` — semantic comparison (REFERENCE by refId, COLLECTION/CHECKLIST by items)
  - `diffChecklist(List, List)` — item-level diff between checklist snapshots
  - Constants: `ENUM_REGISTRY`, `REF_*` entity types, `FIELD_*` JSON field names, checklist prefixes

- `audit/AuditDetails.java` - Audit detail utilities
  - `toJson(Map<String, ?>)` — serializes snapshot to JSON string
  - `diff(Map<String, AuditField>, Map<String, AuditField>)` — computes typed field-level changes as `{ field: { old: AuditField, new: AuditField } }`
  - `resolveDisplayNames(Map, MessageSource, Locale)` — maps raw field keys to human-readable names via `audit.field.{key}` message keys; falls back to raw key if no message found

- `audit/AuditTemplateHelper.java` - `@Component` for Thymeleaf audit rendering
  - `resolveEnumLabel(enumClass, constant)` — translates enum constant to localized label via `ENUM_REGISTRY`
  - `resolveUrl(refType, refId)` — resolves entity reference to URL (Project, Task)
  - `diffChecklist(oldItems, newItems)` — delegates to `AuditField.diffChecklist()`
  - `formatItem(item)` — decodes `[x]/[ ]` prefix to Unicode ☑/☐
  - `isBlank(Map<String, Object>)` — detects empty typed fields in deserialized maps

- `audit/AuditEventListener.java` - `@EventListener` that persists `AuditEvent` → `AuditLog`
- `audit/AuthAuditListener.java` - `@EventListener` for Spring's `AuthenticationSuccessEvent`/`AuthenticationFailureBadCredentialsEvent`; saves directly to `AuditLogRepository` with `@Transactional` (cannot use `ApplicationEventPublisher` → `@TransactionalEventListener` because Spring Security auth events fire outside Spring-managed transactions)

### Event Package
- `event/TaskAssignedEvent.java` - Record published when a task is assigned to someone; fields: `task` (Task), `actor` (User)
- `event/TaskUpdatedEvent.java` - Record published when task fields change; fields: `task` (Task), `actor` (User)
- `event/ProjectUpdatedEvent.java` - Record published when project fields change; fields: `project` (Project), `actor` (User)
- `event/CommentAddedEvent.java` - Record published when a comment is created; fields: `comment` (Comment), `task` (Task), `actor` (User)
- `event/TaskChangeEvent.java` - Record for WebSocket task change broadcast; fields: `action` (String), `taskId` (long), `userId` (long); serialized to JSON for JS clients
- `event/CommentChangeEvent.java` - Record for WebSocket comment change broadcast; fields: `action` (String), `taskId` (long), `commentId` (long), `userId` (long); serialized to JSON for JS clients
- `event/RecentViewEventListener.java` - `@TransactionalEventListener` for title sync; listens to `TaskUpdatedEvent` and `ProjectUpdatedEvent`, calls `RecentViewService.updateTitle()` to keep cached titles current
- `event/NotificationEventListener.java` - Centralized notification routing; listens for domain events (`TaskAssignedEvent`, `TaskUpdatedEvent`, `CommentAddedEvent`) and decides who gets notified via `NotificationService.create()`
- `event/WebSocketEventListener.java` - Handles ephemeral WebSocket broadcasting; listens for `TaskChangeEvent` → `/topic/tasks`, `CommentChangeEvent` → `/topic/tasks/{taskId}/comments`

### Presence Package
- `presence/PresenceService.java` - Online user tracking via `ConcurrentHashMap<String, Long>` (session ID → user ID)
  - Handles multi-tab: same user with multiple sessions tracked as separate entries, `getOnlineUsers()` returns distinct sorted names resolved via `UserService`
  - `userConnected(sessionId, userId)`, `userDisconnected(sessionId)`, `getOnlineUsers()`, `getOnlineCount()`
- `presence/PresenceEventListener.java` - WebSocket session lifecycle listener
  - `@EventListener` for `SessionConnectEvent` and `SessionDisconnectEvent`
  - On connect: resolves user via `SecurityUtils.getUserFrom(principal)`, registers user ID with `PresenceService`
  - On disconnect: removes session from `PresenceService`
  - Broadcasts updated presence payload to `/topic/presence` after each event

### Repository Layer
- `repository/TaskRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Task, Long>` and `JpaSpecificationExecutor<Task>`
  - Active query methods:
    - `findByStatusNotIn(Collection<TaskStatus>)` - used by `getIncompleteTasks()` (finds tasks not in terminal statuses)
    - `findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String, String)` - used by `searchTasks()`
    - `findByUser(User)` - used by `UserService.deleteUser()` to reassign tasks before deleting a user
    - `countByUserAndStatus`, `countByUserAndDueDateBeforeAndStatusNotIn`, `countByStatus`, `countByDueDateBeforeAndStatusNotIn` — dashboard counts
    - `findTop5ByUserOrderByCreatedAtDesc` — recent tasks for dashboard
    - `findByUserAndDueDateBetweenAndStatusNotIn` — due this week for dashboard
    - `findByDueDateAndStatusNotIn` — scheduled reminders
  - `@EntityGraph` annotations (required since OSIV is disabled):
    - `findById`: `{"tags", "user", "project", "checklistItems"}` — full eager load for edit form/detail page
    - `findAll()`: `{"tags", "user", "project"}` — REST API mapper accesses these
    - `findByStatusNotIn()`, `findByTitleContaining...()`: `{"tags", "user"}` — list queries
    - `findAll(Specification, Pageable)`: `{"tags", "user", "project"}` — paginated task list (cards/table/calendar)
    - `findTop5ByUserOrderByCreatedAtDesc()`: `{"project"}` — dashboard recent tasks display project name
    - `findByUserAndDueDateBetweenAndStatusNotIn()`: `{"project"}` — dashboard due-this-week tasks display project name
    - `findByDueDateAndStatusNotIn()`: `{"user"}` — scheduled reminders access task.getUser()
  - `JpaSpecificationExecutor` used by `searchAndFilterTasks()` for paginated filtering

- `repository/ProjectRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Project, Long>`
  - `findById(Long)` — `@EntityGraph(attributePaths = {"createdBy", "members", "members.user"})` for eager loading
  - `findByStatusOrderByNameAsc(ProjectStatus)` — `@EntityGraph(attributePaths = {"createdBy"})` for sorted project list
  - `findAllByOrderByNameAsc()` — `@EntityGraph(attributePaths = {"createdBy"})` for admin all-projects list
  - `findByStatusOrderByCreatedAtDesc(ProjectStatus)` — `@EntityGraph(attributePaths = {"createdBy"})` for newest-first sort
  - `findAllByOrderByCreatedAtDesc()` — `@EntityGraph(attributePaths = {"createdBy"})` for admin newest-first

- `repository/ProjectMemberRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<ProjectMember, Long>`
  - `findByProjectId(Long)` — `@EntityGraph(attributePaths = {"user"})` for member list
  - `findByProjectIdAndUserId(Long, Long)` — single member lookup
  - `existsByProjectIdAndUserId(Long, Long)` — membership check
  - `countByProjectIdAndRole(Long, ProjectRole)` — count owners
  - `findByUserId(Long)` — `@EntityGraph(attributePaths = {"project", "project.createdBy"})` for user's projects
  - `deleteByProjectIdAndUserId(Long, Long)` — remove member

- `repository/TaskSpecifications.java` - JPA Specifications for dynamic queries
  - Uses entity `FIELD_*` constants everywhere (e.g. `Task.FIELD_STATUS`, `Task.FIELD_PROJECT`, `User.FIELD_ID`, `Tag.FIELD_ID`)
  - `build(projectId, keyword, statusFilter, overdue, priority, userId, tagIds)` - single-project query (for `/projects/{id}`)
  - `build(projectId, keyword, statusFilter, overdue, priority, userId, tagIds, dueDateFrom, dueDateTo)` - single-project with date range
  - `buildForProjects(accessibleProjectIds, keyword, statusFilter, overdue, priority, userId, tagIds)` - cross-project query (for `/tasks`, `/api/tasks`)
  - `buildForProjects(accessibleProjectIds, keyword, statusFilter, overdue, priority, userId, tagIds, dueDateFrom, dueDateTo)` - cross-project with date range
  - `withProjectId(Long)` — filters tasks by single project
  - `withProjectIds(List<Long>)` — filters tasks by project membership; null = no filter (admin sees all)
  - `withStatusFilter(TaskStatusFilter)` — maps filter enum name directly to `TaskStatus` enum (ALL returns all; others filter by matching status)
  - `withOverdue(boolean)` — filters to non-terminal tasks with past due dates
  - `withPriority(Priority)` — filters by priority level
  - `withUserId(Long)` — filters tasks by assigned user
  - `withTagIds(List<Long>)` — filters tasks having any of the given tags (OR logic, uses INNER JOIN + distinct)
  - `withDueDateBetween(LocalDate, LocalDate)` — filters tasks with due date in range
  - `withDateInRange(LocalDate, LocalDate)` — filters tasks visible on calendar: due date in range, or start-date-only tasks with start date in range

- `model/TaskStatusFilter.java` - Enum for task status filtering: `ALL`, `BACKLOG`, `OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `COMPLETED`, `CANCELLED`
  - `DEFAULT` constant (`"ALL"`) — for use in `@RequestParam` default value annotations
  - `from(String)` — safe conversion from URL param, returns ALL for null/unknown (case-insensitive)
  - Inner `StringConverter` — `@Component` auto-registered by Spring Boot; binds `?statusFilter=completed` → `TaskStatusFilter.COMPLETED`

- `repository/TagRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Tag, Long>`
  - `findByName(String)` — exact name lookup
  - `findAllByOrderByNameAsc()` — sorted tag list for tag page and task form checkboxes

- `repository/UserRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<User, Long>`
  - `findByEmail(String)` — used by `CustomUserDetailsService` for login and `@Unique` validator for duplicate checks
  - `findAllByOrderByNameAsc()` — sorted user list for admin panel (includes disabled users)
  - `findByNameContainingIgnoreCaseOrderByNameAsc(String)` — server-side user search for remote searchable-select
  - `findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(String, String)` — user page search (name or email)
  - `findByEnabledTrueOrderByNameAsc()` — enabled users only (for assignment dropdowns and public user lists)
  - `findByEnabledTrueAndNameContaining...` — enabled user search (name or email, case-insensitive)

- `repository/CommentRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Comment, Long>`
  - `findByTaskIdOrderByCreatedAtAsc(Long)` — chronological comment list; `@EntityGraph(attributePaths = {"user"})` to prevent N+1 on user names
  - `deleteByTaskId(Long)` — `@Modifying` `@Transactional` bulk delete; called by `CommentService.deleteByTaskId()` which is called by `TaskService.deleteTask()` before removing the task
  - `countByUserId(Long)` — count comments by user; used by `UserService.canDelete()` to determine if user can be hard-deleted
  - `findDistinctUsersByTaskId(Long)` — `@Query` returning distinct comment authors for a task
  - `findCommentTextsByTaskId(Long)` — `@Query` returning raw comment texts for a task; used by `CommentService.findPreviouslyMentionedUserIds()` to extract @mention tokens from prior comments

- `repository/NotificationRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Notification, Long>`
  - `countByUserIdAndReadFalse(Long)` — unread count for badge
  - `findTop10ByUserIdOrderByCreatedAtDesc(Long)` — recent notifications for dropdown; `@EntityGraph(attributePaths = {"actor"})` for mapper access
  - `findByUserIdOrderByCreatedAtDesc(Long, Pageable)` — paginated list for full page; `@EntityGraph(attributePaths = {"actor"})` for mapper access
  - `findByIdAndUserId(Long, Long)` — ownership-scoped single lookup
  - `markAllAsReadByUserId(Long)` — `@Modifying` `@Query` bulk UPDATE (no derived method convention for bulk updates)
  - `deleteByUserId(Long)` — clear all for a user
  - `deleteByCreatedAtBefore(LocalDateTime)` — purge old notifications

- `repository/SprintRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<Sprint, Long>`
  - `findByProjectIdOrderByStartDateDesc(Long)` — all sprints for a project, newest first
  - `findActiveByProjectId(Long, LocalDate)` — `Optional<Sprint>`; sprint whose date range contains the given date
  - `existsOverlapping(Long, Long, LocalDate, LocalDate)` — checks for overlapping date ranges (excludes self by ID)
  - `findWithProjectById(Long)` — `@EntityGraph(attributePaths = {"project"})` for eager project loading

- `repository/RecurringTaskTemplateRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<RecurringTaskTemplate, Long>`
  - `findByProjectIdOrderByCreatedAtDesc(Long)` — all templates for a project
  - `findWithDetailsById(Long)` — `@EntityGraph` eager-loading project, assignee, createdBy, tags
  - `findDueTemplates(LocalDate)` — enabled templates with `nextRunDate <= today`, active non-sprint projects

- `repository/RecentViewRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<RecentView, Long>`
  - `findByUserIdAndEntityTypeAndEntityId(Long, String, Long)` — upsert lookup
  - `findTop10ByUserIdOrderByViewedAtDesc(Long)` — recent views for a user (capped at 10)
  - `countByUserId(Long)` — total count for trimming
  - `deleteByUserIdAndIdNotIn(Long, List<Long>)` — trim oldest beyond 10
  - `updateTitle(String, String, Long, String)` — `@Modifying` `@Query` bulk title update across all users
  - `findByEntityTypeAndEntityId(String, Long)` — `@EntityGraph(attributePaths = {"user"})` for WebSocket push
  - `deleteByEntityTypeAndEntityId(String, Long)` — cleanup on entity delete
  - `deleteByUserId(Long)` — cleanup on user delete

- `repository/AuditLogRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<AuditLog, Long>` and `JpaSpecificationExecutor<AuditLog>`
  - `findByEntityTypeAndEntityIdOrderByTimestampDesc(String, Long)` — entity-specific audit history (used by task detail page)

- `repository/UserPreferenceRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<UserPreference, Long>`
  - `findByUserId(Long)` — all preferences for a user
  - `findByUserIdAndKey(Long, String)` — single preference lookup (upsert in service)

- `repository/SavedViewRepository.java` - Spring Data JPA repository
  - Extends `JpaRepository<SavedView, Long>`
  - `findByUserIdOrderByNameAsc(Long userId)` — all saved views for a user, sorted alphabetically

- `repository/AuditLogSpecifications.java` - JPA Specifications for dynamic audit queries
  - `withCategory(String)` — validates against `AuditEvent.CATEGORIES` list, then uses LIKE pattern (`"AUTH"` → `AUTH_%`, `"TASK"` → `TASK_%`)
  - `withSearch(String)` — case-insensitive LIKE on principal and details
  - `withFrom(Instant)` / `withTo(Instant)` — timestamp range
  - `build(category, search, from, to)` — combines all specs

- `repository/AnalyticsRepository.java` - Aggregate projection queries for analytics charts
  - Uses `EntityManager` directly (not Spring Data) — returns `Object[]` projections, not entities
  - Dynamic WHERE/AND clauses via `projectWhereClause()`/`projectAndClause()`/`bindProjectParams()` helpers — avoids triplicating queries for single project, project list, or all projects
  - `countByUserAndStatus(projectId, projectIds)` — workload distribution (group by user + status)
  - `countCreatedPerDay(projectId, projectIds, from)` — burndown: tasks created per day since date
  - `countCompletedPerDay(projectId, projectIds, from)` — burndown/velocity: tasks completed per day
  - `countOpenAtDate(projectId, projectIds, from, terminalStatuses)` — burndown: initial open count at start date
  - `countOverdueByUser(projectId, projectIds, terminalStatuses)` — overdue tasks grouped by assignee

### DTO Layer
- `dto/TaskRequest.java` - API input DTO (REST API create and update operations)
  - Fields: `projectId` (required on create), `title` (required, 1–100 chars), `description` (optional, max 500 chars), `priority` (optional `Priority`), `startDate` (optional `LocalDate`), `dueDate` (optional `LocalDate`), `effort` (optional `Short`, 0–32767), `tagIds` (optional `List<Long>`), `userId` (optional `Long`), `version` (null on create, required on update for optimistic locking)
  - Validation annotations used by `@Valid` in the controller
  - Lombok `@Data` for getters/setters/equals/hashCode

- `dto/TaskFormRequest.java` - Web form input DTO (task create/edit forms)
  - Fields: `title` (required, 1–100 chars), `description` (optional, max 500 chars), `status` (`TaskStatus`), `priority` (`Priority`), `startDate`, `dueDate` (both `@DateTimeFormat(iso = ISO.DATE)`), `effort` (optional `Short`), `version`
  - `fromEntity(Task)` — static factory; populates DTO from entity for edit form pre-fill
  - `toEntity()` — creates Task entity from form data (does not set project, tags, user, or checklist — controller handles those)
  - Does NOT include `tagIds`, `assigneeId`, `projectId`, or checklist arrays — those come as separate `@RequestParam` in the controller
  - Lombok `@Data`

- `dto/CommentRequest.java` - Comment input DTO
  - Fields: `text` (required, max 500 chars)
  - Lombok `@Data`

- `dto/ProjectRequest.java` - Project input DTO (create and edit forms)
  - Fields: `name` (required, 1–100 chars), `description` (optional, max 500 chars)
  - `fromEntity(Project)` — static factory; populates DTO from entity for settings form pre-fill
  - `toEntity()` — creates Project entity from form data (does not set createdBy or status — service handles those)
  - Lombok `@Data`

- `dto/TaskResponse.java` - API output DTO (returned by all read/write endpoints)
  - Fields: `id`, `title`, `description`, `status` (`TaskStatus`), `priority` (`Priority`), `dueDate` (`LocalDate`), `effort` (`Short`, nullable), `createdAt`, `tags` (`List<TagResponse>`), `user` (`UserResponse`, nullable), `version`, `blocked` (boolean), `blockedBy` (`List<TaskDependencyResponse>`), `blocks` (`List<TaskDependencyResponse>`)
  - Lombok `@Data`

- `dto/TaskDependencyResponse.java` - Lightweight dependency DTO for task relationship display
  - Fields: `id`, `title`, `status` (`TaskStatus`)
  - Lombok `@Data`

- `dto/TagResponse.java` - Tag output DTO
  - Fields: `id`, `name`
  - Lombok `@Data`

- `dto/UserRequest.java` - User input DTO (REST API)
  - Fields: `name` (required, max 100), `email` (required, max 150)
  - `@Unique(entity = User.class, field = User.FIELD_EMAIL)` — class-level uniqueness validation
  - Lombok `@Data`

- `dto/TagRequest.java` - Tag input DTO (admin tag management form)
  - Fields: `id` (null on create, set on edit), `name` (required, max 50)
  - `@Unique(entity = Tag.class, field = Tag.FIELD_NAME)` — class-level uniqueness validation
  - Lombok `@Data`

- `dto/UserResponse.java` - User output DTO
  - Fields: `id`, `name`, `email`
  - Lombok `@Data`

- `dto/RegistrationRequest.java` - Registration form DTO
  - Fields: `name` (required, max 100), `email` (required, max 150), `password` (required, 8–72 chars), `confirmPassword` (required)
  - `@Unique(entity = User.class, field = User.FIELD_EMAIL)` — class-level uniqueness validation (replaces manual duplicate email check)
  - Cross-field validation (password match) handled programmatically in `RegistrationController`
  - Lombok `@Data`

- `dto/CommentResponse.java` - Comment output DTO
  - Fields: `id`, `text`, `taskId`, `user` (`UserResponse`), `createdAt`
  - Lombok `@Data`

- `dto/NotificationResponse.java` - Notification output DTO
  - Fields: `id`, `type` (String), `message`, `link`, `read`, `createdAt` (`LocalDateTime`), `actorName`
  - Lombok `@Data`

- `dto/PresenceResponse.java` - Record for presence data
  - Fields: `users` (`List<String>`), `count` (int)
  - Used by both `PresenceApiController` (REST response) and `PresenceEventListener` (WebSocket broadcast to `/topic/presence`)

- `dto/TimelineEntry.java` - Record for unified activity timeline entries
  - Fields: `type` (String), `timestamp` (LocalDateTime), `commentId`, `commentText`, `commentUserName`, `commentUserId`, `canDelete` (comment fields), `auditAction`, `auditPrincipal`, `auditDetails` (audit fields)
  - `TYPE_COMMENT` / `TYPE_AUDIT` constants — discriminator values for the `type` field
  - Represents either a comment or an audit log event in a merged chronological timeline
  - Built by `TimelineService.getTimeline()`; consumed by `task-activity.html`

- `dto/CalendarDay.java` - Record for calendar view day cells
  - Fields: `date` (LocalDate), `currentMonth` (boolean), `today` (boolean), `tasks` (List<Task>)
  - Built by `TaskController.buildCalendarWeeks()` for the calendar grid template

- `dto/DashboardStats.java` - Record carrying all dashboard data
  - Personal stats: `myOpen`, `myInProgress`, `myInReview`, `myCompleted`, `myOverdue`, `myTotal`
  - Per-project summaries: `projectSummaries` (`List<ProjectSummary>`)
  - System stats (admin only): `totalTasks`, `totalOpen`, `totalCompleted`, `totalOverdue`, `onlineCount`
  - Lists: `myRecentTasks` (`List<Task>`), `dueSoon` (`List<Task>`), `recentActivity` (`List<AuditLog>`), `activityTaskTitles` (`Map<Long, String>`)
  - `editableProjects` (`List<Project>`) — for "New Task" button visibility
  - Immutable record — built by `DashboardService.buildStats()`

- `dto/ProjectSummary.java` - Per-project task stats for dashboard cards
  - Immutable record: `id`, `name`, `openTasks`, `inProgressTasks`, `inReviewTasks`, `completedTasks`, `overdueTasks`, `totalTasks`
  - Factory method `of(Project, ...)` for construction from service-level counts

- `dto/ProfileRequest.java` - Profile edit form DTO
  - Fields: `id` (set to current user's ID — used by `@Unique` to exclude self), `name` (required, max 100), `email` (required, max 150, @Email)
  - `@Unique(entity = User.class, field = User.FIELD_EMAIL)` — class-level uniqueness validation
  - Lombok `@Data`

- `dto/ChangePasswordRequest.java` - Password change form DTO
  - Fields: `currentPassword` (required), `newPassword` (required, 8–72 chars), `confirmPassword` (required)
  - Cross-field validation (password match, current password verification) handled programmatically in `ProfileController`
  - Lombok `@Data`

- `dto/AdminUserRequest.java` - Admin user create/edit form DTO
  - Fields: `id` (null on create, set on edit — used by `@Unique` to exclude self), `name` (required, max 100), `email` (required, max 150, @Email), `password` (no bean validation — controller validates manually: required on create, ignored on edit), `role` (required, defaults to USER)
  - `@Unique(entity = User.class, field = User.FIELD_EMAIL)` — class-level uniqueness validation
  - Lombok `@Data`

- `dto/BulkTaskRequest.java` - Bulk action input DTO (web controller `POST /tasks/bulk`)
  - Fields: `taskIds` (required, `@NotEmpty`, `List<Long>`), `action` (required, `@NotBlank`), `value` (optional)
  - Action constants: `ACTION_STATUS`, `ACTION_PRIORITY`, `ACTION_ASSIGN`, `ACTION_EFFORT`, `ACTION_DELETE`
  - Lombok `@Data`

- `dto/TaskListQuery.java` - Controller-layer binding DTO for task list query parameters (`@ModelAttribute`)
  - Fields: `search`, `statusFilter` (default ALL), `overdue`, `priority`, `selectedUserId`, `tags`
  - `toCriteria(List<Long> accessibleProjectIds)` — factory for cross-project searches
  - `toCriteria(Long projectId)` — factory for project-scoped searches
  - Lombok `@Data`

- `dto/TaskSearchCriteria.java` - Service-layer query object for task searches
  - Fields: `keyword`, `status`, `priority`, `overdue`, `userId`, `tagIds`, `projectId`, `projectIds`, `dueDateFrom`, `dueDateTo`
  - Project scoping: single project (`projectId`) or multi-project (`projectIds`, null = admin bypass)
  - Lombok `@Data`

- `dto/ProjectListQuery.java` - Controller-layer binding DTO for project list query parameters (`@ModelAttribute`)
  - Fields: `sort` (default "name"), `showArchived` (default false)

- `dto/SavedViewData.java` - Typed wrapper for saved view persistence; replaces opaque JSON string
  - Fields: `type` (discriminator, "task" for now), `query` (`TaskListQuery`), `view`, `sort` (list of `SortField`)
  - `toJson()`/`fromJson(String)` for entity serialization via Jackson `ObjectMapper`
  - `ofTask(TaskListQuery, String, List<SortField>)` — factory method
  - Inner record `SortField(String field, String direction)`

- `dto/SavedViewRequest.java` - Saved view input DTO (REST API create)
  - Fields: `name` (required, `@NotBlank`, max 100), `data` (required, `@NotNull @Valid SavedViewData`)
  - Record DTO

- `dto/SavedViewResponse.java` - Saved view output DTO
  - Fields: `id`, `name`, `data` (`SavedViewData`)
  - Record DTO; `fromEntity()` deserializes JSON via `SavedViewData.fromJson()`

- `dto/SprintRequest.java` - Form/API binding for sprint CRUD
  - Fields: `name` (required, max 100), `goal` (optional, max 500), `startDate` (required), `endDate` (required)
  - Validation annotations used by `@Valid` in the controller
  - `fromEntity(Sprint)` / `toEntity()` methods
  - Lombok `@Data`

- `dto/SprintResponse.java` - API response DTO for sprints
  - Fields: `id`, `name`, `goal`, `startDate`, `endDate`, `status` (String: "past"/"active"/"future")
  - `fromEntity(Sprint)` static factory
  - Lombok `@Data`

- `dto/RecurringTaskTemplateRequest.java` - Form/API binding for recurring template CRUD
  - Fields: `title` (required), `description`, `priority`, `effort`, `recurrence` (required), `dayOfWeek`, `dayOfMonth`, `dueDaysAfter`, `nextRunDate` (required), `endDate`, `assigneeId`, `tagIds`
  - Lombok `@Data`

- `dto/RecurringTaskTemplateResponse.java` - API response DTO for recurring templates
  - Fields: `id`, `title`, `description`, `priority`, `effort`, `recurrence`, `dayOfWeek`, `dayOfMonth`, `dueDaysAfter`, `nextRunDate`, `endDate`, `enabled`, `lastGeneratedAt`, `createdAt`, `assigneeId`, `assigneeName`, `createdByName`, `tags` (List<TagResponse>)
  - `fromEntity(RecurringTaskTemplate)` static factory
  - Lombok `@Data`

- `dto/RecentViewResponse.java` - Recent view output DTO (record)
  - Fields: `entityType`, `entityId`, `entityTitle`, `href`, `viewedAt` (LocalDateTime), `titleOnly` (boolean)
  - Used for both REST API responses and WebSocket push payloads
  - `titleOnly` flag distinguishes title-sync messages (update in place) from new view entries (prepend)

- `dto/AnalyticsResponse.java` - Analytics API response record with 6 inner records
  - Top-level record: `statusBreakdown`, `priorityBreakdown`, `workloadDistribution`, `burndown` (list), `velocity` (list), `overdueAnalysis`
  - `StatusBreakdown(Map<String, Long> counts)` — task count per status
  - `PriorityBreakdown(Map<String, Long> counts)` — task count per priority
  - `WorkloadDistribution(List<String> assignees, Map<String, List<Long>> statusCounts)` — stacked bar data: status → [count per assignee]
  - `BurndownPoint(LocalDate date, long remaining)` — daily remaining task count
  - `VelocityPoint(LocalDate weekStart, long completed)` — weekly completion count
  - `OverdueAnalysis(List<String> assignees, List<Long> counts)` — overdue tasks per assignee

### Mapper Layer
- `mapper/TaskMapper.java` - MapStruct mapper interface
  - `@Mapper(componentModel = "spring", uses = {TagMapper.class, UserMapper.class})` — auto-discovers nested converters
  - `toResponse(Task)` — MapStruct auto-calls `TagMapper` and `UserMapper` for relationship fields
  - `toResponseList(List<Task>)` — generated automatically
  - `toEntity(TaskRequest)` — `id`, `status`, `createdAt`, `tags`, `user`, `version` explicitly ignored via `Task.FIELD_*` constants (service resolves relationships)
  - Implementation `TaskMapperImpl` generated into `target/generated-sources/` at compile time

- `mapper/TagMapper.java` - MapStruct mapper for Tag ↔ TagResponse; `toResponseList` accepts `Collection<Tag>`

- `mapper/CommentMapper.java` - MapStruct mapper for Comment ↔ CommentResponse
  - `@Mapping(source = "task.id", target = "taskId")` — flattens task association to ID
  - `uses = {UserMapper.class}` — delegates nested user mapping

- `mapper/NotificationMapper.java` - MapStruct mapper for Notification ↔ NotificationResponse
  - `@Mapper(componentModel = "spring")`
  - `@Mapping(source = "actor.name", target = "actorName")` — flattens actor association to name string
  - `toResponse(Notification)`, `toResponseList(List<Notification>)`

- `mapper/UserMapper.java` - MapStruct mapper for User ↔ UserResponse / UserRequest
  - `toEntity(UserRequest)` — `id`, `password`, `role`, `enabled`, `tasks` explicitly ignored via `User.FIELD_*` constants

- `mapper/ProjectMapper.java` - MapStruct mapper for Project ↔ ProjectRequest
  - `toRequest(Project)`, `toEntity(ProjectRequest)` — ignores `id`, `status`, `createdBy`, `createdAt`, `updatedAt`, `members`, `sprints`

- `mapper/SprintMapper.java` - MapStruct mapper for Sprint ↔ SprintRequest / SprintResponse
  - `toResponse(Sprint)` — derived `status` field via `sprintStatus()` default method (past/active/future)
  - `toResponseList(List<Sprint>)`, `toRequest(Sprint)`, `toEntity(SprintRequest)` — ignores `id`, `createdAt`, `updatedAt`, `project`

- `mapper/TaskFormMapper.java` - MapStruct mapper for Task ↔ TaskFormRequest (web form binding)
  - `toRequest(Task)` — flattens `sprint.id` → `sprintId`, `template.title` → `templateName`
  - `toEntity(TaskFormRequest)` — ignores all association/audit fields (service resolves relationships)

- `mapper/RecurringTaskTemplateMapper.java` - MapStruct mapper for RecurringTaskTemplate ↔ Request/Response
  - `uses = {TagMapper.class}` — delegates tag collection mapping
  - `toResponse(RecurringTaskTemplate)` — flattens `assignee.id/name`, `createdBy.name`
  - `toRequest(RecurringTaskTemplate)` — custom `tagIds()` default method extracts IDs from tag set

### Service Layer
- `service/CommentService.java` - Comment business logic with audit and domain event publishing
  - Constructor injection: `CommentRepository`, `TaskQueryService`, `UserService`, `ApplicationEventPublisher`
  - `createComment(text, taskId, userId)` — creates comment, publishes `COMMENT_CREATED` audit event, `CommentAddedEvent` (for notifications), and `CommentChangeEvent` (for WebSocket broadcast)
  - `getSubscriberIds(taskId)` — returns all user IDs subscribed to a task (commenters + @mentioned users)
  - `getCommenterIds(taskId)` — returns user IDs of all distinct commenters on a task
  - `getPreviouslyMentionedUserIds(taskId)` — scans all comment texts for a task via `CommentRepository.findCommentTextsByTaskId()` and extracts `@mention` user IDs via `MentionUtils`; previously-mentioned users get notified on subsequent comments (subscription behavior)
  - `countByUserId(userId)` — count of comments by a user (used by `UserService.canDelete()`)
  - `getCommentById(id)` — single comment lookup
  - `getCommentsByTaskId(taskId)` — chronological comment list for a task
  - `deleteByTaskId(taskId)` — bulk deletes all comments for a task; called by `TaskService.deleteTask()`
  - `deleteComment(id)` — deletes comment, publishes `COMMENT_DELETED` audit event and `CommentChangeEvent`

- `service/TaskQueryService.java` - Read-only task queries and cross-service task operations; `@Transactional(readOnly = true)` class-level
  - Constructor injection: `TaskRepository`
  - Breaks circular dependency: `TaskService` → `UserService`/`CommentService` → `TaskService`
  - All task read methods: `getTaskById`, `getAllTasks`, `getIncompleteTasks`, `searchTasks(criteria, pageable)`, count methods, `getRecentTasksByUser`, `getDueSoon`, `getTasksDueOn`, `getTitlesByIds`, `groupByStatus`
  - `unassignTasks(user)` — sets user to null on all user's tasks, resets non-completed to OPEN; used by `UserService` when disabling/deleting users
  - `unassignTasksInProject(user, projectId)` — unassigns non-terminal tasks for a user within a specific project; used by `ProjectService` when demoting a member to VIEWER

- `service/CommentQueryService.java` - Read-only comment lookups for cross-service use
  - Constructor injection: `CommentRepository`
  - Breaks circular dependency: `CommentService` → `UserService` → `CommentService`
  - `countByUserId(userId)` — used by `UserService.countComments()`

- `service/ProjectQueryService.java` - Read-only project queries; `@Transactional(readOnly = true)` class-level
  - Constructor injection: `ProjectRepository`, `ProjectMemberRepository`
  - `getProjectById(Long)` — throws `EntityNotFoundException` if not found
  - `getActiveProjects()` — returns ACTIVE projects sorted by name
  - `getAdminProjects(boolean includeArchived, String sort)` — consolidated admin project listing
  - `getProjectsForUser(Long userId)` — active projects where user is a member
  - `getProjectsForUser(Long userId, boolean includeArchived, String sort)` — user's projects with sort/archive filter
  - `getAccessibleProjectIds(Long userId)` — returns IDs of active projects for a user; used by controllers for project-scoped queries
  - `getEditableProjectsForUser(Long userId)` — returns active projects where user is EDITOR or OWNER
  - Member queries: `getMembers(Long)` (returns `Set<ProjectMember>`), `isMember`, `getMemberRole`, `isOwner`, `isEditor` — used by `ProjectAccessGuard`

- `service/ProjectService.java` - Project write operations with audit event publishing
  - Constructor injection: `ProjectRepository`, `ProjectQueryService`, `UserService`, `TaskQueryService`, `ApplicationEventPublisher`, `Messages`
  - `createProject(Project, User)` — creator becomes OWNER via cascaded `ProjectMember`; publishes `PROJECT_CREATED` audit event
  - `updateProject(Long, Project)` — updates name/description with diff tracking; publishes `PROJECT_UPDATED` audit event and `ProjectUpdatedEvent` (with actor) if changed
  - `archiveProject(Long)` — sets status to ARCHIVED; publishes `PROJECT_ARCHIVED`
  - `unarchiveProject(Long)` — restores to ACTIVE; publishes `PROJECT_UNARCHIVED`
  - `deleteProject(Long)` — only if no COMPLETED tasks (cancelled tasks don't block); publishes `PROJECT_DELETED`
  - Member management: `addMember` (rejects duplicates), `removeMember` (prevents removing last OWNER), `updateMemberRole` (prevents demoting last OWNER, no-op if same role; demoting to VIEWER unassigns non-terminal tasks in the project)

- `service/TaskService.java` - Write-only task operations with audit and domain event publishing
  - Constructor injection: `TaskRepository`, `TaskQueryService`, `TagService`, `UserService`, `ApplicationEventPublisher`, `Messages`
  - `createTask(task, tagIds, assigneeId)` and `createTask(task, tagIds, assigneeId, checklistTexts, checklistChecked)` — validates task has a project; publishes `TaskAssignedEvent` and `TaskChangeEvent("created")`
  - `updateTask` — two overloads (with/without checklist); publishes `TaskAssignedEvent` (if assignment changed), `TaskUpdatedEvent` (if fields changed), and `TaskChangeEvent("updated")`
  - `advanceStatus(id)` — cycles BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED → OPEN; CANCELLED → OPEN; publishes `TaskUpdatedEvent` and `TaskChangeEvent("updated")`
  - `setStatus(id, TaskStatus)` — sets status directly (for kanban drop); publishes `TaskUpdatedEvent` and `TaskChangeEvent("updated")`
  - `updateField(id, fieldName, value)` — updates a single named field (title, description, priority, status, dueDate) in-place; used by inline editing in table view; publishes `TaskUpdatedEvent` and `TaskChangeEvent("updated")`
  - `groupByStatus(List<Task>)` — groups a list of tasks into a `Map<TaskStatus, List<Task>>`; preserves all statuses (empty list for statuses with no tasks); used by the kanban board view
  - `deleteTask` — blocks deletion of COMPLETED tasks; publishes `TaskChangeEvent("deleted")`
  - `updateTask` — reassigning an IN_PROGRESS task to a different user resets status to OPEN (new assignee hasn't started)
  - `getIncompleteTasks()` — uses `findByStatusNotIn(terminalStatuses())` instead of single-status exclusion

- `service/TaskDependencyService.java` - Dependency management with cycle detection and validation
  - Constructor injection: `TaskQueryService`
  - `reconcileBlockedBy(task, blockedByIds)` — replaces `blockedBy` set; validates same-project, no self-reference, no cycles
  - `reconcileBlocks(task, blocksIds)` — replaces `blocks` set; validates same-project, no self-reference, no cycles
  - `getActiveBlockers(task)` — returns non-terminal tasks from `blockedBy` set
  - `hasActiveBlockers(task)` — returns true if any non-terminal task blocks this one
  - `wouldCreateCycle(source, target)` — BFS cycle detection; returns true if adding source→target would create a cycle

- `service/UserService.java` - User business logic
  - Constructor injection: `UserRepository`, `TaskQueryService`, `CommentQueryService`, `ApplicationEventPublisher`
  - `getAllUsers`, `getUserById`, `findUserById`, `findByEmail`, `searchUsers`, `getEnabledUsers`, `searchEnabledUsers`, `createUser`, `updateUser`, `updateProfile`, `changePassword`, `updateRole`, `deleteUser`, `disableUser`, `enableUser`, `canDelete`, `countCompletedTasks`, `countComments`, `countAssignedTasks`
  - `findUserById(Long id)` — returns null if id is null or not found (vs `getUserById` which throws `EntityNotFoundException`); used by `TaskService` for user resolution
  - `searchUsers(String query)` — returns all users if query is blank, otherwise searches by name or email (case-insensitive substring); used by admin user management
  - `getEnabledUsers()` / `searchEnabledUsers(query)` — only enabled users; used by public user list, API, and assignment dropdowns (hides disabled users)
  - `findByEmail(String)` — returns `Optional<User>`; used by `CustomUserDetailsService`
  - `updateUser(userId, name, email, role)` — updates user fields; publishes `USER_UPDATED` audit event
  - `updateProfile(userId, name, email)` — self-service profile update; publishes `PROFILE_UPDATED` audit event
  - `changePassword(userId, encodedPassword)` — sets pre-encoded password; publishes `PROFILE_PASSWORD_CHANGED` audit event
  - `updateRole(Long userId, Role role)` — loads user, sets role, saves; publishes `USER_ROLE_CHANGED` audit event
  - `canDelete(userId)` — true if user has no completed tasks and no comments (safe to hard-delete)
  - `disableUser(userId)` — sets `enabled = false`, unassigns open/in-progress tasks (resets to OPEN); publishes `USER_DISABLED`
  - `enableUser(userId)` — sets `enabled = true`; publishes `USER_ENABLED`
  - `deleteUser` — unassigns all tasks (via `TaskQueryService.unassignTasks()`), then deletes user; prevents FK constraint failure

- `service/TagService.java` - Tag business logic with audit event publishing
  - `getAllTags`, `getTagById`, `findAllByIds(List<Long>)`, `countTasksByTagId`, `createTag`, `deleteTag`
  - `findAllByIds(ids)` — returns `Set<Tag>` matching the given IDs; returns empty set for null/empty input; used by `TaskService` for tag resolution
  - `countTasksByTagId(tagId)` — uses ORM relationship traversal (`getTagById(tagId).getTasks().size()`) instead of custom repository query

- `audit/AuditLogService.java` - Audit log business logic
  - `searchAuditLogs(category, search, from, to, pageable)` — paginated search with JPA Specifications
  - `getEntityHistory(Class<?>, entityId)` — entity-specific audit trail; accepts entity class (uses `getSimpleName()` for DB lookup); used by `TimelineService` and task detail/modal
  - `getRecentByActions(List<String>)` — top 10 entries filtered by action type (used by dashboard activity feed)
  - `searchAuditLogs` and `getEntityHistory` resolve field display names via `AuditDetails.resolveDisplayNames()` before returning

- `service/NotificationService.java` - Notification business logic with WebSocket push
  - Constructor injection: `NotificationRepository`, `NotificationMapper`, `SimpMessagingTemplate`
  - `create(recipient, actor, type, message, link)` — `@Transactional`; saves to DB then pushes to recipient via `convertAndSendToUser(email, "/queue/notifications", payload)`
  - `getUnreadCount(userId)` — count of unread notifications for badge
  - `getRecentForUser(userId)` — top 10 most recent (for dropdown)
  - `findAllForUser(userId, pageable)` — paginated list (for full page)
  - `markAsRead(id, userId)` — marks single notification as read (ownership-scoped)
  - `markAllAsRead(userId)` — bulk mark all as read
  - `clearAll(userId)` — deletes all notifications for user

- `service/UserPreferenceService.java` - Per-user preference business logic
  - Constructor injection: `UserPreferenceRepository`, `UserService`
  - `load(Long userId)` — reads all DB rows for a user into a `UserPreferences` POJO via `BeanWrapper`; missing keys keep field defaults (mirrors `SettingService.load()` pattern)
  - `save(Long userId, String key, String value)` — creates or updates a single preference (upsert)
  - `saveAll(Long userId, Map<String, String>)` — saves multiple preferences at once

- `service/SettingService.java` - Setting persistence with audit event publishing
  - `load()` — reads all DB rows into a `Settings` POJO via `BeanWrapper`; missing keys keep field defaults
  - `updateValue(key, value)` — upserts a setting row; publishes `AuditEvent` with before/after diff
  - Used by `GlobalModelAttributes` (load) and `SettingsController` (update)

- `service/TimelineService.java` - Merges comments and audit history into chronological timeline
  - Constructor injection: `CommentService`, `AuditLogService`
  - `getTimeline(taskId, currentUser)` — fetches comments and audit entries for a task, converts to `TimelineEntry` records, sorts by timestamp descending
  - Computes `canDelete` per comment entry using `AuthExpressions.isAdmin()` and owner check
  - Used by `TaskController` to populate the activity panel on task detail/modal pages

- `report/TaskReport.java` - `@Service` for task CSV export
  - Constructor injection: `Messages`
  - `exportCsv(HttpServletResponse, String filename, List<Task>)` — writes CSV to the response; uses `Messages.get(Translatable)` for translated column headers and enum values (priority, status)
  - Used by both `TaskController` (cross-project export at `GET /tasks/export`) and `ProjectController` (per-project export at `GET /projects/{id}/export`); replaces the inline `CsvWriter` call that was previously only in `TaskController`

- `service/ScheduledTaskService.java` - Centralized home for all `@Scheduled` jobs
  - Constructor injection: `TaskQueryService`, `NotificationService`, `NotificationRepository`, `RecurringTaskGenerationService`, `UserPreferenceService`, `SettingService`, `Messages`
  - `generateRecurringTasks()` — `@Scheduled(cron = "0 0 6 * * *")` `@Transactional`; generates tasks from due recurring templates (runs before due reminders)
  - `sendDueReminders()` — `@Scheduled(cron = "0 0 8 * * *")`; finds tasks due tomorrow, sends `TASK_DUE_REMINDER` notifications to assigned users who have the `dueReminder` preference enabled
  - `purgeOldNotifications()` — `@Scheduled(cron = "0 0 3 * * *")` `@Transactional`; reads `notificationPurgeDays` from `Settings`, deletes notifications older than that

- `service/SavedViewService.java` - Saved view CRUD; `@Transactional` class-level
  - Constructor injection: `SavedViewRepository`
  - `getViewsForUser(Long userId)` — returns all saved views for a user, sorted by name ascending
  - `createView(User, String name, SavedViewData data)` — persists a new saved view; calls `data.toJson()` for entity storage
  - `getViewById(Long id)` — returns view or throws `EntityNotFoundException`
  - `deleteView(SavedView)` — deletes view entity (caller must have already checked ownership via `OwnershipGuard`)

- `service/SprintService.java` - Command service for sprint lifecycle
  - Constructor injection: `SprintRepository`, `TaskRepository`, `SprintQueryService`, `ProjectQueryService`, `ApplicationEventPublisher`, `Messages`
  - `createSprint(Long projectId, Sprint)` — validates dates and no overlap, persists sprint; publishes audit event
  - `updateSprint(Long id, Sprint)` — validates dates and no overlap; publishes audit event
  - `clearSprintAssignments(Long projectId)` — nullifies sprint FK on all tasks in a project (used when disabling sprints)
  - `deleteSprint(Long)` — nullifies sprint FK on associated tasks before deleting; publishes audit event

- `service/SprintQueryService.java` - Read-only query service; `@Transactional(readOnly = true)` class-level
  - Constructor injection: `SprintRepository`
  - `getSprintById(Long)` — throws `EntityNotFoundException` if not found
  - `getSprintsByProject(Long projectId)` — all sprints for a project, newest first
  - `getActiveSprint(Long projectId)` — returns `Optional<Sprint>` for sprint whose date range contains today

- `service/RecurringTaskTemplateService.java` - Command service for recurring template CRUD; `@Transactional` class-level
  - Constructor injection: `RecurringTaskTemplateRepository`, `ProjectQueryService`, `TagService`, `UserService`, `ApplicationEventPublisher`, `Messages`
  - `getTemplatesByProject(Long)` — list templates (read-only)
  - `getTemplateById(Long)` — with eager-loaded details
  - `createTemplate(Long projectId, RecurringTaskTemplateRequest)` — validates non-sprint project and end date; publishes audit event
  - `updateTemplate(Long id, RecurringTaskTemplateRequest)` — validates end date; publishes audit event
  - `toggleEnabled(Long)` — flips enabled flag; publishes audit event
  - `deleteTemplate(Long)` — deletes template; publishes audit event

- `service/RecurringTaskGenerationService.java` - Generates tasks from due recurring templates
  - Constructor injection: `RecurringTaskTemplateRepository`, `TaskRepository`, `ApplicationEventPublisher`
  - `generateDueTasks()` — finds all due templates, creates tasks, advances `nextRunDate` to next future date (skips missed), auto-disables when end date reached
  - Silently skips assignee if user is disabled; logs errors per template without failing the batch

- `service/RecentViewService.java` - Recently viewed items with WebSocket push
  - Constructor injection: `RecentViewRepository`, `SimpMessagingTemplate`, `AppRoutesProperties`
  - `recordView(User, String entityType, Long entityId, String title)` — `@Transactional`; upserts recent view (updates viewedAt if exists, creates if not), trims to 10 per user, pushes `RecentViewResponse` via WebSocket to `/queue/recent-views`
  - `updateTitle(String entityType, Long entityId, String newTitle)` — `@Transactional(propagation = REQUIRES_NEW)`; bulk-updates title across all users' recent views, pushes `titleOnly` response to affected users via WebSocket
  - `deleteByEntity(String entityType, Long entityId)` — cleanup on entity delete
  - `deleteByUserId(Long)` — cleanup on user delete
  - `getRecentViews(Long userId)` — returns top 10 recent views as `RecentViewResponse` list

- `service/AnalyticsService.java` - Analytics chart data builder
  - `@Transactional(readOnly = true)`, constructor injection: `TaskRepository`, `AnalyticsRepository`, `UserService`, `Messages`
  - `getProjectAnalytics(Long projectId)` — single-project analytics
  - `getCrossProjectAnalytics(List<Long> accessibleProjectIds)` — cross-project; null = admin (all projects)
  - Private builders: `buildStatusBreakdown` (spec-based counts per status), `buildPriorityBreakdown` (spec-based counts per priority), `buildWorkloadDistribution` (grouped by user + status via `AnalyticsRepository`), `buildBurndown` (30-day rolling: initial open + daily created − daily completed), `buildVelocity` (12-week completed per ISO week, includes effort-based velocity), `buildOverdueAnalysis` (overdue grouped by assignee), `buildEffortDistribution` (total effort by assignee)
  - `projectScope()` helper returns `Specification` — `cb.conjunction()` for no-filter case

- `service/DashboardService.java` - Orchestrates dashboard data via owning services
  - Constructor injection: `TaskQueryService`, `ProjectQueryService`, `AuditLogService`, `PresenceService` (follows service-to-service convention — no direct repository access)
  - `buildStats(User, List<Long> accessibleProjectIds)` — returns `DashboardStats` record; `accessibleProjectIds` null = admin (show all), non-null = scoped to user's projects
  - Builds per-project `ProjectSummary` cards via `buildProjectSummary()` helper using single-project count methods
  - System stats (totalTasks, onlineCount, etc.) only populated for admins; null/zero for regular users
  - Personal stats always unscoped (user's own tasks across all projects); includes In Review count
  - Filters activity to `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED` actions only

### Controller Layer
- `controller/api/TaskApiController.java` - Task REST API endpoints
  - `@RestController` with `/api/tasks` base path
  - Constructor injection: `TaskService`, `TaskQueryService`, `ProjectQueryService`, `TaskMapper`, `ProjectAccessGuard`
  - Standard HTTP methods: GET, POST, PUT, PATCH, DELETE
  - GET `/api/tasks` — paginated with filters, scoped to accessible projects via `projectService.getAccessibleProjectIds()`; admin sees all (null bypass)
  - Accepts `TaskRequest` (includes `tagIds`, `userId`, `projectId`), returns `TaskResponse` — no raw entity exposure
  - Injects `TaskMapper` for all DTO ↔ entity conversion
  - **Security**: injects `ProjectAccessGuard`; uses `@AuthenticationPrincipal CustomUserDetails` on all mutating methods
  - POST: requires `projectId`; checks edit access to project; auto-assigns task to caller; admins can override via `request.getUserId()`
  - PUT: calls `projectAccessGuard.requireEditAccess()` on task's project
  - DELETE: three-way check via `requireDeleteAccess()` — admin OR task creator OR project owner
  - PATCH `/api/tasks/{id}/toggle` — advance status; checks edit access to task's project
  - GET `/api/tasks/search-for-dependency` — search for tasks within a project, excluding specified task IDs; returns id, title, status

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

- `controller/api/PresenceApiController.java` - Presence REST API
  - `@RestController`
  - `GET /api/presence` — returns `PresenceResponse` record (`{ users: [...], count: N }`); needed because `SessionConnectEvent` fires before client subscription completes

- `controller/api/NotificationApiController.java` - Notification REST API endpoints
  - `@RestController` with `/api/notifications` base path
  - `GET /api/notifications/unread-count` — returns `{ count: N }`
  - `GET /api/notifications?page=0&size=10` — paginated notification list
  - `PATCH /api/notifications/{id}/read` — mark single as read (204)
  - `PATCH /api/notifications/read-all` — mark all as read (204)
  - `DELETE /api/notifications` — clear all (204)
  - All endpoints scoped to current user via `@AuthenticationPrincipal`

- `controller/api/ProjectApiController.java` - Project REST API endpoints
  - `@RestController` with `/api/projects` base path
  - Constructor injection: `ProjectQueryService`, `UserMapper`, `AnalyticsService`, `ProjectAccessGuard`
  - `GET /api/projects/{id}/members` — all enabled members of a project (returns `List<UserResponse>`)
  - `GET /api/projects/{id}/members/assignable` — editors and owners only, excludes VIEWERs (for task assignment dropdowns)
  - `GET /api/projects/{id}/analytics` — project-scoped analytics data; requires view access via `ProjectAccessGuard`

- `controller/api/SprintApiController.java` - Sprint REST API endpoints
  - `@RestController` with `/api/projects/{projectId}/sprints` base path
  - Constructor injection: `SprintService`, `SprintQueryService`, `ProjectAccessGuard`
  - `GET /api/projects/{projectId}/sprints` — list all sprints for a project
  - `POST /api/projects/{projectId}/sprints` — create sprint; requires edit access
  - `PUT /api/projects/{projectId}/sprints/{id}` — update sprint; requires edit access
  - `DELETE /api/projects/{projectId}/sprints/{id}` — delete sprint (nullifies task FKs); requires edit access
  - **Security**: uses `ProjectAccessGuard` for all mutating operations; `@AuthenticationPrincipal CustomUserDetails` on all endpoints

- `controller/api/RecurringTaskTemplateApiController.java` - Recurring task template REST API
  - `@RestController` with `/api/projects/{projectId}/recurring-templates` base path
  - Constructor injection: `RecurringTaskTemplateService`, `ProjectAccessGuard`
  - `GET` — list templates for a project
  - `GET /{id}` — get template details
  - `POST` — create template; requires edit access
  - `PUT /{id}` — update template; requires edit access
  - `POST /{id}/toggle` — toggle enabled/disabled; requires edit access
  - `DELETE /{id}` — delete template; requires edit access

- `controller/api/RecentViewApiController.java` - Recent views REST API
  - `@RestController` with `/api/recent-views` base path
  - `GET /api/recent-views` — returns current user's recent views as `List<RecentViewResponse>`; used by JS for initial load on WebSocket connect

- `controller/api/AnalyticsApiController.java` - Cross-project analytics REST API
  - `@RestController` with `/api/analytics` base path
  - Constructor injection: `AnalyticsService`, `ProjectQueryService`
  - `GET /api/analytics` — cross-project analytics; optional `projectIds` query param for filtering
  - **Security**: intersects requested `projectIds` with user's accessible projects; admin can filter to any projects

- `controller/api/SavedViewController.java` - Saved views REST API
  - `@RestController` with `/api/views` base path
  - Constructor injection: `SavedViewService`, `OwnershipGuard`
  - `GET /api/views` — returns all saved views for the current user (name + filters JSON)
  - `POST /api/views` (201) — creates a new saved view for the current user; accepts `SavedViewRequest`, returns `SavedViewResponse`
  - `DELETE /api/views/{id}` (204) — deletes a saved view; owner or admin only via `OwnershipGuard.requireAccess()`

- `controller/NotificationController.java` - Notifications web page
  - `@Controller` with `/notifications` base path
  - `GET /notifications` — paginated notification list page (default 25 per page)
  - Uses `@AuthenticationPrincipal` to scope to current user

- `controller/HomeController.java` - Home page
  - `@Controller` — single `GET /` mapping, returns `"home"` template

- `controller/LoginController.java` - Login page
  - `@Controller` — single `GET /login` mapping, returns `"login"` template
  - Spring Security handles `POST /login` automatically via `UsernamePasswordAuthenticationFilter`

- `controller/RegistrationController.java` - User self-registration
  - `GET /register` — serves registration form with empty `RegistrationRequest`
  - `POST /register` — validates form (email uniqueness via `@Unique` on DTO), checks password match, creates user with `Role.USER`
  - Encodes password via `PasswordEncoder.encode()` before persisting
  - Redirects to `/login?registered` on success

- `controller/admin/TagManagementController.java` - Admin tag management
  - `@Controller` with `/admin/tags` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `GET /admin/tags` — lists all tags with task counts
  - `POST /admin/tags` — creates tag via `@Valid @ModelAttribute TagRequest`; HTMX-aware (returns fragment or full page)
  - `DELETE /admin/tags/{id}` — deletes tag; triggers `tagDeleted` HX-Trigger event
  - `populateModel()` — adds tags list and task counts map to model

- `controller/admin/UserManagementController.java` - Admin user management (modal-based)
  - `@Controller` with `/admin/users` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `GET /admin/users` — lists all users (HTMX-aware: returns `user-table` fragment or full `users` page)
  - `GET /admin/users/new` — returns empty modal form for creating a user
  - `GET /admin/users/{id}/edit` — returns pre-filled modal form for editing a user
  - `POST /admin/users` — creates user; `Object` return type (view name on validation error, `ResponseEntity` on success); password validated manually (required on create)
  - `PUT /admin/users/{id}` — updates user name, email, role; triggers `userSaved` event
  - `DELETE /admin/users/{id}` — deletes user (only if `canDelete`); triggers `userSaved` event
  - `POST /admin/users/{id}/disable` — disables user; triggers `userSaved` event
  - `POST /admin/users/{id}/enable` — enables user; triggers `userSaved` event
  - `GET /admin/users/{id}/info` — returns JSON with `name`, `canDelete`, `completedTasks`, `comments`, `assignedTasks` (used by JS confirmation dialog)

- `controller/admin/SettingsController.java` - Admin settings page (theme, site name, registration, maintenance banner)
  - `@Controller` with `/admin/settings` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `ThemeOption` record — holds theme id and preview swatch colors
  - `THEMES` list — single source of truth for valid themes; used for both rendering and validation
  - `GET /admin/settings` — settings page with theme picker
  - `POST /admin/settings/general` — saves site name, registration toggle, maintenance banner, notification purge days; triggers `settingsSaved` event
  - `POST /admin/settings/theme` — validates theme against `THEMES` list (400 if invalid), saves; triggers `themeSaved` event with theme id

- `controller/admin/AuditController.java` - Audit log page
  - `@Controller` with `/admin/audit` base path; secured via `SecurityConfig` (`hasRole(ADMIN)`)
  - `GET /admin/audit` — paginated audit log with category, search, and date range filters
  - Params: `category` (Task/User/Profile/Comment/Tag/Auth/Setting), `search` (principal/details text), `from`/`to` (LocalDate → Instant)
  - HTMX requests → `"admin/audit-table"` (bare fragment); full requests → `"admin/audit"`

- `controller/TagController.java` - Tag web UI
  - `@Controller` with `/tags` base path
  - `GET /tags` — lists all tags sorted A-Z in a table; tag names link to `/tasks?tags={id}&userId=` (all users)

- `controller/UserController.java` - User web UI
  - `@Controller` with `/users` base path
  - `GET /users` — lists all users sorted A-Z in a table with HTMX live search (name/email)
  - User names link to `/tasks?userId={id}` to show that user's tasks
  - HTMX requests return `users/user-table` fragment; full requests return `users/users`

- `controller/ProfileController.java` - User self-service profile page
  - `@Controller` with `/profile` base path
  - `GET /profile` — shows profile page with account form (`ProfileRequest`), password form (`ChangePasswordRequest`), and preferences
  - `POST /profile` — updates name and email; validates via `@Valid` on `ProfileRequest` (includes `@Unique` email check); updates `SecurityContext` with new values
  - `POST /profile/password` — changes password; verifies current password, checks new/confirm match, encodes via `PasswordEncoder`; updates `SecurityContext` to keep session valid
  - `POST /profile/preferences` — saves task view, default user filter, and due reminder preferences via `UserPreferenceService`
  - Flash attributes trigger toast notifications on redirect

- `controller/AnalyticsController.java` - Cross-project analytics web page
  - `@Controller` with `/analytics` base path
  - Constructor injection: `ProjectQueryService`
  - `GET /analytics` — analytics page; admin sees all active projects, regular users see their projects
  - Passes `apiUrl` (`/api/analytics`) and `projects` list to template

- `controller/ProjectController.java` - Project web UI endpoints
  - `@Controller` with `/projects` base path
  - Constructor injection: `ProjectService`, `ProjectQueryService`, `TaskQueryService`, `TagService`, `UserService`, `ProjectAccessGuard`, `TaskReport`, `RecentViewService`
  - `GET /projects` — list projects via `@ModelAttribute ProjectListQuery`; admin sees all (with sort and archived toggle); users see their projects; HTMX-aware (returns `project-grid :: grid` fragment)
  - `GET /projects/new` — create form (returns `project-form` template)
  - `POST /projects` — create project; creator becomes OWNER; HTMX-aware (triggers `projectSaved`)
  - `GET /projects/{id}` — project home with full task filtering (search, status, priority, user, tags, overdue); uses `projectAccessGuard.requireViewAccess()`; resolves `canEditProject` and `isProjectOwner` for template
  - `GET /projects/{id}/settings` — project settings page with member management (owner/admin only via `requireOwnerAccess`)
  - `POST /projects/{id}` — update project name/description (owner/admin only)
  - `POST /projects/{id}/archive` — archive project (owner/admin only)
  - `POST /projects/{id}/unarchive` — restore archived project (owner/admin only)
  - Member management: `POST /{id}/members` (add), `PATCH /{id}/members/{userId}/role` (change role), `DELETE /{id}/members/{userId}` (remove) — all return `member-table` fragment
  - Task filtering scoped to single project via `searchAndFilterTasksForProject(projectId, ...)`
  - Supports cards, table, calendar, and board view modes with user preferences
  - Board view: when `view=board`, groups tasks by status via `TaskService.groupByStatus()` and returns `task-board.html` (no pagination in board view)
  - `GET /projects/{id}/analytics` — project-scoped analytics page; requires view access; passes project and API URL to shared analytics template
  - `GET /projects/{id}/export` — per-project CSV download of filtered tasks; delegates to `TaskReport.exportCsv()`
  - `buildCalendarWeeks()` — private helper for project-scoped calendar view

- `controller/DashboardController.java` - Dashboard page and HTMX stats fragment
  - Uses `@AuthenticationPrincipal CustomUserDetails` for reliable user resolution
  - `GET /dashboard` — full dashboard page; resolves `accessibleProjectIds` and passes to `DashboardService.buildStats()`
  - `GET /dashboard/stats` — returns `dashboard/dashboard-stats` bare fragment for HTMX refresh; resolves project scoping

- `controller/TaskController.java` - Task web UI endpoints (cross-project task views)
  - `@Controller` with `/tasks` base path
  - Constructor injection: `TaskService`, `TaskQueryService`, `TaskDependencyService`, `ProjectQueryService`, `TagService`, `UserService`, `CommentService`, `TimelineService`, `RecentViewService`, `OwnershipGuard`, `ProjectAccessGuard`, `TaskReport`, `Messages`
  - Returns Thymeleaf template names or fragment selectors
  - HTMX support: detects `HX-Request` header via `HtmxUtils.isHtmxRequest()`
  - `Object` return type on POST methods to allow returning either a String view name or `ResponseEntity`
  - Fires `HX-Trigger` events (`taskSaved`, `taskDeleted`) via `HtmxUtils.triggerEvent()`
  - Task list defaults based on `userPreferences.defaultUserFilter` (from `GlobalModelAttributes`); view mode defaults based on `userPreferences.taskView`; URL params override preferences
  - Three view modes: cards, table, calendar — resolved via URL `view` param or user preference
  - Calendar view: accepts `month` param (YearMonth), queries tasks with dates in visible grid range (unpaged), builds `CalendarDay` grid via `buildCalendarWeeks()`; no pagination
  - `GET /tasks/{id}` — show task in view (read-only) mode; supports HTMX modal
  - `GET /tasks/new` — optional `projectId` param; if provided, checks edit access and pre-selects project; if omitted, adds `editableProjects` list for project dropdown; accepts optional `dueDate` param (ISO date) to pre-fill
  - `POST /tasks` — create task; requires `projectId`; accepts `TaskFormRequest` + separate `@RequestParam` for `tagIds`, `assigneeId`, checklist arrays; on validation error re-render, adds `editableProjects` to model
  - `GET /tasks/{id}/edit` — edit form; checks `projectAccessGuard.requireEditAccess()`
  - `POST /tasks/{id}` — update task; checks edit access; accepts `TaskFormRequest`; dependency management via form params `blockedByIds` and `blocksIds`
  - `DELETE /tasks/{id}` — delete via `requireDeleteAccess()`: admin OR task creator OR project owner
  - `GET /tasks/{id}/activity` — activity timeline fragment (HTMX live refresh via WebSocket)
  - `POST /{id}/comments` — add comment to task; returns `task-activity` template (whole file for hx-swap-oob count updates)
  - `DELETE /{id}/comments/{commentId}` — delete comment (owner or admin); returns `task-activity` template
  - `POST /tasks/{id}/toggle` — advance status; checks edit access; returns card/row/trigger based on view mode
  - `GET /tasks/export` — CSV download of filtered tasks (same filter params as `listTasks`, unpaged); delegates to `TaskReport.exportCsv()`; works independently of view mode
  - Board view: when `view=board`, groups tasks by status via `TaskService.groupByStatus()` and returns `task-board.html` (no pagination in board view)
  - `PATCH /tasks/{id}/field` — inline field edit endpoint; accepts `fieldName` + `value` params, delegates to `TaskService.updateField()`; returns updated card or row fragment for the active view
  - `POST /tasks/{id}/status` — kanban drop endpoint; accepts `status` param, delegates to `TaskService.setStatus()`; returns 200 on success
  - `POST /tasks/bulk` — bulk action endpoint; `@ResponseBody` returns JSON; accepts `BulkTaskRequest` (taskIds, action, value); validates edit access per project (cached), delete access per task; loops over existing service methods for proper audit/event publishing; actions: STATUS, PRIORITY, ASSIGN, EFFORT, DELETE
  - Task list is scoped to accessible projects via `searchAndFilterTasksForProjects(accessibleProjectIds, ...)`; admin sees all (null bypass)
  - `addProjectEditPermissions()` — builds `projectEditMap` (Map<Long, Boolean>) for cross-project views; admin short-circuits to `canEditProject=true`
  - `addEditableProjects()` — private helper; adds `editableProjects` list to model (admin gets all active projects, regular users get EDITOR/OWNER projects); used by task list, create form, and validation error re-render
  - Resolves `filterUserName` when filtering by another user's ID (passed to template for user filter button label)
  - **Security**: uses `ProjectAccessGuard` for task create/edit/delete/toggle; `OwnershipGuard` for comment delete

- `controller/FrontendConfigController.java` - Serves `/config.js` (JS runtime config)
  - `@RestController` producing `application/javascript`
  - Emits `RouteTemplate.JS_CLASS` + `window.APP_CONFIG = { routes: { ... }, messages: { ... } };`
  - Routes auto-discovered via reflection over `AppRoutesProperties` fields; emitted as `new Route("template")` JS expressions
  - Messages serialized via Jackson `ObjectMapper` from `ResourceBundle`
  - Loaded by the `scripts` fragment on every page; `APP_CONFIG` is available globally to all page scripts
  - NOTE: Uses JVM default locale; for i18n, would need `MessageSource` with request `Locale` (conflicts with content-hash caching)

### Security Layer
- `security/CustomUserDetails.java` - Implements Spring Security's `UserDetails`
  - Wraps the `User` entity; exposes it via `getUser()` for controllers and templates
  - `getUsername()` returns `user.getEmail()` (email is the login identifier)
  - `getAuthorities()` returns single authority: `ROLE_USER` or `ROLE_ADMIN`
  - `isEnabled()` returns `user.isEnabled()` — disabled users cannot log in
  - Other account status methods return `true` (no expiry/lock features yet)

- `security/CustomUserDetailsService.java` - Implements Spring Security's `UserDetailsService`
  - `loadUserByUsername(String email)` — looks up user via `UserRepository.findByEmail()`
  - Throws `UsernameNotFoundException` if not found
  - Wraps result in `CustomUserDetails`

- `security/ProjectAccessGuard.java` - Project-scoped access control component
  - Constructor injection: `ProjectQueryService`
  - `requireViewAccess(projectId, currentDetails)` — throws `AccessDeniedException` unless member or admin
  - `requireEditAccess(projectId, currentDetails)` — throws unless EDITOR/OWNER or admin
  - `requireOwnerAccess(projectId, currentDetails)` — throws unless OWNER or admin
  - Used by `ProjectController` and `TaskApiController` for project-scoped security

- `security/OwnershipGuard.java` - Reusable access control component
  - `requireAccess(OwnedEntity entity, CustomUserDetails currentDetails)` — throws `AccessDeniedException` if caller is neither admin nor owner
  - Does NOT handle unassigned entities — callers should check `entity.getUser() == null` before calling if unassigned entities should be open
  - Used by `CommentApiController` (web) for comment ownership checks

- `security/AuthExpressions.java` - Ownership and role check logic (shared between templates and Java)
  - Exposed as `${#auth}` in Thymeleaf templates via `AuthDialect`
  - Instance methods (template use): `isOwner(OwnedEntity)`, `isAdmin()`, `canEdit(OwnedEntity)` (admin OR owner)
  - Static methods (Java use): `isOwner(User, OwnedEntity)`, `isAdmin(User)` — reused by `OwnershipGuard`
  - Unassigned entities (`entity.getUser() == null`): `isOwner()` and `canEdit()` return false — business rules for unassigned entities belong in the controller/template, not here

- `security/AuthDialect.java` - Thymeleaf `IExpressionObjectDialect` implementation
  - Registers `${#auth}` expression object, built per-request from `SecurityContextHolder`
  - Auto-discovered by Spring Boot; no manual configuration needed

- `security/SecurityUtils.java` - Central utility for resolving the current authenticated user
  - `getCurrentPrincipal()` — returns username from `SecurityContextHolder` or `"system"` if unauthenticated; used by services for audit events
  - `getCurrentUser()` — returns the `User` entity from `SecurityContextHolder`, or null if unauthenticated
  - `getCurrentUserDetails()` — returns `CustomUserDetails` from `SecurityContextHolder`, or null
  - `getUserFrom(Principal)` — extracts `User` from an arbitrary `Principal` (e.g., WebSocket session events); returns null if not a `CustomUserDetails`
  - All user-resolution logic centralized here — controllers, services, template dialects, and WebSocket listeners delegate to these methods

### Validation
- `validation/Unique.java` - Generic class-level validation annotation for field uniqueness
  - `@Constraint(validatedBy = UniqueValidator.class)`, `@Target(ElementType.TYPE)`, `@Repeatable(Unique.List.class)`
  - Attributes: `entity` (JPA entity class), `field` (field to check), `idField` (defaults to `"id"` — reads from validated object to exclude self on update)
  - Lives on DTOs only (not entities) — Spring MVC `@Valid` has full Spring DI; Hibernate validation does not

- `validation/UniqueValidator.java` - `ConstraintValidator<Unique, Object>` implementation
  - Constructor injection: `EntityManager` (Spring DI — only works with Spring MVC `@Valid`, not Hibernate pre-insert validation)
  - Uses `BeanWrapper` to read field value and optional ID from the validated object
  - JPQL query: `SELECT COUNT(e) FROM Entity e WHERE LOWER(e.field) = LOWER(:value)` (case-insensitive)
  - If ID is present and non-null (edit), adds `AND e.id != :excludeId` to exclude self
  - Binds error to specific field via `addPropertyNode(field)` for per-field error display

### Configuration
- `config/GlobalBindingConfig.java` - `@ControllerAdvice` for global form string trimming
  - `@InitBinder` registers `StringTrimmerEditor(true)` — trims all form-bound strings, converts blank to null
  - Applies to `@ModelAttribute`, `@RequestParam`, `@PathVariable` — NOT `@RequestBody` (JSON)
  - Eliminates manual `.trim()` calls across all controllers; `@NotBlank` catches null values

- `config/WebSocketConfig.java` - WebSocket/STOMP configuration
  - `@EnableWebSocketMessageBroker`
  - Simple broker on `/topic` (broadcast) and `/queue` (user-specific)
  - Application destination prefix: `/app`
  - STOMP endpoint: `/ws` (no SockJS fallback — modern browsers only)

- `config/SecurityConfig.java` - Spring Security configuration
  - `PasswordEncoder` bean — `BCryptPasswordEncoder` (default strength)
  - `SecurityFilterChain` bean — HTTP security rules:
    - Public: `/login`, `/register`, static assets, `/favicon.svg`, `/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/actuator/health`, `/actuator/info`
    - Admin-only: `/admin/**`, `POST /api/tags`, `DELETE /api/tags/**`, `POST /api/users`, `DELETE /api/users/**`
    - Everything else: `authenticated()`
  - Auth entry point: `/api/**` → 401 Unauthorized (no redirect); HTMX → `HX-Redirect` to login; browser → redirect to login
  - Form login: custom login page at `/login`, success → `/`, failure → `/login?error`
  - Logout: `POST /logout` → `/login?logout`, invalidates session, deletes JSESSIONID
  - CSRF: enabled for web forms (Thymeleaf auto-injects); disabled for `/api/**` and `/ws` (WebSocket endpoint)
  - Headers: `X-Frame-Options: DENY`

- `config/DevSecurityConfig.java` - Dev-only security rules (`@Profile("dev")`)
  - `@Order(1)` filter chain for `/h2-console/**` — permits all, disables CSRF, allows frames (sameOrigin)
  - Ordered before the main filter chain so H2 paths are matched first

- `config/H2DevConfig.java` - H2 database tooling (`@Profile("dev")`)
  - `startH2WebServer()` — starts H2 web server on port 8082
  - `h2ConsoleServlet()` — registers H2 console servlet at `/h2-console/*`

- `config/AppRoutesProperties.java` - `@ConfigurationProperties(prefix = "app.routes")`, Lombok `@Data`
  - All fields are `RouteTemplate` — plain routes and parameterized templates use the same type
  - Web routes: `projects`, `tasks`, `audit`, `dashboard`, `analytics`, `login`, `profile`
  - Parameterized web routes: `projectDetail`, `projectSettings`, `taskDetail`
  - API resource routes: `apiTasks`, `apiProjects`, `apiUsers`, `apiTags`, `apiNotifications`, `apiPresence`, `apiAnalytics`, `apiViews`, `apiAudit`, `apiRecentViews`
  - Parameterized API routes (URL templates with `{placeholder}` tokens): `apiProjectAnalytics`, `apiProjectSprints`, `apiProjectMembers`, `apiProjectMembersAssignable`, `apiNotificationRead`, `apiNotificationsUnreadCount`, `apiNotificationsReadAll`, `apiTaskSearchForDependency`, `apiViewById`
  - Spring binds `String` properties to `RouteTemplate` via `RouteTemplate.StringConverter` (`@ConfigurationPropertiesBinding`)
  - Single source of truth for all paths used by Thymeleaf templates, controllers, and frontend JS

- `config/GlobalModelAttributes.java` - `@ControllerAdvice` that injects shared attributes into every Thymeleaf model
  - `@ModelAttribute("appRoutes")` — exposes the `AppRoutesProperties` bean as `${appRoutes}` in all templates
  - `@ModelAttribute("currentPath")` — exposes `request.getRequestURI()` for navbar active link highlighting
  - `@ModelAttribute("settings")` — loads `Settings` POJO via `SettingService.load()` on every request
  - `@ModelAttribute("currentUser")` — resolves authenticated `User` from `SecurityContextHolder`; null for anonymous
  - `@ModelAttribute("userPreferences")` — loads `UserPreferences` POJO via `UserPreferenceService.load()` for current user; returns defaults when not logged in
  - Used by HTMX attributes (`th:attr="hx-get=${appRoutes.tasks + ...}"`) where `@{}` URL syntax cannot be used

- `config/UserPreferences.java` - Typed POJO for per-user preferences with defaults (mirrors `Settings` pattern)
  - `KEY_*` constants — DB key names matching field names exactly (`BeanWrapper` resolves by name): `KEY_TASK_VIEW`, `KEY_DEFAULT_USER_FILTER`, `KEY_DUE_REMINDER`
  - Value constants: `VIEW_CARDS`/`VIEW_TABLE`/`VIEW_CALENDAR`, `FILTER_MINE`/`FILTER_ALL`
  - Fields: `taskView` (default `"cards"`, also `"table"` or `"calendar"`), `defaultUserFilter` (default `"mine"`), `dueReminder` (default `true`)
  - `UserPreferenceService.load()` populates via `BeanWrapper`; missing keys keep defaults
  - To add a new preference: (1) add field with default, (2) add `KEY_*` constant matching field name

- `config/Settings.java` - Typed POJO for site-wide settings with defaults (not a JPA entity)
  - `KEY_*` constants — DB key names matching field names exactly (`BeanWrapper` resolves by name)
  - `THEME_DEFAULT`, `THEME_WORKSHOP`, `THEME_INDIGO` — theme id constants
  - Fields: `theme` (default `"default"`), `siteName` (default `"Spring Workshop"`), `registrationEnabled` (default `true`), `maintenanceBanner` (default `""`), `maintenanceBannerVersion` (default `""`), `notificationPurgeDays` (default `30`)

### Exception Handling
- `exception/EntityNotFoundException.java` - Custom unchecked exception for missing entities

- `exception/StaleDataException.java` - Custom unchecked exception for optimistic locking conflicts (409)

- `exception/BlockedTaskException.java` - Custom unchecked exception for completing a task with active blockers
  - `getBlockerNames()` — returns list of blocker task titles for user-facing messages

- `exception/CyclicDependencyException.java` - Custom unchecked exception for dependency cycles

- `exception/ApiExceptionHandler.java` - `@RestControllerAdvice` scoped to `controller.api`; extends `ResponseEntityExceptionHandler`
  - Ordered at `HIGHEST_PRECEDENCE` to win over `WebExceptionHandler`
  - Returns RFC 9457 `ProblemDetail` responses (`application/problem+json` content type)
  - Overrides `handleMethodArgumentNotValid()` — adds field-level `errors` map via `ProblemDetail.setProperty("errors", fieldErrors)`
  - Handlers sorted by error code:
    - 400: `MethodArgumentNotValidException`, `IllegalArgumentException`
    - 403: `AccessDeniedException`
    - 404: `EntityNotFoundException`
    - 409: `StaleDataException`, `BlockedTaskException` (adds `blockers` property with blocker names list)
    - 422: `CyclicDependencyException`
    - 500: catch-all `Exception`

- `exception/WebExceptionHandler.java` - `@ControllerAdvice` for Thymeleaf web controllers
  - Constructor injection: `SettingService` — injects `settings` into each `ModelAndView` manually (since `@ModelAttribute` methods from `GlobalModelAttributes` don't run for exception handlers)
  - Handles: `EntityNotFoundException` and `NoResourceFoundException` → `error/404.html`, `StaleDataException` → `error/409.html`, `BlockedTaskException` and `CyclicDependencyException` → `error/400.html`, catch-all → `error/500.html`
  - `AccessDeniedException` is explicitly re-thrown so Spring Security's `ExceptionTranslationFilter` can handle it → `error/403.html` (without this, the catch-all `Exception` handler would swallow it as a 500)
  - Each error `ModelAndView` adds `settingService.load()` as `"settings"` so error pages can access theme and site name

### Utilities
- `util/RouteTemplate.java` - URL template value type with `{placeholder}` tokens
  - `resolve()` overloads: no-arg (returns template), single key-value, Map params, Map params + Map query (with URL encoding)
  - `toString()` returns raw template — transparent in Thymeleaf expressions and string concatenation
  - `JS_CLASS` constant — JavaScript `Route` class with matching `resolve(params, query)`, `toString()`, `valueOf()` methods; emitted by `FrontendConfigController` into `/config.js`
  - Nested `StringConverter` (`@ConfigurationPropertiesBinding`) — allows Spring to bind `String` properties to `RouteTemplate` in `AppRoutesProperties`

- `util/Messages.java` - `@Component` wrapper around `MessageSource` for convenient message resolution in service layer
  - Constructor injection: `MessageSource`
  - `get(String key)` — resolves message with default locale, no args
  - `get(String key, Object... args)` — resolves message with default locale and arguments
  - `get(Translatable)` — convenience overload; calls `get(translatable.getMessageKey())` for type-safe enum translation
  - Used by `ProjectService`, `TaskService`, `TaskController`, and `TaskReport` (replaces direct `MessageSource` + `Locale` boilerplate)

- `util/HtmxUtils.java` - HTMX helper methods
  - `isHtmxRequest(HttpServletRequest)` - checks for `HX-Request: true` header
  - `triggerEvent(String eventName)` - returns `ResponseEntity` with `HX-Trigger` header set

- `util/MentionUtils.java` - `@Component("mentionUtils")` for parsing and rendering @mention tokens in comment text
  - Encoded format: `@[Display Name](userId:3)` — stored in DB as-is
  - `extractMentionedUserIds(String)` — static; parses encoded mention tokens, returns list of user IDs; used by `CommentService` for mention notifications
  - `renderHtml(String)` — instance method; converts encoded tokens to `<a href="/tasks?selectedUserId=N" class="mention">@Name</a>` with HTML escaping; exposed to Thymeleaf as `${@mentionUtils.renderHtml(text)}`; clicking a mention navigates to the task list filtered by that user
  - Regex pattern: `@\[([^\]]+)\]\(userId:(\d+)\)`

- `util/CsvWriter.java` - Generic CSV export utility
  - `write(HttpServletResponse, filename, headers, rows, rowMapper)` — sets `Content-Disposition` header, writes CSV with proper escaping (quotes, commas, newlines)
  - Generic `<T>` — caller provides `Function<T, String[]>` row mapper for any entity type
  - Used by `TaskController.exportTasks()` for task CSV download

### Bootstrap
- `DataLoader.java` - Seeds database on startup (`@Profile("dev")`): **20 users**, **8 tags**, **4 projects**, **56 tasks** (48 project-specific + 8 curated demo interactions), **4 recurring templates** (2 per non-sprint project), **6 saved views** (3 per Alice/Bob)
  - First user (Alice Johnson) gets `Role.ADMIN`; all others get `Role.USER`
  - All passwords: `"password"` (BCrypt-encoded once, reused for all 20 users for speed)
  - Dev credentials: `alice.johnson@example.com` / `password` (admin), `bob.smith@example.com` / `password` (regular)
  - 4 projects: Platform Engineering, Product Development, Security & Compliance, Operations — each with 5-7 members (OWNER/EDITOR/VIEWER)
  - 12 tasks per project, each assigned to an actual project member (not round-robin); uses all 6 statuses (BACKLOG, OPEN, IN_PROGRESS, IN_REVIEW, COMPLETED, CANCELLED)
  - Tags use software development categories: Bug, Feature, DevOps, Security, Documentation, Spike, Blocked, Tech Debt
  - Meaningful comments between actual project teammates; @mentions between collaborators
  - Checklists on ~30% of tasks with project-relevant items
  - `seedDemoInteractions()` — creates 8 curated tasks between Alice, Bob, Carol, David, Eva with comments (@mentions), checklists, notifications (assigned, comment, mention, overdue, due reminder), and audit logs for a realistic demo experience

## Thymeleaf Templates

### Layouts
- `templates/layouts/base.html` - Base layout with reusable fragments
  - `head(title, cssFile)` - two-parameter head fragment; `cssFile` is nullable for pages without page-specific CSS; includes `<link rel="icon">` for SVG favicon; `<meta name="_userId">` exposes current user ID for JS (WebSocket filtering); loads `mentions.css` globally
  - `sec:authorize="isAuthenticated()"` guard on WebSocket scripts (`stomp.umd.min.js`, `websocket.js`, `presence.js`, `notifications.js`) — prevents connection attempts for anonymous users
  - `navbar` - navigation bar with auth-aware elements:
    - Left nav links: Dashboard, Projects, Tasks, Analytics, Tags, Users — each with `currentPath`-based active highlighting via `th:classappend`
    - Anonymous: shows Register link
    - Authenticated: user dropdown with name, email, role badge, logout button
    - Admin: additional "User Management", "Tag Management", "Audit Log", and "Settings" links in dropdown
    - Uses `sec:authorize` (Spring Security Thymeleaf dialect) and `${#auth}` for conditional rendering
  - `footer` - footer
  - Notification bell dropdown in navbar (unread count badge, recent notifications list, mark-all-read and view-all links)
  - Online users indicator in navbar (count badge + dropdown list)
  - Recently viewed drawer markup (vertical tab on left edge, slide-out panel with item list; `d-none d-lg-block` — lg+ only); authenticated users only
  - `scripts` - Bootstrap + HTMX + `/config.js` + `utils.js` + Tribute.js (WebJar) + `mentions.js` + STOMP.js (WebJar) + `websocket.js` + `presence.js` + `notifications.js` + `recent-views.js` (in that order — `APP_CONFIG` must be set before page scripts; Tribute before mentions; STOMP client before feature scripts)

- `templates/layouts/pagination.html` - Reusable pagination control bar
  - `controlBar(page, position, label)` — `page` is `Page<?>`, `position` is `'top'`/`'bottom'`, `label` is item noun (e.g. "tasks", "entries")
  - Renders: result count, page navigation with ellipsis (±2 window), per-page selector (10/25/50/100)
  - Dispatches custom DOM events (`pagination:navigate`, `pagination:resize`) instead of calling named JS functions
  - `th:selected` on `<option>` elements auto-syncs per-page selector after HTMX swaps

### Task Views
- `templates/tasks/tasks.html` - Main cross-project task list page
  - "New Task" button (`btn-lg`) in page header; links to `/tasks/new` (opens modal via HTMX)
  - Includes `task-workspace.html` via `th:replace` for all filter/sort/view controls
  - All state managed in JS (`tasks/tasks.js`) — synced to URL params and cookies
  - Loads `tasks/task-form.js`, `tasks/inline-edit.js`, `tasks/kanban.js`, `tasks/keyboard-shortcuts.js` page-specifically

- `templates/tasks/task-cards.html` - Card grid fragment (`grid` fragment)
- `templates/tasks/task-card.html` - Individual task card fragment (`card` fragment, reads `${task}` from context); 6-state status badge and toggle button (Backlog/Open/In Progress/In Review/Completed/Cancelled with distinct icons and colors); project name link in card body; checklist progress bar (checked/total) when task has checklist items; `canEdit` resolved from `canEditProject` (project-scoped) or `projectEditMap` (cross-project) or ownership fallback
- `templates/tasks/task-table.html` - Table view fragment (`grid` fragment)
- `templates/tasks/task-workspace.html` - Shared task list controls fragment — used by both `tasks.html` (cross-project) and `project.html` (single project); renamed from `task-list-fragment.html` in Phase 8
  - Reads from model context: `view`, `selectedUserId`, `filterUserName`, `allTags`, `taskPage`, `calendarWeeks`, `calendarMonth`, `undatedCount`, `tasksByStatus` (kanban board)
  - Optional `canEditProject` attribute — when set, controls task edit/delete visibility for project-scoped views
  - Stale-data banner (`#stale-banner`) — hidden by default; shown by JS on WebSocket task change events
  - Search input (live search, `autocomplete="off"`), status filter dropdown (All/Backlog/Open/In Progress/In Review/Completed/Cancelled/Overdue), user filter (All Users / Mine), priority dropdown filter, sort dropdown (title, createdAt, priorityOrder, dueDate, updatedAt, description), tag filter dropdown with pills, view toggle (cards/table/calendar/board)
  - Saved views dropdown — lists user's saved views; load button applies stored filter state; save button POSTs current filter state to `/api/views`; delete removes via `DELETE /api/views/{id}`. Active saved view name shown on button (filled bookmark icon, primary color); clears when user manually changes filters
  - Task list container with conditional `th:replace` for table, calendar, cards, or board view
  - Two shared modal shells: `#task-modal` (create/edit form via HTMX) and `#task-delete-modal` (delete confirmation populated via JS `show.bs.modal` event)
- `templates/tasks/task-calendar.html` - Calendar view fragment (`grid` fragment)
  - Monthly grid (Monday start); reads `${calendarWeeks}` (List<List<CalendarDay>>), `${calendarMonth}` (YearMonth), `${undatedCount}` (long)
  - Month navigation: prev/next buttons + Today button (right-aligned with `>`)
  - Task chips: colored by status (open/in-progress/completed/overdue), border-left for start-date-only tasks, border-right for due-date tasks
  - User initials circle on chips (clickable to filter by user)
  - Scrollable chip container per cell (no max chip limit)
  - "+" button on hover to create task with pre-filled due date
  - Undated tasks info banner when tasks without dates exist in current filters
- `templates/tasks/task-table-row.html` - Single table row fragment (`row` fragment); 6-state status badge and toggle button; project name link; checklist progress display (checked/total) when task has checklist items; `canEdit` resolved from `canEditProject` or `projectEditMap` or ownership fallback
- `templates/tasks/task-activity.html` - Unified activity timeline template (replaces task-comments.html and task-audit.html)
  - Merges comments and audit history into a single chronological list; uses `TimelineEntry.type` to discriminate between comment and audit entries
  - `:: list` fragment selector — returns timeline list only (used by `task.html` and `task-modal.html` during page render via `task-layout.html`)
  - Whole-file return — includes timeline list + `hx-swap-oob` spans for activity count updates (used by controller for HTMX add/delete responses)
  - Comment entries rendered with `MentionUtils.renderHtml()` for styled @mention spans; delete buttons use `hx-delete` with `hx-confirm` and `data-confirm-*` attributes
  - Audit entries show action badge, principal, and field-level change details

- `templates/tasks/task-dependencies.html` - Dependency panel fragment with "Blocked By" and "Blocks" sections
  - Searchable select for adding dependencies; remove buttons in edit mode; hidden inputs for form submission
  - Used by `task-layout.html` in both modal and full-page views

- `templates/tasks/task-board.html` - Kanban board view fragment (`grid` fragment)
  - Reads `${tasksByStatus}` — `Map<TaskStatus, List<Task>>` built by `TaskService.groupByStatus()`
  - One column per `TaskStatus` (6 columns: Backlog/Open/In Progress/In Review/Completed/Cancelled)
  - Draggable cards with title, priority badge, assignee initials circle, due date chip; drag handle via `draggable="true"`
  - Powered by `kanban.js` — native HTML5 Drag and Drop (`dragstart`/`dragover`/`drop`/`dragend` handlers); on drop POSTs to `POST /tasks/{id}/status`
  - Column headers show status label and task count badge

- `templates/tasks/keyboard-help-modal.html` - Modal fragment showing keyboard shortcut reference table
  - Bare fragment (no HTML wrapper); loaded into a static modal shell in `task-workspace.html`
  - Two-column table: shortcut key and action description; all strings from `messages.properties`

- `templates/tasks/task-form.html` - **Shared form fields fragment only**
  - `fields` fragment — hidden `version` input; project selector dropdown (shown in create mode when `editableProjects` available, hidden field fallback when project is pre-set); title, description, status radio buttons (6-state: Backlog/Open/In Progress/In Review/Completed/Cancelled with icons and colors, shown on edit/view only), priority radio buttons (with reception bar icons), start date picker, due date picker, read-only completedAt/updatedAt (on edit/view), user `<searchable-select>` (remote, one value, @ManyToOne), tag checkboxes (multiple, @ManyToMany)
  - `checklist` fragment — separate fragment for checklist items section; rendered with existing items on edit, empty container on create; add/remove/reorder via `task-form.js`; each item has a drag handle for reordering
  - `mode` attribute controls field state: `'create'` hides status; `'view'` disables all inputs; `'edit'` shows everything editable
  - No `<form>` tag; `th:object` is set by the including template (binds to `TaskFormRequest`)
  - Used by both `task.html` and `task-modal.html`

- `templates/tasks/task-layout.html` - Shared two-column layout fragment used by both `task-modal.html` and `task.html`
  - Contains form column (left) and activity panel column (right) with unified timeline via `task-activity.html`
  - Comment input uses `div` + HTMX (not nested `<form>`) to avoid form-in-form issues; Enter key guarded by `isMentionMenuActive()` to prevent submission while selecting @mentions
  - `data-mention` + `data-project-id` attributes on comment input for project-scoped Tribute.js @mention autocomplete
  - Stale-data banner and WebSocket subscription logic shared across both consumers
- `templates/tasks/task.html` - Full-page create/edit form; uses shared `task-layout.html` fragment for two-column layout with flex-grow; project subtitle hidden in create mode (user picks project from dropdown instead); stale-data banner via WebSocket when another user modifies the same task; live activity auto-refresh via WebSocket subscription to `/topic/tasks/{id}/comments`
- `templates/tasks/task-modal.html` - HTMX modal content (bare file, split-panel layout); uses shared `task-layout.html` fragment; project subtitle hidden in create mode; footer moved outside `d-flex` for full-width; submit button uses `form="task-form"` attribute; stale-data banner via WebSocket; live activity auto-refresh via WebSocket; `.task-panels` constrains two-panel layout to 80vh with independent scrolling

### Project Views
- `templates/projects/projects.html` - Project list page with sort controls (Name/Newest) and archived toggle
  - Admin sees all projects; regular users see their projects
  - Sort and filter via `updateProjectList()` JS function using `htmx.ajax()`
  - HTMX-aware: `project-grid :: grid` fragment swap on sort/filter change
- `templates/projects/project.html` - Project home page with task list (single-project scope)
  - Project header: name, description, archived badge, settings link (owner/admin only)
  - Members bar: inline member names with role badges
  - Includes `task-workspace.html` for full task filtering/sorting/view controls
  - Sets `TASKS_BASE_OVERRIDE` JS variable for project-scoped task API calls
  - Loads `tasks/task-form.js`, `tasks/tasks.js`, `tasks/inline-edit.js`, `tasks/kanban.js`, `tasks/keyboard-shortcuts.js` page scripts
- `templates/projects/project-form.html` - Create project form (full page, centered card)
  - Name (required) and description fields with validation
  - Posts to `/projects`
- `templates/projects/project-settings.html` - Project settings page (owner/admin only)
  - Back link to project home
  - Project details card: name/description edit form
  - Members card: includes `member-table.html` fragment
  - Danger zone card: archive/unarchive with `showConfirm()` styled confirmation
- `templates/projects/project-grid.html` - Project card grid fragment (`grid` fragment, bare fragment pattern)
  - Responsive 3-column grid of project cards with `card-lift` hover effect
  - Archived projects shown with `bg-light text-muted` styling (no lift)
  - Card footer: creator name, creation date, sprint badge (when enabled), status badge; `flex-wrap` prevents crowding at narrow widths
  - Empty state with folder icon
- `templates/projects/member-table.html` - Member management fragment (bare fragment pattern)
  - Add member form: `<searchable-select>` for user (remote), role dropdown, add button; HTMX POST
  - Toast triggers for add/remove/role change via inline `<script>` blocks
  - Member table: name, email, role dropdown (with HTMX PATCH for instant role change), remove button with `hx-confirm` styled confirmation
  - Role dropdown color-coded: Owner (primary), Editor (success), Viewer (secondary)

### Analytics Views
- `templates/analytics/analytics.html` - Analytics page with 6 Chart.js charts in responsive grid
  - Shared template for both cross-project (`/analytics`) and project-scoped (`/projects/{id}/analytics`) views
  - `<meta name="_analyticsApi">` passes API URL to JS (different per scope)
  - Project filter card (cross-project only, `th:if="${project == null}"`) with per-project checkboxes and Select All
  - Back link (project-scoped only) to project home
  - Row 1: Status Breakdown (doughnut) + Priority Breakdown (doughnut)
  - Row 2: Workload Distribution (stacked bar, full width)
  - Row 3: Burndown Chart (line) + Velocity (line)
  - Row 4: Overdue by Assignee (bar, full width)
  - Chart.js loaded via WebJar (`/webjars/chart.js/4.5.1/dist/chart.umd.min.js`)

### Notification Views
- `templates/notifications.html` - Full notifications page
  - List-group items with action link icons, read/unread styling
  - Pagination support
  - Clear all button
  - Listens to custom DOM events (`notification:received`, `notification:read`, `notification:allRead`, `notification:cleared`) for real-time updates

### Dashboard Views
- `templates/dashboard/dashboard.html` - Dashboard page with welcome banner, `btn-lg` quick action buttons ("New Task" conditional on editable projects, "My Tasks" links to `/tasks?selectedUserId=`), and `th:replace` of `dashboard-stats.html`; subscribes to `/topic/tasks` and `/topic/presence` via WebSocket; refreshes stats fragment via `htmx.ajax()` on any task or presence change (no self-filtering — dashboard should reflect own actions from other tabs)
- `templates/dashboard/dashboard-stats.html` - Bare fragment (`<div id="dashboard-stats">`); three sections: My Tasks stats (6 cards including In Review), per-project summary cards (open/in-progress/in-review/completed/overdue per project with progress bar), admin-only System Overview (5 cards: total/open/completed/overdue/online); "At a Glance" section with My Recent Tasks, Due This Week, and Recent Activity feed; detail card bodies use `dashboard-scroll` class (max-height with overflow scroll) to prevent unbounded growth; returned whole-file for HTMX refresh or via `th:replace` for full page

### Profile Views
- `templates/profile/profile.html` - User profile page with three cards in two-column layout
  - Left column: Account card (name/email form with `ProfileRequest`), Change Password card (`ChangePasswordRequest` with current/new/confirm fields)
  - Right column: Preferences card (task view mode radio: cards/table, default user filter radio: mine/all)
  - Flash-attribute-driven toast notifications via `data-toast` attributes and inline JS

### Home, Tag, User, Auth, Admin, Error Views
- `templates/home.html` - Home page with hero section and 8 feature cards (REST API, Spring Security, Dynamic UI, Data & Persistence, Real-Time, Task Lifecycle, Admin & Audit, Production Ready); all strings from `messages.properties` (`home.feature.*` keys)
- `templates/tags/tags.html` - Tag list page (table, badge links to task filter)
- `templates/users/users.html` - User list page (HTMX live search)
- `templates/users/user-table.html` - User table fragment (bare file)
- `templates/login.html` - Login page (Spring Security handles POST)
- `templates/register.html` - Registration page
- `templates/error/400.html` - Bad Request error page
- `templates/error/403.html` - Access Denied page
- `templates/error/404.html` - Not Found page
- `templates/error/409.html` - Conflict page (optimistic locking, rendered by `WebExceptionHandler`)
- `templates/error/500.html` - Server Error page
- `templates/admin/tags.html` - Admin tag management page (full page: heading + includes tag-table fragment; JS listens for `tagDeleted` event)
- `templates/admin/tag-table.html` - Tag management table fragment (bare file: inline create form + tag table with name, task counts, delete button; `hx-confirm` with styled confirmation)
- `templates/admin/users.html` - Admin user management page (modal shell for create/edit; JS: `htmx:afterSwap` shows modal, `userSaved` refreshes table, delete/disable confirmation via `showConfirm`)
- `templates/admin/user-table.html` - User table fragment (bare file: status column with Active/Disabled badges, edit/enable/remove buttons, disabled rows with `table-secondary`)
- `templates/admin/user-modal.html` - User create/edit modal form (shared for both; hidden `id` field, password field on create only; uses `data-method`/`data-action` with inline JS for dynamic `hx-post`/`hx-put`)
- `templates/admin/settings.html` - Admin settings page (general settings form + theme picker grid)
  - General settings: site name input, registration toggle, maintenance banner input; saved via `htmx.ajax()` POST
  - Theme picker: color swatch cards with `hx-post`; JS `themeSaved` listener applies theme live and updates active state
- `templates/admin/audit.html` - Audit log page (admin only)
- `templates/admin/audit-table.html` - Audit table fragment (HTMX partial); uses shared `audit-diff.html` fragment
- `templates/layouts/audit-diff.html` - Shared audit diff rendering fragment; type-dispatched rendering for diff (old/new AuditField) and snapshot (single AuditField) modes; called from `task-activity.html` and `audit-table.html`

## Static Resources

- `static/favicon.svg` - SVG favicon (blue rounded square with white "S")
- `static/css/base.css` - Global styles (body, btn transitions, validation, navbar, footer, HTMX indicator, toast container/animations); `.card-clip` for overflow clipping; `.card-lift` opt-in hover lift; `#confirm-modal` z-index and width styles; `.nav-link-bright` for brighter navbar links with active state; recently viewed drawer styles (`.recent-views-tab` fixed left-side vertical tab, `.recent-views-drawer` slide-out panel, lg+ only via media query); audit diff styles (`.audit-diff-field`, `.audit-added`, `.audit-removed`, `.audit-unchecked`)
- `static/css/tasks.css` - Task page styles (filters, search clear button, tag badges, `.task-panels` for constrained two-panel modal layout, `.task-side-panel` and `.task-side-panel-body` for exclusive side panels with independent scrolling, two-column layout styles, timeline entry styles)
- `static/css/mentions.css` - Tribute.js dropdown styles (Bootstrap-themed: `.tribute-container` positioning/shadow/borders) + rendered mention span styles (`.mention` class with background highlight)
- `static/css/analytics.css` - Analytics page styles (chart container sizing: 300px default, 350px wide; responsive breakpoints at 768px)
- `static/css/audit.css` - Audit page styles (category buttons, search clear button)
- `static/css/theme.css` - Theme overrides per `[data-theme]` value; palette tokens (`--theme-*`) mapped to Bootstrap `--bs-*` variables; themes: `workshop`, `indigo`
- `static/css/components/searchable-select-bootstrap5.css` - Bootstrap 5 theme for `<searchable-select>`
- `static/js/websocket.js` - Shared STOMP WebSocket client
  - Creates a single `StompJs.Client` connection to `/ws`
  - Exposes `window.stompClient.onConnect(callback)` — feature scripts register subscriptions via this; handles late registration (calls callback immediately if already connected)
  - Single connection shared by all features (presence, notifications)
- `static/js/presence.js` - Online presence indicator
  - Uses `window.stompClient.onConnect()` to subscribe to `/topic/presence`
  - Fetches `GET /api/presence` after subscribing for initial state (connect event fires before subscription)
  - Updates online count badge and user list dropdown in navbar
- `static/js/notifications.js` - Notification bell and event bus
  - Subscribes to `/user/queue/notifications` via shared STOMP client for real-time push
  - Custom DOM events decouple producers from consumers: `notification:received`, `notification:read`, `notification:allRead`, `notification:cleared`
  - Manages navbar badge count and dropdown notification list
  - Exposes `window.notificationHelpers` for the full notifications page to reuse rendering logic
- `static/js/mentions.js` - @mention autocomplete via Tribute.js
  - `initMentionInputs(root)` — attaches Tribute to any `[data-mention]` element; project-scoped via `data-project-id` attr (fetches `GET /api/projects/{id}/members` once, cached per input), falls back to `GET /api/users?q=` when no project context; uses `positionMenu: false` + CSS for dropdown positioning (Tribute's built-in caret calculation unreliable in flex/modal layouts)
  - `mentionMap` WeakMap — tracks `element → Map<name, id>` for encoding; populated by Tribute's `selectTemplate` callback
  - `isMentionMenuActive()` — returns true if any Tribute dropdown is visible; used to suppress Enter-to-post while selecting a mention
  - `encodeMentions(text, el)` — converts clean `@Name` display to encoded `@[Name](userId:N)` format before submission
  - `clearMentions(el)` — resets mention tracking for an element (used after comment post)
  - Atomic backspace: `keydown` handler removes entire `@Name` token when cursor is inside a mention
  - Auto-encodes on both HTMX requests (`htmx:configRequest`) and regular form submissions (`submit` event)
  - Auto-initializes on `DOMContentLoaded` and `htmx:afterSwap`
- `static/js/tasks/tasks.js` - Main task list page logic (search, filters, pagination, view switching, saved views); moved from `static/js/tasks.js` in Phase 8
  - `STATUS_CONFIG` maps each status to label/icon/color; `renderStatusButton()` builds status filter dropdown items; `setStatusFilter` uses enum names (BACKLOG, OPEN, IN_PROGRESS, IN_REVIEW, COMPLETED, CANCELLED)
  - Task delete uses `hx-delete`; subscribes to `/topic/tasks` via shared STOMP client for stale-data banner
  - User filter variable: `selectedUserId`; supports `TASKS_BASE_OVERRIDE` for project-scoped views
  - Saved views: `applySavedView(view)` restores filter state from stored JSON and sets `activeViewName`; `saveCurrentView()` POSTs current state to `POST /api/views`; `deleteSavedView(id)` calls `DELETE /api/views/{id}` and refreshes dropdown. `renderActiveViewLabel()` updates button text/style/icon; `clearActiveView()` resets to default. `_keepActiveView` flag prevents `doSearch` from clearing the label when called from `applySavedView`
  - Sort entries use `{field, direction}` objects in `activeSorts` array; `SORT_LABELS` maps `"field,direction"` keys to display labels
- `static/js/tasks/task-form.js` - Task form logic (checklist management, project-aware assignee list); moved from `static/js/task-form.js` in Phase 8
  - `bindProjectChange()` — updates assignee searchable-select `_src` to `/api/projects/{id}/members/assignable` when project dropdown changes; clears cache and current selection; binds on DOMContentLoaded and htmx:afterSwap (for modal)
  - `addChecklistItem()`, `removeChecklistItem(btn)`, `updateChecklistHeading()`, drag-and-drop reorder handlers (`checklistDragStart`, `checklistDragOver`, `checklistDrop`, `checklistDragEnd`)
  - Loaded page-specifically on task pages (via `tasks.html` script block)
- `static/js/tasks/bulk-actions.js` - Cross-page bulk selection and actions for table view
  - `bulkSelectedIds` Set + `bulkSelectedProjectIds` Map (taskId→projectId) — persists selection across HTMX page swaps
  - `onBulkSelectChange(checkbox)`, `toggleSelectAll(checked)`, `clearBulkSelection()` — selection management
  - `recheckVisibleBoxes()` — restores checkbox state after HTMX page navigation; `updateSelectAllState()` — handles indeterminate state
  - `getCommonProjectId()` — returns single project ID if all selected tasks share one project, else null
  - `renderBulkBar()` — shows/hides floating action bar; toggles assign button based on common project
  - `executeBulkAction(action, value)` — sends JSON POST to `/tasks/bulk` with CSRF token from meta tags
  - `executeBulkDelete()` — shows `showConfirm()` dialog before executing
  - `loadBulkAssignUsers()` / `filterBulkAssignUsers(query)` / `renderBulkAssignList(users)` — assign dropdown with project-scoped user list from `/api/projects/{id}/members/assignable`; cached per project
  - Loaded page-specifically on task pages (via `tasks.html` and `project.html` script blocks)
- `static/js/tasks/inline-edit.js` - Toggle-based inline editing for table view
  - `toggleEditMode()` — activates/deactivates inline edit mode via pill button in table header (primary outline → green fill when active, icon swaps pencil-square/pencil-fill); in edit mode, renders editable inputs (text, select, date) in place of static cell content
  - Supports fields: title (text input), description (text input), priority (select), status (select), dueDate (date input)
  - On save: sends `PATCH /tasks/{id}/field` with field name and new value; reverts on error
  - Edit mode toggle controlled by the `e` keyboard shortcut (via `keyboard-shortcuts.js`)
- `static/js/tasks/kanban.js` - Kanban board drag-and-drop via native HTML5 Drag and Drop API
  - `dragstart` — sets transfer data (task ID); adds dragging styles
  - `dragover` — highlights drop target column; prevents default to allow drop
  - `drop` — reads task ID from transfer data; POSTs to `POST /tasks/{id}/status` with target column's status
  - `dragend` — clears dragging styles regardless of outcome
  - Updates card position in DOM optimistically; reverts on server error
- `static/js/tasks/task-dependencies.js` - Dependency picker and management
  - Binds `.dep-picker` elements, creates dependency items with hidden inputs, manages exclude lists dynamically
  - Re-binds on `htmx:afterSettle` for modal support
- `static/js/tasks/keyboard-shortcuts.js` - Keyboard shortcut handler for task pages
  - `h` — open keyboard help modal; `n` — open new task modal; `s` / `/` — focus search input
  - `1` / `2` / `3` / `4` — switch to cards / table / calendar / board view
  - `e` — toggle inline edit mode (table view only; no-op in other views)
  - `Escape` — close open modal or cancel inline edit mode
  - All shortcuts suppressed when focus is in an input, textarea, or select element
- `static/js/recent-views.js` - Recently viewed drawer panel
  - Toggle panel via vertical tab click; outside click closes
  - Fetches initial list from `GET /api/recent-views` on WebSocket connect
  - Subscribes to `/user/queue/recent-views` for live updates
  - `titleOnly` messages update existing item text in place; other messages prepend new item to top
- `static/js/analytics.js` - Analytics page chart rendering
  - IIFE pattern; reads API URL from `<meta name="_analyticsApi">`
  - `STATUS_COLORS` / `PRIORITY_COLORS` — color maps matching app-wide status/priority colors
  - `charts` object tracks Chart.js instances for destroy/re-create on filter change
  - `getSelectedProjectIds()` — reads project filter checkboxes; returns null if no filter UI (project-scoped page)
  - `initFilterListeners()` — binds change events on project checkboxes and Select All; triggers re-fetch
  - `fetchAndRender()` — fetches JSON from API, renders 6 charts: status (doughnut), priority (doughnut), workload (stacked bar), burndown (line), velocity (line), overdue (bar)
  - Status/priority labels resolved via `APP_CONFIG.messages`
- `static/js/utils.js` - Shared utilities (`getCookie`, `setCookie`); `showToast(message, type, options)` for toast notifications (optional `options.href` for clickable toasts); `showConfirm(options, onConfirm)` for styled Bootstrap confirm dialogs; CSRF injection for HTMX; `htmx:confirm` integration with `data-confirm-*` attributes; 409 conflict handler
- `static/js/audit.js` - Audit page logic (category filter, search, date range, pagination)
- `static/js/components/searchable-select.js` - Reusable `<searchable-select>` Web Component
- Bootstrap Icons served via WebJar (`bootstrap-icons:1.13.1`); loaded globally in `base.html`
- Tribute.js served via WebJar (`github-com-zurb-tribute:5.1.3`); loaded globally in `base.html`

## Resource Files

- `resources/messages.properties` - UI display strings
  - Namespace conventions:
    - `action.*` — generic actions; `pagination.*` — pagination controls
    - `nav.*`, `footer.*`, `page.title.*` — layout strings
    - `task.*` — Task feature (includes `task.status.backlog`, `task.status.open`, `task.status.inProgress`, `task.status.inReview`, `task.status.completed`, `task.status.cancelled`, `action.toggle.*.next` for advance status button labels, `task.field.project`, `task.field.project.placeholder`, `task.bulk.*` for bulk action UI, `task.dependency.*` for dependency management UI); `project.*` — Project feature (includes `project.role.*`, `project.member.*`, `project.action.*`); `tag.*` — Tag feature; `user.*` — User feature
    - `analytics.*` — Analytics feature (chart titles, labels, filter keys)
    - `dashboard.*` — Dashboard feature (includes `dashboard.my.inReview`, `dashboard.projects.heading`, `dashboard.glance.heading`, `dashboard.system.heading`, `dashboard.system.*` stat labels)
    - `login.*`, `register.*` — Auth pages
    - `admin.*` — Admin panel; `audit.*` — Audit feature (includes `audit.field.status`)
    - `home.feature.*` — Home page feature cards (rest, security, ui, data, realtime, lifecycle, admin, production)
    - `recentlyViewed.*` — Recently viewed feature (`recentlyViewed.heading`, `recentlyViewed.empty`)
    - `notification.*` — Notification feature (includes `notification.task.assigned`, `notification.comment.added`, `notification.time.*` for relative timestamps)
    - `role.*` — Role display names; `error.*` — Error pages; `toast.*` — Toast notifications

- `resources/META-INF/additional-spring-configuration-metadata.json` - IDE metadata for custom `app.routes.*` properties

## Test Files

- `test/resources/application-test.properties` - Test profile config (separate H2 `testdb`, no SQL logging, Flyway disabled)
- `test/java/.../DemoApplicationTests.java` - Context load smoke test (`@SpringBootTest`, `@ActiveProfiles("test")`)
- `test/java/.../service/TaskQueryServiceTest.java` - 6 unit tests (Mockito): getTaskById, getAllTasks, getIncompleteTasks, searchTasks
- `test/java/.../service/TaskServiceTest.java` - 14 unit tests (Mockito): CRUD, optimistic locking, status transitions, assignment rules
- `test/java/.../service/TagServiceTest.java` - 6 unit tests (Mockito): CRUD, audit event publishing (`any(AuditEvent.class)` for correct overload matching)
- `test/java/.../service/CommentServiceTest.java` - 14 unit tests (Mockito): CRUD, event publishing, subscriber/mention ID extraction, deduplication
- `test/java/.../service/UserServiceTest.java` - 20 unit tests (Mockito): CRUD, find/get, search, canDelete logic, enable/disable + unassign, profile update with diff, role change, password change
- `test/java/.../service/ProjectQueryServiceTest.java` - 9 unit tests (Mockito): getProjectById, getProjectsForUser, access checks (isMember, isOwner, isEditor)
- `test/java/.../service/ProjectServiceTest.java` - 13 unit tests (Mockito): CRUD, archive, delete (with/without completed tasks), member management (add/remove/role change), last-owner protection, viewer demotion unassigns tasks
- `test/java/.../service/NotificationServiceTest.java` - 8 unit tests (Mockito): DB-first create + WebSocket push, unread count, pagination, mark-as-read, mark-all, clear-all
- `test/java/.../audit/AuditFieldTest.java` - 29 unit tests: factory methods, valueEquals semantics (REFERENCE by ID), isBlank, displayValue, checklist encoding/diff, JSON round-trip
- `test/java/.../audit/AuditTemplateHelperTest.java` - 20 unit tests (MessageSource mock): enum label resolution, URL resolution, checklist diff/format, isBlank for all field types
- `test/java/.../audit/AuditDetailsTest.java` - 12 unit tests: typed diff (text, enum, reference, collection changes), JSON serialization, backwards compat
- `test/java/.../audit/AuditEventListenerTest.java` - 2 unit tests (Mockito): persists audit log, skips system principal
- `test/java/.../event/NotificationEventListenerTest.java` - 8 unit tests (Mockito): task assigned/updated/comment notification routing, self-exclusion, deduplication across groups
- `test/java/.../event/WebSocketEventListenerTest.java` - 2 unit tests (Mockito): broadcasts to correct STOMP topics
- `test/java/.../util/MentionUtilsTest.java` - 12 unit tests: extract user IDs (single, multiple, duplicates, none, null, malformed), render HTML links, XSS escaping in text and display names
- `test/java/.../service/TaskDependencyServiceTest.java` - 16 unit tests (Mockito): reconciliation, cycle detection (BFS), same-project validation, self-reference prevention, active blocker filtering
- `test/java/.../controller/api/TaskApiControllerTest.java` - 15 tests (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@MockitoBean`): REST API JSON CRUD, auth redirect, validation 400, ownership 403, optimistic locking 409
- `test/java/.../controller/api/CommentApiControllerTest.java` - 7 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): GET/POST/DELETE, auth redirect, ownership 403, not found 404
- `test/java/.../controller/api/TagApiControllerTest.java` - 7 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): GET all/by-id, admin-only POST 201/DELETE 204, regular user 403
- `test/java/.../controller/api/UserApiControllerTest.java` - 8 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): GET all/with-query/by-id, admin POST 201/DELETE 204, regular user 403, self-delete 400
- `test/java/.../controller/api/NotificationApiControllerTest.java` - 6 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): unread count, paginated list, custom page size, mark-as-read, mark-all, clear-all
- `test/java/.../controller/api/AuditApiControllerTest.java` - 2 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): admin gets page, regular user 403
- `test/java/.../controller/api/PresenceApiControllerTest.java` - 2 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): online users + count, empty list
- `test/java/.../security/SecurityConfigTest.java` - 18 tests (`@SpringBootTest` + `@AutoConfigureMockMvc`): public access (login, register, static assets, actuator health/info), auth required, admin-only (pages + API mutations), CSRF (exempt for API, required for web forms)
- `test/java/.../security/OwnershipGuardTest.java` - 3 unit tests (Mockito): owner access allowed, admin access allowed, non-owner non-admin throws `AccessDeniedException`
- `test/java/.../repository/TaskSpecificationsTest.java` - 10 tests (`@DataJpaTest`): status filter, keyword search (case-insensitive), user/priority/overdue/tag filters, combined filters
- `test/java/.../repository/AuditLogSpecificationsTest.java` - 11 tests (`@DataJpaTest`): category filter (prefix, case-insensitive, null, unknown), search (principal, details, blank), date range, combined build
- `test/java/.../validation/UniqueValidatorTest.java` - 6 tests (`@DataJpaTest` + `@Import(ValidationAutoConfiguration.class)`): unique passes, duplicate fails, case-insensitive, self-exclusion on update, null/blank passthrough

- `resources/ValidationMessages.properties` - Bean Validation error messages
  - Used by Hibernate Validator; reference with `{key}` syntax in constraint annotations
  - `{min}`, `{max}` placeholders interpolated from annotation attributes
  - Includes `validation.unique` (default), `tag.name.unique`, `user.email.unique` for `@Unique` validator

## Configuration Files

- `resources/application.properties` - Shared config across all profiles
  - `spring.profiles.active=dev` (default profile)
  - `spring.jpa.open-in-view=false` (OSIV disabled)
  - `spring.mvc.problemdetails.enabled=true` (RFC 9457 ProblemDetail)
  - springdoc paths, cache busting, actuator exposure (health, info)

- `resources/application-dev.properties` - Dev profile (`@Profile("dev")`)
  - H2 in-memory (`jdbc:h2:mem:taskdb`), `ddl-auto=create-drop`, show-sql, H2 console enabled
  - Flyway disabled (schema created by Hibernate)

- `resources/application-prod.properties` - Prod profile
  - PostgreSQL via `${DATABASE_URL}` env var, `ddl-auto=validate`
  - Flyway enabled (`classpath:db/migration`)
  - H2 console disabled, Swagger UI disabled

- `resources/db/migration/V1__initial_schema.sql` - Flyway initial migration
  - PostgreSQL DDL for all 17 tables: users, projects, project_members, tasks, checklist_items, tags, task_tags, task_dependencies, comments, audit_logs, notifications, settings, user_preferences, recurring_task_templates, recurring_template_tags, saved_views, recent_views
  - Seeds default admin user (`admin@example.com` / `password`)
  - Mirrors JPA entity definitions; used when `ddl-auto=validate` (prod profile)

## Build and Deployment Files

- `docker-compose.prod.yml` - Local prod testing (PostgreSQL 18 + app)
  - PostgreSQL: `springdemo` database, `demo`/`demo` credentials (local testing only)
  - App: builds from Dockerfile, `SPRING_PROFILES_ACTIVE=prod`, port 8081
  - Healthcheck on PostgreSQL; app waits for healthy status

- `.github/workflows/ci.yml` - CI pipeline (GitHub Actions)
  - Triggers: push to main, PR targeting main
  - Steps: checkout, JDK 25 setup, Maven cache, `./mvnw verify`

- `.github/workflows/daily-redeploy.yml` - Render deploy trigger
  - Cron: 6 AM UTC+8 daily
  - Pings Render deploy hook URL

---

## Reference Appendix

The following sections were moved from CLAUDE.md. They are reference material derivable from source files but kept here for quick lookup.

### Available URLs

**Authentication:**
- `http://localhost:8080/login` - Login page (email + password)
- `http://localhost:8080/register` - Self-registration (creates `USER` role)

**Web UI** (requires login):
- `http://localhost:8080/` - Home page
- `http://localhost:8080/projects` - Project list
- `http://localhost:8080/projects/new` - Create project
- `http://localhost:8080/projects/{id}` - Project home with task filtering
- `http://localhost:8080/projects/{id}/settings` - Project settings and member management (owner/admin)
- `http://localhost:8080/tasks` - Cross-project task list (cards, table, calendar, or board view)
- `http://localhost:8080/tasks/new` - Create task (full page; modal preferred)
- `http://localhost:8080/tasks/{id}/edit` - Edit task (full page; modal preferred)
- `http://localhost:8080/tasks/export` - CSV export of filtered tasks (respects current filters/sort)
- `http://localhost:8080/dashboard` - Per-project dashboard (real-time stats, recent tasks, activity feed); admins see additional system overview section
- `http://localhost:8080/tags` - Tag list
- `http://localhost:8080/users` - User list with search
- `http://localhost:8080/admin/users` - User management: create/edit/delete/disable/enable (admin only)
- `http://localhost:8080/admin/tags` - Tag management: create/delete with task counts (admin only)
- `http://localhost:8080/admin/audit` - Audit log with search/filters (admin only)
- `http://localhost:8080/admin/settings` - Site settings: theme, site name, registration, maintenance banner (admin only)
- `http://localhost:8080/analytics` - Cross-project analytics (charts: status, priority, workload, burndown, velocity, overdue)
- `http://localhost:8080/projects/{id}/analytics` - Project-scoped analytics (same charts, single project)
- `http://localhost:8080/notifications` - Notification inbox (paginated, mark-read, clear-all)
- `http://localhost:8080/profile` - User profile: edit name/email, change password, preferences

**REST API — Tasks** (requires login; CSRF exempt):
- `GET /api/tasks` - Paginated task list (default: page=0, size=20, sort=createdAt,desc)
  - Filter params: `search`, `status` (ALL/BACKLOG/OPEN/IN_PROGRESS/IN_REVIEW/COMPLETED/CANCELLED), `overdue`, `priority`, `userId`, `tags`
  - Pagination params: `page`, `size`, `sort` (Spring Data standard)
- `GET /api/tasks/{id}` - Get task by ID
- `POST /api/tasks` - Create task (auto-assigned to caller; admins can specify `userId`)
- `PUT /api/tasks/{id}` - Update task (owner or admin; requires `version` for optimistic locking)
- `DELETE /api/tasks/{id}` - Delete task (owner or admin)
- `PATCH /api/tasks/{id}/toggle` - Toggle completion (any authenticated user)
- `GET /api/tasks/search?keyword=...` - Search by title/description
- `GET /api/tasks/search-for-dependency?projectId=&q=&excludeIds=` - Search for dependency candidates within a project (returns id, title, status)
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

**REST API — Analytics** (requires login; CSRF exempt):
- `GET /api/analytics` - Cross-project analytics data; optional `projectIds` param for filtering
- `GET /api/projects/{id}/analytics` - Project-scoped analytics data

**REST API — Project Members** (requires login):
- `GET /api/projects/{id}/members` - All enabled members of a project
- `GET /api/projects/{id}/members/assignable` - Editors and owners only (for task assignment)

**REST API — Sprints** (requires login; CSRF exempt):
- `GET /api/projects/{projectId}/sprints` - List all sprints for a project
- `POST /api/projects/{projectId}/sprints` - Create sprint (201 Created; requires edit access)
- `PUT /api/projects/{projectId}/sprints/{id}` - Update sprint (requires edit access)
- `DELETE /api/projects/{projectId}/sprints/{id}` - Delete sprint (204 No Content; nullifies task FKs)

**REST API — Recurring Task Templates** (requires login; CSRF exempt):
- `GET /api/projects/{projectId}/recurring-templates` - List recurring templates for a project
- `GET /api/projects/{projectId}/recurring-templates/{id}` - Get template details
- `POST /api/projects/{projectId}/recurring-templates` - Create template (201 Created; non-sprint projects only)
- `PUT /api/projects/{projectId}/recurring-templates/{id}` - Update template
- `POST /api/projects/{projectId}/recurring-templates/{id}/toggle` - Toggle enabled/disabled
- `DELETE /api/projects/{projectId}/recurring-templates/{id}` - Delete template (204 No Content)

**REST API — Saved Views** (requires login; CSRF exempt):
- `GET /api/views` - List saved views for current user
- `POST /api/views` - Save current filters as a named view
- `DELETE /api/views/{id}` - Delete saved view (owner or admin)

**REST API — Recent Views** (requires login; CSRF exempt):
- `GET /api/recent-views` - Current user's recently viewed items (`List<RecentViewResponse>`)

**REST API — Presence** (no authentication required):
- `GET /api/presence` - Online users (`{"users": [...], "count": n}`)

**WebSocket** (STOMP over SockJS):
- Endpoint: `ws://localhost:8080/ws`
- Subscribe: `/user/queue/notifications` — real-time notification push
- Subscribe: `/user/queue/recent-views` — recently viewed item updates (new views + title syncs)
- Subscribe: `/topic/presence` — online user list broadcast

**API Documentation** (public, no auth needed):
- `http://localhost:8080/swagger-ui.html` - Swagger UI (interactive API explorer)
- `http://localhost:8080/api-docs` - OpenAPI 3.1 spec (JSON)

**Monitoring (public, no auth needed):**
- `http://localhost:8080/actuator/health` - Application health status
- `http://localhost:8080/actuator/info` - Application info

**Dev Tools:**
- `http://localhost:8080/h2-console` - H2 database console (JDBC URL: `jdbc:h2:mem:taskdb` / Username: `sa` / Password: empty)

### Test Categories

| Test class | Type | What it tests |
|---|---|---|
| `TaskQueryServiceTest` | Unit (Mockito) | Read-only task lookups: getTaskById, getAllTasks, searchTasks |
| `TaskServiceTest` | Unit (Mockito) | Write operations: CRUD, optimistic locking, status transitions, assignment, dependency blocking |
| `TaskDependencyServiceTest` | Unit (Mockito) | Reconciliation, cycle detection (BFS), same-project validation, self-reference prevention, active blocker filtering |
| `TagServiceTest` | Unit (Mockito) | Service CRUD, audit event publishing |
| `CommentServiceTest` | Unit (Mockito) | CRUD, events, subscriber/mention ID extraction, dedup |
| `UserServiceTest` | Unit (Mockito) | CRUD, canDelete logic, enable/disable, profile update diff, role change |
| `ProjectQueryServiceTest` | Unit (Mockito) | Read-only project lookups: getProjectById, getProjectsForUser, access checks |
| `ProjectServiceTest` | Unit (Mockito) | Write operations: CRUD, member management, last-owner protection |
| `NotificationServiceTest` | Unit (Mockito) | DB-first create + WebSocket push, mark-as-read, pagination, clear |
| `OwnershipGuardTest` | Unit (Mockito) | Owner access, admin access, non-owner denial |
| `AuditFieldTest` | Unit | Factory methods, valueEquals semantics, isBlank, checklist diff, JSON round-trip |
| `AuditTemplateHelperTest` | Unit (Mockito) | Enum label resolution, URL resolution, checklist diff/format, isBlank |
| `AuditDetailsTest` | Unit | Typed diff, JSON serialization, backwards compat |
| `AuditEventListenerTest` | Unit (Mockito) | Persists audit log, skips system principal |
| `NotificationEventListenerTest` | Unit (Mockito) | Task assigned/updated/comment routing, self-exclusion, dedup |
| `WebSocketEventListenerTest` | Unit (Mockito) | Broadcasts to correct STOMP topics |
| `MentionUtilsTest` | Unit | Extract IDs, render HTML links, XSS escaping |
| `TaskSpecificationsTest` | `@DataJpaTest` | JPA Specifications: status/keyword/user/priority/overdue/tag filters |
| `AuditLogSpecificationsTest` | `@DataJpaTest` | Category/search/date-range filters, combined build |
| `UniqueValidatorTest` | `@DataJpaTest` + validation | `@Unique` annotation: uniqueness, case-insensitive, self-exclusion |
| `TaskApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: JSON CRUD, auth, validation, ownership, optimistic locking |
| `CommentApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: GET/POST/DELETE, auth, ownership |
| `TagApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: CRUD, admin-only POST/DELETE |
| `UserApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: CRUD, admin-only POST/DELETE, self-delete prevention |
| `NotificationApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: paginated list, unread count, mark-read, clear |
| `AuditApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: admin-only access |
| `PresenceApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: online users + count |
| `SecurityConfigTest` | `@SpringBootTest` + MockMvc | URL security: public/auth/admin access, CSRF behavior |
| `SprintServiceTest` | Unit (Mockito) | Sprint lifecycle: create (valid, invalid dates, overlapping), update (valid, overlapping), delete (task FK nullification) |
| `SprintQueryServiceTest` | Unit (Mockito) | Read-only sprint lookups: getSprintById, getSprintsByProject, getActiveSprint |
| `SprintApiControllerTest` | `@SpringBootTest` + MockMvc | REST API: list, create, create invalid, update, delete |
| `DemoApplicationTests` | `@SpringBootTest` | Context loads successfully |

### Database Schema

```sql
CREATE TABLE users (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(72),                       -- BCrypt hash; nullable for API-created users
    role     VARCHAR(255) NOT NULL DEFAULT 'USER', -- 'USER' or 'ADMIN' (@Enumerated STRING)
    enabled  BOOLEAN NOT NULL DEFAULT TRUE      -- disabled users cannot log in
);

CREATE TABLE projects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / ARCHIVED
    sprint_enabled BOOLEAN NOT NULL DEFAULT FALSE,     -- enables sprint features for this project
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE project_members (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    role        VARCHAR(50) NOT NULL DEFAULT 'EDITOR',  -- VIEWER / EDITOR / OWNER
    created_at  TIMESTAMP,
    UNIQUE (project_id, user_id)
);

CREATE TABLE tasks (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    version      BIGINT,                         -- @Version optimistic locking
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    status       VARCHAR(255) DEFAULT 'OPEN',   -- BACKLOG / OPEN / IN_PROGRESS / IN_REVIEW / COMPLETED / CANCELLED (@Enumerated STRING)
    priority     VARCHAR(255) DEFAULT 'MEDIUM',  -- LOW / MEDIUM / HIGH (@Enumerated STRING)
    start_date   DATE,                           -- nullable; when work begins
    due_date     DATE,                           -- nullable; overdue = non-terminal + past due
    completed_at TIMESTAMP,                      -- set automatically when status → COMPLETED
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    project_id   BIGINT NOT NULL REFERENCES projects(id),  -- every task belongs to a project
    user_id      BIGINT REFERENCES users(id),   -- nullable FK; @ManyToOne owning side
    sprint_id    BIGINT REFERENCES sprints(id), -- nullable FK; assigned sprint
    parent_id    BIGINT REFERENCES tasks(id)    -- nullable FK; subtask support
);

CREATE TABLE sprints (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    goal         VARCHAR(500),
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    project_id   BIGINT NOT NULL REFERENCES projects(id)
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

CREATE TABLE saved_views (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    name       VARCHAR(100) NOT NULL,
    filters    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE recurring_task_templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    priority        VARCHAR(255) NOT NULL DEFAULT 'MEDIUM',
    effort          SMALLINT,
    recurrence      VARCHAR(20) NOT NULL,
    day_of_week     SMALLINT,
    day_of_month    SMALLINT,
    due_days_after  SMALLINT,
    next_run_date   DATE NOT NULL,
    end_date        DATE,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    last_generated_at TIMESTAMP,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    assignee_id     BIGINT REFERENCES users(id),
    created_by      BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE recurring_template_tags (
    template_id BIGINT NOT NULL REFERENCES recurring_task_templates(id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (template_id, tag_id)
);

CREATE TABLE recent_views (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    entity_type  VARCHAR(50) NOT NULL,              -- 'TASK' or 'PROJECT'
    entity_id    BIGINT NOT NULL,
    entity_title VARCHAR(200) NOT NULL,
    viewed_at    TIMESTAMP NOT NULL,
    UNIQUE (user_id, entity_type, entity_id)
);

-- Join table: singular owning entity + plural inverse entity
CREATE TABLE task_tags (
    task_id BIGINT NOT NULL REFERENCES tasks(id),
    tag_id  BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (task_id, tag_id)
);

CREATE TABLE task_dependencies (
    blocking_task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    blocked_task_id  BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    PRIMARY KEY (blocking_task_id, blocked_task_id),
    CHECK (blocking_task_id <> blocked_task_id)  -- no self-reference
);
```

### Maven Dependencies

Key dependencies (see `pom.xml` for versions):
- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-websocket`
- `thymeleaf-extras-springsecurity7` — `sec:authorize` in templates
- `spring-boot-starter-flyway` + `flyway-database-postgresql` — DB migrations (prod)
- `postgresql` (runtime), `h2` (dev/test)
- `spring-boot-devtools` — hot reload
- `bootstrap` (WebJar 5.3.3), `htmx.org` (WebJar 2.0.4), `stomp-websocket` (WebJar 2.3.4)
- `lombok` — boilerplate reduction (not used on entities)
- `mapstruct` (1.6.3) + `mapstruct-processor` (after Lombok in annotation processor paths)
- `springdoc-openapi-starter-webmvc-ui` (3.0.2) — OpenAPI 3.1 + Swagger UI
- `spotless-maven-plugin` (2.44.5) — `google-java-format` AOSP style, auto-formats at compile phase
