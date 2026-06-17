package io.github.ddsimoes.sweet.internal

import androidx.compose.foundation.layout.ALIGN_BY_BLOCK_KEY
import androidx.compose.foundation.layout.FillMaxHeightModifier
import androidx.compose.foundation.layout.FillMaxWidthModifier
import androidx.compose.foundation.layout.OffsetModifier
import androidx.compose.foundation.layout.PaddingModifier
import androidx.compose.foundation.layout.SizeInModifier
import androidx.compose.foundation.layout.SizeModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.LayoutDirection
import io.github.ddsimoes.sweet.data.getSweetCompositionData
import io.github.ddsimoes.sweet.data.getSweetData
import io.github.ddsimoes.sweet.data.setSweetData
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.layout.LayoutCoordinator
import io.github.ddsimoes.sweet.layout.SweetLayout
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.drawing.CANVAS_DRAW_CALLBACK_KEY
import io.github.ddsimoes.sweet.drawing.SweetDrawScope
import io.github.ddsimoes.sweet.drawing.SwtCanvasBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ControlAdapter
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.graphics.Region
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

// Keys used to track whether the modifier system currently "owns" a control's background /
// foreground. Reset-when-absent only clears colors the modifiers set themselves, so manual
// styling (e.g. Tab selection colors) is never clobbered.
private const val BG_OWNED_KEY = "sweet.modifier.bg.owned"
private const val FG_OWNED_KEY = "sweet.modifier.fg.owned"

/**
 * Reconcile a control's background against the current modifier chain.
 *
 * @param color the background requested by the last background modifier in the chain, or `null`
 *   if the chain contains no background modifier.
 */
private fun reconcileBackground(
    control: Control,
    color: Color?,
) {
    if (control.isDisposed) return
    val owned = control.getData(BG_OWNED_KEY) == true
    when {
        color != null && color != Color.Unspecified -> {
            control.background = control.display.getOrCreateColor(color.toSwtRgb())
            control.setData(BG_OWNED_KEY, true)
        }
        // Modifier explicitly requested "no override" (or removed the modifier): revert only the
        // background the modifier system previously set, leaving manually-styled colors intact.
        owned -> {
            control.background = null
            control.setData(BG_OWNED_KEY, false)
        }
    }
}

private fun reconcileForeground(
    control: Control,
    color: Color?,
) {
    if (control.isDisposed) return
    val owned = control.getData(FG_OWNED_KEY) == true
    when {
        color != null && color != Color.Unspecified -> {
            control.foreground = control.display.getOrCreateColor(color.toSwtRgb())
            control.setData(FG_OWNED_KEY, true)
        }
        owned -> {
            control.foreground = null
            control.setData(FG_OWNED_KEY, false)
        }
    }
}

private const val CLICKABLE_CALLBACK_KEY = "__clickable_callback"
private const val CLICKABLE_SCOPE_KEY = "__clickable_scope"
private const val CLICKABLE_PRESSED_KEY = "__clickable_pressed"
private const val CLICKABLE_ENABLED_KEY = "__clickable_enabled"

/**
 * Reconcile a control's click handler with desktop press-release semantics.
 *
 * Fires on press-release within bounds (MouseDown → MouseUp with containment
 * check via Display.getCursorLocation). Only button 1. When the chain no longer
 * contains a clickable element, the callback is set to `null`.
 *
 * @param control the SWT control
 * @param action the click callback, or null to disable
 * @param enabled whether clicks are accepted. Stored in control data and re-read on every
 *   mouse event (the listeners are installed once, so capturing the parameter would freeze
 *   the first value forever). A press started while enabled also won't fire if the modifier
 *   is disabled before release.
 */
