package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Main {

    @SerializedName("temp")
    private float temp;

    @SerializedName("temp_min")
    private float tempMin;

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("pressure")
    private float pressure;

    @SerializedName("temp_max")
    private float tempMax;

    @SerializedName("sea_level")
    private float seaLevel;

    @SerializedName("grnd_level")
    private float grndLevel;

    @SerializedName("temp_kf")
    private float tempKf;

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public float getTemp() {
        return temp;
    }

    public void setTempMin(float tempMin) {
        this.tempMin = tempMin;
    }

    public float getTempMin() {
        return tempMin;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getHumidity() {
        return humidity + "%";
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getPressure() {
        return pressure;
    }

    public void setTempMax(float tempMax) {
        this.tempMax = tempMax;
    }

    public float getTempMax() {
        return tempMax;
    }

    public float getSeaLevel() {
        return seaLevel;
    }

    public void setSeaLevel(float seaLevel) {
        this.seaLevel = seaLevel;
    }

    public float getGrndLevel() {
        return grndLevel;
    }

    public void setGrndLevel(float grndLevel) {
        this.grndLevel = grndLevel;
    }

    public float getTempKf() {
        return tempKf;
    }

    public void setTempKf(float tempKf) {
        this.tempKf = tempKf;
    }
}