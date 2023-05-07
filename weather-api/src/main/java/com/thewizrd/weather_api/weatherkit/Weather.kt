package com.thewizrd.weather_api.weatherkit

import androidx.annotation.StringDef
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The collection of all requested weather data.
 */
@JsonClass(generateAdapter = true)
data class Weather(
    /**
     * The current weather for the requested location.
     */
    @field:Json(name = "currentWeather")
    var currentWeather: CurrentWeather? = null,
    /**
     * The daily forecast for the requested location.
     */
    @field:Json(name = "forecastDaily")
    var forecastDaily: DailyForecast? = null,
    /**
     * The hourly forecast for the requested location.
     */
    @field:Json(name = "forecastHourly")
    var forecastHourly: HourlyForecast? = null,
    /**
     * The next hour forecast for the requested location.
     */
    @field:Json(name = "forecastNextHour")
    var forecastNextHour: NextHourForecast? = null,
    /**
     * Weather alerts for the requested location.
     */
    @field:Json(name = "weatherAlerts")
    var weatherAlerts: WeatherAlertCollection? = null
)

abstract class ProductData {
    @Json(name = "name")
    abstract var name: String?

    /**
     * (Required) Descriptive information about the weather data.
     */
    @Json(name = "metadata")
    abstract var metadata: Metadata
}

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @field:Json(name = "name")
    override var name: String? = null,
    @field:Json(name = "metadata")
    override var metadata: Metadata,

    /**
     * (Required) The date and time.
     */
    @field:Json(name = "asOf")
    var asOf: String,

    /**
     * The percentage of the sky covered with clouds during the period, from 0 to 1.
     */
    @field:Json(name = "cloudCover")
    var cloudCover: Float? = null,
    @field:Json(name = "cloudCoverLowAltPct")
    var cloudCoverLowAltPct: Float? = null,
    @field:Json(name = "cloudCoverMidAltPct")
    var cloudCoverMidAltPct: Float? = null,
    @field:Json(name = "cloudCoverHighAltPct")
    var cloudCoverHighAltPct: Float? = null,

    /**
     * (Required) An enumeration value indicating the condition at the time.
     */
    @field:Json(name = "conditionCode")
    var conditionCode: String,

    /**
     * A Boolean value indicating whether there is daylight.
     */
    @field:Json(name = "daylight")
    var daylight: Boolean? = null,

    /**
     * (Required) The relative humidity, from 0 to 1.
     */
    @field:Json(name = "humidity")
    var humidity: Float,

    /**
     * (Required) The precipitation intensity, in millimeters per hour.
     */
    @field:Json(name = "precipitationIntensity")
    var precipitationIntensity: Float,

    /**
     * (Required) The sea level air pressure, in millibars.
     */
    @field:Json(name = "pressure")
    var pressure: Float,

    /**
     * (Required) The direction of change of the sea-level air pressure.
     */
    @field:Json(name = "pressureTrend")
    @PressureTrend
    var pressureTrend: String,

    /**
     * (Required) The current temperature, in degrees Celsius.
     */
    @field:Json(name = "temperature")
    var temperature: Float,

    /**
     * (Required) The feels-like temperature when factoring wind and humidity, in degrees Celsius.
     */
    @field:Json(name = "temperatureApparent")
    var temperatureApparent: Float,

    /**
     * (Required) The temperature at which relative humidity is 100%, in Celsius.
     */
    @field:Json(name = "temperatureDewPoint")
    var temperatureDewPoint: Float,

    /**
     * (Required) The level of ultraviolet radiation.
     */
    @field:Json(name = "uvIndex")
    var uvIndex: Int,

    /**
     * (Required) The distance at which terrain is visible, in meters.
     */
    @field:Json(name = "visibility")
    var visibility: Float,

    /**
     * The direction of the wind, in degrees.
     */
    @field:Json(name = "windDirection")
    var windDirection: Int? = null,

    /**
     * The maximum wind gust speed, in kilometers per hour.
     */
    @field:Json(name = "windGust")
    var windGust: Float? = null,

    /**
     * (Required) The wind speed, in kilometers per hour.
     */
    @field:Json(name = "windSpeed")
    var windSpeed: Float
) : ProductData()

/**
 * Descriptive information about the weather data.
 */
