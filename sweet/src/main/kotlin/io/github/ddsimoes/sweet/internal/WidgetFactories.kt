package io.github.ddsimoes.sweet.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import io.github.ddsimoes.sweet.data.updateSweetCompositionData
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Composite

// Simple factory functions that use the composition-scoped applier
internal fun createText(applier: SWTNodeApplier): org.eclipse.swt.widgets.Label {
    val parent = applier.requireCurrentParent("Text")
    return org.eclipse.swt.widgets
        .Label(parent, SWT.NONE)
}

internal fun createButton(applier: SWTNodeApplier): org.eclipse.swt.widgets.Button {
    val parent = applier.requireCurrentParent("Button")
    return org.eclipse.swt.widgets
        .Button(parent, SWT.PUSH)
}

internal fun createTextField(
    applier: SWTNodeApplier,
    extraStyle: Int = 0,
): org.eclipse.swt.widgets.Text {
    val parent = applier.requireCurrentParent("TextField")
    return org.eclipse.swt.widgets
        .Text(parent, SWT.BORDER or extraStyle)
}

internal fun createCheckbox(applier: SWTNodeApplier): org.eclipse.swt.widgets.Button {
    val parent = applier.requireCurrentParent("Checkbox")
    return object : org.eclipse.swt.widgets.Button(parent, SWT.CHECK) {
        override fun checkSubclass() {
        }

        override fun computeSize(
            wHint: Int,
            hHint: Int,
            changed: Boolean,
        ): Point? {
            if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
                // SWT-native checkbox sizing. Deterministic under pinned measurement font (doc 01 T3).
                // Do not change without re-running the flake harness.
                return Point(24, 16)
            }

            return super.computeSize(wHint, hHint, changed)
        }
    }
}

internal fun createToggleButton(applier: SWTNodeApplier): org.eclipse.swt.widgets.Button {
    val parent = applier.requireCurrentParent("Switch")
    return object : org.eclipse.swt.widgets.Button(parent, SWT.TOGGLE) {
        override fun checkSubclass() {
        }

        override fun computeSize(
            wHint: Int,
            hHint: Int,
            changed: Boolean,
        ): Point? {
            if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
                // SWT TOGGLE button sizing — visually a button, not a switch toggle.
                // Tier-2 custom-drawn switch planned (see doc 41 C3).
                return Point(36, 20)
            }

            return super.computeSize(wHint, hHint, changed)
        }
    }
}

internal fun createRadioButton(applier: SWTNodeApplier): org.eclipse.swt.widgets.Button {
    val parent = applier.requireCurrentParent("RadioButton")
    return org.eclipse.swt.widgets
        .Button(parent, SWT.RADIO)
}

internal fun createSpacer(applier: SWTNodeApplier): org.eclipse.swt.widgets.Label {
    val parent = applier.requireCurrentParent("Spacer")
    return org.eclipse.swt.widgets
        .Label(parent, SWT.NONE)
        .apply { text = "" }
}

internal fun createSpinner(applier: SWTNodeApplier): org.eclipse.swt.widgets.Spinner {
    val parent = applier.requireCurrentParent("Spinner")
    return org.eclipse.swt.widgets
        .Spinner(parent, SWT.BORDER)
}

internal fun createCard(
    applier: SWTNodeApplier,
    contentAlignment: Alignment = Alignment.TopStart,
): Composite {
    val parent = applier.requireCurrentParent("Card")
    return Composite(parent, SWT.BORDER).apply {
        // Use SweetLayout and a Box spec that fills children to mimic FillLayout semantics
        layout = SweetLayout()
        updateSweetCompositionData {
            withLayoutSpec(
                io.github.ddsimoes.sweet.layout.LayoutSpec.Box(
                    contentAlignment = contentAlignment,
                    fillChildren = true,
                ),
            )
        }
    }
}

internal fun createDivider(applier: SWTNodeApplier): org.eclipse.swt.widgets.Label {
    val parent = applier.requireCurrentParent("HorizontalDivider")
    return org.eclipse.swt.widgets.Label(parent, SWT.SEPARATOR or SWT.HORIZONTAL).apply {
        text = ""
    }
}

// New Sweet Layout factory functions
internal fun createSweetColumn(
    applier: SWTNodeApplier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
): Composite {
    val parent = applier.requireCurrentParent("Column")
    return Composite(parent, SWT.NONE).apply {
        layout = SweetLayout()
        updateSweetCompositionData {
            withLayoutSpec(
                io.github.ddsimoes.sweet.layout.LayoutSpec
                    .Column(verticalArrangement, horizontalAlignment),
            )
        }
    }
}

internal fun createSweetRow(
    applier: SWTNodeApplier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
): Composite {
    val parent = applier.requireCurrentParent("Row")
    return Composite(parent, SWT.NONE).apply {
        layout = SweetLayout()
        updateSweetCompositionData {
            withLayoutSpec(
                io.github.ddsimoes.sweet.layout.LayoutSpec
                    .Row(horizontalArrangement, verticalAlignment),
            )
        }
    }
}

internal fun createSweetBox(
    applier: SWTNodeApplier,
    contentAlignment: Alignment = Alignment.TopStart,
    fillChildren: Boolean = false,
): Composite {
    val parent = applier.requireCurrentParent("Box")
    return Composite(parent, SWT.NONE).apply {
        layout = SweetLayout()
        updateSweetCompositionData {
            withLayoutSpec(
                io.github.ddsimoes.sweet.layout.LayoutSpec
                    .Box(contentAlignment, fillChildren),
            )
        }
    }
}
