package androidx.compose.ui.res

import androidx.compose.ui.graphics.ImageBitmap
import io.github.ddsimoes.sweet.image.ImageDataImageBitmap
import io.github.ddsimoes.sweet.image.SweetImageBitmapImpl
import org.eclipse.swt.graphics.ImageData
import java.io.InputStream

/**
 * Load and decode [ImageBitmap] from the given [inputStream]. [inputStream] should contain an
 * encoded raster image in a format supported by SWT (PNG, JPEG, GIF, BMP, ICO, TIFF).
 *
 * Matches the MPP Compose Desktop signature (`androidx.compose.ui.res.loadImageBitmap`).
 * Decoding happens immediately and is safe off the UI thread; the native SWT `Image` is
 * created lazily on the device that first draws the bitmap.
 *
 * @param inputStream input stream to load a raster image. All bytes will be read from this
 * stream, but the stream will not be closed after this method.
 * @return the decoded image as an [ImageBitmap]
 */
fun loadImageBitmap(inputStream: InputStream): ImageBitmap =
    SweetImageBitmapImpl(ImageDataImageBitmap(ImageData(inputStream)))
