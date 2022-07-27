package com.thewizrd.shared_resources.weatherdata.model;

import com.squareup.moshi.Json;
import com.thewizrd.shared_resources.utils.CustomJsonObject;

public abstract class BaseForecast extends CustomJsonObject {

    @Json(name = "high_f")
    protected Float highF;

    @Json(name = "high_c")
    protected Float highC;

    @Json(name = "condition")
    protected String condition;

    @Json(name = "icon")
    protected String icon;

    @Json(name = "extras")
    protected ForecastExtras extras;

    public Float getHighF() {
        return highF;
    }

    public void setHighF(Float highF) {
        this.highF = highF;
    }

    public Float getHighC() {
        return highC;
    }

    public void setHighC(Float highC) {
        this.highC = highC;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public ForecastExtras getExtras() {
        return extras;
    }

    public void setExtras(ForecastExtras extras) {
        this.extras = extras;
    }
}
