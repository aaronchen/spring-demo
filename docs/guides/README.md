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

1. [project-setup.md](project-setup.md)
2. [bootstrap-checklist.md](bootstrap-checklist.md)
3. [architecture.md](architecture.md)
4. [security-guide.md](security-guide.md)
5. [backend-guide.md](backend-guide.md)
6. [frontend-guide.md](frontend-guide.md)
7. [backend-frontend-communication.md](backend-frontend-communication.md)
8. [testing-guide.md](testing-guide.md)
9. [database-and-migrations.md](database-and-migrations.md)
10. [css-guide.md](css-guide.md)
11. [conventions.md](conventions.md)
12. [dev-guide.md](dev-guide.md)
13. [agents-and-commands.md](agents-and-commands.md)
14. [example-feature-walkthrough.md](example-feature-walkthrough.md)

## Guide Roles

- Setup: create a safe project baseline
- Architecture: explain system shape and boundaries
- Security: define enforcement model
- Backend/Frontend: guide feature implementation
- Communication: decide how server and browser interact
- Testing: define quality gates
- Database: define persistence and migration strategy
- CSS: define visual design system, palettes, tokens, and component patterns
- Conventions: quick reference only
- Dev: everyday Spring Boot dev workflow (profiles, Maven, Docker, CI patterns)
- Agents and commands: document the Claude Code AI tooling that supports daily development
- Example walkthrough: concrete shape of one full feature slice, to be used as a reference

## Important Rule

These guides must describe the current implemented standard, not a proposed future API. If the code changes, update the guides in the same branch.
