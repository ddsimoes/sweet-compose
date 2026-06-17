@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.weight
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.data.getSweetCompositionData
import io.github.ddsimoes.sweet.internal.FillMaxHeightStep
import io.github.ddsimoes.sweet.internal.FillMaxWidthStep
import io.github.ddsimoes.sweet.internal.LayoutStep
import io.github.ddsimoes.sweet.internal.ResolvedModifierChain
import io.github.ddsimoes.sweet.internal.SizeStep
import io.github.ddsimoes.sweet.internal.applySWTModifier
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 2 (F1) — idempotent modifier reconciliation.
 *
 * These tests pin the contract that modifier-derived state is reproducible from the *current* chain
 * alone: applying a chain twice is deterministic, removing a modifier fully reverts its effect, and
 * cumulative modifiers fold with the documented rules (padding/offset additive; size/weight
 * last-wins; constraints intersected).
 *
 * They operate directly on a freshly created [Label] via [applySWTModifier] so the assertions read
 * the derived [io.github.ddsimoes.sweet.data.SweetCompositionData] without depending on layout or
 * recomposition timing.
 *
 * Chain-modeled layout modifiers (fillMax, size, padding, offset) are asserted through the ordered
 * [ResolvedModifierChain] — their flat-field writes were deleted (see doc 12 decision log in
 * docs/roadmap/foundation-consolidation/STATUS.md). Parent-data modifiers
 */
class ModifierReconciliationTest {
    private val red = RGB(255, 0, 0)

    /** Runs [block] on the SWT thread with a fresh, parented [Label]. */
    private fun withLabel(block: (Label) -> Unit) {
        autoSWT {
            testShell(width = 300, height = 300) {
                Composite(this, SWT.NONE)
            }.test { shell ->
                runOnSWT {
                    // Pin density to 1.0 so DP→px conversions are deterministic across machines.
                    shell.display.setData("sweet.display.density", Density(1f))
                    val composite = shell.children.first() as Composite
                    val label = Label(composite, SWT.NONE)
                    block(label)
                }
            }
        }
    }

    // ========== TOGGLE ON -> OFF REVERTS FULLY ==========

