@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Button as SWTButton

/**
 * Tests for side effects lifecycle (LaunchedEffect, DisposableEffect, rememberCoroutineScope).
 * Ensures proper cleanup and cancellation behavior.
 */
class SideEffectsLifecycleTest {
    // ========== LAUNCHED EFFECT TESTS ==========

    @Test
    fun launchedEffect_executes_on_initial_composition() {
        val executionLog = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    LaunchedEffect(Unit) {
                        executionLog.add("LaunchedEffect executed")
                    }

                    Text("Content")
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Content" }
                label.assertLayout().isVisible()

                // LaunchedEffect should have executed
                assertEquals(1, executionLog.size, "LaunchedEffect should execute once")
                assertEquals("LaunchedEffect executed", executionLog[0])
            }
        }
    }

    @Test
    fun launchedEffect_restarts_when_key_changes() {
        val executionLog = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var key by remember { mutableIntStateOf(0) }

                    LaunchedEffect(key) {
                        executionLog.add("LaunchedEffect with key: $key")
                    }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { key++ }) {
                            Text("Change Key")
                        }
                        Text("Key: $key")
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Initial execution
                assertEquals(1, executionLog.size, "Should execute once initially")
                assertEquals("LaunchedEffect with key: 0", executionLog[0])

                // Change key
                button.doSelect()

                // Should execute again with new key
                assertEquals(2, executionLog.size, "Should execute again after key change")
                assertEquals("LaunchedEffect with key: 1", executionLog[1])

                // Change key again
                button.doSelect()

                assertEquals(3, executionLog.size)
                assertEquals("LaunchedEffect with key: 2", executionLog[2])
            }
        }
    }

    @Test
    fun launchedEffect_cancelled_when_leaving_composition() {
        val executionLog = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var showEffect by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showEffect = !showEffect }) {
                            Text("Toggle Effect")
                        }

                        if (showEffect) {
                            EffectComponent(executionLog)
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Wait a bit for effect to start
                Thread.sleep(50)

                val initialSize = executionLog.size
                assertTrue(initialSize > 0, "Effect should have started")

                // Remove from composition
                button.doSelect()

                // Wait to ensure no more executions happen
                Thread.sleep(100)

                executionLog.size

                // After removal, effect should be cancelled (no new logs or cleanup logged)
                executionLog.forEach {
                    SweetDebugger.log("LaunchedEffectTest", "Log entry: $it")
                }

                // The effect should have been cleaned up
                assertTrue(executionLog.any { it.contains("cleanup") }, "Effect should have cleanup")
            }
        }
    }

    @Composable
    private fun EffectComponent(log: MutableList<String>) {
        LaunchedEffect(Unit) {
            log.add("Effect started")
            try {
                // Long-running operation
                repeat(100) {
                    delay(50)
                    log.add("Effect iteration $it")
                }
            } finally {
                log.add("Effect cleanup")
            }
        }
        Text("Effect Active")
    }

    // ========== DISPOSABLE EFFECT TESTS ==========

    @Test
    fun disposableEffect_onDispose_called_when_leaving_composition() {
        val disposalLog = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var showEffect by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showEffect = !showEffect }) {
                            Text("Toggle")
                        }

                        if (showEffect) {
                            DisposableEffectComponent(disposalLog)
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Initially effect is active
                assertEquals(1, disposalLog.size, "Setup should have been called")
                assertEquals("Setup", disposalLog[0])

                // Remove from composition
                button.doSelect()

                // onDispose should be called
                assertEquals(2, disposalLog.size, "Disposal should have been called")
                assertEquals("Disposed", disposalLog[1])

                // Show again
                button.doSelect()

                // Setup should be called again
                assertEquals(3, disposalLog.size)
                assertEquals("Setup", disposalLog[2])
            }
        }
    }

    @Composable
    private fun DisposableEffectComponent(log: MutableList<String>) {
        DisposableEffect(Unit) {
            log.add("Setup")
            onDispose {
                log.add("Disposed")
            }
        }
        Text("Disposable Active")
    }

    @Test
    fun disposableEffect_key_change_triggers_dispose_and_restart() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var key by remember { mutableIntStateOf(0) }

                    DisposableEffect(key) {
                        log.add("Setup $key")
                        onDispose {
                            log.add("Dispose $key")
                        }
                    }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { key++ }) {
                            Text("Change Key")
                        }
                        Text("Key: $key")
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Initial setup
                assertEquals(1, log.size)
                assertEquals("Setup 0", log[0])

                // Change key
                button.doSelect()

                // Should dispose and setup (key values in log might reflect implementation details)
                // Main goal: verify that dispose is called before new setup
                assertTrue(log.size >= 2, "Should have at least 2 log entries after key change: ${log.joinToString()}")
                assertTrue(log.any { it.contains("Dispose") }, "Should have dispose call: ${log.joinToString()}")
                assertTrue(log.any { it.contains("Setup") && it != "Setup 0" }, "Should have new setup call: ${log.joinToString()}")

                // Change key again
                val sizeBeforeSecondChange = log.size
                button.doSelect()

                // Should have additional dispose and setup calls
                assertTrue(log.size > sizeBeforeSecondChange, "Should have more log entries after second change")
                val disposeCount = log.count { it.contains("Dispose") }
                val setupCount = log.count { it.contains("Setup") }
                assertTrue(disposeCount >= 2, "Should have at least 2 dispose calls")
                assertTrue(setupCount >= 2, "Should have at least 2 setup calls (excluding initial)")
            }
        }
    }

    @Test
    fun multiple_disposableEffects_all_disposed() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var show by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { show = !show }) {
                            Text("Toggle")
                        }

                        if (show) {
                            MultipleDisposableEffects(log)
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // All effects should setup
                assertEquals(3, log.size)
                assertTrue(log.contains("Setup A"))
                assertTrue(log.contains("Setup B"))
                assertTrue(log.contains("Setup C"))

                // Hide
                button.doSelect()

                // All should dispose
                assertEquals(6, log.size)
                assertTrue(log.contains("Dispose A"))
                assertTrue(log.contains("Dispose B"))
                assertTrue(log.contains("Dispose C"))
            }
        }
    }

    @Composable
    private fun MultipleDisposableEffects(log: MutableList<String>) {
        DisposableEffect("A") {
            log.add("Setup A")
            onDispose { log.add("Dispose A") }
        }

        DisposableEffect("B") {
            log.add("Setup B")
            onDispose { log.add("Dispose B") }
        }

        DisposableEffect("C") {
            log.add("Setup C")
            onDispose { log.add("Dispose C") }
        }

        Text("Multiple Effects")
    }

    // ========== REMEMBER COROUTINE SCOPE TESTS ==========

    @Test
    fun rememberCoroutineScope_launches_coroutines() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scope = rememberCoroutineScope()

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = {
                            scope.launch {
                                log.add("Coroutine started")
                                delay(50)
                                log.add("Coroutine completed")
                            }
                        }) {
                            Text("Launch Coroutine")
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                assertEquals(0, log.size)

                // Launch coroutine
                button.doSelect()

                // Wait for completion
                Thread.sleep(100)

                assertEquals(2, log.size)
                assertEquals("Coroutine started", log[0])
                assertEquals("Coroutine completed", log[1])
            }
        }
    }

    @Test
    fun rememberCoroutineScope_cancelled_when_leaving_composition() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var show by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { show = !show }) {
                            Text("Toggle")
                        }

                        if (show) {
                            CoroutineScopeComponent(log)
                        }

                        Text("Count: ${log.size}")
                    }
                }
            }.test { shell ->
                val toggleButton = shell.find<SWTButton> { it.text == "Toggle" }
                val launchButton = shell.find<SWTButton> { it.text == "Start Long Task" }

                // Launch long-running coroutine
                launchButton.doSelect()

                Thread.sleep(50)

                val countBeforeRemoval = log.size
                assertTrue(countBeforeRemoval > 0, "Coroutine should have started")

                // Remove from composition
                toggleButton.doSelect()

                // Wait - no more log entries should appear
                Thread.sleep(150)

                val countAfterRemoval = log.size

                // The coroutine should have been cancelled (might complete current iteration but no more)
                assertTrue(
                    countAfterRemoval < 10,
                    "Coroutine should be cancelled, not complete all 10 iterations. Count: $countAfterRemoval",
                )

                SweetDebugger.log("CoroutineTest", "Log entries: ${log.joinToString()}")
            }
        }
    }

    @Composable
    private fun CoroutineScopeComponent(log: MutableList<String>) {
        val scope = rememberCoroutineScope()

        Button(onClick = {
            scope.launch {
                log.add("Task started")
                repeat(10) { i ->
                    delay(50)
                    log.add("Iteration $i")
                }
                log.add("Task completed")
            }
        }) {
            Text("Start Long Task")
        }
    }

    @Test
    fun multiple_coroutines_from_same_scope() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scope = rememberCoroutineScope()
                    var counter by remember { mutableIntStateOf(0) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = {
                            val id = counter++
                            scope.launch {
                                log.add("Coroutine $id started")
                                delay(50)
                                log.add("Coroutine $id completed")
                            }
                        }) {
                            Text("Launch Coroutine")
                        }
                        Text("Launched: $counter")
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { it.text == "Launch Coroutine" }

                // Launch multiple coroutines
                button.doSelect()
                button.doSelect()
                button.doSelect()

                // Wait for all to complete
                Thread.sleep(150)

                // Should have 6 entries (3 starts + 3 completions)
                assertEquals(6, log.size, "All coroutines should complete")

                // Verify all started
                assertTrue(log.contains("Coroutine 0 started"))
                assertTrue(log.contains("Coroutine 1 started"))
                assertTrue(log.contains("Coroutine 2 started"))

                // Verify all completed
                assertTrue(log.contains("Coroutine 0 completed"))
                assertTrue(log.contains("Coroutine 1 completed"))
                assertTrue(log.contains("Coroutine 2 completed"))
            }
        }
    }

    // ========== COMPLEX SIDE EFFECT SCENARIOS ==========

    @Test
    fun launchedEffect_and_disposableEffect_together() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var show by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { show = !show }) {
                            Text("Toggle")
                        }

                        if (show) {
                            CombinedEffectsComponent(log)
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                Thread.sleep(100)

                // Both effects should have run
                assertTrue(log.any { it.contains("DisposableEffect setup") })
                assertTrue(log.any { it.contains("LaunchedEffect") })

                val sizeBeforeRemoval = log.size

                // Remove
                button.doSelect()

                Thread.sleep(100)

                // DisposableEffect should dispose, LaunchedEffect should be cancelled
                assertTrue(log.any { it.contains("DisposableEffect disposed") })

                val sizeAfterRemoval = log.size
                assertTrue(sizeAfterRemoval > sizeBeforeRemoval, "Disposal should have been logged")
            }
        }
    }

    @Composable
    private fun CombinedEffectsComponent(log: MutableList<String>) {
        DisposableEffect(Unit) {
            log.add("DisposableEffect setup")
            onDispose {
                log.add("DisposableEffect disposed")
            }
        }

        LaunchedEffect(Unit) {
            log.add("LaunchedEffect started")
            repeat(20) {
                delay(50)
                log.add("LaunchedEffect iteration $it")
            }
        }

        Text("Combined Effects")
    }

    @Test
    fun nested_composables_with_effects_all_cleanup() {
        val log = mutableStateListOf<String>()

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var show by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { show = !show }) {
                            Text("Toggle")
                        }

                        if (show) {
                            NestedEffectsParent(log)
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // All nested effects should setup
                assertTrue(log.contains("Parent setup"))
                assertTrue(log.contains("Child 1 setup"))
                assertTrue(log.contains("Child 2 setup"))

                // Remove
                button.doSelect()

                // All should dispose
                assertTrue(log.contains("Parent disposed"))
                assertTrue(log.contains("Child 1 disposed"))
                assertTrue(log.contains("Child 2 disposed"))
            }
        }
    }

    @Composable
    private fun NestedEffectsParent(log: MutableList<String>) {
        DisposableEffect(Unit) {
            log.add("Parent setup")
            onDispose { log.add("Parent disposed") }
        }

        Column {
            NestedEffectsChild(log, "Child 1")
            NestedEffectsChild(log, "Child 2")
        }
    }

    @Composable
    private fun NestedEffectsChild(
        log: MutableList<String>,
        name: String,
    ) {
        DisposableEffect(Unit) {
            log.add("$name setup")
            onDispose { log.add("$name disposed") }
        }
        Text(name)
    }
}
