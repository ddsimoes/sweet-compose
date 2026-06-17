package io.github.ddsimoes.sweet.test

import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Display
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Pins [SweetLayout.measurementFont] to DejaVu Sans Mono 11pt on the
 * current Display. Call at the start of each test inside `autoSWT { }`
 * to ensure deterministic text measurements.
 *
 * The font TTF is loaded from test resources once per JVM process.
 *
 * Usage:
 * ```
 * @Test fun test() = autoSWT {
 *     useMeasurementFont()
 *     testShell(...) { ... }.test { ... }
 * }
 * ```
 */
fun useMeasurementFont() {
    ensureFontLoaded(Display.getDefault())
    if (Display.getDefault().isDisposed) return
    disposeMeasurementFont()
    val font = Font(Display.getDefault(), "DejaVu Sans Mono", 11, SWT.NORMAL)
    SweetLayout.measurementFont = font
}

/**
 * Disposes the current [SweetLayout.measurementFont] and clears it.
 * Safe to call when no font is set.
 */
fun disposeMeasurementFont() {
    val font = SweetLayout.measurementFont
    SweetLayout.measurementFont = null
    if (font != null && !font.isDisposed) {
        font.dispose()
    }
}

// ── Internal ──────────────────────────────────────────────────────────

private var fontLoaded = false

@Synchronized
private fun ensureFontLoaded(display: Display) {
    if (fontLoaded) return
    // Display.loadFont registers the font with the font system (fontconfig on Linux).
    // This persists across Display instances.
    val tempFile: Path = Files.createTempFile("dejavu-", ".ttf")
    try {
        val stream: InputStream =
            checkNotNull(
                MeasurementFont::class.java.getResourceAsStream("/fonts/DejaVuSansMono.ttf"),
            ) { "Measurement font not found in test resources at /fonts/DejaVuSansMono.ttf" }
        Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
        stream.close()
        display.loadFont(tempFile.toString())
    } finally {
        tempFile.toFile().delete()
    }
    fontLoaded = true
}

/** Namespace holder for the resource lookup. */
private object MeasurementFont
