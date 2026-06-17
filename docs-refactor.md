# Sweet Compose – Documentation Refactor Plan

This document proposes a complete reorganization of Sweet Compose’s documentation, guided by the direction in `docs/roadmap/long-term-direction.md`. It includes:

- A critical assessment of the existing docs.
- A target information architecture (IA) for future documentation.
- A concrete migration plan, including which files to archive into `./docs-old`.

The goal is to make the docs:

- Consistent with the current vision (SWT‑first + optional Compose/Material‑like mode).
- Easy to navigate for three audiences: users, contributors, and maintainers.
- Explicit about what is current vs. historical.

---

## 1. Current Documentation Assessment

### 1.1 Root‑level documents

- `README.md`
  - Strengths:
    - Clear, user‑oriented introduction: what Sweet is, key features, quick start, basic example.
    - Good high‑level architecture bullets and project structure overview.
    - Reasonable guidance on building, testing, and contributing.
  - Issues:
    - Mentions “Layout System” and “Testing Framework” without concrete links or document names.
    - Does not reflect the two‑mode direction from `docs/roadmap/long-term-direction.md` (SWT‑first vs. Material/Compose‑like).
    - Technology versions (Kotlin/Compose/SWT) are now codified in `CLAUDE.md`; `README.md` should either reference those or stay version‑agnostic.
    - Roadmap section partially overlaps with the more detailed roadmap docs and the long-term direction doc but is less precise.

- `CLAUDE.md`
  - Strengths:
    - Acts as de‑facto “single source of truth” for internal contributors and agents.
    - Contains a solid, up‑to‑date overview of architecture, layout system, threading, and testing practices (AutoSWT, layout assertion API).
    - Encodes important rules about testing and not editing tests lightly.
  - Issues:
    - Mixes several concerns in a single long document:
      - Developer overview & architecture.
      - Detailed testing how‑to.
      - Agent‑specific instructions and meta‑process.
    - Overlaps with `compose-swt-architecture-en.md`, `compose-swt-layout-system-en.md`, `compose-swt-implementation-plan.md`, and `PLAN.md`.
    - Some sections assume the presence of a separate `auto-swt` repository and could be referenced instead of fully embedded.
    - Hard to skim; no clear separation between “must read to contribute” vs. “deep dive”.

- `CLAUDE.local.md`
  - Currently empty.
  - Likely intended for local, developer‑specific overrides. It should be clearly treated as non‑canonical and excluded from public documentation.

- `PLAN.md`
  - Strengths:
    - Concrete, detailed technical plan for hardening and polishing the implementation.
    - Good explanation of current issues and desired directions (alignment, background, window APIs, scroll, etc.).
  - Issues:
    - Written as a point‑in‑time technical plan; some items are now completed or partially implemented.
    - Overlaps with architecture details that already appear in `CLAUDE.md` and other docs.
    - Lacks clear status tracking for each item (only inline notes).
    - Directionally aligned but has been superseded at the strategic level by `docs/roadmap/long-term-direction.md`.

- Long-term evolution plan (`docs/roadmap/long-term-direction.md`)
  - Strengths:
    - Explicitly flagged as the long‑term evolution plan.
    - Clearly articulates the “two modes” concept (SWT‑first vs. Material/Compose‑like).
    - Describes phased plan (style profiles, drawing backends, Skia/Skiko, Compose‑first mode).
  - Issues:
    - Contains product/architecture direction that should be surfaced in a more discoverable “Architecture / Direction” doc (which we now treat as part of `docs/roadmap/long-term-direction.md`).

- `compose-swt-architecture-en.md`
  - Strengths:
    - Thorough architecture description of a “Compose SWT” implementation: nodes, applier, composition binding, interop patterns.
    - Good educational content about a widget‑based Compose architecture.
  - Issues:
    - Uses older naming and module structure (`compose-swt-core`, `compose-swt-foundation`, etc.) that do not exactly match the current `sweet/` module organization.
    - Overlaps heavily with the architecture information already in `CLAUDE.md` and `compose-swt-layout-system-en.md`.
    - Reads more like an early design document than the current, canonical architecture spec.

