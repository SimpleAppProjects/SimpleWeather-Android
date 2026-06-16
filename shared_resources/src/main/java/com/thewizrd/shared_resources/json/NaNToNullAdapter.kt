package com.thewizrd.shared_resources.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

val doubleNaNToNullAdapter = object : JsonAdapter<Double>() {
    override fun fromJson(reader: JsonReader): Double? {
        return reader.readJsonValue() as? Double
    }

    override fun toJson(writer: JsonWriter, value: Double?) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            writer.nullValue() // Replaces invalid values with null
        } else {
            writer.value(value)
        }
    }
}

val floatNaNToNullAdapter = object : JsonAdapter<Float>() {
    override fun fromJson(reader: JsonReader): Float? {
        return reader.readJsonValue() as? Float
    }

    override fun toJson(writer: JsonWriter, value: Float?) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            writer.nullValue() // Replaces invalid values with null
        } else {
            writer.value(value)
        }
    }
}