# Changelog




## 2026-06-15 — Limitations documentation consolidation

New `docs/limitations.md` as the single source of truth for current and long-term
limitations, tagged per entry (Partial / Shim-no-op / Not implemented / By design / Planned)
and grounded in the current source. Covers platform (desktop + x86_64-only SWT artifacts),
component shims (`Slider`, `CircularProgressIndicator`, button variants, boxed state shims),
partial APIs (`Button`/`Tab` text capture, `Text` `textAlign`, `TextField`, `RadioButton`,
`TopAppBar`, `Surface`, `Scaffold`, `AlertDialog`, `ScrollableTabRow`, `alignBy`), drawing
gaps (blend-mode tier split, ignored image `colorFilter`, unwired `Modifier.paint`,
`drawWithContent`/`drawWithCache`, no GraphicsLayer/Skia), animation gaps, eager lazy lists,
scroll fling, test determinism, and the six-phase roadmap status.

Cross-references added from `docs/index.md`, `docs/overview.md`, `docs/roadmap/long-term-direction.md`,
`README.md`, `CLAUDE.md`, and `docs/guides/drawing.md` (See also).

Doc-drift fixes in `docs/guides/drawing.md`: the BlendMode table now reflects the Tier-1
software-composited path on Canvas/Image/Icon scopes, `Modifier.paint` is documented as an
unwired marker modifier, and ignored image `colorFilter` support is now called out explicitly.
## 2026-06-15 — State-review follow-up (WS-1..WS-8, branch improvements-1)

Eight workstreams from the project state review. Suite green
(330 tests, 0 failures; `:sweet:apiCheck` passing). Summary:

### WS-1 — Documentation drift & cleanup
README Quick Start rewritten to `application { Window {} }` (matches `minimal.kt`).
Component table corrected: Switch/RadioButton→Implemented (real SWT.TOGGLE/SWT.RADIO),
Icon→Implemented (real vector rendering via Canvas). Project structure updated
(removed stale `sample/`/`swttest/`, added real modules). Dead doc links removed
from README, overview.md, drawing.md. Roadmap checkboxes honestly reflect reality.
`:sample:run`→`:sample-sweet:run`.

### WS-2 — CI & publishable artifact
`.github/workflows/ci.yml`: xvfb + GTK3, runs `xvfb-run -a ./gradlew test :sweet:apiCheck --continue`
on push/PR. `repeatTests` determinism loop in root `build.gradle.kts` (`-PrepeatTests=N`).
Publishing configuration remains deferred (phase Later).

### WS-3 — Parity gate (behavioral-first, D-1 Option A)
A1: `SharedSampleParityTest` in `:sample-sweet` (9 tests, behavioral assertions on all 5 shared
composables — TodoMVC mount/add/delete/filter, KitchenSink3 mount/tab-switch, SweetLayoutTest,
AnimationDemo, DrawingKitchenSink). Regression-verified.
A2: `3rdparty/COMPOSE_REVISION.txt` + `fetch-upstream.sh` pin vendored upstream; 30-package
allowlist; `verifyUpstreamPin` gradle task. BCV ABI guard (binary-compatibility-validator on
`:sweet`, scoped to `io.github.ddsimoes.sweet.*`; 635-line golden dump; `apiCheck` enforced in CI).
All drift-gate vaporware claims removed from CLAUDE.md, overview.md, drawing.md.

### WS-4 — Test contract upgrade
5 legacy Tab files collapsed into `TabTest.kt` (8 tests). TopAppBar upgraded from 5 smoke tests
to 4 behavioral + 1 data-class. ButtonVariants reorganized: shim tests labeled, RadioButton→behavioral.
StateManagementTest already behavioral (mutate-then-assert). Contract codified in
`docs/development/testing.md`.

