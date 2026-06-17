# Sweet Compose — Drawing Guide

> **Status:** D0–D3 complete (2026-06-08) | **See:** `docs/roadmap/drawing-foundation.md`

Sweet Compose provides a faithful Compose drawing API on top of SWT's `GC` backend. This guide covers the supported surface — what works, how degraded modes behave, and what is out of scope.

## Quick start

```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    // DrawScope includes dp.toPx() / sp.toPx() — it extends Density
    val inset = 16.dp.toPx()

    // Solid fills
    drawRect(Color.Red, topLeft = Offset(inset, inset), size = Size(100f, 80f))
    drawRoundRect(Color.Blue, topLeft = Offset(inset, inset + 100f), size = Size(100f, 80f), cornerRadius = 16f)
    drawCircle(Color.Green, radius = 30f, center = Offset(200f, 200f))

    // Brushes (gradients)
    val gradient = Brush.horizontalGradient(
        0f to Color.Red,
        1f to Color.Blue,
    )
    drawRect(gradient, topLeft = Offset(300f, inset), size = Size(120f, 80f))

    // Images
    drawImage(myImageBitmap, topLeft = Offset.Zero, alpha = 0.8f)
    drawImage(myImageBitmap, srcOffset = Offset(10f, 10f), srcSize = Size(50f, 50f), dstOffset = Offset(50f, 50f), dstSize = Size(200f, 100f))

    // Paths
    val path = Path().apply {
        moveTo(50f, 50f)
        cubicTo(150f, 0f, 200f, 100f, 100f, 150f)
        close()
    }
    drawPath(path, Color.Cyan, style = Stroke(width = 3f))
}
```

## Supported API surface

### Primitives (DrawScope)

| Operation | Supported? | Notes |
|-----------|-----------|-------|
| `drawRect(color\|brush, topLeft, size, alpha, style, colorFilter, blendMode)` | ✅ | All params honored |
| `drawRoundRect(color, topLeft, size, cornerRadius, …)` | ✅ | Degrades to rect for zero radius |
| `drawCircle(color\|brush, radius, center, …)` | ✅ | |
| `drawOval(color, topLeft, size, …)` | ✅ | |
| `drawArc(color\|brush, startAngle, sweepAngle, useCenter, …)` | ✅ | |
| `drawLine(color\|brush, start, end, strokeWidth, …)` | ✅ | |
| `drawPoints(points, color, pointSize, …)` | ✅ | |
| `drawImage(image, topLeft, alpha, colorFilter)` | ✅ | Simple overload |
| `drawImage(image, srcOffset, srcSize, dstOffset, dstSize, …)` | ✅ | Sub-rect overload |
| `drawPath(path, color\|brush, …)` | ✅ | |
| `drawText(text, topLeft, fontSize, color)` | ✅ | Basic text (no rich styling) |
| `translate/scale/rotate` | ✅ | Affine transforms |
| `clipRect/clipPath` | ✅ | |
| `withTransform { }` | ✅ | Save/restore around block |
| `inset { }` | ✅ | Scoped inset |

### Color & Painting

| Feature | Status |
|---------|--------|
| `Color` as `@JvmInline value class` | ✅ |
| `Color.Unspecified` distinct sentinel | ✅ |
| `copy(alpha=…)`, `lerp()`, `compositeOver()` | ✅ |
| `Color(0xFFRRGGBB)` / `Color(red, green, blue, alpha)` | ✅ |
| `Stroke(width, miter, cap, join, pathEffect)` | ✅ |
| `PathEffect.DashPathEffect` | ✅ |

### Brush (gradients)

| Brush | Supported? |
|-------|-----------|
| `SolidColor(value)` | ✅ |
| `LinearGradient` (via `Brush.linearGradient`) | ⚠️ 2-stop gradients ✅; **multi-stop (>2) collapses to first+last only** (intermediate stops dropped, logged) |
| `RadialGradient` | ⚠️ Degrades to first color stop |
| `SweepGradient` | ⚠️ Degrades to first color stop |
| `Brush.horizontalGradient` / `verticalGradient` | ✅ Sugar for linear |

### ColorFilter & BlendMode

| Feature | Supported? |
|---------|-----------|
| `ColorFilter.Tint(color, blendMode)` | ✅ Approximated |
| `ColorFilter.ColorMatrix` | ⚠️ No-op (logged) |
| `ColorFilter.Lighting` | ⚠️ No-op (logged) |
| `BlendMode.SrcOver` | ✅ Native |
| Porter-Duff / common separable blend modes (`DstOver`, `SrcIn`/`SrcOut`, `DstIn`/`DstOut`, `Plus`, `Multiply`, `Screen`, `Overlay`) | ✅ Software-composited per pixel — **Canvas/Image/Icon only** (requires an offscreen image) |
| Remaining blend modes; all non-SrcOver modes on container/leaf surfaces | ⚠️ Degrade to SrcOver + log |

### Path

| Operation | Supported? |
|-----------|-----------|
| `moveTo/lineTo/close` | ✅ |
| `quadraticBezierTo/rQuadraticBezierTo` | ✅ |
| `cubicTo/rCubicTo` | ✅ |
| `arcTo/addArc` | ✅ |
| `addRect/addOval/addRoundRect` | ✅ |
| `addPath` | ✅ |
| `fillType` (EvenOdd/NonZero) | ✅ |
| `getBounds/reset/rewind` | ✅ |
| `op(*)` | ⚠️ Union only |

### Image & Painter

