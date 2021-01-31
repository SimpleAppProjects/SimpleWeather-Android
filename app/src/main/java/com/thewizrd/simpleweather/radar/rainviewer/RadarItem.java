package com.thewizrd.simpleweather.radar.rainviewer;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class RadarItem {

    @SerializedName("path")
    private String path;

    @SerializedName("time")
    private int time;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }
}