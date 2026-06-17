package androidx.compose.ui.layout

import androidx.compose.ui.geometry.Size

sealed class ContentScale {
    companion object {
        val Fit: ContentScale = FitImpl
        val Crop: ContentScale = CropImpl
        val FillBounds: ContentScale = FillBoundsImpl
        val Inside: ContentScale = InsideImpl
        val None: ContentScale = NoneImpl

        fun FillWidth(centerCrop: Boolean = false): ContentScale =
            FillWidthImpl(centerCrop)

        fun FillHeight(centerCrop: Boolean = false): ContentScale =
            FillHeightImpl(centerCrop)
    }

    private object FitImpl : ContentScale()

    private object CropImpl : ContentScale()

    private object FillBoundsImpl : ContentScale()

    private object InsideImpl : ContentScale()

    private object NoneImpl : ContentScale()

    private data class FillWidthImpl(val centerCrop: Boolean = false) : ContentScale()

    private data class FillHeightImpl(val centerCrop: Boolean = false) : ContentScale()

    fun computeSize(
        srcWidth: Float,
        srcHeight: Float,
        dstWidth: Float,
        dstHeight: Float,
    ): Size =
        when (this) {
            is FitImpl -> computeFit(srcWidth, srcHeight, dstWidth, dstHeight)
            is CropImpl -> computeCrop(srcWidth, srcHeight, dstWidth, dstHeight)
            is FillBoundsImpl -> Size(dstWidth, dstHeight)
            is InsideImpl -> computeInside(srcWidth, srcHeight, dstWidth, dstHeight)
            is NoneImpl -> Size(srcWidth, srcHeight)
            is FillWidthImpl -> {
                val scale = dstWidth / srcWidth
                Size(dstWidth, srcHeight * scale)
            }
            is FillHeightImpl -> {
                val scale = dstHeight / srcHeight
                Size(srcWidth * scale, dstHeight)
            }
        }

    private fun computeFit(
        sw: Float,
        sh: Float,
        dw: Float,
        dh: Float,
    ): Size {
        val scale = minOf(dw / sw, dh / sh)
        return Size(sw * scale, sh * scale)
    }

    private fun computeCrop(
        sw: Float,
        sh: Float,
        dw: Float,
        dh: Float,
    ): Size {
        val scale = maxOf(dw / sw, dh / sh)
        return Size(sw * scale, sh * scale)
    }

    private fun computeInside(
        sw: Float,
        sh: Float,
        dw: Float,
        dh: Float,
    ): Size =
        if (sw <= dw && sh <= dh) {
            Size(sw, sh)
        } else {
            computeFit(sw, sh, dw, dh)
        }
}
