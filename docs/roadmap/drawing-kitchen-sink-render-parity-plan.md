# Drawing Kitchen Sink Render Parity Plan

**Status:** Partially addressed — reactive redraw, paint ghosting, border insets fixed post-consolidation. Stroke fidelity (cap/join/dash), gradient paint state, and screenshot normalization remain | **Created:** 2026-06-08 | **Scope:** `tmp/Screenshot-mpp.png` vs `tmp/Screenshot-sweet.png`

This plan covers the work needed to make the Drawing Kitchen Sink comparison meaningful and then fix the Sweet-side rendering issues that explain the remaining visual divergence from JetBrains Compose Desktop.

It complements `docs/roadmap/drawing-foundation.md` and `docs/roadmap/kitchen-sink-compat-plan.md`. The compatibility plan is mostly about making standard Compose APIs compile against Sweet. This plan is about comparing and fixing runtime visual output.

## 1. Goal

The end state is:

- The Sweet and JetBrains Compose runners launch equivalent Drawing Kitchen Sink content.
- Screenshots are captured at the same window/client size, scroll position, theme, title, and section list.
- Sweet renders every supported drawing section recognizably the same as JetBrains Compose Desktop.
- Any remaining differences are explicitly classified as expected SWT GC limitations, not accidental regressions.
- Root validation still uses `./gradlew test` per `CLAUDE.md`; UI tests run under a display or `xvfb-run`.

## 2. Non-goals

- Pixel-perfect text rendering across SWT/GTK and Skia.
- Full `BlendMode`, radial/sweep gradient, `ColorMatrix`, or Skia-level fidelity on SWT GC.
- Owner-drawing native SWT widgets.
- Weakening existing tests to match current Sweet behavior. Per `CLAUDE.md`, do not change failing test assertions without approval.

## 3. Current Evidence

The provided screenshots are not a clean renderer comparison yet:

- `tmp/Screenshot-mpp.png` is `818x998`; `tmp/Screenshot-sweet.png` is `794x981`.
- Trimmed visible content also differs: roughly `800x979+9+9` vs `780x966+7+8`.
- Sweet uses the default window title `"Untitled"` because `sample-sweet/.../DrawingKitchenSink.kt` calls `Window(onCloseRequest = ::exitApplication)` with no title.
- The JetBrains runner title is stale: `sample-jetbrains/.../JetbrainsRunners.kt` still says `"Sweet - TodoMVC (JetBrains Compose)"` while launching `DrawingKitchenSinkAppMPP()`.
- The two apps do not render identical source:
  - Sweet renders `sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample/DrawingKitchenSink.kt`.
  - JetBrains Compose renders `sample-jetbrains/src/main/kotlin/io/github/ddsimoes/sweet/sample/mpp/DrawingKitchenSinkMPP.kt`.
  - The MPP fork omits section 10 (`Image - ContentScale + Alignment`) as unsupported.
- Sweet's SWT backend accepts several Compose-like drawing parameters but ignores or approximates them:
  - Stroke cap, join, miter, and dash path effects are not mapped to `GC` line state.
  - Non-`SrcOver` blend modes intentionally degrade to `SrcOver`.
  - Radial and sweep gradients degrade to solid colors.
  - Image color filters are ignored.
- Sweet's gradient path likely leaks paint state between draw calls:
  - `SwtCanvasBackend.drawWithLinearGradient()` sets `gc.backgroundPattern` or `gc.foregroundPattern`, disposes the `Pattern`, but does not restore or clear the pattern fields.
  - Later solid operations only set `gc.background`/`gc.foreground`, so stale pattern state can contaminate following drawing.
- Sweet's sample-only section 10 uses `BrushPainter`, whose intrinsic size is `Size.Unspecified`. `Image(painter)` currently feeds that `NaN` size into `ContentScale.computeSize`, making that section unreliable even before comparing with MPP.

## 4. Working Principles

