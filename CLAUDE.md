# CLAUDE.md - Project Context for Claude Code

## Project Overview

**Spring Workshop** - A growing full-stack learning project demonstrating modern Spring Boot 4.0 development with both REST API and server-side rendered UI. New features are added incrementally as we explore Spring Boot patterns together.

- **Package**: `cc.desuka.demo`
- **Java Version**: 25
- **Spring Boot**: 4.0.5
- **Security**: Spring Security 7.0 (form login, BCrypt, role-based access)
- **Database**: H2 in-memory database
- **Template Engine**: Thymeleaf 3.x
- **Frontend**: Bootstrap 5.3.8 + HTMX 2.0.4 + Stimulus 3.2.2
- **Real-Time**: WebSocket + STOMP (via STOMP.js 7.3)

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

1. **REST API** (`TaskApiController`) - `/api/tasks/*` — JSON CRUD with `TaskRequest`/`TaskResponse` DTOs, MapStruct mappers for conversion
2. **Web UI** (`TaskWebController`) - `/tasks/*` — Thymeleaf + HTMX + Bootstrap

**Shared report logic** — `report/` package contains services used by multiple controllers (e.g., CSV export).

## Key Files and Structure

Detailed per-file documentation is in [CLAUDE-reference.md](CLAUDE-reference.md). Read it when you need specifics about a particular class, template, or resource file.

For database schema, available URLs, config properties, Maven dependencies, and test class listings, see the **Reference Appendix** section at the end of [CLAUDE-reference.md](CLAUDE-reference.md).

## Important Patterns and Conventions

### Thymeleaf Fragment Pattern

**Layout fragments** — every page includes four fragments from `layouts/base.html`:
- `head(title, cssFile)` — `<head>` with meta, CSS, import map
- `navbar` — nav bar only
- `chrome` — UI shell: drawers, maintenance banner, global JS templates (confirm dialog)
- `footer` — footer
- `scripts` — script tags only

Three fragment styles:
- **Bare fragment** (no HTML wrapper) — controller returns `"tasks/task-modal"` (no `::` selector)
- **Fragment in HTML wrapper** — controller returns `"tasks/task-card :: card"` (needs `::` selector)
- **Non-parameterized** — fragments read `${task}` from model context, not fragment parameters (controller return strings cannot use `${}` expressions)

### Template Comment Convention

- **Regular comments (`<!-- -->`)** — short section labels visible in DevTools (1-4 words)
- **Parser comments (`<!--/* */-->`)** — developer docs stripped by Thymeleaf (explains *why* or *how*)

### Message Source Pattern

All user-facing strings go in `messages.properties`. Key rules:
- `#{key}` in Thymeleaf, with static fallback text in the tag for IDE preview
- Parameterized: `#{pagination.showing(${start}, ${end}, ${total}, #{task.pagination.label})}`
- Conditional with message keys — use *outer* form: `th:text="${isEdit} ? #{task.edit.heading} : #{action.newTask}"`
- Validation messages in `ValidationMessages.properties` use `{key}` (curly braces, not `${key}`); `{min}`/`{max}` resolved from annotation attributes

### Thymeleaf Ternary Operator Syntax

Ternary `? :` must be **inside** `${}` for string literals: `th:classappend="${condition ? 'a' : 'b'}"`. Outside `${}` is only for `#{}` message key branches.

### `th:object` Propagation to Fragments

`th:object` on a `<form>` propagates into included fragments — `*{field}` works in `task-form :: fields` even though the `<form>` tag is in the parent template.

### Form Validation Error Preservation

On validation error, controllers must restore all user-submitted values onto the task entity before re-rendering. `TaskController` uses `restoreFormSelections()` to rebuild assignee, sprint, tags, dependencies, and checklist from form params. `populateFormModel()` skips `taskFormRequest` if Spring's `@ModelAttribute` already placed it in the model (preserving text field values). `FormMode` enum (`util/`) provides type-safe mode constants (`VIEW`, `CREATE`, `EDIT`) with `getValue()` for template strings.

### HTMX Patterns

**HTMX attribute binding** — use `th:hx-get`, `th:hx-post`, `th:hx-delete`, `th:hx-patch`, `th:hx-confirm`, etc. instead of `th:attr`. Thymeleaf resolves `th:*` for any attribute name, not just known HTML attributes. URLs use `appRoutes.*` with the builder API (`.params().build()`) — never hardcode paths in `@{}` or string concatenation. For single-quoted attribute values containing JSON, use `th:hx-vals='|{"key":"${value}"}|'`.

**Fragment return** — controller detects HTMX via `HtmxUtils.isHtmxRequest(request)` and returns a fragment instead of redirect.

**Event triggers** — `HtmxUtils.triggerEvent("taskSaved")` returns a ResponseEntity with `HX-Trigger` header. JS listens on `document.body`. Active events: `taskSaved`, `taskDeleted`.

