# Spring Workshop

A growing full-stack application built as a hands-on learning project for Spring Boot 4.0, featuring both a REST API and an interactive web UI powered by Thymeleaf and HTMX. New features and patterns are added as we continue exploring Spring Boot together.

## Features

### Authentication & Authorization
- **Form Login** - Email + password authentication with BCrypt hashing
- **Self-Registration** - New users can sign up; default role is USER
- **Role-Based Access** - Two roles: USER (standard) and ADMIN (elevated privileges)
- **Ownership Checks** - Users can edit/delete their own tasks and unassigned tasks; admins can access all
- **Admin Panel** - Manage user roles at `/admin/users` (admin only)
- **Auth-Aware UI** - Navbar shows user info, role badge, and role-appropriate links

### Web Interface
- **Responsive Design** - Mobile-friendly UI built with Bootstrap 5
- **Card & Table Views** - Toggle between card grid and sortable table; preference persisted via cookie
- **Real-time Search** - Filter tasks as you type (debounced, 300ms); clear button appears on input
- **Filter Buttons** - All / Completed / Pending with color-coded active states
- **Sortable Columns** - Sort by title, date, or description (ascending/descending)
- **Pagination** - Configurable page size (10/25/50/100); top and bottom controls
- **Modal Forms** - Create and edit tasks in a modal overlay; context (filters, search, sort) is preserved
- **Color-Coded Tasks** - Green = completed, yellow = pending throughout UI
- **Dynamic Updates** - Toggle completion and delete without page reloads via HTMX
- **User Assignment** - Assign tasks to users via dropdown (`@ManyToOne`)
- **Tags** - Tag tasks with multiple labels via checkboxes (`@ManyToMany`)

### REST API
- **RESTful Endpoints** - Complete CRUD for tasks, tags, and users via JSON API
- **Data Validation** - Input validation with structured JSON error responses
- **Ownership Enforcement** - Task PUT/DELETE require owner or admin; POST auto-assigns to caller
- **Role Restrictions** - Tag and user mutations (POST/DELETE) restricted to admins
- **Search & Filter** - Query tasks by keyword and completion status
- **Toggle Completion** - Quick PATCH endpoint (open to all authenticated users)

### Error Handling
- **Dual exception handlers** - `ApiExceptionHandler` returns JSON for REST; `WebExceptionHandler` returns Thymeleaf pages for web
- **Custom error pages** - 403 (Access Denied), 404 (Not Found), 500 (Server Error)
- **Structured API errors** - Consistent JSON with `timestamp`, `status`, `error` fields

### Technical Highlights
- Spring Boot 4.0.3 with Java 25
- Spring Security 7.0 with form login, BCrypt, and role-based access control
- Custom Thymeleaf dialect (`${#auth}`) for ownership/role checks in templates
- H2 in-memory database (easy development setup)
- Spring Data JPA with Specifications for dynamic filtering
- DTO layer (`TaskRequest` / `TaskResponse`) with MapStruct for compile-time mapping
- Thymeleaf with shared fragment architecture
- HTMX 2.0 for dynamic interactions and HX-Trigger events
- Bootstrap 5.3 for styling
- Split CSS: `base.css` (global) + `tasks.css` (page-specific)
- Split JS: `utils.js` (global) + `tasks.js` (page-specific)
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
- **ADMIN** — full access to all tasks, can manage users and tags, can assign tasks to any user

### Web Interface

Navigate to http://localhost:8080/tasks (requires login).

#### Viewing Tasks

- **Search** — type to filter tasks by title or description in real time
- **Filter buttons** — All / Completed / Pending
- **Sort dropdown** — sort by title, created date, or description
- **View toggle** — switch between card grid and table view
- **Page size** — choose 10 / 25 / 50 / 100 tasks per page

#### Creating a Task

Click **New Task** — a modal opens. Fill in title (required, max 100 chars) and description (optional, max 500 chars), then click **Create Task**. Your current search/filter/sort state is preserved.

#### Editing a Task

Click the title or the **Edit** button on any card or table row. The same modal opens pre-filled. In edit mode you can also toggle **Mark as completed**.

#### Completing a Task

Click the toggle button (checkmark icon) on a card or row to flip its completion status instantly.

#### Deleting a Task

Click the trash icon, confirm in the dialog.

### REST API

All API endpoints require authentication. CSRF is disabled for `/api/**`, so you only need a valid session cookie. See `rest.http` for ready-to-use examples.

**To authenticate:** Log in via browser, copy `JSESSIONID` from DevTools → Application → Cookies, and send it as a `Cookie` header.

#### Task Endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/tasks` | Any user | List all tasks |
| GET | `/api/tasks/{id}` | Any user | Get task by ID |
| POST | `/api/tasks` | Any user | Create task (auto-assigned to caller) |
| PUT | `/api/tasks/{id}` | Owner/Admin | Update task |
| DELETE | `/api/tasks/{id}` | Owner/Admin | Delete task (204) |
| PATCH | `/api/tasks/{id}/toggle` | Any user | Toggle completion |
| GET | `/api/tasks/search?keyword=` | Any user | Search by title/description |
| GET | `/api/tasks/incomplete` | Any user | Get incomplete tasks only |

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

#### Example: Create Task
```bash
POST /api/tasks
Cookie: JSESSIONID=your-session-id
Content-Type: application/json

{
  "title": "Write documentation",
  "description": "Document all API endpoints",
  "tagIds": [1, 3]
}
```

