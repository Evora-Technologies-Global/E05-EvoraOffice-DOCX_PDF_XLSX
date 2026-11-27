# Standard Corporate Product Lifecycle (Quy Trình Phát Triển Sản Phẩm Chuẩn Doanh Nghiệp)

This workflow defines the mandatory corporate software development lifecycle (SDLC) from CEO problem statement to App Store / Google Play release.

Whenever a project or feature is requested, agents **MUST NEVER jump directly into coding**. They must follow this sequential lifecycle and produce complete markdown documentation files under the `docs/` directory.

---

## 🗺️ The Sequential Documentation Pipeline (8 Phases & 11 Deliverables)

```
[Phase 1: Research & Reverse Code] ──> [Phase 2: CEO Decision Gate] ──> [Phase 3: PO/BA Specs] ──> [Phase 4: UI/UX Design & Gate]
            │                                                                      │                            │
     docs/01_MARKET_RESEARCH.md                                           docs/03_PRD.md             docs/05_DESIGN_SYSTEM.md
     docs/01_COMPETITOR_REVERSE_ENGINEERING.md                            docs/04_USER_STORIES.md    + Wireframe Blueprints
                                                                                                                │
                                                                                                                ▼
[Phase 8: Store Release]          <── [Phase 7: QA & Security]   <── [Phase 6: Dev Implementation] <── [Phase 5: PM Sprint Backlog]
            │                                      │                                   │                           │
    docs/09_ASO_METADATA.md               docs/08_QA_TEST_REPORT.md              docs/07_TECH_SPEC.md        docs/06_SPRINT_BACKLOG.md
    docs/10_RELEASE_CHECKLIST.md                                                 + Native Source Code
```

---

## Detailed Phases & Mandatory Deliverables

### Phase 1: Market Research & Competitor Reverse Engineering (Nghiên Cứu Thị Trường & Reverse Code Đối Thủ)
- **Responsible Agents**: `trend-researcher`, `pricing-analyst`, `reality-checker`, **`app-reverse-engineer`** (armed with REA MCP & `reverse-engineer-anything` skill).
- **Output Files**:
  - `docs/01_MARKET_RESEARCH.md` (Market dynamics, competitor audit, monetization, financial feasibility)
  - `docs/01_COMPETITOR_REVERSE_ENGINEERING.md` (Concrete technical, UX, and architectural extraction from top competitor apps using REA)
- **Mandatory Sections in `docs/01_COMPETITOR_REVERSE_ENGINEERING.md`**:
  1. **Decompiled App Architecture & Tech Stack**: Languages (Kotlin/Java/Swift/Flutter), core libraries (Hilt/Koin, Coroutines, StateFlow), and third-party SDKs (RevenueCat, Amplitude, Adjust, Lottie).
  2. **Screen Inventory & User Navigation Topology**: Full Activity/Composable/ViewController hierarchy, onboarding funnels, monetization paywalls, notification loops.
  3. **UI/UX & Design System Extraction**: Layout hierarchies, extracted design tokens (exact hex colors, font hierarchies, spacing grids 4dp/8dp, radius), key component structures, animation/haptic patterns.
  4. **Data Models, State Machines & API Schemas**: Local database schemas (Room/SQLite tables), state machine events/reducers, network API endpoints, request/response payload schemas, offline sync mechanisms.
  5. **Feature Gap & Competitive Edge (How We Outperform Them)**: Identifies competitor flaws (intrusive ads, slow cold start > 2.5s, no home widgets) and specifies our superior implementation strategy.

---

### Phase 2: CEO Decision Gate (Cột Mốc CEO Chốt Làm)
- **Responsible Agent**: `chief-of-staff` (synthesizes 1-page executive brief)
- **Output File**: `docs/02_PROJECT_CHARTER.md`
- **Mandatory Flow**:
  1. The agent stops and presents a concise summary in **Vietnamese** to the CEO.
  2. The CEO reviews and confirms: **"Chốt làm" (Approved)** or requests pivots.
  3. Upon approval, `docs/02_PROJECT_CHARTER.md` is committed with project vision, budget ceiling, target platform, and release deadline.

---

### Phase 3: PO & BA Feature Specification (Đặc Tả Tính Năng Sản Phẩm)
- **Responsible Agents**: `product-manager`, `ba-writer`
- **Upstream Input**: PO & BA **MUST** thoroughly study `docs/01_COMPETITOR_REVERSE_ENGINEERING.md` to incorporate real-world screen states, onboarding funnels, notification cadences, and edge cases proven by market leaders.
- **Output Files**:
  - `docs/03_PRD.md` (Product Requirements Document)
  - `docs/04_USER_STORIES.md` (Detailed User Stories & Acceptance Criteria)
- **Mandatory Sections**:
  1. **MVP Scope Definition**: Strictly isolate MVP (Phase 1: 3-5 core user flows) vs Post-MVP roadmap based on competitor gap analysis.
  2. **Information Architecture & User Flow**: Screen navigation hierarchy and edge-case flowcharts matching or exceeding competitor benchmarks.
  3. **User Stories Format**:
     ```markdown
     ### US-001: [Feature Name]
     - **As a**: [Target user persona]
     - **I want to**: [Action/capability]
     - **So that**: [Value/benefit]
     - **Acceptance Criteria (Gherkin format)**:
       - Given [initial state/pre-condition]
       - When [user triggers action]
       - Then [expected result and state change]
     ```

---

