# Spring Workshop

A growing full-stack application built as a hands-on learning project for Spring Boot 4.0, featuring both a REST API and an interactive web UI powered by Thymeleaf and HTMX. New features and patterns are added as we continue exploring Spring Boot together.

**Live Demo:** <a href="https://demo.desuka.cc" target="_blank">demo.desuka.cc</a>

| Role | Email | Password |
|------|-------|----------|
| Admin | `alice.johnson@example.com` | `password` |
| User | `bob.smith@example.com` | `password` |

## Features

### Projects & Team Collaboration
- **Project Management** - Create, edit, archive, and delete projects; every task belongs to a project
- **Role-Based Project Access** - Three project roles: VIEWER (read-only), EDITOR (read/write tasks), OWNER (full control including settings and member management)
- **Team Members** - Add/remove project members with role assignment; last owner protection prevents accidental lockout
- **Project-Scoped Views** - Task lists, dashboards, and API endpoints scoped to accessible projects; admins see all

### Authentication & Authorization
- **Form Login** - Email + password authentication with BCrypt hashing
- **Self-Registration** - New users can sign up; default role is USER
- **Role-Based Access** - Two system roles: USER (standard) and ADMIN (elevated privileges); three project roles: VIEWER, EDITOR, OWNER
- **Project Access Control** - `ProjectAccessGuard` enforces view/edit/owner access per project; admin bypass for all projects
- **Admin Panel** - Modal-based user management (create/edit/delete/disable/enable) at `/admin/users`; tag management at `/admin/tags` (admin only)
- **Audit Logging** - All entity changes, project member mutations, and auth events logged; admin audit page with search/filters at `/admin/audit`
- **Admin Settings** - Configurable site name, registration toggle, maintenance banner, notification purge age, and theme picker at `/admin/settings`
- **User Profile** - Self-service account management at `/profile`: edit name/email, change password, and configure preferences (task view mode, default user filter, due date reminders)
- **Auth-Aware UI** - Navbar shows user info, role badge, and role-appropriate links

### Web Interface
- **Responsive Design** - Mobile-friendly UI built with Bootstrap 5
- **Card, Table, Calendar & Board Views** - Toggle between card grid, sortable table, monthly calendar, and Kanban board; preference persisted via user preferences
- **Kanban Board** - Drag-and-drop tasks between status columns; cards show title, priority badge, assignee initials, and due date
- **Inline Editing** - Toggle edit mode in table view to click-to-edit title, description, priority, status, due date, and effort in place
- **Keyboard Shortcuts** - `h` (help), `n` (new task), `s`/`/` (search), `1-4` (switch views), `e` (edit mode in table), `Escape` (close/cancel)
- **Saved Views** - Save current filter/sort/view combinations as named views; recall from dropdown
- **Bulk Actions** - Select tasks in table view with checkboxes (cross-page selection persists); floating action bar for batch status, priority, assign (project-scoped member list), and delete operations; selection clears on filter/search/sort/view change
- **CSV Export** - Download filtered tasks as CSV; respects all active filters (search, status, priority, tags, user, overdue)
- **Real-time Search** - Filter tasks as you type (debounced, 300ms); clear button appears on input
- **Filter Buttons** - All / Open / In Progress / Completed / Overdue with color-coded active states
- **Priority Filter** - Dropdown filter for Low / Medium / High with color-coded button state
- **Sortable Columns** - Sort by title, created date, priority, due date, or description (ascending/descending)
- **Priority Badges** - Color-coded clickable badges (High=red, Medium=yellow, Low=green) with reception bar icons
- **Task Dates** - Optional start date and due date with overdue detection; overdue tasks highlighted in red; completedAt timestamp recorded when task is completed
- **Task Checklist** - Embeddable checklist items on tasks (text + checked state); drag-and-drop reordering via native HTML Drag and Drop API; checklist progress shown on cards and table rows; changes audited in activity timeline
- **Pagination** - Configurable page size (10/25/50/100); top and bottom controls
- **Modal Forms** - Create and edit tasks in a modal overlay; context (filters, search, sort) is preserved
- **Task Dependencies** - Block/unblock relationships between tasks within the same project; cycle detection prevents circular chains; blocked tasks cannot be completed until blockers are resolved; visual indicators on cards, table rows, and board
- **Task Lifecycle** - Six-state status: BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED; CANCELLED as separate terminal state; toggle button advances through the cycle; status radio buttons in edit form
- **Status-Aware Reassignment** - Reassigning an in-progress task resets its status to OPEN
- **Color-Coded Tasks** - Six-status visual system: grey = backlog, secondary = open, yellow = in progress, cyan = in review, green = completed, dark = cancelled throughout UI
- **Dynamic Updates** - Toggle status and delete without page reloads via HTMX
- **User Assignment** - Assign tasks to users via searchable select dropdown (`@ManyToOne`)
- **Tags** - Tag tasks with multiple labels via checkboxes (`@ManyToMany`)
- **User & Tag Filters** - Filter tasks by assigned user and/or tags; clickable names/badges for quick filtering
- **Task Comments** - Any authenticated user can comment on any task; comment owners and admins can delete comments; real-time count updates via HTMX out-of-band swaps
- **@Mentions in Comments** - Type @ to mention users in comments with Tribute.js autocomplete; atomic backspace for mention tokens; clean display with encoded storage; mentioned users receive notifications and are subscribed to the conversation
- **Unified Activity Timeline** - Comments and audit history merged into a single chronological timeline with visual timeline dots and connecting lines; replaces separate comments panel and audit panel in both modal and full-page views
- **Shared Two-Column Layout** - Task modal and full-page task view share the same layout fragment (`task-layout.html`): form fields on left, checklist + activity timeline on right
- **Styled Confirm Dialog** - Bootstrap modal confirm dialog (`showConfirm`) replaces native browser `confirm()` for delete actions
- **Toast Notifications** - Success/error toasts for task save, delete, and conflict events; clickable toasts for notification links (Bootstrap 5 toasts with slide-in animation)
- **Online Presence** - Real-time online user count and list in navbar via WebSocket + STOMP
- **Notification Bell** - Real-time push notifications with unread badge, dropdown list, mark-as-read, and mark-all-as-read
- **Notifications Page** - Full paginated notification history at `/notifications` with clear-all; live updates via client-side event bus
- **Live Task Updates** - Stale-data banner on task list, detail page, and modal when another user modifies a task; click to refresh with current filters
- **Live Comment Updates** - Auto-refresh comment lists and counts when another user adds or deletes comments; works in both modal and full-page views
- **Analytics Dashboard** - Six interactive Chart.js charts: status breakdown, priority breakdown, workload distribution (stacked bar by assignee), 30-day burndown, 12-week velocity, and overdue-by-assignee; available cross-project (`/analytics` with project filter checkboxes) and per-project (`/projects/{id}/analytics`)
- **Real-Time Dashboard** - Per-project stats (open/in-progress/completed/overdue) with clickable cards linking to filtered task list, due-this-week tasks, recent tasks, and activity feed; admin-only system overview across all projects; auto-refreshes via WebSocket on task and presence changes
- **Sprints** - Optional time-boxed iterations per project; date-range-based status (past/active/future); non-overlapping enforcement; sprint filter on task views (active sprint / backlog / all); sprint-scoped analytics with burndown using sprint date range; managed via project settings page
- **Recurring Task Templates** - Automated task generation for non-sprint projects; DAILY/WEEKLY/BIWEEKLY/MONTHLY recurrence; configurable day-of-week/month, relative due dates, optional end date; scheduled 6 AM generation with missed-date skip; auto-disable at end date; managed via project settings with split "New Task" button
- **Due Date Reminders** - Daily scheduled notifications for tasks due tomorrow; per-user opt-in/out via profile preferences
- **Theme System** - Three color schemes (Default, Workshop, Indigo) switchable from admin settings; CSS custom properties with FOUC prevention
- **Maintenance Banner** - Dismissible site-wide alert banner configurable from admin settings
- **Dynamic Site Name** - Customizable site name shown in navbar, footer, and page titles

