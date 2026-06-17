import androidx.compose.ui.window.KeyEvent
import androidx.compose.ui.window.swtEventToKeyEvent
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowKeyEventMappingTest {
    @Test
    fun swtEvent_is_mapped_to_KeyEvent_flags_correctly() {
        val swtEvent =
            Event().apply {
                keyCode = SWT.F5
                character = 'x'
                stateMask = SWT.ALT or SWT.CTRL
            }

        val keyEvent: KeyEvent = swtEventToKeyEvent(swtEvent)

        assertEquals(SWT.F5, keyEvent.keyCode)
        assertEquals('x', keyEvent.character)
        assertTrue(keyEvent.isAltDown, "ALT should be reported as down")
        assertTrue(keyEvent.isCtrlDown, "CTRL should be reported as down")
        assertFalse(keyEvent.isShiftDown, "SHIFT should not be reported as down")
        assertFalse(keyEvent.isMetaDown, "META should not be reported as down")
        assertEquals(swtEvent, keyEvent.rawEvent)
    }
}
