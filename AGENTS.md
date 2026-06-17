# Repository Guidelines

This repository uses a custom agent workflow. For accurate, project‑specific instructions, read `CLAUDE.md` end‑to‑end before making changes. It is the single source of truth for how agents should reason, code, test, and propose PRs here.

## Start Here
- Open `CLAUDE.md` at the repo root.
- Follow the onboarding and execution guidance in order.
- Apply its conventions when naming branches, commits, tests, and APIs.

## What CLAUDE.md Covers
- Architecture and module boundaries: `sweet/` (core), `swttest/` (SWT helpers), `sample/` (examples).
- How to run, test, and debug with Gradle.
- Coding style, review expectations, and documentation standards.
- Decision records and constraints unique to this codebase.

## Minimum Expectations
- Do not submit PRs that contradict `CLAUDE.md`; align or propose changes to it first.
- When in doubt, link to the relevant `CLAUDE.md` section in your PR description.

## Quick References
- Build: `./gradlew build`
- Test: `./gradlew test`
- Modules: `sweet/`, `swttest/`, `sample/`

Also see `README.md` for a general overview and `docs/roadmap/status-and-next-steps.md` for roadmap context. If `CLAUDE.md` seems outdated, open an issue proposing updates before proceeding.
