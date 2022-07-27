package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import okio.Buffer;

public class MinutelyForecast extends CustomJsonObject {
    @Json(name = "date")
    private ZonedDateTime date;

    @Json(name = "rain_mm")
    private Float rainMm;

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public Float getRainMm() {
        return rainMm;
    }

    public void setRainMm(Float rainMm) {
        this.rainMm = rainMm;
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
                    case "rain_mm":
                        this.rainMm = NumberUtils.tryParseFloat(reader.nextString());
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

            // "rain_mm" : ""
            writer.name("rain_mm");
            writer.value(rainMm);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "MinutelyForecast: error writing json string");
        }
    }
}
