# Background: SWT Layout

This document summarizes the SWT layout mechanism. It does not describe Sweet’s layout directly, but it is useful context for understanding why Sweet takes control of layout instead of relying on SWT `Layout` and `LayoutData`.

## Core model

SWT separates **containers** from **layout policy**:

- `Composite` is a container that can host child `Control`s.
- A `Layout` instance on a `Composite` decides how to size and position children.
- Each child can provide an optional `LayoutData` object (e.g., `GridData`, `RowData`, `FormData`) to express per‑child constraints and hints.
- Each widget reports a preferred size via `computeSize(wHint, hHint, changed)`.

Layout naturally falls into two conceptual phases:

1. **Measurement (`computeSize`)** – bottom‑up:
   - Parents ask layouts for preferred size via `Layout.computeSize`.
   - Layouts ask children for their preferred sizes using `child.computeSize(...)`, often with width/height hints.
   - Composite containers aggregate child sizes and add trim (borders, scrollbars, etc.) to compute their own preferred size.
2. **Placement (`layout`)** – top‑down:
   - When a composite is laid out, its `Layout.layout` implementation:
     - Receives the composite’s client area (available bounds).
     - Calculates bounds for each child given layout rules and `LayoutData`.
     - Calls `child.setBounds(x, y, width, height)` to position children.

## LayoutData (“modifiers”)

Each layout defines its own `LayoutData` with constraints and hints. Examples:

- `GridData` (for `GridLayout`):
  - Alignment (`horizontalAlignment`, `verticalAlignment`), e.g. `SWT.FILL`, `SWT.LEFT`, `SWT.CENTER`.
  - Grab flags (`grabExcessHorizontalSpace`, `grabExcessVerticalSpace`) to expand when there is extra space.
  - Spanning (`horizontalSpan`, `verticalSpan`), size hints (`widthHint`, `heightHint`), minimums.
  - `exclude` to temporarily remove a control from layout calculations.
- `RowData` (for `RowLayout`): explicit width/height overrides.
- `FormData` (for `FormLayout`): attachments to parent edges or other controls, enabling constraint‑style layouts.

Layouts interpret these `LayoutData` objects to decide how to distribute available space, align children, and handle spanning/constraints.

## Efficiency: caching and dirty flags

SWT avoids recomputing everything on every change by:

- Caching preferred sizes and layout results where possible.
- Using “dirty” flags and APIs to control cache invalidation:
  - `layout(true, ...)` flushes caches and forces recalculation.
  - `layout(false, ...)` reuses caches when only sizes of ancestors changed.
  - `layout(Control[] changed)` and `Layout.flushCache(Control)` localize recomputation to specific subtrees.
- Deferring work:
  - `Control.requestLayout()` and `layout(..., SWT.DEFER)` schedule layout to run later in the event loop.
  - `Composite.setLayoutDeferred(true/false)` lets you batch many updates and then perform a single optimized layout pass.

Understanding these mechanisms helps explain why Sweet centralizes layout in its own system (`SweetLayout`) and avoids mixing SWT layout policies with Compose-style constraints and modifiers.

