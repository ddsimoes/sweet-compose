package io.github.ddsimoes.sweet.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment

/**
 * Defines the layout algorithm and parameters for a container (parent Composite).
 * This is distinct from child-provided layout data (e.g., weight, fill), which
 * remains in SweetLayoutData on the child control.
 */
sealed interface LayoutSpec {
    data class Column(
        val verticalArrangement: Arrangement.Vertical,
        val horizontalAlignment: Alignment.Horizontal,
    ) : LayoutSpec

    data class Row(
        val horizontalArrangement: Arrangement.Horizontal,
        val verticalAlignment: Alignment.Vertical,
    ) : LayoutSpec

    data class Box(
        val contentAlignment: Alignment,
        // When true, children are measured to fill the parent's available size
        val fillChildren: Boolean = false,
    ) : LayoutSpec
}
