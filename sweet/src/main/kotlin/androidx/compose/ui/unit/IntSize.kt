package androidx.compose.ui.unit

@JvmInline
value class IntSize(val packedValue: Long) {
    val width: Int get() = (packedValue shr 32).toInt()
    val height: Int get() = packedValue.toInt() and 0xFFFFFFFF.toInt()

    companion object {
        val Zero: IntSize = IntSize(0L)
    }
}

fun IntSize(
    width: Int,
    height: Int,
): IntSize =
    IntSize((width.toLong() shl 32) or (height.toLong() and 0xFFFFFFFFL))
