package com.thewizrd.shared_resources.utils;

import android.util.Log;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomJsonConverter<T> extends TypeAdapter<T> {
    private Class<T> clazz;

    public CustomJsonConverter(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        try {
            Method toJson = clazz.getMethod("toJson");
            out.value((String) toJson.invoke(value));
        } catch (NoSuchMethodException e) {
            Logger.writeLine(Log.ERROR, "SimpleWeather: CustomJsonConverter: error invoking ToJson method");
            Logger.writeLine(Log.ERROR, "SimpleWeather: CustomJsonConverter: object: %s", value.toString());
        } catch (IllegalAccessException e) {
            Logger.writeLine(Log.ERROR, "SimpleWeather: CustomJsonConverter: error invoking ToJson method");
            Logger.writeLine(Log.ERROR, "SimpleWeather: CustomJsonConverter: object: %s", value.toString());
        } catch (InvocationTargetException e) {
            Logger.writeLine(Log.ERROR, "SimpleWeather: CustomJsonConverter: error invoking ToJson method");
            Logger.writeLine(Log.ERROR, "SimpleWeather: CustomJsonConverter: object: %s", value.toString());
        }
    }

    @Override
    public T read(JsonReader in) {
        try {
            Method fromJson = clazz.getMethod("fromJson", JsonReader.class);
            if (fromJson != null && fromJson.getReturnType() == clazz) {
                T object = (T) fromJson.invoke(null, in);

                if (object != null)
                    return object;
            }
        } catch (NoSuchMethodException e) {
            throw new JsonParseException(String.format("%s type does not implement FromJson(string) method", clazz.getName()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}

