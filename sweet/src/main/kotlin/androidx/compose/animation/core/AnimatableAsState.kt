/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

/**
 * A [State] wrapper whose [value] reads the backing [Animatable].
 *
 * Because [Animatable.value] is snapshot state, reading this [State] during composition
 * subscribes to the animation: every frame the value changes, readers recompose.
 */
private class AnimatableState<T>(private val animatable: Animatable<T, *>) : State<T> {
    override val value: T
        get() = animatable.value
}

/**
 * Animates `Float` values toward [targetValue], recomposing readers on every frame.
 *
 * The animation runs in a [LaunchedEffect] keyed on [targetValue] / [animationSpec]: when either
 * changes, the in-flight animation is cancelled and a new one starts from the current value.
 *
 * Reactivity relies on [Animatable.value] being snapshot-backed — writes in the animation
 * coroutine drive recomposition through the framework's snapshot apply observer + frame clock.
 */
@Composable
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = spring(),
    label: String = "FloatAnimation",
): State<Float> {
    val animatable = remember { Animatable(targetValue, FloatConverter) }
    LaunchedEffect(targetValue, animationSpec) {
        animatable.animateTo(targetValue, animationSpec)
    }
    return remember(animatable) { AnimatableState(animatable) }
}

/**
 * Animates [Dp] values toward [targetValue], recomposing readers on every frame. See
 * [animateFloatAsState] for the reactivity contract.
 */
@Composable
fun animateDpAsState(
    targetValue: Dp,
    animationSpec: AnimationSpec<Dp> = spring(),
    label: String = "DpAnimation",
): State<Dp> {
    val animatable = remember { Animatable(targetValue, DpConverter) }
    LaunchedEffect(targetValue, animationSpec) {
        animatable.animateTo(targetValue, animationSpec)
    }
    return remember(animatable) { AnimatableState(animatable) }
}

/** [TwoWayConverter] for [Dp] ↔ [AnimationVector1D]. */
private val DpConverter: TwoWayConverter<Dp, AnimationVector1D> =
    object : TwoWayConverter<Dp, AnimationVector1D> {
        override val convertToVector: (Dp) -> AnimationVector1D = { AnimationVector1D(it.value) }
        override val convertFromVector: (AnimationVector1D) -> Dp = { Dp(it.value) }
    }
