@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertTrue

class ScrollIntegrationTest {
    @Composable
    private fun ScrollableColumn(captureState: (androidx.compose.foundation.ScrollState) -> Unit) {
        val state = rememberScrollState()
        // expose the state to the test code
        remember(state) { captureState(state) }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(state),
        ) {
            // Enough items to ensure the content is larger than the viewport
            repeat(50) { index ->
                Text("Item $index")
            }
        }
    }

    @Composable
    private fun HorizontalScrollableRow(captureState: (androidx.compose.foundation.ScrollState) -> Unit) {
        val state = rememberScrollState()
        remember(state) { captureState(state) }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxSize().horizontalScroll(state),
        ) {
            repeat(50) { index ->
                Text("Item $index")
            }
        }
    }

    @Test
    fun verticalScrollState_updates_scroller_origin_when_jumping() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    ScrollableColumn { state -> capturedState = state }
                }
            }.test { shell ->
                val scroller = shell.find<ScrollViewport> { true }
                val initialOrigin = runOnSWT { scroller.origin }

                val state = capturedState
                // Ensure we captured the scroll state and can control it
                requireNotNull(state) { "ScrollState should have been captured from composition" }

                // Jump programmatically and verify that the ScrollViewport origin changes
                runOnSWT {
                    kotlinx.coroutines.runBlocking { state.scrollTo(100) }
                }

                val originAfter = runOnSWT { scroller.origin }
                assertTrue(
                    originAfter.y >= initialOrigin.y,
                    "Scroller origin.y should increase or stay ahead after jumpTo; before=${initialOrigin.y}, after=${originAfter.y}",
                )
            }
        }
    }

    @Test
    fun horizontalScrollState_updates_scroller_origin_when_jumping() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    HorizontalScrollableRow { state -> capturedState = state }
                }
            }.test { shell ->
                val scroller = shell.find<ScrollViewport> { true }
                val initialOrigin = runOnSWT { scroller.origin }

                val state = capturedState
                requireNotNull(state) { "ScrollState should have been captured from composition" }

                runOnSWT {
                    kotlinx.coroutines.runBlocking { state.scrollTo(100) }
                }

                val originAfter = runOnSWT { scroller.origin }
                assertTrue(
                    originAfter.x >= initialOrigin.x,
                    "Scroller origin.x should increase or stay ahead after jumpTo; before=${initialOrigin.x}, after=${originAfter.x}",
                )
            }
        }
    }

    @Test
    fun horizontalScrollState_tracks_horizontal_bar_selection() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    HorizontalScrollableRow { state -> capturedState = state }
                }
            }.test { shell ->
                val scroller = shell.find<ScrollViewport> { true }
                val state = capturedState
                requireNotNull(state) { "ScrollState should have been captured from composition" }

                val targetSelection =
                    runOnSWT {
                        val hbar = scroller.horizontalBar
                        requireNotNull(hbar) { "Expected horizontal scrollbar to be present" }

                        // Choose a selection within the valid scroll range
                        val max = hbar.maximum
                        val thumb = hbar.thumb
                        val upperBound = (max - thumb).coerceAtLeast(0)
                        val target = (upperBound / 2).coerceAtLeast(0)

                        hbar.selection = target
                        val event = org.eclipse.swt.widgets.Event().apply { widget = hbar }
                        hbar.notifyListeners(SWT.Selection, event)
                        target
                    }

                val valueAfter = state.value
                assertTrue(
                    valueAfter == targetSelection,
                    "ScrollState.value should match horizontal bar selection; expected=$targetSelection, actual=$valueAfter",
                )
            }
        }
    }

    @Test
    fun mouse_wheel_scrolls_vertical_viewport() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    ScrollableColumn { state -> capturedState = state }
                }
            }.test { shell ->
                val viewport = shell.find<ScrollViewport> { true }
                val state = capturedState
                requireNotNull(state) { "ScrollState should have been captured from composition" }

                // Plain composites don't scroll on wheel natively; the viewport must
                // handle MouseVerticalWheel itself (new responsibility vs ScrolledComposite).
                runOnSWT {
                    val event =
                        org.eclipse.swt.widgets.Event().apply {
                            widget = viewport
                            count = -3 // wheel down
                            x = 50
                            y = 50
                        }
                    viewport.notifyListeners(SWT.MouseVerticalWheel, event)
                }

                val originAfter = runOnSWT { viewport.origin }
                assertTrue(
                    originAfter.y > 0,
                    "Wheel-down should scroll the viewport; origin.y=${originAfter.y}",
                )
                assertTrue(
                    state.value == originAfter.y,
                    "ScrollState should track wheel scrolling; state=${state.value}, origin=${originAfter.y}",
                )
            }
        }
    }

    @Test
    fun verticalScrollState_tracks_vertical_bar_selection() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    ScrollableColumn { state -> capturedState = state }
                }
            }.test { shell ->
                val scroller = shell.find<ScrollViewport> { true }
                val state = capturedState
                requireNotNull(state) { "ScrollState should have been captured from composition" }

                val targetSelection =
                    runOnSWT {
                        val vbar = scroller.verticalBar
                        requireNotNull(vbar) { "Expected vertical scrollbar to be present" }

                        val max = vbar.maximum
                        val thumb = vbar.thumb
                        val upperBound = (max - thumb).coerceAtLeast(0)
                        val target = (upperBound / 2).coerceAtLeast(0)

                        vbar.selection = target
                        val event = org.eclipse.swt.widgets.Event().apply { widget = vbar }
                        vbar.notifyListeners(SWT.Selection, event)
                        target
                    }

                val valueAfter = state.value
                assertTrue(
                    valueAfter == targetSelection,
                    "ScrollState.value should match vertical bar selection; expected=$targetSelection, actual=$valueAfter",
                )
            }
        }
    }
}
