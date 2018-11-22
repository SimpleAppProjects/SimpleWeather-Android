package com.thewizrd.shared_resources.utils;

import android.arch.persistence.room.TypeConverter;
import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.weatherdata.Astronomy;
import com.thewizrd.shared_resources.weatherdata.Atmosphere;
import com.thewizrd.shared_resources.weatherdata.Condition;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Location;
import com.thewizrd.shared_resources.weatherdata.Precipitation;
import com.thewizrd.shared_resources.weatherdata.TextForecast;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class WeatherDBConverters {
    private static final DateTimeFormatter zDTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ");
    private static final DateTimeFormatter lDTF = DateTimeFormatter.ISO_INSTANT;

    @TypeConverter
    public static Location locationFromJson(String value) {
        return value == null ? null : Location.fromJson(new JsonReader(new StringReader(value)));
    }

    @TypeConverter
    public static String locationToJson(Location value) {
        return value == null ? null : value.toJson();
    }

    @TypeConverter
    public static ZonedDateTime zonedDateTimeFromString(String value) {
        return value == null ? null : ZonedDateTime.parse(value, zDTF);
    }

    @TypeConverter
    public static String zonedDateTimetoString(ZonedDateTime value) {
        return value == null ? null : value.format(zDTF);
    }

    @TypeConverter
    public static LocalDateTime localDateTimeFromString(String value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.from(lDTF.parse(value)), ZoneOffset.UTC);
    }

    @TypeConverter
    public static String localDateTimetoString(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }

    @TypeConverter
    public static Forecast[] forecastArrfromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            List<Forecast> result = new ArrayList<>();

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    result.add(Forecast.fromJson(reader));
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result.toArray(new Forecast[0]);
        }
    }

    @TypeConverter
    public static String forecastArrtoJson(Forecast[] value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (Forecast forecast : value) {
                    writer.value(forecast.toJson());
                }

                writer.endArray();
                writer.close();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error writing JSON");
            }

            return sw.toString();
        }
    }

    @TypeConverter
    public static HourlyForecast[] hrforecastArrfromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            List<HourlyForecast> result = new ArrayList<>();

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    result.add(HourlyForecast.fromJson(reader));
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result.toArray(new HourlyForecast[0]);
        }
    }

    @TypeConverter
    public static String hrforecastArrtoJson(HourlyForecast[] value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (HourlyForecast forecast : value) {
                    writer.value(forecast.toJson());
                }

                writer.endArray();
                writer.close();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error writing JSON");
            }

            return sw.toString();
        }
    }

    @TypeConverter
    public static TextForecast[] txtforecastArrfromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            ArrayList<TextForecast> result = new ArrayList<>();

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    result.add(TextForecast.fromJson(reader));
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result.toArray(new TextForecast[0]);
        }
    }

    @TypeConverter
    public static String txtforecastArrtoJson(TextForecast[] value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (TextForecast forecast : value) {
                    writer.value(forecast.toJson());
                }

                writer.endArray();
                writer.close();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error writing JSON");
            }

            return sw.toString();
        }
    }

    @TypeConverter
    public static Condition conditionFromJson(String value) {
        return value == null ? null : Condition.fromJson(new JsonReader(new StringReader(value)));
    }

    @TypeConverter
    public static String conditionToJson(Condition value) {
        return value == null ? null : value.toJson();
    }

    @TypeConverter
    public static Atmosphere atmosphereFromJson(String value) {
        return value == null ? null : Atmosphere.fromJson(new JsonReader(new StringReader(value)));
    }

    @TypeConverter
    public static String atmosphereToJson(Atmosphere value) {
        return value == null ? null : value.toJson();
    }

    @TypeConverter
    public static Astronomy astronomyFromJson(String value) {
        return value == null ? null : Astronomy.fromJson(new JsonReader(new StringReader(value)));
    }

    @TypeConverter
    public static String astronomyToJson(Astronomy value) {
        return value == null ? null : value.toJson();
    }

    @TypeConverter
    public static Precipitation precipitationFromJson(String value) {
        return value == null ? null : Precipitation.fromJson(new JsonReader(new StringReader(value)));
    }

    @TypeConverter
    public static String precipitationToJson(Precipitation value) {
        return value == null ? null : value.toJson();
    }

    @TypeConverter
    public static List<WeatherAlert> alertsListFromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            List<WeatherAlert> result = new ArrayList<>();

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    result.add(WeatherAlert.fromJson(reader));
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result;
        }
    }

    @TypeConverter
    public static String alertsListToJson(List<WeatherAlert> value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (WeatherAlert alert : value) {
                    writer.value(alert.toJson());
                }

                writer.endArray();
                writer.close();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error writing JSON");
            }

            return sw.toString();
        }
    }
}
