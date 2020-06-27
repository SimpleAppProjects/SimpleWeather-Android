package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;
import java.io.StringReader;

public class Precipitation extends CustomJsonObject {

    @SerializedName("pop")
    private String pop;

    @SerializedName("qpf_rain_in")
    private float qpfRainIn;

    @SerializedName("qpf_rain_mm")
    private float qpfRainMm;

    @SerializedName("qpf_snow_in")
    private float qpfSnowIn;

    @SerializedName("qpf_snow_cm")
    private float qpfSnowCm;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Precipitation() {
        // Needed for deserialization
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.openweather.Current current) {
        // Use cloudiness value here
        pop = Integer.toString(current.getClouds());
        if (current.getRain() != null) {
            qpfRainIn = Float.parseFloat(ConversionMethods.mmToIn(Float.toString(current.getRain().get_1h())));
            qpfRainMm = current.getRain().get_1h();
        }
        if (current.getSnow() != null) {
            qpfSnowIn = Float.parseFloat(ConversionMethods.mmToIn(Float.toString(current.getSnow().get_1h())));
            qpfSnowCm = current.getSnow().get_1h() / 10;
        }
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata.Time time) {
        // Use cloudiness value here
        pop = Integer.toString(Math.round(time.getLocation().getCloudiness().getPercent().floatValue()));
        // The rest DNE
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecast) {
        pop = forecast.getPrecipitationProbability();

        try {
            qpfRainIn = Float.parseFloat(forecast.getRainFall());
            qpfRainMm = Float.parseFloat(ConversionMethods.inToMM(Float.toString(qpfRainIn)));
        } catch (NumberFormatException ex) {
            qpfRainIn = 0.00f;
            qpfRainMm = 0.00f;
        }

        try {
            qpfSnowIn = Float.parseFloat(forecast.getSnowFall());
            qpfSnowCm = Float.parseFloat(ConversionMethods.inToMM(Float.toString(qpfSnowIn))) / 10;
        } catch (NumberFormatException ex) {
            qpfSnowIn = 0.00f;
            qpfSnowCm = 0.00f;
        }
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        if (obsCurrentResponse.getPrecipitationLastHour().getValue() != null) {
            // "unit:m"
            qpfRainMm = obsCurrentResponse.getPrecipitationLastHour().getValue() * 1000;
            qpfRainIn = Float.parseFloat(ConversionMethods.mmToIn(Float.toString(qpfRainMm)));
        } else if (obsCurrentResponse.getPrecipitationLast3Hours().getValue() != null) {
            qpfRainMm = obsCurrentResponse.getPrecipitationLast3Hours().getValue() * 1000;
            qpfRainIn = Float.parseFloat(ConversionMethods.mmToIn(Float.toString(qpfRainMm)));
        }
        // The rest DNE
    }

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public float getQpfRainIn() {
        return qpfRainIn;
    }

    public void setQpfRainIn(float qpfRainIn) {
        this.qpfRainIn = qpfRainIn;
    }

    public float getQpfRainMm() {
        return qpfRainMm;
    }

    public void setQpfRainMm(float qpfRainMm) {
        this.qpfRainMm = qpfRainMm;
    }

    public float getQpfSnowIn() {
        return qpfSnowIn;
    }

    public void setQpfSnowIn(float qpfSnowIn) {
        this.qpfSnowIn = qpfSnowIn;
    }

    public float getQpfSnowCm() {
        return qpfSnowCm;
    }

    public void setQpfSnowCm(float qpfSnowCm) {
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
                        this.pop = reader.nextString();
                        break;
                    case "qpf_rain_in":
                        this.qpfRainIn = Float.parseFloat(reader.nextString());
                        break;
                    case "qpf_rain_mm":
                        this.qpfRainMm = Float.parseFloat(reader.nextString());
                        break;
                    case "qpf_snow_in":
                        this.qpfSnowIn = Float.parseFloat(reader.nextString());
                        break;
                    case "qpf_snow_cm":
                        this.qpfSnowCm = Float.parseFloat(reader.nextString());
                        break;
                    default:
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