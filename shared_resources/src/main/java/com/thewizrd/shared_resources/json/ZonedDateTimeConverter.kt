package com.thewizrd.shared_resources.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.thewizrd.shared_resources.utils.DateTimeUtils
import java.io.IOException
import java.time.ZonedDateTime

class ZonedDateTimeConverter : JsonAdapter<ZonedDateTime>() {
    companion object {
        private val formatter = DateTimeUtils.getZonedDateTimeFormatter()
    }

    @Synchronized
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): ZonedDateTime? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }

        return ZonedDateTime.parse(reader.nextString(), formatter)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: ZonedDateTime?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(formatter.format(value))
        }
    }
}