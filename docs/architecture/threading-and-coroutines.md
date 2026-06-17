# Sweet Architecture – Threading and Coroutines

This document summarizes Sweet’s threading model and coroutine usage. It complements `CLAUDE.md`.

## SWT UI thread as the single source of truth

- All SWT widget access must happen on the SWT UI thread.
- Sweet’s `Dispatchers.Main` is provided by the `kotlinx-coroutines-swt` module:
  - `SWTMainCoroutineDispatcher`
  - `SWTMainDispatcherFactory`
  - Immediate dispatcher variant for inline UI work.

## Compose Runtime integration

- The main composition runs on `Dispatchers.Main` + a SWT-backed frame clock.
- State changes schedule recomposition via the standard Compose machinery, but recomposition ultimately runs on the SWT UI thread.

## Testing implications

- AutoSWT tests rely on the SWT main thread semantics:
  - No `waitForIdle()` is necessary for Sweet recomposition.
  - Helpers like `find`, `findAll`, `typeText`, `doSelect` already marshal to the UI thread.
- `runOnSWT` is required only when directly accessing SWT widget properties, not when using AutoSWT helpers.

For rules and examples, see `docs/development/testing.md` and the testing section in `CLAUDE.md`.

