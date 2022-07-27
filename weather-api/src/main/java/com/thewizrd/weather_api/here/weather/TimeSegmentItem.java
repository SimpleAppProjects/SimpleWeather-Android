package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class TimeSegmentItem {

    @Json(name = "segment")
    private String segment;

    @Json(name = "value")
    private String value;

    //@Json(name = "otherAttributes")
    //private OtherAttributes otherAttributes;

    @Json(name = "day_of_week")
    private String dayOfWeek;

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /*
    public void setOtherAttributes(OtherAttributes otherAttributes) {
        this.otherAttributes = otherAttributes;
    }

    public OtherAttributes getOtherAttributes() {
        return otherAttributes;
    }
     */

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }
}