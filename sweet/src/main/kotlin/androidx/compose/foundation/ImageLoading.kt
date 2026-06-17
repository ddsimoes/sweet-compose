package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.ddsimoes.sweet.compose.locals.LocalDisplay
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.image.ImageBitmapLoader
import io.github.ddsimoes.sweet.image.SweetImageBitmapImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.swt.widgets.Display
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * Display-aware cache keyed by (Display, path) so bitmaps for one display
 * are never served to another, avoiding disposal-cross-display crashes.
 */
private val imageCache = mutableMapOf<Pair<Display, String>, WeakReference<ImageBitmap>>()
private val imageCacheMutex = Mutex()

private suspend fun getCachedImage(
    display: Display,
    path: String,
): ImageBitmap? =
    imageCacheMutex.withLock {
        imageCache[display to path]?.get()?.let { cached ->
            val backend = (cached as? SweetImageBitmapImpl)?.backend
            if (backend != null) cached else null
        }
    }

private suspend fun cacheImage(
    display: Display,
    path: String,
    bitmap: ImageBitmap,
) {
    imageCacheMutex.withLock {
        imageCache[display to path] = WeakReference(bitmap)
    }
}

internal fun clearImageCache() {
    imageCache.clear()
}

@Composable
fun rememberImageBitmap(path: String): ImageBitmap? {
    val display = LocalDisplay.current

    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(path, display) {
        // 1) Check the display-aware cache first.
        val cached = getCachedImage(display, path)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }

        // 2) Yield until SWT has processed an event, so the initial
        // frame can be laid out and painted.
        suspendCancellableCoroutine<Unit> { cont ->
            if (display.isDisposed) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }
            display.asyncExec {
                if (!cont.isCompleted) {
                    cont.resume(Unit)
                }
            }
        }

        // 3) Decode ImageData off the UI thread.
        val imageData =
            withContext(Dispatchers.IO) {
                ImageBitmapLoader.loadImageData(path)
            }

        // 4) Back on the SWT main dispatcher, create the SWT Image
        //    and update Compose state. Skip the path-only cache;
        //    we store in our own display-aware cache.
        val loaded = ImageBitmapLoader.loadFromImageData(path, imageData, display, useCache = false)
        cacheImage(display, path, loaded)
        bitmap = loaded
        SweetDebugger.log("ImageLoading", "Image $path loaded")
    }

    DisposableEffect(bitmap) {
        // Capture the current bitmap instance so we dispose exactly
        // the image that was active for this effect key, avoiding
        // races when the state changes.
        val toDispose = bitmap as? SweetImageBitmapImpl
        onDispose {
            toDispose?.backend?.dispose()
        }
    }

    return bitmap
}
