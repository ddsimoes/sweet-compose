# Sweet Architecture – SWT Interop

Sweet is designed for deep interoperability with SWT. This document summarizes the main interop patterns.

## Embedding Compose in SWT

- Existing SWT applications can host Sweet content inside any `Composite`.
- A typical pattern:
  - Create a `Composite` in an existing SWT view or part.
  - Bind a Sweet composition to that composite using an interop helper (see `sweet` sources).
  - Use Sweet composables (`Column`, `Row`, `Text`, `Button`, etc.) inside that hosted area.

## Embedding SWT widgets in Compose

- Complex SWT widgets can be embedded as leaf nodes in Sweet layouts (e.g., `Browser`, `StyledText`).
- The interop composable:
  - Creates the widget inside a Sweet-managed composite.
  - Lets `SweetLayout` handle measurement and placement.
  - Allows modifiers such as `size`, `padding`, `fillMaxSize` to participate in layout.

For concrete code examples, refer to:

- Sample code under `sample/`.
