import io.github.ddsimoes.sweet.layout.LayoutCoordinator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutCoordinatorTest {
    private lateinit var coordinator: LayoutCoordinator

    @BeforeEach
    fun setUp() {
        coordinator = LayoutCoordinator.createForTest()
    }

    @AfterEach
    fun tearDown() {
        coordinator.assertBalanced()
    }

    @Test
    fun `re-entrant beginFrame endFrame pairs flush only at outermost end`() {
        assertFalse(coordinator.isInFrame(), "Should start not in frame")

        coordinator.beginFrame()
        assertTrue(coordinator.isInFrame())

        // Inner (re-entrant) frame
        coordinator.beginFrame()
        assertTrue(coordinator.isInFrame())

        // Inner endFrame — should NOT leave frame (depth still > 0)
        coordinator.endFrame()
        assertTrue(coordinator.isInFrame(), "Still in outer frame after inner endFrame")

        // Outer endFrame — should leave frame (depth reaches 0)
        coordinator.endFrame()
        assertFalse(coordinator.isInFrame(), "Should exit frame after outermost endFrame")
    }

    @Test
    fun `endFrame without beginFrame is no-op`() {
        assertFalse(coordinator.isInFrame(), "Should start not in frame")

        coordinator.endFrame()
        assertFalse(coordinator.isInFrame(), "Should still not be in frame")
    }

    @Test
    fun `normal beginFrame endFrame cycle still works`() {
        assertFalse(coordinator.isInFrame())

        coordinator.beginFrame()
        assertTrue(coordinator.isInFrame())

        coordinator.endFrame()
        assertFalse(coordinator.isInFrame())
    }
}