- `compose-swt-layout-system-en.md`
  - Strengths:
    - Extensive, clear explanation of the layout system design (constraints, measurables, placeables, layout nodes, modifiers, root node).
    - Contains concrete examples and is very useful as a deep dive for maintainers.
  - Issues:
    - Uses older naming around `SwtNode`/`RootSwtNode` that partially diverge from the current `SweetLayout`‑based implementation.
    - Conceptually overlaps with layout descriptions in `CLAUDE.md` and `sweet` source itself.
    - Needs either alignment with the current `SweetLayout` architecture or clear labelling as historical.

- `compose-swt-implementation-plan.md`
  - Strengths:
    - Focused on implementation hardening, especially layout, applier, scrolling, modifiers, and resource management.
    - Aligns with objectives in `PLAN.md`.
  - Issues:
    - Another plan‑like document; adds to confusion between multiple plan files and this refactor file (now resolved by consolidating into `docs/roadmap/`).

### 1.2 `docs/` tree

- `docs/layout/compose/*.md`
  - Content:
    - Deep dives into Jetpack Compose’s layout system, parent/child choreography, constraints, and performance.
  - Strengths:
    - Strong conceptual explanations; useful as background for understanding Sweet’s layout goals.
  - Issues:
    - Some files start with conversational artifacts (“Of course. Here is...”), clearly produced by a chat assistant and not edited into neutral documentation style.
    - They describe stock Compose, not Sweet; they are best treated as reference material, not primary project docs.
    - The relationship between these files and `compose-swt-layout-system-en.md` is not explicit.

- `docs/layout/swt/*.md`
  - Content:
    - Detailed explanation of SWT layout mechanisms, passes, `LayoutData`, and efficiency.
  - Strengths:
    - Good conceptual background for contributors coming from Compose/Android.
  - Issues:
    - Same conversational intro issues as compose layout docs.
    - Not clearly linked from anywhere; easy to miss.
    - Some content duplicates ideas already explained in the main architecture docs (e.g., two‑pass layout, dirty flag).

### 1.3 Other meta/docs files

- `AGENTS.md`
  - Local meta‑instructions for agents, correctly pointing to `CLAUDE.md` as canonical.
  - Should remain as is, but future reorganization should ensure `CLAUDE.md` links back into the new documentation structure.

---

## 2. Documentation Goals and Audiences

The refactor should explicitly support three main audiences:

1. **End users (SWT developers using Sweet)**  
   - Need: quick overview, getting started, API examples, known limitations, and a conceptual understanding of Sweet’s behavior.
   - Entry point: `README.md` plus a concise “User Guide” in `docs/`.

2. **Contributors (engineers working on Sweet itself)**  
   - Need: project architecture, layout system, threading model, testing strategy, coding conventions, and contribution workflow.
   - Entry point: `docs/` (architecture + contribution guide) plus a slimmed‑down `CLAUDE.md` for critical rules.

3. **Maintainers / core contributors**  
   - Need: long‑term product direction, open design questions, historical notes, and in‑depth implementation plans.
   - Entry point: `docs/roadmap/long-term-direction.md` plus architectural docs under `docs/architecture/`.

---

## 3. Target Documentation Information Architecture

### 3.1 High‑level structure

Target structure (not yet implemented):

```
README.md                       # Short, user-focused overview and links
CLAUDE.md                       # Canonical agent + contributor rules, slimmed
docs/roadmap/long-term-direction.md   # Long-term evolution / direction
docs/
  index.md                      # Documentation entrypoint and table of contents
  overview.md                   # High-level concepts and usage modes
  architecture/
    runtime-and-layout.md       # Sweet architecture + layout system
    threading-and-coroutines.md # Threading model and coroutines
    interop.md                  # SWT interop patterns
  development/
    contributing.md             # How to contribute, coding style, PR expectations
    testing.md                  # AutoSWT, layout assertion API, testing rules
  guides/
    getting-started-swt.md      # Using Sweet in an SWT app (SWT-first mode)
    material-like-mode.md       # Future Compose-first / Material-like mode (experimental)
  background/
    jetpack-compose-layout.md   # Jetpack Compose layout deep dive (from docs/layout/compose)
    swt-layout.md               # SWT layout deep dive (from docs/layout/swt)
  roadmap/
    status-and-next-steps.md    # Current state + short, prioritized next tasks
    hardening-plan.md           # Extracted, updated content from PLAN.md + impl plan
docs-old/
  ...                           # Archived superseded docs (see section 4)
```

