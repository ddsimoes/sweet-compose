# Sweet Architecture – Runtime and Layout

This document summarizes the current architecture of Sweet’s runtime and layout system. It is intended as the primary entrypoint for contributors who need to understand how Sweet maps Compose to SWT.

## Runtime integration

- Sweet uses the official Compose Runtime without modification.
- A custom node/applier layer (e.g., `SWTNode`, `SWTNodeApplier`, `EnhancedSWTComposition`) manages the UI tree that maps to SWT widgets.
- Composition is bound to SWT via:
  - A frame clock integrated with the SWT event loop.
  - `Dispatchers.Main` backed by the SWT main dispatcher from `kotlinx-coroutines-swt`.
- Composition lifecycle is tied to SWT widget disposal (when the host `Composite` is disposed, the composition is cancelled and cleaned up).

The live recomposition path is owned by `ComposeManager`: it creates a `Recomposer` with a
`Dispatchers.Main.immediate + SWTMonotonicFrameClock` context, starts the frame clock, installs the
SWT snapshot write observer, and launches `recomposer.runRecomposeAndApplyChanges()`. Snapshot
writes request a frame; `SWTMonotonicFrameClock` then sends the `BroadcastFrameClock` frame on the
SWT UI thread. There is no separate manual frame pump in the SWT event loop.

### Frame-based layout coordination

Sweet coordinates Compose frames with SWT layout using a dedicated `LayoutCoordinator`:

- `SWTMonotonicFrameClock` drives frames on the SWT UI thread. Each frame is wrapped in:
  - `LayoutCoordinator.beginFrame()`
  - `BroadcastFrameClock.sendFrame(...)` (runs recomposition + applier callbacks)
  - `LayoutCoordinator.endFrame()` (flushes all pending layout requests)
- During a frame:
  - Calls to `LayoutCoordinator.requestLayout(composite)` enqueue the topmost `SweetLayout`-managed ancestor as a “layout root”.
  - At frame end, `LayoutCoordinator` sorts those roots and calls `layout(true, true)` once per root (skipping descendants of already-laid-out roots).
- Outside frames (pure SWT events such as shell resize or direct widget manipulation):
  - `requestLayout` performs immediate `composite.layout(true, true)` to preserve normal SWT behavior.

Key entry points that now route through `LayoutCoordinator.requestLayout` include:

- `SWTNodeApplier.onEndChanges` for regular dirty composites (including window and embedded content).
- `material3.Text` when text changes, instead of calling `label.parent?.layout()` directly.

Scroll containers use a slightly different path (see “Scroll and interop” below): invalidations inside scrolled content escalate through the Sweet `ScrollViewport`, which re-measures the content (scroll axis unbounded) and keeps the scroll range in sync.

## Layout system

Sweet implements a modern, Compose-inspired layout system on top of SWT:

- Layout is 100% controlled by Sweet/Compose:
  - SWT only reports available size (`Composite.clientArea`) and receives final bounds via `Control.setBounds`.
  - SWT’s own `LayoutData` is not used to drive Sweet’s layout decisions.
- Constraints-based measurement:
  - Each parent measures its children with `Constraints` (`minWidth`, `maxWidth`, `minHeight`, `maxHeight`).
  - Children choose a size within those bounds and return a “placeable” result.
  - Parents never decide their children’s positions during measurement, only their own size.
- Layout delegates:
  - Specialised delegates implement the measurement/placement logic for `Row`, `Column`, `Box`, and scroll containers.
  - Delegates are designed to do a single measurement pass per parent/child pair per layout cycle in the common case, with carefully-scoped exceptions when SWT semantics require a second measurement (e.g., `Box` with `fillChildren = true` measuring children at final parent size).


### Shadow layout tree (doc 11, M0-M2 cleanup — permanent as of 2026-06-10)

Sweet owns measurement and placement outright via a **shadow layout tree**
(`SweetLayoutNode`) built alongside the Compose node tree. The applier maintains it
structurally in `insertTopDown`/`insertBottomUp`/`remove`/`move`.

**Current state (M2, flag deleted):** The shadow pass is the **only** layout engine
for all Sweet-managed composites. There is no feature flag — the shadow tree is
always active.

**How it works:**
- `SweetLayout.layout()` — no shadow node (e.g. window Shell): fills the boundary
  child to clientArea; the boundary's `layout()` then drives the pass.