private fun reconcileClickable(
    control: Control,
    action: (() -> Unit)?,
    enabled: Boolean = true,
) {
    if (control.isDisposed) return

    if (action == null) {
        if (control.getData(CLICKABLE_SCOPE_KEY) != null) {
            control.setData(CLICKABLE_CALLBACK_KEY, null)
            control.setData(CLICKABLE_ENABLED_KEY, null)
        }
        return
    }

    control.setData(CLICKABLE_CALLBACK_KEY, action)
    control.setData(CLICKABLE_ENABLED_KEY, enabled)

    if (control.getData(CLICKABLE_SCOPE_KEY) == null) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        control.setData(CLICKABLE_SCOPE_KEY, scope)

        // MouseDown: record press start (button 1 only, only while enabled)
        control.addListener(SWT.MouseDown) { event ->
            if (event.button == 1 && control.getData(CLICKABLE_ENABLED_KEY) != false) {
                control.setData(CLICKABLE_PRESSED_KEY, true)
            }
        }

        // MouseUp: fire callback if pressed, still enabled, and cursor still inside bounds
        control.addListener(SWT.MouseUp) { event ->
            if (event.button == 1 &&
                control.getData(CLICKABLE_PRESSED_KEY) == true &&
                control.getData(CLICKABLE_ENABLED_KEY) != false
            ) {
                control.setData(CLICKABLE_PRESSED_KEY, null)
                @Suppress("UNCHECKED_CAST")
                val callback = control.getData(CLICKABLE_CALLBACK_KEY) as? (() -> Unit)
                if (callback != null) {
                    // Use event coordinates (control-relative) for containment check.
                    // Falls back to global cursor position if event coords unavailable.
                    val ctrlX = event.x
                    val ctrlY = event.y
                    val bounds = control.bounds
                    // If control has zero size (not yet laid out), treat any click as within bounds.
                    val within =
                        bounds.width <= 0 || bounds.height <= 0 ||
                            ctrlX in 0 until bounds.width && ctrlY in 0 until bounds.height
                    if (within) {
                        scope.launch { callback() }
                    }
                }
            }
        }

        control.addDisposeListener {
            scope.cancel()
            control.setData(CLICKABLE_SCOPE_KEY, null)
            control.setData(CLICKABLE_CALLBACK_KEY, null)
            control.setData(CLICKABLE_PRESSED_KEY, null)
            control.setData(CLICKABLE_ENABLED_KEY, null)
        }
    }
}

// ── Draw modifier reconciliation ──────────────────────────────────────────

private const val DRAW_LISTENER_KEY = "sweet.modifier.draw.listener"
private const val DRAW_CALLBACKS_KEY = "sweet.modifier.draw.callbacks"

/**
 * Reconcile draw-modifier callbacks on a [Control].
 *
 * Draw modifiers are only supported on [Composite] containers. For leaf
 * widgets the callbacks are silently skipped with a debug log (Tier-3).
 *
 * @param control   the target SWT control
 * @param callbacks ordered list of draw callbacks to chain together
 */
