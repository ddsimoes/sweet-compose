package androidx.compose.material3

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

data class Typography(
    val headlineMedium: TextStyle = TextStyle(fontSize = 24.sp),
    val headlineSmall: TextStyle = TextStyle(fontSize = 20.sp),
    val titleMedium: TextStyle = TextStyle(fontSize = 18.sp),
    val titleLarge: TextStyle = TextStyle(fontSize = 20.sp),
    val bodyLarge: TextStyle = TextStyle(fontSize = 16.sp),
    val bodyMedium: TextStyle = TextStyle(fontSize = 14.sp),
    val bodySmall: TextStyle = TextStyle(fontSize = 12.sp),
    val labelSmall: TextStyle = TextStyle(fontSize = 10.sp),
)

internal val LocalTypography = staticCompositionLocalOf { Typography() }
