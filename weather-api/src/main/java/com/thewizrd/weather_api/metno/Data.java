package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Data {

    @SerializedName("instant")
    private Instant instant;

    @SerializedName("next_6_hours")
    private Next6Hours next6Hours;

    @SerializedName("next_12_hours")
    private Next12Hours next12Hours;

    @SerializedName("next_1_hours")
    private Next1Hours next1Hours;

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setNext6Hours(Next6Hours next6Hours) {
        this.next6Hours = next6Hours;
    }

    public Next6Hours getNext6Hours() {
        return next6Hours;
    }

    public void setNext12Hours(Next12Hours next12Hours) {
        this.next12Hours = next12Hours;
    }

    public Next12Hours getNext12Hours() {
        return next12Hours;
    }

    public void setNext1Hours(Next1Hours next1Hours) {
        this.next1Hours = next1Hours;
    }

    public Next1Hours getNext1Hours() {
        return next1Hours;
    }
}