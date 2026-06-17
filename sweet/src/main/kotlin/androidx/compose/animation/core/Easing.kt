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

// Sweet: Inlined cubic bezier math (evaluateCubic, findFirstCubicRoot, etc.) from compose.ui.graphics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Easing is a way to adjust an animation's fraction. Easing allows transitioning elements to speed
 * up and slow down, rather than moving at a constant rate.
 *
 * Fraction is a value between 0 and 1.0 indicating our current point in the animation where 0
 * represents the start and 1.0 represents the end.
 *
 * An [Easing] must map fraction=0.0 to 0.0 and fraction=1.0 to 1.0.
 */
public fun interface Easing {
    public fun transform(fraction: Float): Float
}

/**
 * Elements that begin and end at rest use this standard easing. They speed up quickly and slow down
 * gradually, in order to emphasize the end of the transition.
 *
 * Standard easing puts subtle attention at the end of an animation, by giving more time to
 * deceleration than acceleration. It is the most common form of easing.
 *
 * This is equivalent to the Android `FastOutSlowInInterpolator`
 */
public val FastOutSlowInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

/**
 * Incoming elements are animated using deceleration easing, which starts a transition at peak
 * velocity (the fastest point of an element's movement) and ends at rest.
 *
 * This is equivalent to the Android `LinearOutSlowInInterpolator`
 */
public val LinearOutSlowInEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/**
 * Elements exiting a screen use acceleration easing, where they start at rest and end at peak
 * velocity.
 *
 * This is equivalent to the Android `FastOutLinearInInterpolator`
 */
public val FastOutLinearInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

/**
 * It returns fraction unmodified. This is useful as a default value for cases where a [Easing] is
 * required but no actual easing is desired.
 */
public val LinearEasing: Easing = Easing { fraction -> fraction }

// This is equal to 1f.ulp or 1f.nextUp() - 1f, but neither ulp nor nextUp() are part of all KMP
// targets, only JVM and native
private const val OneUlpAt1 = 1.1920929e-7f

// Sweet: Local epsilon for root clamping
private const val FloatEpsilon: Float = 1E-4f

/**
 * A cubic polynomial easing.
 *
 * The [CubicBezierEasing] class implements third-order Bézier curves.
 *
 * This is equivalent to the Android `PathInterpolator` when a single cubic Bézier curve is
 * specified.
 *
 * Note: [CubicBezierEasing] instances are stateless and can be used concurrently from multiple
 * threads.
 *
 * Rather than creating a new instance, consider using one of the common cubic [Easing]s:
 *
 * @param a The x coordinate of the first control point. The line through the point (0, 0) and the
 *   first control point is tangent to the easing at the point (0, 0).
 * @param b The y coordinate of the first control point. The line through the point (0, 0) and the
 *   first control point is tangent to the easing at the point (0, 0).
 * @param c The x coordinate of the second control point. The line through the point (1, 1) and the
 *   second control point is tangent to the easing at the point (1, 1).
 * @param d The y coordinate of the second control point. The line through the point (1, 1) and the
 *   second control point is tangent to the easing at the point (1, 1).
 * @see FastOutSlowInEasing
 * @see LinearOutSlowInEasing
 * @see FastOutLinearInEasing
 */
