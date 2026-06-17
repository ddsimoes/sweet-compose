@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reactivity tests for the `animate*AsState` layer.
 *
 * These prove the headline contract: writing [Animatable.value] during the animation
 * coroutine drives recomposition through the framework's snapshot apply observer + frame
 * clock — i.e. the API actually recomposes readers frame-by-frame, not just on settle.
 */
class AnimateAsStateTest {

    @Test
    fun `animateFloatAsState recomposes toward target and settles`() {
        val target = mutableStateOf(0f)
        val recompositions = AtomicInteger(0)
        val lastValue = AtomicReference(0f)

        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val v by animateFloatAsState(
                        targetValue = target.value,
                        animationSpec = tween(durationMillis = 200),
                    )
                    recompositions.incrementAndGet()
                    lastValue.set(v)
                }
            }.test {
                runBlocking {
                    delay(300) // let the initial state settle
                    val baseline = recompositions.get()
                    target.value = 1f
                    delay(900) // let the 200ms tween run several frames and settle

                    assertTrue(
                        recompositions.get() > baseline + 1,
                        "Expected multiple recompositions during the tween, got " +
                            "${recompositions.get() - baseline} after baseline $baseline",
                    )
                    assertEquals(1f, lastValue.get(), 0.1f, "Value should settle at target")
                }
            }
        }
    }

    @Test
    fun `animateDpAsState settles at the target value`() {
        val target = mutableStateOf(0.dp)
        val lastValue = AtomicReference(0.dp)

        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val v by animateDpAsState(
                        targetValue = target.value,
                        animationSpec = tween(durationMillis = 150),
                    )
                    lastValue.set(v)
                }
            }.test {
                runBlocking {
                    delay(250)
                    target.value = 48.dp
                    delay(800)
                    assertEquals(48.dp, lastValue.get(), "Dp value should settle at target")
                }
            }
        }
    }
}
