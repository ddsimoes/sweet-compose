# Drawing Kitchen Sink — API Compatibility Plan

**Status:** Partially addressed — Alignment companions removed, CubicBezierEasing fixed, KitchenSink3 compiles against both runtimes. Remaining gap: scoped clip, BoxScope.align, painterResource bridge | **Created:** 2026-06-08 | **Depends on:** `drawing-foundation.md` (D0–D3)

## 1. Problem Statement

A developer ported `DrawingKitchenSink.kt` to real Compose MPP. The port uses only
standard Compose APIs — and it works correctly under JetBrains Compose Desktop.

When the same file is compiled against Sweet, it fails with multiple compiler errors.
These are **not** feature gaps where Sweet deliberately doesn't support something.
They are places where Sweet *claims* to implement a Compose API (and owns the matching
package path) but uses the wrong types, signatures, parameter names, or missing
companion members — so code written for real Compose breaks.

**The rule is:** Sweet may be a subset, but the subset must be faithful. Any API
Sweet exposes under an `androidx.compose.*` package MUST match the real Compose
signature exactly. Optional/default parameters, method order, receiver types,
companion members — all of it.

## 2. Audit of the Sample (reference Compose MPP version)

The sample at `sample-sweet/src/main/kotlin/…/DrawingKitchenSink.kt` exercises
these drawing APIs. Lines noted are where Sweet diverges.

### 2.1 Solid Colors (lines 96–102) — `drawPoints` + `PointMode`

```kotlin
drawPoints(
    points = listOf(Offset(320f, 16f), …),
    pointMode = PointMode.Points,      // ❌ Sweet has no PointMode enum
    color = Color(0xFFE91E63),
    strokeWidth = 4f,                  // ❌ Sweet calls this `pointSize`
    cap = StrokeCap.Round,             // ❌ Sweet has no `cap` param
)
```

**Real Compose `DrawScope.drawPoints`** signature:
```kotlin
fun drawPoints(
    points: List<Offset>,
    pointMode: PointMode,
    color: Color,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap = StrokeCap.Butt,
    pathEffect: PathEffect? = null,
    alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = BlendMode.SrcOver,
)
```

**Sweet's current signature** (wrong):
```kotlin
fun drawPoints(
    points: List<Offset>,
    color: Color,
    pointSize: Float = 2f,
    alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = BlendMode.SrcOver,
)
```

**What must change:** Add `PointMode` enum (`Points`, `Lines`, `Polygon`). Replace
`pointSize` with `strokeWidth`. Add `cap` and `pathEffect` params. Reorder to match.

### 2.2 Gradient Brushes (line 125) — `CornerRadius` type

```kotlin
val rr = RoundRect(Rect(280f, 42f, 330f, 72f), CornerRadius(12f, 12f))
//                                               ❌ Sweet has no CornerRadius type
```

**Real Compose:** `CornerRadius` is a data class:
```kotlin
data class CornerRadius(val x: Float, val y: Float)
companion object { fun CornerRadius(radius: Float) = CornerRadius(radius, radius) }
```

**Sweet's current `RoundRect`:** uses 4 individual `Float` fields
(`topLeftCornerRadius`, `topRightCornerRadius`, …). There is no `CornerRadius` type.

**What must change:** Add `CornerRadius` to `ui.geometry`. Add `RoundRect(rect, CornerRadius)`
constructor overload.

### 2.3 Shapes (line 156) — `drawRoundRect` takes `CornerRadius`, not `Float`

```kotlin
drawRoundRect(Color(0xFF673AB7), Offset(8f, 8f), Size(60f, 30f),
    cornerRadius = CornerRadius(12f, 12f))
//                ❌ Sweet takes `cornerRadius: Float`
```

**Real Compose:**
```kotlin
fun drawRoundRect(
    color: Color,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    cornerRadius: CornerRadius = CornerRadius.Zero,  // ← CornerRadius, not Float
    alpha: Float = DefaultAlpha,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = BlendMode.SrcOver,
)
```

**What must change:** Change `cornerRadius` param type from `Float` to `CornerRadius`.
Update `SweetDrawScope` and `SweetCanvas` accordingly.
Add `drawRoundRect(brush, topLeft, size, cornerRadius: CornerRadius, …)` overload too.

### 2.4 Transforms (lines 194, 200) — `scale`/`rotate` missing `pivot`

