package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Values {

    @Json(name = "precipitationProbability")
    private Float precipitationProbability;

    @Json(name = "snowAccumulation")
    private Float snowAccumulation;

    @Json(name = "visibility")
    private Float visibility;

    @Json(name = "windGust")
    private Float windGust;

    @Json(name = "precipitationIntensity")
    private Float precipitationIntensity;

    @Json(name = "temperatureApparent")
    private Float temperatureApparent;

    @Json(name = "weatherCode")
    private Integer weatherCode;

    @Json(name = "cloudCover")
    private Float cloudCover;

    @Json(name = "dewPoint")
    private Float dewPoint;

    @Json(name = "sunsetTime")
    private String sunsetTime;

    @Json(name = "temperature")
    private Float temperature;

    @Json(name = "temperatureMin")
    private Float temperatureMin;

    @Json(name = "temperatureMax")
    private Float temperatureMax;

    @Json(name = "humidity")
    private Float humidity;

    @Json(name = "windDirection")
    private Float windDirection;

    @Json(name = "windSpeed")
    private Float windSpeed;

    @Json(name = "moonPhase")
    private Integer moonPhase;

    @Json(name = "pressureSeaLevel")
    private Float pressureSeaLevel;

    @Json(name = "sunriseTime")
    private String sunriseTime;

    @Json(name = "grassIndex")
    private Integer grassIndex;

    @Json(name = "epaIndex")
    private Integer epaIndex;

    @Json(name = "treeIndex")
    private Integer treeIndex;

    @Json(name = "weedIndex")
    private Integer weedIndex;

    @Json(name = "weatherCodeDay")
    private Integer weatherCodeDay;

    @Json(name = "weatherCodeFullDay")
    private Integer weatherCodeFullDay;

    @Json(name = "weatherCodeNight")
    private Integer weatherCodeNight;

    @Json(name = "particulateMatter25")
    private Double particulateMatter25;

    @Json(name = "pollutantCO")
    private Double pollutantCO;

    @Json(name = "pollutantNO2")
    private Double pollutantNO2;

    @Json(name = "pollutantSO2")
    private Double pollutantSO2;

    @Json(name = "pollutantO3")
    private Double pollutantO3;

    @Json(name = "particulateMatter10")
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