package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class AstroMeta {

    @Json(name = "licenseurl")
    private String licenseurl;

    public void setLicenseurl(String licenseurl) {
        this.licenseurl = licenseurl;
    }

    public String getLicenseurl() {
        return licenseurl;
    }
}