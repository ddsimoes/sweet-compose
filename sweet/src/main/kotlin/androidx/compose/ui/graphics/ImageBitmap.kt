package androidx.compose.ui.graphics

/**
 * Basic bitmap abstraction for Sweet's Compose UI layer.
 *
 * This is intentionally minimal for the initial Image support:
 * it exposes only width and height. Creation helpers are provided
 * via the [companion object] for SWT-backed images.
 */
interface ImageBitmap {
    val width: Int
    val height: Int

    companion object
}
