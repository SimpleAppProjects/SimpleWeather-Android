package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

public class Forecast extends BaseForecast {

    @SerializedName("date")
    private LocalDateTime date;

    @SerializedName("low_f")
    private Float lowF;

    @SerializedName("low_c")
    private Float lowC;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Forecast() {
        // Needed for deserialization
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatheryahoo.ForecastsItem forecast) {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.YAHOO);
        Locale locale = LocaleUtils.getLocale();

        if (locale.toString().equals("en") || locale.toString().startsWith("en_") || locale.equals(Locale.ROOT)) {
            condition = forecast.getText();
        } else {
            condition = provider.getWeatherCondition(Integer.toString(forecast.getCode()));
        }
        icon = provider.getWeatherIcon(Integer.toString(forecast.getCode()));

        date = LocalDateTime.ofEpochSecond(forecast.getDate(), 0, ZoneOffset.UTC);
        highF = (float) forecast.getHigh();
        highC = ConversionMethods.FtoC(highF);
        lowF = (float) forecast.getLow();
        lowC = ConversionMethods.FtoC(lowF);
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.openweather.ListItem forecast) {
        date = LocalDateTime.ofEpochSecond(forecast.getDt(), 0, ZoneOffset.UTC);
        highF = ConversionMethods.KtoF(forecast.getMain().getTempMax());
        highC = ConversionMethods.KtoC(forecast.getMain().getTempMax());
        lowF = ConversionMethods.KtoF(forecast.getMain().getTempMin());
        lowC = ConversionMethods.KtoC(forecast.getMain().getTempMin());
        condition = StringUtils.toUpperCase(forecast.getWeather().get(0).getDescription());
        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(Integer.toString(forecast.getWeather().get(0).getId()));

        // Extras
        extras = new ForecastExtras();
        extras.setHumidity(forecast.getMain().getHumidity());
        extras.setCloudiness(forecast.getClouds().getAll());
        // 1hPA = 1mbar
        extras.setPressureMb(forecast.getMain().getPressure());
        extras.setPressureIn(ConversionMethods.mbToInHg(forecast.getMain().getPressure()));
        extras.setWindDegrees(Math.round(forecast.getWind().getDeg()));
        extras.setWindMph((float) Math.round(ConversionMethods.msecToMph(forecast.getWind().getSpeed())));
        extras.setWindKph((float) Math.round(ConversionMethods.msecToKph(forecast.getWind().getSpeed())));
        float temp_c = ConversionMethods.KtoC(forecast.getMain().getTemp());
        if (temp_c > 0 && temp_c < 60 && forecast.getMain().getHumidity() > 1) {
            extras.setDewpointC((float) Math.round(WeatherUtils.calculateDewpointC(temp_c, forecast.getMain().getHumidity())));
            extras.setDewpointF((float) Math.round(ConversionMethods.CtoF(extras.getDewpointC())));
        }
        if (forecast.getMain().getFeelsLike() != null) {
            extras.setFeelslikeF(ConversionMethods.KtoF(forecast.getMain().getFeelsLike()));
            extras.setFeelslikeC(ConversionMethods.KtoC(forecast.getMain().getFeelsLike()));
        }
        if (forecast.getPop() != null) {
            extras.setPop(Math.round(forecast.getPop() * 100));
        }
        if (forecast.getVisibility() != null) {
            extras.setVisibilityKm(forecast.getVisibility().floatValue() / 1000);
            extras.setVisibilityMi(ConversionMethods.kmToMi(extras.getVisibilityKm()));
        }
        if (forecast.getWind().getGust() != null) {
            extras.setWindGustMph((float) Math.round(ConversionMethods.msecToMph(forecast.getWind().getGust())));
            extras.setWindGustKph((float) Math.round(ConversionMethods.msecToKph(forecast.getWind().getGust())));
        }
        if (forecast.getRain() != null && forecast.getRain().get_3h() != null) {
            extras.setQpfRainMm(forecast.getRain().get_3h());
            extras.setQpfRainIn(ConversionMethods.mmToIn(forecast.getRain().get_3h()));
        }
        if (forecast.getSnow() != null && forecast.getSnow().get_3h() != null) {
            extras.setQpfSnowCm(forecast.getSnow().get_3h() / 10);
            extras.setQpfSnowIn(ConversionMethods.mmToIn(forecast.getSnow().get_3h()));
        }
    }

    /* OpenWeather OneCall
    public Forecast(com.thewizrd.shared_resources.weatherdata.openweather.onecall.DailyItem forecast) {
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
        if (forecast.getPop() != null) {
            extras.setPop(Math.round(forecast.getPop() * 100));
        }
        extras.setCloudiness(forecast.getClouds());
        // 1hPA = 1mbar
        extras.setPressureMb(forecast.getPressure());
        extras.setPressureIn(ConversionMethods.mbToInHg(forecast.getPressure()));
        extras.setWindDegrees(forecast.getWindDeg());
        extras.setWindMph((float) Math.round(ConversionMethods.msecToMph(forecast.getWindSpeed())));
        extras.setWindKph((float) Math.round(ConversionMethods.msecToKph(forecast.getWindSpeed())));
        extras.setUvIndex(forecast.getUvi());
        if (forecast.getVisibility() != null) {
            extras.setVisibilityKm(forecast.getVisibility().floatValue() / 1000);
            extras.setVisibilityMi(ConversionMethods.kmToMi(extras.getVisibilityKm()));
        }
        if (forecast.getWindGust() != null) {
            extras.setWindGustMph((float) Math.round(ConversionMethods.msecToMph(forecast.getWindGust())));
            extras.setWindGustKph((float) Math.round(ConversionMethods.msecToKph(forecast.getWindGust())));
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
     */

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

    public Forecast(com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem forecastItem) {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.NWS);
        Locale locale = LocaleUtils.getLocale();

        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        if (forecastItem.getIsDaytime()) {
            highF = Float.parseFloat(forecastItem.getTemperature());
            highC = ConversionMethods.FtoC(highF);
        } else {
            lowF = Float.parseFloat(forecastItem.getTemperature());
            lowC = ConversionMethods.FtoC(lowF);
        }

        if (locale.toString().equals("en") || locale.toString().startsWith("en_") || locale.equals(Locale.ROOT)) {
            condition = forecastItem.getShortForecast();
        } else {
            condition = provider.getWeatherCondition(forecastItem.getIcon());
        }
        icon = provider.getWeatherIcon(!forecastItem.getIsDaytime(), forecastItem.getIcon());

        extras = new ForecastExtras();
        extras.setPop(NumberUtils.tryParseInt(forecastItem.getPop(), 0));
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem forecastItem, com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem nightForecastItem) {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.NWS);
        Locale locale = LocaleUtils.getLocale();

        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        highF = Float.parseFloat(forecastItem.getTemperature());
        highC = ConversionMethods.FtoC(highF);
        lowF = Float.parseFloat(nightForecastItem.getTemperature());
        lowC = ConversionMethods.FtoC(lowF);

        if (locale.toString().equals("en") || locale.toString().startsWith("en_") || locale.equals(Locale.ROOT)) {
            condition = forecastItem.getShortForecast();
        } else {
            condition = provider.getWeatherCondition(forecastItem.getIcon());
        }
        icon = provider.getWeatherIcon(false, forecastItem.getIcon());

        extras = new ForecastExtras();
        extras.setPop(NumberUtils.tryParseInt(forecastItem.getPop(), 0));
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatherunlocked.DaysItem day) {
        date = LocalDate.parse(day.getDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)).atStartOfDay();
        highF = day.getTempMaxF();
        highC = day.getTempMaxC();
        lowF = day.getTempMinF();
        lowC = day.getTempMinC();

        //condition = null;
        //icon = null;

        // Extras
        extras = new ForecastExtras();
        extras.setHumidity((int) Math.round((day.getHumidMinPct() + day.getHumidMaxPct()) / 2));
        extras.setPressureMb((float) Math.round((day.getSlpMinMb() + day.getSlpMaxMb()) / 2));
        extras.setPressureIn((float) Math.round((day.getSlpMinIn() + day.getSlpMaxIn()) / 2));
        if (day.getWindspdMaxMph() > 0 && day.getHumidMaxPct() > 0) {
            extras.setFeelslikeF(WeatherUtils.getFeelsLikeTemp(highF, (float) day.getWindspdMaxMph(), (int) Math.round(day.getHumidMaxPct())));
            extras.setFeelslikeC(ConversionMethods.FtoC(extras.getFeelslikeF()));
        }
        if (highC > 0 && highC < 60 && day.getHumidMaxPct() > 1) {
            extras.setDewpointC((float) Math.round(WeatherUtils.calculateDewpointC(highC, (int) Math.round(day.getHumidMaxPct()))));
            extras.setDewpointF((float) Math.round(ConversionMethods.CtoF(extras.getDewpointC())));
        }
        extras.setWindMph((float) Math.round(day.getWindspdMaxMph()));
        extras.setWindKph((float) Math.round(day.getWindspdMaxKmh()));
        extras.setPop((int) Math.round(day.getProbPrecipPct()));
        extras.setWindGustMph((float) Math.round(day.getWindgstMaxMph()));
        extras.setWindGustKph((float) Math.round(day.getWindgstMaxKmh()));
        extras.setQpfRainMm(day.getRainTotalMm());
        extras.setQpfRainIn(day.getRainTotalIn());
        extras.setQpfSnowCm(day.getSnowTotalMm() / 10f);
        extras.setQpfSnowIn(day.getSnowTotalIn());
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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