```kotlin
scale(scaleX = 1.5f, scaleY = 1.5f, pivot = Offset.Zero)
//                                  ❌ no `pivot` param
rotate(degrees = 30f, pivot = Offset.Zero)
//                     ❌ no `pivot` param
```

**Real Compose:**
```kotlin
fun scale(scaleX: Float, scaleY: Float = scaleX, pivot: Offset = center)
fun rotate(degrees: Float, pivot: Offset = center)
```

**What must change:** Add `pivot` param with default `center` to both methods in
`DrawScope` interface and `SweetDrawScope` implementation. The `SweetCanvas` interface
and `SwtCanvasBackend` must handle pivot via translate-pivot → scale/rotate → translate-back.

### 2.5 Clipping (lines 226, 236, 244) — scoped `clipRect`/`clipPath`

```kotlin
clipRect(left = 8f, top = 8f, right = 80f, bottom = 60f) {
    drawRect(…)
    drawCircle(…)
}
// ❌ Sweet's clipRect(l,t,r,b) is not scoped — it clips and never restores

clipPath(roundedClip) {
    drawRect(…)
}
// ❌ Same — not scoped
```

**Real Compose:**
```kotlin
inline fun DrawScope.clipRect(
    left: Float, top: Float, right: Float, bottom: Float,
    clipOp: ClipOp = ClipOp.Intersect,
    block: DrawScope.() -> Unit,
)
inline fun DrawScope.clipPath(
    path: Path,
    clipOp: ClipOp = ClipOp.Intersect,
    block: DrawScope.() -> Unit,
)
```

**What must change:** Add scoped overloads. The non-scoped versions (`clipRect(rect)`,
`clipPath(path)`) remain for imperative use. Scoped versions wrap with `withTransform`
or save/restore + clip + block + restore.

### 2.6 Container Decoration (line 349) — `Modifier.border` takes `Dp`, not `Float`

```kotlin
.border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
//       ^^^^ ❌ Sweet takes `width: Float` (pixels)
```

**Real Compose:**
```kotlin
fun Modifier.border(width: Dp, brush: Brush, shape: Shape): Modifier
fun Modifier.border(width: Dp, color: Color, shape: Shape): Modifier
```

**What must change:** Add `Dp` overloads that convert to px internally. Keep the
existing `Float` overload (marked `internal` or deprecated) for the internal
`drawShape` helper.

### 2.7 Image (line 305) — `alignment` uses `Alignment`, not `ContentAlignment`

```kotlin
Image(demoPainter, "None", Modifier.fillMaxSize(),
    contentScale = ContentScale.None,
    alignment = Alignment.BottomEnd)
//            ^^^^^^^^^^^^^^^^^^^
// ❌ Sweet's `Image` takes `ContentAlignment`, not `Alignment`
// ❌ `Alignment.BottomEnd` doesn't exist on Sweet's `Alignment` (it's on `Alignment.Combined`)
```

**Real Compose:**
```kotlin
@Composable
fun Image(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
)
```

**What must change:** Sweet's `Alignment` type is currently a 1D sealed class (for layout).
Real Compose's `Alignment` is a 2D interface with companion values. This is the
deepest structural issue — see §3.1 for the full plan.

Short-term: add `Alignment` companion values (`BottomStart`, `BottomEnd`, etc.) that
delegate to `ContentAlignment`. Longer-term: align the `Alignment` type hierarchy with
Compose MPP.

### 2.8 Reactive Redraw (line 389) — `BoxScope.align` missing

```kotlin
Text("t=…", modifier = Modifier.align(Alignment.BottomStart).padding(…))
//                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// ❌ Sweet has no `BoxScope` with per-child `Modifier.align`
```

**Real Compose:** `BoxScope` provides `Modifier.align(alignment: Alignment)` for
per-child 2D alignment override.

**What must change:** Add `BoxScope` interface with `Modifier.align(alignment: Alignment)`.
Update `Box` composable to provide `BoxScope` as receiver scope. This depends on the
`Alignment` type fix.

### 2.9 Vector Icons (lines 442–461) — `PathBuilder` + `ImageVector.Builder.path()`

```kotlin
ImageVector.Builder(…).apply {
    path(
        fill = SolidColor(Color.Transparent),
        fillAlpha = 0f,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.25f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        pathBlock()  // PathBuilder.() -> Unit
    }
}.build()
```

