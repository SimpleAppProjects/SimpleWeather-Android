package com.thewizrd.simpleweather.utils;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * Created by bryan on 2/10/2017.
 */

public class JSONParser {
    public static Object deserializer(String response, Object obj) {
        Gson gson = new Gson();
        Object object = gson.fromJson(response, obj.getClass());

        return object;
    }
}
