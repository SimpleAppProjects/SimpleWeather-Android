package com.thewizrd.shared_resources.utils

import android.util.Log
import androidx.core.util.AtomicFile
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.thewizrd.shared_resources.json.CustomJsonConverter
import com.thewizrd.shared_resources.json.DateConverter
import com.thewizrd.shared_resources.json.LocalDateTimeConverter
import com.thewizrd.shared_resources.json.ZonedDateTimeConverter
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.*
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

object JSONParser {
    private var moshi: Moshi

    init {
        moshi = Moshi.Builder()
            .add(Date::class.java, DateConverter())
            .add(LocalDateTime::class.java, LocalDateTimeConverter())
            .add(ZonedDateTime::class.java, ZonedDateTimeConverter())
            .add(LocationData::class.java, CustomJsonConverter(LocationData::class.java))
            .add(Weather::class.java, CustomJsonConverter(Weather::class.java))
            .add(Location::class.java, CustomJsonConverter(Location::class.java))
            .add(Forecast::class.java, CustomJsonConverter(Forecast::class.java))
            .add(HourlyForecast::class.java, CustomJsonConverter(HourlyForecast::class.java))
            .add(TextForecast::class.java, CustomJsonConverter(TextForecast::class.java))
            .add(MinutelyForecast::class.java, CustomJsonConverter(MinutelyForecast::class.java))
            .add(Condition::class.java, CustomJsonConverter(Condition::class.java))
            .add(Atmosphere::class.java, CustomJsonConverter(Atmosphere::class.java))
            .add(Astronomy::class.java, CustomJsonConverter(Astronomy::class.java))
            .add(Precipitation::class.java, CustomJsonConverter(Precipitation::class.java))
            .add(WeatherAlert::class.java, CustomJsonConverter(WeatherAlert::class.java))
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @JvmStatic
    fun registerTypeAdapterFactory(factory: JsonAdapter.Factory) {
        moshi = moshi.newBuilder()
            .add(factory)
            .build()
    }

    @JvmStatic
    fun <T> registerTypeAdapter(type: Type, typeAdapter: JsonAdapter<T>) {
        moshi = moshi.newBuilder()
            .add(type, typeAdapter)
            .build()
    }

    @JvmStatic
    fun <T> deserializer(response: String?, type: Type): T? {
        if (response.isNullOrBlank()) return null

        var `object`: T? = null

        try {
            val adapter = moshi.adapter<T>(type)
                .serializeNulls()
                .nullSafe()

            `object` = adapter.fromJson(response)
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex)
        }

        return `object`
    }

    @JvmStatic
    fun <T> deserializer(stream: InputStream, type: Type): T? {
        var `object`: T? = null

        try {
            stream.source().buffer().use { buffer ->
                val adapter = moshi.adapter<T>(type)
                    .serializeNulls()
                    .nullSafe()

                `object` = adapter.fromJson(buffer)
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex)
        }

        return `object`
    }

    @JvmStatic
    fun <T> deserializer(file: File, type: Type): T? {
        while (FileUtils.isFileLocked(file)) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        var `object`: T? = null
        val mFile = AtomicFile(file)

        try {
            mFile.openRead().use { stream ->
                stream.source().buffer().use { buffer ->
                    val adapter = moshi.adapter<T>(type)
                        .serializeNulls()
                        .nullSafe()

                    `object` = adapter.fromJson(buffer)
                }
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex)
        }

        return `object`
    }

    @JvmStatic
    fun <T> serializer(`object`: T, file: File) {
        // Wait for file to be free
        while (FileUtils.isFileLocked(file)) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        val mFile = AtomicFile(file)

        try {
            mFile.startWrite().use { stream ->
                val adapter = moshi.adapter<T>(`object`!!::class.java)
                    .serializeNulls()
                    .nullSafe()

                adapter.toJson(stream.sink().buffer(), `object`)
                mFile.finishWrite(stream)
            }
        } catch (ex: IOException) {
            Logger.writeLine(Log.ERROR, ex)
        }
    }

    @JvmStatic
    fun <T> serializer(`object`: T?, type: Type): String? {
        if (`object` == null) return null

        val adapter = moshi.adapter<T>(type)
            .serializeNulls()
            .nullSafe()

        return adapter.toJson(`object`)
    }

    /* Custom [De]Serializers for CustomJsonObjects */
    @JvmStatic
    inline fun <reified T : CustomJsonObject> serializer(
        `object`: T?,
        type: Type = T::class.java
    ): String? {
        if (`object` == null) return null

        val buffer = Buffer()
        val writer = JsonWriter.of(buffer)

        try {
            `object`.toJson(writer)
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
            return null
        }

        return buffer.readUtf8()
    }

    @JvmStatic
    inline fun <reified T : CustomJsonObject> deserializer(
        response: String?,
        obj: Class<T> = T::class.java
    ): T? {
        if (response.isNullOrBlank()) return null

        var `object`: T? = null

        try {
            JsonReader.of(Buffer().writeUtf8(response)).use { reader ->
                `object` = obj.newInstance()
                `object`!!.fromJson(reader)
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex)
        }

        return `object`
    }

    @JvmStatic
    inline fun <reified T : CustomJsonObject> deserializer(
        stream: InputStream,
        obj: Class<T> = T::class.java
    ): T? {
        var `object`: T? = null

        try {
            JsonReader.of(stream.source().buffer()).use { reader ->
                `object` = obj.newInstance()
                `object`!!.fromJson(reader)
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex)
        }

        return `object`
    }
}