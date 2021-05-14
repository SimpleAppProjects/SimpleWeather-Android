package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Forecast extends BaseForecast {

    @SerializedName("date")
    private LocalDateTime date;

    @SerializedName("low_f")
    private Float lowF;

    @SerializedName("low_c")
    private Float lowC;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Forecast() {
        // Needed for deserialization
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Float getLowF() {
        return lowF;
    }

    public void setLowF(Float lowF) {
        this.lowF = lowF;
    }

    public Float getLowC() {
        return lowC;
    }

    public void setLowC(Float lowC) {
        this.lowC = lowC;
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
                    case "date":
                        this.date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(reader.nextString())), ZoneOffset.UTC);
                        break;
                    case "high_f":
                        this.highF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "high_c":
                        this.highC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "low_f":
                        this.lowF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "low_c":
                        this.lowC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "condition":
                        this.condition = reader.nextString();
                        break;
                    case "icon":
                        this.icon = reader.nextString();
                        break;
                    case "extras":
                        this.extras = new ForecastExtras();
                        this.extras.fromJson(reader);
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

            // "date" : ""
            writer.name("date");
            writer.value(date.toInstant(ZoneOffset.UTC).toString());

            // "high_f" : ""
            writer.name("high_f");
            writer.value(highF);

            // "high_c" : ""
            writer.name("high_c");
            writer.value(highC);

            // "low_f" : ""
            writer.name("low_f");
            writer.value(lowF);

            // "low_c" : ""
            writer.name("low_c");
            writer.value(lowC);

            // "condition" : ""
            writer.name("condition");
            writer.value(condition);

            // "icon" : ""
            writer.name("icon");
            writer.value(icon);

            // "extras" : ""
            if (extras != null) {
                writer.name("extras");
                if (extras == null)
                    writer.nullValue();
                else
                    extras.toJson(writer);
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Forecast: error writing json string");
        }
    }
}