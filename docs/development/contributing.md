# Contributing to Sweet Compose

This document provides contributor-oriented guidance. It complements `README.md` and `CLAUDE.md`.

## Expectations

- Use Kotlin style consistent with the existing codebase.
- Prefer small, focused changes.
- Always add or update tests when changing behavior.
- Follow the Gradle-based build and test workflow:
  - Do not call `java`, `javac` or `kotlinc` directly.

## Sample Modules and Compatibility

Sweet uses sample modules as a live compatibility surface:

- Shared composables and models live in `:sample-sweet`.
- SWT / AutoSWT tests exercising those samples live in `:sample-sweet`.
- A placeholder `:sample-jetbrains` module exists for future JetBrains Compose Desktop runners.

When adding or modifying samples:

- Put reusable UI and models in `sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample`.
- Add or update AutoSWT tests in `sample-sweet/src/test/kotlin`.
- Keep `sample-sweet` limited to APIs that are intended to be supported by Sweet.

## Workflow (summary)

1. Fork the repository.
2. Create a feature branch.
3. Implement changes and tests.
4. Run tests via Gradle.
5. Open a pull request describing:
   - What changed.
   - How it was tested.
   - Any relevant updates to docs.

For detailed agent and testing rules, see `CLAUDE.md` and `docs/development/testing.md`.
