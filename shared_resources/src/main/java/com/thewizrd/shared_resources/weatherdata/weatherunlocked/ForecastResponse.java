package com.thewizrd.shared_resources.weatherdata.weatherunlocked;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class ForecastResponse {

    @SerializedName("Days")
    private List<DaysItem> days;

    public void setDays(List<DaysItem> days) {
        this.days = days;
    }

    public List<DaysItem> getDays() {
        return days;
    }
}