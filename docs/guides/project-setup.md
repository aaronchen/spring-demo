# Project Setup Guide

Purpose: bootstrap a new Spring Boot project with the same baseline stack and defaults used by this project.

Read this first, then read [architecture.md](architecture.md) and [bootstrap-checklist.md](bootstrap-checklist.md).

## Stack Assumptions

This guide assumes:

- Spring Boot 4.x
- Java 21+
- Maven
- Thymeleaf
- HTMX
- Stimulus + browser-native ES modules
- Spring Security
- Spring Data JPA
- H2 for local dev and tests
- PostgreSQL for production
- Flyway for production migrations

If a future project does not use this stack, use this guide selectively rather than copying it wholesale.

## 1. Create The Project

Use Spring Initializr with:

- Java 21+
- Spring Boot 4.x
- Dependencies:
  - Spring Web
  - Thymeleaf
  - Spring Security
  - Spring Data JPA
  - Validation
  - WebSocket
  - Flyway Migration
  - PostgreSQL Driver
  - H2 Database
  - DevTools
  - Lombok

## 2. Add Baseline Dependencies

Add the frontend dependencies your app actually uses. For this stack, prefer Maven-managed assets over npm unless you have a clear need for a frontend build pipeline.

Recommended additions:

- Bootstrap
- Bootstrap Icons
- HTMX
- Stimulus
- STOMP.js
- MapStruct
- `springdoc-openapi-starter-webmvc-ui`
- `spring-security-test`

Optional but recommended:

- Spotless

## 3. Add Baseline Plugins

Add:

- `maven-compiler-plugin`
  - MapStruct processor
  - Lombok processor
- `spring-boot-maven-plugin`
- `maven-surefire-plugin`
- `spotless-maven-plugin` if the project will format on build

Recommended policy:

- `./mvnw verify` should be the standard local validation command
- formatting should be deterministic
- tests should run under the `test` profile

## 4. Create The Baseline Config Files

Create:

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`
- `src/test/resources/application-test.properties`

Use a neutral base config. Do not hardcode a default active profile in `application.properties`.

### `application.properties`

Keep this file safe and environment-neutral.

Recommended baseline:

```properties
spring.application.name=my-app

spring.jpa.open-in-view=false
spring.mvc.problemdetails.enabled=true
spring.data.web.pageable.serialization-mode=via-dto

spring.web.resources.chain.strategy.content.enabled=true
spring.web.resources.chain.strategy.content.paths=/**

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

### `application-dev.properties`

Development-only convenience belongs here.

Typical baseline:

```properties
spring.datasource.url=jdbc:h2:mem:devdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.h2.console.enabled=true
spring.flyway.enabled=false
spring.thymeleaf.cache=false
```

### `application-prod.properties`

Production-only behavior belongs here.

Typical baseline:

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DATABASE_USERNAME:}
spring.datasource.password=${DATABASE_PASSWORD:}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

spring.h2.console.enabled=false
spring.thymeleaf.cache=true
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

### `application-test.properties`

Tests should be isolated from dev behavior.

Typical baseline:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.flyway.enabled=false
spring.thymeleaf.cache=false
```

## 5. Add Foundational Files

Create these early:

- `.editorconfig`
- `.prettierrc` if using Spotless for JS/CSS
- `src/main/resources/messages.properties`
- `src/main/resources/ValidationMessages.properties`
- `src/main/resources/templates/layouts/base.html`
- `src/main/resources/static/css/base.css`
- `src/main/resources/static/js/application.js`

## 6. Add The Minimum App Infrastructure

Before building features, add:

- `SecurityConfig`
- `WebExceptionHandler`
- `ApiExceptionHandler`
- `GlobalBindingConfig`
- `AppRoutesProperties`
- `FrontendConfigController`
- `RouteTemplate`
- a `Messages` helper

This gives the project:

- a route single source of truth
- shared runtime config for JS
- global i18n access
- consistent error handling
- trimmed form input

## 7. Add The Minimum Test Foundation

Create at least:

- one context boot test with `@ActiveProfiles("test")`
- one `@DataJpaTest`
- one `@SpringBootTest` + `@AutoConfigureMockMvc`
- one pure unit test with Mockito

This proves the test stack works before feature code accumulates.

## 8. Add The Base Layout

The base layout should provide:

- Bootstrap CSS/JS
- icon CSS
- app CSS
- import map
- CSRF meta tags
- `config.js`
- shared navbar/footer shell
- modal shell if the app uses HTMX modal loading

## 9. Add Dev-Only Tooling Carefully

Dev conveniences are allowed, but isolate them:

- H2 console only in `dev`
- demo data loader only in `dev`
- any local DB tooling only in `dev`
- Swagger UI disabled in `prod`

Never put these into the base profile.

## 10. First Verification Pass

Before adding the first real feature, make sure these commands succeed:

```bash
./mvnw test
./mvnw verify
```

If the project includes formatting on build, verify that `verify` also covers formatting.

## 11. First Feature Slice

Use one small feature to establish the house style:

- entity
- repository
- service
- web controller
- API controller
- request/response DTOs
- mapper
- Thymeleaf page/fragment
- one HTMX interaction
- unit/repository/integration tests

Do not add advanced infrastructure before one full vertical slice works.

## Guardrails

- Do not hardcode `spring.profiles.active` in the shared base config.
- Do not put dev-only DB behavior in `application.properties`.
- Do not start with hardcoded URLs in templates or JS; add `AppRoutesProperties` early.
- Do not leave testing for later.
- Do not add a frontend bundler unless the project really needs one.

## Related Guides

- [bootstrap-checklist.md](bootstrap-checklist.md)
- [architecture.md](architecture.md)
- [security-guide.md](security-guide.md)
- [testing-guide.md](testing-guide.md)