- `SweetLayout.layout()` — has shadow node:
  - Pass root (topmost shadow-managed composite under FillLayout/Shell/island):
    runs `layoutPass()` with `Constraints.fixed(clientArea)`.
  - Nested Compose containers have their SWT `layout` nulled after insertion; dirty
    requests climb to the nearest Sweet layout driver instead of asking SWT to recurse.
- `layoutPass(root, constraints)`: measure (constraints down, sizes up via
  delegates) then place (single `setBounds` per control). NodeMeasurable extends
  SWTControlMeasurable for uniform modifier handling (padding/offset/fill/size/aspect).
- `computeSize()` measures through the shadow node when one is available, using the
  current hints as constraints. Non-shadow SWT hosts still fall back to the legacy
  child-list measurement path.

**What was deleted (M2):**
- `lastBundle`/`lastSpec`/`lastConstraints` instance caching
- `LayoutCoordinator.findLayoutRoot` ancestor walk
- Shell computeSize monotonic growth lock
- `m1ShadowPassEnabled` feature flag
- `PENDING_CONSTRAINTS_KEY` out-of-band constraint channel

**What was deleted (doc 13, 2026-06-11):** `layoutLocal.kt` (the last reflection into
SWT internals — `sweet/src/main` is now reflection-free) and all `ScrolledComposite`
min-size machinery (see "Scroll and interop").

Nested Compose containers now run with `layout = null`; root/boundary adapters and
scroll content roots keep a `SweetLayout` driver because SWT still needs a boundary
entry point for resize and preferred-size queries.

### Ordered modifier chain (doc 12, M0 — active as of 2026-06-10)

Compose modifiers are an ordered chain; each layout modifier wraps everything to its
right. Sweet previously flattened this into a `SweetCompositionData` bag, losing order.
The ordered chain restores MPP semantics.

```kotlin
// These are now DIFFERENT (was identical under flat fold):
Modifier.padding(10).size(50)     // outer 70×70, inner 50×50
Modifier.size(50).padding(10)     // outer 50×50, inner 30×30
```

**How it works:**
- `applySWTModifier()` builds a `ResolvedModifierChain` (ordered list of `LayoutStep`)
  stored in `SweetCompositionData.modifierChain`.
- Concrete steps: `PaddingStep`, `SizeStep`, `FillMaxWidthStep`, `FillMaxHeightStep`,
  `OffsetStep`. Each implements `measureWrapped(constraints, inner) -> StepResult`.
- `SWTControlMeasurable.measure()` detects the chain and applies steps recursively
  for both shadow-tree containers and leaf controls (constraints flow down outside-in,
  sizes flow up inside-out), skipping the flat modifier pipeline when a chain exists.
- Parent-data elements (weight, alignment) stay in the flat bag — they are consumed
  by the parent's delegate, not wrapped.

**Test matrix:** `LayoutModifierOrderTest` — 8 rows covering padding-order,
size-order, fillMax-inside-padding, double-additive. All 8 pass.

The layout engine migration is complete: per-Display LayoutCoordinator, shadow-tree unification, and ordered modifier chains are all active with no feature flags.

- Alignment and weight compatibility:
  - Child alignment is driven by Compose-style alignment objects and Sweet’s own layout data.
  - Alignment-line based modifiers (`alignBy` / `AlignmentLine`) behave as follows:
    - `alignBy(alignmentLineBlock)` is honored by `Row`/`Column` as a simple alignment-line group: children that provide a block are positioned so that the line returned by the block coincides across siblings.
    - `alignBy(alignmentLine)` (including `alignByBaseline`) is implemented by mapping the requested line into the same block-based path; baseline lines use SWT font metrics where available.
    - Explicit propagation/merging of custom `AlignmentLine` values between arbitrary parent/child layouts is not yet implemented; only the local per-child blocks and baseline lines are interpreted.
  - Weighting is driven by `SweetLayoutData.weight`, typically set via `Modifier.weight` (which uses `WeightModifier`); the legacy `LayoutWeightElement` delegates to the same data path for source compatibility.
  - Background color is applied via `Modifier.background` and an internal `BackgroundColorModifier` that uses a per-display SWT color cache; shape parameters are accepted for API compatibility but are not yet interpreted by the layout system.

- During `computeSize`:
  - Converts the SWT size hints into `Constraints`.
  - Asks the layout delegate to measure the subtree and caches a `MeasureBundle`.
  - Returns the measured size as the composite’s preferred size.
- During `layout`:
  - Converts the available client area into exact constraints.
  - Reuses the cached `MeasureBundle` where possible.
  - Delegates placement, which calls `setBounds` on SWT widgets as needed.

SWT widgets act as leaf nodes:

