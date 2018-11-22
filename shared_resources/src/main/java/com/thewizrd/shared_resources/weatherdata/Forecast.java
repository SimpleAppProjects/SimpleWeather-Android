package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
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

    private Forecast() {
        // Needed for deserialization
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatheryahoo.ForecastItem forecast) {
        date = LocalDate.parse(forecast.getDate(), DateTimeFormatter.ofPattern("dd MMM yyyy")).atTime(0, 0);
        highF = forecast.getHigh();
        highC = ConversionMethods.FtoC(highF);
        lowF = forecast.getLow();
        lowC = ConversionMethods.FtoC(lowF);
        condition = forecast.getText();
        icon = WeatherManager.getProvider(WeatherAPI.YAHOO)
                .getWeatherIcon(forecast.getCode());
    }

    public Forecast(com.thewizrd.shared_resources.weatherdata.weatherunderground.Forecastday1 forecast) {
        date = ConversionMethods.toEpochDateTime(forecast.getDate().getEpoch()).toLocalDateTime();
        highF = forecast.getHigh().getFahrenheit();
        highC = forecast.getHigh().getCelsius();
        lowF = forecast.getLow().getFahrenheit();
        lowC = forecast.getLow().getCelsius();
        condition = forecast.getConditions();
        icon = WeatherManager.getProvider(WeatherAPI.WEATHERUNDERGROUND)
                .getWeatherIcon(forecast.getIcon_url().replace("http://icons.wxug.com/i/c/k/", "").replace(".gif", ""));
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
        highC = ConversionMethods.FtoC(forecast.getHighTemperature());
        lowF = forecast.getLowTemperature();
        lowC = ConversionMethods.FtoC(forecast.getLowTemperature());
        condition = StringUtils.toPascalCase(forecast.getDescription());
        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", forecast.getDaylight(), forecast.getIconName()));
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

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Forecast: error writing json string");
        }

        return sw.toString();
    }
}