package androidx.compose.material3

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material Design 3 Color Scheme
 */
data class ColorScheme(
    val primary: Color = Color(0xFF6200EAUL),
    val onPrimary: Color = Color.White,
    val primaryContainer: Color = Color(0xFFBB86FCUL),
    val onPrimaryContainer: Color = Color.Black,
    val secondary: Color = Color(0xFF03DAC6UL),
    val onSecondary: Color = Color.Black,
    val secondaryContainer: Color = Color(0xFF018786UL),
    val onSecondaryContainer: Color = Color.White,
    val tertiary: Color = Color(0xFF03DAC6UL),
    val onTertiary: Color = Color.Black,
    val tertiaryContainer: Color = Color(0xFF018786UL),
    val onTertiaryContainer: Color = Color.White,
    val error: Color = Color(0xFFCF6679UL),
    val onError: Color = Color.Black,
    val errorContainer: Color = Color(0xFFB00020UL),
    val onErrorContainer: Color = Color.White,
    val background: Color = Color(0xFF121212UL),
    val onBackground: Color = Color.White,
    val surface: Color = Color(0xFF1E1E1EUL),
    val onSurface: Color = Color.White,
    val surfaceVariant: Color = Color(0xFF2A2A2AUL),
    val onSurfaceVariant: Color = Color(0xFFCCCCCCUL),
    val outline: Color = Color(0xFF666666UL),
    val outlineVariant: Color = Color(0xFF444444UL),
    val scrim: Color = Color.Black,
    val inverseSurface: Color = Color.White,
    val inverseOnSurface: Color = Color.Black,
    val inversePrimary: Color = Color(0xFF6200EAUL),
)

/**
 * Creates a dark color scheme with optional overrides
 */
fun darkColorScheme(
    primary: Color = Color(0xFF6200EAUL),
    onPrimary: Color = Color.White,
    primaryContainer: Color = Color(0xFFBB86FCUL),
    onPrimaryContainer: Color = Color.Black,
    secondary: Color = Color(0xFF03DAC6UL),
    onSecondary: Color = Color.Black,
    secondaryContainer: Color = Color(0xFF018786UL),
    onSecondaryContainer: Color = Color.White,
    tertiary: Color = Color(0xFF03DAC6UL),
    onTertiary: Color = Color.Black,
    tertiaryContainer: Color = Color(0xFF018786UL),
    onTertiaryContainer: Color = Color.White,
    error: Color = Color(0xFFCF6679UL),
    onError: Color = Color.Black,
    errorContainer: Color = Color(0xFFB00020UL),
    onErrorContainer: Color = Color.White,
    background: Color = Color(0xFF121212UL),
    onBackground: Color = Color.White,
    surface: Color = Color(0xFF1E1E1EUL),
    onSurface: Color = Color.White,
    surfaceVariant: Color = Color(0xFF2A2A2AUL),
    onSurfaceVariant: Color = Color(0xFFCCCCCCUL),
    outline: Color = Color(0xFF666666UL),
    outlineVariant: Color = Color(0xFF444444UL),
    scrim: Color = Color.Black,
    inverseSurface: Color = Color.White,
    inverseOnSurface: Color = Color.Black,
    inversePrimary: Color = Color(0xFF6200EAUL),
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
    )

internal val LocalColorScheme = staticCompositionLocalOf { darkColorScheme() }