**Real Compose:**
```kotlin
// Top-level extension on ImageVector.Builder:
fun ImageVector.Builder.path(
    fill: Brush = SolidColor(Color.Black),
    fillAlpha: Float = 1.0f,
    stroke: Brush = SolidColor(Color.Transparent),
    strokeAlpha: Float = 1.0f,
    strokeLineWidth: Float = 1.0f,
    strokeLineCap: StrokeCap = StrokeCap.Butt,
    strokeLineJoin: StrokeJoin = StrokeJoin.Miter,
    strokeLineMiter: Float = 4.0f,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector.Builder

// PathBuilder class:
class PathBuilder {
    fun moveTo(x: Float, y: Float): PathBuilder
    fun lineTo(x: Float, y: Float): PathBuilder
    fun horizontalLineTo(x: Float): PathBuilder
    fun verticalLineTo(y: Float): PathBuilder
    fun close(): PathBuilder
    fun quadraticBezierTo(…): PathBuilder
    fun cubicTo(…): PathBuilder
    fun arcTo(…): PathBuilder
    fun relativeMoveTo(…): PathBuilder
    // … etc
}
```

**Sweet's current API:** `Group.path(pathData: String, fill, stroke, …)` takes a raw
SVG path data string. There is no `PathBuilder` class and no `path()` extension on
`ImageVector.Builder`. The entire vector DSL is different.

**What must change:** Create `PathBuilder` class. Add top-level
`fun ImageVector.Builder.path(…)` extension that creates a `PathBuilder`, invokes the
block, serializes to SVG path data, and adds it to a default root group. This wraps
the existing SVG-path-data rendering without changing the rendering internals.

### 2.10 Image (line 291) — `painterResource` from JetBrains Resources

```kotlin
val demoPainter = painterResource(Res.drawable.compose_multiplatform)
```

This is a JetBrains Compose Resources plugin API. Sweet has no integration with this.
For the sample to work with Sweet, the image section either needs an alternate image
source (e.g., `rememberImageBitmap("path/to/image.png")`) or Sweet needs a bridge.

> **Decision:** This is an external integration gap, not an API bug. The plan
> addresses it as a deferred item (see §4). The immediate fix is to provide a
> Sweet-compatible image path in the sample.

## 3. Root-Cause API Issues (Sweet-side)

### 3.1 `Alignment` type is wrong (affects §§ 2.7, 2.8, 2.9)

Sweet's `Alignment` is `sealed class Alignment` with 1D subtypes for layout
(`Horizontal.Start/CenterHorizontally/End`, `Vertical.Top/CenterVertically/Bottom`,
`Combined`). This was originally built for the layout system before `ContentAlignment`
was added for the drawing layer.

Real Compose's `Alignment` is a 2D `interface` with companion object values:

```kotlin
interface Alignment {
    fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset
    companion object {
        val TopStart: Alignment
        val TopCenter: Alignment
        val TopEnd: Alignment
        val CenterStart: Alignment
        val Center: Alignment
        val CenterEnd: Alignment
        val BottomStart: Alignment
        val BottomEnd: Alignment
        val BottomCenter: Alignment
        // 1D variants:
        val CenterHorizontally: Alignment.Horizontal
        val CenterVertically: Alignment.Vertical
    }
    interface Horizontal : Alignment { … }
    interface Vertical : Alignment { … }
}
```

**Impact:** Until this is fixed, every use of `Alignment.Center`, `Alignment.BottomEnd`,
`Alignment.CenterHorizontally` in Compose MPP code fails against Sweet.

`ContentAlignment` was added as a separate 2D type for `Image` placement, but it cannot
substitute for `Alignment` — they must be the same type because:
- `Image(alignment: Alignment)` in real Compose takes `Alignment`
- `BoxScope.align(alignment)` takes `Alignment`
- `Modifier.align(alignment)` in Column/Row takes `Alignment.Horizontal`/`Alignment.Vertical` subtypes

**Required fix:** Make `Alignment` the 2D interface. `ContentAlignment` becomes either
`Alignment` itself or a deprecated alias. The 1D layout subtypes become nested
interfaces (`Alignment.Horizontal`, `Alignment.Vertical`). The existing 1D `Alignment`
sealed class needs to coexist temporarily via deprecation.

