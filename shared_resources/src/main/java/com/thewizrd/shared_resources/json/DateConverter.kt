package com.thewizrd.shared_resources.json

import android.annotation.SuppressLint
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.thewizrd.shared_resources.utils.DateTimeUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
class DateConverter : JsonAdapter<Date>() {
    companion object {
        private const val FORMAT = DateTimeUtils.ZONED_DATETIME_FORMAT
    }

    private val dateFormats: MutableList<DateFormat> = mutableListOf()

    init {
        dateFormats.add(SimpleDateFormat(FORMAT, Locale.US))
        if (!Locale.getDefault().equals(Locale.US)) {
            dateFormats.add(SimpleDateFormat(FORMAT))
        }
    }

    @Synchronized
    override fun fromJson(reader: JsonReader): Date? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }

        return deserializeToDate(reader)
    }

    @Synchronized
    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val string = SimpleDateFormat(FORMAT).format(value)
            writer.value(string)
        }
    }

    private fun deserializeToDate(reader: JsonReader): Date {
        val string = reader.nextString()

        val result = synchronized(dateFormats) {
            dateFormats.forEach {
                runCatching {
                    return@synchronized it.parse(string)
                }
            }
        }

        if (result is Date) {
            return result
        }

        throw JsonDataException("Invalid date format: $string; expected: $FORMAT")
    }
}