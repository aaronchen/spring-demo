# Bootstrap Checklist

Use this when starting a new project in this style.

## Day 1

1. Create the Spring Boot app with baseline dependencies.
2. Add base profile files:
   - `application.properties`
   - `application-dev.properties`
   - `application-prod.properties`
   - `application-test.properties`
3. Add formatter/editor config.
4. Add `messages.properties` and `ValidationMessages.properties`.
5. Add base layout, base CSS, and `application.js`.

## Infrastructure Baseline

6. Add `SecurityConfig`.
7. Add API and web exception handling.
8. Add `AppRoutesProperties`, `RouteTemplate`, and `FrontendConfigController`.
9. Add global binding and message helpers.
10. Add WebSocket config only if the app truly needs it.

## Test Baseline

11. Add one context boot test under `test`.
12. Add one repository test.
13. Add one MockMvc integration test.
14. Ensure `./mvnw test` passes.

## First Feature Slice

15. Build one end-to-end feature:
   - entity
   - repository
   - service
   - web controller
   - API controller
   - DTOs
   - mapper
   - template
   - test coverage

## Production Readiness Baseline

16. Add Flyway if production DB is part of the project.
17. Add prod-safe settings.
18. Ensure dev-only tooling is isolated to `dev`.
19. Ensure `./mvnw verify` passes.

## Documentation

20. Update guides to reflect actual implemented patterns before the branch merges.
