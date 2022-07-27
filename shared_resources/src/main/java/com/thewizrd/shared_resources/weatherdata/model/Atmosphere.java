package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;

import okio.Buffer;

public class Atmosphere extends CustomJsonObject {

    @Json(name = "humidity")
    private Integer humidity;

    @Json(name = "pressure_mb")
    private Float pressureMb;

    @Json(name = "pressure_in")
    private Float pressureIn;

    @Json(name = "pressure_trend")
    private String pressureTrend;

    @Json(name = "visibility_mi")
    private Float visibilityMi;

    @Json(name = "visibility_km")
    private Float visibilityKm;

    @Json(name = "dewpoint_f")
    private Float dewpointF;

    @Json(name = "dewpoint_c")
    private Float dewpointC;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Atmosphere() {
        // Needed for deserialization
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Float getPressureMb() {
        return pressureMb;
    }

    public void setPressureMb(Float pressureMb) {
        this.pressureMb = pressureMb;
    }

    public Float getPressureIn() {
        return pressureIn;
    }

    public void setPressureIn(Float pressureIn) {
        this.pressureIn = pressureIn;
    }

    public String getPressureTrend() {
        return pressureTrend;
    }

    public void setPressureTrend(String pressureTrend) {
        this.pressureTrend = pressureTrend;
    }

    public Float getVisibilityMi() {
        return visibilityMi;
    }

    public void setVisibilityMi(Float visibilityMi) {
        this.visibilityMi = visibilityMi;
    }

    public Float getVisibilityKm() {
        return visibilityKm;
    }

    public void setVisibilityKm(Float visibilityKm) {
        this.visibilityKm = visibilityKm;
    }

    public Float getDewpointF() {
        return dewpointF;
    }

    public void setDewpointF(Float dewpointF) {
        this.dewpointF = dewpointF;
    }

    public Float getDewpointC() {
        return dewpointC;
    }

    public void setDewpointC(Float dewpointC) {
        this.dewpointC = dewpointC;
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
                    case "humidity":
                        this.humidity = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "pressure_mb":
                        this.pressureMb = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "pressure_in":
                        this.pressureIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "pressure_trend":
                        this.pressureTrend = reader.nextString();
                        break;
                    case "visibility_mi":
                        this.visibilityMi = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "visibility_km":
                        this.visibilityKm = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "dewpoint_f":
                        this.dewpointF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "dewpoint_c":
                        this.dewpointC = NumberUtils.tryParseFloat(reader.nextString());
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

            // "humidity" : ""
            writer.name("humidity");
            writer.value(humidity);

            // "pressure_mb" : ""
            writer.name("pressure_mb");
            writer.value(pressureMb);

            // "pressure_in" : ""
            writer.name("pressure_in");
            writer.value(pressureIn);

            // "pressure_trend" : ""
            writer.name("pressure_trend");
            writer.value(pressureTrend);

            // "visibility_mi" : ""
            writer.name("visibility_mi");
            writer.value(visibilityMi);

            // "visibility_km" : ""
            writer.name("visibility_km");
            writer.value(visibilityKm);

            // "dewpoint_f" : ""
            writer.name("dewpoint_f");
            writer.value(dewpointF);

            // "dewpoint_c" : ""
            writer.name("dewpoint_c");
            writer.value(dewpointC);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Atmosphere: error writing json string");
        }
    }
}