package androidx.compose.ui.text.font

import androidx.compose.runtime.Immutable

/**
 * Defines the thickness of a font.
 */
@Immutable
class FontWeight(val weight: Int) {
    companion object {
        /**
         * Represents a normal font weight (400).
         */
        val Normal = FontWeight(400)

        /**
         * Represents a bold font weight (700).
         */
        val Bold = FontWeight(700)

        /**
         * Represents a light font weight (300).
         */
        val Light = FontWeight(300)

        /**
         * Represents a semi-bold font weight (600).
         */
        val SemiBold = FontWeight(600)

        /**
         * Represents a medium font weight (500).
         */
        val Medium = FontWeight(500)

        /**
         * Represents a thin font weight (100).
         */
        val Thin = FontWeight(100)

        /**
         * Represents an extra light font weight (200).
         */
        val ExtraLight = FontWeight(200)

        /**
         * Represents an extra bold font weight (800).
         */
        val ExtraBold = FontWeight(800)

        /**
         * Represents a black font weight (900).
         */
        val Black = FontWeight(900)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontWeight) return false
        return weight == other.weight
    }

    override fun hashCode(): Int = weight.hashCode()

    override fun toString(): String = "FontWeight(weight=$weight)"
}
