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
import com.thewizrd.shared_resources.utils.DateTimeUtils;
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
import java.util.Arrays;

public class HourlyForecast extends CustomJsonObject {

    @SerializedName("high_f")
    private String highF;

    @SerializedName("high_c")
    private String highC;

    @SerializedName("condition")
    private String condition;

    @SerializedName("icon")
    private String icon;

    @SerializedName("pop")
    private String pop;

    @SerializedName("wind_degrees")
    private int windDegrees;

    @SerializedName("wind_mph")
    private float windMph;

    @SerializedName("wind_kph")
    private float windKph;

    public String get_date() {
        return _date;
    }

    public void set_date(String _date) {
        this._date = _date;
    }

    @SerializedName("date")
    private String _date;

    @SerializedName("extras")
    private ForecastExtras extras;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HourlyForecast() {

    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.openweather.HourlyItem hr_forecast) {
        setDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(hr_forecast.getDt()), ZoneOffset.UTC));
        highF = ConversionMethods.KtoF(Float.toString(hr_forecast.getTemp()));
        highC = ConversionMethods.KtoC(Float.toString(hr_forecast.getTemp()));
        condition = StringUtils.toUpperCase(hr_forecast.getWeather().get(0).getDescription());

        // Use icon to determine if day or night
        String ico = hr_forecast.getWeather().get(0).getIcon();
        String dn = Character.toString(ico.charAt(ico.length() == 0 ? 0 : ico.length() - 1));

        try {
            int x = Integer.parseInt(dn);
            dn = "";
        } catch (NumberFormatException ex) {
            // Do nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(hr_forecast.getWeather().get(0).getId() + dn);

        // Use cloudiness value here
        pop = Integer.toString(hr_forecast.getClouds());
        windDegrees = hr_forecast.getWindDeg();
        windMph = (float) Math.round(Double.parseDouble(ConversionMethods.msecToMph(Float.toString(hr_forecast.getWindSpeed()))));
        windKph = (float) Math.round(Double.parseDouble(ConversionMethods.msecToKph(Float.toString(hr_forecast.getWindSpeed()))));

        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(Float.parseFloat(ConversionMethods.KtoF(Float.toString(hr_forecast.getFeelsLike()))));
        extras.setFeelslikeC(Float.parseFloat(ConversionMethods.FtoC(Float.toString(hr_forecast.getFeelsLike()))));
        extras.setDewpointF(ConversionMethods.KtoF(Float.toString(hr_forecast.getDewPoint())));
        extras.setDewpointC(ConversionMethods.FtoC(Float.toString(hr_forecast.getDewPoint())));
        extras.setHumidity(Integer.toString(hr_forecast.getHumidity()));
        extras.setPop(pop);
        // 1hPA = 1mbar
        extras.setPressureMb(Float.toString(hr_forecast.getPressure()));
        extras.setPressureIn(ConversionMethods.mbToInHg(Float.toString(hr_forecast.getPressure())));
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
        if (hr_forecast.getVisibility() != null) {
            extras.setVisibilityKm(hr_forecast.getVisibility().toString());
            extras.setVisibilityMi(ConversionMethods.kmToMi(extras.getVisibilityKm()));
        }
        if (hr_forecast.getRain() != null) {
            extras.setQpfRainMm(hr_forecast.getRain().get_1h());
            extras.setQpfRainIn(Float.parseFloat(ConversionMethods.mmToIn(Float.toString(hr_forecast.getRain().get_1h()))));
        }
        if (hr_forecast.getSnow() != null) {
            extras.setQpfSnowCm(hr_forecast.getSnow().get_1h() / 10);
            extras.setQpfSnowIn(Float.parseFloat(ConversionMethods.mmToIn(Float.toString(hr_forecast.getSnow().get_1h()))));
        }
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem hr_forecast) {
        // new DateTimeOffset(, TimeSpan.Zero);
        setDate(ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(hr_forecast.getTime())), ZoneOffset.UTC));
        highF = ConversionMethods.CtoF(hr_forecast.getData().getInstant().getDetails().getAirTemperature().toString());
        highC = hr_forecast.getData().getInstant().getDetails().getAirTemperature().toString();
        // Use cloudiness value here
        pop = Integer.toString(Math.round(hr_forecast.getData().getInstant().getDetails().getCloudAreaFraction()));
        windDegrees = Math.round(hr_forecast.getData().getInstant().getDetails().getWindFromDirection());
        windMph = (float) Math.round(Double.parseDouble(ConversionMethods.msecToMph(hr_forecast.getData().getInstant().getDetails().getWindSpeed().toString())));
        windKph = (float) Math.round(Double.parseDouble(ConversionMethods.msecToKph(hr_forecast.getData().getInstant().getDetails().getWindSpeed().toString())));

        if (hr_forecast.getData().getNext12Hours() != null) {
            icon = hr_forecast.getData().getNext12Hours().getSummary().getSymbolCode();
        } else if (hr_forecast.getData().getNext6Hours() != null) {
            icon = hr_forecast.getData().getNext6Hours().getSummary().getSymbolCode();
        } else if (hr_forecast.getData().getNext1Hours() != null) {
            icon = hr_forecast.getData().getNext1Hours().getSummary().getSymbolCode();
        }

        float humidity = hr_forecast.getData().getInstant().getDetails().getRelativeHumidity();
        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(Float.parseFloat(WeatherUtils.getFeelsLikeTemp(highF, Double.toString(windMph), Integer.toString(Math.round(humidity)))));
        extras.setFeelslikeC(Float.parseFloat(ConversionMethods.FtoC(WeatherUtils.getFeelsLikeTemp(highF, Double.toString(windMph), Integer.toString(Math.round(humidity))))));
        extras.setHumidity(Integer.toString(Math.round(humidity)));
        extras.setDewpointF(ConversionMethods.CtoF(hr_forecast.getData().getInstant().getDetails().getDewPointTemperature().toString()));
        extras.setDewpointC(hr_forecast.getData().getInstant().getDetails().getDewPointTemperature().toString());
        extras.setPop(pop);
        extras.setPressureIn(ConversionMethods.mbToInHg(hr_forecast.getData().getInstant().getDetails().getAirPressureAtSeaLevel().toString()));
        extras.setPressureMb(hr_forecast.getData().getInstant().getDetails().getAirPressureAtSeaLevel().toString());
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
        if (hr_forecast.getData().getInstant().getDetails().getFogAreaFraction() != null) {
            float visMi = 10.0f;
            extras.setVisibilityMi(Float.toString((visMi - (visMi * hr_forecast.getData().getInstant().getDetails().getFogAreaFraction() / 100))));
            extras.setVisibilityKm(ConversionMethods.miToKm(extras.getVisibilityMi()));
        }
        if (hr_forecast.getData().getInstant().getDetails().getUltravioletIndexClearSky() != null) {
            extras.setUvIndex(hr_forecast.getData().getInstant().getDetails().getUltravioletIndexClearSky());
        }
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem1 hr_forecast) {
        setDate(ZonedDateTime.parse(hr_forecast.getUtcTime()));
        try {
            highF = hr_forecast.getTemperature();
            highC = ConversionMethods.FtoC(hr_forecast.getTemperature());
        } catch (NumberFormatException ignored) {
            highF = null;
            highC = null;
        }
        condition = StringUtils.toPascalCase(hr_forecast.getDescription());

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", hr_forecast.getDaylight(), hr_forecast.getIconName()));

        pop = hr_forecast.getPrecipitationProbability();
        try {
            windDegrees = Integer.parseInt(hr_forecast.getWindDirection());
        } catch (NumberFormatException ignored) {
        }
        try {
            String windSpeed = hr_forecast.getWindSpeed();
            windMph = Float.parseFloat(windSpeed);
            windKph = Float.parseFloat(ConversionMethods.mphTokph(windSpeed));
        } catch (NumberFormatException ignored) {
        }

        // Extras
        extras = new ForecastExtras();
        try {
            float comfortTempF = Float.parseFloat(hr_forecast.getComfort());
            extras.setFeelslikeF(comfortTempF);
            extras.setFeelslikeC(Float.parseFloat(ConversionMethods.FtoC(Float.toString(comfortTempF))));
        } catch (NumberFormatException ignored) {
        }
        extras.setHumidity(hr_forecast.getHumidity());
        try {
            extras.setDewpointF(hr_forecast.getDewPoint());
            extras.setDewpointC(ConversionMethods.FtoC(hr_forecast.getDewPoint()));
        } catch (NumberFormatException ignored) {
            extras.setDewpointF(null);
            extras.setDewpointC(null);
        }
        try {
            extras.setVisibilityMi(hr_forecast.getVisibility());
            extras.setVisibilityKm(ConversionMethods.miToKm(hr_forecast.getVisibility()));
        } catch (NumberFormatException ignored) {
            extras.setVisibilityMi(null);
            extras.setVisibilityKm(null);
        }
        extras.setPop(pop);
        try {
            float rain_in = Float.parseFloat(hr_forecast.getRainFall());
            extras.setQpfRainIn(rain_in);
            extras.setQpfRainMm(Float.parseFloat(ConversionMethods.inToMM(Float.toString(rain_in))));
        } catch (NumberFormatException ignored) {
        }
        try {
            float snow_in = Float.parseFloat(hr_forecast.getSnowFall());
            extras.setQpfSnowIn(snow_in);
            extras.setQpfSnowCm(Float.parseFloat(ConversionMethods.inToMM(Float.toString(snow_in))) / 10);
        } catch (NumberFormatException ignored) {
        }
        //extras.setPressureIn(hr_forecast.getBarometerPressure());
        //extras.setPressureMb(ConversionMethods.inHgToMB(hr_forecast.getBarometerPressure()));
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem) {
        setDate(ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME));
        highF = Integer.toString(forecastItem.getTemperature());
        highC = ConversionMethods.FtoC(highF);
        condition = forecastItem.getShortForecast();
        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(forecastItem.getIcon());
        pop = null;

        if (forecastItem.getWindSpeed() != null && forecastItem.getWindDirection() != null) {
            windDegrees = WeatherUtils.getWindDirection(forecastItem.getWindDirection());

            // windSpeed is reported usually as, for ex., '7 to 10 mph'
            // Format and split text into min and max
            String[] speeds = forecastItem.getWindSpeed().replace(" mph", "").split(" to ");
            String maxWindSpeed = Iterables.getLast(Arrays.asList(speeds), null);
            if (!StringUtils.isNullOrWhitespace(maxWindSpeed)) {
                Integer windSpeed = NumberUtils.tryParseInt(maxWindSpeed);
                if (windSpeed != null) {
                    windMph = windSpeed;
                    windKph = Float.parseFloat(ConversionMethods.mphTokph(maxWindSpeed));

                    // Extras
                    extras = new ForecastExtras();
                    extras.setWindDegrees(this.windDegrees);
                    extras.setWindMph(this.windMph);
                    extras.setWindKph(this.windKph);
                }
            }
        }
    }

    public ZonedDateTime getDate() {
        ZonedDateTime dateTime = null;

        try {
            dateTime = ZonedDateTime.parse(_date, DateTimeUtils.getZonedDateTimeFormatter());
        } catch (Exception ex) {
            dateTime = null;
        }

        if (dateTime == null)
            dateTime = ZonedDateTime.parse(_date);

        return dateTime;
    }

    public void setDate(ZonedDateTime date) {
        _date = date.format(DateTimeUtils.getZonedDateTimeFormatter());
    }

    public String getHighF() {
        return highF;
    }

    public void setHighF(String highF) {
        this.highF = highF;
    }

    public String getHighC() {
        return highC;
    }

    public void setHighC(String highC) {
        this.highC = highC;
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

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
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
                        this._date = reader.nextString();
                        break;
                    case "high_f":
                        this.highF = reader.nextString();
                        break;
                    case "high_c":
                        this.highC = reader.nextString();
                        break;
                    case "condition":
                        this.condition = reader.nextString();
                        break;
                    case "icon":
                        this.icon = reader.nextString();
                        break;
                    case "pop":
                        this.pop = reader.nextString();
                        break;
                    case "wind_degrees":
                        this.windDegrees = Integer.parseInt(reader.nextString());
                        break;
                    case "wind_mph":
                        this.windMph = Float.parseFloat(reader.nextString());
                        break;
                    case "wind_kph":
                        this.windKph = Float.parseFloat(reader.nextString());
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
            writer.value(_date);

            // "high_f" : ""
            writer.name("high_f");
            writer.value(highF);

            // "high_c" : ""
            writer.name("high_c");
            writer.value(highC);

            // "condition" : ""
            writer.name("condition");
            writer.value(condition);

            // "icon" : ""
            writer.name("icon");
            writer.value(icon);

            // "pop" : ""
            writer.name("pop");
            writer.value(pop);

            // "wind_degrees" : ""
            writer.name("wind_degrees");
            writer.value(windDegrees);

            // "wind_mph" : ""
            writer.name("wind_mph");
            writer.value(windMph);

            // "wind_kph" : ""
            writer.name("wind_kph");
            writer.value(windKph);

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
            Logger.writeLine(Log.ERROR, e, "HourlyForecast: error writing json string");
        }
    }
}