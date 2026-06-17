package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable

/**
 * Defines the horizontal alignment of text within its container.
 */
@Immutable
enum class TextAlign {
    /**
     * Align text to the left edge of the container
     */
    Left,

    /**
     * Align text to the right edge of the container
     */
    Right,

    /**
     * Align text to the center of the container
     */
    Center,

    /**
     * Stretch lines of text that end with a soft line break to fill the width of the container
     */
    Justify,

    /**
     * Align text to the start edge based on the text direction
     */
    Start,

    /**
     * Align text to the end edge based on the text direction
     */
    End,
}
