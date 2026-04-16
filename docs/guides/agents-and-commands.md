# Agents And Commands Guide — Claude Code AI Tooling

Purpose: document the Claude Code sub-agents and slash commands that ship with this starter kit, and describe the development workflow they support.

## Overview

This project uses Claude Code sub-agents and slash commands to support feature development: writing tests, auditing conventions, reviewing code, syncing docs, and merging to main.

Both live under `.claude/`:

- `.claude/agents/` — agent definitions (one `<name>.md` per agent)
- `.claude/commands/` — slash-command definitions (one `<name>.md` per command)

Both are tracked in git via `.gitignore` exceptions so they travel with the repo.

## Agents

Each agent is a specialized sub-agent with a focused job, tool set, and prompt. Agents are invoked by name (e.g., "use the convention-auditor agent on ...") or indirectly via slash commands.

### On-demand agents

Invoked directly during coding.

| Agent | Use when |
|---|---|
| `test-writer` | Write tests for a new feature or module |
| `route-finder` | Trace a URL or route through the stack (controllers, templates, JS) |
| `code-review` | Holistic review of a file or set of files |

### Gauntlet agents

Invoked as part of `/merge-to-main`. Can also be invoked individually for mid-work spot checks.

| Agent | Role |
|---|---|
| `test-writer` | Coverage check plus gap fill (first gauntlet step) |
| `convention-auditor` | Detect violations of project conventions |
| `i18n-checker` | Find hardcoded user-facing strings |
| `template-validator` | Thymeleaf and HTMX pitfalls |
| `code-review` | Holistic review |
| `security-reviewer` | Three-layer authorization, CSRF, OWASP |
| `performance-reviewer` | N+1 queries, lazy-load bugs, hot paths |
| `test-runner` | Run the full test suite |
| `docs-sync` | Update project docs to match the code |

## Slash Commands

Shortcuts for invoking agents against a scope, or for running multi-agent workflows.

| Command | What it does |
|---|---|
| `/merge-to-main` | Full quality gauntlet: coverage → cleanup → audits → deep reviews → tests → docs → memory → commit + merge |
| `/audit <path>` | `convention-auditor` on the given path |
| `/i18n <path>` | `i18n-checker` on the given path |
| `/trace <route>` | `route-finder` on the given route name or URL |
| `/secure <path>` | `security-reviewer` on the given path |
| `/perf <path>` | `performance-reviewer` on the given path |

## The `/merge-to-main` Workflow

This is the main ritual of the development flow. Run it when:

- You and your code are in a state you are happy with
- You have reviewed as you coded and are ready for a final quality pass
- You are ready to commit docs and merge to main

The command runs agents in a sensible order:

1. **Coverage check** — `test-writer` verifies tests exist for changed code; writes missing tests
2. **Cleanup** — `/simplify` skill tidies the changed code
3. **Mechanical audits** — `convention-auditor`, `i18n-checker`, `template-validator` (if templates changed)
4. **Deep review** — `code-review`, `security-reviewer`, `performance-reviewer`
5. **Validation** — `test-runner` runs the full suite
6. **Docs** — `docs-sync` updates all tracked documentation
7. **Memory review** — you decide what is worth saving to per-project memory
8. **Commit + merge** — docs land on the feature branch; merge to main with the appropriate strategy (fast-forward for single-commit, `--no-ff` for multi-commit)

The command does NOT auto-apply fixes or auto-merge. You stay in the loop between phases.

## How The Pieces Fit Together

```text
Guides (docs/guides/*.md)
  ↓ "read these first"
Agents (.claude/agents/*.md)
  ↓ invoked by
Commands (.claude/commands/*.md)
  ↓ triggered by
You (during development)
```

Guides are the source of truth for project conventions. Agents enforce or apply the subset of those guides that can be mechanically checked. Commands are shortcuts for invoking agents against a scope.

When a convention changes: update the relevant guide. The next agent run will pick up the change — no agent rewrite needed.

## Agent Memory

Agent memory is persistent across sessions, stored outside the project:

- **User-scope agent memory**: `~/.claude/agent-memory/<agent-name>/` — learnings the agent accumulates across all projects (best for generic agents like `security-reviewer`, `performance-reviewer`)
- **Per-project memory**: `~/.claude/projects/<project-slug>/memory/` — facts specific to this project (`feedback_*.md` rules, `project_*.md` notes)

Memory is never committed to git. Each developer on the project has their own.

## Adding A New Agent

1. Create `.claude/agents/<name>.md`. Include frontmatter:

   ```markdown
   ---
   name: <name>
   description: "When to use this agent"
   tools: Glob, Grep, Read, ...
   model: opus | sonnet | haiku
   memory: user | project
   ---
   ```

2. Write the prompt. Reference relevant guides at the top (under `## Primary references`).
3. Claude Code picks the agent up automatically on next invocation.

## Adding A New Slash Command

1. Create `.claude/commands/<name>.md`. Optional frontmatter:

   ```markdown
   ---
   description: Short one-liner
   ---
   ```

2. Body is the prompt. Use `$ARGUMENTS` where user-supplied input should go.
3. Invoke as `/<name>` or `/<name> <args>`.

Slash commands can chain multiple agents (see `merge-to-main.md` as an example).

## Working Style With These Agents

- **Plan first.** When starting a feature, include a test strategy in the plan. `test-writer` can fill gaps later, but tests are better when designed alongside the feature.
- **Review as you go.** Do not save all review work for the gauntlet. Invoke agents mid-feature when you want a quick check (e.g., `/audit src/main/java/.../NewService.java` after adding a service).
- **Stay in the loop.** Agents report; you decide. Never let an agent apply fixes without your approval for anything non-trivial.
- **Tune prompts from real use.** If an agent produces too much noise or misses things, edit its `.claude/agents/<name>.md` directly. Commit changes like any code.

## Related Guides

- [conventions.md](conventions.md) — enforced by `convention-auditor`
- [testing-guide.md](testing-guide.md) — referenced by `test-writer` and `test-runner`
- [security-guide.md](security-guide.md) — referenced by `security-reviewer`
- [backend-guide.md](backend-guide.md) — referenced by multiple agents
- [frontend-guide.md](frontend-guide.md) — referenced by `template-validator` and `convention-auditor`
- [database-and-migrations.md](database-and-migrations.md) — referenced by `performance-reviewer`
- [css-guide.md](css-guide.md) — referenced by `docs-sync` when styling changes
- [architecture.md](architecture.md) — the system shape the agents assume
- [dev-guide.md](dev-guide.md) — running a Spring Boot project in this style
