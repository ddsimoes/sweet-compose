package io.github.ddsimoes.sweet.image

import androidx.compose.ui.graphics.ImageBitmap
import org.eclipse.swt.graphics.Device
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.PaletteData
import org.eclipse.swt.widgets.Display
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.ref.WeakReference

/**
 * Internal wrapper for platform-specific bitmap resources.
 */
internal interface SweetImageBitmap {
    val width: Int
    val height: Int

    val config: ImageBitmapConfig

    fun dispose()
}

/**
 * Configuration for on- and off-screen pixel buffers.
 */
enum class ImageBitmapConfig {
    Argb8888,
    Rgb565,
    Alpha8,
}

/**
 * Raw pixel data extracted from a bitmap.
 */
data class PixelMap(
    val width: Int,
    val height: Int,
    val buffer: ByteArray,
    val pixelStride: Int,
)

/**
 * SWT-backed bitmap wrapper.
 *
 * Owns the underlying [Image]; disposing this wrapper disposes the SWT resource.
 */
internal class SwtImageBitmap(
    val swtImage: Image,
    private val display: Display,
) : SweetImageBitmap {
    override val width: Int
        get() = swtImage.bounds.width

    override val height: Int
        get() = swtImage.bounds.height

    override val config: ImageBitmapConfig
        get() =
            when (swtImage.imageData.depth) {
                16 -> ImageBitmapConfig.Rgb565
                8 -> ImageBitmapConfig.Alpha8
                else -> ImageBitmapConfig.Argb8888
            }

    override fun dispose() {
        if (!swtImage.isDisposed) {
            swtImage.dispose()
        }
    }

    /**
     * Extract raw pixel data from this bitmap.
     */
    fun toPixelMap(): PixelMap {
        val imageData = swtImage.imageData
        return PixelMap(
            width = imageData.width,
            height = imageData.height,
            buffer = imageData.data,
            pixelStride = imageData.depth / 8,
        )
    }

    companion object {
        /**
         * Create an offscreen SWT [Image] and wrap it as a [SwtImageBitmap].
         *
         * The returned bitmap owns the underlying native resource;
         * disposing it disposes the SWT [Image].
         */
        fun createOffscreen(
            width: Int,
            height: Int,
            config: ImageBitmapConfig,
            display: Display,
        ): SwtImageBitmap {
            val depth: Int
            val palette: PaletteData
            when (config) {
                ImageBitmapConfig.Argb8888 -> {
                    depth = 32
                    palette = PaletteData(0xFF0000.toInt(), 0xFF00, 0xFF)
                }
                ImageBitmapConfig.Rgb565 -> {
                    depth = 16
                    palette = PaletteData(0xF800, 0x7E0, 0x1F)
                }
                ImageBitmapConfig.Alpha8 -> {
                    depth = 8
                    // Grayscale palette for alpha-only images.
                    palette =
                        PaletteData(
                            *Array(256) { i ->
                                org.eclipse.swt.graphics.RGB(i, i, i)
                            },
                        )
                }
            }
            val imageData = ImageData(width, height, depth, palette)
            val image = Image(display, imageData)
            return SwtImageBitmap(image, display)
        }
    }
}

/**
 * Display-independent bitmap backed by decoded [ImageData].
 *
 * Created by `androidx.compose.ui.res.loadImageBitmap`, which has no display
 * parameter (matching the MPP signature) and may run off the UI thread:
 * decoding to [ImageData] is thread-safe, while the native SWT [Image] is
 * materialized lazily via [swtImageFor] on the device that first draws it.
 */
internal class ImageDataImageBitmap(
    internal val imageData: ImageData,
) : SweetImageBitmap {
    private var cachedImage: Image? = null
    private var cachedDevice: Device? = null

    override val width: Int
        get() = imageData.width

    override val height: Int
        get() = imageData.height

    override val config: ImageBitmapConfig
        get() =
            when (imageData.depth) {
                16 -> ImageBitmapConfig.Rgb565
                8 -> ImageBitmapConfig.Alpha8
                else -> ImageBitmapConfig.Argb8888
            }

    /**
     * Return the SWT [Image] for [device], creating it on first use.
     * Must be called on the SWT UI thread.
     */
    fun swtImageFor(device: Device): Image {
        val existing = cachedImage
        if (existing != null && !existing.isDisposed && cachedDevice == device) {
            return existing
        }
        existing?.takeUnless { it.isDisposed }?.dispose()
        val image = Image(device, imageData)
        cachedImage = image
        cachedDevice = device
        if (device is Display) {
            device.disposeExec {
                if (!image.isDisposed) {
                    image.dispose()
                }
            }
        }
        return image
    }

    override fun dispose() {
        cachedImage?.takeUnless { it.isDisposed }?.dispose()
        cachedImage = null
        cachedDevice = null
    }
}

