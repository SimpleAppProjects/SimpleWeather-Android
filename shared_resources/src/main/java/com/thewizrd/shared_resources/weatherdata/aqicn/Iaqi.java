package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

public class Iaqi {

    @SerializedName("no2")
    private No2 no2;

    @SerializedName("p")
    private P P;

    @SerializedName("o3")
    private O3 o3;

    @SerializedName("pm25")
    private Pm25 pm25;

    @SerializedName("t")
    private T T;

    @SerializedName("so2")
    private So2 so2;

    @SerializedName("w")
    private W W;

    @SerializedName("h")
    private H H;

    @SerializedName("pm10")
    private Pm10 pm10;

    @SerializedName("co")
    private Co co;

    public void setNo2(No2 no2) {
        this.no2 = no2;
    }

    public No2 getNo2() {
        return no2;
    }

    public void setP(P P) {
        this.P = P;
    }

    public P getP() {
        return P;
    }

    public void setO3(O3 o3) {
        this.o3 = o3;
    }

    public O3 getO3() {
        return o3;
    }

    public void setPm25(Pm25 pm25) {
        this.pm25 = pm25;
    }

    public Pm25 getPm25() {
        return pm25;
    }

    public void setT(T T) {
        this.T = T;
    }

    public T getT() {
        return T;
    }

    public void setSo2(So2 so2) {
        this.so2 = so2;
    }

    public So2 getSo2() {
        return so2;
    }

    public void setW(W W) {
        this.W = W;
    }

    public W getW() {
        return W;
    }

    public void setH(H H) {
        this.H = H;
    }

    public H getH() {
        return H;
    }

    public void setPm10(Pm10 pm10) {
        this.pm10 = pm10;
    }

    public Pm10 getPm10() {
        return pm10;
    }

    public void setCo(Co co) {
        this.co = co;
    }

    public Co getCo() {
        return co;
    }
}