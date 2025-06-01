package io.github.ddsimoes.sweet

/**
 * Configuration for the SWT application window
 */
data class SWTWindowConfig(
    val title: String = "Compose SWT App",
    val width: Int = 800,
    val height: Int = 600,
    val resizable: Boolean = true
)
