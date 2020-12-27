package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

public class TextForecast extends CustomJsonObject {

    @SerializedName("date")
    private ZonedDateTime date;

    @SerializedName("fcttext")
    private String fcttext;

    @SerializedName("fcttext_metric")
    private String fcttextMetric;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public TextForecast() {
        // Needed for deserialization
    }

    /* OpenWeather OneCall
    public TextForecast(com.thewizrd.shared_resources.weatherdata.openweather.onecall.DailyItem forecast) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();

        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(forecast.getDt()), ZoneOffset.UTC);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_morning),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoF(forecast.getTemp().getMorn())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoF(forecast.getFeelsLike().getMorn()))));
        sb.append(StringUtils.lineSeparator());
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_day),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoF(forecast.getTemp().getDay())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoF(forecast.getFeelsLike().getDay()))));
        sb.append(StringUtils.lineSeparator());
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_eve),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoF(forecast.getTemp().getEve())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoF(forecast.getFeelsLike().getEve()))));
        sb.append(StringUtils.lineSeparator());
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_night),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoF(forecast.getTemp().getNight())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoF(forecast.getFeelsLike().getNight()))));

        fcttext = sb.toString();

        StringBuilder sb_metric = new StringBuilder();
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_morning),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoC(forecast.getTemp().getMorn())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoC(forecast.getFeelsLike().getMorn()))));
        sb_metric.append(StringUtils.lineSeparator());
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_day),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoC(forecast.getTemp().getDay())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoC(forecast.getFeelsLike().getDay()))));
        sb_metric.append(StringUtils.lineSeparator());
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_eve),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoC(forecast.getTemp().getEve())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoC(forecast.getFeelsLike().getEve()))));
        sb_metric.append(StringUtils.lineSeparator());
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_night),
                context.getString(R.string.label_temp),
                Math.round(ConversionMethods.KtoC(forecast.getTemp().getNight())),
                context.getString(R.string.label_feelslike),
                Math.round(ConversionMethods.KtoC(forecast.getFeelsLike().getNight()))));

        fcttextMetric = sb_metric.toString();
    }
     */

    public TextForecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecast) {
        date = ZonedDateTime.parse(forecast.getUtcTime());
        fcttext = String.format(Locale.ROOT, "%s - %s %s",
                forecast.getWeekday(),
                StringUtils.toPascalCase(forecast.getDescription()),
                StringUtils.toPascalCase(forecast.getBeaufortDescription()));
        fcttextMetric = fcttext;
    }

    public TextForecast(com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem forecastItem) {
        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        fcttext = String.format(Locale.ROOT,
                "%s - %s", forecastItem.getName(), forecastItem.getDetailedForecast());
        fcttextMetric = fcttext;
    }

    public TextForecast(com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem forecastItem,
                        com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem ntForecastItem) {
        date = ZonedDateTime.parse(forecastItem.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        fcttext = String.format(Locale.ROOT,
                "%s - %s\n\n%s - %s",
                forecastItem.getName(), forecastItem.getDetailedForecast(),
                ntForecastItem.getName(), ntForecastItem.getDetailedForecast());
        fcttextMetric = fcttext;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
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
                        this.date = ZonedDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        break;
                    case "fcttext":
                        this.fcttext = reader.nextString();
                        break;
                    case "fcttext_metric":
                        this.fcttextMetric = reader.nextString();
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
            writer.value(date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            // "fcttext" : ""
            writer.name("fcttext");
            writer.value(fcttext);

            // "fcttext_metric" : ""
            writer.name("fcttext_metric");
            writer.value(fcttextMetric);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "TextForecast: error writing json string");
        }
    }
}