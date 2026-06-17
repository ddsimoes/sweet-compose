# Testing Sweet Compose

This document summarizes the testing approach for Sweet, based on the guidelines in `CLAUDE.md`.

## Key rules

- Always use Gradle to run tests.
- **Validate at the root, across all modules — not just `:sweet`.** A change is only green when
  `./gradlew test` passes for every module, including `:sample-sweet` and
  `:kotlinx-coroutines-swt`). `:sample-sweet` runs full-app integration tests (TodoMVC, KitchenSink,
  scroll) that catch layout/integration regressions the `:sweet` unit tests miss, so it is part of
  the gate.
  - `./gradlew test` — authoritative run (all modules).
  - `./gradlew test --continue` — see the complete failure set across modules in one run.
  - `./gradlew :sweet:test` / `:sample-sweet:test` — fast iteration only, never the final gate.
- **UI tests need a display.** Without one, every AutoSWT test fails with
  `kotlin.UninitializedPropertyAccessException` (the `lateinit display` is never set). This is an
  environment failure, not a real test failure. On a headless machine/CI run:
  `xvfb-run -a ./gradlew test --continue`.
- **Layout failures can be environment-sensitive** (font metrics, DPI, GTK/OS theme). The failing

## Deterministic measurement environment

To ensure widget measurements are reproducible across machines, tests pin the measurement font.


### Pinned measurement font

Call `useMeasurementFont()` inside `autoSWT { }` at the start of each text-sensitive
layout test. It loads DejaVu Sans Mono 11pt from
`sweet/src/test/resources/fonts/DejaVuSansMono.ttf` and sets
[SweetLayout.measurementFont] on the current Display. The font is registered
via `Display.loadFont()` once per JVM process.

```kotlin
import io.github.ddsimoes.sweet.test.useMeasurementFont

autoSWT {
    useMeasurementFont()
    testShell(...) { /* composable tree with text */ }.test { ... }
}
```

### Canonical Xvfb display

```
xvfb-run -s "-screen 0 1280x1024x24" ./gradlew test
```

Under this configuration:

- Prefer tests over manual samples for validation.
- Do not change existing failing tests unless you have explicit agreement and a clear rationale that the test is incorrect.

## Test determinism (priority issue — being resolved)

> **The layout/UI test suite is currently non-deterministic, and fixing that is a foundation
> priority.** The set of failing tests varies between machines (fonts/DPI/theme) and between runs on
> the same machine (low-rate flakiness), with no source change in between. While this is unresolved
> you cannot reliably tell a real layout regression from noise — which directly blocks trustworthy
> layout-correctness work.

Practical implications when working on Sweet:

- A red run is **not** automatically a real regression, and a green run does **not** prove
  correctness. Re-run the full root suite (repeatedly) before drawing conclusions.
- Passing a test **in isolation does not mean it is fixed** — it must hold in the full root run.
- When you see failures, record the exact test names **and** the environment (`$DISPLAY`, OS,
  whether `xvfb-run` was used) so the report is reproducible.

**Execution plan:** resolved (see `docs/roadmap/test-determinism.md` for root cause analysis and the per-Display isolation fix).
**Evidence & root cause analysis:** `../roadmap/test-determinism.md` (background reference).

## AutoSWT and layout assertions

- UI tests use AutoSWT:
  - Provide utilities for creating test shells and embedding Compose.
  - Offer helpers to find and interact with widgets (`find`, `findAll`, `typeText`, `doSelect`, etc.).
- Layout is verified via a fluent layout assertion API:
  - Single-control assertions (visibility, size, relative positioning).
  - Multi-control assertions (rows, columns, alignment, containment, spacing).
- Tolerance should be used sparingly and only when platform differences make exact assertions unreliable.


## Behavioral test contract (WS-4)

Every component with a stated implementation level must follow this contract:

| Level | Minimum test requirement | Label convention |
|-------|--------------------------|------------------|
| **Implemented** | At least one behavioral test (state change via interaction, measured size/position, callback fired, expected child present with expected properties). Must assert something real — not just `isNotEmpty()`. | Standard test name (no suffix). |
| **Partial** | Behavioral test for the working portion; smoke test for ignored params is acceptable. | Document ignored params in the test. |
| **Shim** | Smoke test (`composes without crashing`) is allowed. | Append `(shim smoke)` to test name or add `// Shim: smoke-only` comment. |

**Smoke tests** (only `shell.children.isNotEmpty()` or similar) are allowed exclusively for
*shim* and *partial* components and must be labeled as such. A test for an *implemented* component
that contains only `isNotEmpty()`-style assertions is a bug (WS-4).

**Behavioral tests** assert something real:
- State change after interaction (`doSelect()` → `assertEquals(expected, newState)`)
- Expected widgets exist with expected properties (`find<Button> { it.text == "Delete" }`)
- Callbacks fire (`assertTrue(callbackFired)`)
- Layout/position assertions (bounds, arrangement, alignment)

### Examples

```kotlin
// ✅ Behavioral test (implemented component — RadioButton)
@Test fun radioButton_toggles_selected_state() = autoSWT { /* … */ }.test { shell ->
    assertEquals(false, radioSelected)
    runOnSWT { button.doSelect() }
    assertEquals(true, radioSelected)  // state changed via interaction
}

// ✅ Smoke test (shim component — correctly labeled)
@Test fun `elevatedButton composes without crashing (shim smoke)`() = autoSWT { /* … */ }.test { shell ->
    val buttons = shell.findAll<Button> { it.text == "Elevated Button" }
    assertEquals(1, buttons.size)
}

// ❌ Behavioral test (implemented component — NOT OK)
@Test fun testTopAppBarComposition() = autoSWT { /* … */ }.test { shell ->
    assertTrue(shell.children.isNotEmpty())  // smoke-only, not behavioral
}
```

### Parity gate

For behavioral parity between Sweet and JetBrains Compose, the `SharedSampleParityTest` in
`:sample-sweet` mounts shared composables under Sweet's runtime with behavioral assertions.
This is the mechanical check that closes the "compiles ≠ works" gap. See its KDoc for details.
Run: `xvfb-run -a ./gradlew :sample-sweet:test --tests "SharedSampleParityTest"`.

## AutoSWT Display lifecycle

AutoSWT creates **one `TestDisplay` per JVM fork**, not per test class or method.
The same Display instance persists across all tests in a single Gradle worker.
Between tests, `AutoSWT.dispose()` calls `display.reset()`, which:
- Disposes all shells created during the test
- Nullifies all `display.setData(…)` entries (including per-Display caches like
  `LayoutCoordinator`, color systems, density caches)

This means per-Display data is effectively **reset between tests** — a new
`LayoutCoordinator.forDisplay(display)` call produces a fresh instance after each
test. The Display object itself is reused, and `Display.dispose()` is never called
within the JVM process.

## Test cleanup helper

A lightweight cleanup function is available for deterministic test state:

```kotlin
import io.github.ddsimoes.sweet.test.sweetTestCleanup

@Test fun myTest() = autoSWT {
    sweetTestCleanup()
    testShell(...) { ... }.test { ... }
}
```

It clears [SweetLayout.measureCounts] and resets [SweetLayout.measureCountEnabled].
Per-Display state isolation (LayoutCoordinator, SnapshotManager) is handled by
the infrastructure and does not need manual reset.

See the examples and patterns in `CLAUDE.md` and the tests under `sweet` and `sample`.

