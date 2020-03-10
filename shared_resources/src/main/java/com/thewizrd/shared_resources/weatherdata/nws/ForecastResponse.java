package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class ForecastResponse {

    @SerializedName("validTimes")
    private String validTimes;

    @SerializedName("generatedAt")
    private String generatedAt;

    @SerializedName("periods")
    private List<PeriodsItem> periods;

    @SerializedName("updateTime")
    private String updateTime;

    @SerializedName("units")
    private String units;

    @SerializedName("updated")
    private String updated;

    public void setValidTimes(String validTimes) {
        this.validTimes = validTimes;
    }

    public String getValidTimes() {
        return validTimes;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setPeriods(List<PeriodsItem> periods) {
        this.periods = periods;
    }

    public List<PeriodsItem> getPeriods() {
        return periods;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getUnits() {
        return units;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getUpdated() {
        return updated;
    }
}