**Single shared modal** — one modal shell in `tasks.html`, content loaded dynamically via HTMX (`hx-get` for forms) or JS data attributes (`data-task-id`, `data-task-title` for delete confirmation). Call `htmx.process(element)` after dynamic `hx-*` attribute assignment.

**Out-of-band swaps** — `hx-swap-oob="true"` for updating multiple page areas from one response. Template uses `:: list` fragment for page renders, whole-file return (no `::`) for HTMX responses with OOB spans.

### Frontend Route Configuration Pattern

All URLs centralized in `AppRoutesProperties` as `RouteTemplate` fields (defaults in Java, overridable via `application.properties`). `RouteTemplate` wraps URL templates with `{placeholder}` tokens and provides a symmetric builder API in Java, Thymeleaf, and JavaScript. Exposed two ways:
- **Templates**: `GlobalModelAttributes` → `${appRoutes}`. Use `th:hx-*` with builder API for HTMX attributes. Use `@{}` for `th:href`/`th:action` (context-path-aware).
- **JavaScript**: `FrontendConfigController` → `/config.js` → `window.APP_CONFIG.routes` (auto-discovered via reflection) and `APP_CONFIG.messages`

Two categories of routes:
- **Web routes**: `projects`, `tasks`, `audit`, `dashboard`, `analytics`, `login`, `profile` — used for page navigation
- **Parameterized web routes**: `projectDetail`, `projectSettings`, `taskDetail`, `taskEdit`, `taskToggle`, `taskNew`, `taskComments`, `taskCommentDelete` — URL templates for redirects and HTMX attributes
- **Parameterized project settings routes**: `projectArchive`, `projectMembers`, `projectMemberRole`, `projectMemberDelete`, `projectSprints`, `projectSprintsPanel`, `projectSprintDetail`, `projectSprintEdit`, `projectRecurringTemplates`, `projectRecurringDetail`, `projectRecurringEdit`, `projectRecurringGenerate`, `projectRecurringToggle` — HTMX endpoints in project settings panels
- **Parameterized admin routes**: `adminUserEdit`, `adminUserEnable`, `adminTagDelete` — admin HTMX endpoints
- **API resource routes**: `apiTasks`, `apiProjects`, `apiUsers`, `apiTags`, `apiNotifications`, `apiPresence`, `apiAnalytics`, `apiViews`, `apiAudit` — used for fetch calls and HTMX attributes
- **Parameterized API routes**: `apiProjectAnalytics`, `apiProjectSprints`, `apiProjectMembers`, `apiProjectMembersAssignable`, `apiNotificationRead`, `apiNotificationsUnreadCount`, `apiNotificationsReadAll`, `apiTaskSearchForDependency`, `apiViewById` — URL templates with `{placeholder}` tokens
- **STOMP topic routes**: `topicProject`, `topicProjectTasks`, `topicTaskComments`, `topicPresence` — WebSocket broadcast channels, also `RouteTemplate` fields (auto-exposed in `/config.js`)

**RouteTemplate builder API** (symmetric Java/Thymeleaf/JS):
- Java: `route.params("projectId", id).build()`, `route.params("k1", v1, "k2", v2).query("q", "test").build()`
- Thymeleaf: `${appRoutes.taskEdit.params('taskId', task.id).build()}`, `${appRoutes.taskNew.query('projectId', project.id).build()}`
- JS: `APP_CONFIG.routes.taskEdit.params({ taskId: id }).build()`, `route.params({ k1: v1 }).query({ q: "test" }).build()`
- `toString()` returns the raw template — works transparently in string contexts
- `RouteBuilder` is immutable — each `.params()` / `.query()` call returns a new builder instance

**Key rules:**
- Never use `${appRoutes.tasks}` inside `th:href` — it bypasses context-path handling
- Never hardcode API URLs in controllers, templates, or JS — always use `appRoutes` (Java/Thymeleaf) or `APP_CONFIG.routes` (JS)
- Always use the builder API for parameterized routes — never concatenate path segments manually
- Adding a new `RouteTemplate` field to `AppRoutesProperties` auto-exposes it in `/config.js` (reflection-based)

### Entity ID Strategy

User, Project, Task use `UUID` primary keys (`@GeneratedValue(strategy = GenerationType.UUID)`). All other entities (Comment, Sprint, Tag, ProjectMember, Notification, etc.) use `Long` (`GenerationType.IDENTITY`). Polymorphic ID columns (`AuditLog.entityId`, `RecentView.entityId`) are `String` to accommodate both types. Services that accept entity IDs for these (`RecentViewService.recordView`, `deleteByEntity`, `updateTitle`; `AuditEvent` constructor) take `Object` and call `.toString()` internally — callers pass the raw ID.

### Entity Collection Convention

