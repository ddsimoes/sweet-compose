package androidx.compose.runtime

/**
 * Type-safe wrapper for mutableStateOf<Int>.
 *
 * **Sweet note:** This is a boxed shim — it calls `mutableStateOf<Int>(value)` without
 * primitive specialization. Unlike Jetpack Compose, there is no performance benefit over
 * `mutableStateOf(value)`.
 *
 * Creates a MutableState<Int> with the given initial value.
 */
fun mutableIntStateOf(value: Int): MutableState<Int> = mutableStateOf(value)

/**
 * Type-safe wrapper for mutableStateOf<Float>.
 *
 * **Sweet note:** This is a boxed shim — it calls `mutableStateOf<Float>(value)` without
 * primitive specialization. Unlike Jetpack Compose, there is no performance benefit over
 * `mutableStateOf(value)`.
 *
 * Creates a MutableState<Float> with the given initial value.
 */
fun mutableFloatStateOf(value: Float): MutableState<Float> = mutableStateOf(value)
