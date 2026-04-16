---
description: Run the security-reviewer agent on the given scope
---

Use the `security-reviewer` agent to audit the following scope for security issues across the three-layer authorization model (URL-level, controller-level, template-level), CSRF posture, input validation, and OWASP Top 10 concerns:

$ARGUMENTS

If no scope was given, ask the user: file, feature, or "recent changes" (`git diff main...HEAD --name-only`).

Every finding must include the exploit path, not just the rule violated. Rank by severity (🔴/🟠/🟡/🟢).
