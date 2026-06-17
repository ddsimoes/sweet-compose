@file:Suppress("ktlint:standard:filename", "MatchingDeclarationName", "UnusedParameter")

package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable

/**
 * DSL for creating lazy list content
 */
class LazyListScope {
    internal val items = mutableListOf<@Composable () -> Unit>()

    /**
     * Add a list of items to the lazy list
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable (index: Int) -> Unit,
    ) {
        repeat(count) { index ->
            items.add { itemContent(index) }
        }
    }

    /**
     * Add a list of items to the lazy list
     */
    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        contentType: (item: T) -> Any? = { null },
        itemContent: @Composable (item: T) -> Unit,
    ) {
        items.forEach { item ->
            this.items.add { itemContent(item) }
        }
    }

    /**
     * Add a single item to the lazy list
     */
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable () -> Unit,
    ) {
        items.add(content)
    }

    internal fun build(): List<@Composable () -> Unit> = items.toList()
}

/**
 * Helper function for creating lazy list items from arrays
 */
fun <T> LazyListScope.items(
    items: Array<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable (item: T) -> Unit,
) {
    items(items.toList(), key, contentType, itemContent)
}

/**
 * Helper function for creating indexed lazy list items
 */
fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    items.forEachIndexed { index, item ->
        this.items.add { itemContent(index, item) }
    }
}
