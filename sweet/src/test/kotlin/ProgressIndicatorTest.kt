@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.ProgressBar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioral tests for LinearProgressIndicator and CircularProgressIndicator.
 * Progress 0..1 maps to SWT ProgressBar selection 0..100 (min=0, max=100).
 */
class ProgressIndicatorTest {

    @Test
    fun `linearProgressIndicator maps progress to selection`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LinearProgressIndicator(progress = 0.5f)
                }
            }.test { shell ->
                val bar = shell.find<ProgressBar>()!!
                assertEquals(0, runOnSWT { bar.minimum }, "minimum should be 0")
                assertEquals(100, runOnSWT { bar.maximum }, "maximum should be 100")
                assertEquals(50, runOnSWT { bar.selection },
                    "progress 0.5f should map to selection 50")
            }
        }
    }

    // ── Coercion ──────────────────────────────────────────────────────

    @Test
    fun `linearProgressIndicator coerces negative progress to zero`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LinearProgressIndicator(progress = -0.5f)
                }
            }.test { shell ->
                val bar = shell.find<ProgressBar>()!!
                assertEquals(0, runOnSWT { bar.selection },
                    "progress -0.5f should coerce to selection 0")
            }
        }
    }

    @Test
    fun `linearProgressIndicator coerces progress above one to full`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LinearProgressIndicator(progress = 1.5f)
                }
            }.test { shell ->
                val bar = shell.find<ProgressBar>()!!
                assertEquals(100, runOnSWT { bar.selection },
                    "progress 1.5f should coerce to selection 100")
            }
        }
    }

    // ── Boundaries ────────────────────────────────────────────────────

    @Test
    fun `linearProgressIndicator at zero progress has selection zero`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LinearProgressIndicator(progress = 0f)
                }
            }.test { shell ->
                val bar = shell.find<ProgressBar>()!!
                assertEquals(0, runOnSWT { bar.selection },
                    "progress 0f should map to selection 0")
            }
        }
    }

    @Test
    fun `linearProgressIndicator at full progress has selection one hundred`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LinearProgressIndicator(progress = 1f)
                }
            }.test { shell ->
                val bar = shell.find<ProgressBar>()!!
                assertEquals(100, runOnSWT { bar.selection },
                    "progress 1f should map to selection 100")
            }
        }
    }

    // ── CircularProgressIndicator ─────────────────────────────────────

    @Test
    fun `circularProgressIndicator renders an indeterminate progress bar`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    CircularProgressIndicator()
                }
            }.test { shell ->
                val bar = shell.find<ProgressBar>()!!
                assertTrue(
                    (runOnSWT { bar.style } and SWT.INDETERMINATE) != 0,
                    "CircularProgressIndicator should render an SWT.INDETERMINATE ProgressBar",
                )
            }
        }
    }
}