@JsonClass(generateAdapter = true)
data class Metadata(
    /**
     * The URL of the legal attribution for the data source.
     */
    @field:Json(name = "attributionURL")
    var attributionURL: String? = null,

    /**
     * (Required) The time when the weather data is no longer valid.
     */
    @field:Json(name = "expireTime")
    var expireTime: String,

    /**
     * The ISO language code for localizable fields.
     */
    @field:Json(name = "language")
    var language: String? = null,

    /**
     * (Required) The latitude of the relevant location.
     */
    @field:Json(name = "latitude")
    var latitude: Float,

    /**
     * (Required) The longitude of the relevant location.
     */
    @field:Json(name = "longitude")
    var longitude: Float,

    /**
     * The URL of a logo for the data provider.
     */
    @field:Json(name = "providerLogo")
    var providerLogo: String? = null,

    /**
     * The name of the data provider.
     */
    @field:Json(name = "providerName")
    var providerName: String? = null,

    /**
     * (Required) The time the weather data was procured.
     */
    @field:Json(name = "readTime")
    var readTime: String,

    /**
     * The time the provider reported the weather data.
     */
    @field:Json(name = "reportedTime")
    var reportedTime: String? = null,

    /**
     * The weather data is temporarily unavailable from the provider.
     */
    @field:Json(name = "temporarilyUnavailable")
    var temporarilyUnavailable: Boolean? = null,

    /**
     * The system of units that the weather data is reported in. This is set to metric.
     */
    @field:Json(name = "units")
    @UnitsSystem
    var units: String? = null,

    /**
     * (Required) The data format version.
     */
    @field:Json(name = "version")
    var version: Int
)

/**
 * A collection of day forecasts for a specified range of days.
 */
@JsonClass(generateAdapter = true)
data class DailyForecast(
    @field:Json(name = "name")
    override var name: String? = null,
    @field:Json(name = "metadata")
    override var metadata: Metadata,

    /**
     * (Required) An array of the day forecast weather conditions.
     */
    @field:Json(name = "days")
    var days: List<DayWeatherConditions>? = null,

    /**
     * A URL that provides more information about the forecast.
     */
    @field:Json(name = "learnMoreURL")
    var learnMoreURL: String? = null
) : ProductData()

/**
 * The historical or forecasted weather conditions for a specified day.
 */