### WS-5 — Widget fidelity vertical slice (TextField, D-2 Option A)
placeholder (composable sibling when empty), isError (bg/fg colors), label/leadingIcon/trailingIcon
(OutlinedTextField Card wrapper slots), readOnly, singleLine implemented. OutlinedTextField diverged
from TextField (Card + icon slots, not alias). `maxLines`/`minLines` now select single- vs multi-line
rendering. 12 behavioral tests. Deferred: visualTransformation, floating-label animation.

### WS-6 — Animation runtime
`spring()`/`tween()` factories. `Animatable.animateTo` drives real spring physics via `SpringSimulation`
(per-dimension solvers) + tween paced by frame clock's `frameTimeNanos`. `animateFloatAsState`/
`animateDpAsState`: `Animatable.value` backed by `mutableStateOf` (matches AOSP), writes drive
recomposition via snapshot apply observer + frame clock. EasingParityTest (12), SpringAnimationTest (3),
AnimateAsStateTest (2). Deferred: Transition/rememberInfiniteTransition, sample update.

### WS-7 — Input & accessibility
Hover wired: `ModifierReconciler` reconciles `HoverableElement` → SWT.MouseEnter/MouseExit listeners.
Behavioral tests for clickable, focus (onFocusChanged + focusRequester), hover, double-click,
context-menu, combined modifiers. 9 tests (`InputEventTest.kt`). Deferred: Tab/Shift-Tab focus
traversal, DnD, per-control key routing.

### WS-8 — Missing desktop widgets
DropdownMenu behavioral test (4 tests: expanded render, collapsed empty, item click,
ExposedDropdownMenuBox toggle). ProgressIndicator tests (6 behavioral: progress→selection mapping
+ coercion; indeterminate style). Remaining widgets deferred to incremental follow-ons.
### Known gaps
Open follow-ups F-1..F-11 were tracked in the branch follow-up plan.

## 2026-06-15 — Deep-review fixes (branch improvements-1)

Post-merge audit of the state-review follow-up (WS-1..WS-8). Suite confirmed green
(`:sweet:test` 330, 0 failures; `:sweet:apiCheck` passing). Fixes:

### TextField isError SWT Color leak
`isError` allocated a fresh `org.eclipse.swt.graphics.Color` on every toggle and never
disposed it (a native handle leak under the pinned SWT graphics fragment). The color is
now allocated at most once per widget via `errorBackgroundColor()`, cached on widget data,
and disposed on widget dispose.
- File: `sweet/src/main/kotlin/androidx/compose/material3/TextField.kt`

### TextField programmatic-change guard stabilized
`inProgrammaticChange` was a plain `AtomicBoolean` recreated each recomposition, so the
factory's `ModifyListener` and the `update`/`SideEffect` writers used different instances
after the first recomposition and the guard stopped suppressing spurious `onValueChange`.
Now `remember`-ed so all paths share one instance.
- File: `sweet/src/main/kotlin/androidx/compose/material3/TextField.kt`

### Behavioral test upgrades
`DropdownMenuTest."exposedDropdownMenuBox toggles expanded"` now synthesizes a `MouseDown`
and asserts the toggle (was presence-only smoke); `ButtonVariantsTest` spacer now measures
the inter-label gap (was presence-only); new `TextFieldTest` password test asserts
`SWT.PASSWORD`. `CLAUDE.md` broken `:sample:test` task paths corrected to `:sample-sweet:test`.

Known gaps were tracked in the branch follow-up plan (F-1..F-11).

## 2026-06-15 — Path A: Low-hanging follow-ups (F-3, F-4, F-6, F-10, F-11)

Addressing the low-severity follow-ups from the deep review:

### F-10 — CHANGELOG backfill
WS-1..WS-8 entries added (see above).

### F-11 — Animation: CancellationException + deltaMillis precision
`animateTo` now rethrows `CancellationException` (was silently swallowed — prevented proper
coroutine cancellation). `animateWithSpring` deltaMillis changed from `Long` truncation to
`Float` precision; `SpringSimulation.updateValues` param changed from `Long`→`Float`.

