# Spring Workshop

A growing full-stack application built as a hands-on learning project for Spring Boot 4.0, featuring both a REST API and an interactive web UI powered by Thymeleaf and HTMX. New features and patterns are added as we continue exploring Spring Boot together.

## Features

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
- **RESTful Endpoints** - Complete CRUD for tasks and users via JSON API
- **Data Validation** - Input validation with error messages
- **Search & Filter** - Query tasks by keyword and completion status
- **Toggle Completion** - Quick PATCH endpoint

### Technical Highlights
- Spring Boot 4.0.3 with Java 25
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
   - **Web UI**: http://localhost:8080/
   - **REST API**: http://localhost:8080/api/tasks
   - **H2 Console**: http://localhost:8080/h2-console

### Build for Production

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Usage Guide

### Web Interface

Navigate to http://localhost:8080/tasks.

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

#### Base URL
```
http://localhost:8080/api/tasks
```

#### Task Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tasks` | List all tasks |
| GET | `/api/tasks/{id}` | Get task by ID |
| POST | `/api/tasks` | Create task (201 Created) |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task (204 No Content) |
| PATCH | `/api/tasks/{id}/toggle` | Toggle completion |
| GET | `/api/tasks/search?keyword=` | Search by title/description |
| GET | `/api/tasks/incomplete` | Get incomplete tasks only |

#### Tag Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tags` | List all tags |
| GET | `/api/tags/{id}` | Get tag by ID |
| POST | `/api/tags` | Create tag (201 Created) |
| DELETE | `/api/tags/{id}` | Delete tag; tasks retain their other tags (204 No Content) |

#### User Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create user (201 Created) |
| DELETE | `/api/users/{id}` | Delete user; tasks auto-unassigned (204 No Content) |

#### Example: Create Task
```bash
POST /api/tasks
Content-Type: application/json

{
  "title": "Write documentation",
  "description": "Document all API endpoints",
  "tagIds": [1, 3],
  "userId": 5
}
```

#### Validation Rules
- **title**: required, 1–100 characters
- **description**: optional, max 500 characters
- **tagIds**: optional list of tag IDs; omit or send `[]` for no tags
- **userId**: optional; omit or send `null` to leave unassigned

#### Error Responses

**400 Bad Request** — validation failure
**404 Not Found** — task not found

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
│   │   ├── controller/
│   │   │   ├── api/
│   │   │   │   ├── TagApiController.java    # Tag REST API
│   │   │   │   ├── TaskApiController.java   # Task REST API (uses DTOs)
│   │   │   │   └── UserApiController.java   # User REST API
│   │   │   ├── HomeController.java          # Home page (GET /)
│   │   │   └── TaskController.java          # Task web UI
│   │   ├── dto/
│   │   │   ├── TagResponse.java
│   │   │   ├── TaskRequest.java         # API input DTO (create/update)
│   │   │   ├── TaskResponse.java        # API output DTO
│   │   │   ├── UserRequest.java
│   │   │   └── UserResponse.java
│   │   ├── mapper/
│   │   │   ├── TagMapper.java           # MapStruct (impl generated at compile time)
│   │   │   ├── TaskMapper.java
│   │   │   └── UserMapper.java
│   │   ├── model/
│   │   │   ├── Tag.java
│   │   │   ├── Task.java
│   │   │   ├── TaskFilter.java
│   │   │   └── User.java
│   │   ├── repository/
│   │   │   ├── TagRepository.java
│   │   │   ├── TaskRepository.java
│   │   │   ├── TaskSpecifications.java
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   ├── TagService.java
│   │   │   ├── TaskService.java
│   │   │   └── UserService.java
│   │   ├── util/
│   │   │   └── HtmxUtils.java
│   │   ├── DataLoader.java
│   │   └── DemoApplication.java
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   │   ├── base.css            # Global styles
│       │   │   └── tasks.css           # Task page styles
│       │   ├── js/
│       │   │   ├── utils.js            # Shared utilities (cookies)
│       │   │   └── tasks.js            # Task list page logic
│       │   └── bootstrap-icons/
│       ├── templates/
│       │   ├── layouts/
│       │   │   └── base.html           # Base layout + fragments
│       │   └── tasks/
│       │       ├── tasks.html          # Task list page
│       │       ├── task.html           # Full-page create/edit form
│       │       ├── task-modal.html     # Modal create/edit (HTMX partial)
│       │       ├── task-form.html      # Shared form fields fragment
│       │       ├── task-cards.html     # Card grid fragment
│       │       ├── task-card.html      # Single card fragment
│       │       ├── task-table.html     # Table grid fragment
│       │       ├── task-table-row.html # Single table row fragment
│       │       └── task-pagination.html
│       ├── messages.properties         # UI display strings (#{key} in Thymeleaf)
│       ├── ValidationMessages.properties # Bean Validation error messages ({key} in annotations)
│       └── application.properties
├── rest.http                           # VS Code REST Client test file
├── pom.xml
├── CLAUDE.md                           # Developer reference
└── README.md
```

## Sample Data

`DataLoader.java` seeds on startup: **50 users**, **8 tags** (Work, Personal, Home, Urgent, Someday, Meeting, Research, Errand), and **300 tasks** with varied completion status and creation dates — ready to test search, filter, sort, and pagination immediately. ~80% of tasks are assigned to a user; each task gets 1–2 tags.

## Technologies

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.0.3 |
| Language | Java 25 |
| Database | H2 (in-memory) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Validation |
| Templates | Thymeleaf 3.x |
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

## License

Demo project for learning Spring Boot development.
