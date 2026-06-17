# Sweet Compose — Long-Term Evolution Plan

This document describes the long-term vision: two usage modes, drawing backends, and incremental migration.
> For the current-state snapshot of what is partial, shim, by design, or not yet started, see
> `docs/limitations.md` — the single source of truth. This document describes the forward plan.

## Two usage modes

1. **SWT-first (current focus)**
   - Compose is a way to write SWT UIs — native look & feel, no forced Material styling.
   - Ideal for incrementally modernizing existing SWT apps without changing appearance.

2. **Compose-first / Material-like (future, optional)**
   - Closer to Compose Desktop / Material visuals while still running on SWT.
   - Custom-drawn components (Canvas / Skia) where SWT has no equivalent.
   - Components must continue to interoperate with SWT (focus, accessibility, keyboard/mouse, menus).

## Product goals

- Do not break existing SWT applications.
- Provide a smooth migration path: SWT → Sweet (SWT-first) → Sweet (Material-like) → (optional) Compose Desktop.
- Keep SWT as the host; custom components only where necessary.
- Abstract drawing behind two backends: SWT `GC` (default) and Skia/Skiko (planned).

## Style profiles

- **`SwtNativeProfile`** (default): SWT widgets as-is, minimal Material theme influence.
- **`MaterialLikeProfile`** (future): colors/styles from `MaterialTheme.colorScheme`, custom Canvas/Skia components where SWT is insufficient.

Profile selection: global or per-module, supporting gradual migration.

## Drawing backends

The active drawing plan is `docs/roadmap/drawing-foundation.md`. At a high level:

- **Tier 1 — Explicit surfaces** (`Canvas`, `Image`, `Painter`): dedicated SWT `Canvas` + `GC` backend, high Compose fidelity.
- **Tier 2 — Container decoration** (`drawBehind`, `background(shape)`, `border`, `clip`): `PaintListener` on composites.
- **Tier 3 — Native leaf widgets** (draw modifiers on `Button`/`Text`): map to native properties only; otherwise documented no-ops.
- **Skia backend**: future, for richer rendering; backend selection per window/app.

## Remaining phases (post-consolidation)

### Phase 2 — Style/Theme infrastructure

- `SweetThemeConfig(profile, drawingBackend)`.
- `MaterialTheme.colorScheme` lightly influences `Card`, `TopAppBar` — no forced colors on core widgets.
- Centralized color/font mapping, reusing SWT resource caches.

### Phase 3 — Material-like styling for selected components

Guarded by `MaterialLikeProfile`:
- Button variants with `ColorScheme` fill/stroke, basic elevation.
- `surface`/`surfaceVariant` for containers.
- `Typography` → SWT font mapping.

### Phase 4 — Custom Canvas-based components

Components where SWT has no equivalent: vector icons, pill-shaped buttons, badges — on `SweetCanvas` (SWT backend first). Must support focus, accessibility, tab order.

### Phase 5 — Skia/Skiko backend (optional)

Alternate backend for `SweetCanvas`; per-window selection. Equivalent behavior on both backends.

### Phase 6 — Compose-first mode

Honor Compose Desktop assumptions while using SWT as windowing toolkit. Document migration paths: SWT+Sweet → Compose-first Sweet → pure Compose Desktop.

## Migration guide

1. Start in SWT-first mode with components that preserve native look.
2. Migrate flow by flow — new screens first.
3. Introduce Material-like styling in isolated modules.
4. Decide long-term: stay on Sweet + `MaterialLikeProfile`, or move to Compose Desktop.
