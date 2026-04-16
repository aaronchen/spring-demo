# Backend-Frontend Communication Guide

Purpose: define how server and browser exchange data in this stack.

This is the operational guide to use while building features.

## Decision Matrix

Use the simplest mechanism that fits the interaction.

| Situation | Use |
|---|---|
| initial page | Thymeleaf full-page render |
| modal body / inline edit / row refresh | HTMX fragment |
| create/update then redirect | full-page POST + redirect |
| JS-rendered chart or data widget | REST API + `fetch()` |
| live cross-user updates | WebSocket |
| shared routes/messages/enums | `config.js` / `APP_CONFIG` |

## HTMX

HTMX is the default for server-driven interactivity.

Use it for:

- modal content loading
- list refreshes
- button actions
- partial form submissions
- fragment swaps

### Controller Pattern

Detect HTMX requests and return a fragment or partial template when appropriate.

### Response Trigger Pattern

Use `HX-Trigger` to notify the browser about:

- save success
- delete success
- toast events
- lightweight refresh hints

## Stimulus

Stimulus owns browser behavior, not primary data truth.

Use it for:

- keyboard shortcuts
- drag and drop
- local component state
- WebSocket subscriptions
- fetch-driven widgets

## REST API

Use REST endpoints for:

- JS-rendered charts
- search/autocomplete responses
- data-first interactions where HTML fragments are not the right fit

Avoid using JSON APIs for interactions that are better expressed as server-rendered fragments.

## `config.js`

Use `/config.js` to expose browser runtime config:

- routes
- messages
- enum metadata

This keeps browser code aligned with Java-side source of truth.

## Routes

Routes are centralized in:

- `AppRoutesProperties`
- `RouteTemplate`
- `FrontendConfigController`

### Current Standard

Use the `RouteTemplate` builder API: `.params().build()` / `.query().build()`.

Examples:

```java
appRoutes.getTaskDetail().params("taskId", id).build()
appRoutes.getTasks().query("selectedUserId", userId).build()
appRoutes.getTaskToggle().params("taskId", id).query("view", "table").build()
```

```html
${appRoutes.taskEdit.params('taskId', task.id).build()}
${appRoutes.taskNew.query('projectId', project.id).build()}
```

```javascript
APP_CONFIG.routes.taskEdit.params({ taskId: id }).build()
APP_CONFIG.routes.apiTasks.query({ projectId: id }).build()
```

### Rules

- never hardcode URLs in templates
- never hardcode URLs in JS
- never manually concatenate path segments when a route exists

## Thymeleaf Attribute Guidance

Prefer dedicated attributes:

- `th:hx-get`
- `th:hx-post`
- `th:hx-delete`
- `th:hx-patch`
- `th:href`
- `th:src`

Never use `th:attr`. Use individual `th:*` attributes instead (`th:hx-get`, `th:data-sort-dir`, `th:aria-label`, etc.). `th:attr` splits on commas, which breaks expressions containing method calls.

## CSRF

The standard pattern is:

- render CSRF meta tags in the base layout
- inject the token in a shared HTMX listener
- exempt `/api/**` only if that matches the application’s security model

Document the project’s API CSRF stance explicitly.

## Shared Labels And Enum Metadata

Put user-facing labels in `messages.properties`.

Expose enum presentation metadata through `APP_CONFIG.enums` so:

- templates and JS use the same CSS/icon/color definitions
- browser code does not hardcode enum presentation maps

## Browser Data Sources

Preferred order:

1. Thymeleaf model into HTML
2. Stimulus values for controller-local inputs
3. `APP_CONFIG` for global shared runtime config
4. REST endpoints for dynamic JSON data
5. WebSocket for live updates

## WebSocket

Use WebSocket for live updates that originate from other users or background activity.

Typical uses:

- notifications
- presence
- task/project change notices

Do not move normal request/response interactions to WebSocket unless there is a real reason.

## Templates For JS-Cloned Markup

If JS must insert HTML that the server conceptually owns, prefer `<template>` in Thymeleaf over string-building HTML in JS.

This keeps:

- i18n on the server
- markup structure in one place
- JS focused on behavior

## Toasts And Confirmation

Success and error UX should be driven through standard helpers and triggers, not ad hoc inline scripts.

Use:

- shared toast helper
- shared confirm helper
- HTMX trigger headers where appropriate

## Anti-Patterns

- hardcoded browser route strings
- inline `<script>` fragments emitted by HTMX responses
- `window.*` globals for feature-local data
- duplicating enum presentation rules in JS
- using the REST API for interactions that should be fragment-driven

## Related Guides

- [frontend-guide.md](frontend-guide.md)
- [architecture.md](architecture.md)
- [conventions.md](conventions.md)
