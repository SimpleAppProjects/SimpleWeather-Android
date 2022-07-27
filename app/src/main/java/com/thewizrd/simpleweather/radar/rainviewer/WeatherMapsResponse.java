package com.thewizrd.simpleweather.radar.rainviewer;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class WeatherMapsResponse {

    @Json(name = "radar")
    private Radar radar;

    @Json(name = "generated")
    private int generated;

    @Json(name = "host")
    private String host;

    @Json(name = "version")
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