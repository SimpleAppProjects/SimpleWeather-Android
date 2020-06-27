package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Units {

    @SerializedName("cloud_area_fraction_high")
    private String cloudAreaFractionHigh;

    @SerializedName("precipitation_amount")
    private String precipitationAmount;

    @SerializedName("cloud_area_fraction")
    private String cloudAreaFraction;

    @SerializedName("cloud_area_fraction_medium")
    private String cloudAreaFractionMedium;

    @SerializedName("wind_from_direction")
    private String windFromDirection;

    @SerializedName("air_temperature_min")
    private String airTemperatureMin;

    @SerializedName("air_temperature")
    private String airTemperature;

    @SerializedName("ultraviolet_index_clear_sky")
    private String ultravioletIndexClearSky;

    @SerializedName("fog_area_fraction")
    private String fogAreaFraction;

    @SerializedName("air_temperature_max")
    private String airTemperatureMax;

    @SerializedName("air_pressure_at_sea_level")
    private String airPressureAtSeaLevel;

    @SerializedName("wind_speed")
    private String windSpeed;

    @SerializedName("cloud_area_fraction_low")
    private String cloudAreaFractionLow;

    @SerializedName("relative_humidity")
    private String relativeHumidity;

    @SerializedName("dew_point_temperature")
    private String dewPointTemperature;

    public void setCloudAreaFractionHigh(String cloudAreaFractionHigh) {
        this.cloudAreaFractionHigh = cloudAreaFractionHigh;
    }

    public String getCloudAreaFractionHigh() {
        return cloudAreaFractionHigh;
    }

    public void setPrecipitationAmount(String precipitationAmount) {
        this.precipitationAmount = precipitationAmount;
    }

    public String getPrecipitationAmount() {
        return precipitationAmount;
    }

    public void setCloudAreaFraction(String cloudAreaFraction) {
        this.cloudAreaFraction = cloudAreaFraction;
    }

    public String getCloudAreaFraction() {
        return cloudAreaFraction;
    }

    public void setCloudAreaFractionMedium(String cloudAreaFractionMedium) {
        this.cloudAreaFractionMedium = cloudAreaFractionMedium;
    }

    public String getCloudAreaFractionMedium() {
        return cloudAreaFractionMedium;
    }

    public void setWindFromDirection(String windFromDirection) {
        this.windFromDirection = windFromDirection;
    }

    public String getWindFromDirection() {
        return windFromDirection;
    }

    public void setAirTemperatureMin(String airTemperatureMin) {
        this.airTemperatureMin = airTemperatureMin;
    }

    public String getAirTemperatureMin() {
        return airTemperatureMin;
    }

    public void setAirTemperature(String airTemperature) {
        this.airTemperature = airTemperature;
    }

    public String getAirTemperature() {
        return airTemperature;
    }

    public void setUltravioletIndexClearSky(String ultravioletIndexClearSky) {
        this.ultravioletIndexClearSky = ultravioletIndexClearSky;
    }

    public String getUltravioletIndexClearSky() {
        return ultravioletIndexClearSky;
    }

    public void setFogAreaFraction(String fogAreaFraction) {
        this.fogAreaFraction = fogAreaFraction;
    }

    public String getFogAreaFraction() {
        return fogAreaFraction;
    }

    public void setAirTemperatureMax(String airTemperatureMax) {
        this.airTemperatureMax = airTemperatureMax;
    }

    public String getAirTemperatureMax() {
        return airTemperatureMax;
    }

    public void setAirPressureAtSeaLevel(String airPressureAtSeaLevel) {
        this.airPressureAtSeaLevel = airPressureAtSeaLevel;
    }

    public String getAirPressureAtSeaLevel() {
        return airPressureAtSeaLevel;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setCloudAreaFractionLow(String cloudAreaFractionLow) {
        this.cloudAreaFractionLow = cloudAreaFractionLow;
    }

    public String getCloudAreaFractionLow() {
        return cloudAreaFractionLow;
    }

    public void setRelativeHumidity(String relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public String getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setDewPointTemperature(String dewPointTemperature) {
        this.dewPointTemperature = dewPointTemperature;
    }

    public String getDewPointTemperature() {
        return dewPointTemperature;
    }
}