# Security Guide

Purpose: define the default security model for future projects built in this style.

## Core Rule

Server-side enforcement is the security boundary.

Templates may hide or show UI, but they do not grant access.

## Layers

### 1. URL-Level Security

Handled in `SecurityConfig`.

Use it for:

- authentication requirements
- broad role-based restrictions
- static resource allowances
- login/logout setup
- CSRF policy

### 2. Guard-Level Enforcement

Handled in guards and controller/service checks.

Use it for:

- ownership
- project membership
- project role rules
- resource-specific permissions

Examples:

- `OwnershipGuard`
- `ProjectAccessGuard`

### 3. Template Visibility

Use template checks only for:

- hiding unavailable actions
- reducing user confusion
- presenting role-aware UI

Never rely on template checks alone.

## Authentication Model

Default model:

- form login
- session-based auth
- custom `UserDetailsService`
- password hashing with BCrypt

Use a different model only if the product requires it.

## Authorization Model

Separate:

- system roles
- domain/resource roles

Example:

- system role: `ADMIN`
- domain role: project `OWNER` / `EDITOR` / `VIEWER`

This is more scalable than forcing all authorization into top-level roles.

## CSRF

Default:

- protect browser form and HTMX interactions
- inject token centrally for HTMX
- exempt only routes that are intentionally outside that model

If `/api/**` is CSRF-exempt, document why.

## API vs Web

Keep API and web behavior distinct where needed:

- API errors return `ProblemDetail`
- web errors render pages or redirect
- API endpoints may have different CSRF and response expectations

## Guard Design Rules

Guards should:

- accept current user/context explicitly
- answer one clear access question
- throw `AccessDeniedException` or return a clear boolean

Avoid burying large access rules in templates or repositories.

## Admin Bypass

If the project has an admin bypass rule, define it clearly and apply it consistently.

Do not let “sometimes admin bypasses, sometimes not” evolve by accident.

## What Every New Protected Feature Needs

- URL-level rule review
- guard/service enforcement review
- template visibility review
- positive and negative tests

## Related Guides

- [architecture.md](/Users/aaron/Code/spring-demo/docs/guides/architecture.md)
- [testing-guide.md](/Users/aaron/Code/spring-demo/docs/guides/testing-guide.md)