- They report preferred sizes via `Control.computeSize`.
- Sweet can override these using constraints and modifiers (e.g., `size`, `fillMaxSize`, `padding`).
- Embedded SWT widgets still participate in the Sweet layout system through their wrapper composites.

## Scroll and interop

- Scrollable content is wrapped in Sweet's own `ScrollViewport`
  (`io.github.ddsimoes.sweet.widgets`, public) via `SWTScrollableNode` — `ScrolledComposite`
  is gone (workstream 13, 2026-06-11):
  - The viewport is a plain `Composite(V_SCROLL|H_SCROLL)` whose native `ScrollBar`s are
    driven by Sweet. Its private `ViewportLayout` measures the content with the scroll axis
    **unbounded** and the cross axis fixed to the viewport (Compose scroll semantics), sizes
    the content (filling the viewport when smaller), clamps the scroll offset, and syncs the
    bars (`maximum = contentExtent`, `thumb = viewportExtent`; hidden when content fits).
  - Scrolling is placement-only: `content.setLocation(-x, -y)` — no layout pass runs.
  - Mouse-wheel is handled explicitly by the viewport (plain composites do not auto-scroll;
    verified on GTK by `ScrollViewportProbeTest`).
  - Invalidations originating inside scrolled content escalate through the viewport:
    `SweetLayout.layout` detects a pass root whose SWT parent is a `ScrollViewport` and calls
    `viewport.layout(true)` instead (guarded by `viewport.isLayingOut`).
- Scroll state semantics:
  - `ScrollState` is axis-agnostic and represents a single offset in pixels (Y for `verticalScroll`, X for `horizontalScroll`).
  - Programmatic `suspend scrollTo(value)` jumps synchronously (clamped to `0..maxValue`); no fling or animation yet (`animateScrollTo` is gated on doc 21's `AnimationSpec` types).
  - `maxValue` = `contentExtent - viewportExtent` (>= 0), updated on every viewport layout; `viewportSize` likewise.
  - User-driven scrollbar changes are reflected back into `ScrollState.value`; `isScrollInProgress` is `true` during scrollbar thumb drags (best-effort), gesture/fling tracking is not implemented yet.
- Interop APIs allow:
  - Embedding Compose content into existing SWT apps.
  - Embedding complex SWT widgets (e.g., `Browser`, `StyledText`) into Sweet layouts.

### Layout and scroll gotchas

When working on layout or scroll integration, keep these constraints in mind:

- For Sweet-managed composites (those using `SweetLayout`), do not call `layout(true, true)` directly from composables or modifiers. Instead, rely on `SWTNodeApplier.onEndChanges` and `LayoutCoordinator.requestLayout`, or on existing helpers such as `updateScrollerMinSize`.
- For scroll containers:
  - Let `SweetLayout` measure the scroll content; do not try to manually clamp its size inside `updateScrollerMinSize`.
  - Ensure the scroller’s viewport remains constrained by the parent shell/composite; use `setMinSize` to reflect content size and leave `clientArea` to SWT.
- For weights in `Row`/`Column`, always drive them via `WeightModifier` (e.g. `Modifier.weight`) so the delegates can use `SweetLayoutData.weight`; the legacy `LayoutWeightElement` now delegates to the same data path for source compatibility.

## Windowing semantics

- `WindowState`:
  - Tracks `placement` (`Floating`, `Maximized`, `Fullscreen`), `isMinimized`, `position`, and `size` (`DpSize`).
  - High-level `ApplicationScope.Window` keeps `WindowState` in sync with the underlying SWT `Shell` (size, position, minimized) and applies state changes back to the shell via `applyWindowStateToShell`.
- Placement and minimized:
  - `Floating` clears both `shell.fullScreen` and `shell.maximized`.
  - `Maximized` sets `shell.maximized = true` and `shell.fullScreen = false`.
  - `Fullscreen` sets `shell.fullScreen = true`.
  - `isMinimized` maps directly to `shell.minimized`.
- Position:
  - `WindowPosition.Absolute(x, y)` is interpreted in Dp and converted to SWT pixels; when `isSpecified` is true it updates `shell.location`.
  - `WindowPosition.PlatformDefault` does not override SWT’s default positioning.
  - `WindowPosition.Aligned` uses an `Alignment` value (compatible with Compose Desktop) to place the window at logical positions within the primary monitor’s client area (e.g., centered, top-start, bottom-end) and updates `shell.location` accordingly.

For detailed algorithms and examples, see the source code under `sweet/` and the historical architecture docs referenced above.
