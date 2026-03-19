# Roadmap

## Phase 1: Structural Cleanup ✅
1. ~~Extract `TaskQueryService` for read-only task lookups~~ — done
2. ~~Extract `CommentQueryService` for read-only comment lookups~~ — done
3. ~~Move `AuditLogService` into `audit/` package~~ — done
4. ~~Constructor formatting: one parameter per line~~ — done
5. Controllers stay in `controller/` (not moved to feature packages — see architecture decisions)

## Phase 2: Transactional Integrity & Core Fixes
4. Add `@Transactional` to write methods in `TaskService`, `UserService`, `CommentService`, `TagService`, `SettingService`, `UserPreferenceService`, `NotificationService`
5. Switch `@EventListener` to `@TransactionalEventListener(phase = AFTER_COMMIT)` on `AuditEventListener`, `NotificationEventListener`, `WebSocketEventListener`
6. Disable OSIV (`spring.jpa.open-in-view=false`) and fix any lazy-loading breakages that surface
7. Remove unnecessary `spring.jpa.database-platform=org.hibernate.dialect.H2Dialect`

## Phase 3: Environment Separation
8. Create Spring profiles: `dev`, `test`, `prod`
9. Gate DataLoader behind `dev` profile (`@Profile("dev")`)
10. Gate H2 web server bean behind `dev` profile
11. Gate H2 console (`spring.h2.console.enabled`) behind `dev` profile
12. Move `show-sql=true` to `application-dev.properties` only
13. Gate H2 console `permitAll()` in SecurityConfig behind `dev` profile

## Phase 4: Testing
14. Unit tests for services (`TaskService`, `UserService`, `CommentService`, `TagService`)
15. Repository tests (custom queries, specifications)
16. Controller tests — MockMvc for web controllers (HTMX fragment responses, redirects)
17. Controller tests — MockMvc for API controllers (JSON responses, status codes)
18. Security tests (role-based access, ownership checks, CSRF behavior)
19. Validation tests (`@Unique`, `@NotBlank`, `@Size`, form binding)
20. Integration tests with `test` profile (full request lifecycle)

## Phase 5: API Quality
21. Add pagination to `GET /api/tasks` (match web UI's existing pagination)
22. Switch `ApiExceptionHandler` to `ProblemDetail` responses (RFC 9457 / Spring 6 native support)
23. Add springdoc-openapi for auto-generated API documentation

## Phase 6: Production Readiness
24. Add `application-prod.properties` with PostgreSQL config
25. Add Docker Compose with PostgreSQL service
26. Add Flyway for schema migrations (initial migration from current DDL)
27. Configure actuator endpoints (health details, metrics exposure, info)
28. Basic CI pipeline — GitHub Actions: build, test, lint

## Phase 7: Future Considerations (Not Currently Planned)
29. API versioning
30. Rate limiting / brute-force protection
31. Mutation testing
32. Container orchestration (Kubernetes)
33. Custom observability (tracing, log correlation)
34. Task toggle authorization tightening (current "any user can toggle" is intentional)
35. Move authorization logic from controllers into service layer (tradeoff: reduces duplication across web/API controllers, but couples services to Spring Security context)
