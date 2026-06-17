# Sweet Compose – Drawing Foundation Plan

**Status:** D0–D3 complete (2026-06-08), D4 (perf/docs) pending. Post-consolidation fixes landed: reactive redraw invalidation, paint ghosting, border insets | **Created:** 2026-06-08 | **Owner:** drawing/graphics foundation
**Scope:** Canvas, Image, DrawScope, Painter, ImageBitmap, Path/Brush/ColorFilter, draw modifiers,
vectors/icons, and drawing backends. **This is the single source of truth for drawing work**.

Sweet already has a minimal SWT-backed drawing layer, but on a **reduced, API-divergent** surface.
This plan re-frames the work around (a) **API fidelity** with Android/MPP Compose and (b) a **correct
SWT bridging model** that keeps native widgets native.

---

## 1. Guiding principle: decoration, not replacement

Sweet is **not** a Skia/Swing-style renderer that uses SWT as one big dumb canvas. The app is a tree
of **native SWT widgets** (an SWT `Button` stays a real `org.eclipse.swt.widgets.Button`), and Sweet
must embed cleanly into existing SWT applications for incremental migration. Drawing therefore has a
**hard boundary**: it decorates and supplements native widgets; it never takes over their rendering.

This yields three tiers, each with a different (and honest) SWT mapping:

| Tier | What | SWT bridge | Fidelity target |
|------|------|-----------|-----------------|
| **1 — Explicit surfaces** | `Canvas {}`, `Image`, `Painter`, `Icon`/vectors — content with *no* native widget | Dedicated `org.eclipse.swt.widgets.Canvas` + `GC` backend | **High** — match Compose |
| **2 — Container decoration** | `Modifier.drawBehind/drawWithContent/drawWithCache`, `background(shape/brush)`, `border`, `clip(shape)` on Box/Row/Column | `PaintListener` on the container `Composite` (paints *behind* its child controls) + `Control.setRegion` for clipping | **High** for "behind", **bounded** for "in front" |
| **3 — Native leaf widgets** | Draw modifiers on `Button`/`Text`/`Checkbox`/… | Map to native properties only (`background`/`foreground`); otherwise documented no-op | **Native-first** (intentionally limited) |

> The boundary is deliberate and matches `long-term-direction.md`: *"custom components should appear
> only when necessary, and must behave as robustly and interoperably as normal SWT widgets."*
> We do **not** owner-draw native widgets to satisfy arbitrary draw modifiers.

---

## 2. Current state (audit)

What already exists (`io.github.ddsimoes.sweet.drawing.*`, `androidx.compose.{foundation,ui.graphics}`):

- `SweetCanvas` (internal) → `SwtCanvasBackend` over `GC`; `SweetDrawScope`; `CanvasIntegration`
  (`Canvas` widget + `PaintListener`). `Canvas`, `Image(bitmap)`, `Image(painter)`, `Painter`,
  `BitmapPainter`. Image loading + `WeakReference` cache; `ImageBitmap` disposal via composition.
- Per-display **Color** and **Font** caches (`getOrCreateColor/Font`); per-op `Path`/`Transform`
  dispose. Path → `org.eclipse.swt.graphics.Path` (MoveTo/LineTo/Close only).

Where it **diverges from real Compose** (the gap this plan closes):

