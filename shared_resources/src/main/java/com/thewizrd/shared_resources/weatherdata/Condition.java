package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;

public class Condition extends CustomJsonObject {

    @SerializedName("weather")
    private String weather;

    @SerializedName("temp_f")
    private Float tempF;

    @SerializedName("temp_c")
    private Float tempC;

    @SerializedName("wind_degrees")
    private Integer windDegrees;

    @SerializedName("wind_mph")
    private Float windMph;

    @SerializedName("wind_kph")
    private Float windKph;

    @SerializedName("feelslike_f")
    private Float feelslikeF;

    @SerializedName("feelslike_c")
    private Float feelslikeC;

    @SerializedName("icon")
    private String icon;

    @SerializedName("beaufort")
    private Beaufort beaufort;

    @SerializedName("uv")
    private UV uv;

    @SerializedName("high_f")
    private Float highF;

    @SerializedName("high_c")
    private Float highC;

    @SerializedName("low_f")
    private Float lowF;

    @SerializedName("low_c")
    private Float lowC;

    @SerializedName("airQuality")
    private AirQuality airQuality;

    @SerializedName("observation_time")
    private ZonedDateTime observationTime;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Condition() {
        // Needed for deserialization
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.weatheryahoo.CurrentObservation observation) {
        weather = observation.getCondition().getText();
        tempF = NumberUtils.tryParseFloat(observation.getCondition().getTemperature());
        tempC = ConversionMethods.FtoC(tempF);
        windDegrees = NumberUtils.tryParseInt(observation.getWind().getDirection());
        windMph = NumberUtils.tryParseFloat(observation.getWind().getSpeed());
        windKph = ConversionMethods.mphTokph(windMph);
        feelslikeF = NumberUtils.tryParseFloat(observation.getWind().getChill());
        feelslikeC = ConversionMethods.FtoC(feelslikeF);
        icon = WeatherManager.getProvider(WeatherAPI.YAHOO)
                .getWeatherIcon(observation.getCondition().getCode());

        beaufort = new Beaufort(WeatherUtils.getBeaufortScale((int) Math.round(windMph)).getValue());
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.openweather.Current current) {
        weather = StringUtils.toUpperCase(current.getWeather().get(0).getDescription());
        tempF = ConversionMethods.KtoF(current.getTemp());
        tempC = ConversionMethods.KtoC(current.getTemp());
        windDegrees = current.getWindDeg();
        windMph = ConversionMethods.msecToMph(current.getWindSpeed());
        windKph = ConversionMethods.msecToKph(current.getWindSpeed());
        feelslikeF = ConversionMethods.KtoF(current.getFeelsLike());
        feelslikeC = ConversionMethods.KtoC(current.getFeelsLike());

        String ico = current.getWeather().get(0).getIcon();
        String dn = Character.toString(ico.charAt(ico.length() == 0 ? 0 : ico.length() - 1));

        try {
            int x = Integer.parseInt(dn);
            dn = "";
        } catch (NumberFormatException ex) {
            // DO nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(current.getWeather().get(0).getId() + dn);

        uv = new UV(current.getUvi());
        beaufort = new Beaufort(WeatherUtils.getBeaufortScale(current.getWindSpeed()).getValue());

        observationTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(current.getDt()), ZoneOffset.UTC);
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem time) {
        // weather
        tempF = ConversionMethods.CtoF(time.getData().getInstant().getDetails().getAirTemperature());
        tempC = time.getData().getInstant().getDetails().getAirTemperature();
        windDegrees = Math.round(time.getData().getInstant().getDetails().getWindFromDirection());
        windMph = (float) Math.round(ConversionMethods.msecToMph(time.getData().getInstant().getDetails().getWindSpeed()));
        windKph = (float) Math.round(ConversionMethods.msecToKph(time.getData().getInstant().getDetails().getWindSpeed()));
        feelslikeF = WeatherUtils.getFeelsLikeTemp(tempF, windMph, Math.round(time.getData().getInstant().getDetails().getRelativeHumidity()));
        feelslikeC = ConversionMethods.FtoC(feelslikeF);

        if (time.getData().getNext12Hours() != null) {
            icon = time.getData().getNext12Hours().getSummary().getSymbolCode();
        } else if (time.getData().getNext6Hours() != null) {
            icon = time.getData().getNext6Hours().getSummary().getSymbolCode();
        } else if (time.getData().getNext1Hours() != null) {
            icon = time.getData().getNext1Hours().getSummary().getSymbolCode();
        }

        beaufort = new Beaufort(WeatherUtils.getBeaufortScale(time.getData().getInstant().getDetails().getWindSpeed()).getValue());
        if (time.getData().getInstant().getDetails().getUltravioletIndexClearSky() != null) {
            uv = new UV(time.getData().getInstant().getDetails().getUltravioletIndexClearSky());
        }
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.here.ObservationItem observation,
                     com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecastItem) {
        weather = StringUtils.toPascalCase(observation.getDescription());
        Float temp_F = NumberUtils.tryParseFloat(observation.getTemperature());
        if (temp_F != null) {
            tempF = temp_F;
            tempC = ConversionMethods.FtoC(temp_F);
        }

        Float highTempF = NumberUtils.tryParseFloat(observation.getHighTemperature());
        Float lowTempF = NumberUtils.tryParseFloat(observation.getLowTemperature());
        if (highTempF != null && lowTempF != null) {
            this.highF = highTempF;
            this.highC = ConversionMethods.FtoC(highTempF);
            this.lowF = lowTempF;
            this.lowC = ConversionMethods.FtoC(lowTempF);
        } else {
            highTempF = NumberUtils.tryParseFloat(forecastItem.getHighTemperature());
            lowTempF = NumberUtils.tryParseFloat(forecastItem.getLowTemperature());

            if (highTempF != null && lowTempF != null) {
                this.highF = highTempF;
                this.highC = ConversionMethods.FtoC(highTempF);
                this.lowF = lowTempF;
                this.lowC = ConversionMethods.FtoC(lowTempF);
            } else {
                this.highF = 0.00f;
                this.highC = 0.00f;
                this.lowF = 0.00f;
                this.lowC = 0.00f;
            }
        }

        Integer windDeg = NumberUtils.tryParseInt(observation.getWindDirection());
        if (windDeg != null) {
            windDegrees = Integer.parseInt(observation.getWindDirection());
        }

        Float windSpeed = NumberUtils.tryParseFloat(observation.getWindSpeed());
        if (windSpeed != null) {
            windMph = windSpeed;
            windKph = ConversionMethods.mphTokph(windSpeed);
        }

        Float comfortTempF = NumberUtils.tryParseFloat(observation.getComfort());
        if (comfortTempF != null) {
            feelslikeF = comfortTempF;
            feelslikeC = ConversionMethods.FtoC(comfortTempF);
        }

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", observation.getDaylight(), observation.getIconName()));

        Integer scale = NumberUtils.tryParseInt(forecastItem.getBeaufortScale());
        if (scale != null) {
            beaufort = new Beaufort(scale, forecastItem.getBeaufortDescription());
        }

        Float index = NumberUtils.tryParseFloat(forecastItem.getUvIndex());
        if (index != null) {
            uv = new UV(index, forecastItem.getUvDesc());
        }

        observationTime = ZonedDateTime.parse(observation.getUtcTime());
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        weather = obsCurrentResponse.getTextDescription();
        if (obsCurrentResponse.getTemperature().getValue() != null) {
            tempC = obsCurrentResponse.getTemperature().getValue();
            tempF = ConversionMethods.CtoF(tempC);
        }

        if (obsCurrentResponse.getWindDirection().getValue() != null) {
            windDegrees = obsCurrentResponse.getWindDirection().getValue().intValue();
        }

        if (obsCurrentResponse.getWindSpeed().getValue() != null) {
            windKph = obsCurrentResponse.getWindSpeed().getValue();
            windMph = ConversionMethods.kphTomph(windKph);
        }

        if (obsCurrentResponse.getHeatIndex().getValue() != null) {
            feelslikeC = obsCurrentResponse.getHeatIndex().getValue();
            feelslikeF = ConversionMethods.CtoF(feelslikeC);
        } else if (obsCurrentResponse.getWindChill().getValue() != null) {
            feelslikeC = obsCurrentResponse.getWindChill().getValue();
            feelslikeF = ConversionMethods.CtoF(feelslikeC);
        } else if (tempF != null && !ObjectsCompat.equals(tempF, tempC) && windMph != null) {
            float humidity = NumberUtils.getValueOrDefault(obsCurrentResponse.getRelativeHumidity().getValue(), -1.f);
            feelslikeF = WeatherUtils.getFeelsLikeTemp(tempF, windMph, Math.round(humidity));
            feelslikeC = ConversionMethods.FtoC(feelslikeF);
        }

        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(obsCurrentResponse.getIcon());

        if (windMph != null) {
            beaufort = new Beaufort(WeatherUtils.getBeaufortScale((int) Math.round(windMph)).getValue());
        }

        observationTime = ZonedDateTime.parse(obsCurrentResponse.getTimestamp());
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

    public int getWindDegrees() {
        return windDegrees;
    }

    public void setWindDegrees(int windDegrees) {
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

    public ZonedDateTime getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(ZonedDateTime observationTime) {
        this.observationTime = observationTime;
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
                    case "observation_time":
                        observationTime = ZonedDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

            // "observation_time" : ""
            writer.name("observation_time");
            writer.value(observationTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Condition: error writing json string");
        }
    }
}