### REST API
- **RESTful Endpoints** - Complete CRUD for tasks, tags, and users via JSON API
- **Data Validation** - Input validation with structured JSON error responses
- **Optimistic Locking** - `@Version` on Task entity; stale updates return 409 Conflict
- **Ownership Enforcement** - Task PUT/DELETE require owner or admin; POST auto-assigns to caller
- **Role Restrictions** - Tag and user mutations (POST/DELETE) restricted to admins
- **Search & Filter** - Query tasks by keyword and status
- **Toggle Status** - Quick PATCH endpoint advances task through BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED cycle
- **Task Comments** - Nested comment endpoints under each task
- **Notifications** - Unread count, paginated list, mark read, mark all read, clear all
- **Presence** - Online user count and list

### Audit Logging
- **Event-Driven** - Services publish audit events via `ApplicationEventPublisher`; listener persists to database
- **Tracked Actions** - Project CRUD and member management, task CRUD, comment create/delete, user CRUD (including disable/enable), tag CRUD, settings changes, auth success/failure, role changes, registration, profile changes
- **Field-Level Diffs** - Update events record before/after values for each changed field
- **Admin Audit Page** - Searchable, filterable audit log at `/admin/audit` with dynamically generated category buttons (from `AuditEvent.CATEGORIES`), text search, date range, and pagination
- **Task History** - Per-task audit trail shown in unified activity timeline alongside comments

### Error Handling
- **Dual exception handlers** - `ApiExceptionHandler` returns RFC 9457 ProblemDetail JSON for REST; `WebExceptionHandler` returns Thymeleaf pages for web
- **Custom error pages** - 400 (Bad Request), 403 (Access Denied), 404 (Not Found), 409 (Conflict), 500 (Server Error)
- **RFC 9457 ProblemDetail** - Structured `application/problem+json` responses with `type`, `title`, `status`, `detail` fields; validation errors include field-level `errors` map

