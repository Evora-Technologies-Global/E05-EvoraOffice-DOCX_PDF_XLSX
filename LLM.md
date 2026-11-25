# LLM.md — E05 code structure

> **Read this file BEFORE writing any code. Update it AFTER any structural change.**
> This is the map of the codebase: where things live, what each layer may depend on, and
> where a new file belongs. For *how* to write an MVI screen, read
> [`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md).

**Status (25/11/2026): skeleton. E05 has no product code yet.** This repository is configured for
Phase 1–8 execution under [`GEMINI.md`](GEMINI.md) and
[`.gemini/rules/product-lifecycle-workflow.md`](.gemini/rules/product-lifecycle-workflow.md). The
module graph, package layout, and conventions will be decided in Phase 6 by `docs/07_TECH_SPEC.md`,
then documented into §2–§10 from the real code. Until then, each section records only constraints
already fixed and marks the rest **TBD (Phase 6)**.

Why a skeleton and not a proposed layout: this file describes code that actually exists in this
repository. Snippets here must be copied from real E05 code, never written ahead of it.

---

## 1. Rules of use

| When | Do |
|---|---|
| Before writing code | Read §2–§10 of this file, then the relevant section of [`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md). |
| While §10 says `TBD (Phase 6)` | Do not create product source files. Throwaway feasibility spikes live only in `.local/spike/`, outside git. |
| Before adding a file | Check §10 "Where does a new file go?". Do not invent a package. |
| After adding, moving, renaming, or deleting a package, module, layer, or architectural rule | Update this file in the same commit. |
| After fixing something in §11 | Move the row from "Open" to "Fixed" with the commit reference. |
| Never | Copy a pattern from a file listed in §11 as a known deviation. |

---

## 2. Module graph

**TBD (Phase 6).**

Already fixed:
- One native Android application in **Kotlin + Jetpack Compose**, following **MVI + Clean Architecture**
  ([`GEMINI.md`](GEMINI.md)). No iOS target and no XML ViewBinding.

---

## 3. Package layout

**TBD (Phase 6).** Root `applicationId` / package namespace is locked at `com.evora.technologies.office`
(Launcher name: `Evora Office`; Play Store title: `Evora Office: DOCX, PDF, XLSX` per Charter `CD-05`,
2026-11-25). Feature module and subpackage layout remain **TBD (Phase 6)**.

---

## 4. The MVI core

**TBD (Phase 6).** The base class must follow
[`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md) §1:
`MviViewModel<S, I, E>`, `launchSafely` (always rethrowing `CancellationException`), one-shot
effects through `Channel(Channel.BUFFERED).receiveAsFlow()`, and `CollectEffects`.

---

## 5. Anatomy of a feature package

**TBD (Phase 6).** `XContract.kt`, `XViewModel.kt`, `XRoute` / `XScreen` split per
[`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md) §2–§4.

---

## 6. Dependency injection

**TBD (Phase 6).** [`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md)
§5 supports Koin or Hilt; the choice is locked in `docs/07_TECH_SPEC.md`.

---

## 7. Navigation

**TBD (Phase 6).** Navigation is always a one-shot `UiEffect`, never a boolean or destination flag
stored in `UiState`.

---

## 8. Compose stability

**TBD (Phase 6).** Domain types passed into Compose state must be immutable and declared in
`compose-stability.conf` (or `@Immutable`) so screen composables remain skippable.

---

## 9. Testing layout

**TBD (Phase 6).** Unit tests for ViewModels (Turbine + Kotlin Coroutines Test) and `:domain`
policies using readable fakes rather than fragile mocks where feasible.

---

## 10. Where does a new file go?

**TBD (Phase 6).** Until this section is populated from `docs/07_TECH_SPEC.md`, no product source
file has a home. See §1.

---

## 11. Known deviations from the standard

### Open

None. There is no product code yet.

### Fixed

None.

---

## 12. What is already right — keep it

None yet.

---

## 13. Constraints already fixed (inputs to Phase 6)

These come from project setup and CEO decisions. Phase 6 builds on them rather than reopening them.

| Constraint | Value | Source |
|---|---|---|
| Target platform | Native Android (Kotlin + Jetpack Compose) | [`GEMINI.md`](GEMINI.md) (CEO, 25/11/2026) |
| Architecture | MVI + Clean Architecture | [`GEMINI.md`](GEMINI.md); [`.gemini/rules/android-native-standards.md`](.gemini/rules/android-native-standards.md) |
| Documentation & lifecycle | 8-phase Company Bot lifecycle (`docs/01_*.md` to `docs/10_*.md`) in English; CEO communication & `README.md` in Vietnamese | [`GEMINI.md`](GEMINI.md); [`.gemini/rules/product-lifecycle-workflow.md`](.gemini/rules/product-lifecycle-workflow.md) |
| Untrusted / temp competitor data | `.local/` only, never committed; release builds must never ship competitor assets | [`GEMINI.md`](GEMINI.md) §6 |
| SDK levels, versioning & monetization | `minSdk 26`, `targetSdk 36` (Android 16), `16 KB` ELF alignment; `versionName "1.0.0"`, `versionCode 100`; Track A Open-Source (`$0` royalty, `100%` offline `$0` server); Hybrid Play Billing + UMP CMP + AdMob Mediation (**ads implementation deferred until core features are Done per `CD-07`**); Tier B UA (`$1k–$5k/mo`) | [`02_PROJECT_CHARTER.md`](docs/02_PROJECT_CHARTER.md) (`CD-01`–`CD-07`, 25/11/2026) |
| Brand, package, style & build infra | App name `Evora Office` (`Evora Office: DOCX, PDF, XLSX`), `applicationId = com.evora.technologies.office`, **Style B (Familiar Vibrant Utility)**, App logo deferred (`CD-06`), Interactive HTML Gate 2 wireframes (`CD-08`), LOK `16 KB` C++ spike via **GitHub Actions Linux Runner** (`CD-08`), Personal Play account 14d/12-tester gate ignored (`CD-09`) | [`02_PROJECT_CHARTER.md`](docs/02_PROJECT_CHARTER.md) (`CD-05`–`CD-09`, 25/11/2026) |
