# Operations

Project-specific infrastructure, deployment, and CI details for this app.

For generic Spring Boot dev commands, profiles, Docker patterns, and dependency management, see [docs/guides/dev-guide.md](docs/guides/dev-guide.md).

## Test Suite

318 tests across 39 test classes, run under the `test` profile with H2.

```bash
./mvnw test
```

## Docker: Running Prod Profile Locally

Requires Docker. Uses `docker-compose.prod.yml`, which starts PostgreSQL 18 plus the app with the `prod` profile.

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
# Ctrl+C in the terminal, then clean up:
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

This gives you breakpoints and hot-reload while using PostgreSQL instead of H2.

### How it works

Docker Compose reads `docker-compose.prod.yml` and does two things:

1. Starts a PostgreSQL 18 container (database: `springdemo`, user: `demo`, password: `demo`)
2. Builds the app from the `Dockerfile`, then runs it with `SPRING_PROFILES_ACTIVE=prod` and `DATABASE_URL` pointing at the PostgreSQL container

The image name (`spring-demo-app`) is auto-generated from the folder name + service name.

## Deployment (Render)

The app is deployed on Render using the `dev` profile (H2 in-memory). Render builds from the `Dockerfile` on every push to `main`.

- Data resets on each deploy (H2 is in-memory)
- `.github/workflows/daily-redeploy.yml` GitHub Action triggers a Render rebuild daily at 6 AM UTC+8

To switch Render to PostgreSQL: create a Render PostgreSQL instance, then set `SPRING_PROFILES_ACTIVE=prod` and `DATABASE_URL` as environment variables on the web service.

## CI Pipeline (GitHub Actions)

`.github/workflows/ci.yml` runs on every push to `main` and every PR targeting `main`:

1. Checks out the code
2. Sets up JDK 25
3. Runs `./mvnw verify` (compile + all 318 tests using H2)

Results show as a green check or red X on the commit/PR.
