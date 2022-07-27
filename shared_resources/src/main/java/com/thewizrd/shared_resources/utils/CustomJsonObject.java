package com.thewizrd.shared_resources.utils;

import androidx.annotation.NonNull;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

public abstract class CustomJsonObject {
    public abstract void fromJson(@NonNull JsonReader reader);

    public abstract void toJson(@NonNull JsonWriter writer);
}


