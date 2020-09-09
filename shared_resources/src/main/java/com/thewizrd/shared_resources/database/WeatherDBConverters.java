package com.thewizrd.shared_resources.database;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.room.TypeConverter;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
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
import java.util.Collection;
import java.util.List;

public class WeatherDBConverters {
    private static final DateTimeFormatter zDTF = DateTimeUtils.getZonedDateTimeFormatter();
    private static final DateTimeFormatter lDTF = DateTimeFormatter.ISO_INSTANT;

    @TypeConverter
    public static Location locationFromJson(String value) {
        return JSONParser.deserializer(value, Location.class);
    }

    @TypeConverter
    public static String locationToJson(Location value) {
        return JSONParser.serializer(value, Location.class);
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
    public static List<Forecast> forecastArrfromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            List<Forecast> result = new ArrayList<>(10);

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    Forecast obj = new Forecast();
                    obj.fromJson(reader);
                    result.add(obj);
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result;
        }
    }

    @TypeConverter
    public static String forecastArrtoJson(List<Forecast> value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (Forecast forecast : value) {
                    forecast.toJson(writer);
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
    public static HourlyForecast hrforecastFromJson(String value) {
        if (value == null)
            return null;
        else {
            HourlyForecast obj = new HourlyForecast();
            obj.fromJson(new JsonReader(new StringReader(value)));
            return obj;
        }
    }

    @TypeConverter
    public static String hrforecastToJson(HourlyForecast value) {
        return JSONParser.serializer(value, HourlyForecast.class);
    }

    @TypeConverter
    public static List<HourlyForecast> hrforecastArrfromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            List<HourlyForecast> result = new ArrayList<>(90);

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    HourlyForecast obj = new HourlyForecast();
                    obj.fromJson(reader);
                    result.add(obj);
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result;
        }
    }

    @TypeConverter
    public static String hrforecastArrtoJson(List<HourlyForecast> value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (HourlyForecast forecast : value) {
                    forecast.toJson(writer);
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
    public static List<TextForecast> txtforecastArrfromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            ArrayList<TextForecast> result = new ArrayList<>(20);

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    TextForecast obj = new TextForecast();
                    obj.fromJson(reader);
                    result.add(obj);
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result;
        }
    }

    @TypeConverter
    public static String txtforecastArrtoJson(List<TextForecast> value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (TextForecast forecast : value) {
                    forecast.toJson(writer);
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
        if (value == null)
            return null;
        else {
            Condition obj = new Condition();
            obj.fromJson(new JsonReader(new StringReader(value)));
            return obj;
        }
    }

    @TypeConverter
    public static String conditionToJson(Condition value) {
        return JSONParser.serializer(value, Condition.class);
    }

    @TypeConverter
    public static Atmosphere atmosphereFromJson(String value) {
        if (value == null)
            return null;
        else {
            Atmosphere obj = new Atmosphere();
            obj.fromJson(new JsonReader(new StringReader(value)));
            return obj;
        }
    }

    @TypeConverter
    public static String atmosphereToJson(Atmosphere value) {
        return JSONParser.serializer(value, Atmosphere.class);
    }

    @TypeConverter
    public static Astronomy astronomyFromJson(String value) {
        if (value == null)
            return null;
        else {
            Astronomy obj = new Astronomy();
            obj.fromJson(new JsonReader(new StringReader(value)));
            return obj;
        }
    }

    @TypeConverter
    public static String astronomyToJson(Astronomy value) {
        return JSONParser.serializer(value, Astronomy.class);
    }

    @TypeConverter
    public static Precipitation precipitationFromJson(String value) {
        if (value == null)
            return null;
        else {
            Precipitation obj = new Precipitation();
            obj.fromJson(new JsonReader(new StringReader(value)));
            return obj;
        }
    }

    @TypeConverter
    public static String precipitationToJson(Precipitation value) {
        return JSONParser.serializer(value, Precipitation.class);
    }

    @TypeConverter
    public static Collection<WeatherAlert> alertsListFromJson(String value) {
        if (value == null)
            return null;
        else {
            StringReader sr = new StringReader(value);
            JsonReader reader = new JsonReader(sr);
            List<WeatherAlert> result = new ArrayList<>();

            try {
                reader.beginArray();

                while (reader.hasNext()) {
                    @SuppressLint("RestrictedApi") WeatherAlert obj = new WeatherAlert();
                    obj.fromJson(reader);
                    result.add(obj);
                }

                reader.endArray();
            } catch (IOException ex) {
                Logger.writeLine(Log.ERROR, ex, "Error parsing JSON");
            }

            return result;
        }
    }

    @TypeConverter
    public static String alertsListToJson(Collection<WeatherAlert> value) {
        if (value == null)
            return null;
        else {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);

            try {
                writer.beginArray();

                for (WeatherAlert alert : value) {
                    alert.toJson(writer);
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