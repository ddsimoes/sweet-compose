@file:Suppress("ktlint:standard:function-naming")

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.input.key.KeyShortcut
import io.github.ddsimoes.sweet.internal.setupMenuBar

/**
 * MenuBar scope that provides access to top-level menu creation
 */
@Stable
class MenuBarScope internal constructor() {
    internal val menus = mutableListOf<MenuData>()

    @Composable
    fun Menu(
        text: String,
        mnemonic: Char? = null,
        enabled: Boolean = true,
        content: @Composable MenuScope.() -> Unit,
    ) {
        val menuScope = MenuScope()
        menuScope.content()

        menus.add(
            MenuData(
                text = text,
                mnemonic = mnemonic,
                enabled = enabled,
                items = menuScope.items.toList(),
            ),
        )
    }
}

/**
 * MenuScope for creating menu items within a menu
 */
@Stable
class MenuScope internal constructor() {
    internal val items = mutableListOf<MenuItemData>()

    @Composable
    fun Menu(
        text: String,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        content: @Composable MenuScope.() -> Unit,
    ) {
        val subMenuScope = MenuScope()
        subMenuScope.content()

        items.add(
            MenuItemData.SubMenu(
                text = text,
                enabled = enabled,
                mnemonic = mnemonic,
                items = subMenuScope.items.toList(),
            ),
        )
    }

    @Composable
    fun Item(
        text: String,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        shortcut: KeyShortcut? = null,
        onClick: () -> Unit,
    ) {
        items.add(
            MenuItemData.Regular(
                text = text,
                icon = icon,
                enabled = enabled,
                mnemonic = mnemonic,
                shortcut = shortcut,
                onClick = onClick,
            ),
        )
    }

    @Composable
    fun CheckboxItem(
        text: String,
        checked: Boolean,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        shortcut: KeyShortcut? = null,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        items.add(
            MenuItemData.Checkbox(
                text = text,
                checked = checked,
                icon = icon,
                enabled = enabled,
                mnemonic = mnemonic,
                shortcut = shortcut,
                onCheckedChange = onCheckedChange,
            ),
        )
    }

    @Composable
    fun RadioButtonItem(
        text: String,
        selected: Boolean,
        icon: Painter? = null,
        enabled: Boolean = true,
        mnemonic: Char? = null,
        shortcut: KeyShortcut? = null,
        onClick: () -> Unit,
    ) {
        items.add(
            MenuItemData.RadioButton(
                text = text,
                selected = selected,
                icon = icon,
                enabled = enabled,
                mnemonic = mnemonic,
                shortcut = shortcut,
                onClick = onClick,
            ),
        )
    }

    @Composable
    fun Separator() {
        items.add(MenuItemData.Separator)
    }
}

/**
 * Internal data structures for menu representation
 */
internal data class MenuData(
    val text: String,
    val mnemonic: Char?,
    val enabled: Boolean,
    val items: List<MenuItemData>,
)

internal sealed class MenuItemData {
    data class Regular(
        val text: String,
        val icon: Painter?,
        val enabled: Boolean,
        val mnemonic: Char?,
        val shortcut: KeyShortcut?,
        val onClick: () -> Unit,
    ) : MenuItemData()

    data class Checkbox(
        val text: String,
        val checked: Boolean,
        val icon: Painter?,
        val enabled: Boolean,
        val mnemonic: Char?,
        val shortcut: KeyShortcut?,
        val onCheckedChange: (Boolean) -> Unit,
    ) : MenuItemData()

    data class RadioButton(
        val text: String,
        val selected: Boolean,
        val icon: Painter?,
        val enabled: Boolean,
        val mnemonic: Char?,
        val shortcut: KeyShortcut?,
        val onClick: () -> Unit,
    ) : MenuItemData()

    data class SubMenu(
        val text: String,
        val enabled: Boolean,
        val mnemonic: Char?,
        val items: List<MenuItemData>,
    ) : MenuItemData()

    data object Separator : MenuItemData()
}

/**
 * Window-level MenuBar composable that integrates with SWT
 */
@Composable
fun FrameWindowScope.MenuBar(content: @Composable MenuBarScope.() -> Unit) {
    val menuBarScope = MenuBarScope()
    menuBarScope.content()

    SideEffect {
        if (!window.isDisposed) {
            setupMenuBar(window, menuBarScope.menus)
        }
    }
}
