# Spring Boot Task Manager

A modern, full-stack task management application built with Spring Boot 4.0, featuring both a REST API and an interactive web UI powered by Thymeleaf and HTMX.

## Features

### Web Interface
- ✅ **Responsive Design** - Mobile-friendly UI built with Bootstrap 5
- 🔍 **Real-time Search** - Filter tasks as you type with HTMX
- 🎨 **Color-Coded Tasks** - Visual status indicators (green for completed, yellow for pending)
- ⚡ **Dynamic Updates** - Toggle and delete tasks without page reloads
- 📋 **Filter Options** - View all tasks, completed only, or pending only
- 🎯 **Intuitive UI** - Clean card-based layout with smooth animations

### REST API
- 🔌 **RESTful Endpoints** - Complete CRUD operations via JSON API
- ✔️ **Data Validation** - Input validation with detailed error messages
- 📊 **Search & Filter** - Query tasks by keyword and completion status
- 🔄 **Toggle Completion** - Quick task completion endpoint

### Technical Highlights
- 🚀 Spring Boot 4.0.2 with Java 25
- 💾 H2 in-memory database (easy development setup)
- 🔄 Spring Data JPA for data persistence
- 🎨 Thymeleaf for server-side rendering
- ⚡ HTMX 2.0 for dynamic interactions
- 🎭 Bootstrap 5.3 for styling
- 🔧 Hot reload with Spring DevTools

## Getting Started

### Prerequisites