@JsonClass(generateAdapter = true)
data class DayWeatherConditions(

    /**
     * (Required) An enumeration value indicating the condition at the time.
     */
    @field:Json(name = "conditionCode")
    var conditionCode: String,

    /**
     * The forecast between 7 AM and 7 PM for the day.
     */
    @field:Json(name = "daytimeForecast")
    var daytimeForecast: DayPartForecast? = null,

    /**
     * (Required) The ending date and time of the day.
     */
    @field:Json(name = "forecastEnd")
    var forecastEnd: String,

    /**
     * (Required) The starting date and time of the day.
     */
    @field:Json(name = "forecastStart")
    var forecastStart: String,

    /**
     * (Required) The maximum ultraviolet index value during the day.
     */
    @field:Json(name = "maxUvIndex")
    var maxUvIndex: Int,

    /**
     * (Required) The phase of the moon on the specified day.
     */
    @field:Json(name = "moonPhase")
    @MoonPhase
    var moonPhase: String,

    /**
     * The time of moonrise on the specified day.
     */
    @field:Json(name = "moonrise")
    var moonrise: String? = null,

    /**
     * The time of moonset on the specified day.
     */
    @field:Json(name = "moonset")
    var moonset: String? = null,

    /**
     * The day part forecast between 7 PM and 7 AM for the overnight.
     */
    @field:Json(name = "overnightForecast")
    var overnightForecast: DayPartForecast? = null,

    /**
     * (Required) The amount of precipitation forecasted to occur during the day, in millimeters.
     */
    @field:Json(name = "precipitationAmount")
    var precipitationAmount: Float,

    /**
     * (Required) The chance of precipitation forecasted to occur during the day.
     */
    @field:Json(name = "precipitationChance")
    var precipitationChance: Float,

    /**
     * (Required) The type of precipitation forecasted to occur during the day.
     */
    @field:Json(name = "precipitationType")
    @PrecipitationType
    var precipitationType: String,

    //@field:Json(name = "precipitationAmountByType")
    //var precipitationAmountByType: Any? = null,

    /**
     * (Required) The depth of snow as ice crystals forecasted to occur during the day, in millimeters.
     */
    @field:Json(name = "snowfallAmount")
    var snowfallAmount: Float,

    /**
     * The time when the sun is lowest in the sky.
     */
    @field:Json(name = "solarMidnight")
    var solarMidnight: String? = null,

    /**
     * The time when the sun is highest in the sky.
     */
    @field:Json(name = "solarNoon")
    var solarNoon: String? = null,

    /**
     * The time when the top edge of the sun reaches the horizon in the morning.
     */
    @field:Json(name = "sunrise")
    var sunrise: String? = null,

    /**
     * The time when the sun is 18 degrees below the horizon in the morning.
     */
    @field:Json(name = "sunriseAstronomical")
    var sunriseAstronomical: String? = null,

    /**
     * The time when the sun is 6 degrees below the horizon in the morning.
     */
    @field:Json(name = "sunriseCivil")
    var sunriseCivil: String? = null,

    /**
     * The time when the sun is 12 degrees below the horizon in the morning.
     */
    @field:Json(name = "sunriseNautical")
    var sunriseNautical: String? = null,

    /**
     * The time when the top edge of the sun reaches the horizon in the evening.
     */
    @field:Json(name = "sunset")
    var sunset: String? = null,

    /**
     * The time when the sun is 18 degrees below the horizon in the evening.
     */
    @field:Json(name = "sunsetAstronomical")
    var sunsetAstronomical: String? = null,

    /**
     * The time when the sun is 6 degrees below the horizon in the evening.
     */
    @field:Json(name = "sunsetCivil")
    var sunsetCivil: String? = null,

    /**
     * The time when the sun is 12 degrees below the horizon in the evening.
     */
    @field:Json(name = "sunsetNautical")
    var sunsetNautical: String? = null,

    /**
     * (Required) The maximum temperature forecasted to occur during the day, in degrees Celsius.
     */
    @field:Json(name = "temperatureMax")
    var temperatureMax: Float,

    /**
     * (Required) The minimum temperature forecasted to occur during the day, in degrees Celsius.
     */
    @field:Json(name = "temperatureMin")
    var temperatureMin: Float,

    //@field:Json(name = "restOfDayForecast")
    //var restOfDayForecast: RestOfDayForecast? = null
)

/**
 * A summary forecast for a daytime or overnight period.
 */
@JsonClass(generateAdapter = true)
data class DayPartForecast(
    /**
     * (Required) The percentage of the sky covered with clouds during the period, from 0 to 1.
     */
    @field:Json(name = "cloudCover")
    var cloudCover: Float,

    /**
     * (Required) An enumeration value indicating the condition at the time.
     */
    @field:Json(name = "conditionCode")
    var conditionCode: String,

    /**
     * (Required) The ending date and time of the forecast.
     */
    @field:Json(name = "forecastEnd")
    var forecastEnd: String,

    /**
     * (Required) The starting date and time of the forecast.
     */
    @field:Json(name = "forecastStart")
    var forecastStart: String,

    /**
     * (Required) The relative humidity during the period, from 0 to 1.
     */
    @field:Json(name = "humidity")
    var humidity: Float,

    /**
     * (Required) The amount of precipitation forecasted to occur during the period, in millimeters.
     */
    @field:Json(name = "precipitationAmount")
    var precipitationAmount: Float,

    //@field:Json(name = "precipitationAmountByType")
    //var precipitationAmountByType: Any? = null,

    /**
     * (Required) The chance of precipitation forecasted to occur during the period.
     */
    @field:Json(name = "precipitationChance")
    var precipitationChance: Float,

    /**
     * (Required) The type of precipitation forecasted to occur during the period.
     */
    @field:Json(name = "precipitationType")
    @PrecipitationType
    var precipitationType: String,

    /**
     * (Required) The depth of snow as ice crystals forecasted to occur during the period, in millimeters.
     */
    @field:Json(name = "snowfallAmount")
    var snowfallAmount: Float,

    /**
     * The direction the wind is forecasted to come from during the period, in degrees.
     */
    @field:Json(name = "windDirection")
    var windDirection: Int? = null,

    /**
     * (Required) The average speed the wind is forecasted to be during the period, in kilometers per hour.
     */
    @field:Json(name = "windSpeed")
    var windSpeed: Float,
)

/**
 * The hourly forecast information.
 */
