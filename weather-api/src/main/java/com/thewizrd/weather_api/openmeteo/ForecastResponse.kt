package com.thewizrd.weather_api.openmeteo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(

    @Json(name = "elevation")
    var elevation: Float? = null,

    @Json(name = "minutely_15")
    var minutely15: Minutely15? = null,

    @Json(name = "daily_units")
    var dailyUnits: DailyUnits? = null,

    @Json(name = "timezone")
    var timezone: String? = null,

    @Json(name = "latitude")
    var latitude: Float? = null,

    @Json(name = "minutely_15_units")
    var minutely15Units: Minutely15Units? = null,

    @Json(name = "hourly_units")
    var hourlyUnits: HourlyUnits? = null,

    @Json(name = "generationtime_ms")
    var generationtimeMs: Double? = null,

    @Json(name = "current")
    var current: Current? = null,

    @Json(name = "timezone_abbreviation")
    var timezoneAbbreviation: String? = null,

    @Json(name = "current_units")
    var currentUnits: CurrentUnits? = null,

    @Json(name = "daily")
    var daily: Daily? = null,

    @Json(name = "utc_offset_seconds")
    var utcOffsetSeconds: Int? = null,

    @Json(name = "hourly")
    var hourly: Hourly? = null,

    @Json(name = "longitude")
    var longitude: Float? = null
)

