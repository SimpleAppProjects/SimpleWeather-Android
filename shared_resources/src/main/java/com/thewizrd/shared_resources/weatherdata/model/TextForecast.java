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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import okio.Buffer;

public class TextForecast extends CustomJsonObject {

    @Json(name = "date")
    private ZonedDateTime date;

    @Json(name = "fcttext")
    private String fcttext;

    @Json(name = "fcttext_metric")
    private String fcttextMetric;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public TextForecast() {
        // Needed for deserialization
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getFcttext() {
        return fcttext;
    }

    public void setFcttext(String fcttext) {
        this.fcttext = fcttext;
    }

    public String getFcttextMetric() {
        return fcttextMetric;
    }

    public void setFcttextMetric(String fcttextMetric) {
        this.fcttextMetric = fcttextMetric;
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
                    case "date":
                        this.date = ZonedDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        break;
                    case "fcttext":
                        this.fcttext = reader.nextString();
                        break;
                    case "fcttext_metric":
                        this.fcttextMetric = reader.nextString();
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

    @Override
    public void toJson(@NonNull JsonWriter writer) {
        try {
            // {
            writer.beginObject();

            // "date" : ""
            writer.name("date");
            writer.value(date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            // "fcttext" : ""
            writer.name("fcttext");
            writer.value(fcttext);

            // "fcttext_metric" : ""
            writer.name("fcttext_metric");
            writer.value(fcttextMetric);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "TextForecast: error writing json string");
        }
    }
}