@JsonClass(generateAdapter = true)
data class HourlyForecast(
    @field:Json(name = "name")
    override var name: String? = null,
    @field:Json(name = "metadata")
    override var metadata: Metadata,

    /**
     * (Required) An array of hourly forecasts.
     */
    @field:Json(name = "hours")
    var hours: List<HourWeatherConditions>? = null,
) : ProductData()

/**
 * The historical or forecasted weather conditions for a specified hour.
 */
@JsonClass(generateAdapter = true)
data class HourWeatherConditions(
    /**
     * (Required) The percentage of the sky covered with clouds during the period, from 0 to 1.
     */
    @field:Json(name = "cloudCover")
    var cloudCover: Float,

    /**
     * (Required) An enumeration value indicating the condition at the time.
     */
    @field:Json(name = "conditionCode")
    var conditionCode: String,

    /**
     * Indicates whether the hour starts during the day or night.
     */
    @field:Json(name = "daylight")
    var daylight: Boolean?,

    /**
     * (Required) The starting date and time of the forecast.
     */
    @field:Json(name = "forecastStart")
    var forecastStart: String,

    /**
     * (Required) The relative humidity at the start of the hour, from 0 to 1.
     */
    @field:Json(name = "humidity")
    var humidity: Float,

    /**
     * (Required) The chance of precipitation forecasted to occur during the hour, from 0 to 1.
     */
    @field:Json(name = "precipitationChance")
    var precipitationChance: Float,

    /**
     * (Required) The type of precipitation forecasted to occur during the period.
     */
    @field:Json(name = "precipitationType")
    @PrecipitationType
    var precipitationType: String,

    /**
     * (Required) The sea-level air pressure, in millibars.
     */
    @field:Json(name = "pressure")
    var pressure: Float,

    /**
     * The direction of change of the sea-level air pressure.
     */
    @field:Json(name = "pressureTrend")
    @PressureTrend
    var pressureTrend: String? = null,

    /**
     * The rate at which snow crystals are falling, in millimeters per hour.
     */
    @field:Json(name = "snowfallIntensity")
    var snowfallIntensity: Float? = null,

    //@field:Json(name = "snowfallAmount")
    //var snowfallAmount: Float? = null,

    /**
     * (Required) The temperature at the start of the hour, in degrees Celsius.
     */
    @field:Json(name = "temperature")
    var temperature: Float,

    /**
     * (Required) The feels-like temperature when considering wind and humidity, at the start of the hour, in degrees Celsius.
     */
    @field:Json(name = "temperatureApparent")
    var temperatureApparent: Float,

    /**
     * The temperature at which relative humidity is 100% at the top of the hour, in degrees Celsius.
     */
    @field:Json(name = "temperatureDewPoint")
    var temperatureDewPoint: Float? = null,

    /**
     * (Required) The level of ultraviolet radiation at the start of the hour.
     */
    @field:Json(name = "uvIndex")
    var uvIndex: Int,

    /**
     * (Required) The distance at which terrain is visible at the start of the hour, in meters.
     */
    @field:Json(name = "visibility")
    var visibility: Float,

    /**
     * The direction of the wind at the start of the hour, in degrees.
     */
    @field:Json(name = "windDirection")
    var windDirection: Int? = null,

    /**
     * The maximum wind gust speed during the hour, in kilometers per hour.
     */
    @field:Json(name = "windGust")
    var windGust: Float? = null,

    /**
     * (Required) The wind speed at the start of the hour, in kilometers per hour.
     */
    @field:Json(name = "windSpeed")
    var windSpeed: Float,

    /**
     * The amount of precipitation forecasted to occur during period, in millimeters.
     */
    @field:Json(name = "precipitationAmount")
    var precipitationAmount: Float? = null,

    //@field:Json(name = "precipitationIntensity")
    //var precipitationIntensity: Float? = null,
)

/**
 * The next hour forecast information.
 */
@JsonClass(generateAdapter = true)
data class NextHourForecast(
    @field:Json(name = "name")
    override var name: String? = null,
    @field:Json(name = "metadata")
    override var metadata: Metadata,

    /**
     * The time the forecast ends.
     */
    @field:Json(name = "forecastEnd")
    var forecastEnd: String? = null,

    /**
     * The time the forecast starts.
     */
    @field:Json(name = "forecastStart")
    var forecastStart: String? = null,

    /**
     * (Required) An array of the forecast minutes.
     */
    @field:Json(name = "minutes")
    var minutes: List<ForecastMinute>,

    /**
     * (Required) An array of the forecast summaries.
     */
    @field:Json(name = "summary")
    var summary: List<ForecastPeriodSummary>,
) : ProductData()

