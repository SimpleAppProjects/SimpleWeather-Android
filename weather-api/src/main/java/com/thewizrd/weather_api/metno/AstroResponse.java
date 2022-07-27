package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class AstroResponse {

    @Json(name = "meta")
    private AstroMeta meta;

    @Json(name = "location")
    private Location location;

    public void setMeta(AstroMeta meta) {
        this.meta = meta;
    }

    public AstroMeta getMeta() {
        return meta;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}