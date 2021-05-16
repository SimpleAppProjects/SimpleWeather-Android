package com.thewizrd.shared_resources.weatherdata.openweather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Components {

    @SerializedName("no2")
    private double no2;

    @SerializedName("no")
    private double no;

    @SerializedName("o3")
    private double o3;

    @SerializedName("so2")
    private double so2;

    @SerializedName("pm2_5")
    private double pm25;

    @SerializedName("pm10")
    private double pm10;

    @SerializedName("nh3")
    private double nh3;

    @SerializedName("co")
    private double co;

    public void setNo2(double no2) {
        this.no2 = no2;
    }

    public double getNo2() {
        return no2;
    }

    public void setNo(double no) {
        this.no = no;
    }

    public double getNo() {
        return no;
    }

    public void setO3(double o3) {
        this.o3 = o3;
    }

    public double getO3() {
        return o3;
    }

    public void setSo2(double so2) {
        this.so2 = so2;
    }

    public double getSo2() {
        return so2;
    }

    public void setPm25(double pm25) {
        this.pm25 = pm25;
    }

    public double getPm25() {
        return pm25;
    }

    public void setPm10(double pm10) {
        this.pm10 = pm10;
    }

    public double getPm10() {
        return pm10;
    }

    public void setNh3(double nh3) {
        this.nh3 = nh3;
    }

    public double getNh3() {
        return nh3;
    }

    public void setCo(double co) {
        this.co = co;
    }

    public double getCo() {
        return co;
    }
}