/**
 * The precipitation forecast for a specified minute.
 */
@JsonClass(generateAdapter = true)
data class ForecastMinute(
    /**
     * (Required) The probability of precipitation during this minute.
     */
    @field:Json(name = "precipitationChance")
    var precipitationChance: Float,

    /**
     * (Required) The precipitation intensity in millimeters per hour.
     */
    @field:Json(name = "precipitationIntensity")
    var precipitationIntensity: Float,

    /**
     * (Required) The start time of the minute.
     */
    @field:Json(name = "startTime")
    var startTime: String
)

/**
 * The summary for a specified period in the minute forecast.
 */
@JsonClass(generateAdapter = true)
data class ForecastPeriodSummary(
    /**
     * (Required) The type of precipitation forecasted.
     */
    @field:Json(name = "condition")
    @PrecipitationType
    var condition: String,

    /**
     * The end time of the forecast.
     */
    @field:Json(name = "endTime")
    var endTime: String? = null,

    /**
     * (Required) The probability of precipitation during this period.
     */
    @field:Json(name = "precipitationChance")
    var precipitationChance: Float,

    /**
     * (Required) The precipitation intensity in millimeters per hour.
     */
    @field:Json(name = "precipitationIntensity")
    var precipitationIntensity: Float,

    /**
     * (Required) The start time of the forecast.
     */
    @field:Json(name = "startTime")
    var startTime: String
)

/**
 * A collection of severe weather alerts for a specified location.
 */
@JsonClass(generateAdapter = true)
data class WeatherAlertCollection(
    @field:Json(name = "name")
    override var name: String? = null,
    @field:Json(name = "metadata")
    override var metadata: Metadata,

    /**
     * (Required) An array of weather alert summaries.
     */
    @field:Json(name = "alerts")
    var alerts: List<WeatherAlertSummary>,

    /**
     * A URL that provides more information about the alerts.
     */
    @field:Json(name = "detailsUrl")
    var detailsUrl: String? = null,
) : ProductData()

@JsonClass(generateAdapter = true)
data class WeatherAlertSummary(
    /**
     * An official designation of the affected area.
     */
    @field:Json(name = "areaId")
    var areaId: String? = null,

    /**
     * A human-readable name of the affected area.
     */
    @field:Json(name = "areaName")
    var areaName: String? = null,

    /**
     * (Required) How likely the event is to occur.
     */
    @field:Json(name = "certainty")
    @Certainty
    var certainty: String,

    /**
     * (Required) The ISO code of the reporting country.
     */
    @field:Json(name = "countryCode")
    var countryCode: String,

    /**
     * (Required) A human-readable description of the event.
     */
    @field:Json(name = "description")
    var description: String,

    /**
     * The URL to a page containing detailed information about the event.
     */
    @field:Json(name = "detailsUrl")
    var detailsUrl: String? = null,

    /**
     * (Required) The time the event went into effect.
     */
    @field:Json(name = "effectiveTime")
    var effectiveTime: String,

    /**
     * The time when the underlying weather event is projected to end.
     */
    @field:Json(name = "eventEndTime")
    var eventEndTime: String? = null,

    /**
     * The time when the underlying weather event is projected to start.
     */
    @field:Json(name = "eventOnsetTime")
    var eventOnsetTime: String? = null,

    /**
     * (Required) The time when the event expires.
     */
    @field:Json(name = "expireTime")
    var expireTime: String,

    /**
     * (Required) A unique identifier of the event.
     */
    @field:Json(name = "id")
    var id: String,

    /**
     * (Required) The time that event was issued by the reporting agency.
     */
    @field:Json(name = "issuedTime")
    var issuedTime: String,

    /**
     * (Required) An array of recommended actions from the reporting agency.
     */
    @field:Json(name = "responses")
    @ResponseType
    var responses: List<String>,

    /**
     * (Required) The level of danger to life and property.
     */
    @field:Json(name = "severity")
    @Severity
    var severity: String,

    /**
     * (Required) The name of the reporting agency.
     */
    @field:Json(name = "source")
    var source: String,

    /**
     * An indication of urgency of action from the reporting agency.
     */
    @field:Json(name = "urgency")
    @Urgency
    var urgency: String? = null
)

