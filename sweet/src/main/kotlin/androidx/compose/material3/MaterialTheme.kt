@file:Suppress("ktlint:standard:filename")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object MaterialTheme {
    val typography: Typography
        @Composable
        get() = LocalTypography.current

    val colorScheme: ColorScheme
        @Composable
        get() = LocalColorScheme.current
}

/**
 * MaterialTheme composable that provides Material Design 3 theming
 */
@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = darkColorScheme(),
    typography: Typography = Typography(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalTypography provides typography,
        content = content,
    )
}
