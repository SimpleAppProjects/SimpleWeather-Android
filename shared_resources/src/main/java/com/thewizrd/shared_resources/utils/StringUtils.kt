package com.thewizrd.shared_resources.utils

import android.text.TextUtils
import java.util.regex.Pattern

object StringUtils {
    @JvmStatic
    fun String?.isNullOrEmpty(): Boolean {
        return this == null || this.isEmpty()
    }

    @JvmStatic
    fun String?.isNullOrWhitespace(): Boolean {
        return this.isNullOrBlank()
    }

    @JvmStatic
    fun String.toUpperCase(): String {
        if (this.isBlank()) {
            return this
        }
        val buffer = this.toCharArray()
        var capitalizeNext = true
        for (i in buffer.indices) {
            val ch = buffer[i]
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch)
                capitalizeNext = false
            }
        }
        return String(buffer)
    }

    @JvmStatic
    fun String.toPascalCase(): String {
        val strArray = this.split("\\.".toRegex(), limit = 0)
        val sb = StringBuilder()

        for (str in strArray) {
            if (str.isEmpty()) continue

            sb.append(str.trim().substring(0, 1).uppercase())
                .append(str.trim().substring(1).lowercase())
                .append(". ")
        }

        return sb.toString().trim()
    }

    @JvmStatic
    fun CharSequence.removeNonDigitChars(): CharSequence {
        return if (TextUtils.isEmpty(this) || this.isBlank()) {
            ""
        } else {
            this.replace("[^\\d.-]".toRegex(), "").trim()
        }
    }

    @JvmStatic
    fun String.removeNonDigitChars(): String {
        return if (this.isBlank()) {
            ""
        } else {
            this.replace("[^\\d.-]".toRegex(), "").trim()
        }
    }

    @JvmStatic
    fun String.removeDigitChars(): String {
        return if (this.isBlank()) {
            ""
        } else {
            this.replace("[0-9]".toRegex(), "").trim()
        }
    }

    @JvmStatic
    fun String?.containsDigits(): Boolean {
        return if (this.isNullOrBlank()) {
            false
        } else {
            Pattern.matches(".*[0-9].*", this)
        }
    }

    @JvmStatic
    fun lineSeparator(): String {
        return System.lineSeparator()
    }

    fun String.unescapeUnicode(): String {
        if (this.isNullOrEmpty()) {
            return this
        } else {
            val sb = StringBuilder()

            val seqEnd = this.length
            var i = 0
            while (i < this.length) {
                // Uses -2 to ensure there is something after the &#
                val c = this[i]
                if (this[i] == '&' && i < seqEnd - 2 && this[i + 1] == '#') {
                    var start = i + 2
                    var isHex = false

                    val firstChar = this[start]
                    if (firstChar == 'x' || firstChar == 'X') {
                        start++
                        isHex = true
                        if (start == seqEnd) {
                            sb.append(this.substring(i))
                            break
                        }
                    }

                    var end = start
                    while (end < seqEnd && this[end] != ';') {
                        end++
                    }

                    val value = try {
                        if (isHex) {
                            this.substring(start, end).toInt(16)
                        } else {
                            this.substring(start, end).toInt(10)
                        }
                    } catch (nfe: NumberFormatException) {
                        sb.append(this.substring(i))
                        break
                    }

                    val chars = Character.toChars(value)
                    sb.append(chars)

                    i = end
                } else {
                    sb.append(this[i])
                }

                i++
            }

            return sb.toString()
        }
    }
}