public class CubicBezierEasing(
    private val a: Float,
    private val b: Float,
    private val c: Float,
    private val d: Float,
) : Easing {
    private val min: Float
    private val max: Float

    init {
        requirePrecondition(!a.isNaN() && !b.isNaN() && !c.isNaN() && !d.isNaN()) {
            "Parameters to CubicBezierEasing cannot be NaN. Actual parameters are: $a, $b, $c, $d."
        }
        val roots = FloatArray(5)
        // Sweet: Inline computeCubicVerticalBounds
        val extrema = computeCubicVerticalBoundsImpl(0.0f, b, d, 1.0f, roots, 0)
        min = extrema.first
        max = extrema.second
    }

    /**
     * Transforms the specified [fraction] in the range 0..1 by this cubic Bézier curve. To solve
     * the curve, [fraction] is used as the x coordinate along the curve, and the corresponding y
     * coordinate on the curve is returned. If no solution exists, this method throws an
     * [IllegalArgumentException].
     *
     * @throws IllegalArgumentException If the cubic Bézier curve cannot be solved
     */
    override fun transform(fraction: Float): Float {
        return if (fraction > 0f && fraction < 1f) {
            // We translate the coordinates by the fraction when calling findFirstCubicRoot,
            // but we need to make sure the translation can be done at 1.0f so we take at
            // least 1 ulp at 1.0f
            val f = max(fraction, OneUlpAt1)
            val t = findFirstCubicRootImpl(0.0f - f, a - f, c - f, 1.0f - f)

            // No root, the cubic curve has no solution
            if (t.isNaN()) {
                throwNoSolution(fraction)
            }

            // Don't clamp the values since the curve might be used to over- or under-shoot
            // The test above that checks if fraction is in ]0..1[ will ensure we start and
            // end at 0 and 1 respectively
            evaluateCubicImpl(b, d, t).coerceIn(min, max)
        } else {
            fraction
        }
    }

    private fun throwNoSolution(fraction: Float) {
        throw IllegalArgumentException(
            "The cubic curve with parameters ($a, $b, $c, $d) has no solution at $fraction",
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is CubicBezierEasing &&
            a == other.a &&
            b == other.b &&
            c == other.c &&
            d == other.d
    }

    override fun hashCode(): Int {
        return ((a.hashCode() * 31 + b.hashCode()) * 31 + c.hashCode()) * 31 + d.hashCode()
    }

    override fun toString(): String = "CubicBezierEasing(a=$a, b=$b, c=$c, d=$d)"

    companion object {
        // Sweet: Inlined evaluateCubic (3-param version: evaluates y given p1, p2, t
        // for cubic bezier from (0,0) to (1,1))
        private fun evaluateCubicImpl(
            p1: Float,
            p2: Float,
            t: Float,
        ): Float {
            val a = 1.0f / 3.0f + (p1 - p2)
            val b = (p2 - 2.0f * p1)
            val c = p1
            return 3.0f * ((a * t + b) * t + c) * t
        }

        // Sweet: Inlined evaluateCubic (5-param version)
        private fun evaluateCubicImpl(
            p0: Float,
            p1: Float,
            p2: Float,
            p3: Float,
            t: Float,
        ): Float {
            val a = p3 + 3.0f * (p1 - p2) - p0
            val b = 3.0f * (p2 - 2.0f * p1 + p0)
            val c = 3.0f * (p1 - p0)
            return ((a * t + b) * t + c) * t + p0
        }

        // Sweet: Returns r if in [0..1], NaN otherwise (with epsilon tolerance)
        private fun clampValidRootInUnitRange(r: Float): Float {
            val s = r.coerceIn(0f, 1f)
            return if (abs(s - r) > FloatEpsilon) Float.NaN else s
        }

        // Sweet: Inlined findFirstCubicRoot using Cardano's algorithm
        // Matches MPP implementation in compose.ui.graphics.Bezier.kt
        private fun findFirstCubicRootImpl(
            p0: Float,
            p1: Float,
            p2: Float,
            p3: Float,
        ): Float {
            val a = 3.0 * (p0 - 2.0 * p1 + p2)
            val b = 3.0 * (p1 - p0)
            var c = p0.toDouble()
            val d = (-p0 + 3.0 * (p1 - p2) + p3).toDouble()

            // Not a cubic — degenerates to quadratic or linear
            if (abs(d) < 1e-7) {
                if (abs(a) < 1e-7) {
                    // Not a quadratic either — linear or no solution
                    if (abs(b) < 1e-7) return Float.NaN
                    return clampValidRootInUnitRange((-c / b).toFloat())
                }
                // Quadratic: a*t^2 + b*t + c = 0
                val q = sqrt(b * b - 4.0 * a * c)
                val a2 = 2.0 * a
                val root = clampValidRootInUnitRange(((q - b) / a2).toFloat())
                if (!root.isNaN()) return root
                return clampValidRootInUnitRange(((-b - q) / a2).toFloat())
            }

            // Normalize: divide by leading coefficient d
            val aNorm = a / d
            val bNorm = b / d
            c /= d

            val o3 = (3.0 * bNorm - aNorm * aNorm) / 9.0
            val q2 = (2.0 * aNorm * aNorm * aNorm - 9.0 * aNorm * bNorm + 27.0 * c) / 54.0
            val discriminant = q2 * q2 + o3 * o3 * o3
            val a3 = aNorm / 3.0

            if (discriminant < 0.0) {
                val mp33 = -(o3 * o3 * o3)
                val r = sqrt(mp33)
                val t = -q2 / r
                val cosPhi = t.coerceIn(-1.0, 1.0)
                val phi = acos(cosPhi)
                val t1 = 2.0 * cbrt(r)
                val tau = 2.0 * PI

                var root = clampValidRootInUnitRange((t1 * cos(phi / 3.0) - a3).toFloat())
                if (!root.isNaN()) return root
                root = clampValidRootInUnitRange((t1 * cos((phi + tau) / 3.0) - a3).toFloat())
                if (!root.isNaN()) return root
                return clampValidRootInUnitRange((t1 * cos((phi + 2.0 * tau) / 3.0) - a3).toFloat())
            } else if (abs(discriminant) < 1e-12) {
                val u1 = -cbrt(q2)
                val root = clampValidRootInUnitRange((2.0 * u1 - a3).toFloat())
                if (!root.isNaN()) return root
                return clampValidRootInUnitRange((-u1 - a3).toFloat())
            }

            val sd = sqrt(discriminant)
            val u1 = cbrt(-q2 + sd)
            val v1 = cbrt(q2 + sd)
            return clampValidRootInUnitRange((u1 - v1 - a3).toFloat())
        }

        // Sweet: Inlined findQuadraticRoots
        private fun findQuadraticRoots(
            p0: Float,
            p1: Float,
            p2: Float,
            roots: FloatArray,
            index: Int = 0,
        ): Int {
            val a = p0.toDouble()
            val bVal = p1.toDouble()
            val cVal = p2.toDouble()
            val d = a - 2.0 * bVal + cVal

            var rootCount = 0

            if (d != 0.0) {
                val v1 = -sqrt(bVal * bVal - a * cVal)
                val v2 = -a + bVal
                rootCount += writeValidRootInUnitRange((-(v1 + v2) / d).toFloat(), roots, index)
                rootCount +=
                    writeValidRootInUnitRange(((v1 - v2) / d).toFloat(), roots, index + rootCount)
            } else if (bVal != cVal) {
                rootCount +=
                    writeValidRootInUnitRange(
                        ((2.0 * bVal - cVal) / (2.0 * bVal - 2.0 * cVal)).toFloat(),
                        roots, index,
                    )
            }

            return rootCount
        }

        // Sweet: Inlined findLineRoot
        private fun findLineRoot(
            p0: Float,
            p1: Float,
            roots: FloatArray,
            index: Int = 0,
        ): Int =
            writeValidRootInUnitRange(-p0 / (p1 - p0), roots, index)

        // Sweet: Inlined writeValidRootInUnitRange
        private fun writeValidRootInUnitRange(
            r: Float,
            roots: FloatArray,
            index: Int,
        ): Int {
            val v = clampValidRootInUnitRange(r)
            roots[index] = v
            return if (v.isNaN()) 0 else 1
        }

        // Sweet: Inlined computeCubicVerticalBounds
        private fun computeCubicVerticalBoundsImpl(
            p0y: Float,
            p1y: Float,
            p2y: Float,
            p3y: Float,
            roots: FloatArray,
            index: Int = 0,
        ): Pair<Float, Float> {
            val d0 = 3.0f * (p1y - p0y)
            val d1 = 3.0f * (p2y - p1y)
            val d2 = 3.0f * (p3y - p2y)
            var count = findQuadraticRoots(d0, d1, d2, roots, index)

            val dd0 = 2.0f * (d1 - d0)
            val dd1 = 2.0f * (d2 - d1)
            count += findLineRoot(dd0, dd1, roots, index + count)

            var minY = min(p0y, p3y)
            var maxY = max(p0y, p3y)

            for (i in 0 until count) {
                val t = roots[i]
                val y = evaluateCubicImpl(p0y, p1y, p2y, p3y, t)
                minY = min(minY, y)
                maxY = max(maxY, y)
            }

            return Pair(minY, maxY)
        }
    }
}
