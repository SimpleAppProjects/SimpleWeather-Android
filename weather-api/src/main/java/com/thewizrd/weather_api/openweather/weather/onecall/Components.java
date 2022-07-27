package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Components {

    @Json(name = "no2")
    private Double no2;

    @Json(name = "no")
    private Double no;

    @Json(name = "o3")
    private Double o3;

    @Json(name = "so2")
    private Double so2;

    @Json(name = "pm2_5")
    private Double pm25;

    @Json(name = "pm10")
    private Double pm10;

    @Json(name = "nh3")
    private Double nh3;

    @Json(name = "co")
    private Double co;

    public void setNo2(Double no2) {
        this.no2 = no2;
    }

    public Double getNo2() {
        return no2;
    }

    public void setNo(Double no) {
        this.no = no;
    }

    public Double getNo() {
        return no;
    }

    public void setO3(Double o3) {
        this.o3 = o3;
    }

    public Double getO3() {
        return o3;
    }

    public void setSo2(Double so2) {
        this.so2 = so2;
    }

    public Double getSo2() {
        return so2;
    }

    public void setPm25(Double pm25) {
        this.pm25 = pm25;
    }

    public Double getPm25() {
        return pm25;
    }

    public void setPm10(Double pm10) {
        this.pm10 = pm10;
    }

    public Double getPm10() {
        return pm10;
    }

    public void setNh3(Double nh3) {
        this.nh3 = nh3;
    }

    public Double getNh3() {
        return nh3;
    }

    public void setCo(Double co) {
        this.co = co;
    }

    public Double getCo() {
        return co;
    }
}