package androidx.compose.ui.semantics

import androidx.compose.runtime.Immutable

/**
 * The type of user interface element. Accessibility services might use this to describe the
 * element or do customizations.
 *
 * Mirrors MPP `androidx.compose.ui.semantics.Role` (defined in SemanticsProperties.kt). Sweet
 * currently records the role for API compatibility only; SWT native widgets carry their own
 * platform accessibility roles.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Role private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /** This element is a button control. */
        val Button = Role(0)

        /** This element is a Checkbox: a component that represents two states. */
        val Checkbox = Role(1)

        /** This element is a Switch: a two-state toggleable component. */
        val Switch = Role(2)

        /** This element is a RadioButton: selected or not selected. */
        val RadioButton = Role(3)

        /** This element is a Tab representing a single page of content. */
        val Tab = Role(4)

        /** This element is an image. */
        val Image = Role(5)

        /** This element is associated with a drop down menu. */
        val DropdownList = Role(6)

        /** This element is a value picker. */
        val ValuePicker = Role(7)

        /** This element is a Carousel of items. */
        val Carousel = Role(8)
    }

    override fun toString() =
        when (this) {
            Button -> "Button"
            Checkbox -> "Checkbox"
            Switch -> "Switch"
            RadioButton -> "RadioButton"
            Tab -> "Tab"
            Image -> "Image"
            DropdownList -> "DropdownList"
            ValuePicker -> "Picker"
            Carousel -> "Carousel"
            else -> "Unknown"
        }
}
