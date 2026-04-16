# Testing Guide

Purpose: define the minimum testing approach for future projects in this style.

## Core Rule

Every feature should prove:

- business behavior
- persistence behavior when relevant
- security behavior when relevant
- web/API behavior when relevant

## Test Types

### Unit Tests

Use for:

- service logic
- helpers
- event listeners
- policies/guards

Prefer Mockito or plain unit tests here.

### Repository Tests

Use `@DataJpaTest` for:

- specifications
- custom repository methods
- entity mapping behavior that depends on JPA

### Integration / Controller Tests

Use `@SpringBootTest` + `@AutoConfigureMockMvc` when:

- security filter chain matters
- request routing matters
- response behavior depends on real application wiring

### Context Boot Test

Keep one basic context boot test under the `test` profile.

## Test Profile

Always run tests under `test`.

The test profile should:

- avoid demo data
- avoid dev-only tooling
- use isolated in-memory DB or dedicated test DB
- stay deterministic

## What New Features Should Test

### Service Layer

- success path
- validation/business-rule failures
- permission failures if enforced there
- edge cases

### Repository Layer

- filtering
- sorting
- pagination
- relationship loading when custom behavior matters

### Controller/API Layer

- happy path
- invalid input
- unauthorized/forbidden behavior
- not found behavior
- conflict behavior if relevant

### Web UI / HTMX

At least test:

- endpoint access rules
- correct fragment/full-page response path where meaningful
- redirect behavior

## Suggested Baseline Per Feature

- 1+ unit test class
- repository test if query logic changed
- MockMvc test if endpoint behavior changed

## Anti-Patterns

- relying only on `contextLoads()`
- putting all logic verification into controller tests
- skipping negative permission cases
- letting test fixtures depend on dev data loaders

## Related Guides

- [backend-guide.md](backend-guide.md)
- [security-guide.md](security-guide.md)
