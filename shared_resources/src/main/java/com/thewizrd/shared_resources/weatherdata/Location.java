package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class Location extends CustomJsonObject {

    @SerializedName("name")
    private String name;
    @SerializedName("latitude")
    private Float latitude;
    @SerializedName("longitude")
    private Float longitude;
    @SerializedName("tz_long")
    private String tzLong;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Location() {
        // Needed for deserialization
        tzLong = "UTC";
    }

    public Location(com.thewizrd.shared_resources.weatherdata.openweather.ForecastRootobject root) {
        // Use location name from location provider
        name = null;
        latitude = root.getCity().getCoord().getLat();
        longitude = root.getCity().getCoord().getLon();
        tzLong = null;
    }

    /* OpenWeather OneCall
    public Location(com.thewizrd.shared_resources.weatherdata.openweather.onecall.Rootobject root) {
        // Use location name from location provider
        name = null;
        latitude = root.getLat();
        longitude = root.getLon();
        tzLong = root.getTimezone();
    }
     */

    public Location(com.thewizrd.shared_resources.weatherdata.metno.Response foreRoot) {
        // API doesn't provide location name (at all)
        name = null;
        latitude = foreRoot.getGeometry().getCoordinates().get(1);
        longitude = foreRoot.getGeometry().getCoordinates().get(0);
        tzLong = null;
    }

    public Location(com.thewizrd.shared_resources.weatherdata.here.LocationItem location) {
        // Use location name from location provider
        name = null;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        tzLong = null;
    }

    public Location(com.thewizrd.shared_resources.weatherdata.nws.observation.ForecastResponse forecastResponse) {
        // Use location name from location provider
        name = null;
        latitude = NumberUtils.tryParseFloat(forecastResponse.getLocation().getLatitude());
        longitude = NumberUtils.tryParseFloat(forecastResponse.getLocation().getLongitude());
        tzLong = null;
    }

    public Location(com.thewizrd.shared_resources.weatherdata.weatherunlocked.CurrentResponse currRoot) {
        // Use location name from location provider
        name = null;
        latitude = currRoot.getLat();
        longitude = currRoot.getLon();
        tzLong = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    public ZoneOffset getTzOffset() {
        if (!StringUtils.isNullOrWhitespace(tzLong)) {
            ZoneId tzId = ZoneId.of(tzLong);
            if (tzId != null)
                return tzId.getRules().getOffset(Instant.now());
        }
        return ZoneOffset.UTC;
    }

    public String getTzShort() {
        if (!StringUtils.isNullOrWhitespace(tzLong)) {
            ZoneId zId = ZoneId.of(tzLong);
            if (zId != null) {
                return ZonedDateTime.now(zId).format(
                        DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.TIMEZONE_NAME)
                );
            }
        }
        return "UTC";
    }

    public String getTzLong() {
        return tzLong;
    }

    public void setTzLong(String tzLong) {
        this.tzLong = tzLong;
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
                    case "name":
                        this.name = reader.nextString();
                        break;
                    case "latitude":
                        this.latitude = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "longitude":
                        this.longitude = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "tz_long":
                        this.tzLong = reader.nextString();
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

            // "name" : ""
            writer.name("name");
            writer.value(name);

            // "latitude" : ""
            writer.name("latitude");
            writer.value(latitude);

            // "longitude" : ""
            writer.name("longitude");
            writer.value(longitude);

            // "tz_long" : ""
            writer.name("tz_long");
            writer.value(tzLong);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Location: error writing json string");
        }
    }
}