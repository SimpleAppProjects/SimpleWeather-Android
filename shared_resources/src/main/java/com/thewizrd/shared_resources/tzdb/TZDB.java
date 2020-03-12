package com.thewizrd.shared_resources.tzdb;

import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "tzdb", primaryKeys = {"latitude", "longitude"})
public class TZDB {
    @ColumnInfo(index = true)
    private double latitude;
    @ColumnInfo(index = true)
    private double longitude;
    @ColumnInfo(name = "tz_long")
    private String tzLong;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    TZDB() {
    }

    public TZDB(double latitude, double longitude, String tz_long) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.tzLong = tz_long;
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

    public String getTzLong() {
        return tzLong;
    }

    public void setTzLong(String tzLong) {
        this.tzLong = tzLong;
    }
}
