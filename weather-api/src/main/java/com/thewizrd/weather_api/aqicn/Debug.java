package com.thewizrd.weather_api.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Debug {

    @SerializedName("sync")
    private String sync;

    public void setSync(String sync) {
        this.sync = sync;
    }

    public String getSync() {
        return sync;
    }
}