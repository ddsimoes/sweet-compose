/*
 * Copyright 2020 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0.
 */
// Ported from 3rdparty/compose-multiplatform-core.
// Sweet adaptations: JvmDefaultWithCompatibility→removed, Density.roundToPx→toPx,
// fastRoundToInt→roundToInt, 1D align(Int,Int)→2D align(IntSize,IntSize).x/.y.
package androidx.compose.foundation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
object Arrangement {
    @Stable
    interface Horizontal {
        val spacing get() = Dp.Unspecified

        fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray,
        )
    }

    @Stable
    interface Vertical {
        val spacing get() = Dp.Unspecified

        fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray,
        )
    }

    @Stable
    interface HorizontalOrVertical : Horizontal, Vertical {
        override val spacing: Dp get() = Dp.Unspecified
    }

    @Stable val Start =
        object : Horizontal {
            override fun Density.arrange(
                t: Int,
                s: IntArray,
                ld: LayoutDirection,
                o: IntArray,
            ) =
                if (ld == LayoutDirection.Ltr) placeLeftOrTop(s, o, false) else placeRightOrBottom(t, s, o, true)

            override fun toString() = "Arrangement#Start"
        }

    @Stable val End =
        object : Horizontal {
            override fun Density.arrange(
                t: Int,
                s: IntArray,
                ld: LayoutDirection,
                o: IntArray,
            ) =
                if (ld == LayoutDirection.Ltr) placeRightOrBottom(t, s, o, false) else placeLeftOrTop(s, o, true)

            override fun toString() = "Arrangement#End"
        }

    @Stable val Top =
        object : Vertical {
            override fun Density.arrange(
                t: Int,
                s: IntArray,
                o: IntArray,
            ) = placeLeftOrTop(s, o, false)

            override fun toString() = "Arrangement#Top"
        }

    @Stable val Bottom =
        object : Vertical {
            override fun Density.arrange(
                t: Int,
                s: IntArray,
                o: IntArray,
            ) = placeRightOrBottom(t, s, o, false)

            override fun toString() = "Arrangement#Bottom"
        }

    @Stable val Center =
        object : HorizontalOrVertical {
            override val spacing = Dp.Unspecified

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                ld: LayoutDirection,
                o: IntArray,
            ) =
                if (ld == LayoutDirection.Ltr) placeCenter(t, s, o, false) else placeCenter(t, s, o, true)

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                o: IntArray,
            ) = placeCenter(t, s, o, false)

            override fun toString() = "Arrangement#Center"
        }

    @Stable val SpaceEvenly =
        object : HorizontalOrVertical {
            override val spacing = Dp.Unspecified

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                ld: LayoutDirection,
                o: IntArray,
            ) =
                if (ld == LayoutDirection.Ltr) placeSpaceEvenly(t, s, o, false) else placeSpaceEvenly(t, s, o, true)

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                o: IntArray,
            ) = placeSpaceEvenly(t, s, o, false)

            override fun toString() = "Arrangement#SpaceEvenly"
        }

    @Stable val SpaceBetween =
        object : HorizontalOrVertical {
            override val spacing = Dp.Unspecified

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                ld: LayoutDirection,
                o: IntArray,
            ) =
                if (ld == LayoutDirection.Ltr) placeSpaceBetween(t, s, o, false) else placeSpaceBetween(t, s, o, true)

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                o: IntArray,
            ) = placeSpaceBetween(t, s, o, false)

            override fun toString() = "Arrangement#SpaceBetween"
        }

    @Stable val SpaceAround =
        object : HorizontalOrVertical {
            override val spacing = Dp.Unspecified

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                ld: LayoutDirection,
                o: IntArray,
            ) =
                if (ld == LayoutDirection.Ltr) placeSpaceAround(t, s, o, false) else placeSpaceAround(t, s, o, true)

            override fun Density.arrange(
                t: Int,
                s: IntArray,
                o: IntArray,
            ) = placeSpaceAround(t, s, o, false)

            override fun toString() = "Arrangement#SpaceAround"
        }

    // Sweet uses 2D Alignment.align(IntSize, IntSize, LayoutDirection): IntOffset,
    // so horizontal calls extract .x and vertical calls set layoutDirection to Ltr + extract .y.

    @Stable fun spacedBy(space: Dp): HorizontalOrVertical =
        SpacedAligned(space, true) { s, ld -> Alignment.Start.align(IntSize.Zero, IntSize(s, 0), ld).x }

    @Stable fun spacedBy(
        space: Dp,
        alignment: Alignment.Horizontal,
    ): Horizontal =
        SpacedAligned(space, true) { s, ld -> alignment.align(IntSize.Zero, IntSize(s, 0), ld).x }

    @Stable fun spacedBy(
        space: Dp,
        alignment: Alignment.Vertical,
    ): Vertical =
        SpacedAligned(space, false) { s, _ -> alignment.align(IntSize.Zero, IntSize(0, s), LayoutDirection.Ltr).y }

    @Stable fun aligned(alignment: Alignment.Horizontal): Horizontal =
        SpacedAligned(Dp.Unspecified, true) { s, ld -> alignment.align(IntSize.Zero, IntSize(s, 0), ld).x }

    @Stable fun aligned(alignment: Alignment.Vertical): Vertical =
        SpacedAligned(Dp.Unspecified, false) { s, _ -> alignment.align(IntSize.Zero, IntSize(0, s), LayoutDirection.Ltr).y }

    @Immutable
    object Absolute {
        @Stable val Left =
            object : Horizontal {
                override fun Density.arrange(
                    t: Int,
                    s: IntArray,
                    ld: LayoutDirection,
                    o: IntArray,
                ) = placeLeftOrTop(s, o, false)

                override fun toString() = "AbsoluteArrangement#Left"
            }

        @Stable val Center =
            object : Horizontal {
                override fun Density.arrange(
                    t: Int,
                    s: IntArray,
                    ld: LayoutDirection,
                    o: IntArray,
                ) = placeCenter(t, s, o, false)

                override fun toString() = "AbsoluteArrangement#Center"
            }

        @Stable val Right =
            object : Horizontal {
                override fun Density.arrange(
                    t: Int,
                    s: IntArray,
                    ld: LayoutDirection,
                    o: IntArray,
                ) = placeRightOrBottom(t, s, o, false)

                override fun toString() = "AbsoluteArrangement#Right"
            }

        @Stable val SpaceBetween =
            object : Horizontal {
                override fun Density.arrange(
                    t: Int,
                    s: IntArray,
                    ld: LayoutDirection,
                    o: IntArray,
                ) = placeSpaceBetween(t, s, o, false)

                override fun toString() = "AbsoluteArrangement#SpaceBetween"
            }

        @Stable val SpaceEvenly =
            object : Horizontal {
                override fun Density.arrange(
                    t: Int,
                    s: IntArray,
                    ld: LayoutDirection,
                    o: IntArray,
                ) = placeSpaceEvenly(t, s, o, false)

                override fun toString() = "AbsoluteArrangement#SpaceEvenly"
            }

        @Stable val SpaceAround =
            object : Horizontal {
                override fun Density.arrange(
                    t: Int,
                    s: IntArray,
                    ld: LayoutDirection,
                    o: IntArray,
                ) = placeSpaceAround(t, s, o, false)

                override fun toString() = "AbsoluteArrangement#SpaceAround"
            }

        @Stable fun spacedBy(space: Dp): HorizontalOrVertical = SpacedAligned(space, false, null)

        @Stable fun spacedBy(
            space: Dp,
            alignment: Alignment.Horizontal,
        ): Horizontal =
            SpacedAligned(space, false) { s, ld -> alignment.align(IntSize.Zero, IntSize(s, 0), ld).x }

        @Stable fun spacedBy(
            space: Dp,
            alignment: Alignment.Vertical,
        ): Vertical =
            SpacedAligned(space, false) { s, _ -> alignment.align(IntSize.Zero, IntSize(0, s), LayoutDirection.Ltr).y }

        @Stable fun aligned(alignment: Alignment.Horizontal): Horizontal =
            SpacedAligned(Dp.Unspecified, false) { s, ld -> alignment.align(IntSize.Zero, IntSize(s, 0), ld).x }
    }

    @Immutable
    internal data class SpacedAligned(
        val space: Dp,
        val rtlMirror: Boolean,
        val alignment: ((Int, LayoutDirection) -> Int)?,
    ) : HorizontalOrVertical {
        override val spacing = space

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray,
        ) {
            if (sizes.isEmpty()) return
            val spacePx = if (space == Dp.Unspecified) 0 else (space.value * this.density).roundToInt()
            var occupied = 0
            var lastSpace = 0
            val reversed = rtlMirror && layoutDirection == LayoutDirection.Rtl
            sizes.forEachIndexed(reversed) { index, it ->
                outPositions[index] = min(occupied, totalSize - it)
                lastSpace = min(spacePx, totalSize - outPositions[index] - it)
                occupied = outPositions[index] + it + lastSpace
            }
            occupied -= lastSpace
            if (alignment != null && occupied < totalSize) {
                val gp = alignment.invoke(totalSize - occupied, layoutDirection)
                for (i in outPositions.indices) outPositions[i] += gp
            }
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray,
        ) =
            arrange(totalSize, sizes, LayoutDirection.Ltr, outPositions)

        override fun toString() = "${if (rtlMirror) "" else "Absolute"}Arrangement#spacedAligned($space, $alignment)"
    }

    internal fun placeRightOrBottom(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        rev: Boolean,
    ) {
        val consumed = size.fold(0) { a, b -> a + b }
        var cur = totalSize - consumed
        size.forEachIndexed(rev) { i, it ->
            outPosition[i] = cur
            cur += it
        }
    }

    internal fun placeLeftOrTop(
        size: IntArray,
        outPosition: IntArray,
        rev: Boolean,
    ) {
        var cur = 0
        size.forEachIndexed(rev) { i, it ->
            outPosition[i] = cur
            cur += it
        }
    }

    internal fun placeCenter(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        rev: Boolean,
    ) {
        val consumed = size.fold(0) { a, b -> a + b }
        var cur = (totalSize - consumed).toFloat() / 2
        size.forEachIndexed(rev) { i, it ->
            outPosition[i] = cur.roundToInt()
            cur += it.toFloat()
        }
    }

    internal fun placeSpaceEvenly(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        rev: Boolean,
    ) {
        val consumed = size.fold(0) { a, b -> a + b }
        val gap = (totalSize - consumed).toFloat() / (size.size + 1)
        var cur = gap
        size.forEachIndexed(rev) { i, it ->
            outPosition[i] = cur.roundToInt()
            cur += it.toFloat() + gap
        }
    }

    internal fun placeSpaceBetween(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        rev: Boolean,
    ) {
        if (size.isEmpty()) return
        val consumed = size.fold(0) { a, b -> a + b }
        val gap = (totalSize - consumed).toFloat() / maxOf(size.lastIndex, 1)
        var cur = if (rev && size.size == 1) gap else 0f
        size.forEachIndexed(rev) { i, it ->
            outPosition[i] = cur.roundToInt()
            cur += it.toFloat() + gap
        }
    }

    internal fun placeSpaceAround(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        rev: Boolean,
    ) {
        val consumed = size.fold(0) { a, b -> a + b }
        val gap = if (size.isNotEmpty()) (totalSize - consumed).toFloat() / size.size else 0f
        var cur = gap / 2
        size.forEachIndexed(rev) { i, it ->
            outPosition[i] = cur.roundToInt()
            cur += it.toFloat() + gap
        }
    }

    private inline fun IntArray.forEachIndexed(
        reversed: Boolean,
        action: (Int, Int) -> Unit,
    ) {
        if (!reversed) forEachIndexed(action) else for (i in (size - 1) downTo 0) action(i, get(i))
    }
}