This is the largest single change and blocks several other fixes. It touches:
- `ui/Alignment.kt` — full rewrite
- `ui/graphics/ContentAlignment.kt` — deprecate/merge
- `foundation/Image.kt` — change param type from `ContentAlignment` to `Alignment`
- `foundation/layout/layout.kt` — `Box`, `Column`, `Row` alignment params
- `ui/graphics/vector/ImageVector.kt` — icon rendering alignment
- `foundation/foundation.kt` — drawPhase alignment usage
- All tests referencing `Alignment.Combined`, `ContentAlignment.*`

### 3.2 DrawScope API gaps (affects §§ 2.1, 2.3, 2.4, 2.5)

Several `DrawScope` methods have diverged from the real Compose signature during the
D0–D3 implementation. These are straightforward to fix:

| Method | Missing/Divergent | Fix |
|--------|-------------------|-----|
| `drawPoints` | No `PointMode`, wrong param names | Add `PointMode` enum, rename `pointSize`→`strokeWidth`, add `cap`/`pathEffect` |
| `drawRoundRect` | `cornerRadius: Float`, not `CornerRadius` | Change type, add brush overload with `CornerRadius` |
| `scale` | No `pivot: Offset` | Add defaulted `pivot = center` param |
| `rotate` | No `pivot: Offset` | Add defaulted `pivot = center` param |
| `clipRect` scoped | No scoped overload with block | Add `inline fun clipRect(l,t,r,b, clipOp, block)` |
| `clipPath` scoped | No scoped overload with block | Add `inline fun clipPath(path, clipOp, block)` |

Files touched:
- `ui/graphics/Canvas.kt` — `DrawScope` interface additions
- `io/…/drawing/SweetDrawScope.kt` — implementation updates
- `io/…/drawing/SweetCanvas.kt` — backend additions (pivot, PointMode rendering)
- `io/…/drawing/SwtCanvasBackend.kt` — GC-level rendering for new params
- `ui/geometry/geometry.kt` — `CornerRadius` type, `PointMode` enum (or `ui.graphics`)

### 3.3 ImageVector builder API gap (affects § 2.9)

Missing `PathBuilder` class and `ImageVector.Builder.path()` extension. The internal
SVG path data string format already works for rendering — `PathBuilder` just needs to
build that string from the DSL.

Files touched:
- `ui/graphics/vector/ImageVector.kt` — add `PathBuilder`, add `path()` extension
- `ui/graphics/vector/` — new file `PathBuilder.kt` (or inline in ImageVector.kt)

### 3.4 Modifier API gaps (affects §§ 2.6, 2.8)

- `Modifier.border(width: Dp, …)` — missing the `Dp` overload
- `BoxScope` — missing entirely; no per-child alignment in `Box`

Files touched:
- `foundation/foundation.kt` — `Dp` overloads for border
- `foundation/layout/layout.kt` — `BoxScope` interface, `Box` signature update

## 4. Phased Implementation Plan

### Phase A — Fix API signatures (no behavioral change, makes sample compile)

| # | Change | Est. complexity | Depends on |
|---|--------|-----------------|------------|
| A1 | Add `PointMode` enum; fix `drawPoints` signature | Small | — |
| A2 | Add `CornerRadius` to `ui.geometry`; add `RoundRect(CornerRadius)` ctor | Small | — |
| A3 | Change `drawRoundRect` `cornerRadius` from `Float` → `CornerRadius` | Small | A2 |
| A4 | Add `pivot` param to `DrawScope.scale` / `rotate` | Medium | — |
| A5 | Add scoped `clipRect(l,t,r,b, block)` / `clipPath(path, block)` | Small | — |
| A6 | Add `PathBuilder` class + `ImageVector.Builder.path()` extension | Medium | — |
| A7 | Add `Modifier.border(width: Dp, …)` overloads | Small | — |
| A8 | Fix `Alignment` type (2D interface) + `BoxScope` | **Large** | — |

**Phase A gates:**
- `sample-sweet` compiles against Sweet
- All existing Sweet tests continue to pass

### Phase B — Make the visual output correct

After Phase A, the sample compiles but visual behavior may be degraded for new params:

