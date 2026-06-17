import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertNotNull

class CardLayoutTest {
    @Test
    fun card_fillMaxWidth_respects_parent_padding_and_contains_content() {
        autoSWT {
            val shell =
                testShell(width = 480, height = 320) {
                    val root = Composite(this, SWT.NONE)
                    root.layout = FillLayout()
                    root.embedCompose {
                        Column(Modifier.fillMaxSize().padding(10.dp).background(Color.Gray)) {
                            Card(Modifier.fillMaxWidth().background(Color.Cyan).padding(4.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Inside Card", modifier = Modifier.background(Color.White))
                                }
                            }
                        }
                    }
                }

            shell.test { sh ->
                val container: Composite = runOnSWT { sh.children.first() as Composite }
                val composeRoot: Composite = runOnSWT { container.children.first() as Composite }

                // Find the Card composite (BORDER style)
                val card: Composite =
                    runOnSWT {
                        composeRoot.children
                            .filterIsInstance<Composite>()
                            .first { (it.style and SWT.BORDER) != 0 }
                    }

                // Sanity: card exists and has a child label
                val innerLabel = sh.find<Label> { it.text == "Inside Card" }
                assertNotNull(innerLabel, "Card content label should exist")

                // Layout assertions: card is within the compose root and fills width minus padding
                val cardInRoot = card.visibleBounds(composeRoot)
                val rootWidth = runOnSWT { composeRoot.size.x }

                // Card should be fully within compose root bounds
                card.assertLayout(composeRoot).isWithin(composeRoot)

                // With symmetric Column padding, left inset equals right inset
                val leftInset = cardInRoot.x
                val rightInset = rootWidth - cardInRoot.width - cardInRoot.x

                // Allow small tolerance due to DPI rounding
                fun approxEq(
                    a: Int,
                    b: Int,
                    tol: Int = 2,
                ) = kotlin.math.abs(a - b) <= tol
                assert(approxEq(leftInset, rightInset)) {
                    "Card horizontal insets should be symmetric: left=$leftInset right=$rightInset rootWidth=$rootWidth card=$cardInRoot"
                }

                // Content must be inside the card
                innerLabel.assertLayout(composeRoot).isWithin(card)

                // Save artifacts for debugging if needed
                sh.saveSVG()
                sh.saveScreenshot()
            }
        }
    }

    @Test
    fun card_wraps_content_by_default_and_does_not_fill_without_modifier() {
        autoSWT {
            val shell =
                testShell(width = 600, height = 300) {
                    val root = Composite(this, SWT.NONE)
                    root.layout = FillLayout()
                    root.embedCompose {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Card { Text("Card Content") }
                        }
                    }
                }

            shell.test { sh ->
                val container: Composite = runOnSWT { sh.children.first() as Composite }
                val composeRoot: Composite = runOnSWT { container.children.first() as Composite }
                val card: Composite =
                    runOnSWT {
                        composeRoot.children
                            .filterIsInstance<Composite>()
                            .first { (it.style and SWT.BORDER) != 0 }
                    }

                // Card should be visible and within the compose root
                card.assertLayout(container).isVisible().isWithin(composeRoot)

                // Card should not span the entire width since no fillMaxWidth was applied
                val composeVB = composeRoot.visibleBounds(container)
                val cardVB = card.visibleBounds(container)
                assert(cardVB.width < composeVB.width) { "Card without fillMaxWidth should not fill parent width" }

                // Its child should be within the card
                val label = sh.find<Label> { it.text == "Card Content" }
                assertNotNull(label)
                label.assertLayout(container).isWithin(card)
            }
        }
    }
}
