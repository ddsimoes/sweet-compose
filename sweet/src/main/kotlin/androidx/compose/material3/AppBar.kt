@file:Suppress("ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * A TopAppBar with title and optional actions, rendered inside a Card + Row layout.
 *
 * **Sweet note:** The `colors` parameter is accepted for API compatibility but the
 * color values are not applied to the SWT widgets. The app bar renders with default
 * system colors.
 *
 * @param title The title content, typically a Text composable
 * @param modifier The modifier to be applied to the TopAppBar
 * @param navigationIcon Optional navigation icon at the start
 * @param actions Optional action items at the end
 * @param colors TopAppBar colors configuration (currently ignored)
 */
@ExperimentalMaterial3Api
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("TopAppBar", "TopAppBar colors parameter is accepted but currently ignored")
    }
    // Use Card for elevated appearance
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Navigation icon
            navigationIcon?.let {
                Box(modifier = Modifier.size(24.dp)) {
                    it()
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Title - takes remaining space
            Box(
                modifier = Modifier.weight(1f),
            ) {
                title()
            }

            // Actions
            Box {
                actions()
            }
        }
    }
}

/**
 * Contains default values used by TopAppBar
 */
object TopAppBarDefaults {
    /**
     * Creates a TopAppBarColors with default colors
     */
    @Composable
    fun topAppBarColors(
        containerColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ): TopAppBarColors =
        TopAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor,
            navigationIconContentColor = navigationIconContentColor,
            actionIconContentColor = actionIconContentColor,
        )
}

/**
 * Represents the colors used by a TopAppBar.
 *
 * **Sweet note:** Color values are accepted for API compatibility but are not yet
 * applied to the rendered SWT widgets.
 */
data class TopAppBarColors(
    val containerColor: Color,
    val titleContentColor: Color,
    val navigationIconContentColor: Color,
    val actionIconContentColor: Color,
)
