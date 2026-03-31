# Developer Guide

## Spring Profiles

| Profile | Database | Flyway | Use case |
|---------|----------|--------|----------|
| `dev` (default) | H2 in-memory | Off | Everyday development — fast startup, data resets on restart |
| `test` | H2 in-memory | Off | Automated tests (`@ActiveProfiles("test")`) |
| `prod` | PostgreSQL | On | Production — persistent data, schema managed by Flyway |

## Everyday Commands (Dev Profile)

```bash
# Start the application (dev profile, H2 database)
./mvnw spring-boot:run

# Stop — Ctrl+C in the terminal

# Start with debug mode (attach debugger on port 5005)
./mvnw spring-boot:run -Pdebug

# Clean and rebuild
./mvnw clean compile

# Full build (clean + compile + test + package JAR)
./mvnw clean package

# Skip tests during build (use sparingly)
./mvnw clean package -DskipTests
```

## Running Tests

```bash
# Run all tests (242 tests, uses H2 via test profile)
./mvnw test

# Run all tests + package JAR (full build lifecycle)
./mvnw verify

# Run a specific test class
./mvnw test -Dtest=TaskServiceTest

# Run a specific test method
./mvnw test -Dtest=TaskServiceTest#createTask_withTags_delegatesToTagService
```

`test` runs unit tests. `verify` runs the full build lifecycle (compile, test, package). For this project they produce the same test results, but `verify` also builds the JAR.

## Docker: Running Prod Profile Locally

Requires Docker. Uses `docker-compose.prod.yml` which starts PostgreSQL 18 + the app with the prod profile.

### Start the prod stack

```bash
# First time, or after code changes (rebuilds the app image)
docker compose -f docker-compose.prod.yml up --build

# Subsequent runs (reuses existing image, faster)
docker compose -f docker-compose.prod.yml up
```

The app runs at `http://localhost:8081` (port 8081 to avoid conflict with a dev instance on 8080).

### Stop the prod stack

```bash
# Ctrl+C in the terminal where it's running, then clean up:
docker compose -f docker-compose.prod.yml down -v
```

`down` stops and removes containers. `-v` also removes the PostgreSQL data volume (clean slate next time). Omit `-v` to keep the data between restarts.

### Run only PostgreSQL (debug the app natively)

```bash
# Terminal 1 — start just PostgreSQL
docker compose -f docker-compose.prod.yml up postgres

# Terminal 2 — run the app natively with prod profile + debug
DATABASE_URL=jdbc:postgresql://localhost:5432/springdemo?user=demo&password=demo \
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod -Pdebug
```

This gives you full debugging (breakpoints, hot-reload) while using PostgreSQL instead of H2.

### How it works

Docker Compose reads `docker-compose.prod.yml` and does two things:
1. Starts a PostgreSQL 18 container (database: `springdemo`, user: `demo`, password: `demo`)
2. Builds the app from the `Dockerfile`, then runs it with `SPRING_PROFILES_ACTIVE=prod` and `DATABASE_URL` pointing at the PostgreSQL container

The image name (`spring-demo-app`) is auto-generated from the folder name + service name.

## Deployment (Render)

The app is deployed on Render using the dev profile (H2 in-memory). Render builds from the `Dockerfile` on every push to `main`.

- Data resets on each deploy (H2 is in-memory)
- `daily-redeploy.yml` GitHub Action triggers a Render rebuild daily at 6 AM UTC+8

To switch Render to PostgreSQL: create a Render PostgreSQL instance, set `SPRING_PROFILES_ACTIVE=prod` and `DATABASE_URL` as environment variables on the web service.

## CI Pipeline (GitHub Actions)

`.github/workflows/ci.yml` runs on every push to `main` and on every PR targeting `main`:
1. Checks out the code
2. Sets up JDK 25
3. Runs `./mvnw verify` (compile + all 242 tests using H2)

Results show as green check or red X on the commit/PR.

## Two-Terminal Dev Setup (MapStruct)

```bash
# Terminal 1 — keep running
./mvnw spring-boot:run

# Terminal 2 — only needed after changing a mapper interface
./mvnw compile
```

