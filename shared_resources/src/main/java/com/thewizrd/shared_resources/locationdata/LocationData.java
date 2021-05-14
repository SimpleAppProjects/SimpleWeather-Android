package com.thewizrd.shared_resources.locationdata;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ibm.icu.util.TimeZone;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.model.LocationType;
import com.thewizrd.shared_resources.weatherdata.model.Weather;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(tableName = "locations")
public class LocationData extends CustomJsonObject {
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
            if (zId != null) {
                return ZonedDateTime.now(zId).format(
                        DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.TIMEZONE_NAME)
                );
            }
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
        if (SettingsManager.Companion.isLoaded()) {
            final SettingsManager settingsMgr = SimpleLibrary.getInstance().getApp().getSettingsManager();
            weatherSource = settingsMgr.getAPI();
        }
    }

    @Ignore
    public LocationData(@NonNull Weather weather) {
        query = weather.getQuery();
        name = weather.getLocation().getName();
        latitude = weather.getLocation().getLatitude();
        longitude = weather.getLocation().getLongitude();
        tzLong = weather.getLocation().getTzLong();
        weatherSource = weather.getSource();
    }

    @Ignore
    public LocationData(@NonNull LocationQueryViewModel query_vm) {
        query = query_vm.getLocationQuery();
        name = query_vm.getLocationName();
        latitude = query_vm.getLocationLat();
        longitude = query_vm.getLocationLong();
        tzLong = query_vm.getLocationTZLong();
        weatherSource = query_vm.getWeatherSource();
        locationSource = query_vm.getLocationSource();
    }

    @Ignore
    public LocationData(@NonNull LocationQueryViewModel query_vm, @NonNull Location location) {
        setData(query_vm, location);
    }

    public void setData(@NonNull LocationQueryViewModel query_vm, @NonNull Location location) {
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

    @Override
    public void fromJson(JsonReader reader) {
        try {
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
                        this.query = reader.nextString();
                        break;
                    case "name":
                        this.name = reader.nextString();
                        break;
                    case "latitude":
                        this.latitude = Double.parseDouble(reader.nextString());
                        break;
                    case "longitude":
                        this.longitude = Double.parseDouble(reader.nextString());
                        break;
                    case "tz_long":
                        this.tzLong = reader.nextString();
                        break;
                    case "locationType":
                        this.locationType = LocationType.valueOf(Integer.parseInt(reader.nextString()));
                        break;
                    case "source":
                        this.weatherSource = reader.nextString();
                        break;
                    case "locsource":
                        this.locationSource = reader.nextString();
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
    }

    public boolean isValid() {
        return !StringUtils.isNullOrWhitespace(query) && !StringUtils.isNullOrWhitespace(weatherSource) && !StringUtils.isNullOrWhitespace(locationSource);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s|%s|%s", this.query, this.name, this.locationType.toString());
    }
}