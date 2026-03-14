# Spring Workshop

A growing full-stack application built as a hands-on learning project for Spring Boot 4.0, featuring both a REST API and an interactive web UI powered by Thymeleaf and HTMX. New features and patterns are added as we continue exploring Spring Boot together.

## Features

### Authentication & Authorization
- **Form Login** - Email + password authentication with BCrypt hashing
- **Self-Registration** - New users can sign up; default role is USER
- **Role-Based Access** - Two roles: USER (standard) and ADMIN (elevated privileges)
- **Ownership Checks** - Users can edit/delete their own tasks and unassigned tasks; admins can access all
- **Admin Panel** - Modal-based user management (create/edit/delete/disable/enable) at `/admin/users`; tag management at `/admin/tags` (admin only)
- **Audit Logging** - All entity changes and auth events logged; admin audit page with search/filters at `/admin/audit`
- **Admin Settings** - Configurable site name, registration toggle, maintenance banner, and theme picker at `/admin/settings`
- **Auth-Aware UI** - Navbar shows user info, role badge, and role-appropriate links

### Web Interface
- **Responsive Design** - Mobile-friendly UI built with Bootstrap 5
- **Card & Table Views** - Toggle between card grid and sortable table; preference persisted via cookie
- **Real-time Search** - Filter tasks as you type (debounced, 300ms); clear button appears on input
- **Filter Buttons** - All / Open / In Progress / Completed / Overdue with color-coded active states
- **Priority Filter** - Dropdown filter for Low / Medium / High with color-coded button state
- **Sortable Columns** - Sort by title, created date, priority, due date, or description (ascending/descending)
- **Priority Badges** - Color-coded clickable badges (High=red, Medium=yellow, Low=green) with reception bar icons
- **Due Dates** - Optional due date with overdue detection; overdue tasks highlighted in red
- **Pagination** - Configurable page size (10/25/50/100); top and bottom controls
- **Modal Forms** - Create and edit tasks in a modal overlay; context (filters, search, sort) is preserved
- **Task Lifecycle** - Three-state status: OPEN → IN_PROGRESS → COMPLETED; toggle button advances through the cycle; status radio buttons in edit form
- **Status-Aware Reassignment** - Reassigning an in-progress task resets its status to OPEN
- **Color-Coded Tasks** - Green = completed, blue = in progress, yellow = open throughout UI
- **Dynamic Updates** - Toggle status and delete without page reloads via HTMX
- **User Assignment** - Assign tasks to users via searchable select dropdown (`@ManyToOne`)
- **Tags** - Tag tasks with multiple labels via checkboxes (`@ManyToMany`)
- **User & Tag Filters** - Filter tasks by assigned user and/or tags; clickable names/badges for quick filtering
- **Task Comments** - Any authenticated user can comment on any task; comment owners and admins can delete comments; visible in modal side panel and full-page view with real-time count updates via HTMX out-of-band swaps
- **Task Audit History** - View change history in task edit modal (split-panel) and full-page view
- **Styled Confirm Dialog** - Bootstrap modal confirm dialog (`showConfirm`) replaces native browser `confirm()` for delete actions
- **Toast Notifications** - Success/error toasts for task save, delete, and conflict events; clickable toasts for notification links (Bootstrap 5 toasts with slide-in animation)
- **Online Presence** - Real-time online user count and list in navbar via WebSocket + STOMP
- **Notification Bell** - Real-time push notifications with unread badge, dropdown list, mark-as-read, and mark-all-as-read
- **Notifications Page** - Full paginated notification history at `/notifications` with clear-all; live updates via client-side event bus
- **Live Task Updates** - Stale-data banner on task list, detail page, and modal when another user modifies a task; click to refresh with current filters
- **Live Comment Updates** - Auto-refresh comment lists and counts when another user adds or deletes comments; works in both modal and full-page views
- **Real-Time Dashboard** - Personal stats (open/in-progress/completed/overdue), system overview, recent tasks, and activity feed; auto-refreshes via WebSocket on task and presence changes
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
- **Toggle Status** - Quick PATCH endpoint advances task through OPEN → IN_PROGRESS → COMPLETED cycle
- **Task Comments** - Nested comment endpoints under each task
- **Notifications** - Unread count, paginated list, mark read, mark all read, clear all
- **Presence** - Online user count and list

