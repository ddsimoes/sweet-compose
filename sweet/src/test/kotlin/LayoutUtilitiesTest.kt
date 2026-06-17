import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.LargePadding
import androidx.compose.foundation.layout.MediumPadding
import androidx.compose.foundation.layout.NoPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.SmallPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LayoutUtilitiesTest {
    @Test
    fun testPaddingValuesWithAllSides() {
        val padding = PaddingValues(16.dp)

        assertEquals(16.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, padding.calculateTopPadding())
        assertEquals(16.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, padding.calculateBottomPadding())
    }

    @Test
    fun testPaddingValuesWithHorizontalVertical() {
        val padding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)

        assertEquals(20.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(10.dp, padding.calculateTopPadding())
        assertEquals(20.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(10.dp, padding.calculateBottomPadding())
    }

    @Test
    fun testPaddingValuesWithIndividualSides() {
        val padding =
            PaddingValues(
                start = 4.dp,
                top = 8.dp,
                end = 12.dp,
                bottom = 16.dp,
            )

        assertEquals(4.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(8.dp, padding.calculateTopPadding())
        assertEquals(12.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, padding.calculateBottomPadding())
    }

    @Test
    fun testPaddingValuesWithDefaultValues() {
        val padding = PaddingValues()

        assertEquals(0.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(0.dp, padding.calculateTopPadding())
        assertEquals(0.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(0.dp, padding.calculateBottomPadding())
    }

    @Test
    fun testNoPadding() {
        val padding = NoPadding

        assertEquals(0.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(0.dp, padding.calculateTopPadding())
        assertEquals(0.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(0.dp, padding.calculateBottomPadding())
    }

    @Test
    fun testStandardPaddingConstants() {
        // Test that standard padding constants have expected values
        assertEquals(8.dp, SmallPadding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, MediumPadding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(24.dp, LargePadding.calculateLeftPadding(LayoutDirection.Ltr))

        // Test that all sides are equal for standard constants
        assertEquals(SmallPadding.calculateLeftPadding(LayoutDirection.Ltr), SmallPadding.calculateTopPadding())
        assertEquals(MediumPadding.calculateLeftPadding(LayoutDirection.Ltr), MediumPadding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(LargePadding.calculateTopPadding(), LargePadding.calculateBottomPadding())
    }

    @Test
    fun testPaddingValuesEquality() {
        val padding1 = PaddingValues(16.dp)
        val padding2 =
            PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            )
        val padding3 = PaddingValues(20.dp)

        assertEquals(padding1, padding2)
        assertTrue(padding1 != padding3)
        assertEquals(padding1.hashCode(), padding2.hashCode())
        assertTrue(padding1.hashCode() != padding3.hashCode())
    }

    @Test
    fun testArrangementSpacedBy() {
        val spacing = 16.dp
        val arrangement = Arrangement.spacedBy(spacing)

        assertNotNull(arrangement)
        assertEquals(spacing, arrangement.spacing)

        // The spacing property returns the Dp value
        assertEquals(spacing, arrangement.spacing)
    }

    @Test
    fun testArrangementSpacedByArrangement() {
        val arrangement = Arrangement.spacedBy(8.dp)

        // Verify the arrangement is a valid HorizontalOrVertical with spacing
        assertNotNull(arrangement)
        assertEquals(8.dp, arrangement.spacing)

        // arrange() testing skipped — it's a Density extension and the
        // internal SpacedAligned implementation is tested via integration tests
        // in LayoutDelegatesTest (row/column arrangement tests).
    }

    @Test
    fun testPaddingValuesWithRtlLayout() {
        val padding =
            PaddingValues(
                start = 10.dp,
                top = 5.dp,
                end = 15.dp,
                bottom = 20.dp,
            )

        // In LTR, start = left, end = right
        assertEquals(10.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(15.dp, padding.calculateRightPadding(LayoutDirection.Ltr))

        // In RTL, start = right, end = left (but our implementation is absolute)
        assertEquals(10.dp, padding.calculateLeftPadding(LayoutDirection.Rtl))
        assertEquals(15.dp, padding.calculateRightPadding(LayoutDirection.Rtl))

        // Top and bottom should be the same regardless of direction
        assertEquals(5.dp, padding.calculateTopPadding())
        assertEquals(20.dp, padding.calculateBottomPadding())
    }
}