### F-6 — AnimationDemo conversion (animation API dogfood)
**Deferred.** Replacing the hand-rolled `LaunchedEffect { delay(16) }` loop with
`Animatable`/`animateFloatAsState` fails to drive widget position changes in autoSWT
context (frame-clock/dispatcher interaction). `AnimateAsStateTest` separately
validates the animation API. Sample kept original code; investigation tracked for
medium-term. Zero test failures.

### F-3 — A2 shape diff formally dropped
The upstream API shape diff (A2) was never implemented and offered no signal the behavioral
parity gate (A1) doesn't already catch. Decision: drop. Updated CLAUDE.md.

### F-4 — Behavioral parity tests upgraded
`sweetLayoutTest`: added interactive click counter; test clicks "Increment" and asserts
label text changes (Clicks: 0→1→2). `drawingKitchenSink`: test now toggles Pause/Resume
button and asserts text flips between "Resume" and "Pause" — no longer presence-only smoke.

## 2026-06-11 — Drawing Kitchen Sink & Layout Fixes

### Button text-change triggers relayout (Issue #1)
`Button` now calls `requestLayout()` on the backing SWT Button and its parent
`Composite` when text changes via `LocalButtonTextCapture`. Previously a text
change (e.g., "Pause" → "Resume") was applied without layout invalidation, so
the button width stayed fixed and longer text overflowed.
- File: `sweet/src/main/kotlin/androidx/compose/material3/Button.kt`

### Label text-change invalidates cached size (Issue #2)
`Text` composable now calls `label.requestLayout()` when text changes on
recomposition, before requesting the parent layout. SWT caches `computeSize()`
and `setText()` alone does not invalidate that cache; without the call, a label
growing beyond its original text width was clipped. The invalidation is
guarded to skip initial composition (when fonts are not yet applied), avoiding
zero-size layout during first render.
- File: `sweet/src/main/kotlin/androidx/compose/material3/Text.kt`

### Blend mode compositing via off-screen Image (Issue #3)
`SwtCanvasBackend` now implements per-pixel blend-mode compositing for all
`drawRect` calls when the blend mode is not `SrcOver`. The off-screen Image
used by `CanvasIntegration` is passed to the backend; destination pixels are
read from it, source content is rendered to a temporary Image, and the two
are composited via Porter-Duff and separable blend formulas (Multiply, Screen,
Plus, etc.) using SWT `ImageData`. DstOver, SrcIn, SrcOut, Plus, Multiply all
render correctly in the Drawing Kitchen Sink blend-mode demo.
- Files: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/drawing/SwtCanvasBackend.kt`,
  `sweet/src/main/kotlin/io/github/ddsimoes/sweet/drawing/CanvasIntegration.kt`

### Frame clock inline dispatch (Issue #4)
`SWTMonotonicFrameClock` now dispatches frames inline when the consumer is
already on the SWT display thread, instead of unconditionally posting via
`runOnUIThread`. During initial composition the frame clock consumer and
`LaunchedEffect` share the same UI thread; posting through the event queue
deadlocked because composition had not yet yielded. Inline dispatch avoids
the deadlock, making `withFrameNanos {}` safe to use during composition.
- File: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/internal/FrameClock.kt`

### Modifier re-apply triggers layout invalidation
`applySWTModifier` now detects layout-affecting flat data changes (padding, size,
offset, constraints, modifier chain) and requests layout on the nearest parent
`Composite` via `LayoutCoordinator`. Previously, state-driven modifier value
changes (e.g. animated `Modifier.padding`) during recomposition updated the
`SweetCompositionData` bag but never invalidated SWT layout, so animated
controls appeared stationary.
- File: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/internal/ModifierReconciler.kt`

### sizeIn now works with ordered layout chain
Added `SizeInStep` to the ordered modifier chain (`LayoutSteps.kt`) so
`Modifier.sizeIn(min, max)` is applied during `measureWithOrderedChain`.
Previously `sizeIn` was skipped in the chain and only applied by the flat
`measureFlat` path, which was bypassed whenever any ordered step (e.g. `padding`)
existed. This made `Modifier.sizeIn(minWidth=200).padding(8.dp)` ignore the
minimum width.
- Files: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/internal/LayoutSteps.kt`,
  `sweet/src/main/kotlin/io/github/ddsimoes/sweet/internal/ModifierReconciler.kt`

