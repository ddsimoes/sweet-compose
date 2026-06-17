@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * A Material Design slider.
 *
 * **Sweet note:** Currently renders as text ("Slider: N%") rather than an interactive
 * slider control. This is a compatibility shim; a native SWT slider implementation is
 * planned for a future version.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("Slider", "Slider currently renders as text; interactive slider control is not yet implemented")
    }
    Text("Slider: ${(value * 100).toInt()}%", modifier = modifier)
}
