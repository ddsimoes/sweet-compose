@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusRequester
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.onContextMenu
import androidx.compose.foundation.onDoubleClick
import androidx.compose.foundation.onFocusChanged
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Behavioral tests for input event handling — clickable, focus, hover,
 * double-click, and context-menu.
 *
 * Each test SYNTHESIZES the SWT event on the target control and asserts that the
 * registered callback actually fires — not merely that a callback was installed.
 * The internal data keys are used only to *locate* the wired control; the
 * assertions are on observable behavior (callback invoked / not invoked).
 */
class InputEventTest {

    /** Synthesize and dispatch an SWT event of [type] on this control. */
    private fun Control.fire(type: Int, button: Int = 0, x: Int = 0, y: Int = 0) {
        val e = Event().apply {
            widget = this@fire
            this.button = button
            this.x = x
            this.y = y
            this.type = type
        }
        notifyListeners(type, e)
    }

    // ── Clickable ──────────────────────────────────────────────────────

    @Test
    fun `clickable fires callback on press-release click`() {
        var fired = false
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text("Click me", modifier = Modifier.clickable { fired = true })
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }
                        .first { it.getData("__clickable_callback") != null }
                }
                assertNotNull(target, "clickable control should be wired")
                runOnSWT {
                    target.fire(SWT.MouseDown, button = 1, x = 5, y = 5)
                    target.fire(SWT.MouseUp, button = 1, x = 5, y = 5)
                }
                assertTrue(fired, "clickable callback should fire on press-release click")
            }
        }
    }

    @Test
    fun `disabled clickable does not fire on click`() {
        var fired = false
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text("Disabled", modifier = Modifier.clickable(enabled = false) { fired = true })
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }
                        .first { it.getData("__clickable_callback") != null }
                }
                assertNotNull(target, "disabled clickable should still be wired")
                runOnSWT {
                    target.fire(SWT.MouseDown, button = 1, x = 5, y = 5)
                    target.fire(SWT.MouseUp, button = 1, x = 5, y = 5)
                }
                assertFalse(fired, "disabled clickable should not fire")
            }
        }
    }

    // ── Focus ──────────────────────────────────────────────────────────

    @Test
    fun `onFocusChanged fires on focus in and out`() {
        var focusState: Boolean? = null
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text("Focusable", modifier = Modifier.onFocusChanged { focusState = it })
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }
                        .first { it.getData("__sweet_focus_action") != null }
                }
                assertNotNull(target, "focus control should be wired")
                runOnSWT { target.fire(SWT.FocusIn) }
                assertEquals(true, focusState, "FocusIn should report true")
                runOnSWT { target.fire(SWT.FocusOut) }
                assertEquals(false, focusState, "FocusOut should report false")
            }
        }
    }

    // ── Hover ──────────────────────────────────────────────────────────

    @Test
    fun `hoverable fires on mouse enter and exit`() {
        var hoverState: Boolean? = null
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text("Hover me", modifier = Modifier.hoverable { hoverState = it })
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }
                        .first { it.getData("__sweet_hover_action") != null }
                }
                assertNotNull(target, "hover control should be wired")
                runOnSWT { target.fire(SWT.MouseEnter) }
                assertEquals(true, hoverState, "MouseEnter should report true")
                runOnSWT { target.fire(SWT.MouseExit) }
                assertEquals(false, hoverState, "MouseExit should report false")
            }
        }
    }

    // ── Double-click ───────────────────────────────────────────────────

    @Test
    fun `onDoubleClick fires on double click`() {
        var fired = false
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text("Dbl-click", modifier = Modifier.onDoubleClick { fired = true })
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }
                        .first { it.getData("__sweet_double_click_action") != null }
                }
                assertNotNull(target, "double-click control should be wired")
                runOnSWT { target.fire(SWT.MouseDoubleClick, button = 1, x = 5, y = 5) }
                assertTrue(fired, "double-click callback should fire")
            }
        }
    }

    // ── Context menu ───────────────────────────────────────────────────

    @Test
    fun `onContextMenu fires on menu detect`() {
        var fired = false
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text("Right-click", modifier = Modifier.onContextMenu { fired = true })
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }
                        .first { it.getData("__sweet_context_menu_action") != null }
                }
                assertNotNull(target, "context-menu control should be wired")
                runOnSWT { target.fire(SWT.MenuDetect) }
                assertTrue(fired, "context-menu callback should fire")
            }
        }
    }

    // ── Combined modifiers ─────────────────────────────────────────────

    @Test
    fun `multiple event modifiers coexist and each fires`() {
        var clicked = false
        var hovered = false
        val requester = androidx.compose.foundation.FocusRequester()
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Text(
                        "Multi-event",
                        modifier = Modifier
                            .clickable { clicked = true }
                            .onFocusChanged { }
                            .hoverable { if (it) hovered = true }
                            .focusRequester(requester),
                    )
                }
            }.test { shell ->
                val target = runOnSWT {
                    shell.findAll<Control> { true }.first {
                        it.getData("__clickable_callback") != null &&
                            it.getData("__sweet_focus_action") != null &&
                            it.getData("__sweet_hover_action") != null
                    }
                }
                assertNotNull(target, "combined-modifier control should be wired")
                runOnSWT {
                    target.fire(SWT.MouseDown, button = 1, x = 5, y = 5)
                    target.fire(SWT.MouseUp, button = 1, x = 5, y = 5)
                }
                assertTrue(clicked, "click should fire on the combined control")
                runOnSWT { target.fire(SWT.MouseEnter) }
                assertTrue(hovered, "hover should fire on the combined control")
            }
        }
    }
}
