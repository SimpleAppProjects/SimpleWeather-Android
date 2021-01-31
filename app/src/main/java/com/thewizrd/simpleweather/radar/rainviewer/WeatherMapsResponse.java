package com.thewizrd.simpleweather.radar.rainviewer;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class WeatherMapsResponse {

    @SerializedName("radar")
    private Radar radar;

    @SerializedName("generated")
    private int generated;

    @SerializedName("host")
    private String host;

    @SerializedName("version")
    private String version;

    public void setRadar(Radar radar) {
        this.radar = radar;
    }

    public Radar getRadar() {
        return radar;
    }

    public void setGenerated(int generated) {
        this.generated = generated;
    }

    public int getGenerated() {
        return generated;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}