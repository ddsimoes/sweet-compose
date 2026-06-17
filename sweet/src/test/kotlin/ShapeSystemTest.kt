import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.createRoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShapeSystemTest {
    @Test
    fun testCircleShape() {
        val shape = CircleShape
        assertNotNull(shape)
        assertEquals("CircleShape", shape.toString())
    }

    @Test
    fun testRectangleShape() {
        val shape = RectangleShape
        assertNotNull(shape)
        assertEquals("RectangleShape", shape.toString())

        // RectangleShape is now its own object, not a RoundedCornerShape
        assertTrue(shape === RectangleShape)
    }

    @Test
    fun testRoundedCornerShapeWithSingleRadius() {
        val radius = 8.dp
        val shape = RoundedCornerShape(radius)

        assertEquals(radius, shape.topStartCorner)
        assertEquals(radius, shape.topEndCorner)
        assertEquals(radius, shape.bottomEndCorner)
        assertEquals(radius, shape.bottomStartCorner)
    }

    @Test
    fun testRoundedCornerShapeWithDifferentCorners() {
        val topStart = 4.dp
        val topEnd = 8.dp
        val bottomEnd = 12.dp
        val bottomStart = 16.dp

        val shape =
            createRoundedCornerShape(
                topStart = topStart,
                topEnd = topEnd,
                bottomEnd = bottomEnd,
                bottomStart = bottomStart,
            )

        assertEquals(topStart, shape.topStartCorner)
        assertEquals(topEnd, shape.topEndCorner)
        assertEquals(bottomEnd, shape.bottomEndCorner)
        assertEquals(bottomStart, shape.bottomStartCorner)
    }

    @Test
    fun testRoundedCornerShapeWithHorizontalVertical() {
        val horizontal = 10.dp
        val vertical = 5.dp

        val shape = RoundedCornerShape(horizontal, vertical)

        assertEquals(horizontal, shape.topStartCorner)
        assertEquals(vertical, shape.topEndCorner)
        assertEquals(horizontal, shape.bottomEndCorner)
        assertEquals(vertical, shape.bottomStartCorner)
    }

    @Test
    fun testRoundedCornerShapeEquality() {
        val shape1 = RoundedCornerShape(8.dp)
        val shape2 = RoundedCornerShape(8.dp)
        val shape3 = RoundedCornerShape(12.dp)

        assertEquals(shape1, shape2)
        assertNotEquals(shape1, shape3)
        assertEquals(shape1.hashCode(), shape2.hashCode())
        assertNotEquals(shape1.hashCode(), shape3.hashCode())
    }

    @Test
    fun testRoundedCornerShapeEqualityWithDifferentCorners() {
        val shape1 =
            createRoundedCornerShape(
                topStart = 4.dp,
                topEnd = 8.dp,
                bottomEnd = 12.dp,
                bottomStart = 16.dp,
            )
        val shape2 =
            createRoundedCornerShape(
                topStart = 4.dp,
                topEnd = 8.dp,
                bottomEnd = 12.dp,
                bottomStart = 16.dp,
            )
        val shape3 =
            createRoundedCornerShape(
                topStart = 4.dp,
                topEnd = 8.dp,
                bottomEnd = 12.dp,
                // Different bottom start
                bottomStart = 20.dp,
            )

        assertEquals(shape1, shape2)
        assertNotEquals(shape1, shape3)
        assertEquals(shape1.hashCode(), shape2.hashCode())
        assertNotEquals(shape1.hashCode(), shape3.hashCode())
    }

    @Test
    fun testRoundedCornerShapeToString() {
        val shape =
            createRoundedCornerShape(
                topStart = 4.dp,
                topEnd = 8.dp,
                bottomEnd = 12.dp,
                bottomStart = 16.dp,
            )

        val expectedString = "RoundedCornerShape(topStart=4.0dp, topEnd=8.0dp, bottomEnd=12.0dp, bottomStart=16.0dp)"
        assertEquals(expectedString, shape.toString())
    }

    @Test
    fun testRoundedCornerShapeWithZeroRadius() {
        val shape = RoundedCornerShape(0.dp)

        assertEquals(0.dp, shape.topStartCorner)
        assertEquals(0.dp, shape.topEndCorner)
        assertEquals(0.dp, shape.bottomEndCorner)
        assertEquals(0.dp, shape.bottomStartCorner)

        // Zero radius should behave like rectangle
        assertTrue(shape.topStartCorner == 0.dp)
    }

    @Test
    fun testShapeFactoryMethods() {
        // Test RoundedCornerShape factory with single corner
        val shape1 = RoundedCornerShape(corner = 8.dp)
        assertEquals(8.dp, shape1.topStartCorner)
        assertEquals(8.dp, shape1.topEndCorner)
        assertEquals(8.dp, shape1.bottomEndCorner)
        assertEquals(8.dp, shape1.bottomStartCorner)

        // Test RoundedCornerShape factory with horizontal/vertical
        val shape2 = RoundedCornerShape(horizontal = 10.dp, vertical = 5.dp)
        assertEquals(10.dp, shape2.topStartCorner)
        assertEquals(5.dp, shape2.topEndCorner)
        assertEquals(10.dp, shape2.bottomEndCorner)
        assertEquals(5.dp, shape2.bottomStartCorner)
    }
}
