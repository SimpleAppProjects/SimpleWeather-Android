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

public class MoonPhase extends CustomJsonObject {

    public enum MoonPhaseType {
        NEWMOON,
        WAXING_CRESCENT,
        FIRST_QTR,
        WAXING_GIBBOUS,
        FULL_MOON,
        WANING_GIBBOUS,
        LAST_QTR,
        WANING_CRESCENT,
    }

    @SerializedName("phase")
    private MoonPhaseType phase;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public MoonPhase() {
        // Needed for deserialization
    }

    public MoonPhase(MoonPhaseType moonPhaseType) {
        this.phase = moonPhaseType;
    }

    public MoonPhaseType getPhase() {
        return phase;
    }

    public void setPhase(MoonPhaseType phase) {
        this.phase = phase;
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
                    case "phase":
                        this.phase = MoonPhaseType.valueOf(reader.nextString());
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

            // "phase" : ""
            writer.name("phase");
            writer.value(phase.name());

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "MoonPhase: error writing json string");
        }
    }
}