| Feature | Status |
|---------|--------|
| `Image(bitmap, …, alignment, contentScale, alpha, colorFilter)` | ⚠️ Alignment/contentScale/alpha ✅; `colorFilter` ignored on image draws |
| `Image(painter, …, alignment, contentScale, alpha, colorFilter)` | ⚠️ Built-in painters draw, but `colorFilter` is currently ignored |
| `ContentAlignment` (Center, TopStart, …, BottomEnd) | ✅ 2D alignment |
| `ContentScale` (Fit, Crop, FillBounds, …) | ✅ |
| `Painter` abstract class | ✅ |
| `BitmapPainter`, `ColorPainter`, `BrushPainter` | ✅ |
| `ImageBitmapConfig` (Argb8888, Rgb565, Alpha8) | ✅ |
| `ImageBitmap.createOffscreen(w, h, config)` | ✅ |
| `ImageBitmap.toPixelMap()` | ✅ |
| Display-aware image cache | ✅ |
| `Modifier.paint(painter, …)` | ⚠️ Marker only — modifier-driven painter drawing not wired yet |

### Vectors & Icons

| Feature | Status |
|---------|--------|
| `ImageVector` (Builder, Group, PathNode) | ✅ |
| `Icons.Default.*` (Add, ArrowBack, Check, Close, Menu) | ✅ |
| `Icons.Filled.*` / `Icons.Outlined.*` | ✅ (same set) |
| `Icon(imageVector, tint)` | ✅ Real SVG rendering |
| `Icon(painter, tint)` | ✅ |

### Draw modifiers (on containers)

| Modifier | Status |
|----------|--------|
| `Modifier.drawBehind { }` | ✅ Container only |
| `Modifier.drawWithCache { }` | ⚠️ Forwards to `drawBehind`; cache lifetime ignored |
| `Modifier.drawWithContent { }` | ⚠️ Behind-only (no "in front") |
| `Modifier.background(color)` | ✅ All widgets (SWT native) |
| `Modifier.background(brush, shape?)` | ✅ Container only (drawBehind) |
| `Modifier.border(width, color\|brush, shape)` | ✅ Container only |
| `Modifier.clip(shape)` | ✅ SWT Region |

## Reactive redraw

When snapshot state read inside a `Canvas { }` draw block changes, the global snapshot write observer triggers recomposition, which updates the draw callback and calls `canvas.redraw()`. This means:

```kotlin
var hue by remember { mutableStateOf(0f) }
Canvas(modifier = Modifier.fillMaxSize()) {
    drawRect(Color(red = hue, green = 0.5f, blue = 0.5f, alpha = 1f), size = size)
    // Changes to 'hue' automatically redraw
}
LaunchedEffect(Unit) {
    while (true) { delay(50); hue += 0.01f }
}
```

Invalidations from multiple widgets are coalesced per frame via `SWTMonotonicFrameClock`.

## Degradation policy (tiered fidelity)

| Tier | Widgets | Policy |
|------|---------|--------|
| 1 — Explicit surfaces | `Canvas`, `Image`, `Icon` | **Highest fidelity** — Compose parity target, with the documented backend gaps below |
| 2 — Container decoration | `Row`, `Column`, `Box` | **Best effort** — draw modifiers honored |
| 3 — Native leaf widgets | `Button`, `Text`, `Checkbox` | **Native-first** — only `background(color)`/`foreground(color)` mapped; draw modifiers are no-ops + log |

### Degradation log (with `-ea` JVM flag)

```
[DrawModifier] Draw modifiers are only supported on Composite containers; ignoring on Label
[SwtCanvasBackend] radial gradient not natively supported; using first colour-stop
[SwtCanvasBackend] blend mode Lighten not natively supported; degrading to SrcOver
```

## What's not yet supported

- **D4 optimizations**: dirty-region redraws, offscreen caching, benchmarks — planned but not blocking
- **Skia backend**: interface seam exists; no Skia implementation scheduled
- **`Modifier.paint`**: currently a marker only; modifier-driven painter drawing is not wired yet
- **Full `drawWithContent` "in front"**: the behind-in-front interleaving for drawWithContent is limited to behind-only on the container surface; the "in front" overlay canvas is a future enhancement
- **Rich text in Canvas**: `drawText` is basic; no font styling, text measuring, or paragraph layout
- **GraphicsLayer / hardware acceleration**: not available through GC; limited to what SWT GC provides
- **Image `colorFilter`**: accepted by the API but ignored on SWT image draws
- `drawWithCache` cache lifetime: the modifier forwards to `drawBehind` and ignores the cache lifetime
- `Path.op` beyond `Union`, `ColorFilter.ColorMatrix`/`Lighting` no-ops, `RadialGradient`/`SweepGradient` degrade-to-solid — see the tables above
- **Rendering precision (inherent to the SWT `GC` integer backend):** fractional stroke widths and all coordinates/sizes/radii truncate to int (sub-pixel geometry can shift or disappear); `clipPath` (rounded-rect/oval clip) produces aliased edges (GC clipping is a pixel mask with no anti-aliasing); multi-stop linear gradients collapse to first+last only. Full detail in `roadmap/drawing-foundation.md` §11.

## See also

- `docs/limitations.md` — consolidated current and long-term limitations (incl. drawing gaps)
- `docs/roadmap/drawing-foundation.md` — the master plan (D0–D4)
- `sweet/src/main/kotlin/androidx/compose/ui/graphics/Canvas.kt` — DrawScope interface
- `sweet/src/main/kotlin/io/github/ddsimoes/sweet/drawing/` — SWT backend implementation
