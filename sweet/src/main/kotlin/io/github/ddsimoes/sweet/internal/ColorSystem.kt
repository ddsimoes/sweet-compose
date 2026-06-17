package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.graphics.Color
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display

private const val COLOR_CACHE_KEY = "sweet.color.cache"

private fun Display.getOrCreateColorCache(): MutableMap<Int, org.eclipse.swt.graphics.Color> {
    @Suppress("UNCHECKED_CAST")
    val existing = getData(COLOR_CACHE_KEY) as? MutableMap<Int, org.eclipse.swt.graphics.Color>
    if (existing != null) return existing

    val map = mutableMapOf<Int, org.eclipse.swt.graphics.Color>()
    setData(COLOR_CACHE_KEY, map)

    // Add dispose listener to free all cached colors when the display is disposed
    disposeExec {
        map.values.forEach { color ->
            if (!color.isDisposed) {
                color.dispose()
            }
        }
        map.clear()
    }

    return map
}

private fun RGB.toColorCacheKey(): Int = (red shl 16) or (green shl 8) or blue

internal fun Display.getOrCreateColor(rgb: RGB): org.eclipse.swt.graphics.Color {
    val key = rgb.toColorCacheKey()
    val cache = getOrCreateColorCache()
    return cache.getOrPut(key) {
        org.eclipse.swt.graphics
            .Color(this, rgb)
    }
}

internal fun Display.getSweetColorCacheSize(): Int {
    @Suppress("UNCHECKED_CAST")
    val cache = getData(COLOR_CACHE_KEY) as? Map<Int, org.eclipse.swt.graphics.Color>
    return cache?.size ?: 0
}

internal fun Display.hasSweetCachedColor(color: Color): Boolean {
    @Suppress("UNCHECKED_CAST")
    val cache = getData(COLOR_CACHE_KEY) as? Map<Int, org.eclipse.swt.graphics.Color>
    return cache?.containsKey(color.toSwtRgb().toColorCacheKey()) == true
}

// ---- font cache ----

private const val FONT_CACHE_KEY = "sweet.font.cache"

private data class CachedFontKey(
    val name: String,
    val height: Float,
    val style: Int,
)

private fun org.eclipse.swt.graphics.FontData.toCacheKey(): CachedFontKey =
    CachedFontKey(name, height, style)

private fun Display.getOrCreateFontCache(): MutableMap<CachedFontKey, org.eclipse.swt.graphics.Font> {
    @Suppress("UNCHECKED_CAST")
    val existing = getData(FONT_CACHE_KEY) as? MutableMap<CachedFontKey, org.eclipse.swt.graphics.Font>
    if (existing != null) return existing

    val map = mutableMapOf<CachedFontKey, org.eclipse.swt.graphics.Font>()
    setData(FONT_CACHE_KEY, map)

    // Dispose all cached fonts when the display is disposed
    disposeExec {
        map.values.forEach { font ->
            if (!font.isDisposed) {
                font.dispose()
            }
        }
        map.clear()
    }

    return map
}

internal fun Display.getOrCreateFont(fontData: org.eclipse.swt.graphics.FontData): org.eclipse.swt.graphics.Font {
    val key = fontData.toCacheKey()
    val cache = getOrCreateFontCache()
    return cache.getOrPut(key) {
        org.eclipse.swt.graphics.Font(this, fontData)
    }
}

internal fun Display.getSweetFontCacheSize(): Int {
    @Suppress("UNCHECKED_CAST")
    val cache = getData(FONT_CACHE_KEY) as? Map<CachedFontKey, org.eclipse.swt.graphics.Font>
    return cache?.size ?: 0
}

/**
 * Applies the Compose text styling parameters to an SWT control's font.
 *
 * The font is derived from the display's system font: `fontSize` (sp) is converted to
 * points (1sp ≈ 1px at 96dpi → 0.75pt), and `fontWeight`/`fontStyle` map to
 * [org.eclipse.swt.SWT.BOLD]/[org.eclipse.swt.SWT.ITALIC]. Fonts are shared via the
 * display-scoped cache, so callers never dispose them. Passing all-default parameters
 * resets the control to its inherited font.
 */
internal fun Control.applyTextFont(
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontStyle: androidx.compose.ui.text.font.FontStyle?,
    fontWeight: androidx.compose.ui.text.font.FontWeight?,
) {
    if (isDisposed) return
    val hasSize = fontSize != androidx.compose.ui.unit.TextUnit.Unspecified && fontSize.value > 0f
    val bold = (fontWeight?.weight ?: 400) >= 600
    val italic = fontStyle == androidx.compose.ui.text.font.FontStyle.Italic
    if (!hasSize && !bold && !italic) {
        font = null // inherit the default font
        return
    }
    val base = display.systemFont.fontData[0]
    val heightPoints: Int =
        if (hasSize) {
            Math.round(fontSize.value * 0.75f).coerceAtLeast(1)
        } else {
            Math.round(base.height)
        }
    var swtStyle = org.eclipse.swt.SWT.NORMAL
    if (bold) swtStyle = swtStyle or org.eclipse.swt.SWT.BOLD
    if (italic) swtStyle = swtStyle or org.eclipse.swt.SWT.ITALIC
    val fontData = org.eclipse.swt.graphics.FontData(base.name, heightPoints, swtStyle)
    font = display.getOrCreateFont(fontData)
}

internal fun Control.applyBackground(color: Color) {
    if (!isDisposed && color != Color.Unspecified) {
        background = display.getOrCreateColor(color.toSwtRgb())
    }
}

internal fun Control.applyForeground(color: Color) {
    if (!isDisposed && color != Color.Unspecified) {
        foreground = display.getOrCreateColor(color.toSwtRgb())
    }
}

internal class BackgroundColorModifier(
    private val color: Color,
) : BackgroundColorElement {
    override val backgroundColor: Color get() = color

    override fun apply(control: Control) {
        control.applyBackground(color)
    }
}

internal class ForegroundColorModifier(
    private val color: Color,
) : ForegroundColorElement {
    override val foregroundColor: Color get() = color

    override fun apply(control: Control) {
        control.applyForeground(color)
    }
}
