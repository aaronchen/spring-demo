# Guides Overview

These guides capture the baseline architecture, conventions, and implementation patterns from this project so future Spring Boot projects can start from a stronger base.

## Assumed Stack

- Spring Boot
- Thymeleaf
- HTMX
- Stimulus
- Spring Security
- Spring Data JPA
- H2 for dev/test
- PostgreSQL for production

If a future project uses a different stack, use these guides selectively.

## Reading Order

1. [project-setup.md](/Users/aaron/Code/spring-demo/docs/guides/project-setup.md)
2. [bootstrap-checklist.md](/Users/aaron/Code/spring-demo/docs/guides/bootstrap-checklist.md)
3. [architecture.md](/Users/aaron/Code/spring-demo/docs/guides/architecture.md)
4. [security-guide.md](/Users/aaron/Code/spring-demo/docs/guides/security-guide.md)
5. [backend-guide.md](/Users/aaron/Code/spring-demo/docs/guides/backend-guide.md)
6. [frontend-guide.md](/Users/aaron/Code/spring-demo/docs/guides/frontend-guide.md)
7. [backend-frontend-communication.md](/Users/aaron/Code/spring-demo/docs/guides/backend-frontend-communication.md)
8. [testing-guide.md](/Users/aaron/Code/spring-demo/docs/guides/testing-guide.md)
9. [database-and-migrations.md](/Users/aaron/Code/spring-demo/docs/guides/database-and-migrations.md)
10. [conventions.md](/Users/aaron/Code/spring-demo/docs/guides/conventions.md)

## Guide Roles

- Setup: create a safe project baseline
- Architecture: explain system shape and boundaries
- Security: define enforcement model
- Backend/Frontend: guide feature implementation
- Communication: decide how server and browser interact
- Testing: define quality gates
- Database: define persistence and migration strategy
- Conventions: quick reference only

## Important Rule

These guides must describe the current implemented standard, not a proposed future API. If the code changes, update the guides in the same branch.
