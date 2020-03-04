package com.thewizrd.shared_resources.weatherdata;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class TextForecast {

    @SerializedName("title")
    private String title;

    @SerializedName("fcttext")
    private String fcttext;

    @SerializedName("fcttext_metric")
    private String fcttextMetric;

    @SerializedName("icon")
    private String icon;

    @SerializedName("pop")
    private String pop;

    private TextForecast() {

    }

    public TextForecast(com.thewizrd.shared_resources.weatherdata.weatherunderground.ForecastdayItem txt_forecast) {
        title = txt_forecast.getTitle();
        fcttext = txt_forecast.getFcttext();
        fcttextMetric = txt_forecast.getFcttextMetric();

        icon = WeatherManager.getProvider(WeatherAPI.WEATHERUNDERGROUND)
                .getWeatherIcon(txt_forecast.getIconUrl().replace("http://icons.wxug.com/i/c/k/", "").replace(".gif", ""));

        pop = txt_forecast.getPop();
    }

    public TextForecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecast) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();

        title = forecast.getWeekday();

        String fctxt = String.format("%s %s %s: %s",
                StringUtils.toPascalCase(forecast.getDescription()), StringUtils.toPascalCase(forecast.getBeaufortDescription()),
                context.getString(R.string.label_humidity),
                forecast.getHumidity() + "%");

        fcttext = String.format("%s %s %sF. %s %sF. %s %s %smph",
                fctxt,
                context.getString(R.string.label_high),
                Math.round(Double.parseDouble(forecast.getHighTemperature())),
                context.getString(R.string.label_low),
                Math.round(Double.parseDouble(forecast.getLowTemperature())),
                context.getString(R.string.label_wind),
                forecast.getWindDesc(), Math.round(Double.parseDouble(forecast.getWindSpeed())));

        fcttextMetric = String.format("%s %s %sC. %s %sC. %s %s %skph",
                fctxt,
                context.getString(R.string.label_high),
                ConversionMethods.FtoC(forecast.getHighTemperature()),
                context.getString(R.string.label_low),
                ConversionMethods.FtoC(forecast.getLowTemperature()),
                context.getString(R.string.label_wind),
                forecast.getWindDesc(), Math.round(Double.parseDouble(ConversionMethods.mphTokph(forecast.getWindSpeed()))));

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", forecast.getDaylight(), forecast.getIconName()));

        pop = forecast.getPrecipitationProbability();
    }

    public TextForecast(com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem) {
        title = forecastItem.getName();
        fcttext = forecastItem.getDetailedForecast();
        fcttextMetric = forecastItem.getDetailedForecast();
        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(forecastItem.getIcon());
        pop = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFcttext() {
        return fcttext;
    }

    public void setFcttext(String fcttext) {
        this.fcttext = fcttext;
    }

    public String getFcttextMetric() {
        return fcttextMetric;
    }

    public void setFcttextMetric(String fcttextMetric) {
        this.fcttextMetric = fcttextMetric;
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

    public static TextForecast fromJson(JsonReader extReader) {
        TextForecast obj = null;

        try {
            obj = new TextForecast();
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
                    case "title":
                        obj.title = reader.nextString();
                        break;
                    case "fcttext":
                        obj.fcttext = reader.nextString();
                        break;
                    case "fcttext_metric":
                        obj.fcttextMetric = reader.nextString();
                        break;
                    case "icon":
                        obj.icon = reader.nextString();
                        break;
                    case "pop":
                        obj.pop = reader.nextString();
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

            // "title" : ""
            writer.name("title");
            writer.value(title);

            // "fcttext" : ""
            writer.name("fcttext");
            writer.value(fcttext);

            // "fcttext_metric" : ""
            writer.name("fcttext_metric");
            writer.value(fcttextMetric);

            // "icon" : ""
            writer.name("icon");
            writer.value(icon);

            // "pop" : ""
            writer.name("pop");
            writer.value(pop);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "TextForecast: error writing json string");
        }

        return sw.toString();
    }
}