Most code changes (controllers, services, templates, JS, CSS) are handled by DevTools automatically. Only MapStruct mapper interface changes require a manual `./mvnw compile`.

## Dependency Management

```bash
# Download/resolve all dependencies
./mvnw dependency:resolve

# Show the full dependency tree
./mvnw dependency:tree

# Show only a specific dependency
./mvnw dependency:tree -Dincludes=org.webjars:bootstrap

# Force re-download dependencies (if corrupted/stale cache)
./mvnw dependency:resolve -U
```

### Checking for Updates

```bash
# Check for newer versions of all dependencies
./mvnw versions:display-dependency-updates

# Check for newer Spring Boot parent version
./mvnw versions:display-parent-updates

# Check for newer plugin versions
./mvnw versions:display-plugin-updates
```

The Spring Boot parent (`spring-boot-starter-parent`) manages versions for most Spring ecosystem dependencies. Bumping the parent version upgrades them as a tested-together set. Dependencies outside the parent (WebJars like Bootstrap, HTMX, Chart.js, STOMP.js) are versioned individually in `pom.xml`.

When updating a WebJar version, also update the version in the resource path in `base.html` (e.g., `/webjars/bootstrap/5.3.8/css/bootstrap.min.css`).

Skip pre-release versions (milestones like `-M4`, alphas, RCs) unless you have a specific reason to adopt them.

## Common Workflow

```bash
# 1. After adding a new dependency to pom.xml:
./mvnw dependency:resolve

# 2. After making code changes:
#    DevTools auto-restarts for most changes, but not pom.xml changes.
#    For pom.xml changes, restart the app manually.

# 3. After changing a MapStruct mapper interface:
#    Run in a second terminal while the app is running.
./mvnw compile

# 4. Before committing:
./mvnw clean test

# 5. To create a deployable JAR:
./mvnw clean package
```

## Maven Wrapper

- `./mvnw` (Mac/Linux) or `mvnw.cmd` (Windows) is the Maven Wrapper
- Downloads the correct Maven version automatically — no global Maven install needed
- Config in `.mvn/wrapper/maven-wrapper.properties`

## Project Dependencies

| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-web | REST API + Web MVC |
| spring-boot-starter-data-jpa | Database access (JPA/Hibernate) |
| spring-boot-starter-validation | Bean validation (@Valid, @NotBlank) |
| spring-boot-starter-thymeleaf | Server-side HTML templates |
| spring-boot-starter-security | Authentication and authorization |
| spring-boot-starter-websocket | WebSocket + STOMP support |
| spring-boot-starter-actuator | Health checks and monitoring endpoints |
| spring-boot-starter-tomcat | Embedded Tomcat web server |
| spring-boot-starter-flyway | Flyway database migration (prod profile) |
| thymeleaf-extras-springsecurity6 | sec:authorize attributes in templates |
| spring-boot-devtools | Auto-restart during development |
| h2 | In-memory database (dev/test) |
| postgresql | PostgreSQL JDBC driver (prod) |
| lombok | Reduce boilerplate (getters/setters) |
| mapstruct | Compile-time DTO mapping code generation |
| springdoc-openapi-starter-webmvc-ui | OpenAPI 3.1 spec + Swagger UI |
| bootstrap (WebJar) | CSS framework |
| htmx.org (WebJar) | Reactive HTML updates |
| stomp__stompjs (WebJar) | STOMP.js client for WebSocket |
| chart.js (WebJar) | Chart.js for analytics charts |
| bootstrap-icons (WebJar) | Icon font |
| github-com-zurb-tribute (WebJar) | @mention autocomplete |
| spring-boot-starter-data-jpa-test | JPA test slice (@DataJpaTest) |
| spring-boot-starter-validation-test | Validation test support |
| spring-boot-starter-webmvc-test | MockMvc test support (@AutoConfigureMockMvc) |
| spring-security-test | Security test utilities (mock users, CSRF) |

## Useful Diagnostics

```bash
# Show effective POM (resolved parent + this project)
./mvnw help:effective-pom

# Show active profiles
./mvnw help:active-profiles

# Show Maven version
./mvnw --version

# Validate pom.xml
./mvnw validate
```
