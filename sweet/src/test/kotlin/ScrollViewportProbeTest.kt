import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pure-SWT characterization test (workstream 13): documents how a plain
 * `Composite(parent, SWT.V_SCROLL)` behaves on this platform (GTK under xvfb).
 * `io.github.ddsimoes.sweet.widgets.ScrollViewport` is built on these behaviors;
 * if this test's expectations change on a platform, the viewport's wheel/bar
 * wiring assumptions must be revisited.
 *
 * Questions probed:
 * 1. Does the native vertical ScrollBar exist and accept setValues()?
 * 2. Does programmatic bar.setSelection fire SWT.Selection? (expected: NO per SWT contract)
 * 3. Does a mouse wheel event over the composite move the bar natively, and does it
 *    fire SWT.Selection? (determines whether we must add our own wheel handler)
 * 4. Are children clipped to the composite client area?
 */
class ScrollViewportProbeTest {
    @Test
    fun plain_vscroll_composite_behavior() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val viewport = Composite(this, SWT.V_SCROLL)
                viewport.setData("probe.viewport", true)
                this.layout = FillLayout()
                // one tall child, manually positioned (layout = null)
                val content = Composite(viewport, SWT.NONE)
                content.setData("probe.content", true)
                val label = Label(content, SWT.NONE)
                label.text = "tall content"
                label.setBounds(0, 0, 100, 20)
                content.setBounds(0, 0, 200, 1000)
            }.test { shell ->
                val viewport = shell.find<Composite> { it.getData("probe.viewport") == true }
                assertNotNull(viewport)

                val results =
                    runOnSWT {
                        val bar = viewport.verticalBar
                        val r = mutableListOf<String>()
                        r += "bar=${bar != null}"
                        if (bar != null) {
                            r += "bar.visible=${bar.isVisible}"
                            // 1. setValues
                            val client = viewport.clientArea
                            r += "client=$client bounds=${viewport.bounds}"
                            bar.setValues(0, 0, 1000, client.height, 20, client.height)
                            r += "afterSetValues sel=${bar.selection} max=${bar.maximum} thumb=${bar.thumb}"
                            // 2. programmatic selection -> Selection event?
                            var selectionFired = 0
                            bar.addListener(SWT.Selection) { selectionFired++ }
                            bar.selection = 100
                            r += "progSetSelection firedEvents=$selectionFired sel=${bar.selection}"
                            // 3. wheel event over viewport
                            var wheelFired = 0
                            viewport.addListener(SWT.MouseVerticalWheel) { wheelFired++ }
                            val selBeforeWheel = bar.selection
                            val e = Event()
                            e.widget = viewport
                            e.count = -3 // wheel down
                            e.x = 50
                            e.y = 50
                            viewport.notifyListeners(SWT.MouseVerticalWheel, e)
                            r += "wheel notify fired=$wheelFired selBefore=$selBeforeWheel selAfter=${bar.selection} selectionFired=$selectionFired"
                        }
                        // 4. clipping: child extends beyond viewport; check child bounds preserved
                        val content = viewport.children.firstOrNull()
                        r += "contentBounds=${content?.bounds}"
                        r
                    }
                println("[DEBUG_LOG] PROBE RESULTS:")
                results.forEach { println("[DEBUG_LOG]   $it") }
                assertTrue(results.isNotEmpty())
            }
        }
    }

    @Test
    fun bar_visibility_when_not_needed() {
        autoSWT {
            testShell(width = 300, height = 200) {
                this.layout = FillLayout()
                val viewport = Composite(this, SWT.V_SCROLL or SWT.H_SCROLL)
                viewport.setData("probe.viewport", true)
            }.test { shell ->
                val viewport = shell.find<Composite> { it.getData("probe.viewport") == true }
                val results =
                    runOnSWT {
                        val v = viewport.verticalBar
                        val h = viewport.horizontalBar
                        val r = mutableListOf<String>()
                        r += "vbar.visible=${v?.isVisible} hbar.visible=${h?.isVisible}"
                        // try hiding
                        v?.isVisible = false
                        r += "after hide: vbar.visible=${v?.isVisible}"
                        r += "clientArea=${viewport.clientArea} bounds=${viewport.bounds}"
                        v?.isVisible = true
                        r += "after show: clientArea=${viewport.clientArea}"
                        // disabled-bar look
                        v?.isEnabled = false
                        r += "vbar disabled ok"
                        r
                    }
                println("[DEBUG_LOG] PROBE RESULTS:")
                results.forEach { println("[DEBUG_LOG]   $it") }
                assertTrue(results.isNotEmpty())
            }
        }
    }
}