- **Java 25** or higher
- **Maven 3.6+** (or use included Maven wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd spring-demo
   ```

2. **Run the application**
   ```bash
   # Using Maven wrapper (recommended)
   ./mvnw spring-boot:run

   # Or using installed Maven
   mvn spring-boot:run
   ```

3. **Access the application**
   - **Web UI**: http://localhost:8080/web/tasks
   - **REST API**: http://localhost:8080/api/tasks
   - **H2 Console**: http://localhost:8080/h2-console

### Build for Production

```bash
# Build JAR file
./mvnw clean package

# Run the JAR
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Usage Guide

### Web Interface

#### Viewing Tasks

Navigate to http://localhost:8080/web/tasks to see all tasks. The interface includes:

- **Search Bar** - Type to filter tasks by title or description
- **Filter Buttons** - Click "All", "Completed", or "Pending" to filter by status
- **Task Cards** - Color-coded cards showing task details
  - 🟢 **Green** = Completed tasks
  - 🟡 **Yellow** = Pending tasks

#### Creating a Task

1. Click the **"New Task"** button in the top right
2. Fill in the task details:
   - **Title** (required) - 1-100 characters
   - **Description** (optional) - Up to 500 characters
3. Click **"Create Task"**

#### Editing a Task

1. Click the **"Edit"** button on any task card
2. Update the task details
3. Optionally toggle the **"Mark as completed"** checkbox
4. Click **"Update Task"**

#### Completing a Task

Click the **"Complete"** button on any pending task. The card will update instantly to show green (completed status).

To undo, click the **"Undo"** button on a completed task.

#### Deleting a Task

1. Click the **"Delete"** button on any task card
2. Confirm deletion in the modal dialog
3. The task card will be removed instantly

### REST API

#### Base URL
```
http://localhost:8080/api/tasks
```

#### Endpoints

##### Get All Tasks
```bash
GET /api/tasks

# Response: 200 OK
[
  {
    "id": 1,
    "title": "Fix login bug",
    "description": "Users can brute-force login...",
    "completed": true,
    "createdAt": "2026-01-15T10:30:00"
  },
  ...
]
```

##### Get Task by ID
```bash
GET /api/tasks/1

# Response: 200 OK
{
  "id": 1,
  "title": "Fix login bug",
  "description": "Users can brute-force login...",
  "completed": true,
  "createdAt": "2026-01-15T10:30:00"
}
```

##### Create Task
```bash
POST /api/tasks
Content-Type: application/json

{
  "title": "Write documentation",
  "description": "Document all API endpoints"
}

# Response: 201 Created
{
  "id": 123,
  "title": "Write documentation",
  "description": "Document all API endpoints",
  "completed": false,
  "createdAt": "2026-02-11T15:00:00"
}
```

##### Update Task
```bash
PUT /api/tasks/1
Content-Type: application/json

{
  "title": "Updated title",
  "description": "Updated description",
  "completed": true
}

# Response: 200 OK
{
  "id": 1,
  "title": "Updated title",
  "description": "Updated description",
  "completed": true,
  "createdAt": "2026-01-15T10:30:00"
}
```

##### Delete Task
```bash
DELETE /api/tasks/1

# Response: 204 No Content
```

##### Toggle Task Completion
```bash
PATCH /api/tasks/1/toggle

# Response: 200 OK
{
  "id": 1,
  "title": "Fix login bug",
  "description": "Users can brute-force login...",
  "completed": false,  # Toggled from true to false
  "createdAt": "2026-01-15T10:30:00"
}
```

##### Search Tasks
```bash
GET /api/tasks/search?keyword=bug

# Response: 200 OK
[
  {
    "id": 1,
    "title": "Fix login bug",
    "description": "Users can brute-force login...",
    "completed": true,
    "createdAt": "2026-01-15T10:30:00"
  },
  ...
]
```

##### Get Incomplete Tasks
```bash
GET /api/tasks/incomplete

# Response: 200 OK
[
  {
    "id": 3,
    "title": "Refactor search endpoint",
    "description": "Split query parsing...",
    "completed": false,
    "createdAt": "2026-01-30T09:00:00"
  },
  ...
]
```

#### Validation Rules

- **Title**: Required, 1-100 characters
- **Description**: Optional, max 500 characters
- **Completed**: Boolean, defaults to `false`

#### Error Responses

**400 Bad Request** - Validation error
```json
{
  "timestamp": "2026-02-11T15:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "title",
      "message": "Title is required"
    }
  ]
}
```

**404 Not Found** - Task not found
```json
{
  "timestamp": "2026-02-11T15:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id: 999"
}
```

## Database Access

The application uses an H2 in-memory database that can be accessed via the web console:

**H2 Console URL**: http://localhost:8080/h2-console

**Connection Settings:**
- JDBC URL: `jdbc:h2:mem:taskdb`
- Username: `sa`
- Password: (leave empty)

**Note**: Data is lost when the application stops. This is intended for development/demo purposes.

## Project Structure

```
spring-demo/
├── src/
│   ├── main/
│   │   ├── java/cc/desuka/demo/
│   │   │   ├── controller/        # REST and Web controllers
│   │   │   ├── model/              # JPA entities
│   │   │   ├── repository/         # Spring Data repositories
│   │   │   ├── service/            # Business logic
│   │   │   ├── util/               # Utility classes
│   │   │   ├── DataLoader.java     # Seeds sample data
│   │   │   └── DemoApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/            # Custom CSS
│   │       │   └── bootstrap-icons/
│   │       ├── templates/
│   │       │   ├── layouts/        # Base layout
│   │       │   └── tasks/          # Task views
│   │       └── application.properties
│   └── test/                       # Unit tests
├── pom.xml                         # Maven configuration
├── CLAUDE.md                       # Developer documentation
└── README.md                       # This file
```

## Development

### Configuration

Edit `src/main/resources/application.properties` to customize:

```properties
# Application name
spring.application.name=demo

# Database settings
spring.datasource.url=jdbc:h2:mem:taskdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# H2 console
spring.h2.console.enabled=true
```

### Hot Reload

The application uses Spring DevTools for automatic restart on code changes. Just save your Java files and the app will restart automatically.

### Testing REST API

Use the included `rest.http` file with VS Code's REST Client extension to test API endpoints interactively.

## Technologies Used

### Backend
- **Spring Boot 4.0.2** - Application framework
- **Spring Data JPA** - Data access layer
- **Hibernate** - ORM
- **H2 Database** - In-memory database
- **Jakarta Validation** - Bean validation

### Frontend
- **Thymeleaf 3.x** - Template engine
- **Bootstrap 5.3.3** - CSS framework
- **Bootstrap Icons** - Icon library
- **HTMX 2.0.4** - Dynamic HTML updates
- **Custom CSS** - Additional styling

### Build & Dev Tools
- **Maven** - Build tool
- **Spring DevTools** - Hot reload
- **Lombok** - Boilerplate reduction

## Sample Data

The application automatically loads 100+ realistic sample tasks on startup via `DataLoader.java`. These include:

- Tasks with various completion statuses
- Tasks created on different dates
- Realistic titles and descriptions covering common software development scenarios

This makes it easy to test search, filtering, and pagination features immediately.

## Contributing

When working on new features:

1. Create a feature branch: `git checkout -b feature/your-feature-name`
2. Make your changes
3. Test thoroughly (web UI + REST API)
4. Commit with clear messages
5. Create a pull request

## Troubleshooting

### Application won't start

- Ensure Java 25 is installed: `java -version`
- Check if port 8080 is available: `lsof -i :8080`
- Try cleaning and rebuilding: `./mvnw clean install`

### Database connection errors

- H2 is in-memory, no external database needed
- Check `application.properties` for correct JDBC URL
- Verify H2 dependency is in `pom.xml`

### HTMX not working

- Check browser console for JavaScript errors
- Verify HTMX library is loaded (view page source)
- Ensure server returns correct fragment HTML

### Styles not loading

- Clear browser cache
- Check static resources are in `src/main/resources/static/`
- Verify WebJars are included in `pom.xml`

## License

This is a demo project for learning Spring Boot development.

## Support

For issues or questions:
- Check `CLAUDE.md` for detailed developer documentation
- Review Spring Boot documentation: https://spring.io/projects/spring-boot
- Check HTMX documentation: https://htmx.org/
