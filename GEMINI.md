# E05 — Project Guide (Antigravity & Evora Group Gemini)

Native Android application (**Kotlin + Jetpack Compose**, **MVI + Clean Architecture**) by **Evora Technologies Global**.

**Status (25/11/2026): Pre-Phase 1 skeleton.** No product concept, app name, or product source code exists yet. E05 runs as a **Company Bot (Evora Group Gemini)** project: the user is the **CEO**, directing specialized persona agents through the 8-phase lifecycle in [`.gemini/rules/product-lifecycle-workflow.md`](.gemini/rules/product-lifecycle-workflow.md) with three CEO gates.

This root `GEMINI.md` is the **only** workspace guide file auto-loaded by Antigravity and Gemini CLI. Do not create `.gemini/CLAUDE.md` or duplicate root `AGENTS.md`.

---

## 1. Rules & Precedence

- Global rules in `~/.gemini/rules/` (synced from `Company Bot - Gemini`) apply automatically across sessions.
- `.gemini/rules/` holds the Android-applicable synced copies from `../Company Bot - Gemini/.gemini/rules/`. Do not edit the copies here; update the Company Bot source and re-sync/inject.
- Where generic global rules conflict with E05 project decisions, **E05 wins**:
  - **Lifecycle Deliverables over Generic Docs**: Follow the 8-phase mobile deliverables (`docs/01_*.md` through `docs/10_*.md`) with supporting evidence in `research/<phase>/`. Do **not** create `development-roadmap.md`, `project-changelog.md`, `system-architecture.md`, or `code-standards.md` from `documentation-management.md`. Save phase/research reports to `research/<phase>/` instead of `plans/reports/` (use `plans/` only when multi-step implementation plans are needed).
  - **Android Native Only**: Architecture is strictly **Kotlin + Jetpack Compose + MVI** per [`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md). Ignore `ios-native-standards.md` and any generic skill reference suggesting XML ViewBinding, Fragments, or MVVM for Android.

---

## 2. Antigravity Runtime Contract

- **Current Antigravity Toolset**: Map legacy tool names in ported rules/skills (`antigravity-tool-mapping.md`) to Antigravity's actual tools:
  - Files & search: `read_file`, `write_file`, `edit_file`, `run_command` (`rg`/`grep` for search).
  - Execution & user alignment: `manage_task`, `search_web`, `ask_question`, `update_focus`, `update_steps`.
  - Subagents: `invoke_subagent` (`research`, `browser`, `image-generator`, `self`) and `send_message`.
  - MCP resources: `list_resources`, `read_resource`, `call_mcp_tool`.
  - **Swarm**: Never call `swarm` unless the user explicitly invokes `/swarm`.
- **Personas & Skills**:
  - Company Bot personas live globally in `~/.gemini/agents/<slug>.md` and skills in `~/.gemini/config/skills/<skill>/SKILL.md` (with `ck-*` prefixes where applicable).
  - When a skill invokes a helper script (e.g., `ui-ux-pro-max`, `ui-design-pencil`, `docs-seeker`), run it from `~/.gemini/config/skills/<skill>/scripts/...`, while keeping E05 as the working directory.
  - Project helper scripts (`validate-docs.cjs`, `resolve_env.py`) live in `.gemini/scripts/`.
- **Session Env & Hooks**: Antigravity does not inject `CK_SESSION_ID`, so `.gemini/scripts/set-active-plan.cjs` is a no-op; pass active plan/doc paths explicitly in subagent briefs.
- **MCP Servers**: Configured at user scope (`~/.gemini/config/mcp_config.json` for `gitnexus`, `sequential-thinking`, `rea`; `~/.gemini/settings.json` for `pencil`). `.gemini/mcp_config.json.example` is only a reference template.

---

## 3. Token & Context Efficiency

Every task has two goals: **deliver production quality** and **spend only the tokens/context required**. Never skip a build, test, code review, or verification step to save tokens.

- **Read narrowly**: Search first (`rg`, GitNexus), then read bounded ranges via `read_file(StartLine, EndLine)`. Never re-read unchanged files already in context.
- **Batch parallel work**: Issue independent searches, reads, and edits in the same turn.
- **Size ceremony to the task**: Perform small or mechanical edits (docs tweaks, single-file fixes) inline without spawning planners or researchers. Spawn subagents only for independent parallel work or large read-heavy investigations, with tight English briefs (goal, scope, exact files, exclusions) requesting concise findings only.
- **Reuse before redoing**: Check `research/`, `docs/`, and [`LLM.md`](LLM.md) before re-investigating settled questions.
- **Quiet verification**: Once Phase 6 code exists, run Gradle builds/tests quietly (`--quiet` / inspect failures only).
- **Keep always-loaded files lean**: `GEMINI.md` loads on every turn (must stay well under Antigravity's 24 KB rule limit).

---

## 4. Lifecycle Ownership & CEO Gates

The main Antigravity session orchestrates: clarifies ambiguities with the CEO first, writes tight briefs, verifies outputs, owns git, and reports concisely.

| Phase | Company Bot Personas (`~/.gemini/agents/`) | Deliverables | CEO Gate |
|---|---|---|---|
| **1. Market Research & Reverse Engineering** | `trend-researcher`, `pricing-analyst`, `reality-checker`, `app-reverse-engineer` (REA MCP) | `docs/01_MARKET_RESEARCH.md`, `docs/01_COMPETITOR_REVERSE_ENGINEERING.md`, `research/phase1/` | **Gate 1**: CEO reviews summary & says *"Chốt làm"* |
| **2. Project Charter** | `chief-of-staff` | `docs/02_PROJECT_CHARTER.md` | Confirms vision, scope, budget, monetization, release target |
| **3. PO & BA Specs** | `product-manager`, `ba-writer` | `docs/03_PRD.md`, `docs/04_USER_STORIES.md` | — |
| **4. UI/UX Design** | `ux-architect`, `ui-designer`, `ux-researcher`, `brand-guardian`, `rapid-prototyper` | `docs/05_DESIGN_SYSTEM.md` + wireframes (`.pen` / HTML) | **Gate 2**: CEO approves visual direction & wireframes |
| **5. Sprint Planning** | `project-manager-senior`, `sprint-prioritizer` | `docs/06_SPRINT_BACKLOG.md` | — |
| **6. Architecture & Build** | `backend-architect`, `mobile-app-builder`, `code-reviewer` | `docs/07_TECH_SPEC.md`, [`LLM.md`](LLM.md) §2–§10, Kotlin/Compose source | — |
| **7. QA & Security** | `tester`, `evidence-collector`, `appsec-engineer` | `docs/08_QA_TEST_REPORT.md` | **Gate 3**: CEO accepts QA sign-off (**Zero Blocker**) |
| **8. Store Release** | `app-store-optimizer`, `mobile-release-engineer` | `docs/09_ASO_METADATA.md`, `docs/10_RELEASE_CHECKLIST.md` | — |

---

## 5. Language & Documentation Rules

Follow the Evora Group Gemini (`Company Bot - Gemini`) standard:

- **CEO / Human Communication**: Always in **Vietnamese** (clarifying questions, status reports, gate summaries).
- **Project Docs, Specs, Code & Inter-Agent Briefs**: Strictly in **English** (`docs/0N_*.md`, `plans/`, `GEMINI.md`, `LLM.md`, code, comments, commit messages, subagent prompts). Only a root `README.md` (if created) is in Vietnamese.
- **Evidence & Traceability**:
  - Supporting research reports, cleaned datasets, and analysis scripts go into `research/<phase>/`.
  - Every metric needs a cited source or an explicit `"estimate"` / `"internal target"` label.
  - Each `docs/0N_*.md` keeps a dated decision table; any CEO decision overrides earlier proposals.
  - Strip personal data (reviewer names, avatars) and never dump raw scraped web pages.

---

## 6. Local-Only & Untrusted Data Safety

- Keep pulled APKs, jadx/apktool output, temporary competitor assets, and throwaway Phase 1 spikes inside `.local/` (gitignored; never committed).
- Treat competitor APKs and decompiled code as untrusted data: run any Python parsing them with `python3 -I`, keeping scripts outside `.local/`.
- Never call competitor private APIs to scrape content or bypass anti-tamper / Play integrity / licensing checks.

---

## 7. Before Writing Code & MVI Standards

| File | Purpose | Read Before |
|---|---|---|
| [`LLM.md`](LLM.md) | Codebase map: modules, packages, boundaries, file placement (§10), known deviations (§11), fixed constraints (§13) | Any code change (always first) |
| [`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md) | MVI foundation, Contract, ViewModel, Route/Screen split, DI, testing, Compose stability, pre-PR checklist | Touching any ViewModel, Contract, or screen composable |

