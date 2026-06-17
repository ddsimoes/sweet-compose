package androidx.compose.ui

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class AlignmentTest {
    @Test
    fun horizontal_alignment_behaves_as_expected() {
        val container = IntSize(100, 1)
        val child = IntSize(40, 1)

        assertEquals(0, Alignment.Start.align(child, container, LayoutDirection.Ltr).x)
        assertEquals(30, Alignment.CenterHorizontally.align(child, container, LayoutDirection.Ltr).x)
        assertEquals(60, Alignment.End.align(child, container, LayoutDirection.Ltr).x)
    }

    @Test
    fun vertical_alignment_behaves_as_expected() {
        val container = IntSize(1, 90)
        val child = IntSize(1, 30)

        assertEquals(0, Alignment.Top.align(child, container, LayoutDirection.Ltr).y)
        assertEquals(30, Alignment.CenterVertically.align(child, container, LayoutDirection.Ltr).y)
        assertEquals(60, Alignment.Bottom.align(child, container, LayoutDirection.Ltr).y)
    }

    @Test
    fun combined_alignment_delegates_to_components() {
        val containerWidth = 200
        val containerHeight = 100
        val childWidth = 50
        val childHeight = 20

        val spaceH = IntSize(containerWidth, 1)
        val childH = IntSize(childWidth, 1)
        val spaceV = IntSize(1, containerHeight)
        val childV = IntSize(1, childHeight)

        val center = Alignment.Center
        val topStart = Alignment.TopStart

        assertEquals(
            Alignment.CenterHorizontally.align(childH, spaceH, LayoutDirection.Ltr).x,
            center.align(IntSize(childWidth, childHeight), IntSize(containerWidth, containerHeight), LayoutDirection.Ltr).x,
        )
        assertEquals(
            Alignment.CenterVertically.align(childV, spaceV, LayoutDirection.Ltr).y,
            center.align(IntSize(childWidth, childHeight), IntSize(containerWidth, containerHeight), LayoutDirection.Ltr).y,
        )
        assertEquals(
            Alignment.Start.align(childH, spaceH, LayoutDirection.Ltr).x,
            topStart.align(IntSize(childWidth, childHeight), IntSize(containerWidth, containerHeight), LayoutDirection.Ltr).x,
        )
        assertEquals(
            Alignment.Top.align(childV, spaceV, LayoutDirection.Ltr).y,
            topStart.align(IntSize(childWidth, childHeight), IntSize(containerWidth, containerHeight), LayoutDirection.Ltr).y,
        )
    }
}