@StringDef(
    Certainty.OBSERVED, Certainty.LIKELY, Certainty.POSSIBLE, Certainty.UNLIKELY,
    Certainty.UNKNOWN
)
@Retention(AnnotationRetention.SOURCE)
annotation class Certainty {
    companion object {
        const val OBSERVED = "observed"
        const val LIKELY = "likely"
        const val POSSIBLE = "possible"
        const val UNLIKELY = "unlikely"
        const val UNKNOWN = "unknown"
    }
}

@StringDef(
    ResponseType.SHELTER,
    ResponseType.EVACUATE,
    ResponseType.PREPARE,
    ResponseType.EXECUTE,
    ResponseType.AVOID,
    ResponseType.MONITOR,
    ResponseType.ASSESS,
    ResponseType.ALL_CLEAR,
    ResponseType.NONE
)
@Retention(AnnotationRetention.SOURCE)
annotation class ResponseType {
    companion object {
        const val SHELTER = "shelter"
        const val EVACUATE = "evacuate"
        const val PREPARE = "prepare"
        const val EXECUTE = "execute"
        const val AVOID = "avoid"
        const val MONITOR = "monitor"
        const val ASSESS = "assess"
        const val ALL_CLEAR = "allClear"
        const val NONE = "none"
    }
}

@StringDef(Severity.EXTREME, Severity.SEVERE, Severity.MODERATE, Severity.MINOR, Severity.UNKNOWN)
@Retention(AnnotationRetention.SOURCE)
annotation class Severity {
    companion object {
        const val EXTREME = "extreme"
        const val SEVERE = "severe"
        const val MODERATE = "moderate"
        const val MINOR = "minor"
        const val UNKNOWN = "unknown"
    }
}

@StringDef(Urgency.IMMEDIATE, Urgency.EXPECTED, Urgency.FUTURE, Urgency.PAST, Urgency.UNKNOWN)
@Retention(AnnotationRetention.SOURCE)
annotation class Urgency {
    companion object {
        const val IMMEDIATE = "immediate"
        const val EXPECTED = "expected"
        const val FUTURE = "future"
        const val PAST = "past"
        const val UNKNOWN = "unknown"
    }
}

@StringDef(UnitsSystem.M)
@Retention(AnnotationRetention.SOURCE)
annotation class UnitsSystem {
    companion object {
        const val M = "m"
    }
}

@StringDef(
    MoonPhase.NEW,
    MoonPhase.WAXING_CRESCENT,
    MoonPhase.FIRST_QUARTER,
    MoonPhase.FULL,
    MoonPhase.WAXING_GIBBOUS,
    MoonPhase.WANING_GIBBOUS,
    MoonPhase.THIRD_QUARTER,
    MoonPhase.WANING_CRESCENT
)
@Retention(AnnotationRetention.SOURCE)
annotation class MoonPhase {
    companion object {
        const val NEW = "new"
        const val WAXING_CRESCENT = "waxingCrescent"
        const val FIRST_QUARTER = "firstQuarter"
        const val FULL = "full"
        const val WAXING_GIBBOUS = "waxingGibbous"
        const val WANING_GIBBOUS = "waningGibbous"
        const val THIRD_QUARTER = "thirdQuarter"
        const val WANING_CRESCENT = "waningCrescent"
    }
}

@StringDef(
    PrecipitationType.CLEAR,
    PrecipitationType.PRECIPITATION,
    PrecipitationType.RAIN,
    PrecipitationType.SNOW,
    PrecipitationType.SLEET,
    PrecipitationType.HAIL,
    PrecipitationType.MIXED
)
@Retention(AnnotationRetention.SOURCE)
annotation class PrecipitationType {
    companion object {
        const val CLEAR = "clear"
        const val PRECIPITATION = "precipitation"
        const val RAIN = "rain"
        const val SNOW = "snow"
        const val SLEET = "sleet"
        const val HAIL = "hail"
        const val MIXED = "mixed"
    }
}

@StringDef(PressureTrend.RISING, PressureTrend.FALLING, PressureTrend.STEADY)
@Retention(AnnotationRetention.SOURCE)
annotation class PressureTrend {
    companion object {
        const val RISING = "rising"
        const val FALLING = "falling"
        const val STEADY = "steady"
    }
}