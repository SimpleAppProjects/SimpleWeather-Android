package com.thewizrd.shared_resources.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

val doubleNaNToNullAdapter = object : JsonAdapter<Double>() {
    override fun fromJson(reader: JsonReader): Double? {
        val value = reader.readJsonValue()
        return (value as? Number)?.toDouble()
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
        val value = reader.readJsonValue()
        return (value as? Number)?.toFloat()
    }

    override fun toJson(writer: JsonWriter, value: Float?) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            writer.nullValue() // Replaces invalid values with null
        } else {
            writer.value(value)
        }
    }
}