### Audit Logging
- **Event-Driven** - Services publish audit events via `ApplicationEventPublisher`; listener persists to database
- **Tracked Actions** - Task CRUD, comment create/delete, user CRUD (including disable/enable), tag CRUD, settings changes, login success/failure, role changes, registration
- **Field-Level Diffs** - Update events record before/after values for each changed field
- **Admin Audit Page** - Searchable, filterable audit log at `/admin/audit` with category buttons, text search, date range, and pagination
- **Task History** - Per-task audit trail shown in edit modal (split-panel) and full-page view

### Error Handling
- **Dual exception handlers** - `ApiExceptionHandler` returns JSON for REST; `WebExceptionHandler` returns Thymeleaf pages for web
- **Custom error pages** - 403 (Access Denied), 404 (Not Found), 409 (Conflict), 500 (Server Error)
- **Structured API errors** - Consistent JSON with `timestamp`, `status`, `error` fields

### Technical Highlights
- Spring Boot 4.0.3 with Java 25
- Spring Security 7.0 with form login, BCrypt, and role-based access control
- Custom Thymeleaf dialect (`${#auth}`) for ownership/role checks in templates
- H2 in-memory database (easy development setup)
- Spring Data JPA with Specifications for dynamic filtering
- Service-to-service composition (TaskService delegates to TagService/UserService/CommentService instead of direct repository access)
- Generic `@Unique` validation annotation — class-level, `@Repeatable`, uses `EntityManager` JPQL for uniqueness checks with self-exclusion on edit
- Global string trimming via `GlobalBindingConfig` (`StringTrimmerEditor`) — trims all form fields, converts blank to null
- User enable/disable pattern — disabled users can't log in and are hidden from assignment dropdowns; users with completed tasks or comments can only be disabled (not deleted)
- Entity `FIELD_*` constants for field names (no hardcoded strings in audit snapshots or specifications)
- `get`/`find` naming convention: `getXxx()` throws `EntityNotFoundException`, `findXxx()` returns null
- DTO layer (`TaskRequest` / `TaskResponse`) with MapStruct for compile-time mapping
- Thymeleaf with shared fragment architecture
- HTMX 2.0 for dynamic interactions, HX-Trigger events, and out-of-band swaps
- Bootstrap 5.3 for styling
- Reusable pagination fragment with custom DOM events
- Typed `Settings` POJO with `BeanWrapper` auto-mapping from DB key/value rows
- CSS theme system with `[data-theme]` selectors and FOUC prevention
- Split CSS: `base.css` (global) + `theme.css` (theme overrides) + page-specific (`tasks.css`, `audit.css`)
- WebSocket + STOMP via `spring-boot-starter-websocket` and STOMP.js 7.1
- Shared STOMP client (`websocket.js`) with `onConnect(callback)` pattern for feature scripts
- Client-side event bus via `CustomEvent` — decouples notification producers (WebSocket, dropdown, page) from consumers (badge, dropdown list, page list)
- Online presence tracking with `ConcurrentHashMap` (multi-tab safe); broadcast via `/topic/presence`
- Notification persistence with DB-first pattern (save then push) — offline users see notifications on login
- Auto-purge of old notifications via `@Scheduled` cron (30 days)
- Central user-resolution helpers in `SecurityUtils` (replaces duplicated patterns across services, dialects, and listeners)
- Split JS: `utils.js` (global) + page-specific (`tasks.js`, `audit.js`) + WebSocket (`websocket.js`, `presence.js`, `notifications.js`)
- Toast notification system via `showToast()` in `utils.js` (Bootstrap 5 toasts, lazy-created container)
- Styled confirm dialog via `showConfirm()` in `utils.js` (Bootstrap 5 modal, replaces native `confirm()`)
- All `messages.properties` keys served to JavaScript via `APP_CONFIG.messages` in `/config.js`
- Externalized UI strings via `messages.properties` (Spring MessageSource)
- Externalized validation messages via `ValidationMessages.properties` (Hibernate Validator)
- Externalized frontend routes via `@ConfigurationProperties` + `GlobalModelAttributes` (Thymeleaf) and `/config.js` endpoint (JavaScript)
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
   - **Dashboard**: http://localhost:8080/dashboard (personal stats + real-time updates)
   - **Tag Management**: http://localhost:8080/admin/tags (admin only)
   - **Audit Log**: http://localhost:8080/admin/audit (admin only)
   - **Settings**: http://localhost:8080/admin/settings (admin only)
   - **REST API**: http://localhost:8080/api/tasks
   - **H2 Console**: http://localhost:8080/h2-console