### Padding preserves unbounded constraints
`Constraints.inset()` no longer subtracts padding from `Int.MAX_VALUE`, the
sentinel for unbounded constraints. Previously `(Int.MAX_VALUE - pad)` was
treated as bounded by downstream steps like `fillMaxWidth`, causing content
to expand to near-2B pixels when padding was combined with `fillMaxSize`.
- File: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/internal/LayoutSteps.kt`

### Arrangement.spacedBy uses display density
`spacingFor` and `Arrangement.arrange()` in `ColumnDelegate` and `RowDelegate`
now use the actual display density (via `Display.getSweetDensity()`) instead
of `Density.Default`. On HiDPI displays, `spacedBy(Dp)` spacing and
arrangement positions were computed at 1x density, producing wrong pixel
values.
- File: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/layout/delegate/LayoutDelegates.kt`

### Row default verticalAlignment → CenterVertically
`Row` and `LazyRow` now default to `Alignment.Vertical.CenterVertically` instead
of `Alignment.Vertical.Top`, matching MPP Compose semantics. This fixes
TodoMVC's checkbox appearing clipped (top-aligned in a row with taller text)
and the todo item row showing text at top while checkbox and button centered.
- Files: `sweet/src/main/kotlin/androidx/compose/foundation/layout/Row.kt`,
  `sweet/src/main/kotlin/androidx/compose/foundation/lazy/LazyRow.kt`


### Remove Alignment.Horizontal/Vertical companion objects
The companion objects on `Alignment.Horizontal` and `Alignment.Vertical` (which
held duplicate constant aliases `Start`, `CenterHorizontally`, `End`, `Top`,
`CenterVertically`, `Bottom`) are removed. These constants exist only in
`Alignment.Companion` in MPP Compose, and the `Alignment.Horizontal.*` form
caused compilation failures in `sample-jetbrains`. All Sweet internal code
and samples now use the standard `Alignment.*` form.
- Files: `sweet/src/main/kotlin/androidx/compose/ui/Alignment.kt`,
  `sweet/src/main/kotlin/androidx/compose/foundation/layout/Column.kt`,
  `sweet/src/main/kotlin/androidx/compose/foundation/layout/Row.kt`,
  `sweet/src/main/kotlin/androidx/compose/foundation/lazy/LazyColumn.kt`,
  `sweet/src/main/kotlin/androidx/compose/foundation/lazy/LazyRow.kt`,
  `sweet/src/main/kotlin/androidx/compose/material3/AppBar.kt`,
  `sweet/src/main/kotlin/io/github/ddsimoes/sweet/internal/WidgetFactories.kt`,
  `sweet/src/main/kotlin/io/github/ddsimoes/sweet/layout/delegate/LayoutDelegates.kt`,
  test/sample files

### SweetLayoutTest compiles with JetBrains Compose MPP
The hardlinked `SweetLayoutTest.kt` now uses `application { Window {} }` for its
entry point (instead of Sweet-specific `runCompose`) and `Alignment.CenterHorizontally`
/ `Alignment.CenterVertically` (instead of the `Alignment.Horizontal`/`Vertical`
companion form), matching the API surface shared by Sweet and JetBrains Compose
Desktop. The sample compiles cleanly against both runtimes.
- File: `sample-jetbrains/src/main/kotlin/io/github/ddsimoes/sweet/sample/SweetLayoutTest.kt`

