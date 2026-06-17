import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.AnimationDemoApp
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class AnimationDemoLayoutTest {
    // The animated blue box is the deepest childless Composite.
    private fun Composite.deepestLeaf(): Composite? {
        val all = mutableListOf<Composite>()
        fun walk(c: Composite) { c.children.forEach { if (it is Composite) { all.add(it); walk(it) } } }
        walk(this)
        return all.lastOrNull { it.children.isEmpty() }
    }

    @Test
    fun `real AnimationDemoApp box animates`() {
        autoSWT {
            testShell(width = 500, height = 400) {
                val c = Composite(this, SWT.NONE)
                c.layout = FillLayout()
                c.embedCompose { AnimationDemoApp() }
            }.test { shell ->
                val xs = mutableListOf<Int>()
                repeat(8) {
                    Thread.sleep(120)
                    xs.add(runOnSWT { (shell.children.first() as Composite).deepestLeaf()?.bounds?.x ?: -1 })
                }
                println("[ANIM] blue box x over time = $xs")
                assertTrue(xs.toSet().size > 1, "AnimationDemo box should move over time, was $xs")
            }
        }
    }
}
