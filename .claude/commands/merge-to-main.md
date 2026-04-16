---
description: Run the full quality gauntlet (all review agents + tests + docs) and merge the current feature branch to main
---

The user is ready to merge the current feature branch to `main`. This is the final ritual before the code becomes part of the project history.

Execute the gauntlet below in order. Between phases, report findings, apply fixes with user approval, and re-run upstream phases if fixes invalidated them. Do NOT auto-apply agent suggestions or auto-merge — always confirm with the user.

## Pre-flight

1. Check current branch: `git rev-parse --abbrev-ref HEAD`. If on `main`, stop and tell the user — this command runs on a feature branch.
2. Ensure working tree is committed so far: `git status`. If uncommitted changes exist, ask the user to stage/commit or stash before proceeding.
3. Identify change scope: `git diff main...HEAD --stat` and `git log main..HEAD --oneline`. Share a brief summary.

## Phase 1 — Coverage check

**Agent:** `test-writer`

List source files changed in the branch. For each, verify a corresponding test file exists and covers the new behavior. If gaps exist, write missing tests per the feature plan's test strategy (ask the user if no plan is available). Report "coverage adequate" or "N tests added" before proceeding.

## Phase 2 — Cleanup

**Skill:** `/simplify`

Run `/simplify` on the changed code. This reduces noise for the deeper review agents downstream.

## Phase 3 — Mechanical audits

Run these three, report findings, apply fixes with user approval:

1. **`convention-auditor`** — scope to changed files
2. **`i18n-checker`** — scope to changed files
3. **`template-validator`** — only if `.html` files changed

## Phase 4 — Deep review

Run these three, report findings, apply fixes with user approval:

1. **`code-review`** — holistic
2. **`security-reviewer`** — especially if controllers, guards, or auth touched
3. **`performance-reviewer`** — especially if queries, loops, or services touched

If Phase 4 produced fixes, re-run `test-writer` coverage check for any newly added code.

## Phase 5 — Validation

**Agent:** `test-runner`

Run the full test suite: `./mvnw test`. Everything must pass. If failures appear, diagnose and fix, then re-run. Do not proceed to Phase 6 with failing tests.

## Phase 6 — Docs

**Agent:** `docs-sync`

Update the project's documentation to reflect the current state of the code. Tracked docs:

- `CLAUDE.md`
- `CLAUDE-reference.md`
- `README.md`
- `rest.http`
- `OPERATIONS.md` — project-specific ops (Docker, deployment, CI) — if any of those details changed
- `docs/guides/*.md` — if a new reusable pattern emerged or existing patterns changed, update the relevant guide(s), including `docs/guides/css-guide.md` for styling/theme changes and `docs/guides/agents-and-commands.md` if the AI tooling workflow changed

Apply edits with user approval. Do not silently rewrite sections that are still accurate.

## Phase 7 — Memory review

Ask the user: *"Anything new to save to memory from this feature?"*

Your per-project memory folder lives under `~/.claude/projects/<project-slug>/memory/` (the slug is derived from the project's absolute path). Check `MEMORY.md` there for context. Save new `feedback_*.md` / `project_*.md` entries only for surprising or non-obvious lessons — not routine task details.

## Phase 8 — Commit + merge

1. Show final `git status` and `git diff --stat`.
2. **Docs land on the feature branch, not as a separate commit on main.** If uncommitted doc changes from Phase 6 remain, commit them on the feature branch before merging.
3. Switch to `main`: `git checkout main`.
4. Merge strategy:
   - **Single commit on feature branch** → fast-forward (`git merge <branch>`)
   - **Multiple commits on feature branch** → preserve history (`git merge --no-ff <branch>`)
5. Confirm with user before the merge command runs.
6. Do NOT push. The user decides when to push.

## Rules

- **User stays in the loop.** Every agent's findings must be reported before fixes apply.
- **Each agent runs once.** If re-runs are needed (because fixes changed code), explicitly note why and run again.
- **Stop on blockers.** Failing tests, unresolved security findings, or doc sync errors pause the gauntlet. Don't skip ahead.
- **Respect project memory overrides.** The user's per-project memory at `~/.claude/projects/<project-slug>/memory/` may contain `feedback_*.md` rules that override defaults in this ritual (e.g., custom merge strategies, commit conventions, style preferences). Check `MEMORY.md` there at Phase 7 and defer to those rules when they conflict with the defaults here.
