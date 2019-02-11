package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.Logger;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

public class Astronomy {

    @SerializedName("sunrise")
    private LocalDateTime sunrise;

    @SerializedName("sunset")
    private LocalDateTime sunset;

    private Astronomy() {
        // Needed for deserialization
    }

    public Astronomy(com.thewizrd.shared_resources.weatherdata.weatherunderground.SunPhase sun_phase) {
        LocalDate now = LocalDate.now();

        sunset = LocalTime.parse(String.format("%s:%s",
                sun_phase.getSunset().getHour(), sun_phase.getSunset().getMinute()),
                DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)).atDate(now);
        sunrise = LocalTime.parse(String.format("%s:%s",
                sun_phase.getSunrise().getHour(), sun_phase.getSunrise().getMinute()),
                DateTimeFormatter.ofPattern("H:mm", Locale.ROOT)).atDate(now);
    }

    public Astronomy(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Astronomy astronomy) {
        LocalDate now = LocalDate.now();

        sunrise = LocalTime.parse(astronomy.getSunrise().toUpperCase(), DateTimeFormatter.ofPattern("h:m a", Locale.ROOT)).atDate(now);
        sunset = LocalTime.parse(astronomy.getSunset().toUpperCase(), DateTimeFormatter.ofPattern("h:m a", Locale.ROOT)).atDate(now);
    }

    public Astronomy(com.thewizrd.shared_resources.weatherdata.openweather.CurrentRootobject root) {
        sunrise = LocalDateTime.ofEpochSecond(root.getSys().getSunrise(), 0, ZoneOffset.UTC);
        sunset = LocalDateTime.ofEpochSecond(root.getSys().getSunset(), 0, ZoneOffset.UTC);
    }

    public Astronomy(com.thewizrd.shared_resources.weatherdata.metno.Astrodata astroRoot) {
        if (astroRoot.getLocation().getTime().getSunrise() != null) {
            sunrise = ZonedDateTime.parse(astroRoot.getLocation().getTime().getSunrise().getTime(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        }
        if (astroRoot.getLocation().getTime().getSunset() != null) {
            sunset = ZonedDateTime.parse(astroRoot.getLocation().getTime().getSunset().getTime(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        }

        // If the sun won't set/rise, set time to the future
        if (sunrise == null) {
            sunrise = LocalDateTime.now().plusYears(1).minusNanos(1);
        }
        if (sunset == null) {
            sunset = LocalDateTime.now().plusYears(1).minusNanos(1);
        }
    }

    public Astronomy(List<com.thewizrd.shared_resources.weatherdata.here.AstronomyItem> astronomy) {
        com.thewizrd.shared_resources.weatherdata.here.AstronomyItem astroData = astronomy.get(0);

        LocalDate now = LocalDate.now();

        sunrise = LocalTime.parse(astroData.getSunrise(), DateTimeFormatter.ofPattern("h:mma", Locale.ROOT)).atDate(now);
        sunset = LocalTime.parse(astroData.getSunset(), DateTimeFormatter.ofPattern("h:mma", Locale.ROOT)).atDate(now);
    }

    public LocalDateTime getSunrise() {
        return sunrise;
    }

    public void setSunrise(LocalDateTime sunrise) {
        this.sunrise = sunrise;
    }

    public LocalDateTime getSunset() {
        return sunset;
    }

    public void setSunset(LocalDateTime sunset) {
        this.sunset = sunset;
    }

    public static Astronomy fromJson(JsonReader extReader) {
        Astronomy obj = null;

        try {
            obj = new Astronomy();
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
                    case "sunrise":
                        obj.sunrise = LocalDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        break;
                    case "sunset":
                        obj.sunset = LocalDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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

            // "sunrise" : ""
            writer.name("sunrise");
            writer.value(sunrise.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // "sunset" : ""
            writer.name("sunset");
            writer.value(sunset.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Astronomy: error writing json string");
        }

        return sw.toString();
    }
}