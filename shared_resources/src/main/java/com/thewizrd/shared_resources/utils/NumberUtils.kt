package com.thewizrd.shared_resources.utils

object NumberUtils {
    @JvmStatic
    fun String.tryParseInt(): Int? {
        return this.toIntOrNull()
    }

    @JvmStatic
    fun String.tryParseInt(defaultValue: Int): Int {
        return this.toIntOrNull() ?: defaultValue
    }

    @JvmStatic
    fun String.tryParseFloat(): Float? {
        return this.toFloatOrNull()
    }

    @JvmStatic
    fun String.tryParseFloat(defaultValue: Float): Float {
        return this.toFloatOrNull() ?: defaultValue
    }

    @JvmStatic
    fun String.tryParseDouble(): Double? {
        return this.toDoubleOrNull()
    }

    @JvmStatic
    fun String.tryParseDouble(defaultValue: Double): Double {
        return this.toDoubleOrNull() ?: defaultValue
    }

    @JvmStatic
    fun toString(number: Float?): String? {
        return number?.toString()
    }

    @JvmStatic
    fun toString(number: Int?): String? {
        return number?.toString()
    }

    @JvmStatic
    fun Int?.getValueOrDefault(defaultValue: Int): Int {
        return this ?: defaultValue
    }

    @JvmStatic
    fun Float?.getValueOrDefault(defaultValue: Float): Float {
        return this ?: defaultValue
    }

    @JvmStatic
    fun Double?.getValueOrDefault(defaultValue: Double): Double {
        return this ?: defaultValue
    }
}