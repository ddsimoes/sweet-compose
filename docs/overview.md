# Sweet Compose – Overview

Sweet Compose is a Kotlin UI toolkit that brings Jetpack Compose-style declarative UI to SWT (Standard Widget Toolkit). It aims to be:

- A robust way to build native SWT UIs using a modern, reactive API.
- A migration bridge from “classic” SWT codebases toward Compose/Material-style UIs.

## Execution modes

Sweet is designed to support two modes (see `docs/roadmap/long-term-direction.md` for details):

1. **SWT-first mode (current, primary focus)**
   - Compose is “just another way” to write SWT UIs.
   - Visuals and behavior follow native SWT look & feel.
   - Ideal for incrementally modernizing existing SWT apps without changing appearance.

2. **Compose-first / Material-like mode (future, optional)**
   - Tries to get closer to Compose Desktop / Material behavior and visuals.
   - Still runs on SWT, but may use custom drawing (Canvas / Skia) where SWT is insufficient.
   - Intended for apps that are mostly Compose-based and want a more Material-like look while still using SWT.

## Architecture snapshot

At a high level (see `architecture/runtime-and-layout.md` for details):

- Sweet uses the standard Compose Runtime.
- A custom node/applier layer maps composables to SWT widgets.
- A modern layout system (`SweetLayout` + layout delegates) handles measurement and placement, independent of SWT’s `LayoutData`.
- SWT widgets are integrated as leaf nodes or via interop APIs.
- Drawing (`Canvas`, `Image`, `DrawScope`, `Painter`, `ImageBitmap`) runs on a minimal SWT `Canvas`/`GC` backend today; the active roadmap hardens it into a faithful Compose subset with draw modifiers and vector icons (see `roadmap/drawing-foundation.md`).
- A dedicated SWT coroutine dispatcher (`kotlinx-coroutines-swt`) provides `Dispatchers.Main` on the SWT UI thread.

## Documentation map

- **Architecture and internals**
  - `architecture/runtime-and-layout.md`
  - `architecture/threading-and-coroutines.md`
  - `architecture/interop.md`
- **Development and contributions**
  - `development/contributing.md`
  - `development/testing.md`
- **Current and long-term limitations**
  - `limitations.md` — single source of truth for what is partial, shim, by design, or planned
- **Usage guides**
  - `guides/drawing.md`
- **Roadmap and status**
  - `roadmap/long-term-direction.md` (long-term evolution plan)
  - `roadmap/drawing-foundation.md` (drawing/graphics foundation plan)
  - `roadmap/test-determinism.md` (test determinism analysis)

## Compatibility surface

Sweet provides a Compose-compatible API surface under `androidx.compose.*` packages for source
compatibility. Not all APIs are fully implemented — some are partial (core works, some params
ignored), shims (compiles but does something different), or stubs (placeholders only). See
`limitations.md` for the authoritative per-area breakdown. Two complementary checks exist:
- **Behavioral parity gate** (`SharedSampleParityTest` in `:sample-sweet`): mounts shared composables
  under Sweet's runtime with AutoSWT and asserts real behavior (widget counts, state changes via
  interactions, filter round-trips). This is the check that closes the "compiles ≠ works" gap.
- **Upstream pin verification** (`./gradlew :sweet:verifyUpstreamPin`): confirms the vendored
  MPP upstream is checked out at its pinned revision and the mirror-scope allowlist is present. This
  is a reproducibility precondition for a future BCV signature diff (WS-3 A2), NOT itself a shape or
  parity check.

When JVM assertions are enabled (`-ea`), `SweetDebugger` logs a message whenever a shim/no-op API
is invoked or a partial API receives ignored parameters with non-default values.

## List semantics (LazyColumn / LazyRow)

The current `LazyColumn` / `LazyRow` implementations in Sweet are **eager**:

- All items passed via the `LazyListScope` are composed eagerly into a `Column`/`Row`.
- A Sweet-owned `ScrollViewport` provides scrolling, but there is no item virtualization yet.
- This means behaviour is predictable and Compose-like, but very large lists do not
  get the memory/performance benefits of a true lazy list.

Future work may introduce a more sophisticated, virtualized lazy list while keeping
the existing APIs source-compatible where possible.