`Set` (not `List`) for `@ManyToMany` and `@OneToMany` associations — avoids Hibernate multiple-bags exception and matches relational semantics. Use `LinkedHashSet` for initialization. Only use `List` with `@OrderColumn` (e.g., `checklistItems` for drag-and-drop ordering). `@OrderBy` works with both `Set` and `List` — prefer `Set` with `LinkedHashSet` to preserve iteration order.

### Task Dependency Pattern

Bidirectional `@ManyToMany` self-referential relationship on `Task`: `blocks` (owning side, `@JoinTable(task_dependencies)`) and `blockedBy` (inverse, `mappedBy`). `blocked` virtual column (`@Formula`) checks for non-terminal blockers without loading the graph. `TaskDependencyService` handles reconciliation, BFS cycle detection, same-project validation, and self-reference prevention. `BlockedTaskException` (409) blocks status transitions; `CyclicDependencyException` (422) prevents circular chains. Dependencies managed via form params (`blockedByIds`, `blocksIds`) in web UI; searchable picker via `/api/tasks/search-for-dependency`.

### Sprint Pattern

Optional per-project time-boxed iterations. Sprint status derived from date ranges: `endDate < today` = past, `startDate <= today <= endDate` = active, `startDate > today` = future. Non-overlapping date ranges enforced at service level (at most one active sprint at any time). Sprint filter sentinel: `sprintId=null` = no filter (all tasks), `0` = no sprint assigned, positive = real sprint. Default to active sprint on initial page load (non-HTMX); HTMX requests treat null as "all." Sprint filter rendered as a Bootstrap dropdown button matching other filters. Incomplete tasks stay in ended sprints — users manually move them to the next sprint or backlog (no auto-carryover). Managed via GitHub-style project settings page (sidebar nav + content panels). Sprint filter only shown on single-project views. Disabling sprints clears all task sprint assignments. Deleting a sprint unassigns its tasks. Task form dynamically loads sprint dropdown on project change via JS fetch.

### Recurring Task Pattern

`RecurringTaskTemplate` entity — separate from `Task`, only on non-sprint projects (`sprintEnabled = false`). `Recurrence` enum: DAILY, WEEKLY, BIWEEKLY, MONTHLY (implements `Translatable`). Relative due dates via nullable `dueDaysAfter` (Short). Open-ended: `endDate` nullable, runs until disabled. Optional `dayOfWeek` (1-7 ISO) for WEEKLY/BIWEEKLY, `dayOfMonth` (1-31) for MONTHLY. `@Scheduled` cron at 6 AM generates tasks from due templates. Skip missed dates: advance `nextRunDate` past today without generating multiple tasks. Auto-disable when end date reached. Silent assignee skip if user disabled. Task FK `template_id` for display only. Managed via project settings "Recurring Tasks" panel. Split "New Task" button (Bootstrap `btn-group` with `dropdown-toggle-split`) on non-sprint projects offers "New Recurring Task" linking to settings.

### Constructor Injection Pattern

Always use constructor injection, not `@Autowired` field injection.

### MapStruct Mapper Convention

All entity↔DTO conversion uses MapStruct `@Mapper(componentModel = "spring")` interfaces in the `mapper/` package — never manual `fromEntity()`/`toEntity()` methods on DTOs. Mappers are injected into controllers via constructor injection. Use `@Mapping(target = ..., ignore = true)` for fields the DTO shouldn't set (id, timestamps, associations). Use `@Mapping(source = "nested.field", target = "flatField")` for flattening. Custom logic via `default` methods or `expression = "java(...)"`. Current mappers: `TaskMapper`, `TaskFormMapper`, `ProjectMapper`, `SprintMapper`, `RecurringTaskTemplateMapper`, `TagMapper`. Exception: `SavedViewResponse.fromEntity()` remains manual (uses `SavedViewData.fromJson()`).

### `@Unique` Validation Pattern

Class-level annotation on DTOs (not entities) for field uniqueness. `@Repeatable`, `idField` defaults to `"id"` for self-exclusion on update. Entities use `@Column(unique = true)` for DB-level enforcement.

### Global String Trimming

`GlobalBindingConfig` registers `StringTrimmerEditor(true)` — trims all form-bound strings, converts blank to null. Applies to `@ModelAttribute`/`@RequestParam`/`@PathVariable`, NOT `@RequestBody` (JSON).

### User Enable/Disable Pattern

Users disabled (not deleted) when they have completed tasks, comments, or recurring task templates, or are the sole owner of a project. `UserService.canDelete(id)` checks all four conditions. Sole owners cannot be disabled either — `canDisable(id)` guards both the controller and the admin UI. `UserCommandService.cleanupBeforeDeletion(user)` handles cascade cleanup: unassigns tasks, nulls notification actors and recurring template assignees, deletes notifications, project memberships, pins, recent views, saved views, and user preferences. Disabled users can't log in, hidden from assignment dropdowns. Disabling unassigns open/in-progress tasks.

