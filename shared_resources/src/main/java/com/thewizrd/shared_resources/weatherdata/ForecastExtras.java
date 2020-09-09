package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.io.StringReader;

public class ForecastExtras extends CustomJsonObject {

    @SerializedName("feelslike_f")
    private Float feelslikeF;

    @SerializedName("feelslike_c")
    private Float feelslikeC;

    @SerializedName("humidity")
    private Integer humidity;

    @SerializedName("dewpoint_f")
    private Float dewpointF;

    @SerializedName("dewpoint_c")
    private Float dewpointC;

    @SerializedName("uv_index")
    private Float uvIndex;

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

    @SerializedName("pressure_mb")
    private Float pressureMb;

    @SerializedName("pressure_in")
    private Float pressureIn;

    @SerializedName("wind_degrees")
    private Integer windDegrees;

    @SerializedName("wind_mph")
    private Float windMph;

    @SerializedName("wind_kph")
    private Float windKph;

    @SerializedName("visibility_mi")
    private Float visibilityMi;

    @SerializedName("visibility_km")
    private Float visibilityKm;

    @SerializedName("windgust_mph")
    private Float windGustMph;

    @SerializedName("windgust_kph")
    private Float windGustKph;

    public Float getFeelslikeF() {
        return feelslikeF;
    }

    public void setFeelslikeF(Float feelslikeF) {
        this.feelslikeF = feelslikeF;
    }

    public Float getFeelslikeC() {
        return feelslikeC;
    }

    public void setFeelslikeC(Float feelslikeC) {
        this.feelslikeC = feelslikeC;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
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

    public Float getUvIndex() {
        return uvIndex;
    }

    public void setUvIndex(Float uvIndex) {
        this.uvIndex = uvIndex;
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

    public Integer getWindDegrees() {
        return windDegrees;
    }

    public void setWindDegrees(Integer windDegrees) {
        this.windDegrees = windDegrees;
    }

    public Float getWindMph() {
        return windMph;
    }

    public void setWindMph(Float windMph) {
        this.windMph = windMph;
    }

    public Float getWindKph() {
        return windKph;
    }

    public void setWindKph(Float windKph) {
        this.windKph = windKph;
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

    public Float getWindGustMph() {
        return windGustMph;
    }

    public void setWindGustMph(Float windGustMph) {
        this.windGustMph = windGustMph;
    }

    public Float getWindGustKph() {
        return windGustKph;
    }

    public void setWindGustKph(Float windGustKph) {
        this.windGustKph = windGustKph;
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
                    case "feelslike_f":
                        this.feelslikeF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "feelslike_c":
                        this.feelslikeC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "humidity":
                        this.humidity = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "dewpoint_f":
                        this.dewpointF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "dewpoint_c":
                        this.dewpointC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "uv_index":
                        this.uvIndex = NumberUtils.tryParseFloat(reader.nextString());
                        break;
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
                    case "pressure_mb":
                        this.pressureMb = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "pressure_in":
                        this.pressureIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "wind_degrees":
                        this.windDegrees = Integer.parseInt(reader.nextString());
                        break;
                    case "wind_mph":
                        this.windMph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "wind_kph":
                        this.windKph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "visibility_mi":
                        this.visibilityMi = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "visibility_km":
                        this.visibilityKm = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "windgust_mph":
                        this.windGustMph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "windgust_kph":
                        this.windGustKph = NumberUtils.tryParseFloat(reader.nextString());
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

    public void toJson(JsonWriter writer) {
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

            // "windgust_mph" : ""
            writer.name("windgust_mph");
            writer.value(windGustMph);

            // "windgust_kph" : ""
            writer.name("windgust_kph");
            writer.value(windGustKph);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "ForecastExtras: error writing json string");
        }
    }
}