# Database And Migrations Guide

Purpose: define the persistence and schema evolution defaults for future projects in this style.

## Entity Defaults

- use `LAZY` associations unless there is a strong reason not to
- choose `Set` vs `List` based on whether order matters
- use explicit join tables and column names when clarity helps
- keep entity invariants obvious

## IDs

Current default for core entities:

- `UUID`

Do not mix `UUID` and `Long` without reason.

## Relationship Loading

Because OSIV is disabled:

- fetch what you need in the service/repository layer
- use `@EntityGraph` where appropriate
- do not rely on template-time lazy loading

## Migrations

Production schema changes should go through Flyway.

Recommended policy:

- prod uses Flyway
- test can use `create-drop` or migrations depending on cost/benefit
- dev may use either, but the project should document which path is standard

## Migration Workflow

When schema changes:

1. change entity/model code
2. add migration
3. update tests
4. verify app boot and repository behavior

## Derived Data / `@Formula`

Use `@Formula` sparingly:

- good for small derived read helpers
- avoid building business logic around fragile formulas

## Audit / Supporting Tables

If the app uses audit, notification, or cross-cutting support tables:

- treat them as part of the schema model
- document whether their IDs follow the core entity strategy or not

## Related Guides

- [backend-guide.md](backend-guide.md)
- [project-setup.md](project-setup.md)
