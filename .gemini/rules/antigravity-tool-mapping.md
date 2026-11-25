---
trigger: always_on
glob:
description: Translate Claude Code tool names and conventions still present in the ported skills and agents into Antigravity equivalents.
---

# Claude Code → Antigravity translation

The skills and agents in `.gemini/` (and linked via `.gemini/`) were ported from a Claude Code kit. Paths, skill names and the 1:1 tool names were rewritten, but some instructions still name Claude Code tools or mechanisms. When an instruction names one of these, do the Antigravity equivalent. Never stop or skip a step because a named tool does not exist here.

| Instruction says | Do this in Antigravity |
|---|---|
| `Task` tool, `Task(subagent_type: X)`, "spawn / launch the X agent" | `invoke_subagent` with the custom agent `X` (`.gemini/agents/X.md`). `Explore` means a read-only search: do it yourself or use a subagent told not to edit. |
| `TaskCreate`, `TaskUpdate`, `TaskList`, `TaskGet`, `TodoWrite`, "task-list items" | Keep the checklist in your task list artifact: add items, mark them in progress and done. Plan files under `plans/` stay the source of truth. `manage_task` is **not** this — it manages background commands. |
| `Skill` tool, "activate the X skill", `/ck-x` | Open and follow `.gemini/skills/<dir>/SKILL.md` for skill `X` (or run the slash command `/X`). Claude's `ck:x` names are `ck-x` here: `/ck-plan`, `/ck-cook`, `ck-xlsx`. |
| `Read`, `Glob`, `Grep`, `LS` | `view_file` and the file / text search tools. |
| `Write`, `Edit`, `MultiEdit`, `NotebookEdit` | `write_to_file`, `replace_file_content`, `multi_replace_file_content`, `notebook_edit`. |
| `Bash`, `BashOutput`, `KillBash` | `run_command`, `command_status`, `manage_task`. |
| `ask_question` | Ask the user with `ask_question`. If it is unavailable (for example as a subagent), ask in your reply and stop until answered. |
| `send_message` | Message your parent or a running subagent with `send_message`. Subagents report when done; do not poll. |
| `search_web`, `read_url_content` | Same names here. |
| `TeamCreate`, teammates, `shutdown_request` | Follow the mapping at the top of the `ck-team` skill. |
| `ListMcpResourcesTool`, `ReadMcpResourceTool` | `list_resources`, `read_resource`. |
| `## Naming`, `## Plan Context`, `CK_*` values "injected by hooks" | No hook injects these. Plans go to `plans/{YYMMDD-HHmm}-{slug}/`, reports to `plans/reports/` (format in `.gemini/.ck.json`). The active plan is the plan path the user or parent agent gave you; ask if there is none. |
| `model: opus / sonnet / haiku`, "ultrathink" | Ignore the model name; reason as deeply as the task needs. |
| `.claude/…`, `~/.claude/…`, `CLAUDE.md` in any leftover text | `.gemini/…`, `~/.gemini/config/…`, `GEMINI.md`. |
