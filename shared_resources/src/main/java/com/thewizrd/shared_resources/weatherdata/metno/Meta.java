package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Meta {

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("units")
    private Units units;

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUnits(Units units) {
        this.units = units;
    }

    public Units getUnits() {
        return units;
    }
}