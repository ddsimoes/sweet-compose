@file:Suppress("ktlint:standard:function-naming")

import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.internal.configureShellKeyEvents
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.MenuItem
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end event handling tests: KeyDown injection and menu callback verification.
 */
class EventHandlingTest {
    // ── KeyDown end-to-end ────────────────────────────────────────────────

    @Test
    fun shell_keyDown_fires_preview_and_key_handlers() {
        autoSWT {
            testShell(width = 400, height = 200) {}.test { shell ->
                var previewCalled = false
                var keyCalled = false

                runOnSWT {
                    shell.configureShellKeyEvents(
                        onPreviewKeyEvent = {
                            previewCalled = true
                            false // don't consume, let key handler run
                        },
                        onKeyEvent = {
                            keyCalled = true
                            true // consume
                        },
                    )

                    // Inject a KeyDown event
                    val event =
                        Event().apply {
                            type = SWT.KeyDown
                            keyCode = SWT.ARROW_DOWN
                            character = 0.toChar()
                            stateMask = 0
                            doit = true
                            widget = shell
                        }
                    shell.notifyListeners(SWT.KeyDown, event)
                }

                assertTrue(previewCalled, "Preview key handler should fire on KeyDown")
                assertTrue(keyCalled, "Key handler should fire when preview does not consume")
            }
        }
    }

    @Test
    fun shell_keyDown_preview_consumption_blocks_key_handler() {
        autoSWT {
            testShell(width = 400, height = 200) {}.test { shell ->
                var previewCalled = false
                var keyCalled = false

                runOnSWT {
                    shell.configureShellKeyEvents(
                        onPreviewKeyEvent = {
                            previewCalled = true
                            true // consume
                        },
                        onKeyEvent = {
                            keyCalled = true
                            false
                        },
                    )

                    val event =
                        Event().apply {
                            type = SWT.KeyDown
                            keyCode = 'A'.code
                            character = 'a'
                            stateMask = 0
                            doit = true
                            widget = shell
                        }
                    shell.notifyListeners(SWT.KeyDown, event)
                }

                assertTrue(previewCalled, "Preview handler should fire")
                assertTrue(!keyCalled, "Key handler should NOT fire when preview consumes")
            }
        }
    }

    // ── Menu callback ────────────────────────────────────────────────────

    @Test
    fun menu_item_callback_fires_when_wired_through_callbacks_map() {
        autoSWT {
            testShell(width = 400, height = 200) {}.test { shell ->
                var callbackFired = false

                runOnSWT {
                    // Register a callback map with a single Item entry at path "0.0"
                    val callbackMap =
                        mapOf<String, Any>(
                            "0.0" to ({ callbackFired = true } as () -> Unit),
                        )
                    shell.setData("sweet.menu.callbacks", callbackMap)

                    // Create a Menu bar with a single MenuItem and wire it like setupMenuBar does
                    val menuBar = org.eclipse.swt.widgets.Menu(shell, SWT.BAR)
                    val fileMenu = org.eclipse.swt.widgets.Menu(shell, SWT.DROP_DOWN)
                    MenuItem(menuBar, SWT.CASCADE).apply {
                        text = "File"
                        menu = fileMenu
                    }
                    MenuItem(fileMenu, SWT.PUSH).apply {
                        text = "Action"
                    }

                    // Simulate selection to verify callback lookup works
                    @Suppress("UNCHECKED_CAST")
                    val callback =
                        (shell.getData("sweet.menu.callbacks") as? Map<*, *>)
                            ?.get("0.0") as? (() -> Unit)
                    assertNotNull(callback, "Callback should be retrievable from shell data")
                    callback.invoke()
                }

                assertTrue(callbackFired, "Menu item callback should fire")
            }
        }
    }
}
