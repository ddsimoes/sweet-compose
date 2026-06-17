# Sweet - Jetpack Compose for SWT

Sweet is a declarative UI framework that maps Jetpack Compose onto SWT (Standard Widget Toolkit). It lets you write desktop UIs with Compose syntax while running on the SWT widget set.

## Features

- **Declarative UI**: Build components with Compose-style syntax
- **Reactive state**: UI updates when state changes
- **SWT integration**: Works with existing SWT widgets and libraries
- **Coroutine support**: Dispatchers for SWT threading
- **Layout system**: Row/Column/Box layouts with modifier support
- **Type safety**: Kotlin's type system applies at compile time
- **Debugging tools**: Utilities for inspecting composition and layout

## Quick Start

### Prerequisites

- Kotlin 2.1+
- JVM 11+
- SWT library (Eclipse SWT)

### Basic Example
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "My Sweet App",
        ) {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    var counter by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "Sweet Counter Example",
            modifier = Modifier.padding(10.dp)
        )

        Text(
            text = "Count: $counter",
            modifier = Modifier.padding(10.dp)
        )

        Button(
            onClick = { counter++ },
            modifier = Modifier.padding(10.dp)
        ) {
            Text("Increment")
        }
    }
}
```

## Project Structure

```
sweet-compose/
├── sweet/                          # Core Sweet toolkit
│   ├── src/main/kotlin/
│   │   ├── androidx/compose/       # Compose API adaptations
│   │   └── io/github/ddsimoes/sweet/
│   │       ├── internal/           # Internal implementation
│   │       ├── layout/             # Layout system
│   │       │   ├── SweetLayout.kt  # Unified layout manager
│   │       │   ├── delegate/       # Layout delegates (Row, Column, Box)
│   │       │   ├── LayoutSpec.kt   # Layout specifications
│   │       │   ├── Alignment.kt    # Alignment utilities
│   │       │   └── Arrangement.kt  # Arrangement utilities
│   │       ├── debug/              # Debugging utilities
│   │       └── drawing/            # SWT Canvas/GC drawing backend
├── sample-sweet/                   # Sample apps (Sweet runtime)
├── sample-jetbrains/               # Same samples compiled against JetBrains Compose
├── kotlinx-coroutines-swt/         # SWT coroutine dispatcher
└── 3rdparty/                       # Vendored upstream (non-tracked checkout)
```

Sweet implements a declarative UI framework with a layout system following Compose principles:

### Layout Architecture
- **SweetLayout**: Unified layout manager that handles all container types
- **Layout Delegates**: Pluggable strategies for Row, Column, and Box layouts
- **Constraint System**: Compose-style min/max width/height constraints
- **Single Measurement Pass**: Eliminates double-measurement issues

### Composition Runtime
- **EnhancedSWTComposition**: Manages composition lifecycle and SWT integration
- **FixedSWTNodeApplier**: Handles the node tree that maps to SWT widgets
- **Frame Clock**: Coordinates recomposition with SWT's event loop

### Threading Model
- **Main Dispatcher**: All UI operations happen on SWT's UI thread
- **Coroutine Integration**: Dispatchers for background work
- **Thread Safety**: Marshals updates to the UI thread

### Component System
- **Composables**: Functions that describe UI structure
- **Modifiers**: Chainable decorators for styling and layout
- **State Management**: Reactive state with automatic recomposition

## Available Components

Sweet provides a Compose-compatible API surface backed by SWT widgets. For a quick summary,
see the tables below; `docs/limitations.md` is the authoritative limitations index.

**Implemented** (full end-to-end): `Column`, `Row`, `Box`, `Card`, `Spacer`, `Checkbox`,
`Switch`, `Icon`, `IconButton`, `HorizontalDivider`, `SWTContainer`, `MaterialTheme`, all
layout modifiers (`padding`, `fillMax*`, `size`, `weight`, `offset`, `sizeIn`, `aspectRatio`),
scroll modifiers, `TabRow`, window management, menu bar, and all data types (`Color`, `Dp`,
`TextUnit`, etc.).

**Partial** (core works, some parameters or slots are ignored): `Button` / `Tab` (text-capture
only; non-Text content dropped, `Tab.icon` ignored), `RadioButton` (`colors` /
`interactionSource` ignored), `TopAppBar` (`colors` ignored), `Surface`
(`shape`/elevation/`border`/`contentColor` ignored), `Scaffold` (empty `PaddingValues`, inline
FAB), `AlertDialog` (native `MessageBox`; rich slots not rendered), `Text`
(`fontSize`/`fontStyle`/`fontWeight` work; `textAlign` ignored), `TextField` /
`OutlinedTextField` (placeholder, `isError`, `readOnly`, `singleLine`, password via
`PasswordVisualTransformation`, label/icons implemented; `maxLines`/`minLines` select single-
vs multi-line mode rather than exact line clamps; general `visualTransformation` deferred),
`LazyColumn` / `LazyRow` (eager, no virtualization; `contentPadding` and `userScrollEnabled`
currently ignored), `background(shape)` (shape ignored), `Canvas` / `Image` (documented
GC-backend gaps, including ignored image `colorFilter` and unwired `Modifier.paint`; see
`docs/guides/drawing.md`), `ScrollableTabRow` (not actually scrollable), `alignBy` (one known
pixel-level test failure).

**Shim / no-op** (compiles but doesn't implement advertised behavior): `Slider` (renders as
text), `CircularProgressIndicator` (renders as an indeterminate horizontal bar), all button
variants (`OutlinedButton`, `TextButton`, `ElevatedButton`, `FilledTonalButton`,
`FloatingActionButton` — all alias `Button`), `menuAnchor()`, `mutableIntStateOf` /
`mutableFloatStateOf` (boxed shims).

When assertions are enabled (`-ea` JVM flag), debug logging surfaces ignored/no-op behavior at runtime.

## Development

### Building the Project

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Run sample applications
./gradlew :sample-sweet:run
```

