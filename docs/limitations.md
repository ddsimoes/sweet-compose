# Sweet Compose — Limitations

This is the **single source of truth** for what Sweet Compose does *not* yet do, what it
does by design, and what is planned. It consolidates facts that are otherwise scattered
across the README component table, the drawing guide, and the roadmap docs. Every entry is
grounded in the current source; the per-API KDoc and the referenced docs hold the detail.

When a limitation is resolved, update this file *and* the relevant component doc together.

## How to read this document

Each entry is tagged with one status:

| Tag | Meaning |
|------|---------|
| **Partial** | API exists and core behavior works; some parameters or behavior are ignored or approximated. |
| **Shim / no-op** | API compiles (source-compatible) but does not implement its advertised behavior. |
| **Not implemented** | API is absent. Calling it is a compile error, not a silent degradation. |
| **By design** | Intentional and unlikely to change — reflects the SWT-first philosophy, not a gap to close. |
| **Planned** | Accepted direction in the roadmap; not started or in progress. |

Runtime visibility: with JVM assertions enabled (`-ea`), `SweetDebugger` logs whenever a
shim/no-op API is invoked or a partial API receives an ignored parameter with a non-default
value. Use this to detect silent degradation in your own code.

---

## Platform and environment

| Area | Status | Detail |
|------|--------|--------|
| Desktop-only | **By design** | Targets SWT desktop applications. No mobile or web target. |
| x86_64 only | **By design** | The build resolves only x86_64 SWT artifacts (`win32.x86_64`, `gtk.linux.x86_64`, `cocoa.macosx.x86_64`). The dependency substitution in `sweet/build.gradle.kts` has no `aarch64`/`arm64` branch, so native ARM hosts (e.g. Apple Silicon) are not wired up. |
| SWT version pinned | **By design** | SWT 3.108.0. Behavior can vary across SWT/GTK/OS releases. |
| Java 11+ | **By design** | Set via `jvmToolchain(11)`. |
| Single UI thread | **By design** | All SWT widget access must happen on the SWT UI thread. `Dispatchers.Main` is backed by `kotlinx-coroutines-swt`. Background work must marshal back. See `architecture/threading-and-coroutines.md`. |
| No published artifact | **Planned** | There is no Maven publication configuration. Sweet is consumed by building from source. Publishing is deferred (roadmap "Later" phase). |

---

## Component fidelity (Material3)

Sweet prioritizes native SWT look-and-feel over pixel-perfect Material parity (SWT-first
mode). This section is the authoritative summary of current user-visible component gaps; the
README component table is only a quick map, and the per-API KDoc holds exact
parameter-level detail.

### Shim / no-op (compiles, does not behave as advertised)

- `Slider` — renders as a `Text` label (`"Slider: N%"`); not interactive. (`material3/Slider.kt`)
- `CircularProgressIndicator` — rendered as an indeterminate horizontal SWT `ProgressBar`;
  no circular presentation yet. (`material3/ProgressIndicator.kt`)
- Button variants — `OutlinedButton`, `TextButton`, `ElevatedButton`,
  `FilledTonalButton`, `FloatingActionButton` all alias `Button` with no variant styling. (`material3/Button.kt`)
- `menuAnchor()` — no-op.
- `mutableIntStateOf` / `mutableFloatStateOf` — boxed shims over the standard mutable state.

### Partial (core works, some behavior ignored)

- `Button` / `Tab` content — use text-capture patterns: a single `Text` routes its string to
  the native SWT label. Non-Text content is silently dropped; `Tab.icon` is ignored.
  (`material3/Button.kt`, `material3/Tab.kt`)
- `Text` — `fontSize`, `fontStyle`, and `fontWeight` apply; `textAlign` is ignored.
  (`material3/Text.kt`)
- `TextField` / `OutlinedTextField` — controlled value, `placeholder`, `isError`, `readOnly`,
  `singleLine`, `label`/`leadingIcon`/`trailingIcon`, and `PasswordVisualTransformation` work.
  Not honored: exact `maxLines`/`minLines` line-count clamping (SWT `Text` exposes no per-line
  clamp; these select single- vs multi-line rendering, fixed at widget creation because SWT
  styles are immutable), general `visualTransformation`, and floating-label animation.
  `OutlinedTextField` has its own `Card` wrapper and is not an alias of `TextField`. (`material3/TextField.kt`)
