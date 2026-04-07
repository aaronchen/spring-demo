# Architecture Guide

Purpose: describe the structural patterns this project settled on and should be reused by future projects built on the same stack.

Read after [project-setup.md](/Users/aaron/Code/spring-demo/docs/guides/project-setup.md).

## Core Principles

- Server-render first
- Keep Java as the source of truth for rules, routes, and labels
- Use HTMX for partial-page interactions
- Use Stimulus for client behavior, not for owning business state
- Keep security enforcement on the server
- Prefer explicit boundaries over convenience shortcuts

## Layering

```text
Spring Security Filter Chain
        ↓
Controllers (web + API)
        ↓
Access guards / auth helpers
        ↓
Services
        ↓
Repositories
        ↓
Database
```

Controllers should not bypass services to call repositories directly.

## Dual Interface Pattern

The app exposes two interfaces over the same domain:

- Web UI controllers
- REST API controllers

Both use the same service layer. This keeps web behavior and API behavior consistent while allowing different response shapes.

Use this pattern when:

- the app has a server-rendered UI
- the app also needs JS-driven data access or public/internal API endpoints

## Package Organization

Current default:

```text
audit/
config/
controller/
controller/admin/
controller/api/
dto/
event/
exception/
mapper/
model/
presence/
report/
repository/
security/
service/
util/
```

This is the current house style for this project family.

It is a default, not a law. If a future app is larger or more domain-heavy, feature packaging may be reasonable. If that happens, make it a deliberate architectural decision rather than a gradual drift.

## Service Boundaries

Default rule:

- services own business logic
- repositories stay behind services
- cross-domain work happens through service collaboration, not repository sharing

When a service grows too large, split by responsibility:

- write operations
- read/query operations
- policy/workflow operations
- supporting orchestration helpers

Examples from this project family:

- `TaskService`
- `TaskQueryService`
- project access/membership policies

Do not split services preemptively. Split when responsibilities are clearly diverging.

## Query vs Command Separation

Use query services when they materially improve:

- reuse across controllers/services
- circular-dependency prevention
- read-only transactional boundaries
- clarity of “reads vs writes”

Do not invent command/query classes for every domain up front. Use them when the service boundary is becoming unclear.

## Transaction Boundaries

Defaults:

- OSIV disabled
- write services are transactional
- query methods are either read-only transactional or backed by repository methods that fully load what they need
- event listeners that depend on committed state should use `@TransactionalEventListener`

Implication:

- if a mapper/template/API needs lazy data, you must load it inside the transaction or use `@EntityGraph`

## Event-Driven Side Effects

Use events for side effects that should not be hardwired into the main business operation:

- audit logs
- notifications
- WebSocket broadcasts
- recently viewed/title sync

Use `ApplicationEventPublisher` in services.

Keep delivery concerns out of core services where practical.

Do not force everything through events. Scheduled jobs and tightly coupled domain writes can still call services directly.

## Security Layers

This architecture assumes three layers:

1. URL-level security in `SecurityConfig`
2. controller/service-level enforcement via guards and checks
3. template-level visibility rules for UX only

Template checks are never a security boundary.

See [security-guide.md](/Users/aaron/Code/spring-demo/docs/guides/security-guide.md).

## Route Single Source Of Truth

All URLs centralized in `AppRoutesProperties` as `RouteTemplate` fields. `RouteTemplate.Builder` provides an immutable fluent API (`.params().query().build()`) that works symmetrically in Java, Thymeleaf, and JS. `FrontendConfigController` auto-exposes all routes to the browser via `APP_CONFIG.routes`.

Never hardcode URLs in Java, templates, or JS. Never concatenate path segments manually.

See [backend-frontend-communication.md](backend-frontend-communication.md) for API usage and examples.

## Runtime Config For JS

Expose shared browser config through `/config.js`:

- routes
- messages
- enum metadata

This avoids duplicating route strings and label maps in JS.

## Enum Presentation Pattern

If an enum is displayed in the UI, let the enum own presentation metadata:

- message key
- CSS class
- icon
- chart color

Then expose that metadata to JS through `config.js`.

This keeps server templates and JS aligned.

## Access Guards

Use dedicated guards when access rules are domain-specific:

- `OwnershipGuard`
- `ProjectAccessGuard`

Guards belong close to the security layer, not buried in templates.

They should answer questions like:

- can this user view this resource?
- can this user edit this resource?
- can this user administer this resource?

## Recommended Defaults For Future Projects

Start with:

- layered architecture
- dual interface pattern
- route single source of truth
- guards for domain-specific access
- event-driven side effects
- query/service split only where it adds clarity

## Related Guides

- [backend-guide.md](/Users/aaron/Code/spring-demo/docs/guides/backend-guide.md)
- [backend-frontend-communication.md](/Users/aaron/Code/spring-demo/docs/guides/backend-frontend-communication.md)
- [security-guide.md](/Users/aaron/Code/spring-demo/docs/guides/security-guide.md)
