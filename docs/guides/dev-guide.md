# Dev Guide — Running A Spring Boot Project In This Style

Purpose: document the development workflow for Spring Boot projects built in this style — profiles, Maven commands, testing, Docker, CI, and dependency management.

This is the generic reference. For this project's specific setup (Render deployment, specific Docker Compose file, CI workflow file), see [OPERATIONS.md](../../OPERATIONS.md) at the project root.

## Spring Profiles

Recommended three-profile model:

| Profile | Database | Migrations | Use case |
|---------|----------|------------|----------|
| `dev` (default) | H2 in-memory | Off | Everyday development — fast startup, data resets on restart |
| `test` | H2 in-memory | Off | Automated tests (`@ActiveProfiles("test")`) |
| `prod` | PostgreSQL | On (Flyway) | Production — persistent data, schema managed by migrations |

Profile-specific config lives in `application-{profile}.properties`. Never hardcode an active profile in shared base config.

## Everyday Commands

```bash
# Start the application (default profile)
./mvnw spring-boot:run

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
# Run all tests
./mvnw test

# Run all tests + package JAR (full build lifecycle)
./mvnw verify

# Run a specific test class
./mvnw test -Dtest=ServiceNameTest

# Run a specific test method
./mvnw test -Dtest=ServiceNameTest#methodName
```

`test` runs unit tests. `verify` runs the full build lifecycle (compile, test, package). Use `verify` before committing.

## Two-Terminal Dev Setup (MapStruct)

If the project uses MapStruct for DTO mapping:

```bash
# Terminal 1 — keep running
./mvnw spring-boot:run

# Terminal 2 — only needed after changing a mapper interface
./mvnw compile
```

Most code changes (controllers, services, templates, JS, CSS) are handled by DevTools automatically. Only MapStruct mapper interface changes require a manual `./mvnw compile` to regenerate the implementation.

## Docker (Prod Profile Testing Pattern)

For local testing of the production profile, use Docker Compose to run the prod database alongside the app:

```bash
# First time or after code changes (rebuilds app image)
docker compose -f docker-compose.prod.yml up --build

# Subsequent runs
docker compose -f docker-compose.prod.yml up

# Stop and clean up (-v removes the data volume for a clean slate)
docker compose -f docker-compose.prod.yml down -v
```

Conventions for a prod-profile Compose setup:

- Use a different port than dev (e.g., 8081 if dev runs on 8080) to avoid conflicts
- Set `SPRING_PROFILES_ACTIVE=prod` and `DATABASE_URL` via environment
- Keep the DB user/password obvious in the Compose file (they're dev-only) but never commit real prod credentials

### Debug the app natively with containerized PostgreSQL

```bash
# Terminal 1 — start just PostgreSQL
docker compose -f docker-compose.prod.yml up postgres

# Terminal 2 — run app natively with prod profile + debug
DATABASE_URL=jdbc:postgresql://localhost:5432/yourdb?user=demo&password=demo \
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod -Pdebug
```

This gives you breakpoints and hot-reload while using real PostgreSQL instead of H2.

## Deployment Patterns

Spring Boot apps deploy well to:

- **PaaS** (Render, Heroku, Fly.io) — set `SPRING_PROFILES_ACTIVE=prod` and database env vars
- **Kubernetes** — via the generated JAR or a container image
- **Traditional servers** — via the JAR directly (`java -jar app.jar`)

Production env vars typically include:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- Any app-specific secrets or feature flags

Document the project's specific deployment provider in the project root's `OPERATIONS.md` (or similar) or `README.md` — not in this guide.

## CI Pipeline Pattern

Recommended CI workflow on every push and PR to the main branch:

1. Check out code
2. Set up JDK (match the project's version)
3. Run `./mvnw verify` (compile + all tests)

Fail the build on any test failure. Do not allow `-DskipTests` in CI. Use the `test` profile so tests are isolated from dev/prod config.

## Dependency Management

```bash
# Download/resolve all dependencies
./mvnw dependency:resolve

# Show the full dependency tree
./mvnw dependency:tree

# Show only a specific dependency
./mvnw dependency:tree -Dincludes=group:artifact

# Force re-download dependencies (if corrupted or stale cache)
./mvnw dependency:resolve -U
```

### Checking For Updates

```bash
# Check for newer versions of all dependencies
./mvnw versions:display-dependency-updates

# Check for newer Spring Boot parent version
./mvnw versions:display-parent-updates

# Check for newer plugin versions
./mvnw versions:display-plugin-updates
```

The Spring Boot parent manages versions for most Spring ecosystem dependencies. Bumping the parent version upgrades them as a tested-together set. Dependencies outside the parent (WebJars, third-party libs) are versioned individually in `pom.xml`.

Skip pre-release versions (milestones, alphas, RCs) unless you have a specific reason to adopt them.

## Common Workflow

```bash
# After adding a new dependency to pom.xml
./mvnw dependency:resolve

# After code changes
#   DevTools auto-restarts for most changes, but not pom.xml changes.
#   For pom.xml changes, restart the app manually.

# After changing a MapStruct mapper interface
#   Run in a second terminal while the app is running.
./mvnw compile

# Before committing
./mvnw clean test

# Produce a deployable JAR
./mvnw clean package
```

## Maven Wrapper

`./mvnw` (Mac/Linux) or `mvnw.cmd` (Windows) is the Maven Wrapper:

- Downloads the correct Maven version automatically — no global Maven install needed
- Config in `.mvn/wrapper/maven-wrapper.properties`
- Always use the wrapper instead of a system-installed `mvn` so the team shares the same version

## Useful Diagnostics

```bash
# Show the effective POM (resolved parent + this project)
./mvnw help:effective-pom

# Show active profiles
./mvnw help:active-profiles

# Show Maven version
./mvnw --version

# Validate pom.xml
./mvnw validate
```

## Dependencies

See `pom.xml` for the current dependency list. For the recommended starter dependencies in a new project, see [project-setup.md](project-setup.md).

## Related Guides

- [project-setup.md](project-setup.md) — bootstrap a new project from scratch
- [bootstrap-checklist.md](bootstrap-checklist.md) — day-one checklist
- [testing-guide.md](testing-guide.md) — what to test and how
- [database-and-migrations.md](database-and-migrations.md) — Flyway and schema evolution
- [agents-and-commands.md](agents-and-commands.md) — Claude Code agents and slash commands for daily development