- `RadioButton` — real SWT `RADIO` behavior works, but `colors` and `interactionSource` are
  ignored. (`material3/Checkbox.kt`)
- `TopAppBar` — layout renders, but `colors` are ignored. (`material3/AppBar.kt`)
- `Surface` — background color and min-constraint propagation work; `shape`, elevation,
  `border`, and `contentColor` propagation are ignored. (`material3/Surface.kt`)
- `Scaffold` — uses a simple `Column`; `content` receives empty `PaddingValues`, the FAB
  renders inline instead of overlaying content, and `FabPosition` has no effect.
  (`material3/Scaffold.kt`)
- `AlertDialog` — renders as a native SWT `MessageBox`; composable `title`/`text`/`icon`
  content and visual styling params are not rendered. (`material3/AlertDialog.kt`)
- `Modifier.background(shape)` — the color works on all widgets; the `shape` parameter is
  accepted for source compatibility but ignored by the layout system. (`foundation/Background.kt`)
- `ScrollableTabRow` — renders, but is not actually scrollable.
- `alignBy` / `AlignmentLine` — local per-child blocks and baseline lines are honored; one
  known pixel-level test failure exists, and custom `AlignmentLine` propagation/merging
  between arbitrary parent/child layouts is not implemented. See `architecture/runtime-and-layout.md`.

---

## Drawing and graphics

Highest fidelity is on explicit surfaces (`Canvas`, `Image`, `Icon`); container decoration is
best-effort; leaf widgets stay native-only. Full per-feature status is in
`guides/drawing.md`. Key gaps:

| Feature | Status | Detail |
|------|--------|--------|
| `LinearGradient` with >2 color stops | **Partial** | SWT `Pattern` supports exactly two colors; multi-stop gradients collapse to first + last only (intermediate stops dropped). 2-stop gradients render correctly. |
| Blend modes | **Partial** | `SrcOver` is native. On **Canvas/Image/Icon** (Tier 1, where an offscreen image exists) most Porter-Duff and common separable modes — `DstOver`, `SrcIn`/`SrcOut`, `DstIn`/`DstOut`, `Plus`, `Multiply`, `Screen`, `Overlay` — are software-composited per pixel. Remaining modes, and **all** non-SrcOver modes on container/leaf surfaces (no offscreen image), degrade to `SrcOver`. |
| `drawImage(..., colorFilter)` | **Partial** | Image placement/scaling/alpha work, but `colorFilter` on SWT image draws is logged and ignored. |
| `ColorFilter.ColorMatrix`, `ColorFilter.Lighting` | **Shim / no-op** | Logged and ignored. |
| `Path.op(...)` | **Partial** | `Union` only. |
| `Modifier.paint(painter, …)` | **Shim / no-op** | Registers a marker modifier only; modifier-driven painter drawing is not wired yet. |
| `Modifier.drawWithContent` | **Partial** | `drawContent()` is a no-op; only the "behind" layer renders. |
| `Modifier.drawWithCache` | **Partial** | Forwards to `drawBehind`; cache lifetime/invalidation semantics are ignored. |
| Rich text in `Canvas` | **Partial** | `drawText` is basic — no font styling, measuring, or paragraph layout. |
| `GraphicsLayer` / hardware acceleration | **Not implemented** | Not available through SWT `GC`. |
| Skia / Skiko backend | **Planned** | Interface seam exists; no Skia implementation. SWT `GC` is the only backend. |

---

## Animation

`Animatable` (real spring physics via `SpringSimulation`, tween paced by the frame clock),
`spring()`/`tween()`, standard easing functions, and `animateFloatAsState`/`animateDpAsState`
are wired and tested. What is missing:

