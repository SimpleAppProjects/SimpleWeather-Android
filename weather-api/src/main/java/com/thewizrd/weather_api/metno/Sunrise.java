package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Sunrise {

    @SerializedName("time")
    private String time;

    @SerializedName("desc")
    private String desc;

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}