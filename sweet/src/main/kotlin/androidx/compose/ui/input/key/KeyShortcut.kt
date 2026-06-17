package androidx.compose.ui.input.key

/**
 * Represents a keyboard shortcut that can be attached to menu items
 */
class KeyShortcut(
    internal val key: Key,
    internal val ctrl: Boolean = false,
    internal val meta: Boolean = false,
    internal val alt: Boolean = false,
    internal val shift: Boolean = false,
) {
    override fun toString(): String {
        val modifiers = mutableListOf<String>()
        if (ctrl) modifiers.add("Ctrl")
        if (meta) modifiers.add("Meta")
        if (alt) modifiers.add("Alt")
        if (shift) modifiers.add("Shift")

        return if (modifiers.isNotEmpty()) {
            "${modifiers.joinToString("+")}+${key.keyCode}"
        } else {
            key.keyCode.toString()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyShortcut) return false

        return key == other.key &&
            ctrl == other.ctrl &&
            meta == other.meta &&
            alt == other.alt &&
            shift == other.shift
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + ctrl.hashCode()
        result = 31 * result + meta.hashCode()
        result = 31 * result + alt.hashCode()
        result = 31 * result + shift.hashCode()
        return result
    }
}

/**
 * Convenience functions for common shortcuts
 */
object CommonShortcuts {
    val Copy = KeyShortcut(Key.C, ctrl = true)
    val Cut = KeyShortcut(Key.X, ctrl = true)
    val Paste = KeyShortcut(Key.V, ctrl = true)
    val Undo = KeyShortcut(Key.Z, ctrl = true)
    val Redo = KeyShortcut(Key.Y, ctrl = true)
    val SelectAll = KeyShortcut(Key.A, ctrl = true)
    val New = KeyShortcut(Key.N, ctrl = true)
    val Open = KeyShortcut(Key.O, ctrl = true)
    val Save = KeyShortcut(Key.S, ctrl = true)
    val SaveAs = KeyShortcut(Key.S, ctrl = true, shift = true)
    val Find = KeyShortcut(Key.F, ctrl = true)
    val Replace = KeyShortcut(Key.H, ctrl = true)
    val Quit = KeyShortcut(Key.Q, ctrl = true)
}
