package com.thewizrd.weather_api.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Values {

    @SerializedName("precipitationProbability")
    private Float precipitationProbability;

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

    @SerializedName("weatherCodeDay")
    private Integer weatherCodeDay;

    @SerializedName("weatherCodeFullDay")
    private Integer weatherCodeFullDay;

    @SerializedName("weatherCodeNight")
    private Integer weatherCodeNight;

    @SerializedName("particulateMatter25")
    private Double particulateMatter25;

    @SerializedName("pollutantCO")
    private Double pollutantCO;

    @SerializedName("pollutantNO2")
    private Double pollutantNO2;

    @SerializedName("pollutantSO2")
    private Double pollutantSO2;

    @SerializedName("pollutantO3")
    private Double pollutantO3;

    @SerializedName("particulateMatter10")
    private Double particulateMatter10;

    public void setPrecipitationProbability(Float precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public Float getPrecipitationProbability() {
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

    public Integer getWeatherCodeDay() {
        return weatherCodeDay;
    }

    public void setWeatherCodeDay(Integer weatherCodeDay) {
        this.weatherCodeDay = weatherCodeDay;
    }

    public Integer getWeatherCodeFullDay() {
        return weatherCodeFullDay;
    }

    public void setWeatherCodeFullDay(Integer weatherCodeFullDay) {
        this.weatherCodeFullDay = weatherCodeFullDay;
    }

    public Integer getWeatherCodeNight() {
        return weatherCodeNight;
    }

    public void setWeatherCodeNight(Integer weatherCodeNight) {
        this.weatherCodeNight = weatherCodeNight;
    }

    public Double getParticulateMatter25() {
        return particulateMatter25;
    }

    public void setParticulateMatter25(Double particulateMatter25) {
        this.particulateMatter25 = particulateMatter25;
    }

    public Double getPollutantCO() {
        return pollutantCO;
    }

    public void setPollutantCO(Double pollutantCO) {
        this.pollutantCO = pollutantCO;
    }

    public Double getPollutantNO2() {
        return pollutantNO2;
    }

    public void setPollutantNO2(Double pollutantNO2) {
        this.pollutantNO2 = pollutantNO2;
    }

    public Double getPollutantSO2() {
        return pollutantSO2;
    }

    public void setPollutantSO2(Double pollutantSO2) {
        this.pollutantSO2 = pollutantSO2;
    }

    public Double getPollutantO3() {
        return pollutantO3;
    }

    public void setPollutantO3(Double pollutantO3) {
        this.pollutantO3 = pollutantO3;
    }

    public Double getParticulateMatter10() {
        return particulateMatter10;
    }

    public void setParticulateMatter10(Double particulateMatter10) {
        this.particulateMatter10 = particulateMatter10;
    }
}