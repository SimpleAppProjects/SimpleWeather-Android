package com.thewizrd.shared_resources.weatherdata.openweather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Snow {

    @SerializedName("1h")
    private float _1h;

    public float get_1h() {
        return _1h;
    }

    public void set_1h(float _1h) {
        this._1h = _1h;
    }
}
