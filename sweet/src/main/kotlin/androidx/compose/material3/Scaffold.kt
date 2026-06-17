@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * <a href="https://material.io/design/layout/understanding-layout.html">Material Design layout</a>.
 *
 * Scaffold implements the basic material design visual layout structure.
 *
 * **Sweet note:** Currently uses a simple [Column] layout — topBar, content, bottomBar,
 * floatingActionButton, and snackbarHost render sequentially. Like the MPP Scaffold, it
 * fills the incoming max constraints (the whole window when used at the top level), and
 * the content slot takes the height remaining after the bars. Padding values passed to
 * [content] are not yet applied, and the FAB renders inline rather than as an overlay.
 * [FabPosition] is accepted but has no effect on layout.
 *
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param topBar top app bar of the screen, typically a [TopAppBar]
 * @param bottomBar bottom bar of the screen, typically a [NavigationBar]
 * @param snackbarHost component to host [Snackbar]s
 * @param floatingActionButton Main action button, typically a [FloatingActionButton]
 * @param floatingActionButtonPosition position of the FAB — currently ignored
 * @param containerColor background color of this scaffold
 * @param contentColor preferred content color — currently ignored
 * @param content content of the screen; receives [PaddingValues] (currently empty)
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log(
            "Scaffold",
            "Using simple Column layout; padding values, FAB overlay, and FabPosition are not yet implemented.",
        )
    }
    // Like MPP Scaffold, size to the incoming max constraints rather than wrapping content.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            // Content takes the vertical space remaining after the bars.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content(PaddingValues(0.dp))
            }
            bottomBar()
            floatingActionButton()
            snackbarHost()
        }
    }
}

/**
 * The possible positions for a [FloatingActionButton] attached to a [Scaffold].
 *
 * **Sweet note:** Accepted for API compatibility but has no effect on layout —
 * the FAB always renders inline in the [Column].
 */
@JvmInline
value class FabPosition internal constructor(
    @Suppress("unused") private val value: Int,
) {
    companion object {
        /** Position FAB at the bottom of the screen at the start. */
        val Start = FabPosition(0)

        /** Position FAB at the bottom of the screen in the center. */
        val Center = FabPosition(1)

        /** Position FAB at the bottom of the screen at the end. */
        val End = FabPosition(2)

        /** Position FAB at the bottom of the screen at the end, overlaying the bottom bar. */
        val EndOverlay = FabPosition(3)
    }

    override fun toString(): String =
        when (this) {
            Start -> "FabPosition.Start"
            Center -> "FabPosition.Center"
            End -> "FabPosition.End"
            else -> "FabPosition.EndOverlay"
        }
}