### Debugging

Enable debugging to see detailed logs:

```kotlin
import io.github.ddsimoes.sweet.debug.SweetDebugger

fun main() {
    SweetDebugger.enable()
    // Your app code
}
```

## Documentation

- **Overview**: `docs/overview.md`
- **Limitations**: `docs/limitations.md` — single source of truth for current and long-term limitations
- **Architecture**: `docs/architecture/runtime-and-layout.md`
- **Development & Contributing**: `docs/development/contributing.md`
- **Testing Framework**: `docs/development/testing.md`
- **Roadmap & Status**:
  - `docs/roadmap/long-term-direction.md` — long-term evolution plan
  - `docs/roadmap/drawing-foundation.md` — drawing/graphics foundation plan
  - `docs/roadmap/test-determinism.md` — test determinism analysis
- **CLAUDE.md**: Canonical agent and contributor guidelines
- **Samples**: See `sample-sweet/src/main/kotlin/` for complete examples

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes with tests
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Add tests for new functionality using AutoSWT framework
- Update documentation for API changes
- Ensure thread safety for SWT operations
- Use Layout Assertion API for testing UI layouts
- Test with `./gradlew test` before submitting

## Roadmap

- [x] **Modern Layout System** - Unified SweetLayout with delegate pattern
- [x] **Comprehensive Testing** - AutoSWT framework with Layout Assertion API
- [ ] More UI components (Tables, Trees, Menus)
- [x] **Animation support** — `Animatable` with `spring()`/`tween()` (real spring physics via `SpringSimulation`); easing parity validated; `animateFloatAsState`/`animateDpAsState` drive recomposition frame-by-frame (`Animatable.value` is snapshot-backed). `Transition`/`rememberInfiniteTransition` deferred.
- [~] **Material Design fidelity** — Several Material3 APIs remain shims or partials (`Slider`, button variants, `Surface`, `Scaffold`, `AlertDialog`). `TextField`/`OutlinedTextField` support placeholder, `isError`, `readOnly`, `singleLine`, password via `PasswordVisualTransformation`, label/icons, and single- vs multi-line mode selection via `maxLines`/`minLines`; general `visualTransformation` is still deferred.

## Known Limitations

The authoritative breakdown of current and long-term limitations — partial APIs, shims/no-ops,
by-design constraints, and what is planned — lives in [`docs/limitations.md`](docs/limitations.md).
Highlights:

- Several Compose APIs are compatibility shims or partial implementations — `Slider`,
  `CircularProgressIndicator`, button variants, `Surface`, `Scaffold`, `AlertDialog`, and more.
- SWT-specific threading requirements (all UI operations must run on the SWT UI thread).
- `Transition` and `rememberInfiniteTransition` animation APIs not yet implemented (`Animatable`, `spring`/`tween`, easing, and `animateFloatAsState`/`animateDpAsState` are wired).
- Platform-specific SWT dependencies; desktop-only (no mobile/web support).
- `LazyColumn`/`LazyRow` compose all items eagerly — no viewport virtualization; `contentPadding`
  and `userScrollEnabled` are currently ignored.
- `TextField` `maxLines`/`minLines` select single- vs multi-line SWT rendering; SWT `Text`
  exposes no per-line-count clamp, so exact visible line counts are layout-driven.

## License

This project is licensed under the [LICENSE](LICENSE) file in the root directory.

## Support

- **Issues**: Report bugs on GitHub Issues
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: Check the README files in each module

## Related Projects

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Original Compose framework
- [Eclipse SWT](https://www.eclipse.org/swt/) - Standard Widget Toolkit
- [Compose Desktop](https://github.com/JetBrains/compose-multiplatform) - Official Compose desktop
