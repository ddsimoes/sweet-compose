# Background: Jetpack Compose Layout

This document summarizes the Jetpack Compose layout model. It does not describe Sweet’s behavior directly, but it is useful context for understanding Sweet’s layout and how it differs from SWT.

## Phases and core contract

Compose uses three conceptual phases:

1. **Composition** – decides *what* to render by executing `@Composable` functions and building a tree of layout nodes and modifier chains.
2. **Layout** – decides *where* and *at what size*, split into:
   - **Measurement**: parents measure children with constraints and pick their own size.
   - **Placement**: parents position children within their bounds.
3. **Drawing** – decides *how* to paint, emitting drawing commands to a canvas/layers.

The key rule of layout is:

- Parents pass **constraints** (`minWidth`, `maxWidth`, `minHeight`, `maxHeight`) *down* to children.
- Children choose a size within those constraints and report it back as a `Placeable`.
- Parents then position children during placement; children never choose their own position.

## Layout nodes and `MeasurePolicy`

Every visual element is represented by an internal layout node. The layout behavior for a composable like `Row`, `Column`, or `Box` is defined by a `MeasurePolicy`:

```kotlin
fun interface MeasurePolicy {
    fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult
}
```

Within a `MeasurePolicy`:

- Parents call `measurable.measure(childConstraints)` on each child.
- Children return `Placeable`s with a chosen width/height.
- The parent calculates its own width/height and returns a `MeasureResult` with a placement block that calls `place(...)` on each child.

Intrinsic measurements (e.g., minimum/maximum intrinsic width/height) are supported but can be more expensive because they may measure children multiple times.

## Modifiers and layout

`Modifier` chains are central to Compose layout:

- Modifiers can intercept measurement and placement:
  - Adjust constraints (e.g., `size`, `width`, `height`, `padding`).
  - Adjust placement (e.g., `offset`, alignment modifiers).
- Modifiers are applied **outside‑in** during measurement and **inside‑out** during placement:

```kotlin
Box(
    Modifier
        .size(200.dp)      // outermost layout modifier
        .padding(16.dp)    // inner layout modifier
        .background(Color.Red) // non-layout modifier
)
```

Layout-affecting modifiers include:

- `size`, `width`, `height`
- `padding`
- `fillMaxWidth` / `fillMaxHeight` / `fillMaxSize`
- `offset`

Non-layout modifiers participate only in drawing, input, semantics, etc.

Advanced implementations can use the `Modifier.Node`/`LayoutModifierNode` APIs to intercept measurement and placement at the node level while reusing modifier nodes across recompositions.

## Efficiency and invalidation

Compose is designed to minimize work:

- **Recomposition skipping**:
  - The runtime tracks which state each composable reads.
  - If inputs and observed state do not change, a composable can be skipped entirely.
- **Per-phase invalidation**:
  - State read during composition triggers recomposition when it changes.
  - State read only during measurement triggers remeasure (and relayout/redraw as needed).
  - State read only during placement or drawing can trigger only relayout/redraw.
- **Size caching**:
  - Layout nodes can cache measurement results for given constraints and reuse them when constraints/content are unchanged.

More advanced APIs like `SubcomposeLayout`, intrinsic measurements, and Lookahead add extra passes when needed (for lazy lists, pre-measuring, or complex animations), but the core model aims to measure each parent/child pair once per frame under typical conditions.

