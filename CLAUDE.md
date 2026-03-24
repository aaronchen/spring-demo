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

1. **REST API** (`TaskApiController`) - `/api/tasks/*` — JSON CRUD with `TaskRequest`/`TaskResponse` DTOs, `TaskMapper` (MapStruct) for conversion
2. **Web UI** (`TaskWebController`) - `/tasks/*` — Thymeleaf + HTMX + Bootstrap

**Shared report logic** — `report/` package contains services used by multiple controllers (e.g., CSV export).

## Key Files and Structure

Detailed per-file documentation is in [CLAUDE-reference.md](CLAUDE-reference.md). Read it when you need specifics about a particular class, template, or resource file.

For database schema, available URLs, config properties, Maven dependencies, and test class listings, see the **Reference Appendix** section at the end of [CLAUDE-reference.md](CLAUDE-reference.md).

## Important Patterns and Conventions

### Thymeleaf Fragment Pattern

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

### HTMX Patterns

**Fragment return** — controller detects HTMX via `HtmxUtils.isHtmxRequest(request)` and returns a fragment instead of redirect.

**Event triggers** — `HtmxUtils.triggerEvent("taskSaved")` returns a ResponseEntity with `HX-Trigger` header. JS listens on `document.body`. Active events: `taskSaved`, `taskDeleted`.

**Single shared modal** — one modal shell in `tasks.html`, content loaded dynamically via HTMX (`hx-get` for forms) or JS data attributes (`data-task-id`, `data-task-title` for delete confirmation). Call `htmx.process(element)` after dynamic `hx-*` attribute assignment.

**Out-of-band swaps** — `hx-swap-oob="true"` for updating multiple page areas from one response. Template uses `:: list` fragment for page renders, whole-file return (no `::`) for HTMX responses with OOB spans.

### Frontend Route Configuration Pattern

Routes defined in `application.properties`, exposed two ways:
- **Templates**: `GlobalModelAttributes` → `${appRoutes}`. Use `@{}` for `th:href`/`th:action` (context-path-aware). Use `${appRoutes.tasks + ...}` only for HTMX `th:attr` values where `@{}` doesn't work.
- **JavaScript**: `FrontendConfigController` → `/config.js` → `window.APP_CONFIG.routes` and `APP_CONFIG.messages`

**Key rule:** never use `${appRoutes.tasks}` inside `th:href` — it bypasses context-path handling.

### Constructor Injection Pattern

Always use constructor injection, not `@Autowired` field injection.

### `@Unique` Validation Pattern

Class-level annotation on DTOs (not entities) for field uniqueness. `@Repeatable`, `idField` defaults to `"id"` for self-exclusion on update. Entities use `@Column(unique = true)` for DB-level enforcement.

### Global String Trimming

`GlobalBindingConfig` registers `StringTrimmerEditor(true)` — trims all form-bound strings, converts blank to null. Applies to `@ModelAttribute`/`@RequestParam`/`@PathVariable`, NOT `@RequestBody` (JSON).

### User Enable/Disable Pattern

Users disabled (not deleted) when they have completed tasks or comments. `UserService.canDelete(id)` checks. Disabled users can't log in, hidden from assignment dropdowns. Disabling unassigns open/in-progress tasks.

### Site Settings Pattern

`Setting` entity → `Settings` POJO (defaults + `BeanWrapper` auto-mapping) → `SettingService.load()`. `GlobalModelAttributes` exposes `${settings}`.

To add a setting: (1) field + default in `Settings.java`, (2) `KEY_*` constant matching field name, (3) `audit.field.<key>` in `messages.properties`.

### User Preferences Pattern

Mirrors Site Settings: `UserPreference` entity → `UserPreferences` POJO → `UserPreferenceService.load(userId)`. `GlobalModelAttributes` exposes `${userPreferences}`. Current prefs: `taskView` (cards/table/calendar/board), `defaultUserFilter` (mine/all).

### Profile Controller Pattern

Three controllers for User concerns: `UserController` (`/users` — public list), `ProfileController` (`/profile` — self-service), `UserManagementController` (`/admin/users` — admin CRUD).

