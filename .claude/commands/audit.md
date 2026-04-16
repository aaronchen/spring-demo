---
description: Run the convention-auditor agent on the given scope
---

Use the `convention-auditor` agent to audit the following scope for project-convention violations:

$ARGUMENTS

If no scope was given, ask the user: file, directory, package, or "recent changes" (`git diff main...HEAD --name-only`).

Report findings grouped by convention with severity ranking. Do not auto-apply fixes — let the user decide.
