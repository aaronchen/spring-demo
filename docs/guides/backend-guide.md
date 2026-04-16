# Backend Guide

Purpose: define the backend defaults for future Spring Boot projects built in this style.

## Entities

### General Rules

- keep entities focused on persistence and domain state
- use validation annotations for basic invariants
- prefer explicit field names and constants when the field names are reused in specs, sorting, audit snapshots, or mapping

### ID Strategy

Current default:

- use `UUID` for core user-facing entities
- use `Long` only when there is a clear reason

Do not mix strategies casually. Pick one deliberately per project.

### Relationship Defaults

- default associations to `LAZY`
- use `@EntityGraph` for repository methods that must fully load graphs
- use `Set` for unordered collections
- use `List` when order is meaningful

This is the better rule than “always Set”:

- unordered relationship: `Set`
- ordered relationship: `List`

### Audit Fields

Typical pattern:

- `createdAt`
- `updatedAt`
- `@PrePersist`
- `@PreUpdate`

Use this consistently for user-facing core entities.

## DTOs

Use separate DTOs by responsibility:

- `*Request` for JSON body input
- `*Response` for API output
- `*FormRequest` for Thymeleaf form binding
- `*Query` for URL/query-string binding

This is important. Do not use one DTO for all contexts by default.

## Mapping

Default to MapStruct for entity/DTO mapping.

Use manual mapping only when:

- transformation is highly conditional
- mapping depends on service lookups
- mapping logic would be harder to understand in MapStruct

Keep repository lookups out of mappers.

## Validation

Use:

- Bean Validation on DTOs/entities
- `ValidationMessages.properties`
- a global `StringTrimmerEditor` for form-bound strings

Remember:

- `@ModelAttribute` benefits from binder trimming
- `@RequestBody` does not

On form validation errors, restore any supporting model state needed to re-render the page:

- selected relationships
- options lists
- mode flags
- dependent UI state

## Transactions

Defaults:

- `@Transactional` on write services
- read methods either in read-only services or explicit `@Transactional(readOnly = true)` methods where needed
- listeners that require committed state use `@TransactionalEventListener`

Do not scatter transactions at controller level.

## Repositories

Repositories should:

- stay persistence-focused
- expose query methods and specifications
- not absorb business rules

Use:

- derived query methods
- `JpaSpecificationExecutor`
- `@EntityGraph`

Use specifications for user-driven filtering, search, and list screens.

## Error Handling

Use dual handling:

- API controllers return `ProblemDetail`
- web controllers render error pages or redirect safely

Keep API and web concerns separate.

## Profiles

Recommended profile model:

- `dev`
- `test`
- `prod`

Do not hardcode an active profile in shared config.

### Dev

- H2 or lightweight local DB
- demo data if needed
- SQL logging if useful
- no production secrets required

### Test

- isolated DB
- no demo data
- deterministic config

### Prod

- real DB
- Flyway enabled
- `ddl-auto=validate`

## Migrations

Use Flyway for production schema evolution.

You may choose whether dev uses:

- Flyway too
- or `create-drop` for fast local iteration

But document the decision. Do not let it be accidental.

See [database-and-migrations.md](database-and-migrations.md).

## Security-Aware Backend Design

Backend code should assume UI checks are untrusted.

Enforce access in:

- `SecurityConfig`
- guards
- service checks where needed

Prefer guard/policy classes for reusable access rules rather than repeating conditionals in controllers.

## Scheduled Jobs

Use `@Scheduled` only for tasks that truly belong to the application.

Rules:

- idempotent
- safe on retries
- handles missing/disabled data
- observable in logs

## Reporting / Export

If the project exports CSV or similar data:

- use the same filters/sorts as the list view
- keep export helpers reusable
- avoid copy-paste row formatting inside controllers

## What Every New Backend Feature Should Include

- entity changes if needed
- migration if needed
- request/response/form/query DTOs as appropriate
- mapper updates
- service logic
- guard/security checks
- API error behavior
- repository/spec coverage
- tests

## Related Guides

- [architecture.md](architecture.md)
- [security-guide.md](security-guide.md)
- [testing-guide.md](testing-guide.md)
- [database-and-migrations.md](database-and-migrations.md)