private fun reconcileDraw(
    control: Control,
    callbacks: List<DrawScope.() -> Unit>,
) {
    if (control.isDisposed) return

    if (control !is Composite) {
        if (callbacks.isNotEmpty() && SweetDebugger.assertionEnabled) {
            SweetDebugger.log(
                "DrawModifier",
                "Draw modifiers are only supported on Composite containers; ignoring on ${control.javaClass.simpleName}",
            )
        }
        return
    }

    // Store the combined callback chain so the CanvasIntegration handler (or
    // our own PaintListener below) can read it.
    control.setData(DRAW_CALLBACKS_KEY, if (callbacks.isEmpty()) null else callbacks)

    val hasCanvasPath = control.getData(CANVAS_DRAW_CALLBACK_KEY) != null

    if (callbacks.isEmpty()) {
        // Remove existing draw hook.
        val listener = control.getData(DRAW_LISTENER_KEY) as? PaintListener
        if (listener != null) {
            control.removePaintListener(listener)
            control.setData(DRAW_LISTENER_KEY, null)
        }
        // If Canvas owns the paint path, trigger redraw to clear the
        // background previously drawn via drawBehind callbacks.
        if (hasCanvasPath) {
            control.redraw()
        }
        return
    }

    // If the control already has a Canvas (onDraw) paint path, the
    // CanvasIntegration handler will pick up the callbacks from
    // DRAW_CALLBACKS_KEY and draw them behind the onDraw content.
    // Do NOT install a second PaintListener — that would paint on top.
    if (hasCanvasPath) {
        control.redraw()
        return
    }

    // Standard path: no Canvas onDraw → install our own PaintListener.
    // Install the PaintListener if not already present.
    if (control.getData(DRAW_LISTENER_KEY) == null) {
        val listener =
            object : PaintListener {
                override fun paintControl(e: PaintEvent) {
                    if (control.isDisposed) return
                    @Suppress("UNCHECKED_CAST")
                    val cbs =
                        control.getData(DRAW_CALLBACKS_KEY) as? List<DrawScope.() -> Unit>
                            ?: return
                    if (cbs.isEmpty()) return

                    val gc = e.gc
                    gc.advanced = true
                    val clientArea = control.clientArea
                    val backend = SwtCanvasBackend(gc, control.display)
                    val size = Size(clientArea.width.toFloat(), clientArea.height.toFloat())
                    val density = control.display.getSweetDensity()
                    val scope =
                        SweetDrawScope(
                            canvas = backend,
                            initialSize = size,
                            layoutDirection = LayoutDirection.Ltr,
                            density = density.density,
                            fontScale = density.fontScale,
                        )
                    for (cb in cbs) {
                        cb.invoke(scope)
                    }
                }
            }
        control.addPaintListener(listener)
        control.setData(DRAW_LISTENER_KEY, listener)

        // Redraw on resize so draw callbacks always see current bounds.
        control.addControlListener(
            object : ControlAdapter() {
                override fun controlResized(e: ControlEvent) {
                    if (!control.isDisposed) {
                        control.redraw()
                    }
                }
            },
        )
    }
}

// ── Clip modifier reconciliation ───────────────────────────────────────────

private const val CLIP_SHAPE_KEY = "sweet.modifier.clip.shape"
private const val CLIP_REGION_KEY = "sweet.modifier.clip.region"
private const val CLIP_LISTENER_KEY = "sweet.modifier.clip.listener"

/**
 * Reconcile clip-modifier on a [Control].
 *
 * When [shape] is non-null, the control's SWT [Region] is set to the
 * bounding outline of the shape. A resize listener keeps it in sync.
 * When [shape] is null (modifier removed), the region is cleared.
 */
private fun reconcileClip(
    control: Control,
    shape: Shape?,
) {
    if (control.isDisposed) return

    if (shape == null) {
        // Clear clip.
        control.region = null
        val listener = control.getData(CLIP_LISTENER_KEY) as? ControlAdapter
        if (listener != null) {
            control.removeControlListener(listener)
        }
        control.setData(CLIP_SHAPE_KEY, null)
        control.setData(CLIP_REGION_KEY, null)
        control.setData(CLIP_LISTENER_KEY, null)
        return
    }

    control.setData(CLIP_SHAPE_KEY, shape)

    // Install resize listener once.
    if (control.getData(CLIP_LISTENER_KEY) == null) {
        val resizeListener =
            object : ControlAdapter() {
                override fun controlResized(e: ControlEvent) {
                    applyClipRegion(control)
                }
            }
        control.addControlListener(resizeListener)
        control.setData(CLIP_LISTENER_KEY, resizeListener)
    }

    applyClipRegion(control)
}

private fun applyClipRegion(control: Control) {
    if (control.isDisposed) return
    val shape = control.getData(CLIP_SHAPE_KEY) as? Shape ?: return

    val bounds = control.bounds
    val size = Size(bounds.width.toFloat(), bounds.height.toFloat())
    if (size.width <= 0f || size.height <= 0f) return

    val density = control.display.getSweetDensity()
    val outline = shape.createOutline(size, LayoutDirection.Ltr, density)

    val region = Region()
    when (outline) {
        is Outline.Rectangle -> {
            val r = outline.rect
            region.add(
                r.left.toInt(),
                r.top.toInt(),
                r.width.toInt(),
                r.height.toInt(),
            )
        }
        is Outline.Rounded -> {
            tessellateRoundRect(region, outline.roundRect)
        }
        is Outline.Generic -> {
            tessellatePath(region, outline.path)
        }
    }

    // Dispose previous region before overwriting.
    val previous = control.getData(CLIP_REGION_KEY) as? Region
    if (previous != null && !previous.isDisposed) {
        previous.dispose()
    }
    control.setData(CLIP_REGION_KEY, region)
    control.region = region
}

