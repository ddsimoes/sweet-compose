package androidx.compose.foundation.interaction

import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * InteractionSource represents a stream of [Interaction]s corresponding to events emitted by a
 * component. These [Interaction]s can be used to change how components appear in different
 * states, such as when a component is pressed or dragged.
 *
 * @see MutableInteractionSource
 * @see Interaction
 */
@Stable
interface InteractionSource {
    /**
     * [Flow] representing the stream of all [Interaction]s emitted through this
     * [InteractionSource].
     */
    val interactions: Flow<Interaction>
}

/**
 * MutableInteractionSource represents a stream of [Interaction]s corresponding to events
 * emitted by a component. It exposes [emit] and [tryEmit] functions that emit the provided
 * [Interaction] to the underlying [interactions] [Flow].
 *
 * An instance can be created with the [MutableInteractionSource] factory function. This
 * instance should be remembered before it is passed to other components that consume it.
 *
 * @see InteractionSource
 * @see Interaction
 */
@Stable
interface MutableInteractionSource : InteractionSource {
    /**
     * Emits [interaction] into [interactions]. This method is not thread-safe and should not
     * be invoked concurrently.
     *
     * @see tryEmit
     */
    suspend fun emit(interaction: Interaction)

    /**
     * Tries to emit [interaction] into [interactions] without suspending. It returns `true`
     * if the value was emitted successfully.
     *
     * @see emit
     */
    fun tryEmit(interaction: Interaction): Boolean
}

/**
 * Returns a new [MutableInteractionSource] that can be hoisted and provided to components,
 * allowing listening to [Interaction] changes inside those components.
 */
fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

@Stable
private class MutableInteractionSourceImpl : MutableInteractionSource {
    override val interactions =
        MutableSharedFlow<Interaction>(
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override suspend fun emit(interaction: Interaction) {
        interactions.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean = interactions.tryEmit(interaction)
}