### Fix CubicBezierEasing Cardano root solver
`findFirstCubicRootImpl` did not divide by the leading coefficient `d` before
running Cardano's algorithm, causing the root to be computed against the wrong
polynomial. The degenerate (non-cubic) case also had a broken quadratic
discriminant (`b^2 - 4b` instead of `b^2 - 4ac`), and the three-real-root case
skipped the first root. Fixed to match the MPP implementation in
`compose.ui.graphics.Bezier.kt`. `FastOutSlowInEasing.transform(0.5f)` and
other standard Material Design easing curves now produce correct values instead
of throwing `IllegalArgumentException`.
- File: `sweet/src/main/kotlin/androidx/compose/animation/core/Easing.kt`

### Column/Row measure non-weighted children against remaining space
`ColumnDelegate` and `RowDelegate` now measure each non-weighted child against
the REMAINING main-axis space (parent max minus space consumed by previous
children plus arrangement spacing), matching MPP `RowColumnMeasurePolicy`.
Previously all children saw the full parent extent, so `fillMaxHeight` on a
later sibling overflowed instead of resolving to "what is left".
- File: `sweet/src/main/kotlin/io/github/ddsimoes/sweet/layout/delegate/LayoutDelegates.kt`


### Flat field write deletion (doc 12, second attempt)
- `applyTo` no-ops on `FillMaxWidthModifier`, `FillMaxHeightModifier`, `SizeModifier`, `PaddingModifier`,
  `OffsetModifier`. Flat writes deleted; chain is the single source of truth for chain-modeled modifiers.
- `LayoutPass.kt`: pass-root origin reads `chainPaddingStart`/`chainPaddingTop` (chain-first, flat fallback).
- Intrinsics: `ResolvedModifierChain.sizeWidth`/`sizeHeight` + `chainSize*` extensions (outermost SizeStep wins).
- `ModifierReconciliationTest`: rewritten to chain-state assertions (owner approved). Additive tests were
  passing vacuously with no-ops; now assert accumulated chain totals.
- Analysis: flat-field writes removed; chain-first placement and measurement unified in `SWTControlMeasurable` and `LayoutDelegates`.

### Shadow-tree cleanup (doc 11)
- `PENDING_CONSTRAINTS_KEY` deleted; `SweetLayout.computeSize` measures through `NodeMeasurable` when shadow
  node exists.
- Nested Compose containers (`Card`, `Column`, `Row`, `Box`) get `layout = null` after insertion; root
  containers keep `SweetLayout` as the SWT layout driver.
- Applier `move`/`remove`/`clear` operations now sync the shadow tree correctly. Keyed reorder test added.
- Shell: minimal fallback (no shadow node, fills boundary child to clientArea).

### Chain-aware placement (doc 12)
- `ResolvedModifierChain` accumulates `totalOffsetX/Y`, `totalPaddingStart/Top/End/Bottom`.
- `SWTControlMeasurable.place()` and `LayoutDelegates` read chain-first with flat fallback.
- Chain consumption unified in `SWTControlMeasurable.measure()` base class (was `NodeMeasurable` override).

### Deferred items
- Doc 14 (container collapsing): performance optimization, gated on soak period for docs 11/13.
- Doc 13 `animateScrollTo`: needs `AnimationSpec` types (doc 21 phase 2).
- Doc 13 virtualization (phase B): separate effort.

## 2026-06-11 — Foundation Consolidation: Scroll & Lazy (Docs 13 complete)

### ScrollViewport
- Sweet-owned `ScrollViewport` (public, `io.github.ddsimoes.sweet.widgets`) replaces `ScrolledComposite`.
  Plain Composite with native ScrollBars driven by Sweet. Content measured with scroll axis unbounded.
  Scrolling is placement-only. Wheel handling explicit. Zero reflection in sweet/src/main.
- `ScrollState` MPP-aligned: `suspend scrollTo`, `maxValue`/`viewportSize`, `canScrollForward/Backward`,
  `isScrollInProgress` via SWT.DRAG. `InteractionSource` moved to `foundation.interaction`.

