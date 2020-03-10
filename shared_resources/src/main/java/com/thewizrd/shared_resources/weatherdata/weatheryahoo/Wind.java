package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Wind {

    @SerializedName("chill")
    private String chill;

    @SerializedName("speed")
    private String speed;

    @SerializedName("direction")
    private String direction;

    public void setChill(String chill) {
        this.chill = chill;
    }

    public String getChill() {
        return chill;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getSpeed() {
        return speed;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }
}