package io.github.ddsimoes.sweet.debug

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Centralized debugging utility for Sweet Compose SWT
 */
object SweetDebugger {
    val assertionEnabled =
        try {
            assert(false)
            false
        } catch (_: AssertionError) {
            true
        }

    private var enabled = assertionEnabled // Disabled by default for cleaner output
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun log(
        component: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (!enabled) return

        val timestamp = LocalTime.now().format(timeFormatter)
        val threadName = Thread.currentThread().name
        val threadId = Thread.currentThread().id
        val isUIThread =
            try {
                Display.getCurrent() != null
            } catch (e: Exception) {
                false
            }

        val threadInfo = "$threadName[$threadId]${if (isUIThread) "[UI]" else "[BG]"}"
        val logLine = "[$timestamp] [$component] [$threadInfo] $message"

        println(logLine)

        throwable?.let {
            println("[$timestamp] [$component] [$threadInfo] Exception: ${it.message}")
            it.printStackTrace()
        }
    }

    fun logWidgetTree(composite: Composite) {
        if (!enabled) return

        log("WIDGET_TREE", "=== Widget Tree Dump ===")
        try {
            dumpWidget(composite, 1)
        } catch (e: Exception) {
            log("WIDGET_TREE", "Failed to dump widget tree", e)
        }
        log("WIDGET_TREE", "=== End Widget Tree ===")
    }

    private fun dumpWidget(
        widget: Control,
        depth: Int,
    ) {
        val indent = "  ".repeat(depth)
        when (widget) {
            is Composite -> {
                val children = widget.children
                log("WIDGET_TREE", "$indent${widget.javaClass.simpleName}: layout=${widget.layout}, children=${children.size}, bounds=${widget.bounds}, visible=${widget.visible}")
                children.forEach { dumpWidget(it, depth + 1) }
            }

            is Text -> {
                log(
                    "WIDGET_TREE",
                    "$indent${widget.javaClass.simpleName}(${widget.text}): bounds=${widget.bounds}, visible=${widget.visible}",
                )
            }

            is Label -> {
                log(
                    "WIDGET_TREE",
                    "$indent${widget.javaClass.simpleName}(${widget.text}): bounds=${widget.bounds}, visible=${widget.visible}",
                )
            }

            is Button -> {
                log(
                    "WIDGET_TREE",
                    "$indent${widget.javaClass.simpleName}(${widget.text}): bounds=${widget.bounds}, visible=${widget.visible}, ${if (widget.style and SWT.CHECK != 0) "CHECK" else ""}",
                )
            }

            else -> {
                log("WIDGET_TREE", "$indent${widget.javaClass.simpleName}: bounds=${widget.bounds}, visible=${widget.visible}")
            }
        }
    }
}
