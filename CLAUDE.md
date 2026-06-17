# Sweet Compose Developer Guidelines

## Project Overview

Sweet is a Kotlin UI toolkit that brings Jetpack Compose declarative UI patterns to desktop applications using SWT (Standard Widget Toolkit). It bridges Compose Runtime with SWT widgets, enabling reactive desktop UIs with familiar Compose APIs.

**Tech Stack:**
- **Language:** Kotlin 2.1.20
- **UI Framework:** Jetpack Compose Runtime 1.8.1
- **Desktop Toolkit:** Eclipse SWT 3.108.0
- **Build System:** Gradle with Kotlin DSL
- **Testing:** JUnit Jupiter 5.8.2
- **Coroutines:** Kotlinx Coroutines 1.10.2
- **Java Version:** 11+ (via jvmToolchain)

## Documentation Map for Agents

When working on this repository, use the following docs as your main references:

- **Overview and architecture**
  - `docs/overview.md`
  - `docs/architecture/runtime-and-layout.md`
  - `docs/architecture/threading-and-coroutines.md`
  - `docs/architecture/interop.md`
- **Development and testing**
  - `docs/development/contributing.md`
  - `docs/development/testing.md`
- **Test determinism (priority foundation issue)**
  - `docs/roadmap/test-determinism.md` (why the suite is non‑deterministic, all collected evidence, and the plan toward a final solution — read before "fixing" any flaky layout test)
- **Current state, direction, and execution plan**
  - `docs/roadmap/long-term-direction.md` — long‑term evolution plan, execution modes, and profiles
  - `docs/roadmap/test-determinism.md` — test determinism analysis and root cause fix
  - `docs/roadmap/drawing-foundation.md` — drawing/graphics foundation plan
- **Drawing and graphics**
  - `docs/roadmap/drawing-foundation.md` (**source of truth for drawing work**: faithful Compose draw API — `DrawScope`/`Brush`/`Path`/draw modifiers/`Canvas`/`Image`/vectors — over a tiered "decoration, not replacement" SWT bridging model. Read before touching any Canvas/Image/draw-modifier/graphics code.)
- **Limitations (what is partial, shim, by design, or planned)**
  - `docs/limitations.md` — single source of truth for current and long-term limitations. Read before assuming a Compose API works end-to-end, and update it when you resolve a limitation.

The older standalone design/plan documents have been consolidated into the `docs/` structure and removed. For historical context, use the Git history and the roadmap docs under `docs/roadmap/`.

### Project Objectives and Directions

Sweet aims to be a **drop-in replacement** for `androidx.compose.ui.*` packages, enabling existing Compose Desktop applications to run without code modification while providing native desktop look-and-feel.

**Core Goals:**
- **API Compatibility**: Existing `@Composable` functions work as-is, including `remember`, `LaunchedEffect`, state management, etc.
- **Native Look-and-Feel**: Prioritize native SWT appearance and behavior over pixel-perfect Compose behavior matching  
- **Desktop-Focused**: Target compatibility with Compose UI for Desktop (JetBrains), supporting 80% of common features that make sense for desktop applications
- **SWT Integration**: Use native SWT components as the foundation, creating missing components when viable and SWT-compatible

**Design Philosophy:**
- Follow JetBrains Compose Desktop's Android-to-desktop mapping strategy
- When SWT widgets lack Compose equivalents, implement them if they don't conflict with SWT architecture
- Maintain full Compose Runtime compatibility while delivering native desktop UX
- Focus on features shared between Android and desktop Compose, prioritizing desktop-appropriate functionality

## MPP Compose Desktop Reference Implementation

The canonical reference for Sweet's public API surface is the Jetpack Compose Multiplatform
Desktop source at `3rdparty/compose-multiplatform-core/`. This is **not optional** — it is
the authoritative guide for every API decision.

### Hard Rules

- **Package structure MUST match MPP.** If MPP defines `DrawScope` in
  `androidx.compose.ui.graphics.drawscope`, Sweet defines it there too — never at the
  top-level `graphics` package. Type aliases pointing backward are gambiarras; fix the root
  cause by moving the symbol.
- **File organization SHOULD mirror MPP.** MPP uses one file per class/component. A 2,000-line
  `material3.kt` with 20 unrelated composables is a container-file anti-pattern. Split it.
