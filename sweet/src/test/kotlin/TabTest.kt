@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Authoritative tests for Tab, TabRow, and ScrollableTabRow:
 * label rendering, click handling via doSelect(), selected styling,
 * tab-vs-button click distinction, icon + text rendering, and scrollable tabs.
 *
 * This is the single Tab test file (legacy Tab* files collapsed into here — WS-4).
 */
class TabTest {
    // ── Label rendering ────────────────────────────────────────────────

    @Test
    fun tab_renders_correct_label() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        Tab(
                            selected = false,
                            onClick = { },
                            text = { Text("Alpha") },
                        )
                        Tab(
                            selected = false,
                            onClick = { },
                            text = { Text("Beta") },
                        )
                    }
                }
            }.test { shell ->
                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertTrue(buttons.size >= 2, "Expected at least 2 tab buttons, got ${buttons.size}")

                val buttonTexts = runOnSWT { buttons.map { it.text } }
                assertTrue(buttonTexts.any { it.contains("Alpha") }, "Alpha tab text not found in $buttonTexts")
                assertTrue(buttonTexts.any { it.contains("Beta") }, "Beta tab text not found in $buttonTexts")
            }
        }
    }

    @Test
    fun tabRow_with_multiple_tabs_renders_all_labels() {
        autoSWT {
            testShell(width = 600, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TabRow(selectedTabIndex = 0) {
                        Tab(selected = true, onClick = { }, text = { Text("One") })
                        Tab(selected = false, onClick = { }, text = { Text("Two") })
                        Tab(selected = false, onClick = { }, text = { Text("Three") })
                    }
                }
            }.test { shell ->
                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertEquals(3, buttons.size, "Expected exactly 3 tab buttons, got ${buttons.size}")

                val texts = runOnSWT { buttons.map { it.text } }
                assertEquals("One", texts[0], "First tab label mismatch")
                assertEquals("Two", texts[1], "Second tab label mismatch")
                assertEquals("Three", texts[2], "Third tab label mismatch")
            }
        }
    }

    // ── Click handling / state change ──────────────────────────────────

    @Test
    fun tab_click_fires_onClick_and_updates_selected_index() {
        var clickIndex = -1

        autoSWT {
            testShell(width = 600, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    var selectedTabIndex by remember { mutableStateOf(0) }

                    Column {
                        Text("Selected: $selectedTabIndex")

                        TabRow(selectedTabIndex = selectedTabIndex) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = {
                                    clickIndex = 0
                                    selectedTabIndex = 0
                                },
                                text = { Text("Tab A") },
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    clickIndex = 1
                                    selectedTabIndex = 1
                                },
                                text = { Text("Tab B") },
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = {
                                    clickIndex = 2
                                    selectedTabIndex = 2
                                },
                                text = { Text("Tab C") },
                            )
                        }
                    }
                }
            }.test { shell ->
                assertEquals(-1, clickIndex, "No tab should be clicked initially")

                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertEquals(3, buttons.size, "Expected 3 tab buttons, got ${buttons.size}")

                // Click the second tab (index 1) and verify onClick fires + state updates
                runOnSWT { buttons[1].doSelect() }
                assertEquals(1, clickIndex, "onClick should fire for index 1, got $clickIndex")

                val selectedText =
                    runOnSWT {
                        shell.find<org.eclipse.swt.widgets.Label> { it.text.startsWith("Selected:") }.text
                    }
                assertEquals("Selected: 1", selectedText, "Selected index should update to 1 after click")

                // Click the third tab (index 2)
                runOnSWT { buttons[2].doSelect() }
                assertEquals(2, clickIndex, "onClick should fire for index 2, got $clickIndex")
            }
        }
    }

    // ── Selected styling ───────────────────────────────────────────────

    @Test
    fun selected_tab_has_different_styling_from_unselected() {
        autoSWT {
            testShell(width = 600, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TabRow(selectedTabIndex = 1) {
                        Tab(selected = false, onClick = { }, text = { Text("Unselected") })
                        Tab(selected = true, onClick = { }, text = { Text("Selected") })
                    }
                }
            }.test { shell ->
                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertEquals(2, buttons.size, "Expected 2 tab buttons")

                // Selected tab (index 1) should have non-null background/foreground
                val selBg = runOnSWT { buttons[1].background }
                val selFg = runOnSWT { buttons[1].foreground }
                assertNotNull(selBg, "Selected tab should have background set")
                assertNotNull(selFg, "Selected tab should have foreground set")

                // Unselected tab should differ from selected
                val unselBg = runOnSWT { buttons[0].background }
                val unselFg = runOnSWT { buttons[0].foreground }
                assertTrue(
                    unselBg != selBg || unselFg != selFg,
                    "Unselected tab styling should differ from selected: bg=$unselBg/$selBg, fg=$unselFg/$selFg",
                )
            }
        }
    }

    @Test
    fun tab_selection_updates_styling_after_click() {
        autoSWT {
            testShell(width = 600, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    var selectedTabIndex by remember { mutableStateOf(0) }

                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("First") },
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Second") },
                        )
                    }
                }
            }.test { shell ->
                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertEquals(2, buttons.size)

                // Initially: first tab selected (background/foreground set), second differs
                val firstBg0 = runOnSWT { buttons[0].background }
                val firstFg0 = runOnSWT { buttons[0].foreground }
                val secondBg0 = runOnSWT { buttons[1].background }
                val secondFg0 = runOnSWT { buttons[1].foreground }
                assertNotNull(firstBg0, "Initially selected tab should have background")
                assertTrue(
                    firstBg0 != secondBg0 || firstFg0 != secondFg0,
                    "Initially selected and unselected tabs should differ: bg=$firstBg0/$secondBg0, fg=$firstFg0/$secondFg0",
                )

                // Click second tab — selection switches, styling should swap
                runOnSWT { buttons[1].doSelect() }

                val firstBg1 = runOnSWT { buttons[0].background }
                val secondBg1 = runOnSWT { buttons[1].background }
                assertNotNull(secondBg1, "After click, second tab should gain background")
                assertTrue(
                    firstBg1 != secondBg1,
                    "After click, first tab should differ from second: bg=$firstBg1 vs $secondBg1",
                )
            }
        }
    }

    // ── Tab vs Button click distinction (migrated from TabClickTest) ──

    @Test
    fun tab_click_is_distinguishable_from_button_click() {
        var tabClicked = false
        var buttonClicked = false

        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        Text("Tab clicked: $tabClicked")
                        Text("Button clicked: $buttonClicked")

                        Tab(
                            selected = false,
                            onClick = { tabClicked = true },
                            text = { Text("Test Tab") },
                        )

                        Button(
                            onClick = { buttonClicked = true },
                        ) {
                            Text("Test Button")
                        }
                    }
                }
            }.test { shell ->
                assertTrue(!tabClicked, "Tab should not be clicked initially")
                assertTrue(!buttonClicked, "Button should not be clicked initially")

                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertTrue(buttons.size >= 2, "Expected at least 2 buttons (tab + button), got ${buttons.size}")

                // Tab and regular Button both create SWT Buttons — distinguish via data keys
                val tabButton = runOnSWT {
                    buttons.find { it.getData("__sweet_onClick") != null }
                }!!
                val regularButton = runOnSWT {
                    buttons.find { it.getData("onClick") != null }
                }!!

                runOnSWT { tabButton.doSelect() }
                assertTrue(tabClicked, "Tab onClick should fire after doSelect")
                assertTrue(!buttonClicked, "Button onClick should NOT fire from tab click")

                runOnSWT { regularButton.doSelect() }
                assertTrue(buttonClicked, "Button onClick should fire after doSelect")
            }
        }
    }

    // ── ScrollableTabRow (migrated from TabSystemTest) ─────────────────

    @Test
    fun scrollableTabRow_renders_all_tabs() {
        var selectedIndex = -1

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    var selection by remember { mutableStateOf(1) }

                    Column {
                        ScrollableTabRow(selectedTabIndex = selection) {
                            repeat(3) { index ->
                                Tab(
                                    selected = index == selection,
                                    onClick = {
                                        selection = index
                                        selectedIndex = index
                                    },
                                    text = { Text("Tab ${index + 1}") },
                                )
                            }
                        }
                        Column(Modifier.background(Color.LightGray)) {
                            Text("Selection: $selection")
                        }
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertEquals(3, buttons.size, "Expected 3 scrollable tab buttons, got ${buttons.size}")

                val buttonTexts = runOnSWT { buttons.map { it.text } }
                assertTrue("Tab 1" in buttonTexts, "Tab 1 text not found in $buttonTexts")
                assertTrue("Tab 2" in buttonTexts, "Tab 2 text not found in $buttonTexts")
                assertTrue("Tab 3" in buttonTexts, "Tab 3 text not found in $buttonTexts")

                // Click tab 0 and verify state updates
                assertEquals(-1, selectedIndex, "No tab should be clicked externally yet")
                runOnSWT { buttons[0].doSelect() }
                assertEquals(0, selectedIndex, "Tab 0 click should set selectedIndex=0, got $selectedIndex")

                val selectionLabel = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Label> { true }
                        .find { it.text.startsWith("Selection:") }
                }
                assertNotNull(selectionLabel, "Selection label should exist")
                assertEquals("Selection: 0", runOnSWT { selectionLabel!!.text },
                    "Selection label should update after click")
            }
        }
    }

    // ── Tab with icon + text (migrated from TabSystemTest) ─────────────

    @Test
    fun tab_with_icon_and_text_renders_both() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TabRow(selectedTabIndex = 0) {
                        Tab(
                            selected = true,
                            onClick = { },
                            text = { Text("Settings") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                )
                            },
                        )
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertTrue(buttons.isNotEmpty(), "Tab should create at least one button")

                val buttonTexts = runOnSWT { buttons.map { it.text } }
                assertTrue(
                    buttonTexts.any { it.contains("Settings") },
                    "Tab should render 'Settings' text when icon is present, got: $buttonTexts",
                )
            }
        }
    }
}
