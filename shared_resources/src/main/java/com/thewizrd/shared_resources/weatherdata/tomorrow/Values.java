package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Values {

    @SerializedName("precipitationProbability")
    private Integer precipitationProbability;

    @SerializedName("snowAccumulation")
    private Float snowAccumulation;

    @SerializedName("visibility")
    private Float visibility;

    @SerializedName("windGust")
    private Float windGust;

    @SerializedName("precipitationIntensity")
    private Float precipitationIntensity;

    @SerializedName("temperatureApparent")
    private Float temperatureApparent;

    @SerializedName("weatherCode")
    private Integer weatherCode;

    @SerializedName("cloudCover")
    private Float cloudCover;

    @SerializedName("dewPoint")
    private Float dewPoint;

    @SerializedName("sunsetTime")
    private String sunsetTime;

    @SerializedName("temperature")
    private Float temperature;

    @SerializedName("temperatureMin")
    private Float temperatureMin;

    @SerializedName("temperatureMax")
    private Float temperatureMax;

    @SerializedName("humidity")
    private Float humidity;

    @SerializedName("windDirection")
    private Float windDirection;

    @SerializedName("windSpeed")
    private Float windSpeed;

    @SerializedName("moonPhase")
    private Integer moonPhase;

    @SerializedName("pressureSeaLevel")
    private Float pressureSeaLevel;

    @SerializedName("sunriseTime")
    private String sunriseTime;

    @SerializedName("grassIndex")
    private Integer grassIndex;

    @SerializedName("epaIndex")
    private Integer epaIndex;

    @SerializedName("treeIndex")
    private Integer treeIndex;

    @SerializedName("weedIndex")
    private Integer weedIndex;

    public void setPrecipitationProbability(Integer precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public Integer getPrecipitationProbability() {
        return precipitationProbability;
    }

    public void setSnowAccumulation(Float snowAccumulation) {
        this.snowAccumulation = snowAccumulation;
    }

    public Float getSnowAccumulation() {
        return snowAccumulation;
    }

    public void setVisibility(Float visibility) {
        this.visibility = visibility;
    }

    public Float getVisibility() {
        return visibility;
    }

    public void setWindGust(Float windGust) {
        this.windGust = windGust;
    }

    public Float getWindGust() {
        return windGust;
    }

    public void setPrecipitationIntensity(Float precipitationIntensity) {
        this.precipitationIntensity = precipitationIntensity;
    }

    public Float getPrecipitationIntensity() {
        return precipitationIntensity;
    }

    public void setTemperatureApparent(Float temperatureApparent) {
        this.temperatureApparent = temperatureApparent;
    }

    public Float getTemperatureApparent() {
        return temperatureApparent;
    }

    public void setWeatherCode(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public void setCloudCover(Float cloudCover) {
        this.cloudCover = cloudCover;
    }

    public Float getCloudCover() {
        return cloudCover;
    }

    public void setDewPoint(Float dewPoint) {
        this.dewPoint = dewPoint;
    }

    public Float getDewPoint() {
        return dewPoint;
    }

    public void setSunsetTime(String sunsetTime) {
        this.sunsetTime = sunsetTime;
    }

    public String getSunsetTime() {
        return sunsetTime;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Float getTemperature() {
        return temperature;
    }

    public Float getTemperatureMin() {
        return temperatureMin;
    }

    public void setTemperatureMin(Float temperatureMin) {
        this.temperatureMin = temperatureMin;
    }

    public Float getTemperatureMax() {
        return temperatureMax;
    }

    public void setTemperatureMax(Float temperatureMax) {
        this.temperatureMax = temperatureMax;
    }

    public void setHumidity(Float humidity) {
        this.humidity = humidity;
    }

    public Float getHumidity() {
        return humidity;
    }

    public void setWindDirection(Float windDirection) {
        this.windDirection = windDirection;
    }

    public Float getWindDirection() {
        return windDirection;
    }

    public void setWindSpeed(Float windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Float getWindSpeed() {
        return windSpeed;
    }

    public void setMoonPhase(Integer moonPhase) {
        this.moonPhase = moonPhase;
    }

    public Integer getMoonPhase() {
        return moonPhase;
    }

    public void setPressureSeaLevel(Float pressureSeaLevel) {
        this.pressureSeaLevel = pressureSeaLevel;
    }

    public Float getPressureSeaLevel() {
        return pressureSeaLevel;
    }

    public void setSunriseTime(String sunriseTime) {
        this.sunriseTime = sunriseTime;
    }

    public String getSunriseTime() {
        return sunriseTime;
    }

    public void setGrassIndex(Integer grassIndex) {
        this.grassIndex = grassIndex;
    }

    public Integer getGrassIndex() {
        return grassIndex;
    }

    public void setEpaIndex(Integer epaIndex) {
        this.epaIndex = epaIndex;
    }

    public Integer getEpaIndex() {
        return epaIndex;
    }

    public void setTreeIndex(Integer treeIndex) {
        this.treeIndex = treeIndex;
    }

    public Integer getTreeIndex() {
        return treeIndex;
    }

    public void setWeedIndex(Integer weedIndex) {
        this.weedIndex = weedIndex;
    }

    public Integer getWeedIndex() {
        return weedIndex;
    }
}