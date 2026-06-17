package androidx.compose.ui.graphics

/** Boolean operation on two paths. */
enum class PathOperation {
    Difference,
    Intersect,
    Union,
    Xor,
    ReverseDifference,
}
