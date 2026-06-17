package androidx.compose.ui.geometry

import androidx.compose.runtime.Stable

@Stable
data class CornerRadius(val x: Float, val y: Float) {
    companion object {
        val Zero: CornerRadius = CornerRadius(0f, 0f)
    }
}

@Stable
fun CornerRadius(radius: Float): CornerRadius = CornerRadius(radius, radius)