- **Method signatures MUST match MPP.** Same parameter types, same default values, same
  overloads. If MPP has `withTransform(transformBlock, drawBlock)` (two-lambda form), Sweet
  must support that exact signature — a single-lambda `withTransform(block)` is an API drift.
- **The sample code (`sample-sweet/`) MUST compile and render identically against both Sweet
  and MPP Compose.** The `sample-jetbrains/` module runs the same composables against the
  real JetBrains Compose runtime. Any difference in render output or compilation is a bug.
- **Deprecated MPP APIs are irrelevant for Sweet.** Sweet has zero legacy users — no
  deprecation grace period is needed. Delete deprecated type aliases, compatibility shims,
  and `@Deprecated` overloads. The API should be clean *now*, not carry MPP's historical
  baggage.

### How to Validate Against MPP

When introducing or modifying a public API:
1. **Find the corresponding file** in `3rdparty/compose-multiplatform-core/compose/`
   (typically under `ui/ui-graphics/src/commonMain/`, `material3/material3/src/commonMain/`,
   or `foundation/foundation/src/commonMain/`).
2. **Compare every public symbol**: class/interface name, package, method signatures,
   parameter types, default values, annotations (`@Composable`, `@Stable`, `@DslMarker`).
3. **Check the `sample-jetbrains/` counterpart** — if the JetBrains sample compiles with a
   different API than Sweet's sample, Sweet is wrong.
4. **Run the behavioral parity gate** — `:sample-sweet:test --tests "SharedSampleParityTest"`
   — to exercise shared composables under Sweet's runtime with real assertions. This is the
   check that closes the "compiles ≠ works" gap. See `sample-sweet/src/test/kotlin/SharedSampleParityTest.kt`
   for details. (A BCV signature/shape guard was evaluated and deferred — the behavioral
   parity gate already catches the API-drift problems that matter.)


## Build and Development

### Prerequisites
- Java 11 or higher
- Gradle (wrapper included)

### Building the Project
```bash
# Build all modules
./gradlew build

# Build specific modules
./gradlew :sweet:build
./gradlew :sample-sweet:build
./gradlew :kotlinx-coroutines-swt:build
```

### Testing

**Validation runs at the ROOT and ALL modules must pass — not just `:sweet`.**
A change is only "green" when the root `./gradlew test` passes, including `:sample-sweet`
(and `:kotlinx-coroutines-swt`). `:sample-sweet` is a real part of the gate:
its tests exercise full apps (TodoMVC, KitchenSink, scroll) and catch integration/layout
regressions that the `:sweet` unit tests miss.

```bash
# Authoritative validation — every module must pass
./gradlew test

# Run even if one module fails, so you see the COMPLETE failure set across all modules
./gradlew test --continue

# Module-specific runs are for fast iteration ONLY, never the final gate
./gradlew :sweet:test
./gradlew :sample-sweet:test

# Verbose output
./gradlew test --info
```

**UI tests require a display.** AutoSWT must create an SWT `Display`; with no display the
`lateinit display` is never set and EVERY UI test fails with
`kotlin.UninitializedPropertyAccessException` (this is an environment failure, not a real
test failure). On a headless machine/CI, run under a virtual framebuffer:

```bash
xvfb-run -a ./gradlew test --continue
```

**Layout tests can be environment-sensitive.** Pixel-level assertions depend on font metrics,
DPI and the GTK/OS theme, so the failing set may differ between machines. When reporting or
fixing failures, capture the exact failing test names and the environment (`echo $DISPLAY`,
OS, whether `xvfb-run` was used).

### Agent instructions:
- To test or run, always use Gradle. Never use java, javac or kotlinc directly.
- To validate, always run tests and not samples.
- Validate at the ROOT: `./gradlew test` (all modules). `:sample-sweet` must pass too — a green
  `:sweet` alone is NOT a passing build. Use `--continue` to see every module's failures at once.
- On a headless environment, run UI tests under `xvfb-run -a ./gradlew test` (see Testing above).
- Use AutoSWT to test the UI behavior
- Always assume that the test is correct - unless it's obvious it's not
- NEVER, EVER, change a failing test case without getting permission from me, explaining why the test is wrong.

## Architecture Guidelines

### Core Architecture Pattern
Sweet implements a tree-based composition system with a modern layout architecture:

