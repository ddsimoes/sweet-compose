package androidx.compose.ui.unit

@JvmInline
value class IntOffset(val packedValue: Long) {
    val x: Int get() = (packedValue shr 32).toInt()
    val y: Int get() = packedValue.toInt() and 0xFFFFFFFF.toInt()

    companion object {
        val Zero: IntOffset = IntOffset(0L)
    }
}

fun IntOffset(
    x: Int,
    y: Int,
): IntOffset =
    IntOffset((x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL))
