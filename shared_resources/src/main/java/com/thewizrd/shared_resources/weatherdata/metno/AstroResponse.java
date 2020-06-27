package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class AstroResponse {

    @SerializedName("meta")
    private AstroMeta meta;

    @SerializedName("location")
    private Location location;

    public void setMeta(AstroMeta meta) {
        this.meta = meta;
    }

    public AstroMeta getMeta() {
        return meta;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}