package androidx.compose.ui.draw

import androidx.compose.foundation.ClipModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * MPP-compatible re-export of [Modifier.clip] from the `ui.draw` package.
 *
 * JetBrains Compose defines this in `androidx.compose.ui.draw`;
 * Sweet defines the equivalent in `androidx.compose.foundation`.
 * This file bridges the import for code written against the MPP convention.
 */
fun Modifier.clip(shape: Shape): Modifier =
    this.then(ClipModifier(shape))
