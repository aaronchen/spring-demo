# Frontend Guide

Purpose: define how frontend behavior should be built in a Thymeleaf + HTMX + Stimulus + import-map app.

## Core Rule

Prefer server-rendered UI plus small client behaviors.

Use:

- Thymeleaf for initial markup
- HTMX for partial-page interactions
- Stimulus for client behavior and state local to a component/page
- REST + `fetch()` only when the UI truly needs JS-driven rendering

## Stack Assumptions

- Bootstrap
- HTMX
- Stimulus
- browser-native ES modules
- import maps
- WebJars or self-hosted static assets

## Responsibilities

### Thymeleaf

Use for:

- initial page rendering
- fragments
- forms
- static structure
- server-side conditional UI

### HTMX

Use for:

- modal body loading
- inline mutations
- fragment refresh
- actions that naturally fit request → HTML fragment

### Stimulus

Use for:

- keyboard shortcuts
- component behavior
- drag and drop
- WebSocket hookup
- DOM orchestration
- fetch-driven widgets like charts

Do not use Stimulus to reimplement the whole server-rendered app in the browser.

## File Structure

Recommended:

```text
static/js/
├── controllers/
├── lib/
├── components/
└── application.js
```

### `controllers/`

- one controller per UI concern
- no controller imports from other controllers
- communicate through DOM events instead

### `lib/`

Shared helpers only:

- API helpers
- toast/confirm helpers
- websocket client
- i18n helpers
- HTMX global listeners

### `components/`

Use for standalone custom elements when a plain Stimulus controller is not enough.

## Import Maps

Keep import maps centralized in the base layout.

Rules:

- all modules registered in one place
- use Thymeleaf URL rewriting for cache-busted URLs
- do not hardcode static asset URLs in JS

## Stimulus Conventions

### Naming

- file: `snake_case_controller.js`
- controller name: `kebab-case`
- nested folder controller: `folder--kebab-case`

### Behavior

- clean up listeners/subscriptions in `disconnect()`
- use typed values and targets
- keep one controller focused on one concern

### Cross-Controller Communication

Use custom DOM events:

- `tasks:refresh`
- `notifications:updated`
- `mention:clear`

Do not wire controllers together directly.

## HTMX Conventions

- use `th:hx-*` attributes directly
- do not use `th:attr` — use individual `th:*` attributes
- return fragments for HTMX requests
- use `HX-Trigger` for success events and toast triggers
- use out-of-band swaps only when they materially simplify the interaction

## Route Usage In Templates And JS

Use `appRoutes` and `APP_CONFIG.routes`.

Use the `RouteTemplate` builder API consistently: `.params().build()` / `.query().build()`.

Always call `.build()` when passing a route to `fetch()` or `htmx.ajax()` — these APIs expect a string, and `RouteTemplate` objects are not auto-coerced. Non-parameterized routes also need `.build()` (e.g., `APP_CONFIG.routes.apiPins.build()`). Thymeleaf `th:hx-*` attributes call `.toString()` implicitly, so `.build()` is not needed there.

Do not hardcode URLs in:

- templates
- inline JS
- Stimulus controllers

## `APP_CONFIG`

Shared browser config belongs in `APP_CONFIG`:

- routes
- messages (accessed via `t()` from `lib/i18n.js`)
- enum metadata

Do not put controller-specific data into `APP_CONFIG`.

### Message Access

Use `t(key, ...args)` from `lib/i18n.js` for all message lookups. Never access `APP_CONFIG.messages` directly outside `i18n.js`.

- `t("key")` — plain lookup, returns `undefined` if missing
- `t("key", arg1, arg2)` — parameterized, replaces `{0}`, `{1}`, etc.
- `t("key") || "fallback"` — defensive default for UI labels
- `resolveLabel(prefix, enumValue)` — enum label lookup, delegates to `t()` internally

For controller-specific data, use Stimulus values.

## Data Flow To JavaScript

Preferred order:

1. Stimulus values for controller-specific inputs
2. `APP_CONFIG` for global shared config
3. `<meta>` tags only for page-level shared data across multiple controllers
4. `<template>` for server-owned HTML clones

## Inline Scripts

Avoid them.

If one is unavoidable:

- keep it self-contained
- do not import modules from it
- do not duplicate logic that belongs in a controller/lib module

## CSS Organization

Recommended:

- `base.css`
- `theme.css`
- feature/page CSS files

Keep design tokens and theme variables centralized.

## New Page Bootstrap Pattern

For every new interactive page:

1. add the page template
2. add a controller only if needed
3. pass page-specific inputs through Stimulus values
4. prefer HTMX for server-rendered fragments
5. add JS fetch only when HTML fragments are the wrong tool

## Anti-Patterns

- hardcoded routes in JS
- inline onclick handlers
- controller-to-controller imports
- dumping all behavior into one large page script
- rebuilding server-rendered HTML in JS when a fragment would do

## Related Guides

- [backend-frontend-communication.md](backend-frontend-communication.md)
- [conventions.md](conventions.md)
