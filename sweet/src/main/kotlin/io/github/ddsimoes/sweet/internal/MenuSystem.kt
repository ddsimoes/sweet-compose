@file:Suppress("ktlint:standard:filename", "UnusedParameter")

package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Shell

private const val MENU_SIGNATURE_KEY = "sweet.menu.signature"
private const val MENU_CALLBACKS_KEY = "sweet.menu.callbacks"
private const val MENU_ITEM_PATH_KEY = "sweet.menu.item.path"

/**
 * Registers menu action callbacks in a per-shell map so they can be updated without
 * rebuilding the native menu bar. Call this from [setupMenuBar] every time, even when
 * the structural signature is unchanged — it ensures stale lambdas from a previous
 * recomposition never fire.
 */
internal fun registerMenuCallbacks(
    shell: Shell,
    menus: List<MenuData>,
) {
    @Suppress("UNCHECKED_CAST")
    val callbacks =
        shell.getData(MENU_CALLBACKS_KEY) as? MutableMap<String, Any>
            ?: mutableMapOf<String, Any>().also {
                shell.setData(MENU_CALLBACKS_KEY, it)
            }

    fun walkItem(
        path: String,
        item: androidx.compose.ui.window.MenuItemData,
    ) {
        when (item) {
            is androidx.compose.ui.window.MenuItemData.Regular -> {
                callbacks[path] = item.onClick
            }
            is androidx.compose.ui.window.MenuItemData.Checkbox -> {
                callbacks[path] = item.onCheckedChange
            }
            is androidx.compose.ui.window.MenuItemData.RadioButton -> {
                callbacks[path] = item.onClick
            }
            is androidx.compose.ui.window.MenuItemData.SubMenu -> {
                item.items.forEachIndexed { i, sub ->
                    walkItem("$path:$i", sub)
                }
            }
            is androidx.compose.ui.window.MenuItemData.Separator -> { /* no callback */ }
        }
    }

    menus.forEachIndexed { menuIdx, menu ->
        menu.items.forEachIndexed { itemIdx, item ->
            walkItem("$menuIdx:$itemIdx", item)
        }
    }
}

/**
 * Sets up SWT MenuBar for the shell.
 *
 * Because [androidx.compose.ui.window.MenuBar] applies this from an (unkeyed) effect, it can run on
 * every recomposition. Rebuilding the native menu bar each time is wasteful and disposes any menu the
 * user currently has open. To avoid that churn we compute a structural signature of [menus] (texts,
 * enabled/checked/selected state, mnemonics, shortcuts, nesting — everything except the click
 * lambdas) and skip the rebuild when nothing visible has changed.
 *
 * Callbacks are always updated via [registerMenuCallbacks] so that changes to
 * [androidx.compose.ui.window.MenuItemData.Regular.onClick], etc. take effect
 * without a full menu rebuild.
 */
internal fun setupMenuBar(
    shell: Shell,
    menus: List<MenuData>,
) {
    // Always refresh callback map so stale lambdas from a previous recomposition never fire.
    registerMenuCallbacks(shell, menus)

    val signature = menus.menuStructureSignature()
    val existing = shell.menuBar
    if (existing != null && !existing.isDisposed && shell.getData(MENU_SIGNATURE_KEY) == signature) {
        // Structure unchanged: keep the existing native menu (and any open submenu) intact.
        // Callbacks were already refreshed above.
        return
    }

    existing?.let {
        if (!it.isDisposed) {
            it.dispose()
        }
    }

    // Create the menu bar
    val menuBar =
        org.eclipse.swt.widgets
            .Menu(shell, SWT.BAR)
    shell.menuBar = menuBar

    // Create top-level menus
    menus.forEachIndexed { menuIdx, menuData ->
        createSWTMenu(menuBar, menuData, "$menuIdx")
    }

    shell.setData(MENU_SIGNATURE_KEY, signature)
}

/** Structural signature of a menu tree, excluding click lambdas (whose identity changes per recomposition). */
private fun List<MenuData>.menuStructureSignature(): String =
    joinToString(";") { menu ->
        "${menu.text}|${menu.enabled}|${menu.mnemonic}[${menu.items.menuItemsSignature()}]"
    }

