@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.windowDecorationFromFlag
import io.github.ddsimoes.sweet.internal.shellStyleFromDecoration
import org.eclipse.swt.SWT
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase 5 tests: window/menu model consolidation — pure helpers.
 */
class WindowMenuTest {
    // ── shellStyleFromDecoration ──────────────────────────────────────

    @Test
    fun system_default_resizable_yields_shell_trim() {
        val style = shellStyleFromDecoration(WindowDecoration.SystemDefault, resizable = true)
        assertEquals(SWT.SHELL_TRIM, style)
    }

    @Test
    fun system_default_nonresizable_excludes_resize_bit() {
        val style = shellStyleFromDecoration(WindowDecoration.SystemDefault, resizable = false)
        val expected = SWT.TITLE or SWT.CLOSE or SWT.MIN or SWT.MAX
        assertEquals(expected, style)
    }

    @Test
    fun undecorated_yields_no_trim() {
        val style =
            shellStyleFromDecoration(
                androidx.compose.ui.window.UndecoratedWindowDecoration(8.dp),
                resizable = true,
            )
        assertEquals(SWT.NO_TRIM, style)
    }

    @Test
    fun null_decoration_defaults_to_shell_trim() {
        val style = shellStyleFromDecoration(null, resizable = true)
        assertEquals(SWT.SHELL_TRIM, style)
    }

    @Test
    fun unknown_decoration_defaults_to_shell_trim() {
        val style = shellStyleFromDecoration("unknown", resizable = false)
        assertEquals(SWT.SHELL_TRIM, style)
    }

    // ── windowDecorationFromFlag ─────────────────────────────────────

    @Test
    fun undecorated_flag_yields_undecorated_decoration() {
        val dec = windowDecorationFromFlag(undecorated = true)
        val style = shellStyleFromDecoration(dec, resizable = true)
        assertEquals(SWT.NO_TRIM, style)
    }

    @Test
    fun decorated_flag_yields_system_default_decoration() {
        val dec = windowDecorationFromFlag(undecorated = false)
        val style = shellStyleFromDecoration(dec, resizable = true)
        assertEquals(SWT.SHELL_TRIM, style)
    }
}