1. First make the comparison fair.
2. Separate "different input" from "different renderer."
3. Fix concrete Sweet bugs before classifying gaps as SWT limitations.
4. Prefer small, focused AutoSWT tests over broad screenshot assertions.
5. Keep expected degradation explicit in docs and debug logs.

## 5. Phase 0 - Baseline Hygiene

### 0.1 Normalize window metadata

Update both runners to use the same title:

```kotlin
title = "Sweet Compose - Drawing Kitchen Sink"
```

Targets:

- `sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample/DrawingKitchenSink.kt`
- `sample-jetbrains/src/main/kotlin/io/github/ddsimoes/sweet/sample/mpp/JetbrainsRunners.kt`

Acceptance:

- Window title no longer appears as `"Untitled"` for Sweet.
- JetBrains runner no longer says TodoMVC.

### 0.2 Normalize window size

Set the same initial window size in both runners. Prefer a size tall enough to show the same top sections without relying on manual resizing, for example `800.dp x 1000.dp`.

Targets:

- Sweet `Window(..., state = rememberWindowState(width = 800.dp, height = 1000.dp), ...)`
- JetBrains Compose `Window(..., state = rememberWindowState(width = 800.dp, height = 1000.dp), ...)`

Acceptance:

- Fresh screenshots have matching outer dimensions or documented platform chrome differences.
- Client/content area is measured and logged if outer dimensions still differ.

### 0.3 Normalize capture workflow

Add or document one repeatable capture command/test for Sweet and one for JetBrains Compose:

- Same display/DPI/theme.
- Same scroll position: top of the scroll container.
- Same time delay after launch, especially while `ReactiveRedrawSection` is animating.
- Same screenshot target: either full window or client area, but not mixed.

Acceptance:

- Re-running capture produces stable dimensions and stable top-section ordering.

## 6. Phase 1 - Make Both Apps Render Equivalent Content

### 1.1 Choose a canonical content strategy

Preferred strategy:

- Keep one canonical section list and section labels.
- Allow tiny API adapters only where Sweet and JetBrains Compose package names differ.
- Avoid maintaining two independent kitchen sink files that can silently drift.

Pragmatic short-term strategy:

- Keep the two files for now, but add a parity checklist at the top of both files.
- Every section present in Sweet must either exist in MPP or be marked Sweet-only with a reason.
- Every section omitted in MPP must be documented in this render parity plan.

Acceptance:

- A source diff between the two files contains expected import/API differences only.
- Section numbering, labels, and order match.

### 1.2 Restore or intentionally remove section 10

Current mismatch:

- Sweet includes `ImageScaleAlignSection()`.
- MPP has `// removed as unsupported`.

Options:

- **Preferred:** Implement a JetBrains-compatible section 10 using a real painter/image resource and keep the same section in both runners.
- **Short-term:** Temporarily hide section 10 from Sweet as well while comparing sections 1-9 and 11-13.

Do not leave the current state where only Sweet has section 10 and the later sections shift vertically.

Acceptance:

- Sections after ColorFilter have the same vertical position in both screenshots, except for renderer differences.

### 1.3 Fix Sweet's section 10 painter sizing before using it as evidence

`BrushPainter.intrinsicSize` is `Size.Unspecified`, and `Image(painter)` currently computes scale from that value. Fix one of these:

- In `Image(painter)`, if `painter.intrinsicSize.isUnspecified`, use the destination canvas size as the source size.
- Or make the demo painter a test painter with an explicit intrinsic size.

Targets:

- `sweet/src/main/kotlin/androidx/compose/foundation/Image.kt`
- `sweet/src/main/kotlin/androidx/compose/ui/graphics/Painter.kt`
- `sample-sweet/.../DrawingKitchenSink.kt` only if the sample needs an explicit-size painter.

Acceptance:

- `Image(BrushPainter(...), contentScale = ...)` does not produce `NaN`, zero, or invisible output.
- Add a focused test for unspecified-intrinsic painter behavior if implementation changes.

## 7. Phase 2 - Fix Sweet GC Paint State

This phase addresses the most likely concrete rendering bug.