fun applySWTModifier(
    control: Control,
    modifier: Modifier,
) {
    if (control.isDisposed) {
        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log(
                "applySWTModifier",
                "Skipping modifier application - control is disposed: ${control.javaClass.simpleName}[${
                    control.hashCode().toString(16)
                }]",
            )
        }
        return
    }

    val oldData = control.getSweetData() as? SweetCompositionData

    // Reset → fold → assign. All modifier-derived child layout/style/event state is rebuilt from
    var data = control.getSweetCompositionData().resetModifierDerived()
    var background: Color? = null
    var foreground: Color? = null
    var clickAction: (() -> Unit)? = null
    var clickEnabled = true
    var focusAction: ((Boolean) -> Unit)? = null
    var doubleClickAction: (() -> Unit)? = null
    var contextMenuAction: (() -> Unit)? = null
    var alignByBlock: Any? = null
    var hoverAction: ((Boolean) -> Unit)? = null
    val drawCallbacks = mutableListOf<DrawScope.() -> Unit>()
    var clipShape: Shape? = null

    try {
        modifier.foldIn(Unit) { _, element ->
            when (element) {
                is LayoutDataModifier -> data = element.applyTo(data)
                is BackgroundColorElement -> background = element.backgroundColor
                is ForegroundColorElement -> foreground = element.foregroundColor
                is ClickableElement -> {
                    clickAction = element.clickAction
                    clickEnabled = element.clickEnabled
                }
                is FocusChangedElement -> focusAction = element.focusAction
                is DoubleClickElement -> doubleClickAction = element.doubleClickAction
                is ContextMenuElement -> contextMenuAction = element.contextMenuAction
                is HoverableElement -> hoverAction = element.hoverAction
                is AlignByBlockElement -> alignByBlock = element.alignByBlockData
                is DrawModifierElement -> drawCallbacks.add(element.drawCallback)
                is ClipModifierElement -> clipShape = element.shape
                is SWTModifierElement -> {
                    // Markers / not-yet-modeled elements (scroll config, menu anchor): keep their
                    // legacy single-shot behavior.
                    element.apply(control)
                }
                else -> {}
            }
        }

        // Build the ordered modifier chain (doc 12).
        val chain = ResolvedModifierChain(buildModifierChain(modifier))
        data = data.withModifierChain(chain)

        // SWT/GTK quirk: once a Region is set on a Composite, anything the
        // parent paints (native background color or PaintListener output) is
        // rendered OVER its child widgets, hiding them (reproduced with pure
        // SWT, independent of backgroundMode). So a clipped composite that
        // paints must not use a Region: route the background color through
        // the draw path and clip all draw callbacks to the shape outline at
        // draw time instead. The Region is kept only for clipped composites
        // that paint nothing themselves (children stay visible there).
        var clipShapeForRegion = clipShape
        val clippedShape = clipShape
        if (clippedShape != null && control is Composite) {
            val effectiveBackground = background
            if (effectiveBackground != null && effectiveBackground != Color.Unspecified) {
                drawCallbacks.add(0) { drawRect(color = effectiveBackground, size = size) }
                background = null
            }
            if (drawCallbacks.isNotEmpty()) {
                val wrapped = drawCallbacks.toList()
                drawCallbacks.clear()
                drawCallbacks.add {
                    val path =
                        when (val outline = clippedShape.createOutline(size, layoutDirection, this)) {
                            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                            is Outline.Generic -> outline.path
                        }
                    clipPath(path) {
                        for (cb in wrapped) cb.invoke(this)
                    }
                }
                clipShapeForRegion = null
            }
        }

        // Assign the derived layout data.
        control.setSweetData(data)

        // Reconcile visual + event capabilities with reset-when-absent semantics.
        reconcileBackground(control, background)
        reconcileForeground(control, foreground)
        reconcileClickable(control, clickAction, clickEnabled)
        reconcileFocusEvents(control, focusAction)
        reconcileDoubleClick(control, doubleClickAction)
        reconcileContextMenu(control, contextMenuAction)
        reconcileHover(control, hoverAction)
        reconcileDraw(control, drawCallbacks)
        reconcileClip(control, clipShapeForRegion)
        control.setData(ALIGN_BY_BLOCK_KEY, alignByBlock)
    } catch (e: org.eclipse.swt.SWTException) {
        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log(
                "applySWTModifier",
                "SWTException applying modifiers to control ${control.javaClass.simpleName}[${
                    control.hashCode().toString(16)
                }]",
                e,
            )
        }
        // Silently ignore SWT exceptions for disposed widgets during recomposition.
        if (e.code != SWT.ERROR_WIDGET_DISPOSED && !control.isDisposed) {
            throw e
        }
    }

    // If layout-affecting modifier data changed, invalidate the nearest layout root.
    // This covers state-only recomposition where the modifier value changes but no
    // structural tree update fires onEndChanges (e.g. AnimationDemo padding animation).
    if (oldData != null) {
        val layoutChanged =
            oldData.layoutData != data.layoutData ||
                oldData.constraintsData != data.constraintsData ||
                oldData.spacingData != data.spacingData ||
                oldData.layoutSpec != data.layoutSpec ||
                oldData.modifierChain != data.modifierChain
        if (layoutChanged) {
            // Invalidate the nearest ancestor that actually owns a SweetLayout. Interior
            // shadow-managed composites have a null SWT layout (the pass is driven by the
            // topmost SweetLayout composite), and calling layout() on a null-layout
            // composite is a no-op — so requesting layout on a bare control.parent silently
            // drops the invalidation when the parent is interior (e.g. a Box child of a
            // Column during the AnimationDemo padding animation). Walking up to the first
            // SweetLayout-bearing ancestor lets SweetLayout.layout drive/escalate the pass.
            var target: Composite? = control.parent
            while (target != null && target.layout !is SweetLayout) {
                target = target.parent
            }
            if (target != null && !target.isDisposed) {
                LayoutCoordinator.forDisplay(target.display).requestLayout(target)
            }
        }
    }
}