4. **Dev credentials** (seeded by `DataLoader`)
   - **Admin**: `alice.johnson@example.com` / `password`
   - **Regular user**: `bob.smith@example.com` / `password`
   - All 50 seeded users share the password `password`

### Build for Production

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Usage Guide

### Authentication

Navigate to http://localhost:8080/login. Enter your email and password, or click **Register** to create a new account. New accounts are created with the USER role.

**Roles:**
- **USER** — can create tasks (defaults to self, can assign to others), edit/delete own and unassigned tasks, view all tasks
- **ADMIN** — full access to all tasks, can manage users (create/edit/delete/disable/enable) and tags, can assign tasks to any user

### Web Interface

Navigate to http://localhost:8080/tasks (requires login).

#### Viewing Tasks

- **Search** — type to filter tasks by title or description in real time
- **Filter buttons** — All / Open / In Progress / Completed / Overdue
- **Sort dropdown** — sort by title, created date, or description
- **View toggle** — switch between card grid and table view
- **Page size** — choose 10 / 25 / 50 / 100 tasks per page

#### Creating a Task

Click **New Task** — a modal opens. Fill in title (required, max 100 chars), description (optional, max 500 chars), priority (Low/Medium/High, defaults to Medium), and optional due date, then click **Create Task**. Your current search/filter/sort state is preserved.

#### Editing a Task

Click the title or the **Edit** button on any card or table row. The same modal opens pre-filled. In edit mode you can set the status via radio buttons (Open / In Progress / Completed).

#### Advancing Task Status