### 2.1 Restore pattern state after gradient drawing

Problem:

- `drawWithLinearGradient()` sets `gc.backgroundPattern` / `gc.foregroundPattern`.
- The `Pattern` is disposed after drawing.
- The GC pattern fields are not restored or cleared.

Fix:

- Capture old `backgroundPattern` / `foregroundPattern` before setting a new pattern.
- Restore the old pattern in `finally`.
- If SWT does not allow reading pattern fields on this version, explicitly set the used pattern field to `null` before disposing.

Targets:

- `sweet/src/main/kotlin/io/github/ddsimoes/sweet/drawing/SwtCanvasBackend.kt`

Acceptance:

- A solid rect drawn after a gradient uses its solid color, not a stale gradient.
- Add a small AutoSWT visual/probe test:
  - Draw a gradient rectangle.
  - Draw a solid red rectangle after it.
  - Assert a pixel inside the second rectangle is red-ish and not gradient-derived.

### 2.2 Restore alpha and line state consistently

Problem:

- `configurePaint()` mutates `gc.alpha`.
- Stroke operations mutate `gc.lineWidth`.
- Scoped transforms save/restore some state, but individual draw calls do not always restore per-operation paint state.

Fix:

- Add a private helper around every primitive draw:

```kotlin
private inline fun withPaintState(block: () -> Unit)
```

It should preserve:

- foreground
- background
- foregroundPattern
- backgroundPattern
- lineWidth
- lineCap
- lineJoin
- lineDash
- alpha

Acceptance:

- Drawing one primitive cannot change the style of the next unrelated primitive.
- Tests cover at least alpha reset and line dash reset.

## 8. Phase 3 - Implement Stroke Fidelity on SWT GC

### 3.1 Map `StrokeCap`

Map Compose stroke caps to SWT:

- `StrokeCap.Butt` -> `SWT.CAP_FLAT`
- `StrokeCap.Round` -> `SWT.CAP_ROUND`
- `StrokeCap.Square` -> `SWT.CAP_SQUARE`

Apply to:

- `drawLine`
- `drawPoints`
- stroked `drawPath`
- stroked rect/roundRect/oval/arc where SWT honors line cap

Acceptance:

- The scatter points in section 1 appear round when `cap = StrokeCap.Round`.
- Vector icon strokes have rounded ends where specified.

### 3.2 Map `StrokeJoin`

Map Compose joins to SWT:

- `StrokeJoin.Miter` -> `SWT.JOIN_MITER`
- `StrokeJoin.Round` -> `SWT.JOIN_ROUND`
- `StrokeJoin.Bevel` -> `SWT.JOIN_BEVEL`

Apply to:

- stroked paths
- stroked rectangles/round rectangles where SWT honors joins

Acceptance:

- Section 3's "Round-join stroked rect" differs from miter joins in the expected way.
- Vector check/close/menu icons render with round joins.

### 3.3 Map dash `PathEffect`

For `PathEffect.DashPathEffect(intervals, phase)`:

- Use `gc.lineDash = intervals.map { it.toInt().coerceAtLeast(1) }.toIntArray()`.
- SWT may not support phase. If phase cannot be honored, log once and ignore phase.
- Restore `lineDash` after drawing.

Acceptance:

- Section 3's dashed rectangle is visibly dashed in Sweet.
- A following solid stroke is not dashed.

### 3.4 Preserve unsupported stroke details honestly

If `miter` cannot be faithfully mapped, document it and log once when the requested value is not the default.

Acceptance:

- Unsupported stroke properties do not silently pretend to work.

## 9. Phase 4 - Verify Core Primitive Sections

Work section by section. Do not use the whole screenshot as the first test.

### Section 1 - Solid colors

Validate:

- `drawRect` fill and stroke
- alpha on green rect
- fill/stroke circles
- line width
- round point cap

Expected remaining differences:

- Antialiasing and font metrics.

### Section 2 - Gradient brushes

Validate:

- Linear gradient first/last colors are positioned correctly.
- Horizontal/vertical gradient directions are correct.
- SolidColor brush works.
- Rounded path fill works.

Expected remaining differences:

- SWT `Pattern` may only use first and last stops; multi-stop gradients may be approximate.

### Section 3 - Stroke styles

Validate:

- Line width
- Dash path effect
- Round join
- State reset after dash

### Section 4 - Shapes

Validate:

- Round rect fill/stroke
- Oval
- Pie arc vs stroked arc
- Circle fill

Expected remaining differences:

- Arc winding/angle details may differ between Skia and SWT. If visible, add a targeted note and test.

### Section 5 - Paths

Validate:

- Cubic and quadratic Bezier conversion
- `arcTo`
- closed filled polygon with alpha

Expected remaining differences:

- SWT path arc semantics may not match Compose exactly when `forceMoveTo` matters.

### Section 6 - Transforms

Validate:

- Translation
- Scale with pivot
- Rotate with pivot
- Nested transforms
- Inset clipping

Acceptance:

- Every transform is scoped and restored.

### Section 7 - Clipping

Validate:

- Scoped `clipRect`
- Scoped `clipPath`
- Transform plus clip interactions
- Clip restoration after block

Acceptance:

- Later drawing is not clipped by earlier scoped clip calls.

## 10. Phase 5 - Reconcile Degraded Features

These features are known GC limitations. The plan is to make them consistent, documented, and visible in the sample.

### 5.1 Blend modes

Current behavior:

- Non-`SrcOver` modes log and degrade to `SrcOver`.

Decision:

- Keep degradation for now.
- Update sample label if needed so the comparison does not imply pixel parity for modes SWT cannot do.
- Add a test that non-`SrcOver` does not crash and produces deterministic fallback.

### 5.2 Color filters

Current behavior:

- Solid draw color filters are best-effort.
- Image color filters are ignored.

Decision:

- For rectangle color filters, either match Compose tint better or document the current approximation.
- For image/vector tint, ensure vector icon tint is handled through stroke/fill color selection rather than image color filters.

### 5.3 Radial/sweep gradients

The current kitchen sink section uses linear gradients only. Keep radial/sweep out of this parity gate unless the sample grows to include them.

## 11. Phase 6 - Vectors and Icons

### 6.1 Fix tint semantics for stroked vectors

Current `Icon(imageVector, tint)` only overrides fill when `node.fill.isSpecified`. The kitchen sink icons use transparent fill and black stroke. Tint should also apply to stroke when a tint is specified.

Fix:

- If `tint.isSpecified`, apply it to fill and stroke paints that are specified.
- Treat transparent fill with `fillAlpha = 0f` as no visible fill.

Target:

- `sweet/src/main/kotlin/androidx/compose/foundation/Icon.kt`

Acceptance:

- Add/back/check/close/menu icons visibly use requested tint.

### 6.2 Scale stroke width with viewport

Current vector rendering scales path coordinates, but stroke width is passed through unchanged. Compose vector stroke width is in viewport units and should scale with the rendered size.

Fix:

- Scale stroke width by an appropriate factor. For uniform icons, `min(scaleX, scaleY)` is acceptable.

Acceptance:

- 16/24/32/40 dp size variants have proportional stroke thickness.

### 6.3 Continue using existing path parser only if sufficient

The kitchen sink icons use simple move/line commands. Do not rewrite the vector parser for this comparison unless a failing case requires it.

## 12. Phase 7 - Container Decoration

Validate section 12 after the primitive fixes:

- Rounded background shape
- CircleShape gradient background
- Border width in dp
- Clip to CircleShape

Potential fixes:

- Ensure `background(brush, shape)` draws only on container composites.
- Ensure shape outlines use the actual measured size.
- Ensure `clip(shape)` owns and disposes SWT `Region` handles correctly.

Acceptance:

- Section 12 is visually recognizable.
- No native handle leaks from repeated layout/clip updates.

## 13. Phase 8 - Reactive Redraw