| Area | Today | Real Compose |
|------|-------|--------------|
| Geometry pkg | `Size`/`Rect`/`RoundRect` in `ui.graphics`; `Offset` is a `data class` | All in `ui.geometry`; `Offset`/`Size` are packed value classes w/ operators, `Unspecified`, `center`, `minDimension`… |
| `Color` | sRGB-only `ULong`; **`Unspecified == Transparent`** (wrong); no `copy`/`lerp`/`Color(0xFF…)` factories | Distinct `Unspecified` sentinel; `copy(alpha=…)`, `lerp`, `compositeOver`, top-level `Color(Long/Int)` |
| `DrawScope` | rect/circle/line/path/image/text only; **not a `Density`**; no `alpha`/`style`/`colorFilter`/`blendMode`/`Brush` | `DrawScope : Density`; rect/roundRect/oval/arc/points/image(src,dst); `Brush` overloads; `inset/scale/rotate/clip*{}` |
| `DrawStyle` | `Fill`, `Stroke(width)` | `Stroke(width, miter, cap, join, pathEffect)` |
| `Brush` | **absent** | `SolidColor`, linear/radial/sweep gradients |
| `ColorFilter` | empty marker | `tint`, `colorMatrix`, `lighting` |
| `Path` | MoveTo/LineTo/Close, fake roundRect | beziers, arcs, addOval/addArc/addRoundRect, `fillType`, `op`, `transform` |
| `Image` | `Image(bitmap, desc, modifier, alignment: Alignment.Combined)` | `Image(…, alignment: Alignment = Center, contentScale = Fit, alpha, colorFilter)` |
| Draw modifiers | **none** (`background` ignores `shape`/`brush`) | `drawBehind`, `drawWithContent`, `drawWithCache`, `clip`, `border`, `background(brush, shape)` |
| Invalidation | redraw on resize only | draw blocks re-run when read snapshot state changes |
| Vectors | `Icon` = emoji shim; no `ImageVector` | `ImageVector`/`Icons`/`VectorPainter`/`rememberVectorPainter` |

---

## 3. Scope of this plan

**In (now): "Core drawing foundation + Material vectors."**
Faithful primitives → Tier-1 surfaces → Tier-2 container draw modifiers → vectors/icons →
invalidation/resources/perf. **SWT `GC` backend only.**