/**
 * ImageBitmap implementation backed by [SweetImageBitmap].
 */
internal class SweetImageBitmapImpl(
    internal val backend: SweetImageBitmap,
) : ImageBitmap {
    override val width: Int
        get() = backend.width

    override val height: Int
        get() = backend.height
}

/**
 * Utilities for creating and loading [ImageBitmap] instances.
 *
 * All loader methods that create a new SWT [Image] transfer ownership to
 * the returned [ImageBitmap]: disposing the bitmap disposes the native resource.
 * The optional path-based cache uses [WeakReference] so bitmaps are collected
 * when no longer referenced; call [clearCache] to purge stale entries.
 */
object ImageBitmapLoader {
    private val cache = mutableMapOf<String, WeakReference<ImageBitmap>>()

    /**
     * Load raw [ImageData] from the given [path].
     *
     * This operation does not touch SWT widgets and can safely be
     * executed on a background thread.
     */
    fun loadImageData(path: String): ImageData = ImageData(path)

    /**
     * Create an [ImageBitmap] from already-decoded [ImageData].
     *
     * The actual SWT [Image] allocation must happen on the SWT UI
     * thread, but callers are free to decode [ImageData] off-thread
     * and hand it to this helper.
     *
     * The returned bitmap owns the underlying SWT [Image].
     */
    fun loadFromImageData(
        path: String?,
        imageData: ImageData,
        display: Display,
        useCache: Boolean = true,
    ): ImageBitmap {
        if (useCache && path != null) {
            cache[path]?.get()?.let { cached ->
                val backend = (cached as? SweetImageBitmapImpl)?.backend as? SwtImageBitmap
                if (backend != null && !backend.swtImage.isDisposed) {
                    return cached
                }
            }
        }

        val swtImage = Image(display, imageData)
        val bitmap: ImageBitmap = SweetImageBitmapImpl(SwtImageBitmap(swtImage, display))

        if (useCache && path != null) {
            cache[path] = WeakReference(bitmap)
        }

        return bitmap
    }

    /**
     * Load an image from the given [path], optionally using a simple
     * in-memory cache keyed by the path.
     *
     * Callers are responsible for invoking this on the SWT UI thread.
     * The returned bitmap owns the underlying SWT [Image].
     */
    fun loadFromPath(
        path: String,
        display: Display,
        useCache: Boolean = true,
    ): ImageBitmap {
        val data = loadImageData(path)
        return loadFromImageData(path, data, display, useCache)
    }

    /**
     * Load an image from the given [bytes].
     *
     * The returned bitmap owns the underlying SWT [Image]; disposing
     * the bitmap disposes the native resource. Must be called on the SWT UI thread.
     */
    fun loadFromBytes(
        bytes: ByteArray,
        display: Display,
    ): ImageBitmap {
        val input = ByteArrayInputStream(bytes)
        val swtImage = Image(display, input)
        return SweetImageBitmapImpl(SwtImageBitmap(swtImage, display))
    }

    /**
     * Load an image from the given [inputStream].
     *
     * The caller is responsible for closing [inputStream].
     * The returned bitmap owns the underlying SWT [Image].
     */
    fun loadFromStream(
        inputStream: InputStream,
        display: Display,
    ): ImageBitmap {
        val swtImage = Image(display, inputStream)
        return SweetImageBitmapImpl(SwtImageBitmap(swtImage, display))
    }

    /**
     * Clear entries whose bitmaps have been GC'd.
     */
    fun clearCache() {
        cache.entries.removeIf { it.value.get() == null }
    }
}

/**
 * Create an [ImageBitmap] from an existing SWT [Image].
 *
 * **Ownership:** the returned [ImageBitmap] takes ownership of [image].
 * Disposing the bitmap (or letting it be collected by a [DisposableEffect])
 * will dispose the underlying SWT [Image]. The caller MUST NOT dispose
 * [image] independently after wrapping it.
 */
fun ImageBitmap.Companion.fromSwtImage(
    image: Image,
    display: Display,
): ImageBitmap = SweetImageBitmapImpl(SwtImageBitmap(image, display))
