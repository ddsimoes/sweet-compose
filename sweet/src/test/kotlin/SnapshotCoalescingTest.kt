import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the AtomicBoolean CAS coalescing pattern used by SWTSnapshotManager.
 * The real observer uses `if (scheduled.compareAndSet(false, true)) { launch { … } }`.
 */
class SnapshotCoalescingTest {
    @Test
    fun testAtomicBooleanCoalescing() {
        val scheduled = AtomicBoolean(false)
        val launchCount = AtomicInteger(0)
        val writeCount = 1000

        // Deferred launch: N writes try CAS, only 1 wins until drained.
        repeat(writeCount) {
            scheduled.compareAndSet(false, true) // only first write wins
        }
        // Single drain of the queued launch
        if (scheduled.get()) {
            launchCount.incrementAndGet()
            scheduled.set(false)
        }
        assertEquals(
            1,
            launchCount.get(),
            "With deferred launch, N writes coalesce into 1 launch",
        )

        // Verify no further CAS wins after drain
        val extraWins = scheduled.compareAndSet(false, true)
        assertEquals(true, extraWins, "After drain, next write should win CAS")
    }
}
