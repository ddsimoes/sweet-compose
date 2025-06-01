package io.github.ddsimoes.sweet.debug

import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*

/**
 * Utility for debugging SWT component trees by printing their structure and properties.
 */
object SwtDebugger {
    
    /**
     * Prints the complete structure of an SWT control tree starting from the given control.
     * 
     * @param control The root control to start printing from
     * @param title Optional title for the debug output
     */
    fun printTree(control: Control, title: String = "SWT Component Tree") {
        SwtDebugger.debug("═══════════════════════════════════════════════════════════════")
        SwtDebugger.debug("  $title")
        SwtDebugger.debug("═══════════════════════════════════════════════════════════════")
        printControlRecursive(control, 0)
        SwtDebugger.debug("═══════════════════════════════════════════════════════════════")
    }
    
    /**
     * Recursively prints control information with proper indentation.
     */
    private fun printControlRecursive(control: Control, depth: Int) {
        val indent = "  ".repeat(depth)
        val prefix = if (depth > 0) "├─ " else ""
        
        SwtDebugger.debug("$indent$prefix${getControlInfo(control)}")
        
        // Print children if this is a Composite
        if (control is Composite) {
            val children = control.children
            if (children.isNotEmpty()) {
                children.forEachIndexed { index, child ->
                    printControlRecursive(child, depth + 1)
                }
            } else if (depth > 0) {
                SwtDebugger.debug("$indent   └─ (no children)")
            }
        }
    }
    
    /**
     * Extracts comprehensive information about a control.
     */
    private fun getControlInfo(control: Control): String {
        val sb = StringBuilder()
        
        // Basic type and identity
        sb.append("${control::class.simpleName}")
        if (control is Shell) {
            sb.append(" \"${control.text}\"")
        }
        sb.append(" [${control.hashCode().toString(16)}]")
        
        // Position and size information
        val bounds = control.bounds
        val size = control.size
        sb.append(" pos=(${bounds.x}, ${bounds.y}) size=(${size.x}×${size.y})")
        
        // Visibility and state
        if (!control.visible) sb.append(" [HIDDEN]")
        if (!control.enabled) sb.append(" [DISABLED]")
        if (control.isDisposed) sb.append(" [DISPOSED]")
        
        // Layout information for Composites
        if (control is Composite) {
            val layout = control.layout
            sb.append(" layout=${getLayoutInfo(layout)}")
            
            // Layout data if control has a parent
            control.parent?.let { parent ->
                val layoutData = control.layoutData
                if (layoutData != null) {
                    sb.append(" layoutData=${getLayoutDataInfo(layoutData)}")
                }
            }
        } else {
            // Layout data for non-composite controls
            val layoutData = control.layoutData
            if (layoutData != null) {
                sb.append(" layoutData=${getLayoutDataInfo(layoutData)}")
            }
        }
        
        // Control-specific information
        sb.append(" ${getControlSpecificInfo(control)}")
        
        return sb.toString()
    }
    
    /**
     * Gets layout-specific information.
     */
    private fun getLayoutInfo(layout: Layout?): String {
        return when (layout) {
            null -> "null"
            is GridLayout -> "GridLayout(cols=${layout.numColumns}, equalWidth=${layout.makeColumnsEqualWidth})"
            is RowLayout -> "RowLayout(type=${if (layout.type == org.eclipse.swt.SWT.HORIZONTAL) "HORIZONTAL" else "VERTICAL"}, wrap=${layout.wrap})"
            is FillLayout -> "FillLayout(type=${if (layout.type == org.eclipse.swt.SWT.HORIZONTAL) "HORIZONTAL" else "VERTICAL"})"
            is FormLayout -> "FormLayout(marginWidth=${layout.marginWidth}, marginHeight=${layout.marginHeight})"
            // StackLayout is not available in all SWT versions
            // is StackLayout -> "StackLayout(topControl=${layout.topControl?.let { it::class.simpleName }})"
            else -> layout::class.simpleName ?: "Unknown"
        }
    }
    
