package com.thewizrd.shared_resources.utils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public abstract class CustomJsonObject {
    public abstract void fromJson(JsonReader reader);

    public abstract void toJson(JsonWriter writer);
}