### Phase 4: UI/UX Design & Prototyping Gate (Thiết Kế Trải Nghiệm & Giao Diện)
- **Responsible Agents**: `ux-architect`, `ui-designer`, `ux-researcher`, `brand-guardian`, `rapid-prototyper`
- **Upstream Input**: Design team **MUST** inherit the extracted design tokens (hex palettes, typography, spacing grid, component states) from `docs/01_COMPETITOR_REVERSE_ENGINEERING.md` to deliver pixel-grade, production-quality visual designs.
- **Output Files**:
  - `docs/05_DESIGN_SYSTEM.md` (Design System Tokens, Component Specifications, and Screen Layouts)
  - Wireframe Blueprints / Screen Layouts (using `ui-design-pencil` skill `.pen` files or structured visual layout specs)
- **Mandatory Sections**:
  1. **Visual Foundations & Design Tokens**:
     - **Color Palette**: Primary, Secondary, Background, Surface, Error, Success tokens mapped across both Light and Dark themes.
     - **Typography Scale**: Display, Title, Subtitle, Body, Caption (font families, weights, line-heights, letter-spacing).
     - **Grid & Spacing System**: Base 4dp / 8dp grid, standard margins (16dp / 24dp screen content padding).
  2. **Component Library Specifications**:
     - Atomic components: Buttons (Primary, Secondary, Ghost, Disabled), Text Fields (Default, Active, Error), Cards, Bottom Sheets, Navigation Bars, Floating Action Buttons.
     - Screen State Variations: Empty State, Loading/Shimmer Skeleton, Error/Retry State, Content State.
  3. **Screen Blueprints & Navigation Flow**:
     - Visual screen layouts for every user story defined in `docs/04_USER_STORIES.md`.
     - Screen transition hierarchies (Push, Modal bottom sheet, Cross-fade).
  4. **Accessibility (WCAG AA) & Ergonomics**:
     - Contrast ratio compliance (minimum 4.5:1 for normal text).
     - Minimum touch target size: 48dp x 48dp for all interactive elements.
     - Haptic feedback trigger points and micro-interaction animations.
- 👑 **CEO Design Review Gate**:
  - The design team synthesizes an executive visual walkthrough in **Vietnamese** for the CEO.
  - The CEO confirms design satisfaction before PM breaks down sprint tasks.

---

### Phase 5: PM Sprint Planning & Task Breakdown (Họp Sprint & Phân Rã Công Việc)
- **Responsible Agents**: `project-manager-senior`, `sprint-prioritizer`
- **Upstream Input**: PM **MUST** reference the competitor reverse engineering blueprint to ensure real-world modules (e.g. background sync, sound engines, billing validation) are scheduled, eliminating toy-scope estimates.
- **Output File**: `docs/06_SPRINT_BACKLOG.md`
- **Mandatory Sections**:
  1. **Sprint 1 (MVP Delivery)**: Prioritized backlog of technical, UI, and QA tasks linked directly to User Stories (`docs/04_USER_STORIES.md`) AND Screen Designs (`docs/05_DESIGN_SYSTEM.md`).
  2. **Definition of Done (DoD)**: Strict criteria for declaring a task complete (lint passed, tests passing, zero critical bugs, matches design tokens).
  3. **RACI Matrix**: Responsibility assignment across the agent workforce.

---

### Phase 6: Technical Architecture & Development (Kiến Trúc & Lập Trình Native)
- **Responsible Agents**: `backend-architect`, `mobile-app-builder`, `code-reviewer`
- **Upstream Input**: Architects **MUST** adopt the proven database schemas, state machines, and API payload contracts extracted from `docs/01_COMPETITOR_REVERSE_ENGINEERING.md`, strictly preventing naive mock architecture.
- **Output Files**:
  - `docs/07_TECH_SPEC.md` (Architecture, Database Schema, API Contracts, State Machine)
  - **Native Source Code**:
    - **Android**: MUST follow `.claude/rules/android-native-standards.md` (MVI, StateFlow, Channel effects, Route/Screen split).
    - **iOS**: MUST follow `.claude/rules/ios-native-standards.md` (Swift 6 Clean MVVM, Sendable, AppContainer).
- **Review Gate**: `code-reviewer` scans diffs and outputs review findings (Blocker 🔴, Suggestion 🟡, Nit 💭).

---

### Phase 7: QA Testing & Security Audit Gate (Kiểm Thử Chất Lượng & An Ninh)
- **Responsible Agents**: `tester`, `evidence-collector`, `appsec-engineer`
- **Output File**: `docs/08_QA_TEST_REPORT.md`
- **Mandatory Sections**:
  1. **Test Matrix**: Unit tests for ViewModels and Domain policies, UI interaction tests.
  2. **Edge-Case & Interruption Validation**: Network disconnect/reconnect, process death, orientation changes, permission denials.
  3. **Mobile Security Audit**: OWASP Mobile Top 10 compliance (no hardcoded secrets, encrypted local storage, secure network configuration).
  4. **Quality Gate Sign-off**: Must state **ZERO BLOCKERS** before proceeding to release.

---

### Phase 8: App Store & Google Play Release (Đóng Gói Phát Hành)
- **Responsible Agents**: `app-store-optimizer`, `mobile-release-engineer`
- **Output Files**:
  - `docs/09_ASO_METADATA.md` (App Title, Subtitle, 100-character Keywords, Promotional text, Full descriptions in Vietnamese & English, Screenshot storyline).
  - `docs/10_RELEASE_CHECKLIST.md` (Fastlane configuration script, Keystore & Provisioning profile checklist, Apple Privacy Manifest, Google Play Data Safety declaration).

---

## 🌐 Language Mandate for this Lifecycle
- **All documentation files (`docs/*.md`)**: Authored strictly in **English**.
- **CEO Checkpoints & Gate Communications**: Conducted strictly in **Vietnamese**.
