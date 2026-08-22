package com.thewizrd.simpleweather.radar

import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sinh

data class BoundingBox(
    val xMin: Double,
    val yMin: Double,
    val xMax: Double,
    val yMax: Double,
) {
    override fun toString(): String {
        return toXYString()
    }

    fun toYXString(): String {
        return "$yMin,$xMin,$yMax,$xMax"
    }

    fun toXYString(): String {
        return "$xMin,$yMin,$xMax,$yMax"
    }

    fun intersects(other: BoundingBox): Boolean {
        return xMin <= other.xMax && xMax >= other.xMin &&
                yMin <= other.yMax && yMax >= other.yMin
    }

    companion object {
        fun fromTile(x: Int, y: Int, zoom: Int): BoundingBox {
            return BoundingBox(
                yMin = tile2lat(y + 1, zoom),
                yMax = tile2lat(y, zoom),
                xMin = tile2lon(x, zoom),
                xMax = tile2lon(x + 1, zoom)
            )
        }

        // Source: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Common_programming_languages
        fun tile2lon(x: Int, z: Int): Double {
            return x / 2.0.pow(z.toDouble()) * 360.0 - 180
        }

        fun tile2lat(y: Int, z: Int): Double {
            val n: Double = Math.PI - (2.0 * Math.PI * y) / 2.0.pow(z.toDouble())
            return Math.toDegrees(atan(sinh(n)))
        }
    }
}