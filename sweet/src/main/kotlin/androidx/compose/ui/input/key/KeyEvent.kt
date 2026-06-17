package androidx.compose.ui.input.key

import org.eclipse.swt.SWT

/**
 * Represents keys on a keyboard, mapped from SWT key codes.
 *
 * For character keys (A-Z, 0-9, Space), the [keyCode] is the character code.
 * For special keys, the [keyCode] is the SWT constant (e.g. [SWT.CR], [SWT.ARROW_LEFT]).
 */
enum class Key(val keyCode: Long) {
    Unknown(0),

    // Letters — SWT uses lowercase character codes for key events
    A('a'.code.toLong()),
    B('b'.code.toLong()),
    C('c'.code.toLong()),
    D('d'.code.toLong()),
    E('e'.code.toLong()),
    F('f'.code.toLong()),
    G('g'.code.toLong()),
    H('h'.code.toLong()),
    I('i'.code.toLong()),
    J('j'.code.toLong()),
    K('k'.code.toLong()),
    L('l'.code.toLong()),
    M('m'.code.toLong()),
    N('n'.code.toLong()),
    O('o'.code.toLong()),
    P('p'.code.toLong()),
    Q('q'.code.toLong()),
    R('r'.code.toLong()),
    S('s'.code.toLong()),
    T('t'.code.toLong()),
    U('u'.code.toLong()),
    V('v'.code.toLong()),
    W('w'.code.toLong()),
    X('x'.code.toLong()),
    Y('y'.code.toLong()),
    Z('z'.code.toLong()),

    // Digits
    Zero('0'.code.toLong()),
    One('1'.code.toLong()),
    Two('2'.code.toLong()),
    Three('3'.code.toLong()),
    Four('4'.code.toLong()),
    Five('5'.code.toLong()),
    Six('6'.code.toLong()),
    Seven('7'.code.toLong()),
    Eight('8'.code.toLong()),
    Nine('9'.code.toLong()),

    // Function keys
    F1(SWT.F1.toLong()),
    F2(SWT.F2.toLong()),
    F3(SWT.F3.toLong()),
    F4(SWT.F4.toLong()),
    F5(SWT.F5.toLong()),
    F6(SWT.F6.toLong()),
    F7(SWT.F7.toLong()),
    F8(SWT.F8.toLong()),
    F9(SWT.F9.toLong()),
    F10(SWT.F10.toLong()),
    F11(SWT.F11.toLong()),
    F12(SWT.F12.toLong()),

    // Special keys
    Enter(SWT.CR.code.toLong()),
    Escape(SWT.ESC.code.toLong()),
    Space(SWT.SPACE.code.toLong()),
    Tab(SWT.TAB.code.toLong()),
    Backspace(SWT.BS.code.toLong()),
    Delete(SWT.DEL.code.toLong()),

    // Navigation
    ArrowLeft(SWT.ARROW_LEFT.toLong()),
    ArrowRight(SWT.ARROW_RIGHT.toLong()),
    ArrowUp(SWT.ARROW_UP.toLong()),
    ArrowDown(SWT.ARROW_DOWN.toLong()),
    Home(SWT.HOME.toLong()),
    End(SWT.END.toLong()),
    PageUp(SWT.PAGE_UP.toLong()),
    PageDown(SWT.PAGE_DOWN.toLong()),

    // Modifiers
    Shift(SWT.SHIFT.toLong()),
    Control(SWT.CTRL.toLong()),
    Alt(SWT.ALT.toLong()),
    Meta(SWT.COMMAND.toLong()),
    ;

    companion object {
        /** Returns the [Key] matching [keyCode], or [Unknown] if no match. */
        fun fromKeyCode(keyCode: Long): Key {
            val kc =
                if (keyCode in 'A'.code.toLong()..'Z'.code.toLong()) {
                    // Normalize uppercase to lowercase (SWT on GTK sends lowercase)
                    keyCode + ('a'.code - 'A'.code)
                } else {
                    keyCode
                }
            return entries.find { it.keyCode == kc } ?: Unknown
        }
    }
}

/**
 * The type of a key event.
 *
 * Mirrors SWT's [SWT.KeyDown] and [SWT.KeyUp] event types.
 */
enum class KeyEventType {
    /** Key pressed down. */
    KeyDown,

    /** Key released. */
    KeyUp,

    /** Unknown event type. */
    Unknown,
}

/**
 * A keyboard key event carrying the [key], [type], and modifier state.
 *
 * Construct from an SWT event via the secondary constructor.
 * Modifier flags ([isCtrlPressed], [isAltPressed], [isShiftPressed], [isMetaPressed])
 * are directly accessible as properties of this class.
 */
class KeyEvent(
    val key: Key,
    val type: KeyEventType,
    val nativeKeyCode: Int,
    val isCtrlPressed: Boolean,
    val isAltPressed: Boolean,
    val isShiftPressed: Boolean,
    val isMetaPressed: Boolean,
) {
    /**
     * Constructs a [KeyEvent] from an [org.eclipse.swt.widgets.Event].
     *
     * Extracts the [Key] from [org.eclipse.swt.widgets.Event.keyCode],
     * the [KeyEventType] from [org.eclipse.swt.widgets.Event.type],
     * and modifier flags from [org.eclipse.swt.widgets.Event.stateMask].
     */
    constructor(event: org.eclipse.swt.widgets.Event) : this(
        key = Key.fromKeyCode(event.keyCode.toLong()),
        type =
            when (event.type) {
                SWT.KeyDown -> KeyEventType.KeyDown
                SWT.KeyUp -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
        nativeKeyCode = event.keyCode,
        isCtrlPressed = (event.stateMask and SWT.CTRL) != 0,
        isAltPressed = (event.stateMask and SWT.ALT) != 0,
        isShiftPressed = (event.stateMask and SWT.SHIFT) != 0,
        isMetaPressed = (event.stateMask and SWT.COMMAND) != 0,
    )
}
