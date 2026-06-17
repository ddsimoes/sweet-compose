@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createCheckbox
import io.github.ddsimoes.sweet.internal.createRadioButton
import io.github.ddsimoes.sweet.internal.createToggleButton
import io.github.ddsimoes.sweet.internal.createWidgetCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Button

/**
 * A checkbox backed by an SWT Button with CHECK style.

 * **Sweet note:** [RadioButton] is now a real radio button backed by SWT.RADIO.
 * [Switch] is now a real toggle switch backed by SWT.TOGGLE.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val applier = LocalSWTNodeApplier.current

    var selected by remember { mutableStateOf(false) }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val checkbox = createCheckbox(applier)
            val scope = checkbox.createWidgetCoroutineScope()
            DirectSWTNode(
                checkbox.apply {
                    val listener =
                        object : SelectionAdapter() {
                            override fun widgetSelected(e: SelectionEvent) {
                                val button = e.source as Button
                                val newChecked = button.selection

                                @Suppress("UNCHECKED_CAST")
                                val callback = getData("onCheckedChange") as? ((Boolean) -> Unit)
                                // Keep visual state driven by Compose `checked` parameter
                                // even if the callback chooses not to update state.
                                button.selection = selected
                                scope.launch {
                                    callback?.invoke(newChecked)
                                }
                            }
                        }
                    addSelectionListener(listener)
                },
            )
        },
        update = {
            set(checked) {
                val button = this.control as Button
                selected = it
                if (!button.isDisposed) {
                    button.selection = it
                }
            }
            set(onCheckedChange ?: {}) { (this.control as Button).setData("onCheckedChange", it) }
            set(modifier) { applySWTModifier(this.control, it) }
            set(enabled) {
                val button = this.control as Button
                if (!button.isDisposed) {
                    button.enabled = it
                }
            }
        },
    )
}

/**
 * A Material Design switch backed by an SWT Button with TOGGLE style.
 *
 * **Sweet note:** This uses SWT.TOGGLE for a native toggle-switch appearance distinct
 * from SWT.CHECK (used by [Checkbox]).
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val applier = LocalSWTNodeApplier.current

    var selected by remember { mutableStateOf(false) }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val toggle = createToggleButton(applier)
            val scope = toggle.createWidgetCoroutineScope()
            DirectSWTNode(
                toggle.apply {
                    val listener =
                        object : SelectionAdapter() {
                            override fun widgetSelected(e: SelectionEvent) {
                                val button = e.source as Button
                                val newChecked = button.selection

                                @Suppress("UNCHECKED_CAST")
                                val callback = getData("__sweet_switch_onCheckedChange") as? ((Boolean) -> Unit)
                                // Keep visual state driven by Compose `checked` parameter
                                // even if the callback chooses not to update state.
                                button.selection = selected
                                scope.launch {
                                    callback?.invoke(newChecked)
                                }
                            }
                        }
                    addSelectionListener(listener)
                },
            )
        },
        update = {
            set(checked) {
                val button = this.control as Button
                selected = it
                if (!button.isDisposed) {
                    button.selection = it
                }
            }
            set(onCheckedChange ?: {}) { (this.control as Button).setData("__sweet_switch_onCheckedChange", it) }
            set(modifier) { applySWTModifier(this.control, it) }
            set(enabled) {
                val button = this.control as Button
                if (!button.isDisposed) {
                    button.enabled = it
                }
            }
        },
    )
}

/**
 * A Material Design radio button backed by an SWT Button with [org.eclipse.swt.SWT.RADIO] style.
 *
 * SWT RADIO buttons within the same parent [org.eclipse.swt.widgets.Composite] automatically
 * participate in mutual exclusion — selecting one deselects all others in the same container.
 * No additional grouping or state management is required.
 *
 * **Sweet note:** The [colors] parameter is accepted for API compatibility but is
 * currently ignored — native SWT radio button styling is used. The
 * [interactionSource] parameter is also accepted but ignored.
 *
 * @param selected whether this radio button is selected
 * @param onClick called when this radio button is clicked
 * @param modifier the [Modifier] to be applied to this radio button
 * @param enabled controls the enabled state of this radio button
 * @param colors [RadioButtonColors] that will be used to resolve colors (currently ignored)
 * @param interactionSource an optional [MutableInteractionSource] for observing interactions (currently ignored)
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val applier = LocalSWTNodeApplier.current

    var rememberedSelected by remember { mutableStateOf(false) }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val button = createRadioButton(applier)
            val scope = button.createWidgetCoroutineScope()
            DirectSWTNode(
                button.apply {
                    val listener =
                        object : SelectionAdapter() {
                            override fun widgetSelected(e: SelectionEvent) {
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val callback = getData("__sweet_radio_onClick") as? (() -> Unit)
                                    // Only fire onClick when becoming selected,
                                    // not when deselected by a peer radio button
                                    if (selection) {
                                        // Reset to Compose-driven state; the update block
                                        // will apply the authoritative value on recomposition
                                        selection = rememberedSelected
                                        scope.launch {
                                            callback?.invoke()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    addSelectionListener(listener)
                },
            )
        },
        update = {
            set(onClick) {
                val btn = this.control as Button
                if (!btn.isDisposed) btn.setData("__sweet_radio_onClick", it)
            }
            set(modifier) { applySWTModifier(this.control, it) }
            set(enabled) {
                val btn = this.control as Button
                if (!btn.isDisposed) btn.enabled = it
            }
            set(selected) {
                val btn = this.control as Button
                rememberedSelected = it
                if (!btn.isDisposed) {
                    btn.selection = it
                }
            }
        },
    )
}

/**
 * Colors for [RadioButton].
 *
 * **Sweet note:** Currently a shim — color values are accepted for API compatibility
 * but ignored. Native SWT radio button styling is used instead.
 */
@Immutable
class RadioButtonColors(
    val selectedColor: Color = Color.Unspecified,
    val unselectedColor: Color = Color.Unspecified,
    val disabledSelectedColor: Color = Color.Unspecified,
    val disabledUnselectedColor: Color = Color.Unspecified,
)

/**
 * Defaults for [RadioButton].
 *
 * **Sweet note:** Currently a shim — returns default [RadioButtonColors] whose values
 * are ignored. Native SWT radio button styling is used instead.
 */
object RadioButtonDefaults {
    /**
     * Creates a [RadioButtonColors] that will be used for the RadioButton.
     * Currently a shim — colors are ignored.
     */
    @Composable
    fun colors() = RadioButtonColors()
}
