# Test Determinism — Root Cause Reference

- **Resolution:** per-Display LayoutCoordinator, thread-check removal, and `@AfterEach` shell disposal eliminated the state-leakage causes of run-to-run flakiness. Environment sensitivity (fonts/DPI/theme) remains bounded by font-metric tolerances and documented.
- **This document:** condensed root-cause analysis. The full investigation log (evidence catalog, state-leakage tables, proposed solution steps) is in git history.

## Why this matters

The layout/UI test suite is **non-deterministic**: the set of failing tests varies between machines (fonts/DPI/theme) and between runs on the same machine (low-rate flakiness), with no source-code change. You cannot distinguish a real layout regression from noise — a red run doesn't mean a bug, and a green run doesn't prove correctness.

## How to run

```bash
xvfb-run -a ./gradlew cleanTest test --continue
```

Always validate at the root, all modules. A display is mandatory (headless → every UI test fails with `UninitializedPropertyAccessException`).

## Root cause: two independent mechanisms

### 1. Environment sensitivity (deterministic per machine)

The measurement chain depends on SWT native sizing, which varies with host configuration.
The propagation chain:

```
system font / DPI / theme
  → control.computeSize() returns different pixels
  → measured sizes differ
  → layout delegates compute positions with different inputs
  → pixel-level assertions hit hardcoded tolerance limits
  → same test passes on one machine, fails on another
```

Key code locations:

| Layer | Location | What depends on environment |
|-------|----------|-----------------------------|
| Widget sizing | `SweetLayout.kt` — `measure()` | `control.computeSize(SWT.DEFAULT, SWT.DEFAULT)` → font-dependent pixels |
| Alignment blocks | `SweetLayout.kt` — `computeAlignmentSize()` | `alignmentSize = min(measuredSize, naturalContentSize + padW)` — font-dependent |
| Baseline offsets | `LayoutDelegates.kt` — `computeBaselineOffset()` | `gc.fontMetrics.ascent` — font-dependent, cached per-control |
| dp→px conversion | `internal.kt` — `Density.fromDisplay()` | DPI-dependent |

### 2. Run-to-run flakiness (same machine)

All `:sweet` tests share one JVM fork. State-leakage candidates that survive across tests:

- **`LayoutCoordinator`** (`object` singleton): if a test crashes mid-frame, `inFrame` stays `true` → subsequent `requestLayout()` calls batch instead of executing immediately.
- **`SweetLayout` companion**: `measureCountEnabled` / `measureCounts` — one test enabling counting changes behavior for all subsequent tests.
- **Per-Display caches**: color/font/density caches survive as long as the Display instance.
- **`SWTSnapshotManager`**: if `ComposeManager.dispose()` isn't called, the observer leaks.

The canary: `box_alignment_variants` creates 5 shells sequentially in a loop, amplifying timing jitter under full-suite load (X server pressure, GC).

## Definition of done

The full root suite produces the **same** result on repeated runs and across supported environments (Linux/xvfb at minimum), with any remaining environment differences explicitly bounded by font-metric-derived tolerances and documented. A red run reliably means a real regression.

## Constraints

- Never change a failing test without explicit permission + written rationale that the test is wrong (per `CLAUDE.md`).
- Always validate at the root, all modules, under a display — never conclude from `:sweet` alone.
- Passing in isolation is insufficient evidence a flaky test is fixed; re-validate in the full root run, repeatedly.
