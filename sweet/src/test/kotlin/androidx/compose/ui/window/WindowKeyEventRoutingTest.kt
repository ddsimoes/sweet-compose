@file:Suppress("ktlint:standard:function-naming")

package androidx.compose.ui.window

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowKeyEventRoutingTest {
    private fun createKeyEvent(): Pair<Event, KeyEvent> {
        val swtEvent =
            Event().apply {
                type = SWT.KeyDown
                keyCode = SWT.ARROW_DOWN
                character = 'x'
                stateMask = 0
                doit = true
            }
        val keyEvent = swtEventToKeyEvent(swtEvent)
        return swtEvent to keyEvent
    }

    @Test
    fun previewHandler_consumes_event_and_neutralizes_swt_event() {
        val (swtEvent, keyEvent) = createKeyEvent()
        var previewCalled = false
        var keyCalled = false

        routeKeyEvent(
            event = swtEvent,
            keyEvent = keyEvent,
            onPreviewKeyEvent = {
                previewCalled = true
                true
            },
            onKeyEvent = {
                keyCalled = true
                false
            },
        )

        assertTrue(previewCalled, "Preview handler should be called")
        assertFalse(keyCalled, "Key handler should not be called when preview consumes")
        assertEquals(SWT.None, swtEvent.type, "Consumed event type should be neutralized")
        assertFalse(swtEvent.doit, "Consumed event doit flag should be false")
    }

    @Test
    fun keyHandler_consumes_event_and_neutralizes_swt_event() {
        val (swtEvent, keyEvent) = createKeyEvent()
        var previewCalled = false
        var keyCalled = false

        routeKeyEvent(
            event = swtEvent,
            keyEvent = keyEvent,
            onPreviewKeyEvent = {
                previewCalled = true
                false
            },
            onKeyEvent = {
                keyCalled = true
                true
            },
        )

        assertTrue(previewCalled, "Preview handler should be called")
        assertTrue(keyCalled, "Key handler should be called when preview does not consume")
        assertEquals(SWT.None, swtEvent.type, "Consumed event type should be neutralized")
        assertFalse(swtEvent.doit, "Consumed event doit flag should be false")
    }

    @Test
    fun unconsumed_event_leaves_swt_event_intact() {
        val (swtEvent, keyEvent) = createKeyEvent()
        var previewCalled = false
        var keyCalled = false

        routeKeyEvent(
            event = swtEvent,
            keyEvent = keyEvent,
            onPreviewKeyEvent = {
                previewCalled = true
                false
            },
            onKeyEvent = {
                keyCalled = true
                false
            },
        )

        assertTrue(previewCalled, "Preview handler should be called")
        assertTrue(keyCalled, "Key handler should be called")
        assertEquals(SWT.KeyDown, swtEvent.type, "Unconsumed event type should be unchanged")
        assertTrue(swtEvent.doit, "Unconsumed event doit flag should remain true")
    }
}
