package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;
import java.io.StringReader;

public class Pollen extends CustomJsonObject {

    public enum PollenCount {
        UNKNOWN,
        LOW,
        MODERATE,
        HIGH,
        VERY_HIGH
    }

    @SerializedName("tree_pollen")
    private PollenCount treePollenCount;

    @SerializedName("grass_pollen")
    private PollenCount grassPollenCount;

    @SerializedName("ragweed_pollen")
    private PollenCount ragweedPollenCount;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Pollen() {
        // Needed for deserialization
    }

    public PollenCount getTreePollenCount() {
        return treePollenCount;
    }

    public void setTreePollenCount(PollenCount treePollenCount) {
        this.treePollenCount = treePollenCount;
    }

    public PollenCount getGrassPollenCount() {
        return grassPollenCount;
    }

    public void setGrassPollenCount(PollenCount grassPollenCount) {
        this.grassPollenCount = grassPollenCount;
    }

    public PollenCount getRagweedPollenCount() {
        return ragweedPollenCount;
    }

    public void setRagweedPollenCount(PollenCount ragweedPollenCount) {
        this.ragweedPollenCount = ragweedPollenCount;
    }

    @Override
    public void fromJson(JsonReader extReader) {
        try {
            JsonReader reader;
            String jsonValue;

            if (extReader.peek() == JsonToken.STRING) {
                jsonValue = extReader.nextString();
            } else {
                jsonValue = null;
            }

            if (jsonValue == null)
                reader = extReader;
            else {
                reader = new JsonReader(new StringReader(jsonValue));
                reader.beginObject(); // StartObject
            }

            while (reader.hasNext() && reader.peek() != JsonToken.END_OBJECT) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }

                switch (property) {
                    case "tree_pollen":
                        this.treePollenCount = PollenCount.valueOf(reader.nextString());
                        break;
                    case "grass_pollen":
                        this.grassPollenCount = PollenCount.valueOf(reader.nextString());
                        break;
                    case "ragweed_pollen":
                        this.ragweedPollenCount = PollenCount.valueOf(reader.nextString());
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            if (reader.peek() == JsonToken.END_OBJECT)
                reader.endObject();

        } catch (Exception ignored) {
        }
    }

    public void toJson(JsonWriter writer) {
        try {
            // {
            writer.beginObject();

            // "tree_pollen" : ""
            writer.name("tree_pollen");
            writer.value(treePollenCount.name());

            // "grass_pollen" : ""
            writer.name("grass_pollen");
            writer.value(grassPollenCount.name());

            // "ragweed_pollen" : ""
            writer.name("ragweed_pollen");
            writer.value(ragweedPollenCount.name());

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Pollen: error writing json string");
        }
    }
}