private fun List<androidx.compose.ui.window.MenuItemData>.menuItemsSignature(): String =
    joinToString(",") { item ->
        when (item) {
            is androidx.compose.ui.window.MenuItemData.Regular ->
                "R:${item.text}|${item.enabled}|${item.mnemonic}|${item.shortcut}"
            is androidx.compose.ui.window.MenuItemData.Checkbox ->
                "C:${item.text}|${item.checked}|${item.enabled}|${item.mnemonic}|${item.shortcut}"
            is androidx.compose.ui.window.MenuItemData.RadioButton ->
                "B:${item.text}|${item.selected}|${item.enabled}|${item.mnemonic}|${item.shortcut}"
            is androidx.compose.ui.window.MenuItemData.SubMenu ->
                "S:${item.text}|${item.enabled}|${item.mnemonic}[${item.items.menuItemsSignature()}]"
            androidx.compose.ui.window.MenuItemData.Separator -> "-"
        }
    }

/**
 * Creates an SWT menu from MenuData.
 *
 * @param prefix the callback path prefix for this menu (e.g. `"0"` for the first top-level menu).
 */
private fun createSWTMenu(
    parent: org.eclipse.swt.widgets.Menu,
    menuData: MenuData,
    prefix: String,
) {
    // Create the menu item for the parent menu
    val menuItem =
        org.eclipse.swt.widgets
            .MenuItem(parent, SWT.CASCADE)
    menuItem.text = menuData.text
    menuItem.isEnabled = menuData.enabled

    // Set mnemonic if available
    menuData.mnemonic?.let { mnemonic ->
        val text = menuData.text
        val mnemonicIndex = text.lowercase().indexOf(mnemonic.lowercaseChar())
        if (mnemonicIndex >= 0) {
            menuItem.text = text.substring(0, mnemonicIndex) + "&" + text.substring(mnemonicIndex)
        }
    }

    // Create the dropdown menu
    val dropDownMenu =
        org.eclipse.swt.widgets
            .Menu(parent.shell, SWT.DROP_DOWN)
    menuItem.menu = dropDownMenu

    // Add menu items with path prefix
    menuData.items.forEachIndexed { itemIdx, itemData ->
        createSWTMenuItem(dropDownMenu, itemData, "$prefix:$itemIdx")
    }
}

/**
 * Helper function to get or create a coroutine scope for a shell
 */
private fun getOrCreateShellScope(shell: Shell): CoroutineScope {
    var scope = shell.getData("__menu_scope") as? CoroutineScope
    if (scope == null) {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        shell.setData("__menu_scope", scope)

        // Cancel the scope when the shell is disposed
        shell.addDisposeListener {
            scope?.cancel()
            shell.setData("__menu_scope", null)
        }
    }
    return scope
}

/**
 * Creates SWT menu items from MenuItemData.
 *
 * @param prefix the callback path for this item, used as a key into the per-shell
 *   callback map updated by [registerMenuCallbacks].
 */