### Site Settings Pattern

`Setting` entity → `Settings` POJO (defaults + `BeanWrapper` auto-mapping) → `SettingService.load()`. `GlobalModelAttributes` exposes `${settings}`.

To add a setting: (1) field + default in `Settings.java`, (2) `KEY_*` constant matching field name, (3) `audit.field.<key>` in `messages.properties`.

**Maintenance banner dismiss** — `maintenanceBannerVersion` setting bumped (timestamp) on banner text change. Inline script in `base.html` compares `bannerDismissed` cookie against rendered version; shows banner only if unmatched. Dismiss stores cookie. New banner text = new version = banner reappears for all users.

### User Preferences Pattern

Mirrors Site Settings: `UserPreference` entity → `UserPreferences` POJO → `UserPreferenceService.load(userId)`. `GlobalModelAttributes` exposes `${userPreferences}`. Current prefs: `taskView` (cards/table/calendar/board), `defaultUserFilter` (mine/all).

### Profile Controller Pattern

Three controllers for User concerns: `UserController` (`/users` — public list), `ProfileController` (`/profile` — self-service), `UserManagementController` (`/admin/users` — admin CRUD).

### Theme System

Custom color schemes via `data-theme` attribute on `<html>`. Palette tokens in `theme.css` mapped to Bootstrap `--bs-*` variables. Design tokens (motion, shadows, radius) and shared refinements (typography, forms, cards, dropdowns) also live in `theme.css` under `[data-theme]`.

Four themes: `default` (stock Bootstrap), `workshop` (cerulean + violet), `notebook` (warm cream + terracotta), `titanium` (industrial precision + steel blue, zero border-radius). All custom palettes include `--theme-info` for In Review status — Bootstrap's stock Info is too close to blue primaries.

To add a theme: (1) `THEME_*` constant in `Settings.java`, (2) `[data-theme="name"]` palette in `theme.css`, (3) `ThemeOption` in `SettingsController.THEMES`, (4) `admin.settings.theme.<name>.{name,description}` in `messages.properties`.

For detailed color palettes, WCAG contrast ratios, design tokens, component patterns, and anti-patterns, see [CSS-GUIDE.md](CSS-GUIDE.md).

### Security Authorization Patterns

Three layers — any can block access:
1. **URL-level (SecurityConfig)** — role-based (`hasRole(ADMIN)`, `.authenticated()`)
2. **Controller-level** — `ProjectAccessGuard` (project membership: VIEWER/EDITOR/OWNER, admin bypasses) and `OwnershipGuard` (entity ownership for comments)
3. **Template-level** — UI visibility only, not a security boundary. `${#auth}` custom expressions, `sec:authorize` Spring Security dialect

### OwnedEntity Pattern

Entities with an owner implement `OwnedEntity` (`getUser()`, null = unassigned). Unassigned-entity rules are business decisions in controllers/templates, not in generic auth utilities. Implemented by `Task` and `Comment`.

### Translatable Enum Pattern

Enums implement `Translatable.getMessageKey()`. Templates use `#{${enum.messageKey}}`. `Messages.get(Translatable)` for Java-side resolution. Implemented by: `TaskStatus`, `Priority`, `ProjectRole`, `ProjectStatus`, `Role`, `Recurrence`. `TaskStatusFilter` is excluded (internal query param enum).

**Enum presentation methods** — `TaskStatus` and `Priority` expose styling metadata as methods: `getCssClass()` (badge bg + text color), `getBtnClass()` (button variant), `getIcon()` (Bootstrap Icon class), `getChartColor()` (hex for Chart.js), `getBorderClass()` / `getTextClass()` (TaskStatus only). Templates use `${status.cssClass}`, `${status.icon}`, etc. JS-side equivalent: `APP_CONFIG.enums.taskStatus` and `APP_CONFIG.enums.priority` (auto-generated from enum methods in `FrontendConfigController`). `cssClass` always includes explicit text color (e.g., `bg-warning text-dark`) for consistency.

### Confirm Dialog Pattern

`showConfirm(options, onConfirm)` in `lib/confirm.js` — Bootstrap modal replacing `window.confirm()`. Clones `<template id="confirm-dialog-template">` from `base.html` (Thymeleaf processes `#{...}` for i18n), sets dynamic content via `data-confirm-*` attribute hooks, destroyed on hide. HTMX integration via `htmx:confirm` interception with `data-confirm-*` attributes.

### Searchable Select Component

`<searchable-select>` Web Component (`js/components/searchable-select.js` + `css/components/searchable-select-bootstrap5.css`). No Shadow DOM — uses Bootstrap classes from page stylesheet.

