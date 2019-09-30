package com.thewizrd.shared_resources.wearable;

import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class WearWeatherJSON {
    private String weatherData;
    private List<String> weatherAlerts;
    private long update_time;

    public String getWeatherData() {
        return weatherData;
    }

    public void setWeatherData(String weatherData) {
        this.weatherData = weatherData;
    }

    public List<String> getWeatherAlerts() {
        return weatherAlerts;
    }

    public void setWeatherAlerts(List<String> weatherAlerts) {
        this.weatherAlerts = weatherAlerts;
    }

    public long getUpdateTime() {
        return update_time;
    }

    public void setUpdateTime(long update_time) {
        this.update_time = update_time;
    }

    public boolean isValid() {
        return weatherData != null && weatherAlerts != null;
    }

    public static WearWeatherJSON fromJson(JsonReader extReader) {
        WearWeatherJSON obj = null;

        try {
            obj = new WearWeatherJSON();
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
                    case "weatherData":
                        obj.weatherData = reader.nextString();
                        break;
                    case "weatherAlerts":
                        List<String> alerts = new ArrayList<>();

                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING)
                                alerts.add(reader.nextString());
                        }
                        obj.weatherAlerts = alerts;

                        if (reader.peek() == JsonToken.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "update_time":
                        obj.update_time = reader.nextLong();
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

            // "weatherData" : ""
            writer.name("weatherData");
            writer.value(weatherData);

            // "weatherAlerts" : ""
            writer.name("weatherAlerts");
            writer.beginArray();
            for (String alert : weatherAlerts) {
                writer.value(alert);
            }
            writer.endArray();

            // "update_time" : ""
            writer.name("update_time");
            writer.value(update_time);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "WearWeatherJSON: error writing json string");
        }

        return sw.toString();
    }
}
