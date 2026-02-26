# Spring Workshop

A growing full-stack application built as a hands-on learning project for Spring Boot 4.0, featuring both a REST API and an interactive web UI powered by Thymeleaf and HTMX. New features and patterns are added as we continue exploring Spring Boot together.

## Features

### Web Interface
- **Responsive Design** - Mobile-friendly UI built with Bootstrap 5
- **Card & Table Views** - Toggle between card grid and sortable table; preference persisted via cookie
- **Real-time Search** - Filter tasks as you type (debounced, 300ms)
- **Filter Buttons** - All / Completed / Pending with color-coded active states
- **Sortable Columns** - Sort by title, date, or description (ascending/descending)
- **Pagination** - Configurable page size (10/25/50/100); top and bottom controls
- **Modal Forms** - Create and edit tasks in a modal overlay; context (filters, search, sort) is preserved
- **Color-Coded Tasks** - Green = completed, yellow = pending throughout UI
- **Dynamic Updates** - Toggle completion and delete without page reloads via HTMX

### REST API
- **RESTful Endpoints** - Complete CRUD operations via JSON API
- **Data Validation** - Input validation with error messages
- **Search & Filter** - Query tasks by keyword and completion status
- **Toggle Completion** - Quick PATCH endpoint

### Technical Highlights
- Spring Boot 4.0.3 with Java 25
- H2 in-memory database (easy development setup)
- Spring Data JPA with Specifications for dynamic filtering
- Thymeleaf with shared fragment architecture
- HTMX 2.0 for dynamic interactions and HX-Trigger events
- Bootstrap 5.3 for styling
- Split CSS: `base.css` (global) + `tasks.css` (page-specific)
- Split JS: `utils.js` (global) + `tasks.js` (page-specific)
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
   - **Web UI**: http://localhost:8080/web/tasks
   - **REST API**: http://localhost:8080/api/tasks
   - **H2 Console**: http://localhost:8080/h2-console

### Build for Production

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Usage Guide

### Web Interface

Navigate to http://localhost:8080/web/tasks.

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

#### Endpoints

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

#### Example: Create Task
```bash
POST /api/tasks
Content-Type: application/json

{
  "title": "Write documentation",
  "description": "Document all API endpoints"
}
```

#### Validation Rules
- **title**: required, 1–100 characters
- **description**: optional, max 500 characters
- **completed**: boolean, defaults to `false`

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
│   │   │   ├── TaskApiController.java   # REST API
│   │   │   └── TaskWebController.java  # Web UI
│   │   ├── model/
│   │   │   ├── Task.java
│   │   │   ├── TaskFilter.java
│   │   │   ├── TaskRequest.java        # API DTO (reserved)
│   │   │   └── TaskResponse.java       # API DTO (reserved)
│   │   ├── repository/
│   │   │   ├── TaskRepository.java
│   │   │   └── TaskSpecifications.java
│   │   ├── service/
│   │   │   └── TaskService.java
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
│       └── application.properties
├── rest.http                           # VS Code REST Client test file
├── pom.xml
├── CLAUDE.md                           # Developer reference
└── README.md
```

## Sample Data

`DataLoader.java` seeds 100+ realistic tasks on startup with varied completion status and creation dates — ready to test search, filter, sort, and pagination immediately.

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
| Dev Tools | Spring DevTools |
| Monitoring | Spring Actuator |

## Troubleshooting

**Application won't start** — check Java 25: `java -version`; check port: `lsof -i :8080`

**HTMX not working** — check browser console; verify `HX-Request` header is sent; ensure controller calls `HtmxUtils.isHtmxRequest()`

**Styles not loading** — clear browser cache; check `src/main/resources/static/css/`

**H2 connection error** — no external database needed; verify JDBC URL is `jdbc:h2:mem:taskdb`

## License

Demo project for learning Spring Boot development.