| # | Change | Notes |
|---|--------|-------|
| B1 | Implement `PointMode.Lines`/`Polygon` in `SwtCanvasBackend` | Lines = GC.drawPolyline; Polygon = GC.drawPolygon |
| B2 | Implement pivot-aware `scale`/`rotate` in backend | translate→op→translate-back pattern |
| B3 | Wire `cap` on `drawPoints` | Already supported via `Stroke` in `Paint`; ensure GC uses it |
| B4 | Wire `pathEffect` on `drawPoints` | If already in `Stroke`, may just work |

**Phase B gates:**
- Visual output on Sweet matches Compose MPP for all 13 sections
- No regressions in existing AutoSWT draw tests

### Phase C — Sample-specific adjustments

| # | Change |
|---|--------|
| C1 | Replace `painterResource` with `rememberImageBitmap` (or a Sweet resource bridge) |
| C2 | Remove MPP-specific imports (`composemppsandbox.*`, `org.jetbrains.compose.resources.*`) |
| C3 | Add `Color.Companion.fromDegrees` to Sweet's `ui.graphics.Color` (or keep as sample helper) |
| C4 | Verify the sample runs under Sweet via AutoSWT test |

### Phase D — Deferred (future work)

| # | Item |
|---|------|
| D1 | JetBrains Compose Resources integration (`painterResource` bridge) |
| D2 | `Stroke.HairlineWidth` constant |
| D3 | `ClipOp` enum (Intersect/Difference) — needed for full `clipRect`/`clipPath` fidelity |

## 5. Files Affected (summary)

| File | Phase | Changes |
|------|-------|---------|
| `ui/graphics/Canvas.kt` (DrawScope) | A1,A3,A4,A5 | `drawPoints`, `drawRoundRect`, `scale`, `rotate`, scoped clips |
| `ui/graphics/graphics.kt` | A1 | `PointMode` enum, `Stroke.HairlineWidth` |
| `ui/geometry/geometry.kt` | A2 | `CornerRadius`, `RoundRect` overload |
| `ui/graphics/vector/ImageVector.kt` | A6 | `PathBuilder`, `Builder.path()` |
| `ui/Alignment.kt` | A8 | Rewrite to 2D interface |
| `ui/graphics/ContentAlignment.kt` | A8 | Deprecate or merge |
| `io/…/drawing/SweetDrawScope.kt` | A1,A3,A4,A5,B1-B4 | All implementation updates |
| `io/…/drawing/SweetCanvas.kt` | A1,A3,A4,B1-B4 | Backend interface updates |
| `io/…/drawing/SwtCanvasBackend.kt` | A1,A3,A4,B1-B4 | GC rendering updates |
| `foundation/Image.kt` | A8 | `alignment: ContentAlignment` → `alignment: Alignment` |
| `foundation/foundation.kt` | A7 | `border(Dp, …)` overloads |
| `foundation/layout/layout.kt` | A8 | `BoxScope`, `Box` signature |
| `sample-sweet/…/DrawingKitchenSink.kt` | C1-C4 | Sweet-compatible adjustments |
| `foundation/Icon.kt` | A8 | `ContentAlignment` → `Alignment` if used |
| `material3/Icon.kt` | A8 | `Alignment.Combined.Center` → `Alignment.Center` |

## 6. Verification Strategy

1. **Compile gate:** `sample-sweet` must compile against `:sweet` with zero errors.
2. **Test gate:** `./gradlew test --continue` (all modules) must pass. No test
   assertions may be weakened.
3. **Visual gate:** Run the Kitchen Sink sample under Sweet and visually compare
   against JetBrains Compose output. All 13 sections must render recognizably.
4. **API shape tests:** Add compile-only tests that import canonical
   `androidx.compose.*` packages and exercise the fixed signatures — these catch
   signature drift before behavior tests.

## 7. Risk Notes

- **`Alignment` rewrite (A8)** is the riskiest change. It touches the layout system's
  type hierarchy. A dual-API approach (keep old `Alignment.Combined` as deprecated
  alias while introducing the 2D interface) minimizes blast radius during migration.
- **`PointMode` on GC backend:** `GC.drawPolyline` maps to `PointMode.Lines`;
  `GC.drawPolygon` maps to `PointMode.Polygon`. `PointMode.Points` is already the
  current behavior (individual points as small strokes).
- **`CornerRadius`:** Adding it doesn't break existing users since the old `Float`
  corner radius on `RoundRect` fields stays. A `CornerRadius` constructor overload
  is purely additive.
