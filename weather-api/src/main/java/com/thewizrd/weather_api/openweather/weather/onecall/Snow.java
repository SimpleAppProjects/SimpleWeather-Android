package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Snow {

    @Json(name = "1h")
    private float _1h;

    public float get_1h() {
        return _1h;
    }

    public void set_1h(float _1h) {
        this._1h = _1h;
    }
}
