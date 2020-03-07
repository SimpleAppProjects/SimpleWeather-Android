package com.thewizrd.shared_resources.weatherdata;

import com.google.gson.annotations.SerializedName;

public abstract class BaseForecast {

    @SerializedName("date")
    protected String _date;

    @SerializedName("high_f")
    protected String highF;

    @SerializedName("high_c")
    protected String highC;

    @SerializedName("condition")
    protected String condition;

    @SerializedName("icon")
    protected String icon;

    @SerializedName("pop")
    protected String pop;

    @SerializedName("extras")
    protected ForecastExtras extras;

    public String getHighF() {
        return highF;
    }

    public void setHighF(String highF) {
        this.highF = highF;
    }

    public String getHighC() {
        return highC;
    }

    public void setHighC(String highC) {
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

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public ForecastExtras getExtras() {
        return extras;
    }

    public void setExtras(ForecastExtras extras) {
        this.extras = extras;
    }
}
