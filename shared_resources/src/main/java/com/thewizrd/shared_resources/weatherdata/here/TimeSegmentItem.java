package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class TimeSegmentItem {

    @SerializedName("segment")
    private String segment;

    @SerializedName("value")
    private String value;

    //@SerializedName("otherAttributes")
    //private OtherAttributes otherAttributes;

    @SerializedName("day_of_week")
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