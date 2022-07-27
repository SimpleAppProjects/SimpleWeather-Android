package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class ObservationItem {

    @Json(name = "barometerTrend")
    private String barometerTrend;

    @Json(name = "country")
    private String country;

    @Json(name = "ageMinutes")
    private String ageMinutes;

    @Json(name = "distance")
    private float distance;

    @Json(name = "city")
    private String city;

    @Json(name = "latitude")
    private float latitude;

    @Json(name = "icon")
    private String icon;

    @Json(name = "description")
    private String description;

    @Json(name = "windDesc")
    private String windDesc;

    @Json(name = "airInfo")
    private String airInfo;

    @Json(name = "lowTemperature")
    private String lowTemperature;

    @Json(name = "precipitation6H")
    private String precipitation6H;

    @Json(name = "airDescription")
    private String airDescription;

    @Json(name = "iconLink")
    private String iconLink;

    @Json(name = "precipitationDesc")
    private String precipitationDesc;

    @Json(name = "utcTime")
    private String utcTime;

    @Json(name = "temperature")
    private String temperature;

    @Json(name = "humidity")
    private String humidity;

    @Json(name = "barometerPressure")
    private String barometerPressure;

    @Json(name = "windDirection")
    private String windDirection;

    @Json(name = "state")
    private String state;

    @Json(name = "windSpeed")
    private String windSpeed;

    @Json(name = "windDescShort")
    private String windDescShort;

    @Json(name = "longitude")
    private float longitude;

    @Json(name = "skyInfo")
    private String skyInfo;

    @Json(name = "skyDescription")
    private String skyDescription;

    @Json(name = "temperatureDesc")
    private String temperatureDesc;

    @Json(name = "elevation")
    private float elevation;

    @Json(name = "precipitation3H")
    private String precipitation3H;

    @Json(name = "precipitation1H")
    private String precipitation1H;

    @Json(name = "visibility")
    private String visibility;

    @Json(name = "iconName")
    private String iconName;

    @Json(name = "highTemperature")
    private String highTemperature;

    @Json(name = "dewPoint")
    private String dewPoint;

    @Json(name = "comfort")
    private String comfort;

    @Json(name = "snowCover")
    private String snowCover;

    @Json(name = "daylight")
    private String daylight;

    @Json(name = "precipitation12H")
    private String precipitation12H;

    @Json(name = "activeAlerts")
    private String activeAlerts;

    @Json(name = "precipitation24H")
    private String precipitation24H;

    public void setBarometerTrend(String barometerTrend) {
        this.barometerTrend = barometerTrend;
    }

    public String getBarometerTrend() {
        return barometerTrend;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setAgeMinutes(String ageMinutes) {
        this.ageMinutes = ageMinutes;
    }

    public String getAgeMinutes() {
        return ageMinutes;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDistance() {
        return distance;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
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

    public void setPrecipitation6H(String precipitation6H) {
        this.precipitation6H = precipitation6H;
    }

    public String getPrecipitation6H() {
        return precipitation6H;
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

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getTemperature() {
        return temperature;
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

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
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

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLongitude() {
        return longitude;
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

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public float getElevation() {
        return elevation;
    }

    public void setPrecipitation3H(String precipitation3H) {
        this.precipitation3H = precipitation3H;
    }

    public String getPrecipitation3H() {
        return precipitation3H;
    }

    public void setPrecipitation1H(String precipitation1H) {
        this.precipitation1H = precipitation1H;
    }

    public String getPrecipitation1H() {
        return precipitation1H;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVisibility() {
        return visibility;
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

    public void setSnowCover(String snowCover) {
        this.snowCover = snowCover;
    }

    public String getSnowCover() {
        return snowCover;
    }

    public void setDaylight(String daylight) {
        this.daylight = daylight;
    }

    public String getDaylight() {
        return daylight;
    }

    public void setPrecipitation12H(String precipitation12H) {
        this.precipitation12H = precipitation12H;
    }

    public String getPrecipitation12H() {
        return precipitation12H;
    }

    public void setActiveAlerts(String activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public String getActiveAlerts() {
        return activeAlerts;
    }

    public void setPrecipitation24H(String precipitation24H) {
        this.precipitation24H = precipitation24H;
    }

    public String getPrecipitation24H() {
        return precipitation24H;
    }
}