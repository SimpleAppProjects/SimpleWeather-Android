package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.common.collect.Iterables;
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
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

public class Forecast extends CustomJsonObject {

    @SerializedName("date")
    private LocalDateTime date;

    @SerializedName("high_f")
    private Float highF;

    @SerializedName("high_c")
    private Float highC;

    @SerializedName("low_f")
    private Float lowF;

    @SerializedName("low_c")
    private Float lowC;

    @SerializedName("condition")
    private String condition;

    @SerializedName("icon")
    private String icon;

    @SerializedName("extras")
    private ForecastExtras extras;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Forecast() {
        // Needed for deserialization
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatheryahoo.ForecastsItem forecast) {
        date = LocalDateTime.ofEpochSecond(Long.parseLong(forecast.getDate()), 0, ZoneOffset.UTC);
        highF = Float.parseFloat(forecast.getHigh());
        highC = ConversionMethods.FtoC(highF);
        lowF = Float.parseFloat(forecast.getLow());
        lowC = ConversionMethods.FtoC(lowF);
        condition = forecast.getText();
        icon = WeatherManager.getProvider(WeatherAPI.YAHOO)
                .getWeatherIcon(forecast.getCode());
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.openweather.DailyItem forecast) {
        date = LocalDateTime.ofEpochSecond(forecast.getDt(), 0, ZoneOffset.UTC);
        highF = ConversionMethods.KtoF(forecast.getTemp().getMax());
        highC = ConversionMethods.KtoC(forecast.getTemp().getMax());
        lowF = ConversionMethods.KtoF(forecast.getTemp().getMin());
        lowC = ConversionMethods.KtoC(forecast.getTemp().getMin());
        condition = StringUtils.toUpperCase(forecast.getWeather().get(0).getDescription());
        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(Integer.toString(forecast.getWeather().get(0).getId()));

        // Extras
        extras = new ForecastExtras();
        extras.setDewpointF(ConversionMethods.KtoF(forecast.getDewPoint()));
        extras.setDewpointC(ConversionMethods.KtoC(forecast.getDewPoint()));
        extras.setHumidity(forecast.getHumidity());
        extras.setPop(forecast.getClouds());
        // 1hPA = 1mbar
        extras.setPressureMb(forecast.getPressure());
        extras.setPressureIn(ConversionMethods.mbToInHg(forecast.getPressure()));
        extras.setWindDegrees(forecast.getWindDeg());
        extras.setWindMph((float) Math.round(ConversionMethods.msecToMph(forecast.getWindSpeed())));
        extras.setWindKph((float) Math.round(ConversionMethods.msecToKph(forecast.getWindSpeed())));
        extras.setUvIndex(forecast.getUvi());
        if (forecast.getVisibility() != null) {
            extras.setVisibilityKm(forecast.getVisibility().floatValue());
            extras.setVisibilityMi(ConversionMethods.kmToMi(extras.getVisibilityKm()));
        }
        if (forecast.getRain() != null) {
            extras.setQpfRainMm(forecast.getRain());
            extras.setQpfRainIn(ConversionMethods.mmToIn(forecast.getRain()));
        }
        if (forecast.getSnow() != null) {
            extras.setQpfSnowCm(forecast.getSnow() / 10);
            extras.setQpfSnowIn(ConversionMethods.mmToIn(forecast.getSnow()));
        }
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem time) {
        date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTime())), ZoneOffset.UTC);

        if (time.getData().getNext12Hours() != null) {
            icon = time.getData().getNext12Hours().getSummary().getSymbolCode();
        } else if (time.getData().getNext6Hours() != null) {
            icon = time.getData().getNext6Hours().getSummary().getSymbolCode();
        } else if (time.getData().getNext1Hours() != null) {
            icon = time.getData().getNext1Hours().getSummary().getSymbolCode();
        }
        // Don't bother setting other values; they're not available yet
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecast) {
        date = ZonedDateTime.parse(forecast.getUtcTime()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        Float high_f = NumberUtils.tryParseFloat(forecast.getHighTemperature());
        if (high_f != null) {
            highF = high_f;
            highC = ConversionMethods.FtoC(high_f);
        }
        Float low_f = NumberUtils.tryParseFloat(forecast.getLowTemperature());
        if (low_f != null) {
            lowF = low_f;
            lowC = ConversionMethods.FtoC(low_f);
        }
        condition = StringUtils.toPascalCase(forecast.getDescription());
        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", forecast.getDaylight(), forecast.getIconName()));

        // Extras
        extras = new ForecastExtras();
        Float comfortTempF = NumberUtils.tryParseFloat(forecast.getComfort());
        if (comfortTempF != null) {
            extras.setFeelslikeF(comfortTempF);
            extras.setFeelslikeC(ConversionMethods.FtoC(comfortTempF));
        }
        Integer humidity = NumberUtils.tryParseInt(forecast.getHumidity());
        if (humidity != null) {
            extras.setHumidity(humidity);
        }
        Float dewpointF = NumberUtils.tryParseFloat(forecast.getDewPoint());
        if (dewpointF != null) {
            extras.setDewpointF(dewpointF);
            extras.setDewpointC(ConversionMethods.FtoC(dewpointF));
        }
        Integer pop = NumberUtils.tryParseInt(forecast.getPrecipitationProbability());
        if (pop != null) {
            extras.setPop(pop);
        }
        Float rain_in = NumberUtils.tryParseFloat(forecast.getRainFall());
        if (rain_in != null) {
            extras.setQpfRainIn(rain_in);
            extras.setQpfRainMm(ConversionMethods.inToMM(rain_in));
        }
        Float snow_in = NumberUtils.tryParseFloat(forecast.getSnowFall());
        if (snow_in != null) {
            extras.setQpfSnowIn(snow_in);
            extras.setQpfSnowCm(ConversionMethods.inToMM(snow_in) / 10);
        }
        Float pressureIN = NumberUtils.tryParseFloat(forecast.getBarometerPressure());
        if (pressureIN != null) {
            extras.setPressureIn(pressureIN);
            extras.setPressureMb(ConversionMethods.inHgToMB(pressureIN));
        }
        Integer windDegrees = NumberUtils.tryParseInt(forecast.getWindDirection());
        if (windDegrees != null) {
            extras.setWindDegrees(windDegrees);
        }
        Float windSpeed = NumberUtils.tryParseFloat(forecast.getWindSpeed());
        if (windSpeed != null) {
            extras.setWindMph(windSpeed);
            extras.setWindKph(ConversionMethods.mphTokph(windSpeed));
        }
        Float uv_index = NumberUtils.tryParseFloat(forecast.getUvIndex());
        if (uv_index != null) {
            extras.setUvIndex(uv_index);
        }
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem) {
        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        if (forecastItem.getIsDaytime()) {
            highF = (float) forecastItem.getTemperature();
            highC = ConversionMethods.FtoC(highF);
        } else {
            lowF = (float) forecastItem.getTemperature();
            lowC = ConversionMethods.FtoC(lowF);
        }
        condition = forecastItem.getShortForecast();
        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(forecastItem.getIcon());

        if (forecastItem.getWindSpeed() != null && forecastItem.getWindDirection() != null) {
            String[] speeds = forecastItem.getWindSpeed().replace(" mph", "").split(" to ");
            String maxWindSpeed = Iterables.getLast(Arrays.asList(speeds), null);
            if (!StringUtils.isNullOrWhitespace(maxWindSpeed)) {
                Integer windSpeed = NumberUtils.tryParseInt(maxWindSpeed);
                if (windSpeed != null) {
                    extras = new ForecastExtras();
                    extras.setWindDegrees(WeatherUtils.getWindDirection(forecastItem.getWindDirection()));
                    extras.setWindMph(windSpeed.floatValue());
                    extras.setWindKph(ConversionMethods.mphTokph(windSpeed.floatValue()));
                }
            }
        }
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem, com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem nightForecastItem) {
        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        highF = (float) forecastItem.getTemperature();
        highC = ConversionMethods.FtoC(highF);
        lowF = (float) nightForecastItem.getTemperature();
        lowC = ConversionMethods.FtoC(lowF);
        condition = forecastItem.getShortForecast();
        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(forecastItem.getIcon());

        if (forecastItem.getWindSpeed() != null && forecastItem.getWindDirection() != null) {
            // windSpeed is reported usually as, for ex., '7 to 10 mph'
            // Format and split text into min and max
            String[] speeds = forecastItem.getWindSpeed().replace(" mph", "").split(" to ");
            String maxWindSpeed = Iterables.getLast(Arrays.asList(speeds), null);
            if (!StringUtils.isNullOrWhitespace(maxWindSpeed)) {
                Integer windSpeed = NumberUtils.tryParseInt(maxWindSpeed);
                if (windSpeed != null) {
                    // Extras
                    extras = new ForecastExtras();
                    extras.setWindDegrees(WeatherUtils.getWindDirection(forecastItem.getWindDirection()));
                    extras.setWindMph(windSpeed.floatValue());
                    extras.setWindKph(ConversionMethods.mphTokph(windSpeed.floatValue()));
                }
            }
        }
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public ForecastExtras getExtras() {
        return extras;
    }

    public void setExtras(ForecastExtras extras) {
        this.extras = extras;
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