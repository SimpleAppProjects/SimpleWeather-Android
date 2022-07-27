package com.thewizrd.shared_resources.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.thewizrd.shared_resources.utils.CustomJsonObject
import java.io.IOException

class CustomJsonConverter<T : CustomJsonObject>(private val clazz: Class<T>) : JsonAdapter<T>() {
    @Synchronized
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): T? {
        try {
            val `object` = clazz.newInstance()
            `object`.fromJson(reader)
            return `object`
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return null
    }

    @Synchronized
    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: T?) {
        if (value == null) {
            writer.nullValue()
        } else {
            value.toJson(writer)
        }
    }
}