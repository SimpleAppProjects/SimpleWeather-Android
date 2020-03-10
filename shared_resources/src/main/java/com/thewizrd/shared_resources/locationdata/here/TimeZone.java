package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class TimeZone {

    @SerializedName("nameDstShort")
    private String nameDstShort;

    @SerializedName("offset")
    private int offset;

    @SerializedName("dstSavings")
    private int dstSavings;

    @SerializedName("rawOffset")
    private int rawOffset;

    @SerializedName("nameLong")
    private String nameLong;

    @SerializedName("nameDstLong")
    private String nameDstLong;

    @SerializedName("id")
    private String id;

    @SerializedName("nameShort")
    private String nameShort;

    @SerializedName("inDaylightTime")
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