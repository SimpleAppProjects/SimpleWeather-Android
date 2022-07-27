package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class TimeZone {

    @Json(name = "nameDstShort")
    private String nameDstShort;

    @Json(name = "offset")
    private int offset;

    @Json(name = "dstSavings")
    private int dstSavings;

    @Json(name = "rawOffset")
    private int rawOffset;

    @Json(name = "nameLong")
    private String nameLong;

    @Json(name = "nameDstLong")
    private String nameDstLong;

    @Json(name = "id")
    private String id;

    @Json(name = "nameShort")
    private String nameShort;

    @Json(name = "inDaylightTime")
    private boolean inDaylightTime;

    public void setNameDstShort(String nameDstShort) {
        this.nameDstShort = nameDstShort;
    }

    public String getNameDstShort() {
        return nameDstShort;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public void setDstSavings(int dstSavings) {
        this.dstSavings = dstSavings;
    }

    public int getDstSavings() {
        return dstSavings;
    }

    public void setRawOffset(int rawOffset) {
        this.rawOffset = rawOffset;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    public void setNameLong(String nameLong) {
        this.nameLong = nameLong;
    }

    public String getNameLong() {
        return nameLong;
    }

    public void setNameDstLong(String nameDstLong) {
        this.nameDstLong = nameDstLong;
    }

    public String getNameDstLong() {
        return nameDstLong;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setNameShort(String nameShort) {
        this.nameShort = nameShort;
    }

    public String getNameShort() {
        return nameShort;
    }

    public void setInDaylightTime(boolean inDaylightTime) {
        this.inDaylightTime = inDaylightTime;
    }

    public boolean isInDaylightTime() {
        return inDaylightTime;
    }
}