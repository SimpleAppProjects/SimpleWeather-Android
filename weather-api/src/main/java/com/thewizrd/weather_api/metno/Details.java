package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Details {

    @Json(name = "cloud_area_fraction_high")
    private Float cloudAreaFractionHigh;

    @Json(name = "air_temperature")
    private Float airTemperature;

    @Json(name = "air_pressure_at_sea_level")
    private Float airPressureAtSeaLevel;

    @Json(name = "wind_speed")
    private Float windSpeed;

    @Json(name = "cloud_area_fraction_low")
    private Float cloudAreaFractionLow;

    @Json(name = "cloud_area_fraction")
    private Float cloudAreaFraction;

    @Json(name = "cloud_area_fraction_medium")
    private Float cloudAreaFractionMedium;

    @Json(name = "relative_humidity")
    private Float relativeHumidity;

    @Json(name = "wind_from_direction")
    private Float windFromDirection;

    @Json(name = "dew_point_temperature")
    private Float dewPointTemperature;

    @Json(name = "air_temperature_max")
    private Float airTemperatureMax;

    @Json(name = "precipitation_amount")
    private Float precipitationAmount;

    @Json(name = "air_temperature_min")
    private Float airTemperatureMin;

    @Json(name = "ultraviolet_index_clear_sky")
    private Float ultravioletIndexClearSky;

    @Json(name = "fog_area_fraction")
    private Float fogAreaFraction;

    @Json(name = "precipitation_amount_max")
    private Float precipitationAmountMax;

    @Json(name = "probability_of_thunder")
    private Float probabilityOfThunder;

    @Json(name = "precipitation_amount_min")
    private Float precipitationAmountMin;

    @Json(name = "probability_of_precipitation")
    private Float probabilityOfPrecipitation;

    @Json(name = "wind_speed_of_gust")
    private Float windSpeedOfGust;

    public void setCloudAreaFractionHigh(Float cloudAreaFractionHigh) {
        this.cloudAreaFractionHigh = cloudAreaFractionHigh;
    }

    public Float getCloudAreaFractionHigh() {
        return cloudAreaFractionHigh;
    }

    public void setAirTemperature(Float airTemperature) {
        this.airTemperature = airTemperature;
    }

    public Float getAirTemperature() {
        return airTemperature;
    }

    public void setAirPressureAtSeaLevel(Float airPressureAtSeaLevel) {
        this.airPressureAtSeaLevel = airPressureAtSeaLevel;
    }

    public Float getAirPressureAtSeaLevel() {
        return airPressureAtSeaLevel;
    }

    public void setWindSpeed(Float windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Float getWindSpeed() {
        return windSpeed;
    }

    public void setCloudAreaFractionLow(Float cloudAreaFractionLow) {
        this.cloudAreaFractionLow = cloudAreaFractionLow;
    }

    public Float getCloudAreaFractionLow() {
        return cloudAreaFractionLow;
    }

    public void setCloudAreaFraction(Float cloudAreaFraction) {
        this.cloudAreaFraction = cloudAreaFraction;
    }

    public Float getCloudAreaFraction() {
        return cloudAreaFraction;
    }

    public void setCloudAreaFractionMedium(Float cloudAreaFractionMedium) {
        this.cloudAreaFractionMedium = cloudAreaFractionMedium;
    }

    public Float getCloudAreaFractionMedium() {
        return cloudAreaFractionMedium;
    }

    public void setRelativeHumidity(Float relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public Float getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setWindFromDirection(Float windFromDirection) {
        this.windFromDirection = windFromDirection;
    }

    public Float getWindFromDirection() {
        return windFromDirection;
    }

    public void setDewPointTemperature(Float dewPointTemperature) {
        this.dewPointTemperature = dewPointTemperature;
    }

    public Float getDewPointTemperature() {
        return dewPointTemperature;
    }

    public void setAirTemperatureMax(Float airTemperatureMax) {
        this.airTemperatureMax = airTemperatureMax;
    }

    public Float getAirTemperatureMax() {
        return airTemperatureMax;
    }

    public void setPrecipitationAmount(Float precipitationAmount) {
        this.precipitationAmount = precipitationAmount;
    }

    public Float getPrecipitationAmount() {
        return precipitationAmount;
    }

    public void setAirTemperatureMin(Float airTemperatureMin) {
        this.airTemperatureMin = airTemperatureMin;
    }

    public Float getAirTemperatureMin() {
        return airTemperatureMin;
    }

    public void setUltravioletIndexClearSky(Float ultravioletIndexClearSky) {
        this.ultravioletIndexClearSky = ultravioletIndexClearSky;
    }

    public Float getUltravioletIndexClearSky() {
        return ultravioletIndexClearSky;
    }

    public void setFogAreaFraction(Float fogAreaFraction) {
        this.fogAreaFraction = fogAreaFraction;
    }

    public Float getFogAreaFraction() {
        return fogAreaFraction;
    }

    public void setPrecipitationAmountMax(Float precipitationAmountMax) {
        this.precipitationAmountMax = precipitationAmountMax;
    }

    public Float getPrecipitationAmountMax() {
        return precipitationAmountMax;
    }

    public void setProbabilityOfThunder(Float probabilityOfThunder) {
        this.probabilityOfThunder = probabilityOfThunder;
    }

    public Float getProbabilityOfThunder() {
        return probabilityOfThunder;
    }

    public void setPrecipitationAmountMin(Float precipitationAmountMin) {
        this.precipitationAmountMin = precipitationAmountMin;
    }

    public Float getPrecipitationAmountMin() {
        return precipitationAmountMin;
    }

    public void setProbabilityOfPrecipitation(Float probabilityOfPrecipitation) {
        this.probabilityOfPrecipitation = probabilityOfPrecipitation;
    }

    public Float getProbabilityOfPrecipitation() {
        return probabilityOfPrecipitation;
    }

    public void setWindSpeedOfGust(Float windSpeedOfGust) {
        this.windSpeedOfGust = windSpeedOfGust;
    }

    public Float getWindSpeedOfGust() {
        return windSpeedOfGust;
    }
}