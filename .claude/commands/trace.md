---
description: Run the route-finder agent to trace a URL or route through the stack
---

Use the `route-finder` agent to trace the following route through every layer (AppRoutesProperties definition, controller handler, Thymeleaf usages, JavaScript usages, test coverage):

$ARGUMENTS

If no route name, URL, or controller method was given, ask the user for one.

Also flag any hardcoded URL usages that bypass `AppRoutesProperties` / `APP_CONFIG.routes`.
