package com.thewizrd.shared_resources.locationdata;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ibm.icu.util.TimeZone;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.LocationType;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

@Entity(tableName = "locations")
public class LocationData {
    @PrimaryKey
    @NonNull
    private String query;
    private String name;
    private double latitude;
    private double longitude;

    @ColumnInfo(name = "tz_long")
    private String tzLong;
    private LocationType locationType = LocationType.SEARCH;
    @ColumnInfo(name = "source")
    private String weatherSource;
    @ColumnInfo(name = "locsource")
    private String locationSource;

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
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
            if (zId != null)
                return ZonedDateTime.now(zId).format(DateTimeFormatter.ofPattern("z", Locale.getDefault()));
        }
        return "UTC";
    }

    public String getCountryCode() {
        if (!StringUtils.isNullOrWhitespace(tzLong)) {
            return TimeZone.getRegion(tzLong);
        }
        return "";
    }

    public String getTzLong() {
        return tzLong;
    }

    public void setTzLong(String tzLong) {
        this.tzLong = tzLong;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public String getWeatherSource() {
        return weatherSource;
    }

    public void setWeatherSource(String source) {
        this.weatherSource = source;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public LocationData() {
        weatherSource = Settings.getAPI();
    }

    @Ignore
    public LocationData(LocationQueryViewModel query_vm) {
        query = query_vm.getLocationQuery();
        name = query_vm.getLocationName();
        latitude = query_vm.getLocationLat();
        longitude = query_vm.getLocationLong();
        tzLong = query_vm.getLocationTZLong();
        weatherSource = query_vm.getWeatherSource();
        locationSource = query_vm.getLocationSource();
    }

    @Ignore
    public LocationData(LocationQueryViewModel query_vm, Location location) {
        setData(query_vm, location);
    }

    public void setData(LocationQueryViewModel query_vm, Location location) {
        query = query_vm.getLocationQuery();
        name = query_vm.getLocationName();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        tzLong = query_vm.getLocationTZLong();
        locationType = LocationType.GPS;
        weatherSource = query_vm.getWeatherSource();
        locationSource = query_vm.getLocationSource();
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || getClass() != o.getClass()) {
            return false;
        } else {
            LocationData locData = (LocationData) o;
            return this.hashCode() == locData.hashCode();
        }
    }

    @Override
    public int hashCode() {
        int hashCode = -19042156;
        long temp;
        hashCode = hashCode * -1521134295 + (query != null ? query.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (name != null ? name.hashCode() : 0);
        temp = Double.doubleToLongBits(latitude);
        hashCode = hashCode * -1521134295 + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        hashCode = hashCode * -1521134295 + (int) (temp ^ (temp >>> 32));
        hashCode = hashCode * -1521134295 + (tzLong != null ? tzLong.hashCode() : 0);
        hashCode = hashCode * -1521134295 + locationType.hashCode();
        hashCode = hashCode * -1521134295 + (weatherSource != null ? weatherSource.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (locationSource != null ? locationSource.hashCode() : 0);
        return hashCode;
    }

    public static LocationData fromJson(JsonReader reader) {
        LocationData obj = null;

        try {
            obj = new LocationData();

            while (reader.hasNext() && reader.peek() != JsonToken.END_OBJECT) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }

                switch (property) {
                    case "query":
                        obj.query = reader.nextString();
                        break;
                    case "name":
                        obj.name = reader.nextString();
                        break;
                    case "latitude":
                        obj.latitude = Double.valueOf(reader.nextString());
                        break;
                    case "longitude":
                        obj.longitude = Double.valueOf(reader.nextString());
                        break;
                    case "tz_long":
                        obj.tzLong = reader.nextString();
                        break;
                    case "locationType":
                        obj.locationType = LocationType.valueOf(Integer.valueOf(reader.nextString()));
                        break;
                    case "source":
                        obj.weatherSource = reader.nextString();
                        break;
                    case "locsource":
                        obj.locationSource = reader.nextString();
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

            // "query" : ""
            writer.name("query");
            writer.value(query);

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

            // "locationType" : ""
            writer.name("locationType");
            writer.value(locationType.getValue());

            // "source" : ""
            writer.name("source");
            writer.value(weatherSource);

            // "locsource" : ""
            writer.name("locsource");
            writer.value(locationSource);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "LocationData: error writing json string");
        }

        return sw.toString();
    }

    public boolean isValid() {
        return !StringUtils.isNullOrWhitespace(query) && !StringUtils.isNullOrWhitespace(weatherSource) && !StringUtils.isNullOrWhitespace(locationSource);
    }

    @Override
    public String toString() {
        return String.format("%s|%s|%s", this.query, this.name, this.locationType.toString());
    }
}