package com.thewizrd.shared_resources.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class CustomJsonConverter<T extends CustomJsonObject> extends TypeAdapter<T> {
    private Class<T> clazz;

    public CustomJsonConverter(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        value.toJson(out);
    }

    @Override
    public T read(JsonReader in) {
        try {
            T object = clazz.newInstance();
            object.fromJson(in);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}