    /**
     * Gets layout data information.
     */
    private fun getLayoutDataInfo(layoutData: Any): String {
        return when (layoutData) {
            is GridData -> {
                val ha = when (layoutData.horizontalAlignment) {
                    org.eclipse.swt.SWT.BEGINNING -> "START"
                    org.eclipse.swt.SWT.CENTER -> "CENTER"
                    org.eclipse.swt.SWT.END -> "END"
                    org.eclipse.swt.SWT.FILL -> "FILL"
                    else -> layoutData.horizontalAlignment.toString()
                }
                val va = when (layoutData.verticalAlignment) {
                    org.eclipse.swt.SWT.BEGINNING -> "START"
                    org.eclipse.swt.SWT.CENTER -> "CENTER"
                    org.eclipse.swt.SWT.END -> "END"
                    org.eclipse.swt.SWT.FILL -> "FILL"
                    else -> layoutData.verticalAlignment.toString()
                }
                "GridData(h=$ha v=$va grabH=${layoutData.grabExcessHorizontalSpace} grabV=${layoutData.grabExcessVerticalSpace} span=${layoutData.horizontalSpan}×${layoutData.verticalSpan})"
            }
            is RowData -> "RowData(${layoutData.width}×${layoutData.height})"
            is FormData -> "FormData(w=${layoutData.width} h=${layoutData.height})"
            else -> layoutData::class.simpleName ?: "Unknown"
        }
    }
    
    /**
     * Gets control-specific information based on control type.
     */
    private fun getControlSpecificInfo(control: Control): String {
        val info = mutableListOf<String>()
        
        when (control) {
            is Label -> {
                info.add("text=\"${control.text}\"")
                info.add("style=${getLabelStyle(control)}")
            }
            is Button -> {
                info.add("text=\"${control.text}\"")
                info.add("style=${getButtonStyle(control)}")
                info.add("selection=${control.selection}")
            }
            is Text -> {
                val text = if (control.text.length > 20) 
                    "${control.text.take(20)}..." 
                else control.text
                info.add("text=\"$text\"")
                info.add("editable=${control.editable}")
                info.add("chars=${control.textLimit}")
            }
            is org.eclipse.swt.widgets.List -> {
                info.add("items=${control.itemCount}")
                info.add("selection=${control.selectionIndex}")
                if (control.itemCount > 0) {
                    info.add("firstItem=\"${control.getItem(0)}\"")
                }
            }
            is Table -> {
                info.add("items=${control.itemCount}")
                info.add("columns=${control.columnCount}")
                info.add("selection=${control.selectionIndex}")
                info.add("headerVisible=${control.headerVisible}")
                info.add("linesVisible=${control.linesVisible}")
            }
            is Tree -> {
                info.add("items=${control.itemCount}")
                info.add("selection=${control.selectionCount}")
                info.add("headerVisible=${control.headerVisible}")
                info.add("linesVisible=${control.linesVisible}")
            }
            is Combo -> {
                info.add("items=${control.itemCount}")
                info.add("text=\"${control.text}\"")
                info.add("selection=${control.selectionIndex}")
            }
            is Composite -> {
                info.add("children=${control.children.size}")
                if (control is Group) {
                    info.add("text=\"${control.text}\"")
                }
                if (control is TabFolder) {
                    info.add("items=${control.itemCount}")
                    info.add("selection=${control.selectionIndex}")
                }
            }
            is ProgressBar -> {
                info.add("min=${control.minimum}")
                info.add("max=${control.maximum}")
                info.add("value=${control.selection}")
            }
            is Scale -> {
                info.add("min=${control.minimum}")
                info.add("max=${control.maximum}")
                info.add("value=${control.selection}")
                info.add("increment=${control.increment}")
                info.add("pageIncrement=${control.pageIncrement}")
            }
            is Slider -> {
                info.add("min=${control.minimum}")
                info.add("max=${control.maximum}")
                info.add("value=${control.selection}")
                info.add("thumb=${control.thumb}")
            }
            is Shell -> {
                info.add("title=\"${control.text}\"")
                info.add("minimized=${control.minimized}")
                info.add("maximized=${control.maximized}")
                info.add("active=${control == control.display.activeShell}")
            }
        }
        
        // Font information
        control.font?.let { font ->
            val fontData = font.fontData.firstOrNull()
            fontData?.let {
                info.add("font=\"${it.name}\" size=${it.height}")
            }
        }
        
        // Color information
        control.foreground?.let { color ->
            info.add("fg=rgb(${color.red},${color.green},${color.blue})")
        }
        control.background?.let { color ->
            info.add("bg=rgb(${color.red},${color.green},${color.blue})")
        }
        
        return info.joinToString(" ")
    }
    
