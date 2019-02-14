package com.thewizrd.shared_resources.weatherdata;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class MoonPhase {

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

    @SerializedName("desc")
    private String desc;

    private MoonPhase() {
        // Needed for deserialization
    }

    public MoonPhase(MoonPhaseType moonPhaseType) {
        this.phase = moonPhaseType;

        Context context = SimpleLibrary.getInstance().getAppContext();

        switch (moonPhaseType) {
            case NEWMOON:
                desc = context.getString(R.string.moonphase_new);
                break;
            case WAXING_CRESCENT:
                desc = context.getString(R.string.moonphase_waxcrescent);
                break;
            case FIRST_QTR:
                desc = context.getString(R.string.moonphase_firstqtr);
                break;
            case WAXING_GIBBOUS:
                desc = context.getString(R.string.moonphase_waxgibbous);
                break;
            case FULL_MOON:
                desc = context.getString(R.string.moonphase_full);
                break;
            case WANING_GIBBOUS:
                desc = context.getString(R.string.moonphase_wangibbous);
                break;
            case LAST_QTR:
                desc = context.getString(R.string.moonphase_lastqtr);
                break;
            case WANING_CRESCENT:
                desc = context.getString(R.string.moonphase_wancrescent);
                break;
        }
    }

    public MoonPhase(MoonPhaseType moonPhaseType, String description) {
        this(moonPhaseType);

        if (!StringUtils.isNullOrWhitespace(description))
            this.desc = description;
    }

    public MoonPhaseType getPhase() {
        return phase;
    }

    public void setPhase(MoonPhaseType phase) {
        this.phase = phase;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String description) {
        this.desc = description;
    }

    public static MoonPhase fromJson(JsonReader extReader) {
        MoonPhase obj = null;

        try {
            obj = new MoonPhase();
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
                        obj.phase = MoonPhaseType.valueOf(reader.nextString());
                        break;
                    case "desc":
                        obj.desc = reader.nextString();
                        break;
                    default:
                        break;
                }
            }

            if (reader.peek() == JsonToken.END_OBJECT)
                reader.endObject();

        } catch (Exception ex) {
            obj = null;
        }

        return obj;
    }

    public String toJson() {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);

        try {
            // {
            writer.beginObject();

            // "phase" : ""
            writer.name("phase");
            writer.value(phase.name());

            // "desc" : ""
            writer.name("desc");
            writer.value(desc);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "MoonPhase: error writing json string");
        }

        return sw.toString();
    }
}