// ── Focus reconciliation ──────────────────────────────────────────────────

private const val FOCUS_ACTION_KEY = "__sweet_focus_action"

/**
 * Reconcile focus events on a control. Installs SWT.FocusIn/FocusOut listeners
 * lazily and swaps the callback on every apply.
 */
internal fun reconcileFocusEvents(
    control: Control,
    action: ((Boolean) -> Unit)?,
) {
    if (control.isDisposed) return

    control.setData(FOCUS_ACTION_KEY, action)

    if (action != null && control.getData("__sweet_focus_listener_installed") == null) {
        control.setData("__sweet_focus_listener_installed", true)

        control.addListener(SWT.FocusIn) {
            @Suppress("UNCHECKED_CAST")
            val cb = control.getData(FOCUS_ACTION_KEY) as? ((Boolean) -> Unit)
            cb?.invoke(true)
        }

        control.addListener(SWT.FocusOut) {
            @Suppress("UNCHECKED_CAST")
            val cb = control.getData(FOCUS_ACTION_KEY) as? ((Boolean) -> Unit)
            cb?.invoke(false)
        }
    }
}

// ── Double-click reconciliation ───────────────────────────────────────────

private const val DOUBLE_CLICK_ACTION_KEY = "__sweet_double_click_action"

/**
 * Reconcile double-click events on a control.
 */