Key ideas:

- `README.md` becomes a short, opinionated front door that points into `docs/`.
- `CLAUDE.md` focuses on rules and expectations for contributors and agents, and de‑duplicates content by linking to `docs/` for deep dives.
- `docs/index.md` (or `docs/README.md`) acts as a table of contents.
- Architecture content is unified under `docs/architecture/`, incorporating and updating the best parts of `compose-swt-architecture-en.md`, `compose-swt-layout-system-en.md`, and `CLAUDE.md`.
- Background educational material (Jetpack Compose layout, SWT layout) moves under `docs/background/` and is clearly marked as “background, not project‑specific behavior”.

### 3.2 Roles of existing root files in the new IA

- `README.md`
  - Stays at the root.
  - Emphasizes:
    - What Sweet is and why it exists.
    - The two execution modes described in the long-term direction doc (SWT‑first and Material‑like).
    - A minimal example and links to:
      - `docs/overview.md`
      - `docs/guides/getting-started-swt.md`
      - `docs/development/contributing.md`

- `CLAUDE.md`
  - Remains at the root as required by `AGENTS.md`.
  - Refocused to:
    - Summarize the most important non‑negotiable rules (testing, not editing tests, using Gradle, etc.).
    - Describe how agents and contributors should navigate the new docs tree.
    - Point to:
      - `docs/architecture/*` for implementation details.
      - `docs/development/testing.md` for AutoSWT patterns.
  - Remove or shorten duplicated architecture snippets that are fully captured elsewhere.

- `docs/roadmap/long-term-direction.md`
  - Keeps the role of main strategy and long‑term vision document.

---

## 4. Archiving Strategy (`./docs-old`)

To reduce confusion while preserving history, superseded or pre‑Sweet documents should be moved to `./docs-old`. They should not be edited except to add a short header explaining why they were archived.

The older “compose-swt-*.md” design docs and the original `PLAN.md` are now considered historical and their relevant content has been folded into:

- `docs/architecture/runtime-and-layout.md`
- `docs/architecture/interop.md`
- `docs/roadmap/status-and-next-steps.md`
- `docs/roadmap/hardening-plan.md`

### 4.2 Historical vs. background material

The following should *not* be moved into `docs-old/`, but reorganized:

- `docs/layout/compose/*.md` and `docs/layout/swt/*.md`
  - They are background/reference material, not project‑specific history.
  - Recommendation: move or copy them into `docs/background/` with neutral, edited introductions, and link them from `docs/index.md` under a “Background Reading” section.

### 4.3 `docs-old` conventions

When a document is moved to `docs-old/`:

- Prepend a short header explaining:
  - The time frame / context (e.g., “Early Compose SWT architecture sketch”).
  - What has replaced it (e.g., “See docs/architecture/runtime-and-layout.md”).
- Do not update the technical content itself except to clearly mark it as historical.
- Do not link `docs-old/` from user‑facing docs; it is intended for maintainers and archeology only.

---

## 5. Concrete Migration Steps

This section enumerates practical steps to perform the refactor. They can be executed incrementally.

### 5.1 Establish the new docs entrypoint

1. Create `docs/index.md`:
   - Short introduction to Sweet.
   - Clear table of contents pointing to:
     - `overview.md`
     - `architecture/*`
     - `development/*`
     - `guides/*`
     - `background/*`
2. Optionally, make `docs/README.md` a symlink or duplicate of `docs/index.md` depending on repository conventions.

### 5.2 Extract and unify architecture documentation

1. Create `docs/architecture/runtime-and-layout.md`:
   - Describe:
     - Core architecture: node tree, composition, SweetLayout, delegates, scroll containers.
     - Layout behavior: constraints, measurement, placement, modifiers.
     - Interop: SWT widgets as leaf nodes, embedding Compose in SWT and vice‑versa.
   - Source material:
     - `CLAUDE.md` architecture sections.
     - Earlier “compose-swt-*” architecture/layout docs.
     - Earlier implementation hardening plans.
