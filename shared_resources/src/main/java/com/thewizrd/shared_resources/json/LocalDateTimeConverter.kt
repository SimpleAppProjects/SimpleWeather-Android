package com.thewizrd.shared_resources.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LocalDateTimeConverter : JsonAdapter<LocalDateTime>() {
    companion object {
        private val formatter = DateTimeFormatter.ISO_INSTANT
    }

    @Synchronized
    override fun fromJson(reader: JsonReader): LocalDateTime? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }

        return LocalDateTime.ofInstant(
            Instant.from(formatter.parse(reader.nextString())), ZoneOffset.UTC
        )
    }

    @Synchronized
    override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.toInstant(ZoneOffset.UTC).toString())
        }
    }
}