### Theme System

Custom color schemes via `data-theme` attribute on `<html>`. Palette tokens in `theme.css` mapped to Bootstrap `--bs-*` variables.

To add a theme: (1) `THEME_*` constant in `Settings.java`, (2) `[data-theme="name"]` palette in `theme.css`, (3) `ThemeOption` in `SettingsController.THEMES`, (4) `admin.settings.theme.<name>.{name,description}` in `messages.properties`.

### Security Authorization Patterns

Three layers — any can block access:
1. **URL-level (SecurityConfig)** — role-based (`hasRole(ADMIN)`, `.authenticated()`)
2. **Controller-level** — `ProjectAccessGuard` (project membership: VIEWER/EDITOR/OWNER, admin bypasses) and `OwnershipGuard` (entity ownership for comments)
3. **Template-level** — UI visibility only, not a security boundary. `${#auth}` custom expressions, `sec:authorize` Spring Security dialect

### OwnedEntity Pattern

Entities with an owner implement `OwnedEntity` (`getUser()`, null = unassigned). Unassigned-entity rules are business decisions in controllers/templates, not in generic auth utilities. Implemented by `Task` and `Comment`.

### Translatable Enum Pattern

Enums implement `Translatable.getMessageKey()`. Templates use `#{${enum.messageKey}}`. `Messages.get(Translatable)` for Java-side resolution. Implemented by: `TaskStatus`, `Priority`, `ProjectRole`, `ProjectStatus`, `Role`. `TaskStatusFilter` is excluded (internal query param enum).

### Confirm Dialog Pattern

`showConfirm(options, onConfirm)` in `utils.js` — Bootstrap modal replacing `window.confirm()`. Created fresh per call, destroyed on hide. HTMX integration via `htmx:confirm` interception with `data-confirm-*` attributes.

### WebSocket + STOMP Pattern

- `/ws` endpoint (SockJS fallback), broker on `/topic` (broadcast) and `/queue` (user-specific)
- `PresenceService` — `ConcurrentHashMap` tracks online users by session
- `NotificationService.create()` — DB-first, then pushes via `SimpMessagingTemplate`
- Live update banners — clients subscribe to `/topic/tasks`, show banner on changes by other users, "Refresh" re-fetches. Self-filtering via `<meta name="_userId">`
- Notification event bus (`notifications.js`) — custom DOM events (`notification:received/read/allRead/cleared`) decouple producers from consumers

**Payload convention:** `*Response` in `dto/` for data returned to clients, `*Event` in `event/` for push-only broadcasts.

### Event-Driven Side Effects Pattern

Services publish domain events via `ApplicationEventPublisher`. Three listeners:
- `AuditEventListener` (audit/) — persists audit logs (DB)
- `NotificationEventListener` (event/) — routes notifications (DB + WebSocket)
- `WebSocketEventListener` (event/) — broadcasts ephemeral events (WebSocket only)

Domain events: `TaskAssignedEvent`, `TaskUpdatedEvent`, `CommentAddedEvent` (`actor` field). WebSocket events: `TaskChangeEvent`, `CommentChangeEvent` (`userId` field).

