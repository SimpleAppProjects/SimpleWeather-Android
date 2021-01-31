package com.thewizrd.simpleweather.radar.rainviewer;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Radar {

    @SerializedName("past")
    private List<RadarItem> past;

    @SerializedName("nowcast")
    private List<RadarItem> nowcast;

    public void setPast(List<RadarItem> past) {
        this.past = past;
    }

    public List<RadarItem> getPast() {
        return past;
    }

    public void setNowcast(List<RadarItem> nowcast) {
        this.nowcast = nowcast;
    }

    public List<RadarItem> getNowcast() {
        return nowcast;
    }
}