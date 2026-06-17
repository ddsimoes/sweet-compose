@file:Suppress("ktlint:standard:filename")

package androidx.compose.ui.text.input

/**
 * Interface used for changing visual output of the input field.
 *
 * For SWT-backed TextField, the default implementation is a no-op — native SWT rendering
 * is used. [PasswordVisualTransformation] wires into SWT's native [org.eclipse.swt.SWT.PASSWORD]
 * flag for platform-level password masking.
 *
 * Note: the upstream `filter(AnnotatedString): TransformedText` abstract method is not
 * yet ported — this is a type-level shim for the `visualTransformation` parameter.
 */
open class VisualTransformation {
    companion object {
        /** A special visual transformation object indicating that no transformation is applied. */
        val None: VisualTransformation = VisualTransformation()
    }
}

/**
 * Visual transformation for password input fields.
 *
 * When applied to [androidx.compose.material3.TextField], sets the SWT.PASSWORD style flag
 * on the backing Text widget, enabling native platform password masking.
 *
 * @param mask The mask character (not used by SWT — native platform masking applies instead).
 */
class PasswordVisualTransformation(val mask: Char = '\u2022') : VisualTransformation() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PasswordVisualTransformation) return false
        return mask == other.mask
    }

    override fun hashCode(): Int = mask.hashCode()
}
