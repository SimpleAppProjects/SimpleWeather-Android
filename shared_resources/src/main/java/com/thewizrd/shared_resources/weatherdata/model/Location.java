package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.ZoneIdCompat;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import okio.Buffer;

public class Location extends CustomJsonObject {

    @Json(name = "name")
    private String name;
    @Json(name = "latitude")
    private Float latitude;
    @Json(name = "longitude")
    private Float longitude;
    @Json(name = "tz_long")
    private String tzLong;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Location() {
        // Needed for deserialization
        tzLong = "UTC";
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
            ZoneId tzId = ZoneIdCompat.of(tzLong);
            if (tzId != null)
                return tzId.getRules().getOffset(Instant.now());
        }
        return ZoneOffset.UTC;
    }

    public String getTzShort() {
        if (!StringUtils.isNullOrWhitespace(tzLong)) {
            ZoneId zId = ZoneIdCompat.of(tzLong);
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
    public void fromJson(@NonNull JsonReader extReader) {
        try {
            JsonReader reader;
            String jsonValue;

            if (extReader.peek() == JsonReader.Token.STRING) {
                jsonValue = extReader.nextString();
            } else {
                jsonValue = null;
            }

            if (jsonValue == null)
                reader = extReader;
            else {
                reader = JsonReader.of(new Buffer().writeUtf8(jsonValue));
                reader.beginObject(); // StartObject
            }

            while (reader.hasNext() && reader.peek() != JsonReader.Token.END_OBJECT) {
                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonReader.Token.NULL) {
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

            if (reader.peek() == JsonReader.Token.END_OBJECT)
                reader.endObject();

        } catch (Exception ignored) {
        }
    }

    @Override
    public void toJson(@NonNull JsonWriter writer) {
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