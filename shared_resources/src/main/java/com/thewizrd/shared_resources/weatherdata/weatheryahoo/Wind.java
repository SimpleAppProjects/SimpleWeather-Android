package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Wind {

    @SerializedName("chill")
    private int chill;

    @SerializedName("speed")
    private float speed;

    @SerializedName("direction")
    private int direction;

    public void setChill(int chill) {
        this.chill = chill;
    }

    public int getChill() {
        return chill;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }
}