**Three modes:**
- **Local** — static `<option>` children, client-side filter
- **Remote prefetch** — `src` + `prefetch` attr: fetch once on open, filter client-side from cache
- **Remote server search** — `src` without `prefetch`: debounced fetch per keystroke via `query-param`

**Public API** — consumers must NOT access `_`-prefixed internals:
- **Properties:** `value` (get/set), `fetchFn` (get/set — `async (query, signal) => Array`)
- **Methods:** `reset()`, `clear()`, `setValue(v, text)`, `getValue()`, `setSrc(url)`, `setOptions([{value, text, ...extra}])`, `enable()`, `disable()`
- **Attributes:** `name`, `placeholder`, `disabled`, `readonly`, `src`, `prefetch`, `value-field`, `text-field`, `query-param`, `debounce`
- **Events:** `change` with `{ detail: { value, text, data } }` — `data` is the original item object (API response for remote, input object for `setOptions`, `dataset` object for static `<option data-*>`, or `undefined`)

**`fetchFn`** overrides `src`-based fetching. Component handles debouncing, abort signals, loading/error states, and mapping via `value-field`/`text-field`. Dev handles URL construction, request method, auth.

**`readonly` vs `disabled`:** Both prevent interaction. `readonly` submits the value (white bg, dashed border). `disabled` does not submit (sunken bg, transparent border). Use `disabled` for view-only mode (matches other form controls); use `readonly` when the form submits but a field should be locked.

**Keyboard vs mouse highlight:** `ss-keyboard-nav` class suppresses `:hover` styling during arrow-key navigation. `overflow: hidden` on `.card` clips absolutely-positioned dropdowns — use `.card-clip` only on cards needing header clipping, not on cards containing searchable-select.

### WebSocket + STOMP Pattern

- `/ws` endpoint (SockJS fallback), broker on `/topic` (broadcast) and `/queue` (user-specific)
- `PresenceService` — `ConcurrentHashMap` tracks online users by session
- `NotificationService.create()` — DB-first, then pushes via `SimpMessagingTemplate`
- Live update banners — per-project scoped via `/topic/projects/{projectId}/tasks`. Clients subscribe only to their accessible projects (passed via `<meta name="_wsProjectIds">`). Self-filtering via `<meta name="_userId">`. Regular users only see alerts for their projects; admins see all active projects.
- Notification event bus (`notifications.js`) — custom DOM events (`notification:received/read/allRead/cleared`) decouple producers from consumers

**Payload convention:** `*Response` in `dto/` for data returned to clients, `*Event` in `event/` for push-only broadcasts.

### Event-Driven Side Effects Pattern

Services publish domain events via `ApplicationEventPublisher`. Four listeners:
- `AuditEventListener` (audit/) — persists audit logs (DB)
- `NotificationEventListener` (event/) — routes notifications (DB + WebSocket)
- `WebSocketEventListener` (event/) — broadcasts ephemeral events (WebSocket only)
- `RecentViewEventListener` (event/) — syncs titles in recently viewed items (DB + per-user WebSocket)

Domain events: `TaskAssignedEvent`, `TaskUpdatedEvent`, `ProjectUpdatedEvent`, `CommentAddedEvent` (`actor` field). WebSocket push events: `TaskPushEvent`, `ProjectPushEvent`, `CommentChangeEvent` (`userId` field), `PinnedItemPushEvent`, `RecentViewPushEvent` (per-user via `/user/queue/*`).

