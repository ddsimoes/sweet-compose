package io.github.ddsimoes.sweet.data

import androidx.compose.ui.Alignment
import io.github.ddsimoes.sweet.layout.LayoutSpec
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Widget

/**
 * Base sealed class for all Sweet control data types
 */
sealed class SweetControlData

/**
 * Layout-related data for Sweet controls
 */
data class SweetLayoutData(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val sizeWidth: Int? = null,
    val sizeHeight: Int? = null,
    val weight: Float? = null,
    val isRow: Boolean = false,
    val alignment: Alignment? = null,
    val boxAlignment: Alignment? = null,
    val horizontalAlignmentOverride: Alignment.Horizontal? = null,
    val verticalAlignmentOverride: Alignment.Vertical? = null,
    val matchParentSize: Boolean = false,
) : SweetControlData()

/**
 * Spacing and positioning data
 */
data class SweetSpacingData(
    val paddingStart: Int = 0,
    val paddingTop: Int = 0,
    val paddingEnd: Int = 0,
    val paddingBottom: Int = 0,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) : SweetControlData()

/**
 * Constraints data for size limitations
 */
data class SweetConstraintsData(
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null,
    val aspectRatio: Float? = null,
) : SweetControlData()

/**
 * Comprehensive composition data that combines all aspects
 */
data class SweetCompositionData(
    // How this control behaves as a CHILD within a parent
    val layoutData: SweetLayoutData = SweetLayoutData(),
    val constraintsData: SweetConstraintsData = SweetConstraintsData(),
    val spacingData: SweetSpacingData = SweetSpacingData(),
    // How this control behaves as a PARENT to its children
    val layoutSpec: LayoutSpec? = null,
    /** Ordered modifier chain for doc-12 layout. Set by applySWTModifier, read by NodeMeasurable. */
    val modifierChain: Any? = null,
) : SweetControlData() {
    /**
     * Creates a new SweetCompositionData with updated layout data
     */
    fun withLayoutData(block: SweetLayoutData.() -> SweetLayoutData): SweetCompositionData {
        return copy(layoutData = layoutData.block())
    }

    /**
     * Creates a new SweetCompositionData with updated constraints data
     */
    fun withConstraintsData(block: SweetConstraintsData.() -> SweetConstraintsData): SweetCompositionData {
        return copy(constraintsData = constraintsData.block())
    }

    /**
     * Creates a new SweetCompositionData with updated spacing data
     */
    fun withSpacingData(block: SweetSpacingData.() -> SweetSpacingData): SweetCompositionData {
        return copy(spacingData = spacingData.block())
    }

    /**
     * Creates a new SweetCompositionData with updated parent layout spec
     */
    fun withLayoutSpec(spec: LayoutSpec?): SweetCompositionData {
        return copy(layoutSpec = spec)
    }

    fun withModifierChain(chain: Any?): SweetCompositionData {
        return copy(modifierChain = chain)
    }

    /**
     * Returns a copy with all modifier-derived **child** data reset to defaults while
     * preserving the [layoutSpec] (which describes how this control behaves as a *parent*
     * and is owned by the container factory, not by modifiers).
     *
     * Modifier reconciliation derives [layoutData], [constraintsData] and [spacingData] purely
     * from the current modifier chain, so they must start from defaults on each apply. This makes
     * modifier application idempotent and removal-safe: removing a modifier fully reverts its effect.
     */
    fun resetModifierDerived(): SweetCompositionData = SweetCompositionData(layoutSpec = layoutSpec)
}

/**
 * Key used to store Sweet data in SWT controls
 */
private const val SWEET_DATA_KEY = "sweet_data"

/**
 * Extension function to set Sweet-specific data on a control
 */
fun Control.setSweetData(data: SweetControlData) {
    setData(SWEET_DATA_KEY, data)
}

/**
 * Extension function to get Sweet-specific data from a control
 */
fun Control.getSweetData(): SweetControlData? {
    return getData(SWEET_DATA_KEY) as? SweetControlData
}

/**
 * Extension function to get Sweet-specific data of a specific type
 */
inline fun <reified T : SweetControlData> Control.getSweetDataAs(): T? {
    return getSweetData() as? T
}

/**
 * Extension function to get or create SweetCompositionData
 */
fun Control.getSweetCompositionData(): SweetCompositionData {
    return getSweetDataAs<SweetCompositionData>() ?: SweetCompositionData()
}

/**
 * Extension function to update SweetCompositionData
 */
fun Control.updateSweetCompositionData(block: SweetCompositionData.() -> SweetCompositionData) {
    val currentData = getSweetCompositionData()
    val newData = currentData.block()
    setData(SWEET_DATA_KEY, newData) // Direct assignment to ensure it's stored as SweetCompositionData
}

/**
 * Extension function to set Sweet-specific data on a widget (for backward compatibility)
 */
fun Widget.setSweetData(data: SweetControlData) {
    setData(SWEET_DATA_KEY, data)
}

/**
 * Extension function to get Sweet-specific data from a widget
 */
fun Widget.getSweetData(): SweetControlData? {
    return getData(SWEET_DATA_KEY) as? SweetControlData
}

/**
 * Extension function to get Sweet-specific data of a specific type from a widget
 */
inline fun <reified T : SweetControlData> Widget.getSweetDataAs(): T? {
    return getSweetData() as? T
}

// Convenient property accessors for common use cases
val Control.sweetLayoutData: SweetLayoutData
    get() = getSweetCompositionData().layoutData

val Control.sweetConstraintsData: SweetConstraintsData
    get() = getSweetCompositionData().constraintsData

val Control.sweetSpacingData: SweetSpacingData
    get() = getSweetCompositionData().spacingData

val Control.sweetLayoutSpec: LayoutSpec?
    get() = getSweetCompositionData().layoutSpec

// Specific property accessors for frequently used values
val Control.fillMaxWidth: Boolean
    get() = sweetLayoutData.fillMaxWidth

val Control.fillMaxHeight: Boolean
    get() = sweetLayoutData.fillMaxHeight

val Control.sizeWidth: Int?
    get() = sweetLayoutData.sizeWidth

val Control.sizeHeight: Int?
    get() = sweetLayoutData.sizeHeight

val Control.weight: Float?
    get() = sweetLayoutData.weight

val Control.isRow: Boolean
    get() = sweetLayoutData.isRow

val Control.minWidth: Int?
    get() = sweetConstraintsData.minWidth

val Control.maxWidth: Int?
    get() = sweetConstraintsData.maxWidth

val Control.minHeight: Int?
    get() = sweetConstraintsData.minHeight

val Control.maxHeight: Int?
    get() = sweetConstraintsData.maxHeight

val Control.aspectRatio: Float?
    get() = sweetConstraintsData.aspectRatio

val Control.paddingStart: Int
    get() = sweetSpacingData.paddingStart

val Control.paddingTop: Int
    get() = sweetSpacingData.paddingTop

val Control.paddingEnd: Int
    get() = sweetSpacingData.paddingEnd

val Control.paddingBottom: Int
    get() = sweetSpacingData.paddingBottom

val Control.offsetX: Int
    get() = sweetSpacingData.offsetX

val Control.offsetY: Int
    get() = sweetSpacingData.offsetY

val Control.alignment: Alignment?
    get() = sweetLayoutData.alignment

val Control.matchParentSize: Boolean
    get() = sweetLayoutData.matchParentSize
