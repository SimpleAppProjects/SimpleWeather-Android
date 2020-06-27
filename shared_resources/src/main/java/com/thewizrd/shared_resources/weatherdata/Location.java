package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

public class Location extends CustomJsonObject {

    @SerializedName("name")
    private String name;
    @SerializedName("latitude")
    private String latitude;
    @SerializedName("longitude")
    private String longitude;
    @SerializedName("tz_offset")
    private ZoneOffset tzOffset;
    @SerializedName("tz_short")
    private String tzShort;
    @SerializedName("tz_long")
    private String tzLong;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Location() {
        // Needed for deserialization
        tzOffset = ZoneOffset.UTC;
    }

    public Location(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Location location) {
        // Use location name from location provider
        name = null;
        latitude = location.getLat();
        longitude = location.get_long();

        ZoneId zId = ZoneId.of(location.getTimezoneId());

        tzOffset = zId.getRules().getOffset(Instant.now());
        tzShort = ZonedDateTime.now(zId)
                .format(DateTimeFormatter.ofPattern("z", Locale.getDefault()));
        tzLong = location.getTimezoneId();
    }

    public Location(com.thewizrd.shared_resources.weatherdata.openweather.Rootobject root) {
        // Use location name from location provider
        name = null;
        latitude = Float.toString(root.getLat());
        longitude = Float.toString(root.getLon());

        ZoneId zId = ZoneId.of(root.getTimezone());

        tzOffset = zId.getRules().getOffset(Instant.now());
        tzShort = ZonedDateTime.now(zId)
                .format(DateTimeFormatter.ofPattern("z", Locale.getDefault()));
        tzLong = root.getTimezone();
    }

    public Location(com.thewizrd.shared_resources.weatherdata.metno.Response foreRoot) {
        // API doesn't provide location name (at all)
        name = null;
        latitude = String.format(Locale.ROOT, "%.4f", foreRoot.getGeometry().getCoordinates().get(1));
        longitude = String.format(Locale.ROOT, "%.4f", foreRoot.getGeometry().getCoordinates().get(0));
        tzOffset = ZoneOffset.UTC;
        tzShort = "UTC";
    }

    public Location(com.thewizrd.shared_resources.weatherdata.here.LocationItem location) {
        // Use location name from location provider
        name = null;
        latitude = Double.toString(location.getLatitude());
        longitude = Double.toString(location.getLongitude());
        tzOffset = ZoneOffset.UTC;
        tzShort = "UTC";
    }

    public Location(com.thewizrd.shared_resources.weatherdata.nws.PointsResponse pointsResponse) {
        // Use location name from location provider
        name = null;

        ZoneId zId = ZoneId.of(pointsResponse.getTimeZone());

        tzOffset = zId.getRules().getOffset(Instant.now());
        tzShort = ZonedDateTime.now(zId)
                .format(DateTimeFormatter.ofPattern("z", Locale.getDefault()));
        tzLong = pointsResponse.getTimeZone();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public ZoneOffset getTzOffset() {
        return tzOffset;
    }

    public void setTzOffset(ZoneOffset tzOffset) {
        this.tzOffset = tzOffset;
    }

    public String getTzShort() {
        return tzShort;
    }

    public void setTzShort(String tzShort) {
        this.tzShort = tzShort;
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
                        this.latitude = reader.nextString();
                        break;
                    case "longitude":
                        this.longitude = reader.nextString();
                        break;
                    case "tz_offset":
                        this.tzOffset = ZoneOffset.of(reader.nextString());
                        break;
                    case "tz_short":
                        this.tzShort = reader.nextString();
                        break;
                    case "tz_long":
                        this.tzLong = reader.nextString();
                        break;
                    default:
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

            // "tz_offset" : ""
            writer.name("tz_offset");
            writer.value(DateTimeUtils.offsetToHMSFormat(tzOffset));

            // "tz_short" : ""
            writer.name("tz_short");
            writer.value(tzShort);

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