internal fun reconcileDoubleClick(
    control: Control,
    action: (() -> Unit)?,
) {
    if (control.isDisposed) return

    control.setData(DOUBLE_CLICK_ACTION_KEY, action)

    if (action != null && control.getData("__sweet_double_click_installed") == null) {
        control.setData("__sweet_double_click_installed", true)

        control.addListener(SWT.MouseDoubleClick) {
            @Suppress("UNCHECKED_CAST")
            val cb = control.getData(DOUBLE_CLICK_ACTION_KEY) as? (() -> Unit)
            cb?.invoke()
        }
    }
}

// ── Context menu reconciliation ───────────────────────────────────────────

private const val CONTEXT_MENU_ACTION_KEY = "__sweet_context_menu_action"

/**
 * Reconcile right-click / context menu events on a control.
 */
internal fun reconcileContextMenu(
    control: Control,
    action: (() -> Unit)?,
) {
    if (control.isDisposed) return

    control.setData(CONTEXT_MENU_ACTION_KEY, action)

    if (action != null && control.getData("__sweet_context_menu_installed") == null) {
        control.setData("__sweet_context_menu_installed", true)

        control.addListener(SWT.MenuDetect) {
            @Suppress("UNCHECKED_CAST")
            val cb = control.getData(CONTEXT_MENU_ACTION_KEY) as? (() -> Unit)
            cb?.invoke()
        }
    }
}

// ── Hover reconciliation ───────────────────────────────────────────────────

private const val HOVER_ACTION_KEY = "__sweet_hover_action"
private const val HOVER_INSTALLED_KEY = "__sweet_hover_installed"

/**
 * Reconcile hover enter/exit events on a control.
 * Installs SWT.MouseEnter/MouseExit listeners lazily and swaps the callback on every apply.
 */
internal fun reconcileHover(
    control: Control,
    action: ((Boolean) -> Unit)?,
) {
    control.setData(HOVER_ACTION_KEY, action)

    if (action != null && control.getData(HOVER_INSTALLED_KEY) != true) {
        control.setData(HOVER_INSTALLED_KEY, true)

        control.addListener(SWT.MouseEnter) {
            @Suppress("UNCHECKED_CAST")
            val cb = control.getData(HOVER_ACTION_KEY) as? ((Boolean) -> Unit)
            cb?.invoke(true)
        }

        control.addListener(SWT.MouseExit) {
            @Suppress("UNCHECKED_CAST")
            val cb = control.getData(HOVER_ACTION_KEY) as? ((Boolean) -> Unit)
            cb?.invoke(false)
        }
    }
}

// ── Modifier chain builder (doc 12 ordered chain) ──────────────────────────

/**
 * Walks the Compose modifier chain in outside-in order and builds an ordered list
 * of [LayoutStep] for recognized layout modifiers.
 *
 * Non-layout modifiers (background, clickable, draw, etc.) are skipped — they are
 * handled by the flat-fold reconciliation path.
 */
internal fun buildModifierChain(modifier: Modifier): List<LayoutStep> {
    val steps = mutableListOf<LayoutStep>()
    modifier.foldIn(Unit) { _, element ->
        when (element) {
            is PaddingModifier ->
                steps.add(
                    PaddingStep(element.left, element.top, element.right, element.bottom),
                )
            is SizeModifier ->
                steps.add(
                    SizeStep(
                        width = if (element.width > 0) element.width else null,
                        height = if (element.height > 0) element.height else null,
                    ),
                )
            is SizeInModifier ->
                steps.add(
                    SizeInStep(
                        minWidth = element.minWidth,
                        maxWidth = element.maxWidth,
                        minHeight = element.minHeight,
                        maxHeight = element.maxHeight,
                    ),
                )
            is FillMaxWidthModifier -> steps.add(FillMaxWidthStep())
            is FillMaxHeightModifier -> steps.add(FillMaxHeightStep())
            is OffsetModifier -> steps.add(OffsetStep(element.x, element.y))
            // Other parent-data elements (weight, alignment, aspectRatio) are
            // consumed by the parent's delegate — they stay in the flat data bag.
            else -> {}
        }
    }
    return steps
}
