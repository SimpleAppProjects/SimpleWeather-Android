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
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class Forecast {

    @SerializedName("date")
    private LocalDateTime date;

    @SerializedName("high_f")
    private String highF;

    @SerializedName("high_c")
    private String highC;

    @SerializedName("low_f")
    private String lowF;

    @SerializedName("low_c")
    private String lowC;

    @SerializedName("condition")
    private String condition;

    @SerializedName("icon")
    private String icon;

    @SerializedName("extras")
    private ForecastExtras extras;

    private Forecast() {
        // Needed for deserialization
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatheryahoo.ForecastsItem forecast) {
        date = LocalDateTime.ofEpochSecond(Long.parseLong(forecast.getDate()), 0, ZoneOffset.UTC);
        highF = forecast.getHigh();
        highC = ConversionMethods.FtoC(highF);
        lowF = forecast.getLow();
        lowC = ConversionMethods.FtoC(lowF);
        condition = forecast.getText();
        icon = WeatherManager.getProvider(WeatherAPI.YAHOO)
                .getWeatherIcon(forecast.getCode());
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatherunderground.Forecastday1 forecast) {
        date = ConversionMethods.toEpochDateTime(forecast.getDate().getEpoch())
                .withZoneSameInstant(ZoneId.of(forecast.getDate().getTzLong())).toLocalDateTime();
        highF = forecast.getHigh().getFahrenheit();
        highC = forecast.getHigh().getCelsius();
        lowF = forecast.getLow().getFahrenheit();
        lowC = forecast.getLow().getCelsius();
        condition = forecast.getConditions();
        icon = WeatherManager.getProvider(WeatherAPI.WEATHERUNDERGROUND)
                .getWeatherIcon(forecast.getIcon_url().replace("http://icons.wxug.com/i/c/k/", "").replace(".gif", ""));

        // Extras
        extras = new ForecastExtras();
        try {
            extras.setFeelslikeF(Float.valueOf(WeatherUtils.getFeelsLikeTemp(highF, Integer.toString(forecast.getAvewind().getMph()), Integer.toString(forecast.getAvehumidity()))));
            extras.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Double.toString(extras.getFeelslikeF()))));
        } catch (NumberFormatException e) {
        }
        try {
            extras.setHumidity(Integer.toString(forecast.getAvehumidity()));
        } catch (NumberFormatException e) {
        }
        try {
            extras.setPop(Integer.toString(forecast.getPop()));
        } catch (NumberFormatException e) {
        }
        try {
            extras.setQpfRainIn((float) forecast.getQpf_allday().getIn());
            extras.setQpfRainMm((float) forecast.getQpf_allday().getMm());
        } catch (NumberFormatException e) {
        }
        try {
            extras.setQpfSnowIn(forecast.getSnow_allday().getIn());
            extras.setQpfSnowCm(forecast.getSnow_allday().getCm());
        } catch (NumberFormatException e) {
        }
        try {
            extras.setWindDegrees(forecast.getAvewind().getDegrees());
            extras.setWindMph((float) forecast.getAvewind().getMph());
            extras.setWindKph((float) forecast.getAvewind().getKph());
        } catch (NumberFormatException e) {
        }
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.openweather.ListItem forecast) {
        date = LocalDateTime.ofEpochSecond(forecast.getDt(), 0, ZoneOffset.UTC);
        highF = ConversionMethods.KtoF(Float.toString(forecast.getMain().getTempMax()));
        highC = ConversionMethods.KtoC(Float.toString(forecast.getMain().getTempMax()));
        lowF = ConversionMethods.KtoF(Float.toString(forecast.getMain().getTempMin()));
        lowC = ConversionMethods.KtoC(Float.toString(forecast.getMain().getTempMin()));
        condition = StringUtils.toUpperCase(forecast.getWeather().get(0).getDescription());
        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(Integer.toString(forecast.getWeather().get(0).getId()));
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata.Time time) {
        date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC);
        // Don't bother setting other values; they're not available yet
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecast) {
        date = ZonedDateTime.parse(forecast.getUtcTime()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        highF = forecast.getHighTemperature();
        try {
            highC = ConversionMethods.FtoC(forecast.getHighTemperature());
        } catch (NumberFormatException ignored) {
            highC = forecast.getHighTemperature();
        }
        lowF = forecast.getLowTemperature();
        try {
            lowC = ConversionMethods.FtoC(forecast.getLowTemperature());
        } catch (NumberFormatException ignored) {
            lowC = forecast.getLowTemperature();
        }
        condition = StringUtils.toPascalCase(forecast.getDescription());
        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", forecast.getDaylight(), forecast.getIconName()));

        // Extras
        extras = new ForecastExtras();
        try {
            float comfortTempF = Float.parseFloat(forecast.getComfort());
            extras.setFeelslikeF(comfortTempF);
            extras.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Float.toString(comfortTempF))));
        } catch (NumberFormatException ignored) {
        }
        extras.setHumidity(forecast.getHumidity());
        try {
            extras.setDewpointF(forecast.getDewPoint());
            extras.setDewpointC(ConversionMethods.FtoC(forecast.getDewPoint()));
        } catch (NumberFormatException ignored) {
        }
        extras.setPop(forecast.getPrecipitationProbability());
        try {
            extras.setQpfRainIn(Float.valueOf(forecast.getRainFall()));
        } catch (NumberFormatException ignored) {
        }
        extras.setQpfRainMm(Float.valueOf(ConversionMethods.inToMM(Float.toString(extras.getQpfRainIn()))));
        try {
            extras.setQpfSnowIn(Float.valueOf(forecast.getSnowFall()));
        } catch (NumberFormatException ignored) {
        }
        extras.setQpfSnowCm(Float.valueOf(ConversionMethods.inToMM(Float.toString(extras.getQpfSnowIn()))) / 10);
        extras.setPressureIn(forecast.getBarometerPressure());
        extras.setPressureMb(ConversionMethods.inHgToMB(forecast.getBarometerPressure()));
        extras.setWindDegrees(Integer.valueOf(forecast.getWindDirection()));
        extras.setWindMph(Float.valueOf(forecast.getWindSpeed()));
        extras.setWindKph(Float.valueOf(ConversionMethods.mphTokph(forecast.getWindSpeed())));
        try {
            extras.setUvIndex(Float.valueOf(forecast.getUvIndex()));
        } catch (NumberFormatException ignored) {
        }
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem, com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem nightForecastItem) {
        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        highF = Integer.toString(forecastItem.getTemperature());
        highC = ConversionMethods.FtoC(highF);
        lowF = Integer.toString(nightForecastItem.getTemperature());
        lowC = ConversionMethods.FtoC(lowF);
        condition = forecastItem.getShortForecast();
        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(forecastItem.getIcon());
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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

    public String getLowF() {
        return lowF;
    }

    public void setLowF(String lowF) {
        this.lowF = lowF;
    }

    public String getLowC() {
        return lowC;
    }

    public void setLowC(String lowC) {
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

    public static Forecast fromJson(JsonReader extReader) {
        Forecast obj = null;

        try {
            obj = new Forecast();
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
                        obj.date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(reader.nextString())), ZoneOffset.UTC);
                        break;
                    case "high_f":
                        obj.highF = reader.nextString();
                        break;
                    case "high_c":
                        obj.highC = reader.nextString();
                        break;
                    case "low_f":
                        obj.lowF = reader.nextString();
                        break;
                    case "low_c":
                        obj.lowC = reader.nextString();
                        break;
                    case "condition":
                        obj.condition = reader.nextString();
                        break;
                    case "icon":
                        obj.icon = reader.nextString();
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
                    writer.value(extras.toJson());
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Forecast: error writing json string");
        }

        return sw.toString();
    }
}