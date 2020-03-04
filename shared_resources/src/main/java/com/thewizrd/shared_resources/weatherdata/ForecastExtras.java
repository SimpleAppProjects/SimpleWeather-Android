package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class ForecastExtras {

    @SerializedName("feelslike_f")
    private double feelslikeF;

    @SerializedName("feelslike_c")
    private double feelslikeC;

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("dewpoint_f")
    private String dewpointF;

    @SerializedName("dewpoint_c")
    private String dewpointC;

    @SerializedName("uv_index")
    private float uvIndex = -1.0f;

    @SerializedName("pop")
    private String pop;

    @SerializedName("qpf_rain_in")
    private float qpfRainIn = -1.0f;

    @SerializedName("qpf_rain_mm")
    private float qpfRainMm = -1.0f;

    @SerializedName("qpf_snow_in")
    private float qpfSnowIn = -1.0f;

    @SerializedName("qpf_snow_cm")
    private float qpfSnowCm = -1.0f;

    @SerializedName("pressure_mb")
    private String pressureMb;

    @SerializedName("pressure_in")
    private String pressureIn;

    @SerializedName("wind_degrees")
    private int windDegrees;

    @SerializedName("wind_mph")
    private float windMph = -1.0f;

    @SerializedName("wind_kph")
    private float windKph = -1.0f;

    @SerializedName("visibility_mi")
    private String visibilityMi;

    @SerializedName("visibility_km")
    private String visibilityKm;

    public double getFeelslikeF() {
        return feelslikeF;
    }

    public void setFeelslikeF(double feelslikeF) {
        this.feelslikeF = feelslikeF;
    }

    public double getFeelslikeC() {
        return feelslikeC;
    }

    public void setFeelslikeC(double feelslikeC) {
        this.feelslikeC = feelslikeC;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getDewpointF() {
        return dewpointF;
    }

    public void setDewpointF(String dewpointF) {
        this.dewpointF = dewpointF;
    }

    public String getDewpointC() {
        return dewpointC;
    }

    public void setDewpointC(String dewpointC) {
        this.dewpointC = dewpointC;
    }

    public float getUvIndex() {
        return uvIndex;
    }

    public void setUvIndex(float uvIndex) {
        this.uvIndex = uvIndex;
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

    public String getPressureMb() {
        return pressureMb;
    }

    public void setPressureMb(String pressureMb) {
        this.pressureMb = pressureMb;
    }

    public String getPressureIn() {
        return pressureIn;
    }

    public void setPressureIn(String pressureIn) {
        this.pressureIn = pressureIn;
    }

    public int getWindDegrees() {
        return windDegrees;
    }

    public void setWindDegrees(int windDegrees) {
        this.windDegrees = windDegrees;
    }

    public float getWindMph() {
        return windMph;
    }

    public void setWindMph(float windMph) {
        this.windMph = windMph;
    }

    public float getWindKph() {
        return windKph;
    }

    public void setWindKph(float windKph) {
        this.windKph = windKph;
    }

    public String getVisibilityMi() {
        return visibilityMi;
    }

    public void setVisibilityMi(String visibilityMi) {
        this.visibilityMi = visibilityMi;
    }

    public String getVisibilityKm() {
        return visibilityKm;
    }

    public void setVisibilityKm(String visibilityKm) {
        this.visibilityKm = visibilityKm;
    }

    public static ForecastExtras fromJson(JsonReader extReader) {
        ForecastExtras obj = null;

        try {
            obj = new ForecastExtras();
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
                    case "feelslike_f":
                        obj.feelslikeF = Float.parseFloat(reader.nextString());
                        break;
                    case "feelslike_c":
                        obj.feelslikeC = Float.parseFloat(reader.nextString());
                        break;
                    case "humidity":
                        obj.humidity = reader.nextString();
                        break;
                    case "dewpoint_f":
                        obj.dewpointF = reader.nextString();
                        break;
                    case "dewpoint_c":
                        obj.dewpointC = reader.nextString();
                        break;
                    case "uv_index":
                        obj.uvIndex = Float.parseFloat(reader.nextString());
                        break;
                    case "pop":
                        obj.pop = reader.nextString();
                        break;
                    case "qpf_rain_in":
                        obj.qpfRainIn = Float.parseFloat(reader.nextString());
                        break;
                    case "qpf_rain_mm":
                        obj.qpfRainMm = Float.parseFloat(reader.nextString());
                        break;
                    case "qpf_snow_in":
                        obj.qpfSnowIn = Float.parseFloat(reader.nextString());
                        break;
                    case "qpf_snow_cm":
                        obj.qpfSnowCm = Float.parseFloat(reader.nextString());
                        break;
                    case "pressure_mb":
                        obj.pressureMb = reader.nextString();
                        break;
                    case "pressure_in":
                        obj.pressureIn = reader.nextString();
                        break;
                    case "wind_degrees":
                        obj.windDegrees = Integer.parseInt(reader.nextString());
                        break;
                    case "wind_mph":
                        obj.windMph = Float.parseFloat(reader.nextString());
                        break;
                    case "wind_kph":
                        obj.windKph = Float.parseFloat(reader.nextString());
                        break;
                    case "visibility_mi":
                        obj.visibilityMi = reader.nextString();
                        break;
                    case "visibility_km":
                        obj.visibilityKm = reader.nextString();
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

            // "feelslike_f" : ""
            writer.name("feelslike_f");
            writer.value(feelslikeF);

            // "feelslike_c" : ""
            writer.name("feelslike_c");
            writer.value(feelslikeC);

            // "humidity" : ""
            writer.name("humidity");
            writer.value(humidity);

            // "dewpoint_f" : ""
            writer.name("dewpoint_f");
            writer.value(dewpointF);

            // "dewpoint_c" : ""
            writer.name("dewpoint_c");
            writer.value(dewpointC);

            // "uv_index" : ""
            writer.name("uv_index");
            writer.value(uvIndex);

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

            // "pressure_mb" : ""
            writer.name("pressure_mb");
            writer.value(pressureMb);

            // "pressure_in" : ""
            writer.name("pressure_in");
            writer.value(pressureIn);

            // "wind_degrees" : ""
            writer.name("wind_degrees");
            writer.value(windDegrees);

            // "wind_mph" : ""
            writer.name("wind_mph");
            writer.value(windMph);

            // "wind_kph" : ""
            writer.name("wind_kph");
            writer.value(windKph);

            // "visibility_mi" : ""
            writer.name("visibility_mi");
            writer.value(visibilityMi);

            // "visibility_km" : ""
            writer.name("visibility_km");
            writer.value(visibilityKm);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "ForecastExtras: error writing json string");
        }

        return sw.toString();
    }
}