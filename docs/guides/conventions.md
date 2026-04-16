# Conventions Guide

Purpose: quick reference for naming, style, and reusable defaults.

This file should stay short. If a section needs heavy explanation, move it to another guide.

## Enforced Conventions

### Java

- constructor injection
- explicit imports
- 4-space indentation
- deterministic formatter

### JavaScript

- `const` by default
- semicolons
- 4-space indentation
- no `var`

### Messages

- all user-facing strings belong in `messages.properties`

### Templates

- use dedicated `th:*` attributes instead of `th:attr`
- no inline event handler attributes like `onclick`

## Preferred Naming

| Type | Convention |
|---|---|
| Entity | `Task`, `Project`, `User` |
| Repository | `TaskRepository` |
| Service | `TaskService`, `TaskQueryService` |
| Web controller | `TaskController` |
| API controller | `TaskApiController` |
| Request DTO | `TaskRequest` |
| Response DTO | `TaskResponse` |
| Query DTO | `TaskListQuery` |
| Form DTO | `TaskFormRequest` |
| Mapper | `TaskMapper` |
| Guard | `ProjectAccessGuard`, `OwnershipGuard` |

## DTO Rules

- JSON body input: `*Request`
- API output: `*Response`
- URL/query binding: `*Query`
- form binding: `*FormRequest`

## Enum Rules

If an enum is displayed in the UI, let it own:

- message key
- presentation metadata if needed

## Thymeleaf Rules

- use `appRoutes` for routes
- use `#{...}` for messages
- use `th:hx-*` directly for HTMX attributes

## Route Rules

Use the current `RouteTemplate` API consistently across:

- Java
- Thymeleaf
- JavaScript

Do not document future route APIs here until they are implemented.

## Exceptions

Exceptions are allowed when:

- the formatter/build/test setup cannot enforce the rule
- the rule makes the code harder to read in a specific case
- the project deliberately chooses a different architecture

If exceptions become common, the guide should be updated.

## Related Guides

- [architecture.md](architecture.md)
- [backend-guide.md](backend-guide.md)
- [frontend-guide.md](frontend-guide.md)
