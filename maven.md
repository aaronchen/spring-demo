# Maven Commands for This Project

## Everyday Commands

```bash
# Start the application
./mvnw spring-boot:run

# Stop the application
# Press Ctrl+C in the terminal (if running in foreground)

# Kill a background instance (Windows)
# Step 1: Find the process using port 8080
netstat -ano | findstr :8080
# Step 2: Kill it by PID (replace 12345 with the actual PID from step 1)
taskkill /PID 12345 /F

# Kill a background instance (one-liner, Windows PowerShell)
Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# Clean and rebuild (removes target/ and recompiles)
./mvnw clean compile

# Full build (clean + compile + test + package)
./mvnw clean package

# Skip tests during build (faster, but use sparingly)
./mvnw clean package -DskipTests
```

## Dependency Management

```bash
# Download/resolve all dependencies
./mvnw dependency:resolve

# Show the full dependency tree (useful for debugging conflicts)
./mvnw dependency:tree

# Show only specific dependency
./mvnw dependency:tree -Dincludes=org.webjars:bootstrap

# Check for dependency updates
./mvnw versions:display-dependency-updates

# Force re-download of dependencies (if corrupted/stale cache)
./mvnw dependency:resolve -U
```

## Running & Testing

```bash
# Run the application
./mvnw spring-boot:run

# Run the application with a specific profile (e.g., dev, prod)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run all tests (uses test profile via @ActiveProfiles("test"))
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TaskServiceTest

# Run a specific test method
./mvnw test -Dtest=TaskServiceTest#createTask_withTags_delegatesToTagService

# Run with debug mode (port 5005)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

## Build & Package

```bash
# Create executable JAR (output: target/demo-0.0.1-SNAPSHOT.jar)
./mvnw clean package

# Run the packaged JAR directly
java -jar target/demo-0.0.1-SNAPSHOT.jar

# Clean build artifacts (deletes target/ folder)
./mvnw clean
```

## Useful Diagnostics

```bash
# Show project info
./mvnw help:effective-pom

# Show active profiles
./mvnw help:active-profiles

# Show Maven version
./mvnw --version

# Validate pom.xml
./mvnw validate
```

## Understanding the Maven Wrapper (mvnw)

- `./mvnw` (Linux/Mac) or `mvnw.cmd` (Windows) is the **Maven Wrapper**
- It downloads the correct Maven version automatically
- You do NOT need Maven installed globally
- The wrapper config is in `.mvn/wrapper/maven-wrapper.properties`

## Common Workflow

```bash
# 1. After adding a new dependency to pom.xml:
./mvnw dependency:resolve

# 2. After making code changes, restart the app:
#    (DevTools auto-restarts for most changes, but not for pom.xml changes)
./mvnw spring-boot:run

# 3. After changing a MapStruct mapper interface (e.g. TaskMapper.java):
#    Run in a second terminal while the app is still running.
#    DevTools detects the new .class and hot-reloads automatically.
./mvnw compile

# 4. Before committing code:
./mvnw clean test

# 5. To create a deployable JAR:
./mvnw clean package
```

## Two-Terminal Dev Setup (MapStruct projects)

```bash
# Terminal 1 — keep running
./mvnw spring-boot:run

# Terminal 2 — only needed after changing a mapper interface
./mvnw compile
```

Most code changes (controllers, services, templates, JS, CSS) are handled by DevTools automatically.
Only mapper interface changes require a manual `./mvnw compile`.

## Project Dependencies (Current)

| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-web | REST API + Web MVC |
| spring-boot-starter-data-jpa | Database access (JPA/Hibernate) |
| spring-boot-starter-validation | Bean validation (@Valid, @NotBlank) |
| spring-boot-starter-thymeleaf | Server-side HTML templates |
| spring-boot-starter-security | Authentication and authorization |
| spring-boot-starter-websocket | WebSocket + STOMP support |
| spring-boot-starter-actuator | Health checks & monitoring |
| spring-boot-starter-tomcat | Embedded Tomcat web server |
| thymeleaf-extras-springsecurity6 | sec:authorize attributes in templates |
| spring-boot-devtools | Auto-restart during development |
| h2 | In-memory database |
| lombok | Reduce boilerplate (getters/setters) |
| mapstruct | Compile-time DTO mapping code generation |
| bootstrap (WebJar) | CSS framework |
| htmx.org (WebJar) | Reactive HTML updates |
| stomp__stompjs (WebJar) | STOMP.js client for WebSocket |
| spring-boot-starter-data-jpa-test | JPA test slice (@DataJpaTest) |
| spring-boot-starter-validation-test | Validation test support |
| spring-boot-starter-webmvc-test | MockMvc test support (@AutoConfigureMockMvc) |
| spring-security-test | Security test utilities (mock users, CSRF) |
