package io.github.ddsimoes.sweet.debug

import org.eclipse.swt.widgets.Display
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Centralized debugging utility for Sweet Compose SWT
 */
object SweetDebugger {
    private var enabled = false // Disabled by default for cleaner output
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    fun enable() {
        enabled = true
    }
    
    fun disable() {
        enabled = false
    }
    
    fun log(component: String, message: String, throwable: Throwable? = null) {
        if (!enabled) return
        
        val timestamp = LocalTime.now().format(timeFormatter)
        val threadName = Thread.currentThread().name
        val threadId = Thread.currentThread().id
        val isUIThread = try {
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
    
    inline fun <T> trace(component: String, operation: String, block: () -> T): T {
        log(component, "→ $operation")
        val start = System.nanoTime()
        return try {
            val result = block()
            val duration = (System.nanoTime() - start) / 1_000_000.0
            log(component, "← $operation completed (${String.format("%.2f", duration)}ms)")
            result
        } catch (e: Exception) {
            val duration = (System.nanoTime() - start) / 1_000_000.0
            log(component, "← $operation failed (${String.format("%.2f", duration)}ms)", e)
            throw e
        }
    }
    
    fun logWidgetTree(display: Display?) {
        if (!enabled || display == null) return
        
        log("WIDGET_TREE", "=== Widget Tree Dump ===")
        try {
            display.shells.forEachIndexed { index, shell ->
                log("WIDGET_TREE", "Shell[$index]: ${shell.text} (${shell.bounds})")
                dumpWidget(shell, 1)
            }
        } catch (e: Exception) {
            log("WIDGET_TREE", "Failed to dump widget tree", e)
        }
        log("WIDGET_TREE", "=== End Widget Tree ===")
    }
    
    private fun dumpWidget(widget: org.eclipse.swt.widgets.Widget, depth: Int) {
        val indent = "  ".repeat(depth)
        val widgetInfo = when (widget) {
            is org.eclipse.swt.widgets.Composite -> {
                val children = widget.children
                log("WIDGET_TREE", "$indent${widget.javaClass.simpleName}: layout=${widget.layout}, children=${children.size}")
                children.forEach { dumpWidget(it, depth + 1) }
            }
            is org.eclipse.swt.widgets.Control -> {
                log("WIDGET_TREE", "$indent${widget.javaClass.simpleName}: bounds=${widget.bounds}, visible=${widget.visible}")
            }
            else -> {
                log("WIDGET_TREE", "$indent${widget.javaClass.simpleName}")
            }
        }
    }
}