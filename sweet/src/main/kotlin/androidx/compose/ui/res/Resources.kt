package androidx.compose.ui.res

import java.io.InputStream

/**
 * Open [InputStream] from a resource stored in resources for the application, calls the [block]
 * callback giving it a InputStream and closes stream once the processing is
 * complete.
 *
 * @param resourcePath  path of resource
 * @return object that was returned by [block]
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
inline fun <T> useResource(
    resourcePath: String,
    block: (InputStream) -> T,
): T = openResource(resourcePath).use(block)

/**
 * Open [InputStream] from a resource stored in resources for the application.
 *
 * @param resourcePath  path of resource
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@PublishedApi
internal fun openResource(resourcePath: String): InputStream = ClassLoaderResourceLoader.load(resourcePath)

/**
 * Resource loader based on JVM current context class loader.
 */
private object ClassLoaderResourceLoader {
    fun load(resourcePath: String): InputStream {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val resource =
            contextClassLoader?.getResourceAsStream(resourcePath)
                ?: this::class.java.getResourceAsStream(resourcePath)
        return requireNotNull(resource) { "Resource $resourcePath not found" }
    }
}