private fun createSWTMenuItem(
    parent: org.eclipse.swt.widgets.Menu,
    itemData: androidx.compose.ui.window.MenuItemData,
    prefix: String,
) {
    /** Reads the current callback for [prefix] from the shell's callback map. */
    fun Shell.lookupCallback(): Any? {
        @Suppress("UNCHECKED_CAST")
        return (getData(MENU_CALLBACKS_KEY) as? Map<String, Any>)?.get(prefix)
    }

    when (itemData) {
        is androidx.compose.ui.window.MenuItemData.Regular -> {
            val menuItem =
                org.eclipse.swt.widgets
                    .MenuItem(parent, SWT.PUSH)
            menuItem.text = itemData.text
            menuItem.isEnabled = itemData.enabled

            // Set mnemonic
            itemData.mnemonic?.let { mnemonic ->
                val text = itemData.text
                val mnemonicIndex = text.lowercase().indexOf(mnemonic.lowercaseChar())
                if (mnemonicIndex >= 0) {
                    menuItem.text = text.substring(0, mnemonicIndex) + "&" + text.substring(mnemonicIndex)
                }
            }

            // Set accelerator (keyboard shortcut)
            itemData.shortcut?.let { shortcut ->
                val accelerator = convertKeyShortcutToSWT(shortcut)
                if (accelerator != 0) {
                    menuItem.accelerator = accelerator
                }
            }

            // Add selection listener — reads current callback from shell map
            val shell = parent.shell
            val scope = getOrCreateShellScope(shell)
            menuItem.addSelectionListener(
                object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent) {
                        scope.launch {
                            @Suppress("UNCHECKED_CAST")
                            (shell.lookupCallback() as? (() -> Unit))?.invoke()
                        }
                    }
                },
            )
        }

        is androidx.compose.ui.window.MenuItemData.Checkbox -> {
            val menuItem =
                org.eclipse.swt.widgets
                    .MenuItem(parent, SWT.CHECK)
            menuItem.text = itemData.text
            menuItem.isEnabled = itemData.enabled
            menuItem.selection = itemData.checked

            // Set mnemonic
            itemData.mnemonic?.let { mnemonic ->
                val text = itemData.text
                val mnemonicIndex = text.lowercase().indexOf(mnemonic.lowercaseChar())
                if (mnemonicIndex >= 0) {
                    menuItem.text = text.substring(0, mnemonicIndex) + "&" + text.substring(mnemonicIndex)
                }
            }

            // Set accelerator
            itemData.shortcut?.let { shortcut ->
                val accelerator = convertKeyShortcutToSWT(shortcut)
                if (accelerator != 0) {
                    menuItem.accelerator = accelerator
                }
            }

            // Add selection listener — reads current callback from shell map
            val shell = parent.shell
            val scope = getOrCreateShellScope(shell)
            menuItem.addSelectionListener(
                object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent) {
                        scope.launch {
                            @Suppress("UNCHECKED_CAST")
                            val cb = shell.lookupCallback()
                            if (cb is Function1<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                (cb as (Boolean) -> Unit)(menuItem.selection)
                            }
                        }
                    }
                },
            )
        }

        is androidx.compose.ui.window.MenuItemData.RadioButton -> {
            val menuItem =
                org.eclipse.swt.widgets
                    .MenuItem(parent, SWT.RADIO)
            menuItem.text = itemData.text
            menuItem.isEnabled = itemData.enabled
            menuItem.selection = itemData.selected

            // Set mnemonic
            itemData.mnemonic?.let { mnemonic ->
                val text = itemData.text
                val mnemonicIndex = text.lowercase().indexOf(mnemonic.lowercaseChar())
                if (mnemonicIndex >= 0) {
                    menuItem.text = text.substring(0, mnemonicIndex) + "&" + text.substring(mnemonicIndex)
                }
            }

            // Set accelerator
            itemData.shortcut?.let { shortcut ->
                val accelerator = convertKeyShortcutToSWT(shortcut)
                if (accelerator != 0) {
                    menuItem.accelerator = accelerator
                }
            }

            // Add selection listener — reads current callback from shell map
            val shell = parent.shell
            val scope = getOrCreateShellScope(shell)
            menuItem.addSelectionListener(
                object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent) {
                        if (menuItem.selection) {
                            scope.launch {
                                @Suppress("UNCHECKED_CAST")
                                (shell.lookupCallback() as? (() -> Unit))?.invoke()
                            }
                        }
                    }
                },
            )
        }

        is androidx.compose.ui.window.MenuItemData.SubMenu -> {
            val menuItem =
                org.eclipse.swt.widgets
                    .MenuItem(parent, SWT.CASCADE)
            menuItem.text = itemData.text
            menuItem.isEnabled = itemData.enabled

            // Set mnemonic
            itemData.mnemonic?.let { mnemonic ->
                val text = itemData.text
                val mnemonicIndex = text.lowercase().indexOf(mnemonic.lowercaseChar())
                if (mnemonicIndex >= 0) {
                    menuItem.text = text.substring(0, mnemonicIndex) + "&" + text.substring(mnemonicIndex)
                }
            }

            // Create submenu
            val subMenu =
                org.eclipse.swt.widgets
                    .Menu(parent.shell, SWT.DROP_DOWN)
            menuItem.menu = subMenu

            // Add sub menu items with path prefix
            itemData.items.forEachIndexed { subIdx, subItemData ->
                createSWTMenuItem(subMenu, subItemData, "$prefix:$subIdx")
            }
        }

        androidx.compose.ui.window.MenuItemData.Separator -> {
            org.eclipse.swt.widgets
                .MenuItem(parent, SWT.SEPARATOR)
        }
    }
}

/**
 * Converts KeyShortcut to SWT accelerator key code
 */
private fun convertKeyShortcutToSWT(shortcut: KeyShortcut): Int {
    // For now, skip keyboard shortcuts to avoid compilation issues
    // Will implement later when Key enum is properly set up
    return 0
}
