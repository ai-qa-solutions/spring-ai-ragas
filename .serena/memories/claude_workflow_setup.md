# Claude Workflow Setup

`.claude/` directory in repo root contains agent infra.

## Refs (canonical coding rules)

- `.claude/refs/java-patterns.md` — Java code standards (no-nest, fail-fast, final, Lombok, comments, error handling)
- `.claude/refs/java-testing.md` — Test standards (Testcontainers, Allure, naming, AssertJ, Podman)
- `.claude/refs/python-patterns.md`, `.claude/refs/react-patterns.md` — present but not relevant for this Java project

ALWAYS read the relevant ref BEFORE writing/editing code. They are the source of truth.

## Custom slash commands (`.claude/commands/`)

- `/plan` — engineering implementation plan → specs dir
- `/plan_w_team` — team-based planning
- `/smart_build` — smart builder with semantic context routing
- `/all_tools` — list all tools

## Custom agents (`.claude/agents/`)

- `context-router.md` — semantic context routing before builder
- `meta-agent.md` — agent generator
- `team/` subdir — team agents

## Hooks (`.claude/hooks/`)

Python hooks via `uv run --no-project` on every event:
PreToolUse, PostToolUse, Notification, Stop, SubagentStop, UserPromptSubmit, PreCompact, SessionStart, SessionEnd, PermissionRequest, PostToolUseFailure, Setup
Plus `context_router.py`, `section_loader.py` for context routing, `validators/` and `utils/` subdirs.

## Settings

`.claude/settings.json` — permission allowlist + hook wiring. Allows mkdir/uv/find/mv/grep/npm/ls/cp/chmod/touch and Write/Edit by default.
