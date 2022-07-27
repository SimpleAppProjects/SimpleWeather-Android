package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Data {

    @Json(name = "instant")
    private Instant instant;

    @Json(name = "next_6_hours")
    private Next6Hours next6Hours;

    @Json(name = "next_12_hours")
    private Next12Hours next12Hours;

    @Json(name = "next_1_hours")
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