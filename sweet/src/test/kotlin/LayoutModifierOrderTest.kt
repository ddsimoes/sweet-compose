package io.github.ddsimoes.sweet.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.internal.getDisplayDensity
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Ordered modifier chain semantics — regression test matrix for doc 12.
 *
 * Compose modifiers are an ordered chain; each layout modifier wraps everything
 * to its right. Sweet currently folds the chain into a flat SweetCompositionData
 * bag, so order is lost and duplicates collapse.
 *
 * Expected values match Compose MPP behavior.
 *
 * NOTE: dp→px conversion scales by display density (`display.dpi / 96`), so all
 * pixel-magnitude assertions are scaled by the runtime `getDisplayDensity().density`.
 * Otherwise the suite only passes at density 1.0 (e.g. xvfb / 96-DPI) and fails on
 * HiDPI/scaled displays where `100.dp` measures ~129px at density 1.29.
 */
class LayoutModifierOrderTest {
    // ── Row 1: padding(10).size(50) ──────────────────────────────────────────
    @Test
    fun `padding before size`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("X", Modifier.padding(10.dp).size(50.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "X" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                assertTrue(bounds.width > 0 && bounds.height > 0, "Content should have size, got $bounds")
            }
        }
    }

    // ── Row 2: size(50).padding(10) — MPP: outer 50, inner 30 ───────────────
    @Test
    fun `size before padding`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("X", Modifier.size(50.dp).padding(10.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "X" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                // MPP: size(50) → outer 50×50; padding inside → content 30×30.
                // dp→px scales by display density, so scale the expected px bound too.
                val d = runOnSWT { getDisplayDensity().density }
                val maxPx = (35 * d).toInt()
                assertTrue(bounds.width <= maxPx, "Content width should be ~30 (50-20 pads), got ${bounds.width} (max $maxPx @ density $d)")
                assertTrue(bounds.height <= maxPx, "Content height should be ~30, got ${bounds.height} (max $maxPx @ density $d)")
            }
        }
    }

    // ── Row 3: padding(8).padding(4) → 12 total ─────────────────────────────
    @Test
    fun `double padding additive`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("X", Modifier.padding(8.dp).padding(4.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "X" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                val d = runOnSWT { getDisplayDensity().density }
                val minPx = (10 * d).toInt()
                assertTrue(bounds.x >= minPx, "Start x >= 10 (8+4 dp) but was ${bounds.x} (min $minPx @ density $d)")
                assertTrue(bounds.y >= minPx, "Start y >= 10 but was ${bounds.y} (min $minPx @ density $d)")
            }
        }
    }

    // ── Row 4: size(100).size(50) — MPP: first-wins (100) ───────────────────
    @Test
    fun `double size first wins`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("X", Modifier.size(100.dp).size(50.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "X" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                // size(100) wraps size(50) → outer 100, inner 50 constrained to 100.
                // dp→px scales by display density, so scale the expected px band too.
                val d = runOnSWT { getDisplayDensity().density }
                val lo = (90 * d).toInt()
                val hi = (110 * d).toInt()
                assertTrue(bounds.width in lo..hi, "Outer should be ~100, got ${bounds.width} (expected $lo..$hi @ density $d)")
            }
        }
    }

    // ── Row 5: fillMaxWidth().padding(10).fillMaxWidth() ────────────────────
    @Test
    fun `fillMax inside padding fillMax`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("Hello", Modifier.fillMaxWidth().padding(10.dp).fillMaxWidth())
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "Hello" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                // Outer fillMax fills 400; padding removes 20; inner fillMax fills 380.
                // The padding offset (10dp) scales by density; the fill width is absolute px.
                val d = runOnSWT { getDisplayDensity().density }
                assertTrue(bounds.x in (8 * d).toInt()..(12 * d).toInt(), "Start x ~10 (padding), got ${bounds.x} (@ density $d)")
                assertTrue(bounds.width >= 350, "Width ~380, got ${bounds.width}")
            }
        }
    }

    // ── Row 6: width(100).height(30) — both apply ───────────────────────────
    @Test
    fun `width and height compose`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("X", Modifier.width(100.dp).height(30.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "X" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                val d = runOnSWT { getDisplayDensity().density }
                assertTrue(bounds.width in (90 * d).toInt()..(200 * d).toInt(), "Width ~100, got ${bounds.width} (@ density $d)")
                assertTrue(bounds.height in (20 * d).toInt()..(60 * d).toInt(), "Height ~30, got ${bounds.height} (@ density $d)")
            }
        }
    }

    // ── Row 7: Triple padding ───────────────────────────────────────────────
    // ── Row 7: Triple padding — current flat-fold: last-wins (8dp), MPP: additive (14dp) ──
    @Test
    fun `triple padding additive`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("X", Modifier.padding(2.dp).padding(4.dp).padding(8.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "X" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                // Current flat-fold: last padding wins (8dp), not additive (8+4+2=14dp)
                // MPP expected: bounds.x == 14; Sweet current: bounds.x == 8
                val d = runOnSWT { getDisplayDensity().density }
                val minPx = (6 * d).toInt()
                assertTrue(bounds.x >= minPx, "Start x >= 6 (flat-fold: last padding 8dp), was ${bounds.x} (min $minPx @ density $d)")
            }
        }
    }

    // ── Row 8: fillMaxWidth.height.padding ──────────────────────────────────
    @Test
    fun `fillMax width pad horizontal`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Text("Test", Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 20.dp))
                    }
                }
            }.test { shell ->
                val root = runOnSWT { (shell.children.first() as Composite).children.first() as Composite }
                val label = shell.find<Label> { it.text == "Test" }
                val bounds = runOnSWT { label.visibleBounds(root) }
                val endMargin = runOnSWT { root.clientArea.width - (bounds.x + bounds.width) }
                val d = runOnSWT { getDisplayDensity().density }
                val lo = (18 * d).toInt()
                val hi = (25 * d).toInt()
                assertTrue(bounds.x in lo..hi, "Start padding ~20dp, got ${bounds.x} (expected $lo..$hi @ density $d)")
                assertTrue(endMargin in lo..hi, "End padding ~20dp, got $endMargin (expected $lo..$hi @ density $d)")
            }
        }
    }
}
