package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Rain {

    @Json(name = "1h")
    private Float _1h;

    @Json(name = "3h")
    private Float _3h;

    public Float get_1h() {
        return _1h;
    }

    public void set_1h(Float _1h) {
        this._1h = _1h;
    }

    public Float get_3h() {
        return _3h;
    }

    public void set_3h(Float _3h) {
        this._3h = _3h;
    }
}
