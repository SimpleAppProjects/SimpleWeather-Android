package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;

import okio.Buffer;

public class Pollen extends CustomJsonObject {

    public enum PollenCount {
        UNKNOWN,
        LOW,
        MODERATE,
        HIGH,
        VERY_HIGH
    }

    @Json(name = "tree_pollen")
    private PollenCount treePollenCount;

    @Json(name = "grass_pollen")
    private PollenCount grassPollenCount;

    @Json(name = "ragweed_pollen")
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
    public void fromJson(@NonNull JsonReader extReader) {
        try {
            JsonReader reader;
            String jsonValue;

            if (extReader.peek() == JsonReader.Token.STRING) {
                jsonValue = extReader.nextString();
            } else {
                jsonValue = null;
            }

            if (jsonValue == null)
                reader = extReader;
            else {
                reader = JsonReader.of(new Buffer().writeUtf8(jsonValue));
                reader.beginObject(); // StartObject
            }

            while (reader.hasNext() && reader.peek() != JsonReader.Token.END_OBJECT) {
                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonReader.Token.NULL) {
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

            if (reader.peek() == JsonReader.Token.END_OBJECT)
                reader.endObject();

        } catch (Exception ignored) {
        }
    }

    public void toJson(@NonNull JsonWriter writer) {
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