2. Create `docs/architecture/threading-and-coroutines.md`:
   - Extract the threading model and coroutine dispatcher information from `CLAUDE.md` and related notes.
3. Create `docs/architecture/interop.md`:
   - Move and update SWT interop examples from `compose-swt-architecture-en.md` and code samples.

### 5.3 Clarify development and contribution workflow

1. Create `docs/development/contributing.md`:
   - High‑level contribution guidelines.
   - Coding style summary.
   - References to testing requirements.
2. Create `docs/development/testing.md`:
   - Extract AutoSWT usage patterns, layout assertion API, and best practices from `CLAUDE.md`.
   - Keep the “never change tests lightly” rule but link back to `CLAUDE.md` for the canonical wording.
3. Update `CLAUDE.md`:
   - Keep:
     - Project overview.
     - Non‑negotiable rules (Gradle only, tests, test editing rules).
     - Pointers into `docs/architecture/*` and `docs/development/*`.
   - Remove detailed examples that are now duplicated elsewhere, or replace them with “See docs/...”.

### 5.4 User‑facing guides and overview

1. Create `docs/overview.md`:
   - Explain:
     - The SWT‑first philosophy.
     - The future Material/Compose‑like mode as described in `docs/roadmap/long-term-direction.md`.
     - How Sweet fits into existing SWT applications and migration paths.
2. Create `docs/guides/getting-started-swt.md`:
   - Step‑by‑step instructions for integrating Sweet into an SWT app.
   - Include a simple example similar to the one in `README.md`, but with more detail on SWT setup.
3. Optionally: create `docs/guides/material-like-mode.md`:
   - Summarize the planned `MaterialLikeProfile` from `docs/roadmap/long-term-direction.md`.
   - Clearly mark it as experimental/future.

4. Update `README.md` to:
   - Briefly mention the two usage modes.
   - Link to:
     - `docs/overview.md`
     - `docs/guides/getting-started-swt.md`
     - `docs/development/contributing.md`

### 5.5 Roadmap and plans

1. Under `docs/roadmap/`, create:
   - `status-and-next-steps.md`:
     - Summarize the **current state** of the project in a compact way (based on the previous technical plan’s “Current State” section and the long-term direction doc).
     - List a short, prioritized set of **next tasks / milestones**, each linking to more detailed items in `hardening-plan.md` or to relevant sections of `long-term-direction.md`.
     - Intended as the primary “what is the state now and what comes next” entrypoint.
   - `hardening-plan.md`:
     - Consolidate still‑open implementation items from the older plans, organized by priority/area.

### 5.6 Background material cleanup

1. Move or copy:
   - `docs/layout/compose/*.md` → `docs/background/jetpack-compose-layout.md` (possibly merged into one or two more polished docs).
   - `docs/layout/swt/*.md` → `docs/background/swt-layout.md`.
2. Edit each file to:
   - Remove conversational openings (“Of course. Here is...”).
   - Add a short header clarifying that these describe standard Jetpack Compose or SWT and are background material.
3. Leave stubs or README files under `docs/layout/` explaining that the content has moved to `docs/background/`.

### 5.7 Final archival pass

Once architecture, development, and roadmap docs are in place and wired from `README.md` and `CLAUDE.md`:

1. Ensure all historical design/plan docs have had their relevant content migrated into `docs/`.
2. Remove the old files from the repository so that `docs/` becomes the single canonical source.

---

## 6. Open Questions / Decisions to Make

The refactor above leaves some choices to maintainers:
- How aggressively to trim `CLAUDE.md`:
  - Option A: keep it as a compact “agent and contributor rules” doc.
  - Option B: keep a slightly more detailed architecture summary, but avoid duplicating full deep dives.
- Whether to keep both the Compose and SWT background docs in full, or to condense them into shorter references (they have now been summarized into `docs/background/*.md`).

These can be decided incrementally; the key is to make the “front door” for each audience clear and to separate current, canonical docs from historical design notes.
