package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class ForecastItem {

    @Json(name = "precipitationProbability")
    private String precipitationProbability;

    @Json(name = "beaufortDescription")
    private String beaufortDescription;

    @Json(name = "icon")
    private String icon;

    @Json(name = "weekday")
    private String weekday;

    @Json(name = "description")
    private String description;

    @Json(name = "windDesc")
    private String windDesc;

    @Json(name = "airInfo")
    private String airInfo;

    @Json(name = "lowTemperature")
    private String lowTemperature;

    @Json(name = "airDescription")
    private String airDescription;

    @Json(name = "iconLink")
    private String iconLink;

    @Json(name = "dayOfWeek")
    private String dayOfWeek;

    @Json(name = "uvDesc")
    private String uvDesc;

    @Json(name = "precipitationDesc")
    private String precipitationDesc;

    @Json(name = "utcTime")
    private String utcTime;

    @Json(name = "humidity")
    private String humidity;

    @Json(name = "barometerPressure")
    private String barometerPressure;

    @Json(name = "windDirection")
    private String windDirection;

    @Json(name = "windSpeed")
    private String windSpeed;

    @Json(name = "windDescShort")
    private String windDescShort;

    @Json(name = "skyInfo")
    private String skyInfo;

    @Json(name = "skyDescription")
    private String skyDescription;

    @Json(name = "temperatureDesc")
    private String temperatureDesc;

    @Json(name = "iconName")
    private String iconName;

    @Json(name = "highTemperature")
    private String highTemperature;

    @Json(name = "dewPoint")
    private String dewPoint;

    @Json(name = "comfort")
    private String comfort;

    @Json(name = "rainFall")
    private String rainFall;

    @Json(name = "snowFall")
    private String snowFall;

    @Json(name = "daylight")
    private String daylight;

    @Json(name = "uvIndex")
    private String uvIndex;

    @Json(name = "beaufortScale")
    private String beaufortScale;

    public void setPrecipitationProbability(String precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public String getPrecipitationProbability() {
        return precipitationProbability;
    }

    public void setBeaufortDescription(String beaufortDescription) {
        this.beaufortDescription = beaufortDescription;
    }

    public String getBeaufortDescription() {
        return beaufortDescription;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setWeekday(String weekday) {
        this.weekday = weekday;
    }

    public String getWeekday() {
        return weekday;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setWindDesc(String windDesc) {
        this.windDesc = windDesc;
    }

    public String getWindDesc() {
        return windDesc;
    }

    public void setAirInfo(String airInfo) {
        this.airInfo = airInfo;
    }

    public String getAirInfo() {
        return airInfo;
    }

    public void setLowTemperature(String lowTemperature) {
        this.lowTemperature = lowTemperature;
    }

    public String getLowTemperature() {
        return lowTemperature;
    }

    public void setAirDescription(String airDescription) {
        this.airDescription = airDescription;
    }

    public String getAirDescription() {
        return airDescription;
    }

    public void setIconLink(String iconLink) {
        this.iconLink = iconLink;
    }

    public String getIconLink() {
        return iconLink;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setUvDesc(String uvDesc) {
        this.uvDesc = uvDesc;
    }

    public String getUvDesc() {
        return uvDesc;
    }

    public void setPrecipitationDesc(String precipitationDesc) {
        this.precipitationDesc = precipitationDesc;
    }

    public String getPrecipitationDesc() {
        return precipitationDesc;
    }

    public void setUtcTime(String utcTime) {
        this.utcTime = utcTime;
    }

    public String getUtcTime() {
        return utcTime;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setBarometerPressure(String barometerPressure) {
        this.barometerPressure = barometerPressure;
    }

    public String getBarometerPressure() {
        return barometerPressure;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindDescShort(String windDescShort) {
        this.windDescShort = windDescShort;
    }

    public String getWindDescShort() {
        return windDescShort;
    }

    public void setSkyInfo(String skyInfo) {
        this.skyInfo = skyInfo;
    }

    public String getSkyInfo() {
        return skyInfo;
    }

    public void setSkyDescription(String skyDescription) {
        this.skyDescription = skyDescription;
    }

    public String getSkyDescription() {
        return skyDescription;
    }

    public void setTemperatureDesc(String temperatureDesc) {
        this.temperatureDesc = temperatureDesc;
    }

    public String getTemperatureDesc() {
        return temperatureDesc;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getIconName() {
        return iconName;
    }

    public void setHighTemperature(String highTemperature) {
        this.highTemperature = highTemperature;
    }

    public String getHighTemperature() {
        return highTemperature;
    }

    public void setDewPoint(String dewPoint) {
        this.dewPoint = dewPoint;
    }

    public String getDewPoint() {
        return dewPoint;
    }

    public void setComfort(String comfort) {
        this.comfort = comfort;
    }

    public String getComfort() {
        return comfort;
    }

    public void setRainFall(String rainFall) {
        this.rainFall = rainFall;
    }

    public String getRainFall() {
        return rainFall;
    }

    public void setSnowFall(String snowFall) {
        this.snowFall = snowFall;
    }

    public String getSnowFall() {
        return snowFall;
    }

    public void setDaylight(String daylight) {
        this.daylight = daylight;
    }

    public String getDaylight() {
        return daylight;
    }

    public void setUvIndex(String uvIndex) {
        this.uvIndex = uvIndex;
    }

    public String getUvIndex() {
        return uvIndex;
    }

    public void setBeaufortScale(String beaufortScale) {
        this.beaufortScale = beaufortScale;
    }

    public String getBeaufortScale() {
        return beaufortScale;
    }
}