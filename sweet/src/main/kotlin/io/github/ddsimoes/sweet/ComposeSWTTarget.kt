package io.github.ddsimoes.sweet

import androidx.compose.runtime.ComposableTargetMarker

/**
 * Annotation to mark composables that are intended for SWT
 */
@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "SWT Composable")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class ComposeSWTTarget