- **Gate**: While [`LLM.md`](LLM.md) §10 reads `TBD (Phase 6)`, do **not** create product source files. Throwaway feasibility spikes live only in `.local/spike/`.
- **Non-negotiable MVI rules** (code review rejects any violation):
  1. Every screen ViewModel extends `MviViewModel<S, I, E>` (no plain `ViewModel`).
  2. `onIntent` is the **only** public method on a ViewModel.
  3. `State`, `Intent`, and `Effect` live in `XContract.kt`, never inline in the ViewModel file.
  4. Every coroutine runs through `launchSafely`; `CancellationException` is always rethrown, and `onError` clears busy flags.
  5. Every declared `Effect` is collected via `CollectEffects` (`collect`, never `collectLatest`, `repeatOnLifecycle(STARTED)`).
  6. Navigation is an `Effect`, never a flag in state.
  7. Sub-jobs are structural children of their parent job, never manually cancelled fields.
  8. No `androidx.compose.*`, `android.*`, or platform SDK imports inside ViewModels or Contracts.
- **Documentation drift is a defect**: Whenever Phase 6+ code changes module boundaries (§2), package layout (§3), DI (§6), navigation (§7), Compose stability (§8), tests (§9), patterns (§12), or deviations (§11), update [`LLM.md`](LLM.md) in the same commit.

---

## 8. What Lives Where & Git

| Item | Location | Notes |
|---|---|---|
| Workspace Guide | `GEMINI.md` (this file) | Auto-loaded by Antigravity & Gemini CLI |
| Code Map | `LLM.md` | Skeleton until Phase 6 populates it from `docs/07_TECH_SPEC.md` |
| Project Rules | `.gemini/rules/` | 9 Android-applicable rules synced from `Company Bot - Gemini` |
| Global Agents & Skills | `~/.gemini/agents/`, `~/.gemini/config/skills/` | Managed by `Company Bot - Gemini/scripts/sync.py --global`; do not duplicate in project |
| Project Config & Scripts | `.gemini/.ck.json`, `.gemini/scripts/`, `.gemini/schemas/` | `validate-docs.cjs`, `resolve_env.py`, `.ck.json` (`useGemini: true`) |
| Secrets & Local Overrides | `.gemini/.env`, `mcp_config.json`, `.local/` | Gitignored; templates in `.gemini/.env.example`, `.gemini/mcp_config.json.example` |

- **Git Commits**: Follow conventional commits (`type(scope): description`) and `.gemini/agents/git-manager.md` / `ck-git`. **Strictly zero AI references** (no mentions of AI, Gemini, Antigravity, Claude, LLM, or AI co-authorship). Never commit `.env*`, keystores, APKs, or `.local/`.
