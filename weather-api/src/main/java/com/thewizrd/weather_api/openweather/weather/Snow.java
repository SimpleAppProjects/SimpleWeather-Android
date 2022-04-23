package com.thewizrd.weather_api.openweather.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Snow {

    @SerializedName("1h")
    private Float _1h;

    @SerializedName("3h")
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
