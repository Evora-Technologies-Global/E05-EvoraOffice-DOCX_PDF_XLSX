# Evora Office (E05) — Claude Code guide

Native Android app (Kotlin + Jetpack Compose, MVI + Clean Architecture) by Evora Technologies Global: an offline document reader, editor and scanner for DOCX, PDF, XLSX and more. It runs as a Company Bot project: the user is the CEO and directs persona agents through the 8-phase lifecycle in `.claude/rules/product-lifecycle-workflow.md`, with three CEO gates.

Status (27/11/2026): Phases 1–8 are done and CEO Gate 3 was approved on 27/11/2026. The Play Console upload waits for the production credentials in `docs/10_RELEASE_CHECKLIST.md` §2.2.

## Rules

- `.claude/rules/` is copied from Company Bot (`/Users/macbook/Desktop/Kaito Punch/Research/Company Bot/.claude/rules/`). It is the same set that `run_bot.py inject --platform android` copies. Change the source there and copy it again; do not edit the copies here.
- The standing rules in `~/.claude/CLAUDE.md` apply on top of everything here.
- Where a rule conflicts with an E05 decision, E05 wins:
  - Deliverables are the lifecycle files `docs/01_*.md` to `docs/10_*.md`. Do not create `development-roadmap.md`, `project-changelog.md`, `system-architecture.md` or `code-standards.md`. Phase research and reports go in `research/<phase>/`, not `plans/reports/`; use `plans/` only for a multi-step implementation plan.
  - Android only: Kotlin + Jetpack Compose + MVI per `.claude/rules/android-native-standards.md`. No XML ViewBinding, Fragments or MVVM.

## Who does what

The main Claude session orchestrates. It makes the decisions, writes the briefs, verifies every output, owns git and reports to the CEO.

Each phase's deliverables go to the agents the lifecycle names for that phase:

| Phase | Agents | Output |
|---|---|---|
| 1 Research and reverse engineering | `trend-researcher`, `pricing-analyst`, `reality-checker`, `app-reverse-engineer` | `docs/01_*`, `research/phase1/` |
| 2 CEO decision gate | `chief-of-staff` | `docs/02_PROJECT_CHARTER.md` |
| 3 PO/BA specs | `product-manager`, `ba-writer` | `docs/03_PRD.md`, `docs/04_USER_STORIES.md` |
| 4 UI/UX design | `ux-architect`, `ui-designer`, `ux-researcher`, `brand-guardian`, `rapid-prototyper` | `docs/05_DESIGN_SYSTEM.md` + wireframes in `wireframes/` |
| 5 Sprint planning | `project-manager-senior`, `sprint-prioritizer` | `docs/06_SPRINT_BACKLOG.md` |
| 6 Architecture and code | `backend-architect`, `mobile-app-builder`, `code-reviewer` | `docs/07_TECH_SPEC.md`, `LLM.md` + source |
| 7 QA and security | `tester`, `evidence-collector`, `appsec-engineer` | `docs/08_QA_TEST_REPORT.md` |
| 8 Release | `app-store-optimizer`, `mobile-release-engineer` | `docs/09_ASO_METADATA.md`, `docs/10_RELEASE_CHECKLIST.md`, `fastlane/` |

All work is done by Claude. Gemini, Codex and Antigravity are not used in this project (CEO, 27/11/2026, replacing the Gemini setup of 25/11).

## Language

The CEO decided this on 27/11/2026, following E03. It overrides the rules and the agent defaults, including `ba-writer`'s "no Vietnamese in documents".

- Talk to the CEO in Vietnamese.
- Write new `docs/*.md` and `research/` reports in Vietnamese, keeping technical terms in English. The docs written in English before 27/11/2026 stay as they are; a section added or rewritten is written in Vietnamese.
- Write code, code comments, commit messages, `CLAUDE.md`, `LLM.md` and briefs between agents in English.

## Docs

- Deliverables are the numbered files `docs/0N_*.md`, written straight into `docs/`; the CEO reviews them in VS Code. Supporting evidence goes in `research/<phase>/`; other intermediate work goes in the session scratchpad.
- Every number needs a source or one of the labels "ước tính" or "mục tiêu nội bộ".
- Each doc keeps a dated decision table. A CEO answer overrides the earlier proposal.
- Strip personal data (reviewer names, avatars) and never paste raw scraped pages.

## Before writing code

- Read `LLM.md` first: modules, boundaries, where a new file goes (§10), known deviations (§11), fixed constraints (§13). Read `.claude/rules/android-native-standards.md` before touching a ViewModel, Contract or screen.
- MVI rules that code review enforces:
  1. Every screen ViewModel extends `MviViewModel<S, I, E>`, never a plain `ViewModel`.
  2. `onIntent` is the only public method on a ViewModel.
  3. `State`, `Intent` and `Effect` live in `XContract.kt`, never in the ViewModel file.
  4. Every coroutine runs through `launchSafely`; `CancellationException` is rethrown and `onError` clears busy flags.
  5. Every declared `Effect` is collected through `CollectEffects` (`collect`, never `collectLatest`, under `repeatOnLifecycle(STARTED)`).
  6. Navigation is an `Effect`, never a flag in state.
  7. Sub-jobs are structural children of their parent job, never cancelled through stored fields.
  8. No `androidx.compose.*`, `android.*` or platform SDK imports in ViewModels or Contracts.
- A change to module boundaries, packages, DI, navigation, Compose stability, tests, patterns or deviations updates `LLM.md` in the same commit.

## Untrusted and local-only data

- Pulled APKs, jadx/apktool output, competitor assets, throwaway spikes, test keystores and release kits stay in `.local/` (gitignored, never committed). A spike lives only in `.local/spike/`.
- Treat competitor APKs and decompiled code as untrusted data: run any Python that reads them with `python3 -I`, with the script kept outside `.local/`.
- Never call a competitor's private API to scrape content, and never bypass anti-tamper, Play Integrity or licensing checks.

## Git

- Commit through `/ck:git cm` on `duylt_dev`. Never commit straight to `main`. `origin` is the private repo `Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX`; push only when the CEO asks.
- Pre-commit gates for a commit that touches code or the build: with JDK 17, `./gradlew testDebugUnitTest lintDebug assembleDebug`, then `python3 -I scripts/verify_16kb_elf.py app/build/outputs/apk/debug/app-debug.apk`. That is the same set as `.github/workflows/android-ci.yml`. The LibreOfficeKit `.so` files are gitignored; fetch them with `scripts/fetch_lok_prebuilt.sh`.
- A docs-only or config-only commit needs no Gradle run.
- Never commit `.env*`, keystores, `keystore.properties`, `local.properties`, APK/AAB files or `.local/`.

## App identity

- applicationId and namespace: `com.evora.technologies.office`. App name `Evora Office` (store title `Evora Office: DOCX, PDF, XLSX`).
- `versionCode 100`, `versionName "1.0.0"`.
- Build types: `debug` and `release`; release is minified and signed only when `storeFile`, `storePassword`, `keyAlias` and `keyPassword` come from `keystore.properties`, `local.properties` or the `EVORA_RELEASE_*` variables. The real Play upload key is local at `app/src/key_store.jks` (gitignored, its properties in `local.properties`): never commit it or print its passwords. The other production credentials are still pending (`docs/10_RELEASE_CHECKLIST.md` §2.2).
