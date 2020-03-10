package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Rain {

    @SerializedName("3h")
    private float _3h;

    public float get_3h() {
        return _3h;
    }

    public void set_3h(float _3h) {
        this._3h = _3h;
    }

}