**Composition System:**
1. **SWTNode**: Abstract base representing UI elements, each wrapping an SWT Widget
2. **SWTControlNode**: Specialized node for SWT Control widgets with modifier support
3. **FixedSWTNodeApplier**: Manages composition tree, handles insertions/removals/moves
4. **EnhancedSWTComposition**: Main composition handler integrating with SWT event loop

**Modern Layout Architecture:**
1. **SweetLayout**: Unified layout manager that delegates measurement and placement
2. **LayoutDelegate**: Strategy pattern for different layout behaviors (Row, Column, Box)
3. **LayoutSpec**: Clean specification of layout behavior attached to composables
4. **SweetMeasurable**: Interface for components that support Sweet's constraint-based measurement

### Layout System Architecture

Sweet implements a modern, unified layout system based on Compose principles:

**Core Components:**
- **SweetLayout**: Single layout manager that handles all container types (Row, Column, Box)
- **LayoutDelegate**: Pluggable strategies for different layout behaviors
  - `RowDelegate`: Horizontal layout with arrangement and weight support
  - `ColumnDelegate`: Vertical layout with arrangement and weight support  
  - `BoxDelegate`: Stack-based layout with alignment support
- **LayoutSpec**: Specifies which delegate and configuration to use
- **Constraints**: Compose-style min/max width/height constraints
- **SweetMeasurable**: Interface for measurable UI components

**Key Features:**
- **Single measurement pass**: No double-measurement issues
- **Compose compatibility**: Full support for `Modifier.weight()`, `Arrangement`, `Alignment`
- **Predictable positioning**: Children always stay within parent bounds
- **Mathematical precision**: Correct weight distribution and spacing
- **Clean separation**: Layout behavior separate from component implementation

### Threading Model
- All UI operations must run on SWT's main thread
- Use `Dispatchers.Main` (backed by SWTMainCoroutineDispatcher) for UI operations
- Snapshot state management is integrated with SWT event loop

## Development Best Practices

### Code Organization
- Place UI components in the **exact same package** as MPP Compose defines them.
  Use `3rdparty/compose-multiplatform-core/` as the authoritative reference.
- One file per class/component; avoid container files that bundle unrelated types.
- Keep internal implementation details in `io.github.ddsimoes.sweet.internal`.
- Use meaningful names that reflect SWT widget mapping.
- Follow Compose naming conventions for composable functions.

### Platform Considerations
- SWT dependencies are automatically resolved based on runtime OS
- Supported platforms: Windows (x86_64), Linux (x86_64), macOS (x86_64)
- Test on target platforms as SWT behavior can vary

### Error Handling
- Always handle SWT widget disposal properly
- Use try-catch blocks around SWT operations that might fail
- Implement proper cleanup in composition disposal

## Testing Guidelines

### Unit Testing
- Write tests for business logic and state management
- Mock SWT dependencies when testing non-UI code
- Use JUnit Jupiter for test structure

### Integration Testing
- Test complete UI flows where possible
- Verify proper SWT widget creation and configuration
- Test modifier application and layout behavior

### Writing Sweet Compose UI Tests

Sweet Compose uses the `autoSWT` testing framework for UI tests. AutoSWT is now available as a standalone library in the [auto-swt repository](../auto-swt). Here's the pattern for creating tests:

#### Basic Test Structure
```kotlin
@Test
fun testMyComponent() {
    autoSWT {
        testShell(width = 400, height = 300) {
            val composite = Composite(this, SWT.NONE)
            composite.layout = FillLayout()
            composite.embedCompose {
                MyTestComponent()
            }
        }.test { shell ->
            // Test logic here - no waitForIdle needed
            
            // Find and interact with widgets
            val textFields = shell.findAll<Text> { true }
            assertEquals(2, textFields.size, "Should have 2 text fields")
            
            runOnSWT {
                SweetDebugger.logWidgetTree(shell)
            }
            
            shell.saveSVG()
            shell.saveScreenshot()
        }
    }
}
```

#### Required Imports for UI Tests
```kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.integration.embedCompose
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.sample.*
# AutoSWT imports (from separate auto-swt repository)
# Add this dependency to your build.gradle.kts:
# implementation("io.github.ddsimoes:auto-swt:1.0.0")
import io.github.ddsimoes.autoswt.AutoSWT
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
```

