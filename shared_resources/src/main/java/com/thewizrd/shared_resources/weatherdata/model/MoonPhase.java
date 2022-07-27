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

    @Json(name = "phase")
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
                    case "phase":
                        this.phase = MoonPhaseType.valueOf(reader.nextString());
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
