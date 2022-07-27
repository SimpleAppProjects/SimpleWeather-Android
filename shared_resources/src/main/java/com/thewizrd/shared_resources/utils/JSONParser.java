package com.thewizrd.shared_resources.utils;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.util.AtomicFile;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;
import com.thewizrd.shared_resources.json.CustomJsonConverter;
import com.thewizrd.shared_resources.json.DateConverter;
import com.thewizrd.shared_resources.json.LocalDateTimeConverter;
import com.thewizrd.shared_resources.json.ZonedDateTimeConverter;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.model.Astronomy;
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere;
import com.thewizrd.shared_resources.weatherdata.model.Condition;
import com.thewizrd.shared_resources.weatherdata.model.Forecast;
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.model.Location;
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast;
import com.thewizrd.shared_resources.weatherdata.model.Precipitation;
import com.thewizrd.shared_resources.weatherdata.model.TextForecast;
import com.thewizrd.shared_resources.weatherdata.model.Weather;
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

public class JSONParser {

    private static Moshi moshi;

    static {
        moshi = new Moshi.Builder()
                .add(Date.class, new DateConverter())
                .add(LocalDateTime.class, new LocalDateTimeConverter())
                .add(ZonedDateTime.class, new ZonedDateTimeConverter())
                .add(LocationData.class, new CustomJsonConverter<>(LocationData.class))
                .add(Weather.class, new CustomJsonConverter<>(Weather.class))
                .add(Location.class, new CustomJsonConverter<>(Location.class))
                .add(Forecast.class, new CustomJsonConverter<>(Forecast.class))
                .add(HourlyForecast.class, new CustomJsonConverter<>(HourlyForecast.class))
                .add(TextForecast.class, new CustomJsonConverter<>(TextForecast.class))
                .add(MinutelyForecast.class, new CustomJsonConverter<>(MinutelyForecast.class))
                .add(Condition.class, new CustomJsonConverter<>(Condition.class))
                .add(Atmosphere.class, new CustomJsonConverter<>(Atmosphere.class))
                .add(Astronomy.class, new CustomJsonConverter<>(Astronomy.class))
                .add(Precipitation.class, new CustomJsonConverter<>(Precipitation.class))
                .add(WeatherAlert.class, new CustomJsonConverter<>(WeatherAlert.class))
                .add(new KotlinJsonAdapterFactory())
                .build();
    }

    public static void registerTypeAdapterFactory(JsonAdapter.Factory factory) {
        moshi = moshi.newBuilder()
                .add(factory)
                .build();
    }

    public static <T> void registerTypeAdapter(Type type, JsonAdapter<T> typeAdapter) {
        moshi = moshi.newBuilder()
                .add(type, typeAdapter)
                .build();
    }

    @Nullable
    public static <T> T deserializer(String response, Type type) {
        T object = null;

        try {
            JsonAdapter<T> adapter = moshi.adapter(type);
            adapter = adapter.serializeNulls().nullSafe();
            object = adapter.fromJson(response);
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex);
        }

        return object;
    }

    @Nullable
    public static <T> T deserializer(InputStream stream, Type type) {
        T object = null;

        try (BufferedSource buffer = Okio.buffer(Okio.source(stream))) {
            JsonAdapter<T> adapter = moshi.adapter(type);
            adapter = adapter.serializeNulls().nullSafe();
            object = adapter.fromJson(buffer);
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex);
        }

        return object;
    }

    @Nullable
    public static <T> T deserializer(File file, Type type) {
        while (FileUtils.isFileLocked(file)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        T object = null;

        AtomicFile mFile = new AtomicFile(file);
        try (
                FileInputStream stream = mFile.openRead();
                BufferedSource buffer = Okio.buffer(Okio.source(stream))
        ) {
            JsonAdapter<T> adapter = moshi.adapter(type);
            adapter = adapter.serializeNulls().nullSafe();
            object = adapter.fromJson(buffer);
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex);
        }

        return object;
    }

    public static <T> void serializer(T object, File file) {
        // Wait for file to be free
        while (FileUtils.isFileLocked(file)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        AtomicFile mFile = new AtomicFile(file);

        try (FileOutputStream stream = mFile.startWrite()) {
            JsonAdapter<T> adapter = moshi.adapter((Type) object.getClass());
            adapter = adapter.serializeNulls().nullSafe();
            adapter.toJson(Okio.buffer(Okio.sink(stream)), object);

            mFile.finishWrite(stream);
        } catch (IOException ex) {
            Logger.writeLine(Log.ERROR, ex);
        }
    }

    @Nullable
    public static <T> String serializer(T object, Type type) {
        if (object == null) return null;

        JsonAdapter<T> adapter = moshi.adapter(type);
        adapter = adapter.serializeNulls().nullSafe();
        return adapter.toJson(object);
    }

    /* Custom [De]Serializers for CustomJsonObjects */
    @Nullable
    public static <T extends CustomJsonObject> String serializer(T object, Type type) {
        if (object == null) return null;

        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);

        try {
            object.toJson(writer);
        } catch (Exception e) {
            Logger.writeLine(Log.ERROR, e);
            return null;
        }

        return buffer.readUtf8();
    }

    @Nullable
    public static <T extends CustomJsonObject> T deserializer(String response, Class<T> obj) {
        T object = null;

        try (JsonReader reader = JsonReader.of(new Buffer().writeUtf8(response))) {
            object = obj.newInstance();
            object.fromJson(reader);
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex);
        }

        return object;
    }

    @Nullable
    public static <T extends CustomJsonObject> T deserializer(InputStream stream, Class<T> obj) {
        T object = null;

        try (JsonReader reader = JsonReader.of(Okio.buffer(Okio.source(stream)))) {
            object = obj.newInstance();
            object.fromJson(reader);
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex);
        }

        return object;
    }
}