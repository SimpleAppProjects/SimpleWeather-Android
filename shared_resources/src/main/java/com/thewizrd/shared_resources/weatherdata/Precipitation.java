package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.io.StringReader;

public class Precipitation extends CustomJsonObject {

    @SerializedName("pop")
    private Integer pop;

    @SerializedName("cloudiness")
    private Integer cloudiness;

    @SerializedName("qpf_rain_in")
    private Float qpfRainIn;

    @SerializedName("qpf_rain_mm")
    private Float qpfRainMm;

    @SerializedName("qpf_snow_in")
    private Float qpfSnowIn;

    @SerializedName("qpf_snow_cm")
    private Float qpfSnowCm;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Precipitation() {
        // Needed for deserialization
    }

    public Integer getPop() {
        return pop;
    }

    public void setPop(Integer pop) {
        this.pop = pop;
    }

    public Integer getCloudiness() {
        return cloudiness;
    }

    public void setCloudiness(Integer cloudiness) {
        this.cloudiness = cloudiness;
    }

    public Float getQpfRainIn() {
        return qpfRainIn;
    }

    public void setQpfRainIn(Float qpfRainIn) {
        this.qpfRainIn = qpfRainIn;
    }

    public Float getQpfRainMm() {
        return qpfRainMm;
    }

    public void setQpfRainMm(Float qpfRainMm) {
        this.qpfRainMm = qpfRainMm;
    }

    public Float getQpfSnowIn() {
        return qpfSnowIn;
    }

    public void setQpfSnowIn(Float qpfSnowIn) {
        this.qpfSnowIn = qpfSnowIn;
    }

    public Float getQpfSnowCm() {
        return qpfSnowCm;
    }

    public void setQpfSnowCm(Float qpfSnowCm) {
        this.qpfSnowCm = qpfSnowCm;
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
                    case "pop":
                        this.pop = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "cloudiness":
                        this.cloudiness = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "qpf_rain_in":
                        this.qpfRainIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "qpf_rain_mm":
                        this.qpfRainMm = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "qpf_snow_in":
                        this.qpfSnowIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "qpf_snow_cm":
                        this.qpfSnowCm = NumberUtils.tryParseFloat(reader.nextString());
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

            // "pop" : ""
            writer.name("pop");
            writer.value(pop);

            // "cloudiness" : ""
            writer.name("cloudiness");
            writer.value(cloudiness);

            // "qpf_rain_in" : ""
            writer.name("qpf_rain_in");
            writer.value(qpfRainIn);

            // "qpf_rain_mm" : ""
            writer.name("qpf_rain_mm");
            writer.value(qpfRainMm);

            // "qpf_snow_in" : ""
            writer.name("qpf_snow_in");
            writer.value(qpfSnowIn);

            // "qpf_snow_cm" : ""
            writer.name("qpf_snow_cm");
            writer.value(qpfSnowCm);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Precipitation: error writing json string");
        }
    }
}