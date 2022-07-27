package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Meta {

    @Json(name = "updated_at")
    private String updatedAt;

    @Json(name = "units")
    private Units units;

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUnits(Units units) {
        this.units = units;
    }

    public Units getUnits() {
        return units;
    }
}