#### Validation Rules
- **title**: required, 1–100 characters
- **description**: optional, max 500 characters
- **tagIds**: optional list of tag IDs; omit or send `[]` for no tags
- **userId**: optional (admin only); omit or send `null` to auto-assign to caller

#### Error Responses

| Status | Meaning |
|--------|---------|
| 400 | Validation failure (field errors in `errors` object) |
| 403 | Access denied (not owner/admin, or role restriction) |
| 404 | Entity not found |
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
│   │   ├── config/
│   │   │   ├── AppRoutesProperties.java     # @ConfigurationProperties for app.routes.*
│   │   │   ├── GlobalModelAttributes.java   # @ControllerAdvice: appRoutes + currentUser
│   │   │   └── SecurityConfig.java          # Spring Security filter chain, auth rules
│   │   ├── controller/
│   │   │   ├── api/
│   │   │   │   ├── TagApiController.java    # Tag REST API (admin-only mutations)
│   │   │   │   ├── TaskApiController.java   # Task REST API (ownership checks)
│   │   │   │   └── UserApiController.java   # User REST API (admin-only mutations)
│   │   │   ├── AdminController.java         # Admin panel (user role management)
│   │   │   ├── FrontendConfigController.java # Serves /config.js with APP_CONFIG routes
│   │   │   ├── HomeController.java          # Home page (GET /)
│   │   │   ├── LoginController.java         # Login page (GET /login)
│   │   │   ├── RegistrationController.java  # Self-registration (GET/POST /register)
│   │   │   └── TaskController.java          # Task web UI (ownership-aware)
│   │   ├── dto/
│   │   │   ├── RegistrationRequest.java # Registration form DTO
│   │   │   ├── TagResponse.java
│   │   │   ├── TaskRequest.java         # API input DTO (create/update)
│   │   │   ├── TaskResponse.java        # API output DTO
│   │   │   ├── UserRequest.java
│   │   │   └── UserResponse.java
│   │   ├── exception/
│   │   │   ├── ApiExceptionHandler.java     # JSON error responses for REST API
│   │   │   ├── EntityNotFoundException.java # Custom 404 exception
│   │   │   └── WebExceptionHandler.java     # Thymeleaf error pages for web UI
│   │   ├── mapper/
│   │   │   ├── TagMapper.java           # MapStruct (impl generated at compile time)
│   │   │   ├── TaskMapper.java
│   │   │   └── UserMapper.java
│   │   ├── model/
│   │   │   ├── OwnedEntity.java         # Marker interface for ownership checks
│   │   │   ├── Role.java                # USER / ADMIN enum
│   │   │   ├── Tag.java
│   │   │   ├── Task.java                # Implements OwnedEntity
│   │   │   ├── TaskFilter.java
│   │   │   └── User.java                # Auth fields: password, role
│   │   ├── repository/
│   │   │   ├── TagRepository.java
│   │   │   ├── TaskRepository.java
│   │   │   ├── TaskSpecifications.java
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   ├── CustomUserDetails.java       # UserDetails wrapper for User entity
│   │   │   ├── CustomUserDetailsService.java # Loads user by email for Spring Security
│   │   │   └── OwnershipGuard.java          # requireAccess() — owner or admin
│   │   ├── service/
│   │   │   ├── TagService.java
│   │   │   ├── TaskService.java
│   │   │   └── UserService.java         # Includes updateRole(), findByEmail()
│   │   ├── util/
│   │   │   ├── AuthDialect.java         # Registers ${#auth} in Thymeleaf
│   │   │   ├── AuthExpressions.java     # isOwner(), isAdmin(), canEdit()
│   │   │   └── HtmxUtils.java
│   │   ├── DataLoader.java              # Seeds 50 users, 8 tags, 300 tasks
│   │   └── DemoApplication.java
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   │   ├── base.css            # Global styles
│       │   │   └── tasks.css           # Task page styles
│       │   ├── js/
│       │   │   ├── utils.js            # Shared utilities (cookies, CSRF for HTMX)
│       │   │   └── tasks.js            # Task list page logic
│       │   └── bootstrap-icons/
│       ├── templates/
│       │   ├── admin/
│       │   │   └── users.html          # User management (admin only)
│       │   ├── error/
│       │   │   ├── 403.html            # Access Denied page
│       │   │   ├── 404.html            # Not Found page
│       │   │   └── 500.html            # Server Error page
│       │   ├── layouts/
│       │   │   └── base.html           # Base layout + auth-aware navbar
│       │   ├── tasks/
│       │   │   ├── tasks.html          # Task list page
│       │   │   ├── task.html           # Full-page create/edit form
│       │   │   ├── task-modal.html     # Modal create/edit (HTMX partial)
│       │   │   ├── task-form.html      # Shared form fields fragment
│       │   │   ├── task-cards.html     # Card grid fragment
│       │   │   ├── task-card.html      # Single card fragment
│       │   │   ├── task-table.html     # Table grid fragment
│       │   │   ├── task-table-row.html # Single table row fragment
│       │   │   └── task-pagination.html
│       │   ├── login.html              # Login page
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

`DataLoader.java` seeds on startup: **50 users**, **8 tags** (Work, Personal, Home, Urgent, Someday, Meeting, Research, Errand), and **300 tasks** with varied completion status and creation dates — ready to test search, filter, sort, and pagination immediately. ~80% of tasks are assigned to a user; each task gets 1–2 tags. The first user (Alice Johnson) is an admin; all others are regular users. All passwords are `password`.

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