### Technical Highlights
- Spring Boot 4.0.5 with Java 25
- Spring Security 7.0 with form login, BCrypt, and role-based access control
- Custom Thymeleaf dialect (`${#auth}`) for ownership/role checks in templates
- H2 in-memory database (easy development setup)
- Spring Data JPA with Specifications for dynamic filtering
- Event-driven side effects — services publish domain events; three independent listeners handle audit logging, notifications, and WebSocket broadcasting
- Project-scoped access control via `ProjectAccessGuard` with VIEWER/EDITOR/OWNER roles; admin bypass
- Service-to-service composition (TaskService delegates to TagService/UserService instead of direct repository access)
- Generic `@Unique` validation annotation — class-level, `@Repeatable`, uses `EntityManager` JPQL for uniqueness checks with self-exclusion on edit
- Global string trimming via `GlobalBindingConfig` (`StringTrimmerEditor`) — trims all form fields, converts blank to null
- User enable/disable pattern — disabled users can't log in and are hidden from assignment dropdowns; users with completed tasks or comments can only be disabled (not deleted)
- Entity `FIELD_*` constants for field names (no hardcoded strings in audit snapshots or specifications)
- `get`/`find` naming convention: `getXxx()` throws `EntityNotFoundException`, `findXxx()` returns null
- Paginated REST API (`GET /api/tasks`) with search, status, priority, overdue, user, and tag filters via Spring Data `Pageable`
- OpenAPI 3.1 documentation via springdoc-openapi — Swagger UI at `/swagger-ui.html`, JSON spec at `/api-docs`
- DTO layer (`TaskRequest` / `TaskResponse`) with MapStruct for compile-time mapping
- Thymeleaf with shared fragment architecture
- HTMX 2.0 for dynamic interactions, HX-Trigger events, and out-of-band swaps
- Tribute.js for @mention autocomplete in comment input
- Bootstrap 5.3 for styling
- Reusable pagination fragment with custom DOM events
- Typed `Settings` POJO with `BeanWrapper` auto-mapping from DB key/value rows
- Per-user preferences (`UserPreferences` POJO + `user_preferences` table) mirroring the Settings pattern
- CSS theme system with `[data-theme]` selectors and FOUC prevention
- Split CSS: `base.css` (global) + `theme.css` (theme overrides) + page-specific (`tasks.css`, `audit.css`)
- WebSocket + STOMP via `spring-boot-starter-websocket` and STOMP.js 7.3
- Shared STOMP client (`websocket.js`) with `onConnect(callback)` pattern for feature scripts
- Client-side event bus via `CustomEvent` — decouples notification producers (WebSocket, dropdown, page) from consumers (badge, dropdown list, page list)
- Online presence tracking with `ConcurrentHashMap` keyed by user ID (multi-tab safe, name-change safe); broadcast via `/topic/presence`
- Notification persistence with DB-first pattern (save then push) — offline users see notifications on login
- Auto-purge of old notifications via `@Scheduled` cron (admin-configurable retention period, default 30 days)
- Central user-resolution helpers in `SecurityUtils` (replaces duplicated patterns across services, dialects, and listeners)
- `Translatable` enum interface — enums store their own `messages.properties` key; `Messages.get(Translatable)` resolves display names; templates use `#{${enum.messageKey}}`
- Split JS: `utils.js` (global) + page-specific (`js/tasks/*.js`, `audit.js`) + WebSocket (`websocket.js`, `presence.js`, `notifications.js`)
- Toast notification system via `showToast()` in `utils.js` (Bootstrap 5 toasts, lazy-created container)
- Styled confirm dialog via `showConfirm()` in `utils.js` (Bootstrap 5 modal, replaces native `confirm()`)
- All `messages.properties` keys served to JavaScript via `APP_CONFIG.messages` in `/config.js`
- Externalized UI strings via `messages.properties` (Spring MessageSource)
- Externalized validation messages via `ValidationMessages.properties` (Hibernate Validator)
- Externalized frontend routes via `@ConfigurationProperties` + `GlobalModelAttributes` (Thymeleaf) and `/config.js` endpoint (JavaScript)
- Spotless + google-java-format (AOSP style, 4-space indent) enforced at compile time
- 207 automated tests: unit (Mockito), repository (@DataJpaTest), integration (MockMvc), validation, security
- CI pipeline: GitHub Actions runs `./mvnw verify` on every push to main and PR
- Spring profiles: `dev` (H2, demo data), `test` (isolated H2, no data seeding), `prod` (PostgreSQL, Flyway migrations)
- Flyway schema migrations for production (PostgreSQL); dev/test use Hibernate `create-drop`
- Spring Actuator health and info endpoints (`/actuator/health`, `/actuator/info`)
- Hot reload with Spring DevTools

## Getting Started

### Prerequisites

