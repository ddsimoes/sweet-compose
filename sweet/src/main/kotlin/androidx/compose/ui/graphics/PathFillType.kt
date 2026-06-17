package androidx.compose.ui.graphics

/** Filling algorithm for paths. */
enum class PathFillType {
    /** Even-odd fill rule. */
    EvenOdd,

    /** Non-zero winding fill rule (default). */
    NonZero,
}
