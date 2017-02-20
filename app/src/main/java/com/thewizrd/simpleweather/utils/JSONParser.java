package com.thewizrd.simpleweather.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;

public class JSONParser {
    public static Object deserializer(String response, Class<?> obj) {
        Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").create();
        Object object = gson.fromJson(response, obj);

        return object;
    }

    public static void serializer(Object object, File file) throws IOException {
        Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").create();
        FileUtils.writeToFile(gson.toJson(object), file);
    }
}