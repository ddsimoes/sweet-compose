package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

sealed class Brush {
    open val intrinsicSize: Size = Size.Unspecified

    companion object
}

data class SolidColor(val value: Color) : Brush()

data class LinearGradient(
    val colorStops: List<Pair<Float, Color>>,
    val start: Offset = Offset.Zero,
    val end: Offset = Offset.Infinite,
) : Brush()

data class RadialGradient(
    val colorStops: List<Pair<Float, Color>>,
    val center: Offset = Offset.Zero,
    val radius: Float = Float.POSITIVE_INFINITY,
) : Brush()

data class SweepGradient(
    val colorStops: List<Pair<Float, Color>>,
    val center: Offset = Offset.Zero,
) : Brush()

fun Brush.Companion.linearGradient(
    vararg colorStops: Pair<Float, Color>,
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
): Brush = LinearGradient(colorStops.toList(), start, end)

fun Brush.Companion.horizontalGradient(
    vararg colorStops: Pair<Float, Color>,
    startX: Float = 0f,
    endX: Float = Float.POSITIVE_INFINITY,
): Brush = LinearGradient(colorStops.toList(), androidx.compose.ui.geometry.Offset(startX, 0f), androidx.compose.ui.geometry.Offset(endX, 0f))

fun Brush.Companion.verticalGradient(
    vararg colorStops: Pair<Float, Color>,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush = LinearGradient(colorStops.toList(), androidx.compose.ui.geometry.Offset(0f, startY), androidx.compose.ui.geometry.Offset(0f, endY))

fun Brush.Companion.radialGradient(
    vararg colorStops: Pair<Float, Color>,
    center: Offset = Offset.Zero,
    radius: Float = Float.POSITIVE_INFINITY,
): Brush = RadialGradient(colorStops.toList(), center, radius)

fun Brush.Companion.sweepGradient(
    vararg colorStops: Pair<Float, Color>,
    center: Offset = Offset.Zero,
): Brush = SweepGradient(colorStops.toList(), center)
