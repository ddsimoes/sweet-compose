package androidx.compose.foundation.interaction

/**
 * An Interaction represents transient UI state for a component, typically separate from the
 * actual 'business' state that a component may control. For example, a button typically fires
 * an `onClick` callback when the button is pressed and released, but it may still want to show
 * that it is being pressed before this callback is fired. This transient state is represented
 * by an Interaction.
 *
 * To emit / observe current Interactions, see [MutableInteractionSource], which represents a
 * stream of Interactions present for a given component.
 *
 * @see InteractionSource
 * @see MutableInteractionSource
 */
interface Interaction
