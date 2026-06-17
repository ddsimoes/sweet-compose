/*
 * Copyright 2019 The Android Open Source Project
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

/**
 * Physics constants for spring-based animations.
 * Sweet: Extracted from VectorizedAnimationSpec.kt (not yet ported).
 */
public object Spring {
    /** Stiffness constant for a high-stiffness spring. */
    public const val StiffnessHigh: Float = 10_000f

    /** Stiffness constant for a medium-stiffness spring. This is the default stiffness. */
    public const val StiffnessMedium: Float = 1500f

    /** Stiffness constant for a medium-low-stiffness spring. */
    public const val StiffnessMediumLow: Float = 400f

    /** Stiffness constant for a low-stiffness spring. */
    public const val StiffnessLow: Float = 200f

    /** Stiffness constant for a very-low-stiffness spring. */
    public const val StiffnessVeryLow: Float = 50f

    /**
     * Damping ratio for a very bouncy spring. Note: Damping ratio less than 1 will oscillate around
     * the final position.
     */
    public const val DampingRatioHighBouncy: Float = 0.2f

    /**
     * Damping ratio for a medium-bouncy spring. Note: Damping ratio less than 1 will oscillate
     * around the final position.
     */
    public const val DampingRatioMediumBouncy: Float = 0.5f

    /**
     * Damping ratio for a low-bouncy spring. Note: Damping ratio less than 1 will oscillate around
     * the final position.
     */
    public const val DampingRatioLowBouncy: Float = 0.75f

    /**
     * Damping ratio for a no-bouncy spring. This is the default damping ratio.
     */
    public const val DampingRatioNoBouncy: Float = 1f

    /**
     * The default displacement threshold for determining when a spring animation is visually close
     * enough to the target.
     */
    public const val DefaultDisplacementThreshold: Float = 0.01f
}
