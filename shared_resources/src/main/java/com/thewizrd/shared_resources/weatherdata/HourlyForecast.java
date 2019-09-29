package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

public class HourlyForecast {

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

    @SerializedName("date")
    private String _date;

    @SerializedName("extras")
    private ForecastExtras extras;

    private HourlyForecast() {

    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.weatherunderground.HourlyForecastItem hr_forecast) {
        String dateformat = String.format("%s/%s/%s %s", hr_forecast.getFCTTIME().getMon(),
                hr_forecast.getFCTTIME().getMday(), hr_forecast.getFCTTIME().getYear(), hr_forecast.getFCTTIME().getCivil());
        setDate(LocalDateTime.parse(dateformat, DateTimeFormatter.ofPattern("M/d/yyyy h:mm a", Locale.ROOT)).atZone(ZoneOffset.UTC));
        highF = hr_forecast.getTemp().getEnglish();
        highC = hr_forecast.getTemp().getMetric();
        condition = hr_forecast.getCondition();

        icon = WeatherManager.getProvider(WeatherAPI.WEATHERUNDERGROUND)
                .getWeatherIcon(hr_forecast.getIconUrl().replace("http://icons.wxug.com/i/c/k/", "").replace(".gif", ""));

        pop = hr_forecast.getPop();
        windDegrees = Integer.valueOf(hr_forecast.getWdir().getDegrees());
        windMph = Float.valueOf(hr_forecast.getWspd().getEnglish());
        windKph = Float.valueOf(hr_forecast.getWspd().getMetric());

        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(Float.valueOf(hr_forecast.getFeelslike().getEnglish()));
        extras.setFeelslikeC(Float.valueOf(hr_forecast.getFeelslike().getMetric()));
        extras.setHumidity(hr_forecast.getHumidity());
        extras.setDewpointF(hr_forecast.getDewpoint().getEnglish());
        extras.setDewpointC(hr_forecast.getDewpoint().getMetric());
        extras.setUvIndex(Float.valueOf(hr_forecast.getUvi()));
        extras.setPop(hr_forecast.getPop());
        extras.setQpfRainIn(Float.valueOf(hr_forecast.getQpf().getEnglish()));
        extras.setQpfRainMm(Float.valueOf(hr_forecast.getQpf().getMetric()));
        extras.setQpfSnowIn(Float.valueOf(hr_forecast.getSnow().getEnglish()));
        extras.setQpfSnowCm(Float.valueOf(hr_forecast.getSnow().getMetric()));
        extras.setPressureIn(hr_forecast.getMslp().getEnglish());
        extras.setPressureMb(hr_forecast.getMslp().getMetric());
        extras.setWindDegrees(Integer.valueOf(hr_forecast.getWdir().getDegrees()));
        extras.setWindMph(Float.valueOf(hr_forecast.getWspd().getEnglish()));
        extras.setWindKph(Float.valueOf(hr_forecast.getWspd().getMetric()));
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.openweather.ListItem hr_forecast) {
        setDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(hr_forecast.getDt()), ZoneOffset.UTC));
        highF = ConversionMethods.KtoF(Float.toString(hr_forecast.getMain().getTemp()));
        highC = ConversionMethods.KtoC(Float.toString(hr_forecast.getMain().getTemp()));
        condition = StringUtils.toUpperCase(hr_forecast.getWeather().get(0).getDescription());

        // Use icon to determine if day or night
        String ico = hr_forecast.getWeather().get(0).getIcon();
        String dn = Character.toString(ico.charAt(ico.length() == 0 ? 0 : ico.length() - 1));

        try {
            int x = Integer.valueOf(dn);
            dn = "";
        } catch (NumberFormatException ex) {
            // Do nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(hr_forecast.getWeather().get(0).getId() + dn);

        // Use cloudiness value here
        pop = Integer.toString(hr_forecast.getClouds().getAll());
        windDegrees = (int) hr_forecast.getWind().getDeg();
        windMph = (float) Math.round(Double.valueOf(ConversionMethods.msecToMph(Float.toString(hr_forecast.getWind().getSpeed()))));
        windKph = (float) Math.round(Double.valueOf(ConversionMethods.msecToKph(Float.toString(hr_forecast.getWind().getSpeed()))));

        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(Float.valueOf(WeatherUtils.getFeelsLikeTemp(highF, Double.toString(windMph), hr_forecast.getMain().getHumidity())));
        extras.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Double.toString(extras.getFeelslikeF()))));
        extras.setHumidity(hr_forecast.getMain().getHumidity());
        extras.setPop(pop);
        if (hr_forecast.getRain() != null) {
            extras.setQpfRainIn(Float.valueOf(ConversionMethods.mmToIn(Float.toString(hr_forecast.getRain().get_3h()))));
            extras.setQpfRainMm(hr_forecast.getRain().get_3h());
        }
        if (hr_forecast.getSnow() != null) {
            extras.setQpfSnowIn(Float.valueOf(ConversionMethods.mmToIn(Float.toString(hr_forecast.getSnow().get_3h()))));
            extras.setQpfSnowCm(hr_forecast.getSnow().get_3h() / 10);
        }
        extras.setPressureIn(ConversionMethods.mbToInHg(Float.toString(hr_forecast.getMain().getPressure())));
        extras.setPressureMb(Float.toString(hr_forecast.getMain().getPressure()));
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata.Time hr_forecast) {
        // new DateTimeOffset(, TimeSpan.Zero);
        setDate(ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(hr_forecast.getFrom())), ZoneOffset.UTC));
        highF = ConversionMethods.CtoF(hr_forecast.getLocation().getTemperature().getValue().toString());
        highC = hr_forecast.getLocation().getTemperature().getValue().toString();
        //condition = hr_forecast.getWeather().get(0).main;
        //icon = hr_forecast.getWeather().get(0).id.ToString();
        // Use cloudiness value here
        pop = Integer.toString(Math.round(hr_forecast.getLocation().getCloudiness().getPercent().floatValue()));
        windDegrees = Math.round(hr_forecast.getLocation().getWindDirection().getDeg().floatValue());
        windMph = (float) Math.round(Double.valueOf(ConversionMethods.msecToMph(hr_forecast.getLocation().getWindSpeed().getMps().toString())));
        windKph = (float) Math.round(Double.valueOf(ConversionMethods.msecToKph(hr_forecast.getLocation().getWindSpeed().getMps().toString())));

        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(Float.valueOf(WeatherUtils.getFeelsLikeTemp(highF, Double.toString(windMph), Integer.toString(Math.round(hr_forecast.getLocation().getHumidity().getValue().floatValue())))));
        extras.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Double.toString(extras.getFeelslikeF()))));
        extras.setHumidity(Integer.toString(Math.round(hr_forecast.getLocation().getHumidity().getValue().floatValue())));
        extras.setDewpointF(ConversionMethods.CtoF(Float.toString(hr_forecast.getLocation().getDewpointTemperature().getValue().floatValue())));
        extras.setDewpointC(Float.toString(hr_forecast.getLocation().getDewpointTemperature().getValue().floatValue()));
        extras.setPop(pop);
        extras.setPressureIn(ConversionMethods.mbToInHg(hr_forecast.getLocation().getPressure().getValue().toString()));
        extras.setPressureMb(hr_forecast.getLocation().getPressure().getValue().toString());
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem1 hr_forecast) {
        setDate(ZonedDateTime.parse(hr_forecast.getUtcTime()));
        highF = hr_forecast.getTemperature();
        try {
            highC = ConversionMethods.FtoC(hr_forecast.getTemperature());
        } catch (NumberFormatException ignored) {
            highC = hr_forecast.getTemperature();
        }
        condition = StringUtils.toPascalCase(hr_forecast.getDescription());

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", hr_forecast.getDaylight(), hr_forecast.getIconName()));

        pop = hr_forecast.getPrecipitationProbability();
        try {
            windDegrees = Integer.valueOf(hr_forecast.getWindDirection());
        } catch (NumberFormatException ignored) {
        }
        try {
            windMph = Float.valueOf(hr_forecast.getWindSpeed());
            windKph = Float.valueOf(ConversionMethods.mphTokph(hr_forecast.getWindSpeed()));
        } catch (NumberFormatException ignored) {
        }

        // Extras
        extras = new ForecastExtras();
        try {
            float comfortTempF = Float.parseFloat(hr_forecast.getComfort());
            extras.setFeelslikeF(comfortTempF);
            extras.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Float.toString(comfortTempF))));
        } catch (NumberFormatException ignored) {
        }
        extras.setHumidity(hr_forecast.getHumidity());
        try {
            extras.setDewpointF(hr_forecast.getDewPoint());
            extras.setDewpointC(ConversionMethods.FtoC(hr_forecast.getDewPoint()));
        } catch (NumberFormatException ignored) {
        }
        extras.setPop(pop);
        try {
            extras.setQpfRainIn(Float.valueOf(hr_forecast.getRainFall()));
        } catch (NumberFormatException ignored) {
        }
        extras.setQpfRainMm(Float.valueOf(ConversionMethods.inToMM(Float.toString(extras.getQpfRainIn()))));
        try {
            extras.setQpfSnowIn(Float.valueOf(hr_forecast.getSnowFall()));
        } catch (NumberFormatException ignored) {
        }
        extras.setQpfSnowCm(Float.valueOf(ConversionMethods.inToMM(Float.toString(extras.getQpfSnowIn()))) / 10);
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
        windMph = Float.parseFloat(StringUtils.removeNonDigitChars(forecastItem.getWindSpeed()));
        windKph = Float.parseFloat(ConversionMethods.mphTokph(Float.toString(windMph)));
        pop = null;
        windDegrees = WeatherUtils.getWindDirection(forecastItem.getWindDirection());

        // Extras
        extras = new ForecastExtras();
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
    }

    public ZonedDateTime getDate() {
        ZonedDateTime dateTime = null;

        try {
            dateTime = ZonedDateTime.parse(_date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ"));
        } catch (Exception ex) {
            dateTime = null;
        }

        if (dateTime == null)
            dateTime = ZonedDateTime.parse(_date);

        return dateTime;
    }

    public void setDate(ZonedDateTime date) {
        _date = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ"));
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

    public static HourlyForecast fromJson(JsonReader extReader) {
        HourlyForecast obj = null;

        try {
            obj = new HourlyForecast();
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
                        obj._date = reader.nextString();
                        break;
                    case "high_f":
                        obj.highF = reader.nextString();
                        break;
                    case "high_c":
                        obj.highC = reader.nextString();
                        break;
                    case "condition":
                        obj.condition = reader.nextString();
                        break;
                    case "icon":
                        obj.icon = reader.nextString();
                        break;
                    case "pop":
                        obj.pop = reader.nextString();
                        break;
                    case "wind_degrees":
                        obj.windDegrees = Integer.valueOf(reader.nextString());
                        break;
                    case "wind_mph":
                        obj.windMph = Float.valueOf(reader.nextString());
                        break;
                    case "wind_kph":
                        obj.windKph = Float.valueOf(reader.nextString());
                        break;
                    case "extras":
                        obj.extras = ForecastExtras.fromJson(reader);
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
                    writer.value(extras.toJson());
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "HourlyForecast: error writing json string");
        }

        return sw.toString();
    }
}