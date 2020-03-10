package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class ObservationItem {

    @SerializedName("barometerTrend")
    private String barometerTrend;

    @SerializedName("country")
    private String country;

    @SerializedName("ageMinutes")
    private String ageMinutes;

    @SerializedName("distance")
    private float distance;

    @SerializedName("city")
    private String city;

    @SerializedName("latitude")
    private float latitude;

    @SerializedName("icon")
    private String icon;

    @SerializedName("description")
    private String description;

    @SerializedName("windDesc")
    private String windDesc;

    @SerializedName("airInfo")
    private String airInfo;

    @SerializedName("lowTemperature")
    private String lowTemperature;

    @SerializedName("precipitation6H")
    private String precipitation6H;

    @SerializedName("airDescription")
    private String airDescription;

    @SerializedName("iconLink")
    private String iconLink;

    @SerializedName("precipitationDesc")
    private String precipitationDesc;

    @SerializedName("utcTime")
    private String utcTime;

    @SerializedName("temperature")
    private String temperature;

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("barometerPressure")
    private String barometerPressure;

    @SerializedName("windDirection")
    private String windDirection;

    @SerializedName("state")
    private String state;

    @SerializedName("windSpeed")
    private String windSpeed;

    @SerializedName("windDescShort")
    private String windDescShort;

    @SerializedName("longitude")
    private float longitude;

    @SerializedName("skyInfo")
    private String skyInfo;

    @SerializedName("skyDescription")
    private String skyDescription;

    @SerializedName("temperatureDesc")
    private String temperatureDesc;

    @SerializedName("elevation")
    private float elevation;

    @SerializedName("precipitation3H")
    private String precipitation3H;

    @SerializedName("precipitation1H")
    private String precipitation1H;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("iconName")
    private String iconName;

    @SerializedName("highTemperature")
    private String highTemperature;

    @SerializedName("dewPoint")
    private String dewPoint;

    @SerializedName("comfort")
    private String comfort;

    @SerializedName("snowCover")
    private String snowCover;

    @SerializedName("daylight")
    private String daylight;

    @SerializedName("precipitation12H")
    private String precipitation12H;

    @SerializedName("activeAlerts")
    private String activeAlerts;

    @SerializedName("precipitation24H")
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