| Feature | Status | Detail |
|------|--------|--------|
| `Transition`, `rememberInfiniteTransition` | **Not implemented** | |
| `AnimatedContent`, `Crossfade`, `AnimatedVisibility`, `animateContentSize` | **Not implemented** | |
| `animateScrollTo` | **Not implemented** | Gated on the `AnimationSpec`/`DecayAnimationSpec` work. |
| Animation driving widget position in tests | **Partial** | `animateFloatAsState`/`animateDpAsState` is validated by `AnimateAsStateTest`, but driving SWT widget position changes through the animation API inside the autoSWT test context has a known frame-clock/dispatcher interaction issue (follow-up F-6). Production widget mutation works; the gap is specifically in the test harness. |

---

## Lists and scrolling

| Feature | Status | Detail |
|------|--------|--------|
| `LazyColumn` / `LazyRow` virtualization | **Partial** | Items are composed **eagerly** into a `Column`/`Row` inside a `ScrollViewport`. There is no viewport virtualization, so very large lists get no memory/perf benefit. `state` and `reverseLayout` are wired; `contentPadding` and `userScrollEnabled` are currently ignored. |
| Scroll fling / gesture tracking | **Partial** | `isScrollInProgress` is best-effort (true during scrollbar thumb drags); fling/gesture tracking is not implemented. |
| `animateScrollTo` | **Not implemented** | See Animation above. |

`ScrollState` and `LazyListState` (Phase A) are real: `scrollTo`/`scrollToItem` are
suspend functions, `firstVisibleItemIndex`/`visibleItemsInfo` are computed from viewport
scroll vs. child bounds. See `architecture/runtime-and-layout.md` → "Scroll and interop".

---

## Testing and determinism

| Area | Status | Detail |
|------|--------|--------|
| Suite determinism | **Partial** | The root cause of run-to-run flakiness (shared singletons, missing shell disposal) is fixed; the suite is deterministic per machine. Remaining cross-machine variance is **environment sensitivity** (host fonts/DPI/GTK theme → different `computeSize()` pixels). A red run on one machine may not reproduce on another. Full analysis: `roadmap/test-determinism.md`. |
| Display required | **By design** | AutoSWT must create an SWT `Display`; without one every UI test fails with `UninitializedPropertyAccessException`. On headless machines/CI run under `xvfb-run -a ./gradlew test`. |
| Pixel-level assertions | **Partial** | Layout assertions depend on font metrics and DPI. Use the layout-assertion API with strict tolerance; widen tolerance only for inherently approximate checks (centering, edge proximity). |

---

## Long-term direction

Sweet's long-term plan (`roadmap/long-term-direction.md`) defines two modes and six phases.
Today only SWT-first mode and Phase 1 (drawing foundation) are substantially in place:

| Phase | Status |
|-------|--------|
| Phase 1 — Drawing foundation (SWT `GC` backend) | Largely done (D0–D3); D4 optimizations (dirty-region redraws, offscreen caching, benchmarks) pending. |
| Phase 2 — Style/Theme infrastructure | **Not started.** No `SweetThemeConfig`; `MaterialTheme.colorScheme` does not yet drive widget colors. |
| Phase 3 — Material-like styling for selected components | **Not started.** Guarded by `MaterialLikeProfile`; this is where the button-variant and `Slider` shims would be resolved. |
| Phase 4 — Custom Canvas-based components | **Not started.** |
| Phase 5 — Skia/Skiko backend | **Not started.** Interface seam only. |
| Phase 6 — Compose-first mode | **Not started.** |

**Drop-in replacement is an aspiration, not the current state.** Many common composables
work end-to-end (see the README "Implemented" list), but the shim/partial set above means an
arbitrary Compose Desktop app will not run unmodified today.

### By-design constraints unlikely to change

- **Native look-and-feel over pixel parity.** SWT widgets are used as-is in SWT-first mode;
  visuals follow the native SWT/GTK/Windows/Cocoa theme, not Material rendering.
- **Immutable SWT styles.** Some widget properties (e.g. single- vs multi-line `Text`) are
  fixed at creation and cannot change on recomposition — they are decided once at factory time.
- **No deprecated-API baggage.** Sweet has zero legacy users, so deprecated MPP aliases and
  compatibility shims are deleted rather than carried (per `CLAUDE.md`).