### LazyListState (Phase A)
- `LazyListState` is now a real class (split to own file per one-file-per-class rule).
  `firstVisibleItemIndex`/`firstVisibleItemScrollOffset` computed from viewport scroll vs. child bounds.
  `layoutInfo` with `LazyListLayoutInfo` and `visibleItemsInfo` (using new `LazyListItemInfo`).
  `suspend scrollToItem(index, scrollOffset)` delegates to internal `ScrollState.scrollTo`.
- `LazyColumn` and `LazyRow` now accept optional `state: LazyListState` parameter (default = `rememberLazyListState()`).
- New support types: `Orientation` enum (`gestures` package), `LazyListItemInfo` interface,
  `LazyListLayoutInfo` interface.
- All 328 tests green, 0 failures.
## 2026-06-08 — Drawing API Bug Fixes

### Bug #1 (High): Canvas onDraw covered by Modifier.background()
Fixed paint ordering when `Canvas` composable is combined with `.background(shape)` or `.drawBehind()`.
- `CanvasIntegration` paint listener now invokes draw-behind callbacks (from `reconcileDraw`) into the offscreen buffer *before* the `onDraw` callback.
- `reconcileDraw` no longer installs a separate `PaintListener` on controls that already have a Canvas paint path — avoids the duplicate listener that painted on top.
- `Canvas.kt` update order swapped: `onDraw` callback is set *before* modifier application so the Canvas path is detectable.
- Test: `CanvasTest.canvas_onDraw_visible_above_background_modifier` — pixel-level verification that onDraw blue rect is visible above a gray background modiifier.

### Bug #2 (High): addRoundRectToPath produces self-crossing (bowtie) path
Fixed corner arc angles in `SwtCanvasBackend.addRoundRectToPath()`. Each corner arc now starts at the endpoint of the preceding `lineTo` and sweeps −90°, eliminating the straight connector that crossed the interior.
- Test: `CanvasTest.gradient_filled_circle_no_bowtie` — CircleShape with horizontal gradient composes without error.

### Bug #3 (Medium): clip(shape) clips to bounding rectangle only
`applyClipRegion` now tessellates rounded rects and generic paths into polygon `Region` objects.
- `Outline.Rounded`: sampled corner arcs produce a polygon matching the actual rounded shape.
- `Outline.Generic`: path ops (including curves) are tessellated to line segments via Bézier subdivision.
- `AddRect`, `AddOval`, `AddArc`, `AddRoundRect`, `AddPath` ops are handled for clip-region building.
- Test: `CanvasTest.clip_circle_shape_clips_to_circle_not_rectangle` — verifies a non-null SWT Region is set.

### Bug #4 (Low): println on hot paint path
Replaced all `println` calls in `SwtCanvasBackend` with `SweetDebugger.log()` via a per-instance `BitSet` that logs each degradation type once.
- BlendMode, RadialGradient, SweepGradient, and ColorFilter-on-drawImage warnings are gated and single-report.

### Bug #5 (Low): convertToSwtPath drops relative beziers
`convertToSwtPath` now tracks the current point (cx, cy) and translates `RQuadTo` and `RCubicTo` ops to absolute `QuadTo`/`CubicTo` calls.

### Bug #6 (Low): density hardcoded to 1f
`SweetDrawScope` now accepts `density` and `fontScale` as constructor parameters (defaulting to `1f` for backward compatibility). Call sites in `CanvasIntegration` and `internal.kt` thread the real display density from `Display.getSweetDensity()`.

### Bug #7 (Low): per-frame offscreen Image/GC allocation
`CanvasIntegration` now caches the offscreen `Image`+`GC` pair per Canvas control, reallocating only on size change. The cache is cleaned up in a `DisposeListener`.

### Bug #8 (Low): ColorFilter.Tint ignores blendMode
`applyColorFilter` now distinguishes the `Tint.blendMode`. For `SrcIn` and `SrcAtop` (the default and most common), it correctly replaces color with the tint color modulated by destination alpha. Other modes fall back to the existing multiplicative approximation.
