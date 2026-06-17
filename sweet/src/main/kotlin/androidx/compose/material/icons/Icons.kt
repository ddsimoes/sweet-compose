package androidx.compose.material.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Material Design icons collection.
 *
 * Each icon is defined as an [ImageVector] with simple SVG path data.
 * Icons are sized at 24x24dp with a 24x24 viewport.
 */
object Icons {
    /** Default (filled) style icons. */
    object Default {
        /** Plus icon. */
        val Add: ImageVector =
            ImageVector.Builder("Add", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Left-pointing arrow icon. */
        val ArrowBack: ImageVector =
            ImageVector.Builder("ArrowBack", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Checkmark icon. */
        val Check: ImageVector =
            ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** X / close icon. */
        val Close: ImageVector =
            ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Hamburger menu icon. */
        val Menu: ImageVector =
            ImageVector.Builder("Menu", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Settings / gear icon. */
        val Settings: ImageVector =
            ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Pencil / edit icon. */
        val Edit: ImageVector =
            ImageVector.Builder("Edit", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c0.39-0.39 0.39-1.02 0-1.41l-2.34-2.34c-0.39-0.39-1.02-0.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Magnifying glass / search icon. */
        val Search: ImageVector =
            ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M15.5 14h-0.79l-0.28-0.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-0.59 4.23-1.57l0.27 0.28v0.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Vertical dots / more icon. */
        val MoreVert: ImageVector =
            ImageVector.Builder("MoreVert", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M12 8c1.1 0 2-0.9 2-2s-0.9-2-2-2-2 0.9-2 2 0.9 2 2 2zm0 2c-1.1 0-2 0.9-2 2s0.9 2 2 2 2-0.9 2-2-0.9-2-2-2zm0 6c-1.1 0-2 0.9-2 2s0.9 2 2 2 2-0.9 2-2-0.9-2-2-2z",
                        fill = Color.Black,
                    )
                }
            }.build()

        /** Circular arrow / refresh icon. */
        val Refresh: ImageVector =
            ImageVector.Builder("Refresh", 24.dp, 24.dp, 24f, 24f).apply {
                addGroup(name = "") {
                    path(
                        pathData = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-0.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14 0.69 4.22 1.78L13 11h7V4l-2.35 2.35z",
                        fill = Color.Black,
                    )
                }
            }.build()
    }

    /** Filled style — delegates to [Default] for now. */
    object Filled {
        val Add: ImageVector get() = Default.Add
        val ArrowBack: ImageVector get() = Default.ArrowBack
        val Check: ImageVector get() = Default.Check
        val Close: ImageVector get() = Default.Close
        val Menu: ImageVector get() = Default.Menu
        val Settings: ImageVector get() = Default.Settings
        val Edit: ImageVector get() = Default.Edit
        val Search: ImageVector get() = Default.Search
        val MoreVert: ImageVector get() = Default.MoreVert
        val Refresh: ImageVector get() = Default.Refresh
    }

    /** Outlined style — delegates to [Default] for now. */
    object Outlined {
        val Add: ImageVector get() = Default.Add
        val ArrowBack: ImageVector get() = Default.ArrowBack
        val Check: ImageVector get() = Default.Check
        val Close: ImageVector get() = Default.Close
        val Menu: ImageVector get() = Default.Menu
        val Settings: ImageVector get() = Default.Settings
        val Edit: ImageVector get() = Default.Edit
        val Search: ImageVector get() = Default.Search
        val MoreVert: ImageVector get() = Default.MoreVert
        val Refresh: ImageVector get() = Default.Refresh
    }
}