- **Java 25** or higher
- **Maven 3.6+** (or use the included Maven wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/aaronchen/spring-demo.git
   cd spring-demo
   ```

2. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access the application**
   - **Login**: http://localhost:8080/login
   - **Web UI**: http://localhost:8080/ (redirects to login if not authenticated)
   - **Projects**: http://localhost:8080/projects (project list)
   - **Dashboard**: http://localhost:8080/dashboard (per-project stats, due this week, real-time updates; admin system overview)
   - **Profile**: http://localhost:8080/profile (edit name/email, change password, preferences)
   - **Tag Management**: http://localhost:8080/admin/tags (admin only)
   - **Audit Log**: http://localhost:8080/admin/audit (admin only)
   - **Settings**: http://localhost:8080/admin/settings (admin only)
   - **REST API**: http://localhost:8080/api/tasks
   - **H2 Console**: http://localhost:8080/h2-console

4. **Dev credentials** (seeded by `DataLoader`)
   - **Admin**: `alice.johnson@example.com` / `password`
   - **Regular user**: `bob.smith@example.com` / `password`
   - All 20 seeded users share the password `password`

5. **Run tests**
   ```bash
   ./mvnw test
   ```
   207 tests across 23 test classes (unit, repository, integration, validation, security).

### Build for Production

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Run with PostgreSQL (Prod Profile)

Requires Docker. See [dev-guide.md](dev-guide.md) for full details.

```bash
# Start PostgreSQL + app in Docker (port 8081)
docker compose -f docker-compose.prod.yml up --build

# Stop and clean up
docker compose -f docker-compose.prod.yml down -v
```

## Usage Guide

### Authentication

Navigate to http://localhost:8080/login. Enter your email and password, or click **Register** to create a new account. New accounts are created with the USER role.

**Roles:**
- **USER** — can create projects and tasks within their projects, edit/delete tasks based on project role (VIEWER/EDITOR/OWNER), manage own profile and preferences
- **ADMIN** — full access to all projects and tasks, can manage users (create/edit/delete/disable/enable) and tags, bypasses all project access checks, manage own profile and preferences

### Web Interface

Navigate to http://localhost:8080/tasks (requires login).

#### Viewing Tasks

- **Search** — type to filter tasks by title or description in real time
- **Filter buttons** — All / Backlog / Open / In Progress / In Review / Completed / Cancelled / Overdue
- **Sort dropdown** — sort by title, created date, priority, due date, updated date, or description
- **View toggle** — switch between card grid, table, calendar, and Kanban board view
- **Page size** — choose 10 / 25 / 50 / 100 tasks per page

#### Creating a Task

Click **New Task** — a modal opens. Select a project from the dropdown (pre-selected if you're on a project page), fill in title (required, max 100 chars), description (optional, max 500 chars), priority (Low/Medium/High, defaults to Medium), optional start date, due date, and effort (points/hours), and optional checklist items, then click **Create Task**. Tasks can be created from any page with the New Task button. Your current search/filter/sort state is preserved.

#### Editing a Task

Click the title or the **Edit** button on any card or table row. The same modal opens pre-filled. In edit mode you can set the status via radio buttons (Backlog / Open / In Progress / In Review / Completed / Cancelled).

#### Advancing Task Status

Click the toggle button on a card or row to advance the task through the lifecycle: BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED → OPEN.

#### Deleting a Task

Click the trash icon, confirm in the styled dialog (Bootstrap modal, not native browser confirm).

### REST API

All API endpoints require authentication. CSRF is disabled for `/api/**`, so you only need a valid session cookie. See `rest.http` for ready-to-use examples.

**To authenticate:** Log in via browser, copy `JSESSIONID` from DevTools → Application → Cookies, and send it as a `Cookie` header.

#### Task Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/tasks` | Any user | List all tasks |
| GET | `/api/tasks/{id}` | Any user | Get task by ID |
| POST | `/api/tasks` | Any user | Create task (auto-assigned to caller) |
| PUT | `/api/tasks/{id}` | Owner/Admin | Update task (requires `version` for optimistic locking) |
| DELETE | `/api/tasks/{id}` | Owner/Admin | Delete task (204) |
| PATCH | `/api/tasks/{id}/toggle` | Project Editor/Admin | Advance status (BACKLOG → OPEN → IN_PROGRESS → IN_REVIEW → COMPLETED) |
| GET | `/api/tasks/search?keyword=` | Any user | Search by title/description |
| GET | `/api/tasks/incomplete` | Any user | Get non-completed tasks (OPEN and IN_PROGRESS) |

POST auto-assigns tasks to the caller. Admins can optionally specify `userId` in the request body to assign the task to another user.

#### Tag Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/tags` | Any user | List all tags |
| GET | `/api/tags/{id}` | Any user | Get tag by ID |
| POST | `/api/tags` | Admin | Create tag (201) |
| DELETE | `/api/tags/{id}` | Admin | Delete tag; tasks keep other tags (204) |

#### User Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/users` | Any user | List all users |
| GET | `/api/users/{id}` | Any user | Get user by ID |
| POST | `/api/users` | Admin | Create user (201) |
| DELETE | `/api/users/{id}` | Admin | Delete user; tasks auto-unassigned (204) |

#### Comment Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/tasks/{taskId}/comments` | Any user | List comments for a task |
| POST | `/api/tasks/{taskId}/comments` | Any user | Add a comment (body: `{"text": "..."}`) (201) |
| DELETE | `/api/tasks/{taskId}/comments/{id}` | Comment owner/Admin | Delete comment (204) |

#### Notification Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/notifications/unread-count` | Any user | Get unread count (`{"count": N}`) |
| GET | `/api/notifications?page=0&size=10` | Any user | Paginated notification list |
| PATCH | `/api/notifications/{id}/read` | Any user | Mark single notification as read (204) |
| PATCH | `/api/notifications/read-all` | Any user | Mark all as read (204) |
| DELETE | `/api/notifications` | Any user | Clear all notifications (204) |

#### Project Member Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/projects/{id}/members` | Any user | All enabled members of a project |
| GET | `/api/projects/{id}/members/assignable` | Any user | Editors and owners only (for task assignment) |

#### Saved View Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/views` | Any user | List saved views for current user |
| POST | `/api/views` | Any user | Save current filters as named view |
| DELETE | `/api/views/{id}` | Owner/Admin | Delete saved view (204) |

#### Presence Endpoint

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/presence` | Any user | Online users and count |

#### Example: Create Task
```bash
POST /api/tasks
Cookie: JSESSIONID=your-session-id
Content-Type: application/json

{
  "projectId": 1,
  "title": "Write documentation",
  "description": "Document all API endpoints",
  "priority": "HIGH",
  "startDate": "2026-03-10",
  "dueDate": "2026-03-15",
  "effort": 5,
  "tagIds": [1, 3]
}
```

#### Validation Rules
- **projectId**: required on create; the project the task belongs to
- **title**: required, 1–100 characters
- **description**: optional, max 500 characters
- **status**: optional, one of `BACKLOG`, `OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `COMPLETED`, `CANCELLED` (defaults to `OPEN`)
- **priority**: optional, one of `LOW`, `MEDIUM`, `HIGH` (defaults to `MEDIUM`)
- **startDate**: optional, ISO date format `yyyy-MM-dd`
- **dueDate**: optional, ISO date format `yyyy-MM-dd`
- **effort**: optional, integer 0–32767 (unit-agnostic: points, hours, etc.)
- **tagIds**: optional list of tag IDs; omit or send `[]` for no tags
- **userId**: optional (admin only); omit or send `null` to auto-assign to caller
- **version**: required on update; must match current entity version (optimistic locking)
- **text** (comments): required, max 500 characters

#### Error Responses

| Status | Meaning |
|--------|---------|
| 400 | Validation failure (field errors in `errors` object) |
| 403 | Access denied (not owner/admin, or role restriction) |
| 404 | Entity not found |
| 409 | Optimistic locking conflict (stale version) |
| 500 | Unexpected server error |

## Database Access

**H2 Console**: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: (leave empty)

Data is lost on restart (in-memory, by design).

## Project Structure

```
spring-demo/
├── src/main/
│   ├── java/cc/desuka/demo/
│   │   ├── audit/
│   │   │   ├── AuditDetails.java            # Snapshot/diff/display-name utilities
│   │   │   ├── AuditEvent.java              # Event class with action constants
│   │   │   ├── AuditEventListener.java      # Persists AuditEvent → AuditLog
│   │   │   ├── AuditField.java              # Typed audit value record (FieldType enum, factory methods)
│   │   │   ├── AuditLogService.java         # Audit search + entity history
│   │   │   ├── AuditTemplateHelper.java     # Thymeleaf helpers (enum labels, ref URLs, checklist diff)
│   │   │   ├── Auditable.java               # Interface for audit snapshots
│   │   │   └── AuthAuditListener.java       # Login success/failure audit events
│   │   ├── config/
│   │   │   ├── AppRoutesProperties.java     # @ConfigurationProperties for app.routes.*
│   │   │   ├── DevSecurityConfig.java      # H2 console security rules (@Profile("dev"))
│   │   │   ├── GlobalBindingConfig.java     # Global string trimming (blank→null)
│   │   │   ├── GlobalModelAttributes.java   # @ControllerAdvice: appRoutes + settings + currentUser
│   │   │   ├── H2DevConfig.java            # H2 web server + console servlet (@Profile("dev"))
│   │   │   ├── SecurityConfig.java          # Spring Security filter chain, auth rules
│   │   │   ├── Settings.java               # Typed settings POJO with defaults
│   │   │   ├── UserPreferences.java        # Typed per-user preferences POJO with defaults
│   │   │   └── WebSocketConfig.java         # STOMP broker (/topic, /queue), endpoint /ws
│   │   ├── controller/
│   │   │   ├── admin/
│   │   │   │   ├── AuditController.java     # Audit log page (admin only)
│   │   │   │   ├── SettingsController.java    # Admin settings page (theme, site name, etc.)
│   │   │   │   ├── TagManagementController.java # Tag CRUD (admin only)
│   │   │   │   └── UserManagementController.java # User management with modal UI (admin only)
│   │   │   ├── api/
│   │   │   │   ├── AuditApiController.java  # Audit REST API
│   │   │   │   ├── CommentApiController.java # Comment REST API (nested under tasks)
│   │   │   │   ├── NotificationApiController.java # Notification REST API
│   │   │   │   ├── PresenceApiController.java # GET /api/presence (online users)
│   │   │   │   ├── ProjectApiController.java # Project REST API (members, assignable)
│   │   │   │   ├── SavedViewController.java  # Saved views REST API (GET/POST/DELETE)
│   │   │   │   ├── TagApiController.java    # Tag REST API (admin-only mutations)
│   │   │   │   ├── TaskApiController.java   # Task REST API (ownership checks)
│   │   │   │   └── UserApiController.java   # User REST API (admin-only mutations)
│   │   │   ├── ProjectController.java         # Project web UI (list, create, settings, archive)
│   │   │   ├── DashboardController.java      # Dashboard page + HTMX stats fragment
│   │   │   ├── FrontendConfigController.java # Serves /config.js with routes + messages
│   │   │   ├── HomeController.java          # Home page (GET /)
│   │   │   ├── NotificationController.java  # Notifications page (GET /notifications)
│   │   │   ├── LoginController.java         # Login page (GET /login)
│   │   │   ├── ProfileController.java       # Self-service profile (GET/POST /profile)
│   │   │   ├── RegistrationController.java  # Self-registration (GET/POST /register)
│   │   │   ├── TagController.java           # Tag web UI
│   │   │   ├── TaskController.java          # Task web UI (ownership-aware, CSV export)
│   │   │   └── UserController.java          # Public user list with search (/users)
│   │   ├── dto/
│   │   │   ├── AdminUserRequest.java  # Admin user creation form DTO
│   │   │   ├── BulkTaskRequest.java   # Bulk action input DTO (taskIds, action, value)
│   │   │   ├── ChangePasswordRequest.java # Password change form DTO
│   │   │   ├── CalendarDay.java        # Calendar view day cell record
│   │   │   ├── CommentRequest.java   # Comment creation form DTO
│   │   │   ├── CommentResponse.java   # Comment API output DTO
│   │   │   ├── DashboardStats.java     # Dashboard data carrier record
│   │   │   ├── NotificationResponse.java # Notification API output DTO
│   │   │   ├── PresenceResponse.java  # Presence data (REST + WebSocket)
│   │   │   ├── ProfileRequest.java    # Profile edit form DTO
│   │   │   ├── ProjectRequest.java   # Project create/edit form DTO
│   │   │   ├── RegistrationRequest.java # Registration form DTO
│   │   │   ├── SavedViewRequest.java  # Saved view create DTO (record)
│   │   │   ├── SavedViewResponse.java # Saved view output DTO (record)
│   │   │   ├── TagRequest.java
│   │   │   ├── TagResponse.java
│   │   │   ├── TaskFormRequest.java     # Web form DTO (parallel array checklist binding)
│   │   │   ├── TaskRequest.java         # API input DTO (create/update)
│   │   │   ├── TaskResponse.java        # API output DTO
│   │   │   ├── TimelineEntry.java       # Unified timeline record (comment or audit)
│   │   │   ├── UserRequest.java
│   │   │   └── UserResponse.java
│   │   ├── event/
│   │   │   ├── CommentAddedEvent.java        # Domain event: comment created
│   │   │   ├── CommentChangeEvent.java       # WebSocket: comment created/deleted
│   │   │   ├── NotificationEventListener.java # Routes notifications to recipients
│   │   │   ├── TaskAssignedEvent.java        # Domain event: task assigned
│   │   │   ├── TaskChangeEvent.java          # WebSocket: task created/updated/deleted
│   │   │   ├── TaskUpdatedEvent.java         # Domain event: task fields changed
│   │   │   └── WebSocketEventListener.java   # Broadcasts ephemeral WebSocket messages
│   │   ├── exception/
│   │   │   ├── ApiExceptionHandler.java     # JSON error responses for REST API
│   │   │   ├── EntityNotFoundException.java # Custom 404 exception
│   │   │   ├── StaleDataException.java      # Custom 409 exception (optimistic locking)
│   │   │   └── WebExceptionHandler.java     # Thymeleaf error pages for web UI
│   │   ├── mapper/
│   │   │   ├── CommentMapper.java       # MapStruct (impl generated at compile time)
│   │   │   ├── NotificationMapper.java  # MapStruct: actor.name → actorName
│   │   │   ├── ProjectMapper.java       # MapStruct: Project ↔ ProjectRequest
│   │   │   ├── RecurringTaskTemplateMapper.java # MapStruct: template ↔ Request/Response
│   │   │   ├── SprintMapper.java        # MapStruct: Sprint ↔ Request/Response (derived status)
│   │   │   ├── TagMapper.java
│   │   │   ├── TaskFormMapper.java      # MapStruct: Task ↔ TaskFormRequest (web forms)
│   │   │   ├── TaskMapper.java
│   │   │   └── UserMapper.java
│   │   ├── model/
│   │   │   ├── AuditLog.java            # Audit log entity
│   │   │   ├── ChecklistItem.java     # Embeddable checklist item (text + checked)
│   │   │   ├── Comment.java            # Comment entity (OwnedEntity)
│   │   │   ├── Notification.java       # Notification entity (@ManyToOne to User)
│   │   │   ├── NotificationType.java   # TASK_ASSIGNED, TASK_UPDATED, COMMENT_ADDED, COMMENT_MENTIONED, TASK_DUE_REMINDER, TASK_OVERDUE, SYSTEM
│   │   │   ├── OwnedEntity.java         # Marker interface for ownership checks
│   │   │   ├── Priority.java            # LOW / MEDIUM / HIGH enum (Translatable)
│   │   │   ├── Translatable.java       # Interface for enums with i18n message keys
│   │   │   ├── Project.java             # Project entity (Auditable)
│   │   │   ├── ProjectMember.java       # Project membership (user + role)
│   │   │   ├── ProjectRole.java         # VIEWER / EDITOR / OWNER enum (Translatable)
│   │   │   ├── ProjectStatus.java       # ACTIVE / ARCHIVED enum (Translatable)
│   │   │   ├── Role.java                # USER / ADMIN enum (Translatable)
│   │   │   ├── SavedView.java          # Saved filter view entity (OwnedEntity)
│   │   │   ├── Setting.java             # Key-value setting entity
│   │   │   ├── Tag.java
│   │   │   ├── Task.java                # Implements OwnedEntity (belongs to Project)
│   │   │   ├── TaskStatus.java          # BACKLOG / OPEN / ... / CANCELLED enum (Translatable)
│   │   │   ├── TaskStatusFilter.java    # ALL / BACKLOG / OPEN / IN_PROGRESS / IN_REVIEW / COMPLETED / CANCELLED enum
│   │   │   ├── User.java                # Auth fields: password, role
│   │   │   └── UserPreference.java      # Per-user key/value preference entity
│   │   ├── repository/
│   │   │   ├── AuditLogRepository.java
│   │   │   ├── AuditLogSpecifications.java  # Dynamic audit query filters
│   │   │   ├── CommentRepository.java
│   │   │   ├── NotificationRepository.java
│   │   │   ├── ProjectRepository.java
│   │   │   ├── ProjectMemberRepository.java
│   │   │   ├── SavedViewRepository.java
│   │   │   ├── SettingRepository.java
│   │   │   ├── TagRepository.java
│   │   │   ├── TaskRepository.java
│   │   │   ├── TaskSpecifications.java
│   │   │   ├── UserPreferenceRepository.java
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   ├── AuthDialect.java             # Registers ${#auth} in Thymeleaf
│   │   │   ├── AuthExpressions.java         # isOwner(), isAdmin(), canEdit()
│   │   │   ├── CustomUserDetails.java       # UserDetails wrapper for User entity
│   │   │   ├── CustomUserDetailsService.java # Loads user by email for Spring Security
│   │   │   ├── OwnershipGuard.java          # requireAccess() — owner or admin
│   │   │   ├── ProjectAccessGuard.java     # requireViewAccess/EditAccess/OwnerAccess
│   │   │   └── SecurityUtils.java           # Central user-resolution helpers
│   │   ├── presence/
│   │   │   ├── PresenceEventListener.java # WebSocket connect/disconnect → presence broadcast
│   │   │   └── PresenceService.java       # Online user tracking (ConcurrentHashMap)
│   │   ├── report/
│   │   │   └── TaskReport.java        # Shared CSV export (used by TaskController + ProjectController)
│   │   ├── service/
│   │   │   ├── CommentQueryService.java # Read-only comment lookups (breaks circular deps)
│   │   │   ├── CommentService.java      # Comment CRUD with domain event publishing
│   │   │   ├── DashboardService.java    # Orchestrates dashboard stats via TaskService/AuditLogService
│   │   │   ├── NotificationService.java # Create, mark read, clear (DB + WebSocket push)
│   │   │   ├── ProjectService.java      # Project CRUD, member management, access checks
│   │   │   ├── SavedViewService.java   # Saved view CRUD (per-user)
│   │   │   ├── ScheduledTaskService.java # Centralized @Scheduled jobs (reminders, purge)
│   │   │   ├── SettingService.java      # Load/update settings with BeanWrapper
│   │   │   ├── TagService.java
│   │   │   ├── TaskQueryService.java    # Read-only task lookups (breaks circular deps)
│   │   │   ├── TaskService.java
│   │   │   ├── UserPreferenceService.java # Per-user preferences with BeanWrapper
│   │   │   └── UserService.java         # Includes updateRole(), findByEmail(), updateProfile()
│   │   ├── validation/
│   │   │   ├── Unique.java              # Generic @Unique annotation (class-level, @Repeatable)
│   │   │   └── UniqueValidator.java     # EntityManager-based uniqueness check
│   │   ├── util/
│   │   │   ├── CsvWriter.java             # Generic CSV export utility
│   │   │   ├── HtmxUtils.java
│   │   │   ├── MentionUtils.java          # @mention parsing and display rendering
│   │   │   └── Messages.java             # MessageSource helper (shorthand for getMessage)
│   │   ├── DataLoader.java              # Seeds 20 users, 8 tags, 56 tasks, comments, notifications (@Profile("dev"))
│   │   └── DemoApplication.java
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   │   ├── audit.css           # Audit page styles
│       │   │   ├── base.css            # Global styles
│       │   │   ├── tasks.css           # Task page styles
│       │   │   └── theme.css           # Theme overrides (Workshop, Indigo)
│       │   ├── js/
│       │   │   ├── audit.js            # Audit page logic
│       │   │   ├── utils.js            # Shared utilities (cookies, CSRF, toasts, confirm)
│       │   │   └── tasks/              # Task page scripts
│       │   │       ├── tasks.js        # Task list logic (search, filters, saved views)
│       │   │       ├── task-form.js    # Task form logic (checklist, project-aware assignee)
│       │   │       ├── bulk-actions.js # Cross-page bulk selection and actions
│       │   │       ├── inline-edit.js  # Toggle-based inline editing (table view)
│       │   │       ├── kanban.js       # Drag-and-drop for Kanban board
│       │   │       └── keyboard-shortcuts.js # Keyboard shortcut handler
│       │   ├── favicon.svg             # SVG favicon
│       ├── templates/
│       │   ├── dashboard/
│       │   │   ├── dashboard.html        # Dashboard page with WebSocket subscriptions
│       │   │   └── dashboard-stats.html  # Stats fragment (bare, HTMX-refreshable)
│       │   ├── admin/
│       │   │   ├── audit.html          # Audit log page (admin only)
│       │   │   ├── audit-table.html    # Audit table fragment (HTMX partial)
│       │   │   ├── settings.html       # Admin settings page (admin only)
│       │   │   ├── tags.html           # Tag management page (admin only)
│       │   │   ├── tag-table.html      # Tag table with inline create form
│       │   │   ├── users.html          # User management with modal UI (admin only)
│       │   │   ├── user-table.html     # User table fragment (HTMX partial)
│       │   │   └── user-modal.html     # User create/edit modal form
│       │   ├── error/
│       │   │   ├── 403.html            # Access Denied page
│       │   │   ├── 404.html            # Not Found page
│       │   │   ├── 409.html            # Conflict page (optimistic locking)
│       │   │   └── 500.html            # Server Error page
│       │   ├── layouts/
│       │   │   ├── audit-diff.html     # Shared audit diff rendering fragment
│       │   │   ├── base.html           # Base layout + auth-aware navbar
│       │   │   └── pagination.html     # Reusable pagination fragment
│       │   ├── tags/
│       │   │   └── tags.html           # Tag list page
│       │   ├── tasks/
│       │   │   ├── tasks.html          # Task list page
│       │   │   ├── task.html           # Full-page create/edit form
│       │   │   ├── task-activity.html  # Unified activity timeline (comments + audit history)
│       │   │   ├── task-layout.html    # Shared two-column layout (form + checklist/timeline)
│       │   │   ├── task-modal.html     # Modal shell using task-layout
│       │   │   ├── task-form.html      # Shared form fields fragment
│       │   │   ├── task-calendar.html  # Calendar view grid fragment
│       │   │   ├── task-cards.html     # Card grid fragment
│       │   │   ├── task-card.html      # Single card fragment
│       │   │   ├── task-table.html     # Table grid fragment
│       │   │   ├── task-table-row.html # Single table row fragment
│       │   │   ├── task-board.html    # Kanban board grid fragment
│       │   │   ├── task-workspace.html # Shared task list controls (search, filters, views)
│       │   │   └── keyboard-help-modal.html # Keyboard shortcut reference modal
│       │   ├── users/
│       │   │   ├── users.html          # User list page with search
│       │   │   └── user-table.html     # User table fragment (HTMX partial)
│       │   ├── projects/
│       │   │   ├── projects.html         # Project list with sort/archive toggle
│       │   │   ├── project.html          # Project home with task filtering
│       │   │   ├── project-form.html     # Create/edit project form
│       │   │   ├── project-grid.html     # Project card grid fragment (HTMX partial)
│       │   │   ├── project-settings.html # Project settings and member management
│       │   │   └── member-table.html     # Member management table fragment (HTMX partial)
│       │   ├── profile/
│       │   │   └── profile.html         # Self-service profile page
│       │   ├── home.html               # Home page (project showcase)
│       │   ├── login.html              # Login page
│       │   ├── notifications.html      # Notification inbox page
│       │   └── register.html           # Registration page
│       ├── META-INF/
│       │   └── additional-spring-configuration-metadata.json
│       ├── messages.properties         # UI strings (#{key} in Thymeleaf)
│       ├── ValidationMessages.properties # Validation messages ({key} in annotations)
│       ├── db/migration/
│       │   └── V1__initial_schema.sql    # Flyway initial migration (PostgreSQL DDL + admin seed)
│       ├── application.properties        # Shared config (profile-agnostic)
│       ├── application-dev.properties    # Dev profile: H2, show-sql, console
│       └── application-prod.properties   # Prod profile: PostgreSQL, Flyway, no Swagger
├── src/test/
│   ├── java/cc/desuka/demo/
│   │   ├── audit/
│   │   │   ├── AuditDetailsTest.java              # Typed diff + JSON tests (12)
│   │   │   ├── AuditEventListenerTest.java        # Audit persistence tests (2)
│   │   │   ├── AuditFieldTest.java                # AuditField record tests (29)
│   │   │   └── AuditTemplateHelperTest.java       # Template helper tests (20)
│   │   ├── controller/api/
│   │   │   ├── AuditApiControllerTest.java       # Audit REST API tests (2)
│   │   │   ├── CommentApiControllerTest.java     # Comment REST API tests (8)
│   │   │   ├── NotificationApiControllerTest.java # Notification REST API tests (6)
│   │   │   ├── PresenceApiControllerTest.java    # Presence REST API tests (2)
│   │   │   ├── TagApiControllerTest.java         # Tag REST API tests (7)
│   │   │   ├── TaskApiControllerTest.java        # Task REST API tests (15)
│   │   │   └── UserApiControllerTest.java        # User REST API tests (8)
│   │   ├── event/
│   │   │   ├── NotificationEventListenerTest.java # Notification routing tests (8)
│   │   │   └── WebSocketEventListenerTest.java   # WebSocket broadcast tests (2)
│   │   ├── repository/
│   │   │   ├── AuditLogSpecificationsTest.java   # Audit filter tests (11)
│   │   │   └── TaskSpecificationsTest.java       # Task filter tests (10)
│   │   ├── security/
│   │   │   ├── OwnershipGuardTest.java           # Ownership unit tests (3)
│   │   │   └── SecurityConfigTest.java           # URL security tests (18)
│   │   ├── service/
│   │   │   ├── CommentServiceTest.java           # Comment service tests (13)
│   │   │   ├── NotificationServiceTest.java      # Notification service tests (8)
│   │   │   ├── ProjectServiceTest.java           # Project service tests (22)
│   │   │   ├── TagServiceTest.java               # Tag service tests (6)
│   │   │   ├── TaskServiceTest.java              # Task service tests (16)
│   │   │   └── UserServiceTest.java              # User service tests (20)
│   │   ├── util/
│   │   │   └── MentionUtilsTest.java             # Mention parsing/rendering tests (12)
│   │   ├── validation/
│   │   │   └── UniqueValidatorTest.java          # @Unique validator tests (6)
│   │   └── DemoApplicationTests.java             # Context load test (1)
│   └── resources/
│       └── application-test.properties           # Test profile config
├── rest.http                           # VS Code REST Client test file
├── dev-guide.md                        # Developer guide (Maven, Docker, profiles)
├── docker-compose.prod.yml             # Local prod testing (PostgreSQL + app)
├── Dockerfile                          # Multi-stage build (JDK → JRE)
├── pom.xml
├── CLAUDE.md                           # Developer reference
└── README.md
```

## Sample Data

`DataLoader.java` seeds on startup: **4 projects** (Platform, Product, Security, Ops) with team members across different roles, **20 users**, **8 tags** (Bug, Feature, DevOps, Security, Documentation, Spike, Blocked, Tech Debt), **56 tasks** distributed across projects with varied status (Backlog, Open, In Progress, In Review, Completed, Cancelled), creation dates, priorities, start dates, and due dates, **sample comments** on ~30% of tasks (1–3 comments each from random users), **checklist items** on a subset of tasks, **due-date reminder notifications** for Alice's tasks due tomorrow, and the **Workshop theme** as the default — ready to test search, filter, sort, and pagination immediately. ~80% of tasks are assigned to a user; each task gets 1–2 tags. Priority distribution: ~20% HIGH, ~40% MEDIUM, ~40% LOW. ~80% of tasks have a due date spread -10 to +30 days from today (creating a mix of overdue and upcoming). 3 of Alice's tasks are explicitly set to due tomorrow for demo purposes. The first user (Alice Johnson) is an admin; all others are regular users. All passwords are `password`.

## Technologies

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.0.5 |
| Language | Java 25 |
| Security | Spring Security 7.0 |
| Database | H2 (dev/test), PostgreSQL (prod) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Validation |
| Templates | Thymeleaf 3.x + Spring Security dialect |
| CSS | Bootstrap 5.3.8 |
| Icons | Bootstrap Icons 1.13.1 |
| Dynamic UI | HTMX 2.0.4 |
| @Mentions | Tribute.js 5.1.3 |
| Build | Maven |
| Formatting | Spotless + google-java-format 1.30 (AOSP) |
| Mapping | MapStruct 1.6 |
| Dev Tools | Spring DevTools |
| Migrations | Flyway (prod profile) |
| Monitoring | Spring Actuator |
| CI | GitHub Actions |

## Deployment

### Docker

A multi-stage `Dockerfile` is included for container-based deployment. The build stage compiles with Maven; the runtime stage uses a minimal JRE image.

```bash
docker build -t spring-demo .
docker run -p 8080:8080 spring-demo
```

For local prod testing with PostgreSQL, use the Docker Compose file:

```bash
docker compose -f docker-compose.prod.yml up --build    # PostgreSQL + app on port 8081
docker compose -f docker-compose.prod.yml down -v        # stop and clean up
```

### Spring Profiles

| Profile | Database | Flyway | Schema | Use case |
|---------|----------|--------|--------|----------|
| `dev` | H2 in-memory | Off | `create-drop` | Default — fast dev, data resets on restart |
| `test` | H2 in-memory | Off | `create-drop` | Automated tests |
| `prod` | PostgreSQL | On | `validate` | Production — persistent data |

### Render

The app is deployed at [demo.desuka.cc](https://demo.desuka.cc) on [Render](https://render.com) (Starter plan, Docker runtime). Every push to `main` triggers auto-deploy. Custom domain via Cloudflare DNS (CNAME, no proxy).

Currently runs with the `dev` profile (H2 in-memory). To switch to PostgreSQL: create a Render PostgreSQL instance, then set `SPRING_PROFILES_ACTIVE=prod` and `DATABASE_URL` as environment variables on the web service.

> **Note:** With the dev profile, all data resets on each deploy and on free-tier spin-down.

### CI Pipeline

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` and PR targeting `main`:
1. Sets up JDK 25
2. Caches Maven dependencies
3. Runs `./mvnw verify` (compile + 207 tests)

## Troubleshooting

**Application won't start** — check Java 25: `java -version`; check port: `lsof -i :8080`

**`No qualifying bean of type 'TaskMapper'`** — MapStruct generates `TaskMapperImpl` at compile time. Run `./mvnw compile` once so the class exists, then restart the app.

**HTMX not working** — check browser console; verify `HX-Request` header is sent; ensure controller calls `HtmxUtils.isHtmxRequest()`

**Styles not loading** — clear browser cache; check `src/main/resources/static/css/`

**H2 connection error** — no external database needed; verify JDBC URL is `jdbc:h2:mem:taskdb`

**403 after login** — check your role. Admin-only pages (`/admin/**`) and API mutations on tags/users require ADMIN role. Use the admin account or promote a user via `/admin/users`.

## License

Demo project for learning Spring Boot development.