Services never depend on `SimpMessagingTemplate`, `NotificationService`, or `MessageSource` — only `eventPublisher`. Exception: `ScheduledTaskService` calls `NotificationService` directly (cron doesn't fit event pattern).

### Package-by-Concern Organization

Feature packages for internal infrastructure: `audit/`, `event/`, `presence/`, `report/`. Controllers always stay in `controller/` — never move them into feature packages.

### Project Access Pattern

Every task belongs to a project. `ProjectAccessGuard` enforces view/edit/owner access at controller level. Cross-project views use `accessibleProjectIds` (`null` = admin bypass, non-null = filtered list).

### Cross-Service Dependency Rule

Services use their own repository, delegate to other services for other domains. `TaskQueryService` and `CommentQueryService` break circular dependencies (read-only + `unassignTasks()`).

### Transactional Boundaries

- **OSIV disabled** — lazy associations must load within `@Transactional` or via `@EntityGraph`
- **Class-level `@Transactional`** on write services; method-level for isolated writes in read-only services
- **`@EntityGraph`** on repo methods used outside service transactions (MapStruct mapping). Only include associations callers access
- **`@TransactionalEventListener`** on all event listeners (fires after commit). `AuditEventListener` uses `REQUIRES_NEW`. Exception: `AuthAuditListener` uses `@EventListener` because Spring Security auth events fire outside managed transactions

### SecurityUtils Pattern

Central utility: `SecurityUtils.getCurrentUser()`, `.getCurrentUserDetails()`, `.getUserFrom(principal)`. All other classes delegate here.

### CSRF Token Pattern for HTMX

`utils.js` reads `<meta>` CSRF tags and adds token header via `htmx:configRequest`. REST API (`/api/**`) is CSRF-exempt.

### Audit Event Categories

`AuditEvent.CATEGORIES` is the single source of truth. Every constant must be prefixed with a category. To add: add prefix to `CATEGORIES`, define constants, add `admin.audit.filter.<name>` message key.

### CSV Export Pattern

`CsvWriter.write(response, filename, headers, items, rowMapper)` — generic, handles escaping and headers. Export endpoint uses same filters as list, with `Pageable.unpaged(sort)`.

### ProblemDetail Error Response Pattern

REST API uses RFC 9457 `ProblemDetail` via `ApiExceptionHandler` (scoped to `controller.api`). Web UI errors handled separately by `WebExceptionHandler`.

### @Mention Pattern

Tribute.js autocomplete on `[data-mention]` elements. Client encodes `@[Name](userId:N)`, server `MentionUtils` extracts IDs and renders HTML. Mentioned users get `COMMENT_MENTIONED` notification and subscribe to conversation. To add mentions to a new field: add `data-mention` attribute.

### Shared Task Layout Pattern

`task-layout.html` — two-column layout shared by modal and full page. Comment input uses `<div>` (not `<form>`) to avoid nested forms; HTMX `hx-post` with `hx-include` replaces inner form.

### Checklist Pattern

`@ElementCollection` with `@OrderColumn`. `ChecklistItem` embeddable (`text` + `checked`). Form binding via parallel arrays. Drag-and-drop reordering via native HTML DnD API. Audited in `toAuditSnapshot()` with `[x]/[ ]` format.

### Activity Timeline Pattern

`TimelineService` merges comments and audit history into chronological `TimelineEntry` stream. `task-activity.html` uses dual-usage template pattern for page includes vs HTMX responses.

### CSS Organization

- `base.css` for every page, `tasks.css` for task pages (via `head(title, cssFile)` fragment parameter)
- Task JS files in `static/js/tasks/` subfolder
- Bootstrap active state overrides: use CSS custom properties (`--bs-btn-active-bg`). Bootstrap utilities use `!important` — override with `!important`

## Development Workflow

### Running the Application

```bash
./mvnw spring-boot:run                    # http://localhost:8080
./mvnw spring-boot:run -Pdebug           # with remote debugging (port 5005)
./mvnw test                               # 207 tests, 23 test classes
./mvnw compile                            # regenerate MapStruct after mapper changes
```

Dev credentials: `alice.johnson@example.com` / `password` (admin), `bob.smith@example.com` / `password` (regular)

### MapStruct Workflow

DevTools hot-reloads most changes. MapStruct mapper changes require `./mvnw compile` in a second terminal — DevTools picks up the new classes automatically.

### Spring Profiles

- **`dev`** (default) — H2, demo data seeding, SQL logging, H2 console
- **`prod`** — PostgreSQL, Flyway migrations, no demo data, Swagger UI disabled
- **`test`** — H2 (`testdb`), no SQL logging, Flyway disabled

Profile-gated: `DataLoader` (dev), `H2DevConfig` (dev), `DevSecurityConfig` (dev)

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

## Future Enhancement Ideas

- Add reminders for due dates
- Implement dark mode toggle
- Export tasks to CSV/PDF
- Implement recurring tasks