#### Creating Test Composables
```kotlin
@Composable
private fun MyTestComponent() {
    var textInput by remember { mutableStateOf("Hello") }
    var numberInput by remember { mutableStateOf("3") }
    
    SweetDebugger.log("MyTestComponent", "Recomposing - textInput='$textInput', numberInput='$numberInput'")
    
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("Text:", modifier = Modifier.padding(end = 8.dp))
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f)
            )
        }
        
        val count = numberInput.toIntOrNull() ?: 0
        repeat(count) { index ->
            Text("$textInput ${index + 1}", modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}
```

#### Testing Interactive Elements
```kotlin
// Find widgets
val textFields = shell.findAll<Text> { true }
val buttons = shell.findAll<Button> { it.text == "Click Me" }
val labels = shell.findAll<Label> { it.text.contains("Expected") }

// Interact with widgets
textFields[0].clearText()
textFields[0].typeText("New Text")
buttons[0].doSelect()

// Verify changes immediately - no waiting needed
val updatedLabels = shell.findAll<Label> { it.text.contains("New Text") }
assertEquals(1, updatedLabels.size, "Should show updated text immediately")
```

#### Layout Assertion API

Sweet Compose provides a comprehensive layout assertion API for testing UI component positioning and sizing. Use this API instead of manual `visibleBounds()` calculations.

**Basic Usage:**
```kotlin
// Single control assertions - fluent API
button.assertLayout()
    .isVisible()
    .hasMinSize(width = 100, height = 30)
    .isRightOf(label, gap = 8)
    .isWithin(container)

// Multiple control assertions
listOf(button1, button2, button3).assertLayout()
    .areAllVisible()
    .areArrangedInRow(minGap = 8)
    .areRightAlignedWith()
    .fitWithin(toolbar)
```

**Common Layout Test Patterns:**

**Visibility & Sizing:**
```kotlin
// Check visibility and minimum dimensions
itemLabels.assertLayout().areAllVisible()
itemLabels.forEach { it.assertLayout().hasMinSize(height = 12) }

// Exact sizing with tolerance only when needed
logo.assertLayout().hasSize(64, 64)
title.assertLayout().hasMinSize(width = 200)
```

**Positioning & Alignment:**
```kotlin
// Relative positioning with gaps
label.assertLayout().isLeftOf(button, gap = 8)
button.assertLayout().isRightOf(label).doesNotOverlap(label)

// Edge alignment - strict by default
deleteButtons.assertLayout().areRightAlignedWith()
menuItems.assertLayout().areLeftAlignedWith()

// Containment
allControls.assertLayout().fitWithin(container)
button.assertLayout().isWithin(row)
```

**Layout Patterns:**
```kotlin
// Sequential layout
columnItems.assertLayout()
    .areArrangedInColumn(minGap = 5)
    .areLeftAlignedWith()
    .doNotOverlap()

rowButtons.assertLayout()
    .areArrangedInRow(minGap = 10)
    .areTopAlignedWith()
    .haveEqualSpacing(expectedGap = 10)

// Center alignment
logo.assertLayout().isCenteredIn(header)
dialog.assertLayout().isCenteredIn(shell)
```

**Edge Positioning:**
```kotlin
// Near edges with distance tolerance
closeButton.assertLayout().isNearRightEdgeOf(titleBar, maxDistance = 5)
statusLabel.assertLayout().isNearBottomEdgeOf(window, maxDistance = 10)

// Filling containers
content.assertLayout().fillsHorizontally(container)
sidebar.assertLayout().fillsVertically(mainWindow)
```

**Before/After Comparison:**

**❌ Before (manual assertions):**
```kotlin
val lblVB = label.visibleBounds()
val btnVB = button.visibleBounds()
assertTrue(lblVB.width > 0 && lblVB.height > 0, "Label not visible")
assertTrue(btnVB.x >= lblVB.x + lblVB.width, "Button not right of label")
assertTrue(kotlin.math.abs(lblVB.y - btnVB.y) <= 2, "Not aligned")
```

**✅ After (fluent API):**
```kotlin
label.assertLayout().isVisible().isLeftOf(button)
button.assertLayout().isTopAlignedWith(label)
```

**Tolerance Guidelines:**

The layout assertion API uses strict defaults (tolerance = 0) to catch real layout bugs. Only use tolerance when absolutely necessary:

```kotlin
// ✅ Preferred - exact assertions catch real issues
buttons.assertLayout().areRightAlignedWith()  // tolerance = 0
label.assertLayout().isLeftOf(button, gap = 8)  // tolerance = 0

// ⚠️ Use sparingly - only when platform differences exist
centerTitle.assertLayout().isCenterAlignedHorizontallyWith(container, tolerance = 1)
closeButton.assertLayout().isNearRightEdgeOf(titleBar, maxDistance = 3)
```

**When tolerance might be needed:**
- **Center calculations**: Division by 2 can create 0.5px rounding differences
- **Platform differences**: Font rendering variations between OS
- **Edge proximity**: "Near" edge positioning is inherently approximate

**When to NOT use tolerance:**
- **Alignment**: Edge alignment should be pixel-perfect
- **Containment**: Controls should be fully within bounds
- **Visibility**: Either visible or not - no gray area
- **Exact positioning**: Specified coordinates should match exactly

#### Thread Safety in Tests - When to Use `runOnSWT`

The `runOnSWT` function ensures code runs on the SWT UI thread. Based on the AutoSWT implementation, here's when you need it:

**When `runOnSWT` is AUTOMATICALLY handled (you don't need to use it):**

```kotlin
// Widget finding - AutoSWT.find() and findAll() already wrap with runOnSWT
val textFields = shell.findAll<Text> { true }
val buttons = shell.findAll<Button> { it.text == "Click Me" }

// Widget interactions - All AutoSWT interaction methods wrap with runOnSWT
textField.typeText("Hello")    // Already wrapped
textField.clearText()          // Already wrapped
button.doSelect()             // Already wrapped
button.doDeselect()           // Already wrapped
control.click()               // Already wrapped
control.pressKey(13)          // Already wrapped

// Widget property access in find() predicates - Already on SWT thread
val matchingLabels = shell.findAll<Label> { it.text.matches(regex) }  // CORRECT
```

**When you MUST use `runOnSWT`:**

```kotlin
// Direct widget property access outside of find() predicates
val text = runOnSWT { label.text }
val isEnabled = runOnSWT { button.isEnabled }
val bounds = runOnSWT { control.bounds }

// Multiple widget operations together
val results = runOnSWT {
    labels.map { it.text }  // Multiple property access
}

// Widget creation and configuration
val newWidget = runOnSWT {
    val button = Button(parent, SWT.PUSH)
    button.text = "New Button"
    button
}
```

**Key Insight:** The `find()` and `findAll()` methods already execute their predicates on the SWT thread, so you don't need `runOnSWT` inside the matcher functions.

#### Test Patterns

**State Management Testing:**
```kotlin
// Test reactive state changes - SWT is single-threaded, changes are immediate
var state by remember { mutableStateOf("initial") }
// Change state and verify UI updates (no waitForIdle needed)
state = "updated"
val resultLabels = shell.findAll<Label> { it.text == "updated" }
assertEquals(1, resultLabels.size, "Should show updated state immediately")
```

**Input Validation Testing:**
```kotlin
// Test invalid input handling
numberField.clearText()
numberField.typeText("abc")
// No waitForIdle needed - SWT updates are synchronous
val resultLabels = shell.findAll<Label> { it.text.startsWith("Result") }
assertEquals(0, resultLabels.size, "Should show 0 results for invalid input")
```

**Dynamic List Testing:**
```kotlin
// Test dynamic content generation
val count = 5
numberField.clearText()
numberField.typeText(count.toString())
// No waitForIdle needed - Sweet Compose recomposition is immediate
val dynamicLabels = shell.findAll<Label> { it.text.matches(Regex("Item \\d+")) }
assertEquals(count, dynamicLabels.size, "Should show $count dynamic items")
```

**Migration Example:**

Here's how to migrate a complex layout test from manual assertions to the new API:

```kotlin
// ❌ Before - manual bounds checking
@Test
fun testComplexLayout() {
    autoSWT {
        testShell(width = 400, height = 300) {
            // ... setup composable with toolbar, content, status bar
        }.test { shell ->
            val toolbar = shell.find<Composite> { /* find toolbar */ }
            val content = shell.find<Composite> { /* find content */ }
            val statusBar = shell.find<Composite> { /* find status */ }
            
            // Manual bounds calculations - error-prone!
            val toolbarVB = toolbar.visibleBounds()
            val contentVB = content.visibleBounds() 
            val statusVB = statusBar.visibleBounds()
            val shellClient = shell.clientArea
            
            // Verbose, hard to read assertions
            assertTrue(toolbarVB.y == 0, "Toolbar not at top")
            assertTrue(toolbarVB.width == shellClient.width, "Toolbar not full width")
            assertTrue(contentVB.y >= toolbarVB.y + toolbarVB.height, "Content overlaps toolbar")
            assertTrue(contentVB.width == shellClient.width, "Content not full width") 
            assertTrue(statusVB.y >= contentVB.y + contentVB.height, "Status overlaps content")
            assertTrue(statusVB.y + statusVB.height == shellClient.height, "Status not at bottom")
        }
    }
}

// ✅ After - fluent layout API
@Test
fun testComplexLayout() {
    autoSWT {
        testShell(width = 400, height = 300) {
            // ... same setup
        }.test { shell ->
            val composite = runOnSWT { shell.children.first() as Composite }
            val toolbar = shell.find<Composite> { /* find toolbar */ }
            val content = shell.find<Composite> { /* find content */ }
            val statusBar = shell.find<Composite> { /* find status */ }
            
            // Clear, readable layout assertions
            toolbar.assertLayout()
                .isVisible()
                .isNearTopEdgeOf(composite, maxDistance = 0)
                .fillsHorizontally(composite)
            
            content.assertLayout()
                .isVisible()
                .isBelow(toolbar)
                .isAbove(statusBar)
                .fillsHorizontally(composite)
                .doesNotOverlap(toolbar)
                .doesNotOverlap(statusBar)
            
            statusBar.assertLayout()
                .isVisible()
                .isNearBottomEdgeOf(composite, maxDistance = 0)
                .fillsHorizontally(composite)
                
            // Verify overall layout
            listOf(toolbar, content, statusBar).assertLayout()
                .areAllVisible()
                .areArrangedInColumn(minGap = 0)
                .areLeftAlignedWith()
                .fitWithin(composite)
        }
    }
}
```

#### Best Practices for UI Tests

1. **Use the layout assertion API** instead of manual `visibleBounds()` calculations
2. **Don't use `waitForIdle()`** - SWT is single-threaded and Sweet Compose recomposition is immediate
3. **Use `runOnSWT` only for direct widget property access** - not for find() predicates or interactions
4. **Avoid tolerance unless absolutely necessary** - strict assertions catch real layout bugs
5. **Include debug logging** with `SweetDebugger.log()` for troubleshooting
6. **Save artifacts** with `shell.saveSVG()` and `shell.saveScreenshot()` for debugging
7. **Test edge cases** like empty inputs, invalid values, and boundary conditions
8. **Use descriptive test names** that explain what is being tested
9. **Keep test composables simple** and focused on the specific functionality being tested
10. **Chain assertions fluently** to make test intent clear and reduce boilerplate

#### Running Specific Tests
```bash
# Run a single test
./gradlew :sample-sweet:test --tests "TodoMVCTest.testTodoFilter"

# Run all tests in a class
./gradlew :sample-sweet:test --tests "TodoMVCTest"

# Run tests with detailed output
./gradlew :sample-sweet:test --tests "TodoMVCTest.testTodoFilter" --info
```

## Debugging

### Debug Utilities
- Use `SweetDebugger` for composition tree inspection
- Use `SWTSvgGenerator` from the separate `auto-swt` repository for visual debugging and SVG generation
- Enable debug logging for SWT operations
- Use `[DEBUG_LOG]` prefix for debug messages

### Common Issues
- **Threading**: Ensure UI operations run on SWT thread
- **Memory Leaks**: Properly dispose SWT resources
- **Platform Issues**: Test on target platforms for SWT compatibility

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions focused and small

### Pull Request Guidelines
- Include tests for new functionality
- Update documentation as needed
- Ensure all existing tests pass
## Resources

- **MPP Compose Desktop source (authoritative API reference)**:
  `3rdparty/compose-multiplatform-core/compose/`
  - `ui/ui-graphics/src/commonMain/` — DrawScope, Canvas, Brush, Path, Color, Shape, Painter
  - `material3/material3/src/commonMain/` — Material3 composables (Text, Button, TextField, etc.)
  - `foundation/foundation/src/commonMain/` — Foundation modifiers (clickable, background, border, scroll)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [SWT Documentation](https://www.eclipse.org/swt/)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)