**Seam, not build: Skia.** Reshape the internal `SweetCanvas` so a `SkiaCanvasBackend` *could* drop
in later (mirrors Compose's own `Canvas`/`CanvasDrawScope` split), but **do not** build Skia or
backend-routing config now (YAGNI; SWT-first).

**Explicitly out:** owner-drawing native widgets; transforming/opacity on native child widgets;
full SVG parsing (a minimal subset only, if any); arbitrary `BlendMode`/`PathEffect`/`ColorMatrix`
that the GC backend can't honor (these degrade to documented no-ops with debug logs).

---

## 4. Target architecture

> **Working rule — verify before you build.** Whenever Sweet claims an API is in the supported subset,
> its package and signature **must** match upstream Compose. Before implementing any drawing API, verify
> its exact shape against the upstream source (`3rdparty/compose-multiplatform-core`) or the official
> Compose API docs — never invent an ad-hoc signature where an upstream shape already exists.

### 4.1 Faithful primitives (`ui.geometry` + `ui.graphics`)

- **Move** `Size`, `Rect`, `RoundRect` to `androidx.compose.ui.geometry`. Make `Offset` and `Size`
  faithful value classes with operators, `Unspecified`/`Infinite`, `center`, `min/maxDimension`,
  `Rect(Offset, Size)`, `Rect.center`. Keep **`@Deprecated typealias`** in `ui.graphics` pointing to
  the new types for a migration window (blast radius is only ~10 files — see §6).
- **`Color`**: fix `Unspecified` to a distinct sentinel; add `isSpecified`/`isUnspecified`,
  `copy(alpha,red,green,blue)`, `lerp`, `compositeOver`, `luminance`, and top-level
  `Color(color: Long)` / `Color(color: Int)` factories. Keep sRGB-only packing but **document** the
  simplification (no color-space component); the public component/constructor surface must match.
- **`Path`**: extend to the real surface — `quadraticBezierTo`/`cubicTo` (+ `relative*`), `arcTo`,
  `addOval`, `addArc`, real `addRoundRect`, `addPath`, `fillType` (NonZero/EvenOdd), `getBounds`,
  `reset`/`rewind`, `transform(Matrix)`. `op(p1, p2, PathOperation)` has **no native SWT equivalent**
  → approximate via `Region` boolean ops where possible, else documented best-effort.
- **`DrawStyle`/`Stroke`**: `Stroke(width, miter, cap: StrokeCap, join: StrokeJoin, pathEffect)`.
  Map `cap`→`GC.setLineCap`, `join`→`GC.setLineJoin`, `PathEffect.dashPathEffect`→`GC.setLineDash`.
- **`Brush`**: `SolidColor`, `Brush.linearGradient/horizontalGradient/verticalGradient`,
  `radialGradient`, `sweepGradient`(approx). Backend renders gradients via SWT **`Pattern`** (a native
  handle — must be cached + disposed, see §4.7). Radial/sweep are approximations on the GC backend;
  document limits.
- **`ColorFilter`**: `ColorFilter.tint(color, blendMode)` (used by `Icon`/`Image`), `lighting`,
  best-effort `colorMatrix`. `tint` implemented by recoloring an offscreen `ImageData` for
  small images/icons; document perf for large images.
- **`BlendMode`**: expand to the real enum; honor the few SWT supports (default `SrcOver`),
  debug-log + degrade the rest (consistent with the shim policy in `overview.md`).
- **`DrawScope : Density`** so `dp.toPx()`/`sp.toPx()` work inside draw blocks. Add `center`,
  `drawContext.size`, scoped `inset/translate/rotate/scale/clipRect/clipPath{}` and `withTransform{}`,
  plus `drawRoundRect/drawOval/drawArc/drawPoints/drawImage(srcOffset,srcSize,dstOffset,dstSize)` and
  `Brush` overloads. `alpha`, `style`, `colorFilter`, `blendMode` params on every primitive.

### 4.2 Internal backend seam (`SweetCanvas` + `Paint`)

Reshape `SweetCanvas` to mirror `androidx.compose.ui.graphics.Canvas` (save/restore/translate/scale/
rotate/clipRect/clipPath/drawRect/drawRoundRect/drawOval/drawArc/drawPath/drawImage/drawPoints) taking
a single sweet-internal **`Paint`** (color|brush, style, stroke, alpha, colorFilter, blendMode). A
`CanvasDrawScope`-style adapter implements the public `DrawScope` on top — exactly Compose's own split,
so a `SkiaCanvasBackend` later implements `SweetCanvas` with no change to `DrawScope` or call sites.
`SwtCanvasBackend` implements it over `GC` with the resource discipline in §4.7. The interface also
exposes **capability reporting** (e.g. `supports(feature)`), so the `DrawScope` adapter can degrade
unsupported features (radial/sweep gradients, exotic blend modes, path effects) to a documented no-op +
debug log uniformly across backends. **No `SkiaCanvasBackend` and no `SweetGraphicsConfig` routing is
built now** — only the interface shape is kept Skia-ready.

### 4.3 Tier 1 — explicit drawing surfaces (finish to fidelity)

- **`Canvas(modifier, onDraw)`**: keep the dedicated SWT `Canvas` widget (own paint, `DOUBLE_BUFFERED`),
  reuse the shared `DrawScope`/backend. Conceptually equals `Spacer(Modifier.drawBehind(onDraw))`; once
  Tier-2 exists, document the equivalence (a real SWT Canvas widget is the right host for a pure surface).
  Give it a **real Canvas node** that owns callback reconciliation, redraw, and disposal — the current
  `setData(callback)` plumbing is transitional, not the long-term node contract.
- **`Image(bitmap|painter, contentDescription, modifier, alignment = Alignment.Center, contentScale =
  ContentScale.Fit, alpha = 1f, colorFilter = null)`** — faithful signature. Implement `ContentScale`
  (Fit/Crop/FillBounds/Inside/None/FillWidth/FillHeight) + 2D `Alignment` (see §5 dependency).
- **`Painter`** as the real **abstract class** (`intrinsicSize`, `applyAlpha`, `applyColorFilter`,
  `applyLayoutDirection`, final `draw(...)`, protected `onDraw`) + `BitmapPainter`, `ColorPainter`,
  `BrushPainter`. Add `Modifier.paint(painter, …)` and `painterResource(...)`.
- **`ImageBitmap`**: add `config`, `ImageBitmap(width, height, …)` offscreen factory (SWT `Image`),
  `toPixelMap()`/`readPixels` via `ImageData`. Align loader names (`loadImageBitmap`, `useResource`).
  Make image caching **display-aware**: a cache must never hand back an `Image` bound to a disposed or
  different `Display` (the current global path-keyed `WeakReference` cache is display-unsafe).

### 4.4 Tier 2 — container draw modifiers (the new core capability)

Attach **only to `Composite`-backed nodes** (Box/Row/Column + the generic container). A `Composite`'s
own surface is *behind* its child controls, so the OS paints children on top of whatever we draw — this
gives correct **"draw behind content"** semantics for free.

- `Modifier.drawBehind { DrawScope }` → `PaintListener` on the container, drawn before children paint.
- `Modifier.drawWithCache { onBuildDrawCache }` → cache brushes/paths/offscreen layers across paints.
- `Modifier.drawWithContent { ContentDrawScope }` → **bounded**: the "behind `drawContent()`" portion is
  honored on the container surface; the "in front of content" portion is supported via a transparent
  **overlay canvas** sibling painted last. Arbitrary interleaving between native children is **not**
  supported (it can't be, without owning child paint) — documented.
- `Modifier.background(color|brush, shape, alpha)`:
  - solid color + `RectangleShape` on a leaf → native `control.background` (already works);
  - any shape/brush on a container → drawn via `drawBehind` using `shape.createOutline(size,…)` + `Brush`.
    **This finally honors `shape` (Rounded/Circle/Cut) and gradients.**
- `Modifier.border(width, color|brush, shape)` → stroked outline via `drawBehind` on containers.
- `Modifier.clip(shape)` / `clipToBounds()` → build a `Region` from the shape `Outline` and
  `Control.setRegion(region)`. SWT regions clip the container **and its native children** — a genuinely
  nice capability for non-rectangular clipping. **`Region` is a native handle** → rebuild on size/shape
  change, dispose on change + on control dispose. Document platform caveats (anti-aliased edges are hard;
  region is pixel-mask).
- `Modifier.alpha/rotate/scale/graphicsLayer/shadow`: honored for **drawn content** (Tier 1 + container
  backgrounds) only. Transforming/fading **native child widgets** is out of scope (OS paints them
  axis-aligned/opaque) → documented no-op on leaves. `shadow` = approximated soft shadow behind a
  container; no-op where not feasible.

### 4.5 Tier 3 — native leaf policy

Leaf widgets keep native paint. Draw modifiers are honored only when they map to a native property
(`background`/`foreground` solid color). Anything else logs once (when `-ea`) via `SweetDebugger` and is
a no-op. An **opt-in escape hatch** (`Modifier` that wraps a leaf in a Sweet `Composite`) can be offered
for the rare case where a caller truly needs custom decoration around a native control — but it is never
the default, and is documented as changing the widget's host.

### 4.6 Invalidation & frame coordination (correctness gap today)

Draw blocks read Compose snapshot state (animations, app state). Today nothing repaints on state change.

- Wrap each draw-block execution in **snapshot read observation** (`SnapshotStateObserver`) scoped to the
  owning `Canvas`/`Composite`. When observed state invalidates, schedule a `redraw()`.
- Route redraws through the existing **`SWTMonotonicFrameClock`/`LayoutCoordinator`** frame so multiple
  invalidations coalesce into one repaint per frame (no repaint storms) and stay ordered after layout.
- Tie observer + listener lifecycle to control disposal. Prefer bounded `redraw(x,y,w,h,true)` when the
  invalidation carries bounds (`drawWithCache`).

### 4.7 Resources & threading

- **Threading**: all `GC` ops on the SWT UI thread (paint events already are). `ImageBitmap` **decode**
  (bytes/stream → `ImageData`) may run off-thread, but `new Image(display, data)` **must** be created on
  the UI thread — codify this split in the loader.
- **Native-handle discipline** (SWT objects that leak if not disposed): Color/Font caches exist; **add** a
  per-display **`Pattern` cache** (gradients) and **`Region`** lifecycle (clip); keep per-op `Path`/
  `Transform` dispose; dispose offscreen `Image`s (tint/cache/`ImageBitmap`) via `DisposableEffect`.
  Extend the existing `MemoryLeak`/`SweetImageBitmapDisposalTest` suites with Pattern/Region/offscreen
  coverage.
- **Performance**: `DOUBLE_BUFFERED` Tier-1 canvases (+ `NO_BACKGROUND`/`NO_REDRAW_RESIZE` to cut flicker);
  dirty-region redraws; `drawWithCache` for static/expensive content; optional offscreen-bitmap caching.
  Benchmarks: 60 FPS animated canvas, repeated chart redraws, 1k-shape draw, large-image scaling, and
  vector-icon lists.

### 4.8 Vectors & icons (Tier 1; closes the P0 `Icon` shim)

- `ImageVector` + `ImageVector.Builder` (`path{}`, groups with transforms, `PathNode`s), a starter
  `Icons.Default/Filled/Outlined` set (hand-port/generate the common glyphs), `rememberVectorPainter`
  → `VectorPainter` that renders nodes via `DrawScope` (`Path` + `Brush` + `Stroke`) on the SWT backend.
- `Icon(imageVector|painter|bitmap, contentDescription, tint)` renders on a Tier-1 surface with
  `ColorFilter.tint`. **Replaces emoji glyphs with real vector rendering** — high-value, pure-drawing,
  fully consistent with "native widgets stay native" (an icon has no native widget).
- **Cache policy:** render vector paths directly each paint; only rasterize to a per-`(display, vector
  identity, size, tint)` SWT `Image` cache when profiling shows repeated vector drawing is a hot path.
- SVG parsing: out of scope beyond an optional minimal subset; documented.

---

## 5. Cross-cutting dependency: `Alignment` & `ContentScale`

A faithful `Image`/`Painter` needs the **2D `Alignment`** (real Compose `Alignment.Center` is 2D). Sweet's
2D alignment is currently `Alignment.Combined`, and `Alignment` is the 1D base — so real Compose code
`Image(bmp, null, Modifier, Alignment.Center)` won't compile. **Recommendation:** introduce a real 2D
`Alignment` (interface with `align(size, space, layoutDirection): IntOffset`, `Horizontal`/`Vertical`
nested) and keep `Alignment.Combined` as a `@Deprecated` alias. This is slightly beyond "pure drawing"
but `Image` blocks on it; sequence it in **D1**. `ContentScale` is new and self-contained.

---

## 6. API compatibility & breaking-change migration

The faithful-API direction (the project's stated goal: drop-in `androidx.compose.*`) means **breaking the
current divergent drawing API**. Blast radius is small and mostly inside the drawing layer itself:

- **Geometry move** touches ~10 files: the `drawing/*` backend, `foundation/Image.kt`,
  `foundation/shape/shape.kt`, and tests `CanvasTest`, `SweetComposeCanvasTest`,
  `MemoryLeakAndDisposalTest`, plus `sample-sweet/CanvasSample.kt`. Deprecated `typealias`es in
- **Signature changes** (`DrawScope`, `Image`, `Color.Unspecified`) require touching the same drawing
  tests/samples and updating the API drift allowlist (doc 40) — re-classify entries from "implemented" to
  the honest partial status, then back to implemented as fidelity lands.
> implementation will **not** edit a test to make it pass without explicit sign-off. The migrations here
> are *API renames* (import path, added default params), not weakening assertions — each will be listed
> for a go-ahead before any test is touched.

**Working assumption (consistent with the stated drop-in goal):** match Compose, using deprecated aliases
as a migration bridge. The strictly-additive alternative (no breaks, accept permanent divergence) remains
available and would only re-shape D0.

---

## 7. Testing strategy

- **API-shape / compile tests** (in `sample-sweet/src/main/kotlin` or `sweet-tests`) that import the canonical
  `androidx.compose.*` packages for the supported subset and exercise faithful signatures — these catch
  package/signature drift early, before behavior tests.
- **AutoSWT** UI tests per capability (Tier 1 surfaces, each draw modifier, clip-region, gradients,
  vectors), asserting widget/layout via the layout-assertion API and visuals via `saveSVG()`/
  `saveScreenshot()` (visual regression). Keep pixel asserts tolerant per the determinism caveats in
  `test-determinism.md`.
- **Invalidation tests**: mutate `mutableStateOf` read inside a `Canvas`/`drawBehind` and assert a repaint
  occurred (paint-count probe) — guards the §4.6 correctness gap.
- **Leak tests**: extend `MemoryLeak*`/`SweetImageBitmapDisposalTest` to Pattern/Region/offscreen images.
- Validate at the **root** (`xvfb-run -a ./gradlew test --continue`) — `:sample-sweet` included.

---

## 8. Phasing (each phase shippable + tested)

- **D0 — Faithful primitives & backend seam.** Move geometry types (+ deprecated aliases); fix `Color`;
  expand `Path`/`DrawStyle`/`Stroke`; add `Brush`/`SolidColor`/gradients, `ColorFilter.tint`,
  `BlendMode`; `DrawScope : Density` + full op set; reshape `SweetCanvas` + `Paint`; `Pattern` cache.
  Migrate existing drawing tests/samples. *Unblocks everything.*
- **D1 — Tier-1 surfaces to fidelity.** Faithful `Canvas`; `Image` (+`ContentScale`/`alpha`/`colorFilter`)
  and the 2D `Alignment` dependency (§5); `Painter` abstract + Bitmap/Color/Brush painters; `ImageBitmap`
  factory/pixels; `Modifier.paint`; **state→redraw invalidation** + double-buffering.
- **D2 — Tier-2 container draw modifiers.** `drawBehind`/`drawWithCache`/`drawWithContent`(bounded);
  `background(shape/brush)`; `border`; `clip(shape)`→`Region`; alpha/rotate/scale scoped to drawn content;
  Pattern/Region leak tests.
- **D3 — Vectors & icons.** `ImageVector`/`Icons`/`rememberVectorPainter`/`VectorPainter`; real `Icon` with
  tint. *Closes P0 Icon gap.*
- **D4 — Perf, polish, docs.** Dirty-region redraws, offscreen caching, benchmarks; usage guide
  `docs/guides/drawing.md`; update the API drift allowlist (doc 40); sketch (not build) a `SkiaCanvasBackend`
  signature to validate the seam.

Acceptance per phase: faithful signatures compile against copy-pasted Compose Desktop snippets for that
phase; new AutoSWT + leak tests green at the root; API drift allowlist updated.

### Definition of done

The drawing foundation is "done" when:

- Common supported Compose drawing code **compiles against Sweet without import rewrites**.
- `Canvas`/`Image`/`Icon` and the supported draw modifiers work inside `Row`/`Column`/`Box`, scroll
  containers, and embedded-SWT hosts.
- Unsupported parameters are **documented + debug-logged**, never silently misleading and never thrown.
- Repeated repaint leaks no SWT resources (`Color`/`Font`/`Pattern`/`Region`/`Path`/`Transform`/`Image`).
- The implementation stays **SWT-first** while leaving a clean, untouched-public-API path to Skia.
- Root validation passes under Xvfb (`xvfb-run -a ./gradlew test --continue`).

---

## 9. Risks & honest SWT limitations

| Risk / limit | Mitigation |
|---|---|
| `drawWithContent` arbitrary interleave with native children | Support behind + overlay-in-front only; document the boundary |
| Radial/sweep gradients, `BlendMode`, `ColorMatrix`, `Path.op` weak/absent on `GC` | Approximate where possible; else documented no-op + debug log; full fidelity is the (future) Skia backend's job |
| `Region` clip is a pixel mask (aliased edges); per-`Control` region caveats by platform | Document; restrict to container nodes; platform-specific tests |
| Native-handle leaks (Pattern/Region/Image/Path/Transform) | Per-display caches + `DisposableEffect` + leak-test matrix |
| Repaint storms from snapshot writes | Coalesce via frame clock / `LayoutCoordinator`; dirty regions |
| Breaking the shipped drawing API | Deprecated aliases; explicit test-migration sign-off (§6) |
| `Alignment` rework scope-creep | Confined to introducing 2D `Alignment` + deprecated `Combined` alias in D1 |

---

## 10. Decisions captured / open

- **Scope = Core + Material vectors**, SWT `GC` backend only; Skia kept as a cheap interface seam, not
  built. (Confirmed direction.)
- **Bridging model = decoration, not replacement**; native widgets stay native; no owner-drawing; no
  transform/opacity on native children. (Confirmed direction.)
- **Q1 / API fidelity = match Compose with deprecated aliases** — *assumed*, pending confirmation (§6).
- **`Alignment` 2D rework** — proposed as a D1 dependency; confirm appetite to touch it now vs. a
  drawing-local shim.

---

## 11. Kitchen Sink sample — known rendering limitations (2026-06-09)

These are the rendering gaps observed by diffing the `DrawingKitchenSink` sample against the JetBrains
MPP reference (`sample-jetbrains` MPP runner). They are **inherent to the SWT `GC` backend**, not
plain bugs — each needs a deliberate direction (approximate further, or defer to the future Skia
backend). Recently fixed and **no longer** limitations: `drawPoints` dot sizing, dash-state leaking
into solid strokes, round-join rect corners, and **per-pixel blend-mode compositing** for the
common Porter-Duff/separable modes on Canvas-surface (Tier 1) scopes.

| Sample section | Limitation | Root cause | Possible direction |
|---|---|---|---|
| **§2 Gradient Brushes** | Multi-stop linear gradients collapse to **first + last stop only** (the `Red → Yellow → Blue` box renders `Red → Blue`, i.e. purple midpoint). 2-stop gradients are correct. | SWT `Pattern` linear-gradient supports exactly **two** colors. | (a) Segment-fill: fill axis-aligned shapes in bands between consecutive stops, each a 2-color sub-`Pattern` (rects only); (b) synthesize a gradient `Image` honoring all stops and use `Pattern(device, image)` (general but alignment/tiling work); (c) Skia for full fidelity. |
| **§8 BlendMode** | **Mostly resolved on Canvas surfaces.** The common modes (`DstOver`, `SrcIn`/`SrcOut`, `DstIn`/`DstOut`, `Plus`, `Multiply`, `Screen`, `Overlay`) are now software-composited per pixel on Tier 1 scopes (`Canvas`/`Image`/`Icon`, which have an offscreen image). **Residual:** remaining modes, and all non-`SrcOver` modes on container/leaf surfaces (no offscreen image), still degrade to `SrcOver` + log. | `GC` has no native Porter-Duff/separable support; compositing is done in software against the offscreen `ImageData`. | Full mode coverage and Tier 2/3 support need Skia (or an offscreen buffer on container surfaces). |
| **§9 ColorFilter** | Only `ColorFilter.Tint` is supported, and approximated (multiplicative; only `SrcIn`/`SrcAtop` get a dedicated path). `ColorMatrix` / `Lighting` are ignored; `ColorFilter` on `drawImage` is ignored. | No native `GC` color-filter stage. | Per-pixel image processing for `ColorMatrix`/image tint, or Skia. |
| **§7 Clipping** | `clipPath` (rounded-rect / oval clip) produces **aliased (jagged) edges**. | `GC` clipping is a pixel mask (`Region`/path) with no anti-aliasing. | Document; AA clip is a Skia-backend feature. |
| **§11 Vector Icons** & all strokes | Fractional **stroke widths truncate to int** (icon `strokeLineWidth = 2.25f` → `2`, min `1`). | `strokeWidth(paint)` does `.toInt().coerceAtLeast(1)`. | Round-half-up instead of truncate; keep float widths on the `GC` float path. |
| **All sections** | **Integer truncation** of all coordinates, sizes, and radii (every `.toInt()`). Sub-pixel offsets/sizes lose precision; thin/small geometry can shift or disappear. | Backend converts floats to ints before the `GC` integer drawing APIs. | Prefer the float `Path`/`Transform` APIs where available; round-half rather than truncate. |
| **§13 Reactive Redraw** | The cached offscreen `Image`/`GC` is **reused across paints and not cleared**; GC state must be reset per draw or it leaks between frames (the dash-leak bug was one instance). Animated canvases risk pixel/state accumulation. | `CanvasIntegration` reuses the buffer for performance without a per-frame clear or state reset. | Clear the buffer (and/or reset known GC state: dash, alpha, clip, transform) at the start of each paint. |
| **§4 Shapes** | `drawArc` with `useCenter = false` **filled** has no native equivalent and is approximated as a thick stroke. Stroked `drawRoundRect` join is not configurable. | SWT has `fillArc` (pie) but no "fill open arc"; `drawRoundRectangle` ignores line join. | Build the arc/round-rect as a `Path` and fill/stroke that when fidelity matters. |

**Net for the visible sample:** §2 (gradient midpoint colors), §8 (blend modes), and §9 (non-tint
filters) are the user-visible divergences from MPP; the rest are precision/edge-quality gaps. All are
backend-level and would be resolved wholesale by the deferred Skia backend (§4.2 seam), so the open
decision is **how much to approximate on `GC` now vs. wait for Skia**.
