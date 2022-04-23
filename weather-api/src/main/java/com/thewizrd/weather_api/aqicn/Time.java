package com.thewizrd.weather_api.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Time {

    @SerializedName("s")
    private String S;

    @SerializedName("iso")
    private String iso;

    @SerializedName("tz")
    private String tz;

    @SerializedName("v")
    private long V;

    public void setS(String S) {
        this.S = S;
    }

    public String getS() {
        return S;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getIso() {
        return iso;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public String getTz() {
        return tz;
    }

    public void setV(long V) {
        this.V = V;
    }

    public long getV() {
        return V;
    }
}