Click the toggle button on a card or row to advance the task through the lifecycle: OPEN → IN_PROGRESS → COMPLETED → OPEN.

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
| PATCH | `/api/tasks/{id}/toggle` | Any user | Advance status (OPEN → IN_PROGRESS → COMPLETED) |
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
  "title": "Write documentation",
  "description": "Document all API endpoints",
  "priority": "HIGH",
  "dueDate": "2026-03-15",
  "tagIds": [1, 3]
}
```

#### Validation Rules
- **title**: required, 1–100 characters
- **description**: optional, max 500 characters
- **status**: optional, one of `OPEN`, `IN_PROGRESS`, `COMPLETED` (defaults to `OPEN`)
- **priority**: optional, one of `LOW`, `MEDIUM`, `HIGH` (defaults to `MEDIUM`)
- **dueDate**: optional, ISO date format `yyyy-MM-dd`
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
│   │   │   ├── Auditable.java               # Interface for audit snapshots
│   │   │   └── AuthAuditListener.java       # Login success/failure audit events
│   │   ├── config/
│   │   │   ├── AppRoutesProperties.java     # @ConfigurationProperties for app.routes.*
│   │   │   ├── GlobalBindingConfig.java     # Global string trimming (blank→null)
│   │   │   ├── GlobalModelAttributes.java   # @ControllerAdvice: appRoutes + settings + currentUser
│   │   │   ├── PresenceEventListener.java   # WebSocket connect/disconnect → presence broadcast
│   │   │   ├── SecurityConfig.java          # Spring Security filter chain, auth rules
│   │   │   ├── Settings.java               # Typed settings POJO with defaults
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
│   │   │   │   ├── TagApiController.java    # Tag REST API (admin-only mutations)
│   │   │   │   ├── TaskApiController.java   # Task REST API (ownership checks)
│   │   │   │   └── UserApiController.java   # User REST API (admin-only mutations)
│   │   │   ├── DashboardController.java      # Dashboard page + HTMX stats fragment
│   │   │   ├── FrontendConfigController.java # Serves /config.js with routes + messages
│   │   │   ├── HomeController.java          # Home page (GET /)
│   │   │   ├── NotificationController.java  # Notifications page (GET /notifications)
│   │   │   ├── LoginController.java         # Login page (GET /login)
│   │   │   ├── RegistrationController.java  # Self-registration (GET/POST /register)
│   │   │   ├── TagController.java           # Tag web UI
│   │   │   ├── TaskController.java          # Task web UI (ownership-aware)
│   │   │   └── UserController.java          # User web UI with search
│   │   ├── dto/
│   │   │   ├── AdminUserRequest.java  # Admin user creation form DTO
│   │   │   ├── DashboardStats.java     # Dashboard data carrier record
│   │   │   ├── CommentChangeEvent.java # WebSocket comment change broadcast
│   │   │   ├── CommentResponse.java   # Comment API output DTO
│   │   │   ├── NotificationResponse.java # Notification API output DTO
│   │   │   ├── PresenceResponse.java  # Presence data (REST + WebSocket)
│   │   │   ├── RegistrationRequest.java # Registration form DTO
│   │   │   ├── TaskChangeEvent.java   # WebSocket task change broadcast
│   │   │   ├── TagResponse.java
│   │   │   ├── TaskRequest.java         # API input DTO (create/update)
│   │   │   ├── TaskResponse.java        # API output DTO
│   │   │   ├── UserRequest.java
│   │   │   └── UserResponse.java
│   │   ├── exception/
│   │   │   ├── ApiExceptionHandler.java     # JSON error responses for REST API
│   │   │   ├── EntityNotFoundException.java # Custom 404 exception
│   │   │   ├── StaleDataException.java      # Custom 409 exception (optimistic locking)
│   │   │   └── WebExceptionHandler.java     # Thymeleaf error pages for web UI
│   │   ├── mapper/
│   │   │   ├── CommentMapper.java       # MapStruct (impl generated at compile time)
│   │   │   ├── NotificationMapper.java  # MapStruct: actor.name → actorName
│   │   │   ├── TagMapper.java
│   │   │   ├── TaskMapper.java
│   │   │   └── UserMapper.java
│   │   ├── model/
│   │   │   ├── AuditLog.java            # Audit log entity
│   │   │   ├── Comment.java            # Comment entity (OwnedEntity)
│   │   │   ├── Notification.java       # Notification entity (@ManyToOne to User)
│   │   │   ├── NotificationType.java   # TASK_ASSIGNED, COMMENT_ADDED, TASK_OVERDUE, SYSTEM
│   │   │   ├── OwnedEntity.java         # Marker interface for ownership checks
│   │   │   ├── Priority.java            # LOW / MEDIUM / HIGH enum
│   │   │   ├── Role.java                # USER / ADMIN enum
│   │   │   ├── Setting.java             # Key-value setting entity
│   │   │   ├── Tag.java
│   │   │   ├── Task.java                # Implements OwnedEntity
│   │   │   ├── TaskStatus.java          # OPEN / IN_PROGRESS / COMPLETED enum
│   │   │   ├── TaskStatusFilter.java    # ALL / OPEN / IN_PROGRESS / COMPLETED / OVERDUE enum
│   │   │   └── User.java                # Auth fields: password, role
│   │   ├── repository/
│   │   │   ├── AuditLogRepository.java
│   │   │   ├── AuditLogSpecifications.java  # Dynamic audit query filters
│   │   │   ├── CommentRepository.java
│   │   │   ├── NotificationRepository.java
│   │   │   ├── SettingRepository.java
│   │   │   ├── TagRepository.java
│   │   │   ├── TaskRepository.java
│   │   │   ├── TaskSpecifications.java
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   ├── AuthDialect.java             # Registers ${#auth} in Thymeleaf
│   │   │   ├── AuthExpressions.java         # isOwner(), isAdmin(), canEdit()
│   │   │   ├── CustomUserDetails.java       # UserDetails wrapper for User entity
│   │   │   ├── CustomUserDetailsService.java # Loads user by email for Spring Security
│   │   │   ├── OwnershipGuard.java          # requireAccess() — owner or admin
│   │   │   └── SecurityUtils.java           # Central user-resolution helpers
│   │   ├── service/
│   │   │   ├── AuditLogService.java     # Audit search + entity history
│   │   │   ├── CommentService.java      # Comment CRUD with audit events + WebSocket broadcast
│   │   │   ├── DashboardService.java    # Orchestrates dashboard stats via TaskService/AuditLogService
│   │   │   ├── NotificationService.java # Create, mark read, clear, purge (@Scheduled)
│   │   │   ├── PresenceService.java     # Online user tracking (ConcurrentHashMap)
│   │   │   ├── SettingService.java      # Load/update settings with BeanWrapper
│   │   │   ├── TagService.java
│   │   │   ├── TaskService.java
│   │   │   └── UserService.java         # Includes updateRole(), findByEmail()
│   │   ├── validation/
│   │   │   ├── Unique.java              # Generic @Unique annotation (class-level, @Repeatable)
│   │   │   └── UniqueValidator.java     # EntityManager-based uniqueness check
│   │   ├── util/
│   │   │   └── HtmxUtils.java
│   │   ├── DataLoader.java              # Seeds 50 users, 8 tags, 300 tasks, comments
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
│       │   │   ├── utils.js            # Shared utilities (cookies, CSRF for HTMX)
│       │   │   └── tasks.js            # Task list page logic
│       │   ├── favicon.svg             # SVG favicon
│       │   └── bootstrap-icons/
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
│       │   │   ├── base.html           # Base layout + auth-aware navbar
│       │   │   └── pagination.html     # Reusable pagination fragment
│       │   ├── tags/
│       │   │   └── tags.html           # Tag list page
│       │   ├── tasks/
│       │   │   ├── tasks.html          # Task list page
│       │   │   ├── task.html           # Full-page create/edit form
│       │   │   ├── task-audit.html     # Shared audit history entries fragment
│       │   │   ├── task-comments.html  # Shared comments fragment with OOB count swaps
│       │   │   ├── task-modal.html     # Modal create/edit with history panel
│       │   │   ├── task-form.html      # Shared form fields fragment
│       │   │   ├── task-cards.html     # Card grid fragment
│       │   │   ├── task-card.html      # Single card fragment
│       │   │   ├── task-table.html     # Table grid fragment
│       │   │   └── task-table-row.html # Single table row fragment
│       │   ├── users/
│       │   │   ├── users.html          # User list page with search
│       │   │   └── user-table.html     # User table fragment (HTMX partial)
│       │   ├── home.html               # Home page (project showcase)
│       │   ├── login.html              # Login page
│       │   ├── notifications.html      # Notification inbox page
│       │   └── register.html           # Registration page
│       ├── META-INF/
│       │   └── additional-spring-configuration-metadata.json
│       ├── messages.properties         # UI strings (#{key} in Thymeleaf)
│       ├── ValidationMessages.properties # Validation messages ({key} in annotations)
│       └── application.properties
├── rest.http                           # VS Code REST Client test file
├── pom.xml
├── CLAUDE.md                           # Developer reference
└── README.md
```