@JsonClass(generateAdapter = true)
data class Daily(

    @Json(name = "sunrise")
    var sunrise: List<Int>? = null,

    @Json(name = "wind_speed_10m_max")
    var windSpeed10mMax: List<Float>? = null,

    @Json(name = "cloud_cover_mean")
    var cloudCoverMean: List<Int>? = null,

    @Json(name = "uv_index_max")
    var uvIndexMax: List<Float>? = null,

    @Json(name = "visibility_mean")
    var visibilityMean: List<Float>? = null,

    @Json(name = "apparent_temperature_mean")
    var apparentTemperatureMean: List<Float>? = null,

    @Json(name = "temperature_2m_min")
    var temperature2mMin: List<Float>? = null,

    @Json(name = "wind_gusts_10m_max")
    var windGusts10mMax: List<Float>? = null,

    @Json(name = "dew_point_2m_mean")
    var dewPoint2mMean: List<Float>? = null,

    @Json(name = "relative_humidity_2m_mean")
    var relativeHumidity2mMean: List<Int>? = null,

    @Json(name = "sunset")
    var sunset: List<Int>? = null,

    @Json(name = "precipitation_probability_max")
    var precipitationProbabilityMax: List<Int>? = null,

    @Json(name = "temperature_2m_max")
    var temperature2mMax: List<Float>? = null,

    @Json(name = "wind_direction_10m_dominant")
    var windDirection10mDominant: List<Int>? = null,

    @Json(name = "pressure_msl_mean")
    var pressureMslMean: List<Float>? = null,

    @Json(name = "time")
    var time: List<Long>? = null,

    @Json(name = "weather_code")
    var weatherCode: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class DailyUnits(

    @Json(name = "sunrise")
    var sunrise: String? = null,

    @Json(name = "wind_speed_10m_max")
    var windSpeed10mMax: String? = null,

    @Json(name = "cloud_cover_mean")
    var cloudCoverMean: String? = null,

    @Json(name = "uv_index_max")
    var uvIndexMax: String? = null,

    @Json(name = "visibility_mean")
    var visibilityMean: String? = null,

    @Json(name = "apparent_temperature_mean")
    var apparentTemperatureMean: String? = null,

    @Json(name = "temperature_2m_min")
    var temperature2mMin: String? = null,

    @Json(name = "wind_gusts_10m_max")
    var windGusts10mMax: String? = null,

    @Json(name = "dew_point_2m_mean")
    var dewPoint2mMean: String? = null,

    @Json(name = "relative_humidity_2m_mean")
    var relativeHumidity2mMean: String? = null,

    @Json(name = "sunset")
    var sunset: String? = null,

    @Json(name = "precipitation_probability_max")
    var precipitationProbabilityMax: String? = null,

    @Json(name = "temperature_2m_max")
    var temperature2mMax: String? = null,

    @Json(name = "wind_direction_10m_dominant")
    var windDirection10mDominant: String? = null,

    @Json(name = "pressure_msl_mean")
    var pressureMslMean: String? = null,

    @Json(name = "time")
    var time: String? = null,

    @Json(name = "weather_code")
    var weatherCode: String? = null
)

@JsonClass(generateAdapter = true)
data class Hourly(

    @Json(name = "pressure_msl")
    var pressureMsl: List<Float>? = null,

    @Json(name = "wind_speed_10m")
    var windSpeed10m: List<Float>? = null,

    @Json(name = "rain")
    var rain: List<Float>? = null,

    @Json(name = "dew_point_2m")
    var dewPoint2m: List<Float>? = null,

    @Json(name = "visibility")
    var visibility: List<Float>? = null,

    @Json(name = "relative_humidity_2m")
    var relativeHumidity2m: List<Int>? = null,

    @Json(name = "snowfall")
    var snowfall: List<Float>? = null,

    @Json(name = "is_day")
    var isDay: List<Int>? = null,

    @Json(name = "wind_direction_10m")
    var windDirection10m: List<Int>? = null,

    @Json(name = "wind_gusts_10m")
    var windGusts10m: List<Float>? = null,

    @Json(name = "temperature_2m")
    var temperature2m: List<Float>? = null,

    @Json(name = "uv_index")
    var uvIndex: List<Float>? = null,

    @Json(name = "precipitation_probability")
    var precipitationProbability: List<Int>? = null,

    @Json(name = "cloud_cover")
    var cloudCover: List<Int>? = null,

    @Json(name = "apparent_temperature")
    var apparentTemperature: List<Float>? = null,

    @Json(name = "time")
    var time: List<Long>? = null,

    @Json(name = "weather_code")
    var weatherCode: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class Current(

    @Json(name = "pressure_msl")
    var pressureMsl: Float? = null,

    @Json(name = "wind_speed_10m")
    var windSpeed10m: Float? = null,

    @Json(name = "rain")
    var rain: Float? = null,

    @Json(name = "dew_point_2m")
    var dewPoint2m: Float? = null,

    @Json(name = "visibility")
    var visibility: Float? = null,

    @Json(name = "relative_humidity_2m")
    var relativeHumidity2m: Int? = null,

    @Json(name = "snowfall")
    var snowfall: Float? = null,

    @Json(name = "is_day")
    var isDay: Int? = null,

    @Json(name = "wind_direction_10m")
    var windDirection10m: Int? = null,

    @Json(name = "wind_gusts_10m")
    var windGusts10m: Float? = null,

    @Json(name = "temperature_2m")
    var temperature2m: Float? = null,

    @Json(name = "uv_index")
    var uvIndex: Float? = null,

    @Json(name = "precipitation_probability")
    var precipitationProbability: Int? = null,

    @Json(name = "cloud_cover")
    var cloudCover: Int? = null,

    @Json(name = "apparent_temperature")
    var apparentTemperature: Float? = null,

    @Json(name = "interval")
    var interval: Int? = null,

    @Json(name = "time")
    var time: Long? = null,

    @Json(name = "weather_code")
    var weatherCode: Int? = null
)

@JsonClass(generateAdapter = true)
data class HourlyUnits(

    @Json(name = "pressure_msl")
    var pressureMsl: String? = null,

    @Json(name = "wind_speed_10m")
    var windSpeed10m: String? = null,

    @Json(name = "rain")
    var rain: String? = null,

    @Json(name = "dew_point_2m")
    var dewPoint2m: String? = null,

    @Json(name = "visibility")
    var visibility: String? = null,

    @Json(name = "relative_humidity_2m")
    var relativeHumidity2m: String? = null,

    @Json(name = "snowfall")
    var snowfall: String? = null,

    @Json(name = "is_day")
    var isDay: String? = null,

    @Json(name = "wind_direction_10m")
    var windDirection10m: String? = null,

    @Json(name = "wind_gusts_10m")
    var windGusts10m: String? = null,

    @Json(name = "temperature_2m")
    var temperature2m: String? = null,

    @Json(name = "uv_index")
    var uvIndex: String? = null,

    @Json(name = "precipitation_probability")
    var precipitationProbability: String? = null,

    @Json(name = "cloud_cover")
    var cloudCover: String? = null,

    @Json(name = "apparent_temperature")
    var apparentTemperature: String? = null,

    @Json(name = "time")
    var time: String? = null,

    @Json(name = "weather_code")
    var weatherCode: String? = null
)

@JsonClass(generateAdapter = true)
data class Minutely15(

    @Json(name = "rain")
    var rain: List<Float>? = null,

    @Json(name = "snowfall")
    var snowfall: List<Float>? = null,

    @Json(name = "time")
    var time: List<Long>? = null
)

@JsonClass(generateAdapter = true)
data class Minutely15Units(

    @Json(name = "rain")
    var rain: String? = null,

    @Json(name = "snowfall")
    var snowfall: String? = null,

    @Json(name = "time")
    var time: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentUnits(

    @Json(name = "pressure_msl")
    var pressureMsl: String? = null,

    @Json(name = "wind_speed_10m")
    var windSpeed10m: String? = null,

    @Json(name = "rain")
    var rain: String? = null,

    @Json(name = "dew_point_2m")
    var dewPoint2m: String? = null,

    @Json(name = "visibility")
    var visibility: String? = null,

    @Json(name = "relative_humidity_2m")
    var relativeHumidity2m: String? = null,

    @Json(name = "snowfall")
    var snowfall: String? = null,

    @Json(name = "is_day")
    var isDay: String? = null,

    @Json(name = "wind_direction_10m")
    var windDirection10m: String? = null,

    @Json(name = "wind_gusts_10m")
    var windGusts10m: String? = null,

    @Json(name = "temperature_2m")
    var temperature2m: String? = null,

    @Json(name = "uv_index")
    var uvIndex: String? = null,

    @Json(name = "precipitation_probability")
    var precipitationProbability: String? = null,

    @Json(name = "cloud_cover")
    var cloudCover: String? = null,

    @Json(name = "apparent_temperature")
    var apparentTemperature: String? = null,

    @Json(name = "interval")
    var interval: String? = null,

    @Json(name = "time")
    var time: String? = null,

    @Json(name = "weather_code")
    var weatherCode: String? = null
)
