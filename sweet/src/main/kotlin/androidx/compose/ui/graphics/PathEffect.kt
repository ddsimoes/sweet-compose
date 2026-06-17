package androidx.compose.ui.graphics

sealed class PathEffect {
    companion object {
        /** Creates a dashed line effect. */
        fun dashPathEffect(
            intervals: FloatArray,
            phase: Float = 0f,
        ): PathEffect =
            DashPathEffect(intervals, phase)
    }

    /** Dashed line effect. */
    data class DashPathEffect(
        val intervals: FloatArray,
        val phase: Float = 0f,
    ) : PathEffect() {
        override fun equals(other: Any?): Boolean =
            other is DashPathEffect && intervals.contentEquals(other.intervals) && phase == other.phase

        override fun hashCode(): Int = intervals.contentHashCode() * 31 + phase.hashCode()
    }
}
