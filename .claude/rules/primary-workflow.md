# Primary Workflow

**IMPORTANT:** Analyze the skills catalog and activate the skills that are needed for the task during the process.
**IMPORTANT**: Ensure token efficiency while maintaining high quality.

#### 0. Clarification & Confirmation (MANDATORY)
- **Confirm Before Action**: Before starting ANY task, proactively review all requirements and clarify any ambiguous, unclear, or unspecified points with the user.
- **Language Protocol (Mandatory)**:
  - Communicate, ask clarifying questions, explain, and discuss with the user/CEO in **Vietnamese**.
  - Author all technical artifacts, PRDs, plans, documentation, code, and comments in **English** (except the root `README.md`).
  - Conduct all inter-agent messages, squad context exchanges, and subagent delegations in **English**.
- **Product Lifecycle Execution**: When the CEO introduces a new product idea or major initiative, **STRICTLY FOLLOW** the 8-phase flow defined in `.claude/rules/product-lifecycle-workflow.md`. **NEVER jump straight into code**. Generate each sequential deliverable under `docs/` (`01_MARKET_RESEARCH.md` & `01_COMPETITOR_REVERSE_ENGINEERING.md` ➔ `02_PROJECT_CHARTER.md` ➔ `03_PRD.md` / `04_USER_STORIES.md` ➔ `05_DESIGN_SYSTEM.md` & Wireframes ➔ `06_SPRINT_BACKLOG.md` ➔ `07_TECH_SPEC.md` ➔ Code ➔ `08_QA_TEST_REPORT.md` ➔ `09_ASO_METADATA.md` / `10_RELEASE_CHECKLIST.md`).
- Only proceed with planning and implementation after unclear points are aligned with the user.

#### 1. Code Implementation
- Before you start, delegate to `planner` agent to create a implementation plan with TODO tasks in `./plans` directory.
- When in planning phase, use multiple `researcher` agents in parallel to conduct research on different relevant technical topics and report back to `planner` agent to create implementation plan.
- Write clean, readable, and maintainable code
- Follow established architectural patterns
- Implement features according to specifications
- Handle edge cases and error scenarios
- **DO NOT** create new enhanced files, update to the existing files directly.
- **[IMPORTANT]** After creating or modifying code file, run compile command/script to check for any compile errors.

#### 2. Testing
- Delegate to `tester` agent to run tests on the **simplified code**
  - Write comprehensive unit tests
  - Ensure high code coverage
  - Test error scenarios
  - Validate performance requirements
- Tests verify the FINAL code that will be reviewed and merged
- **DO NOT** ignore failing tests just to pass the build.
- **IMPORTANT:** make sure you don't use fake data, mocks, cheats, tricks, temporary solutions, just to pass the build or github actions.
- **IMPORTANT:** Always fix failing tests follow the recommendations and delegate to `tester` agent to run tests again, only finish your session when all tests pass.

#### 3. Code Quality
- After testing passes, delegate to `code-reviewer` agent to review clean, tested code.
- Follow coding standards and conventions
- Write self-documenting code
- Add meaningful comments for complex logic
- Optimize for performance and maintainability

#### 4. Integration & Git Commit
- Always follow the plan given by `planner` agent
- Ensure seamless integration with existing code
- Follow API contracts precisely
- Maintain backward compatibility
- Document breaking changes
- Delegate to `docs-manager` agent to update docs in `./docs` directory if any.
- **Git Commit**: Always delegate or follow `.claude/agents/git-manager.md` and conventional commits (`type(scope): description`). **STRICTLY ZERO AI REFERENCES** in commit messages (no mentions of AI, Gemini, Claude, Antigravity, LLM, or AI co-authorship).

#### 5. Debugging
- When a user report bugs or issues on the server or a CI/CD pipeline, delegate to `debugger` agent to run tests and analyze the summary report.
- Read the summary report from `debugger` agent and implement the fix.
- Delegate to `tester` agent to run tests and analyze the summary report.
- If the `tester` agent reports failed tests, fix them follow the recommendations and repeat from the **Step 3**.

#### 6. Visual Explanations
When explaining complex code, protocols, or architecture:
- **When to use:** User asks "explain", "how does X work", "visualize", or topic has 3+ interacting components
- Use `/preview --explain <topic>` to generate visual explanation with ASCII + Mermaid
- Use `/preview --diagram <topic>` for architecture and data flow diagrams
- Use `/preview --slides <topic>` for step-by-step walkthroughs
- Use `/preview --ascii <topic>` for terminal-friendly output only
- **Plan context:** Visuals save to plan folder from `## Plan Context` hook injection; if none, uses `plans/visuals/`
- Auto-opens in browser via markdown-novel-viewer with Mermaid rendering
- See `development-rules.md` → "Visual Aids" section for additional guidance
