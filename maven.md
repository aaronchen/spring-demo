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

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=DemoApplicationTests

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

# 3. Before committing code:
./mvnw clean test

# 4. To create a deployable JAR:
./mvnw clean package
```

## Project Dependencies (Current)

| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-web | REST API + Web MVC |
| spring-boot-starter-data-jpa | Database access (JPA/Hibernate) |
| spring-boot-starter-validation | Bean validation (@Valid, @NotBlank) |
| spring-boot-starter-thymeleaf | Server-side HTML templates |
| spring-boot-starter-actuator | Health checks & monitoring |
| spring-boot-starter-tomcat | Embedded Tomcat web server |
| spring-boot-devtools | Auto-restart during development |
| h2 | In-memory database |
| lombok | Reduce boilerplate (getters/setters) |
| bootstrap (WebJar) | CSS framework |
| htmx.org (WebJar) | Reactive HTML updates |