Services never depend on `SimpMessagingTemplate`, `NotificationService`, or `MessageSource` — only `eventPublisher`. Exception: `ScheduledTaskService` calls `NotificationService` directly (cron doesn't fit event pattern).

### Package-by-Concern Organization

Feature packages for internal infrastructure: `audit/`, `event/`, `presence/`, `report/`. Controllers always stay in `controller/` — never move them into feature packages.

### Project Access Pattern

Every task belongs to a project. `ProjectAccessGuard` enforces view/edit/owner access at controller level. Cross-project views use `accessibleProjectIds` (`null` = admin bypass, non-null = filtered list).

### Cross-Service Dependency Rule

Services use their own repository, delegate to other services for other domains. Query services (`TaskQueryService`, `ProjectQueryService`, `CommentQueryService`) separate reads from writes and break circular dependencies. Write services (`TaskService`, `ProjectService`) inject the corresponding query service for internal reads. Command services handle cross-cutting write operations needed by multiple services — extracted to break circular dependencies: `TaskCommandService` (bulk task unassignment), `UserCommandService` (user deletion cascade cleanup). Command services may access foreign repositories directly when the owning service would create a cycle.

### Transactional Boundaries

- **OSIV disabled** — lazy associations must load within `@Transactional` or via `@EntityGraph`
- **Class-level `@Transactional`** on write services; method-level for isolated writes in read-only services
- **`@EntityGraph`** on repo methods used outside service transactions (MapStruct mapping). Only include associations callers access
- **`@TransactionalEventListener`** on all event listeners (fires after commit). `AuditEventListener` uses `REQUIRES_NEW`. Exception: `AuthAuditListener` uses `@EventListener` because Spring Security auth events fire outside managed transactions

### SecurityUtils Pattern

Central utility: `SecurityUtils.getCurrentUser()`, `.getCurrentUserDetails()`, `.getUserFrom(principal)`. All other classes delegate here.

### CSRF Token Pattern for HTMX

`utils.js` reads `<meta>` CSRF tags and adds token header via `htmx:configRequest`. REST API (`/api/**`) is CSRF-exempt.

### JS Fetch Error Handling Pattern

All `fetch()` calls must chain `.then(requireOk)` before parsing the response. `requireOk(response)` (defined in `utils.js`) throws on non-2xx status. Catch handlers should log via `console.error` — never silently swallow errors (empty `.catch(() => {})`).

### Audit Event Categories

`AuditEvent.CATEGORIES` is the single source of truth. Every constant must be prefixed with a category. To add: add prefix to `CATEGORIES`, define constants, add `admin.audit.filter.<name>` message key.

### CSV Export Pattern

`CsvWriter.write(response, filename, headers, items, rowMapper)` — generic, handles escaping and headers. Export endpoint uses same filters as list, with `Pageable.unpaged(sort)`.

### ProblemDetail Error Response Pattern

REST API uses RFC 9457 `ProblemDetail` via `ApiExceptionHandler` (scoped to `controller.api`). `ApiExceptionHandler` injects `Messages` for i18n-aware error detail strings (e.g., `PinLimitReachedException` → 409 with localized message from `messages.properties`). Web UI errors handled separately by `WebExceptionHandler`.

### @Mention Pattern

Tribute.js autocomplete on `[data-mention]` elements. Project-scoped via `data-project-id` attr — only shows project members (fetched once, cached per input). Falls back to all users when no project context. Client encodes `@[Name](userId:N)`, server `MentionUtils` extracts IDs and renders HTML. Mentioned users get `COMMENT_MENTIONED` notification and subscribe to conversation. To add mentions to a new field: add `data-mention` attribute (and `data-project-id` if project-scoped).

### Shared Task Layout Pattern

`task-layout.html` — two-column layout shared by modal and full page. Comment input uses `<div>` (not `<form>`) to avoid nested forms; HTMX `hx-post` with `hx-include` replaces inner form.

### Checklist Pattern

`@ElementCollection` with `@OrderColumn`. `ChecklistItem` embeddable (`text` + `checked`). Form binding via parallel arrays. Drag-and-drop reordering via native HTML DnD API. Audited in `toAuditSnapshot()` with `[x]/[ ]` format.

### Activity Timeline Pattern

`TimelineService` merges comments and audit history into chronological `TimelineEntry` stream. `task-activity.html` uses dual-usage template pattern for page includes vs HTMX responses.

### Recently Viewed Pattern

Controllers call `RecentViewService.recordView()` directly — not event-driven (single-purpose action, not a domain event). Title sync uses `@TransactionalEventListener` on `TaskUpdatedEvent`/`ProjectUpdatedEvent` via `RecentViewEventListener`, with `REQUIRES_NEW` propagation (required because `@TransactionalEventListener(AFTER_COMMIT)` runs outside the original transaction, so `@Modifying` queries need their own).

**Data model:** `RecentView` entity stores (user, entityType, entityId, entityTitle, viewedAt). Max 10 entries per user, trimmed on insert. API endpoint `GET /api/recent-views` for JS initial fetch.

**WebSocket push:** Per-user delivery to `/user/queue/recent-views` via `SimpMessagingTemplate`. `RecentViewResponse` includes `titleOnly` flag: `false` = new view (JS prepends to top), `true` = title-only sync for other users (JS updates text in place without reordering).

**HTMX guard:** `ProjectController.showProject()` skips `recordView` for HTMX requests — those are task list refreshes (`doSearch()`), not actual project views. `TaskController.showTask()` does NOT skip — HTMX loads the task modal, which IS viewing the task.

**`ProjectUpdatedEvent`** includes `actor` field — needed for `updateTitle()` to bump `viewedAt` only for the editing user.

**Frontend:** `recent-views.js` fetches via API after WebSocket connect, then live-updates via WebSocket. Vertical left-side drawer tab (CSS `writing-mode: vertical-lr`), lg+ only. Styles in `base.css`.

### Pinned Items Pattern

User-initiated bookmarking of projects and tasks. Mirrors RecentView architecture (entity, service, API, WebSocket, Stimulus controller) but explicit (pin/unpin toggle) rather than automatic.

**Data model:** `PinnedItem` entity (implements `OwnedEntity`) stores (user, entityType, entityId, entityTitle, pinnedAt, sortOrder). `PinnedItemRepository` with sort variants and `deleteByUserAndProject` join query. Limit configurable via `UserPreferences.pinnedLimit` (default 30).

**API:** `GET /api/pins`, `POST /api/pins` (201 or 409 if limit), `DELETE /api/pins/{id}` (ownership guard), `PATCH /api/pins/reorder`.

**WebSocket push:** `/user/queue/pins` — `PinnedItemResponse` payload built via factory methods (`pinned()`, `titleUpdate()`, `deleted()`), published via `PinnedItemPushEvent`. `PinnedItemEventListener` handles title sync on `TaskUpdatedEvent`/`ProjectUpdatedEvent`.

**Sort options** (user preference `pinnedSortOrder`): pinnedDate (default), name, manual (drag-and-drop via sortOrder).

**Pin icon fragment:** `fragments/pin-icon.html` — dispatches `pin:toggle` custom event on `document`. Shown in card view, table view, project grid, project page header. Hidden on small screens (`d-none d-lg-inline`).

**Drawer:** `fragments/pinned-items.html` — left side below recent views in shared `#left-drawers` container. Dynamic 1-2 column grid based on pin count. `drawer:opened` event ensures only one drawer open at a time.

**Cleanup:** Task deleted / project deleted → `deleteByEntity`. Member removed → `deleteByUserAndProject` (join query, no task IDs needed from caller). User deleted → `deleteByUserId`. Archive keeps pins.

**Color:** `--pin-color` CSS custom property (default `#d63384` pink). Themes can override.

### Analytics Pattern

Chart.js 4.5.1 (via WebJar) renders 7 charts: status/priority doughnuts, workload stacked bar, burndown/velocity lines, overdue bar, effort-by-assignee horizontal bar. Velocity chart includes optional effort line (dual Y-axis) when effort data exists. Thymeleaf page is a shell — JS fetches JSON from REST API and renders client-side. Shared template for both cross-project (`/analytics`) and project-scoped (`/projects/{id}/analytics`) views via `<meta name="_analyticsApi">`. `AnalyticsRepository` uses `EntityManager` with dynamic JPQL for aggregate projections (avoids triplicating queries). Cross-project filter: checkboxes per project + Select All; server intersects requested `projectIds` with accessible projects for security. Sprint-scoped analytics pass `sprintId` to repository queries; burndown uses sprint date range instead of rolling 30-day window.

### Frontend JavaScript Architecture (Stimulus + ES Modules)

The frontend uses **Stimulus controllers** with **browser-native import maps** (no build tools). HTMX handles server communication; Stimulus organizes client-side behavior.

**File structure:**
```
static/js/
├── controllers/          # Stimulus controllers (all interactive behavior)
│   ├── tasks/            # Multi-context: tasks--list, tasks--form, etc.
│   ├── notifications/    # Multi-context: notifications--badge, notifications--page
│   └── *.js              # Single-context: analytics, audit, presence, etc.
├── lib/                  # Shared ES modules (utilities, no business logic)
├── components/           # Standalone Web Components (no framework deps)
└── application.js        # Entry point: side-effect imports + controller registration
```

#### Backend → Frontend Communication Rules

- **HTMX success toasts** — Use `HtmxUtils.toastTrigger(message, ToastType.SUCCESS)` as `HX-Trigger` response header. Never render `<script>showToast(...)</script>` in HTMX fragments.
- **Post-redirect flash toasts** — Render `<div data-flash-toast th:data-message="#{key}">`. `application.js` scans on page load. No inline JS needed.
- **Server data to JS** — Use Stimulus value attributes (`th:data-controller-name-value="${value}"`). Never use `window.GLOBAL = ...` or `<meta>` tags for controller-specific data. Exception: `APP_CONFIG` stays as a window global (dynamically generated, read everywhere).
- **Destructive confirmations** — Use `hx-confirm` + `data-confirm-*` attributes. Never use `showConfirm` from inline scripts.
- **CSRF** — Handled globally by `lib/htmx-csrf.js`. No per-page CSRF handling.
- **`<template>` for JS-cloned markup** — When JS needs to add DOM elements, put a `<template id="...">` in the Thymeleaf file and clone via `template.content.firstElementChild.cloneNode(true)`. Thymeleaf processes `#{...}` inside `<template>` for i18n. JS populates dynamic values after cloning. Global templates (any page) go in the `chrome` fragment in `base.html`; feature templates go in the page that loads the controller. `<template>` is NOT a fragment — don't put in `fragments/`. Used by checklist items, dependency items, and confirm dialog.

#### Frontend JS Rules

- **Stimulus controllers** — All interactive behavior. One controller per concern. Flat file for single-context (`analytics_controller.js`), folder for multi-context (`tasks/list_controller.js`).
- **`lib/` modules** — Shared utilities. Single responsibility. Imported by controllers, never by templates.
- **Side-effect imports** — Global listeners in dedicated `lib/` modules imported in `application.js` (e.g., `lib/htmx-csrf.js`, `lib/flash-toast.js`). Never inline global listeners in `application.js`.
- **`components/`** — Standalone Web Components. Must work without `lib/` imports or `APP_CONFIG`.
- **No `window.*` globals** — Controllers don't expose on window. Only exception: `window.Stimulus` for debugging.
- **Cross-controller communication** — Custom DOM events (`tasks:refresh`, `tasks:switch-view`). Never import one controller from another. Convention: `feature:action` naming.

#### Template JS Rules

- **No `onclick`/`onchange`/`ondrag*`** — Use `data-action="event->controller#method"`. Use `:prevent`/`:stop` suffixes.
- **No inline `<script>` that imports modules** — Inline scripts must be self-contained: no `import`, no `window.*` globals, no shared state. Reserved for page-specific pure client-side UI only (tab navigation, form field toggles). Keep under 30 lines. Exception: `admin/users.html` imports `showConfirm` (legacy — complex confirm dialog with form input).
- **Import map in `<head>`** — All `lib/` modules and `controllers/` registered in import map in `base.html` with Thymeleaf `@{}` URLs for content-based cache versioning. Adding a new lib module or controller requires adding it there.

#### Naming Conventions

- **Controller files** — `snake_case_controller.js` → `data-controller="kebab-case"`. Folders: `tasks/list_controller.js` → `tasks--list`.
- **Lib files** — `kebab-case.js` (e.g., `htmx-csrf.js`, `flash-toast.js`).
- **Custom events** — `feature:action` (e.g., `tasks:refresh`, `mention:clear`).

### CSS Organization

- `base.css` for every page, `theme.css` for theme palettes + design tokens + Bootstrap overrides, `tasks.css` for task pages (via `head(title, cssFile)` fragment parameter)
- Task JS files in `static/js/tasks/` subfolder
- Self-hosted DM Sans font in `static/fonts/` (WOFF2, variable weight 400–600)
- Bootstrap active state overrides: use CSS custom properties (`--bs-btn-active-bg`). Bootstrap utilities use `!important` — override with `!important`
- See [CSS-GUIDE.md](CSS-GUIDE.md) for full design reference

## Development Workflow

### Running the Application

```bash
./mvnw spring-boot:run                    # http://localhost:8080
./mvnw spring-boot:run -Pdebug           # with remote debugging (port 5005)
./mvnw test                               # 302 tests, 32 test classes
./mvnw compile                            # regenerate MapStruct after mapper changes
```

Dev credentials: `alice.johnson@example.com` / `password` (admin), `bob.smith@example.com` / `password` (regular)

### MapStruct Workflow

DevTools hot-reloads most changes. MapStruct mapper changes require `./mvnw compile` in a second terminal — DevTools picks up the new classes automatically.

### Spring Profiles

- **`dev`** (default) — H2, demo data seeding, SQL logging, H2 console
- **`prod`** — PostgreSQL, Flyway migrations, no demo data, Swagger UI disabled
- **`test`** — H2 (`testdb`), no SQL logging, Flyway disabled

Profile-gated: `DataLoader` (dev), `DevH2Config` (dev), `DevSecurityConfig` (dev), login page demo credentials (dev)

### Test Patterns

- **`@SpringBootTest` + `@AutoConfigureMockMvc`** for controller/security tests (full context, `@MockitoBean` services). Preferred over `@WebMvcTest` — Spring Security 7 needs full filter chain
- **`@DataJpaTest`** for repo/spec tests. Add `@Import(ValidationAutoConfiguration.class)` for validation
- **Mock auth**: `SecurityMockMvcRequestPostProcessors.user(CustomUserDetails)`
- **`@MockitoBean` for `ApplicationEventPublisher`**: use `verify(eventPublisher).publishEvent(any(AuditEvent.class))` (not bare `any()`)

## Common Issues and Solutions

| Problem | Solution |
|---|---|
| `Could not parse as expression` | Ternary must be inside `${}`: `"${cond ? 'a' : 'b'}"` not `"${cond} ? 'a' : 'b'"` |
| `Parameters in a view specification must be named` | Don't use `${}` in controller return strings; put data in model |
| HTMX dynamic attribute not firing | Call `htmx.process(element)` after `setAttribute` |
| HTMX returns full page instead of fragment | Check controller uses `HtmxUtils.isHtmxRequest(request)` |
| Bootstrap active state color ignored | Use `!important` — Bootstrap utilities use `!important` |

## Git Workflow

- `main` — production-ready | `feature/*` — new features | `fix/*` — bug fixes | `refactor/*` — improvements

Always ask before committing. Never auto-commit.