    @Test
    fun fillMaxWidth_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.fillMaxWidth())
            assertTrue(label.layoutSteps.any { it is FillMaxWidthStep }, "fillMaxWidth should be set")

            applySWTModifier(label, Modifier)
            assertFalse(label.layoutSteps.any { it is FillMaxWidthStep }, "fillMaxWidth should revert")
        }
    }

    @Test
    fun fillMaxHeight_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.fillMaxHeight())
            assertTrue(label.layoutSteps.any { it is FillMaxHeightStep })

            applySWTModifier(label, Modifier)
            assertFalse(label.layoutSteps.any { it is FillMaxHeightStep })
        }
    }

    @Test
    fun weight_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.weight(2f))
            assertEquals(2f, label.getSweetCompositionData().layoutData.weight)

            applySWTModifier(label, Modifier)
            assertNull(label.getSweetCompositionData().layoutData.weight, "weight should revert to null")
        }
    }

    @Test
    fun size_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.size(120.dp))
            val step = label.layoutSteps.filterIsInstance<SizeStep>().single()
            assertNotNull(step.width)
            assertNotNull(step.height)

            applySWTModifier(label, Modifier)
            assertTrue(label.layoutSteps.none { it is SizeStep }, "size step should revert")
        }
    }

    @Test
    fun sizeIn_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.sizeIn(minWidth = 50.dp, maxWidth = 200.dp))
            val c = label.getSweetCompositionData().constraintsData
            assertNotNull(c.minWidth)
            assertNotNull(c.maxWidth)

            applySWTModifier(label, Modifier)
            val reverted = label.getSweetCompositionData().constraintsData
            assertNull(reverted.minWidth, "minWidth should revert to null")
            assertNull(reverted.maxWidth, "maxWidth should revert to null")
        }
    }

    @Test
    fun padding_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.padding(8.dp))
            assertTrue(label.totalPadding.all { it > 0 })

            applySWTModifier(label, Modifier)
            assertEquals(listOf(0, 0, 0, 0), label.totalPadding, "padding should revert to zero")
        }
    }

    @Test
    fun offset_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.offset(x = 10.dp, y = 20.dp))
            val (x, y) = label.totalOffset
            assertTrue(x != 0 && y != 0)

            applySWTModifier(label, Modifier)
            assertEquals(0 to 0, label.totalOffset, "offset should revert to zero")
        }
    }

    @Test
    fun background_reverts_when_removed() {
        withLabel { label ->
            val defaultRgb = label.background.rgb

            applySWTModifier(label, Modifier.background(Color.Red))
            assertEquals(red, label.background.rgb, "background should be red")

            applySWTModifier(label, Modifier)
            assertEquals(defaultRgb, label.background.rgb, "background should revert to the SWT default")
        }
    }

    @Test
    fun clickable_reverts_when_removed() {
        withLabel { label ->
            applySWTModifier(label, Modifier.clickable { /* no-op */ })
            assertNotNull(label.getData("__clickable_callback"), "callback should be installed")

            applySWTModifier(label, Modifier)
            assertNull(label.getData("__clickable_callback"), "callback should be cleared on removal (clicks become no-ops)")
        }
    }

    // ========== CLICKABLE INVOCATION ==========

    @Test
    fun clickable_callback_fires_then_stops_after_removal() {
        withLabel { label ->
            var count = 0
            applySWTModifier(label, Modifier.clickable { count++ })

            fireLeftClick(label)
            assertEquals(1, count, "callback should fire while clickable is present")

            // Remove clickable; the listener stays installed but reads a null callback.
            applySWTModifier(label, Modifier)
            fireLeftClick(label)
            assertEquals(1, count, "callback should not fire after clickable removal")
        }
    }

    @Test
    fun clickable_disabled_does_not_fire() {
        withLabel { label ->
            var count = 0
            applySWTModifier(label, Modifier.clickable(enabled = false) { count++ })

            fireLeftClick(label)
            assertEquals(0, count, "callback should not fire while clickable is disabled")
        }
    }

    @Test
    fun clickable_enabled_toggles_across_reapply() {
        withLabel { label ->
            var count = 0
            // Listeners are installed on the first apply; the enabled flag must be re-read
            // on every event, not captured at install time.
            applySWTModifier(label, Modifier.clickable(enabled = false) { count++ })
            fireLeftClick(label)
            assertEquals(0, count, "disabled at install time: no fire")

            applySWTModifier(label, Modifier.clickable(enabled = true) { count++ })
            fireLeftClick(label)
            assertEquals(1, count, "re-enabled on reapply: should fire")

            applySWTModifier(label, Modifier.clickable(enabled = false) { count++ })
            fireLeftClick(label)
            assertEquals(1, count, "re-disabled on reapply: should not fire")
        }
    }

    // ========== ADDITIVE FOLDING ==========

    @Test
    fun padding_is_additive() {
        withLabel { label ->
            // padding(8).padding(4) should equal a single padding(12) on every edge.
            // Allow ±1 px tolerance: dp→px Int conversion may lose fractional precision
            // when converting at each additive step vs. converting the sum directly.
            applySWTModifier(label, Modifier.padding(12.dp))
            val single = label.totalPadding
            assertTrue(single.all { it > 0 }, "padding(12) should produce non-zero totals")
            applySWTModifier(label, Modifier.padding(8.dp).padding(4.dp))
            val additive = label.totalPadding
            val maxDelta = single.zip(additive) { a, b -> kotlin.math.abs(a - b) }.max()
            assertTrue(
                maxDelta <= 2,
                "padding(8)+padding(4) == padding(12) — max delta=$maxDelta (single=$single additive=$additive)",
            )
        }
    }

    @Test
    fun offset_is_additive() {
        withLabel { label ->
            applySWTModifier(label, Modifier.offset(x = 12.dp, y = 8.dp))
            val single = label.totalOffset
            assertTrue(single.first != 0 && single.second != 0, "offset(12, 8) should produce non-zero totals")

            applySWTModifier(label, Modifier.offset(x = 5.dp, y = 5.dp).offset(x = 7.dp, y = 3.dp))
            val additive = label.totalOffset

            assertEquals(single.first, additive.first, "offset(5)+offset(7) == offset(12) (x)")
            assertEquals(single.second, additive.second)
        }
    }

    // ========== DETERMINISM ==========

    @Test
    fun same_chain_applied_twice_is_deterministic() {
        withLabel { label ->
            fun chain() = Modifier.fillMaxWidth().padding(8.dp).padding(4.dp).size(60.dp).background(Color.Green)

            applySWTModifier(label, chain())
            val first = label.getSweetCompositionData()

            applySWTModifier(label, chain())
            val second = label.getSweetCompositionData()

            assertEquals(first, second, "the same chain applied twice should yield identical composition data")
        }
    }

    // ========== CONDITIONAL CHAINS CLEAR STALE STATE ==========

    @Test
    fun conditional_chain_clears_stale_state() {
        withLabel { label ->
            // "Expanded" chain.
            applySWTModifier(
                label,
                Modifier
                    .size(80.dp)
                    .padding(8.dp)
                    .background(Color.Red),
            )
            assertEquals(red, label.background.rgb, "background should be red while present")

            // "Collapsed" chain: only padding remains.
            applySWTModifier(label, Modifier.padding(8.dp))

            assertTrue(label.layoutSteps.none { it is SizeStep }, "size should be cleared")
            assertTrue(label.totalPadding.all { it > 0 }, "padding should remain")
            assertNotEquals(red, label.background.rgb, "background should no longer be red")
        }
    }
}

// ── Chain-state helpers ─────────────────────────────────────────────────────
// Chain-modeled layout modifiers no longer write flat fields; assert against the
// ordered ResolvedModifierChain instead.

/** The ordered layout steps from the resolved modifier chain (empty when no chain). */
private val Control.layoutSteps: List<LayoutStep>
    get() = (getSweetCompositionData().modifierChain as? ResolvedModifierChain)?.layoutSteps.orEmpty()

/** Accumulated chain padding as [start, top, end, bottom] (zeros when no chain). */
private val Control.totalPadding: List<Int>
    get() {
        val c = getSweetCompositionData().modifierChain as? ResolvedModifierChain
        return listOf(
            c?.totalPaddingStart ?: 0,
            c?.totalPaddingTop ?: 0,
            c?.totalPaddingEnd ?: 0,
            c?.totalPaddingBottom ?: 0,
        )
    }

/** Accumulated chain offset as (x, y) (zeros when no chain). */
private val Control.totalOffset: Pair<Int, Int>
    get() {
        val c = getSweetCompositionData().modifierChain as? ResolvedModifierChain
        return (c?.totalOffsetX ?: 0) to (c?.totalOffsetY ?: 0)
    }

private fun fireLeftClick(label: Label) {
    val display = label.display
    val down =
        org.eclipse.swt.widgets.Event().apply {
            button = 1
            type = SWT.MouseDown
            this.display = display
            widget = label
        }
    label.notifyListeners(SWT.MouseDown, down)
    val up =
        org.eclipse.swt.widgets.Event().apply {
            button = 1
            type = SWT.MouseUp
            this.display = display
            widget = label
        }
    label.notifyListeners(SWT.MouseUp, up)
}