    /**
     * Gets label style information.
     */
    private fun getLabelStyle(label: Label): String {
        val style = label.style
        val styles = mutableListOf<String>()
        
        if (style and org.eclipse.swt.SWT.CENTER != 0) styles.add("CENTER")
        if (style and org.eclipse.swt.SWT.LEFT != 0) styles.add("LEFT")
        if (style and org.eclipse.swt.SWT.RIGHT != 0) styles.add("RIGHT")
        if (style and org.eclipse.swt.SWT.WRAP != 0) styles.add("WRAP")
        if (style and org.eclipse.swt.SWT.SEPARATOR != 0) styles.add("SEPARATOR")
        
        return if (styles.isEmpty()) "NONE" else styles.joinToString("|")
    }
    
    /**
     * Gets button style information.
     */
    private fun getButtonStyle(button: Button): String {
        val style = button.style
        val styles = mutableListOf<String>()
        
        if (style and org.eclipse.swt.SWT.PUSH != 0) styles.add("PUSH")
        if (style and org.eclipse.swt.SWT.CHECK != 0) styles.add("CHECK")
        if (style and org.eclipse.swt.SWT.RADIO != 0) styles.add("RADIO")
        if (style and org.eclipse.swt.SWT.TOGGLE != 0) styles.add("TOGGLE")
        if (style and org.eclipse.swt.SWT.ARROW != 0) styles.add("ARROW")
        if (style and org.eclipse.swt.SWT.FLAT != 0) styles.add("FLAT")
        
        return if (styles.isEmpty()) "NONE" else styles.joinToString("|")
    }
    
    /**
     * Prints a summary of the tree structure.
     */
    fun printTreeSummary(control: Control) {
        val stats = gatherTreeStats(control)
        SwtDebugger.debug("═══════════════════════════════════════════════════════════════")
        SwtDebugger.debug("  SWT Tree Summary")
        SwtDebugger.debug("═══════════════════════════════════════════════════════════════")
        stats.forEach { (type, count) ->
            SwtDebugger.debug("  $type: $count")
        }
        SwtDebugger.debug("  Total Controls: ${stats.values.sum()}")
        SwtDebugger.debug("═══════════════════════════════════════════════════════════════")
    }
    
    /**
     * Gathers statistics about control types in the tree.
     */
    private fun gatherTreeStats(control: Control): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        gatherStatsRecursive(control, stats)
        return stats.toSortedMap()
    }
    
    /**
     * Recursively gathers statistics.
     */
    private fun gatherStatsRecursive(control: Control, stats: MutableMap<String, Int>) {
        val typeName = control::class.simpleName ?: "Unknown"
        stats[typeName] = stats.getOrDefault(typeName, 0) + 1
        
        if (control is Composite) {
            control.children.forEach { child ->
                gatherStatsRecursive(child, stats)
            }
        }
    }

    fun debug(message: String) {
        println("[${Thread.currentThread()}] $message" )
    }
}