The screenshots are static, but section 13 can still diverge depending on capture time.

### 8.1 Make screenshot capture deterministic

Options:

- Add a sample flag to start `spinning = false` during screenshot tests.
- Or capture at a known delay and accept that hue differs.

Preferred:

- Add a test-only/composable parameter:

```kotlin
fun DrawingKitchenSinkApp(animated: Boolean = true)
```

Then screenshot tests can call `DrawingKitchenSinkApp(animated = false)`.

Acceptance:

- Screenshot comparisons do not fail because animation advanced by a few frames.

### 8.2 Verify state-triggered redraw

Add a focused AutoSWT test:

- Render a Canvas that reads mutable state.
- Mutate the state.
- Assert paint count or screenshot changes.

This belongs to drawing foundation correctness, not just kitchen sink parity.

## 14. Verification Plan

### 14.1 Compile and unit validation

Run:

```bash
./gradlew test --continue
```

If no display exists:

```bash
xvfb-run -a ./gradlew test --continue
```

Acceptance:

- Root test gate passes, including `:sample-sweet`.

### 14.2 Focused visual tests

Add tests for:

- Gradient followed by solid paint state reset.
- Dash followed by solid stroke state reset.
- Round cap/round join mapping.
- `Image(painter)` with unspecified intrinsic size.
- Vector icon tint and scaled stroke width.
- Scoped clip restore.

### 14.3 Screenshot parity check

After phases 0-8:

1. Capture Sweet and MPP screenshots with the normalized workflow.
2. Compare dimensions and trimmed content bounds.
3. Manually inspect sections 1-13.
4. Keep a short result table in this doc or a follow-up report:

| Section | Status | Notes |
|---------|--------|-------|
| 1 Solid colors | TBD | |
| 2 Gradient brushes | TBD | |
| 3 Stroke styles | TBD | |
| 4 Shapes | TBD | |
| 5 Paths | TBD | |
| 6 Transforms | TBD | |
| 7 Clipping | TBD | |
| 8 BlendMode | Expected degraded | Non-SrcOver on SWT GC |
| 9 ColorFilter | TBD | |
| 10 Image | TBD | Must be present in both or excluded in both |
| 11 Vector Icons | TBD | |
| 12 Container decoration | TBD | |
| 13 Reactive redraw | TBD | Disable animation for screenshots |

## 15. Implementation Order

Recommended order:

1. Fix titles and window sizes.
2. Decide section 10 parity strategy.
3. Fix `Image(painter)` unspecified intrinsic size if section 10 remains.
4. Fix GC pattern restoration.
5. Add stroke cap/join/dash mapping and state restore.
6. Verify sections 1-7 with focused tests.
7. Fix vector tint/stroke scaling.
8. Validate container decoration.
9. Make reactive redraw deterministic for screenshots.
10. Re-capture and classify any remaining differences.

## 16. Risks

| Risk | Mitigation |
|------|------------|
| SWT GC cannot match Skia for some primitives | Mark expected degradation in the sample and docs |
| Pattern/line state restore differs by SWT version | Prefer helper APIs that restore defensively; add regression tests |
| Screenshot comparisons are flaky due to fonts/DPI/theme | Use focused tests first; keep whole screenshots as review artifacts |
| Section 10 remains source-divergent | Either implement it in both or exclude it in both until fixed |
| Existing dirty worktree contains in-progress drawing work | Keep changes small and review file diffs carefully before edits |

## 17. Definition of Done

This plan is complete when:

- Sweet and MPP runners render the same section list at the same initial size.
- The stale TodoMVC and Untitled title mismatch is gone.
- GC paint state is restored across primitives.
- Stroke cap, join, and dash are mapped or explicitly documented when unsupported.
- Section 10 either works in both runners or is excluded from both parity screenshots.
- Vector icons tint and scale correctly enough for the kitchen sink.
- Root `./gradlew test --continue` passes under an appropriate display.
- A final screenshot pair is attached or saved with dimensions and known expected differences recorded.
