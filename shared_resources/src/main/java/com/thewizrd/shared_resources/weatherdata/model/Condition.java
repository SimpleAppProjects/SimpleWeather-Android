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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import okio.Buffer;

public class Condition extends CustomJsonObject {

    @Json(name = "weather")
    private String weather;

    @Json(name = "temp_f")
    private Float tempF;

    @Json(name = "temp_c")
    private Float tempC;

    @Json(name = "wind_degrees")
    private Integer windDegrees;

    @Json(name = "wind_mph")
    private Float windMph;

    @Json(name = "wind_kph")
    private Float windKph;

    @Json(name = "windgust_mph")
    private Float windGustMph;

    @Json(name = "windgust_kph")
    private Float windGustKph;

    @Json(name = "feelslike_f")
    private Float feelslikeF;

    @Json(name = "feelslike_c")
    private Float feelslikeC;

    @Json(name = "icon")
    private String icon;

    @Json(name = "beaufort")
    private Beaufort beaufort;

    @Json(name = "uv")
    private UV uv;

    @Json(name = "high_f")
    private Float highF;

    @Json(name = "high_c")
    private Float highC;

    @Json(name = "low_f")
    private Float lowF;

    @Json(name = "low_c")
    private Float lowC;

    @Json(name = "airQuality")
    private AirQuality airQuality;

    @Json(name = "pollen")
    private Pollen pollen;

    @Json(name = "observation_time")
    private ZonedDateTime observationTime;

    @Json(name = "summary")
    private String summary;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Condition() {
        // Needed for deserialization
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public Float getTempF() {
        return tempF;
    }

    public void setTempF(Float tempF) {
        this.tempF = tempF;
    }

    public Float getTempC() {
        return tempC;
    }

    public void setTempC(Float tempC) {
        this.tempC = tempC;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Beaufort getBeaufort() {
        return beaufort;
    }

    public void setBeaufort(Beaufort beaufort) {
        this.beaufort = beaufort;
    }

    public UV getUv() {
        return uv;
    }

    public void setUv(UV uv) {
        this.uv = uv;
    }

    public Float getHighF() {
        return highF;
    }

    public void setHighF(Float highF) {
        this.highF = highF;
    }

    public Float getHighC() {
        return highC;
    }

    public void setHighC(Float highC) {
        this.highC = highC;
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

    public AirQuality getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(AirQuality airQuality) {
        this.airQuality = airQuality;
    }

    public Pollen getPollen() {
        return pollen;
    }

    public void setPollen(Pollen pollen) {
        this.pollen = pollen;
    }

    public ZonedDateTime getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(ZonedDateTime observationTime) {
        this.observationTime = observationTime;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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
                    case "weather":
                        this.weather = reader.nextString();
                        break;
                    case "temp_f":
                        this.tempF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "temp_c":
                        this.tempC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "wind_degrees":
                        this.windDegrees = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "wind_mph":
                        this.windMph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "wind_kph":
                        this.windKph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "windgust_mph":
                        this.windGustMph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "windgust_kph":
                        this.windGustKph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "feelslike_f":
                        this.feelslikeF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "feelslike_c":
                        this.feelslikeC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "icon":
                        this.icon = reader.nextString();
                        break;
                    case "beaufort":
                        this.beaufort = new Beaufort();
                        this.beaufort.fromJson(reader);
                        break;
                    case "uv":
                        this.uv = new UV();
                        this.uv.fromJson(reader);
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
                    case "airQuality":
                        this.airQuality = new AirQuality();
                        this.airQuality.fromJson(reader);
                        break;
                    case "pollen":
                        this.pollen = new Pollen();
                        this.pollen.fromJson(reader);
                        break;
                    case "observation_time":
                        observationTime = ZonedDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        break;
                    case "summary":
                        this.summary = reader.nextString();
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

            // "weather" : ""
            writer.name("weather");
            writer.value(weather);

            // "temp_f" : ""
            writer.name("temp_f");
            writer.value(tempF);

            // "temp_c" : ""
            writer.name("temp_c");
            writer.value(tempC);

            // "wind_degrees" : ""
            writer.name("wind_degrees");
            writer.value(windDegrees);

            // "wind_mph" : ""
            writer.name("wind_mph");
            writer.value(windMph);

            // "wind_kph" : ""
            writer.name("wind_kph");
            writer.value(windKph);

            // "windgust_mph" : ""
            writer.name("windgust_mph");
            writer.value(windGustMph);

            // "windgust_kph" : ""
            writer.name("windgust_kph");
            writer.value(windGustKph);

            // "feelslike_f" : ""
            writer.name("feelslike_f");
            writer.value(feelslikeF);

            // "feelslike_c" : ""
            writer.name("feelslike_c");
            writer.value(feelslikeC);

            // "icon" : ""
            writer.name("icon");
            writer.value(icon);

            // "beaufort" : ""
            if (beaufort != null) {
                writer.name("beaufort");
                if (beaufort == null)
                    writer.nullValue();
                else
                    beaufort.toJson(writer);
            }

            // "uv" : ""
            if (uv != null) {
                writer.name("uv");
                if (uv == null)
                    writer.nullValue();
                else
                    uv.toJson(writer);
            }

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

            // "airQuality" : ""
            if (airQuality != null) {
                writer.name("airQuality");
                if (airQuality == null)
                    writer.nullValue();
                else
                    airQuality.toJson(writer);
            }

            // "pollen" : ""
            if (pollen != null) {
                writer.name("pollen");
                if (pollen == null)
                    writer.nullValue();
                else
                    pollen.toJson(writer);
            }

            // "observation_time" : ""
            writer.name("observation_time");
            writer.value(observationTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            // "summary" : ""
            writer.name("summary");
            writer.value(summary);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Condition: error writing json string");
        }
    }
}