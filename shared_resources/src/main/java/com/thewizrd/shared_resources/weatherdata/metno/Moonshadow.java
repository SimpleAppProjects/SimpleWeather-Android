package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Moonshadow {

    @SerializedName("elevation")
    private String elevation;

    @SerializedName("azimuth")
    private String azimuth;

    @SerializedName("time")
    private String time;

    @SerializedName("desc")
    private String desc;

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getElevation() {
        return elevation;
    }

    public void setAzimuth(String azimuth) {
        this.azimuth = azimuth;
    }

    public String getAzimuth() {
        return azimuth;
    }

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