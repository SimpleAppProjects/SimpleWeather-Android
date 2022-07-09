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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Astronomy extends CustomJsonObject {

    @SerializedName("sunrise")
    private LocalDateTime sunrise;

    @SerializedName("sunset")
    private LocalDateTime sunset;

    @SerializedName("moonrise")
    private LocalDateTime moonrise;

    @SerializedName("moonset")
    private LocalDateTime moonset;

    @SerializedName("moonphase")
    private MoonPhase moonPhase;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Astronomy() {
        // Needed for deserialization
    }

    public LocalDateTime getSunrise() {
        return sunrise;
    }

    public void setSunrise(LocalDateTime sunrise) {
        this.sunrise = sunrise;
    }

    public LocalDateTime getSunset() {
        return sunset;
    }

    public void setSunset(LocalDateTime sunset) {
        this.sunset = sunset;
    }

    public LocalDateTime getMoonrise() {
        return moonrise;
    }

    public void setMoonrise(LocalDateTime moonrise) {
        this.moonrise = moonrise;
    }

    public LocalDateTime getMoonset() {
        return moonset;
    }

    public void setMoonset(LocalDateTime moonset) {
        this.moonset = moonset;
    }

    public MoonPhase getMoonPhase() {
        return moonPhase;
    }

    public void setMoonPhase(MoonPhase moonPhase) {
        this.moonPhase = moonPhase;
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
                    case "sunrise":
                        this.sunrise = LocalDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        break;
                    case "sunset":
                        this.sunset = LocalDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        break;
                    case "moonrise":
                        this.moonrise = LocalDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        break;
                    case "moonset":
                        this.moonset = LocalDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        break;
                    case "moonphase":
                        this.moonPhase = new MoonPhase();
                        this.moonPhase.fromJson(reader);
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

    @Override
    public void toJson(JsonWriter writer) {
        try {
            // {
            writer.beginObject();

            // "sunrise" : ""
            if (sunrise != null) {
                writer.name("sunrise");
                writer.value(sunrise.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            // "sunset" : ""
            if (sunset != null) {
                writer.name("sunset");
                writer.value(sunset.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            // "moonrise" : ""
            if (moonrise != null) {
                writer.name("moonrise");
                writer.value(moonrise.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            // "moonset" : ""
            if (moonset != null) {
                writer.name("moonset");
                writer.value(moonset.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            // "moonphase" : ""
            if (moonPhase != null) {
                writer.name("moonphase");
                moonPhase.toJson(writer);
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Astronomy: error writing json string");
        }
    }
}