package androidx.compose.ui.geometry

sealed class Outline {
    data class Rectangle(val rect: Rect) : Outline()

    data class Rounded(val roundRect: RoundRect) : Outline()

    data class Generic(val path: androidx.compose.ui.graphics.Path) : Outline()
}
