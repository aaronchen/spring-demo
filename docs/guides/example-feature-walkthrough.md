# Example Feature Walkthrough

Purpose: show the expected shape of one complete feature slice.

Use this as a mental template, not a rigid scaffold.

## Example Slice

Feature: project-scoped task label or similar small domain feature.

Expected moving parts:

1. Model/entity change if needed
2. Migration if needed
3. Repository method/spec update
4. Service logic
5. Access/guard review
6. API DTO update
7. Mapper update
8. Web controller update
9. API controller update
10. Thymeleaf update
11. HTMX/Stimulus update if needed
12. Messages update
13. Tests

## Checklist

- Is the route centralized?
- Is security enforced server-side?
- Are user-facing strings externalized?
- Does API behavior return the right error model?
- Does the feature have at least unit plus endpoint/repository coverage as needed?

## Why This Matters

Future projects fail when they start with infrastructure but no proven vertical slice. Build one complete feature early and use it as the style reference for later work.