## Sample Data

`DataLoader.java` seeds on startup: **50 users**, **8 tags** (Work, Personal, Home, Urgent, Someday, Meeting, Research, Errand), **300 tasks** with varied status (Open, In Progress, Completed), creation dates, priorities, and due dates, **sample comments** on ~30% of tasks (1–3 comments each from random users), and the **Workshop theme** as the default — ready to test search, filter, sort, and pagination immediately. ~80% of tasks are assigned to a user; each task gets 1–2 tags. Priority distribution: ~20% HIGH, ~40% MEDIUM, ~40% LOW. ~80% of tasks have a due date spread -10 to +30 days from today (creating a mix of overdue and upcoming). The first user (Alice Johnson) is an admin; all others are regular users. All passwords are `password`.

## Technologies

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.0.3 |
| Language | Java 25 |
| Security | Spring Security 7.0 |
| Database | H2 (in-memory) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Validation |
| Templates | Thymeleaf 3.x + Spring Security dialect |
| CSS | Bootstrap 5.3.3 |
| Icons | Bootstrap Icons |
| Dynamic UI | HTMX 2.0.4 |
| Build | Maven |
| Mapping | MapStruct 1.6 |
| Dev Tools | Spring DevTools |
| Monitoring | Spring Actuator |

## Troubleshooting

**Application won't start** — check Java 25: `java -version`; check port: `lsof -i :8080`

**`No qualifying bean of type 'TaskMapper'`** — MapStruct generates `TaskMapperImpl` at compile time. Run `./mvnw compile` once so the class exists, then restart the app.

**HTMX not working** — check browser console; verify `HX-Request` header is sent; ensure controller calls `HtmxUtils.isHtmxRequest()`

**Styles not loading** — clear browser cache; check `src/main/resources/static/css/`

**H2 connection error** — no external database needed; verify JDBC URL is `jdbc:h2:mem:taskdb`

**403 after login** — check your role. Admin-only pages (`/admin/**`) and API mutations on tags/users require ADMIN role. Use the admin account or promote a user via `/admin/users`.

## License

Demo project for learning Spring Boot development.
