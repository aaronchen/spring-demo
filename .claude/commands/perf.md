---
description: Run the performance-reviewer agent on the given scope
---

Use the `performance-reviewer` agent to audit the following scope for performance issues — N+1 queries, missing `@EntityGraph`, OSIV-disabled lazy-load bugs, unbounded queries, algorithmic hotspots, and transaction scope issues:

$ARGUMENTS

If no scope was given, ask the user: file, feature, or "recent changes" (`git diff main...HEAD --name-only`).

Every finding must name the mechanism (query count, complexity class, cartesian join, etc.) — no vague "this is slow" claims.
