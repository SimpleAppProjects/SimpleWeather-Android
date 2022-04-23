package com.thewizrd.weather_api.here.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class ForecastItem {

    @SerializedName("precipitationProbability")
    private String precipitationProbability;

    @SerializedName("beaufortDescription")
    private String beaufortDescription;

    @SerializedName("icon")
    private String icon;

    @SerializedName("weekday")
    private String weekday;

    @SerializedName("description")
    private String description;

    @SerializedName("windDesc")
    private String windDesc;

    @SerializedName("airInfo")
    private String airInfo;

    @SerializedName("lowTemperature")
    private String lowTemperature;

    @SerializedName("airDescription")
    private String airDescription;

    @SerializedName("iconLink")
    private String iconLink;

    @SerializedName("dayOfWeek")
    private String dayOfWeek;

    @SerializedName("uvDesc")
    private String uvDesc;

    @SerializedName("precipitationDesc")
    private String precipitationDesc;

    @SerializedName("utcTime")
    private String utcTime;

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("barometerPressure")
    private String barometerPressure;

    @SerializedName("windDirection")
    private String windDirection;

    @SerializedName("windSpeed")
    private String windSpeed;

    @SerializedName("windDescShort")
    private String windDescShort;

    @SerializedName("skyInfo")
    private String skyInfo;

    @SerializedName("skyDescription")
    private String skyDescription;

    @SerializedName("temperatureDesc")
    private String temperatureDesc;

    @SerializedName("iconName")
    private String iconName;

    @SerializedName("highTemperature")
    private String highTemperature;

    @SerializedName("dewPoint")
    private String dewPoint;

    @SerializedName("comfort")
    private String comfort;

    @SerializedName("rainFall")
    private String rainFall;

    @SerializedName("snowFall")
    private String snowFall;

    @SerializedName("daylight")
    private String daylight;

    @SerializedName("uvIndex")
    private String uvIndex;

    @SerializedName("beaufortScale")
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