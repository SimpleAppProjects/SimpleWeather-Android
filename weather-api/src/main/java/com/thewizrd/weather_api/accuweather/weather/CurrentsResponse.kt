package com.thewizrd.weather_api.accuweather.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentsResponse(

		var currentsResponse: List<CurrentsResponseItem?>? = null
)

@JsonClass(generateAdapter = true)
data class CurrentMinimum(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Precip1hr(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Past6HourRange(

    @field:Json(name = "Minimum")
		var minimum: CurrentMinimum? = null,

    @field:Json(name = "Maximum")
		var maximum: CurrentMaximum? = null
)

@JsonClass(generateAdapter = true)
data class PastHour(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Metric(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentSpeed(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class TemperatureSummary(

    @field:Json(name = "Past6HourRange")
		var past6HourRange: Past6HourRange? = null,

    @field:Json(name = "Past24HourRange")
		var past24HourRange: Past24HourRange? = null,

    @field:Json(name = "Past12HourRange")
		var past12HourRange: Past12HourRange? = null
)

@JsonClass(generateAdapter = true)
data class Past12Hours(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentMaximum(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Pressure(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class WindChillTemperature(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class PrecipitationSummary(

    @field:Json(name = "Past6Hours")
		var past6Hours: Past6Hours? = null,

    @field:Json(name = "Precipitation")
		var precipitation: Precipitation? = null,

    @field:Json(name = "Past9Hours")
		var past9Hours: Past9Hours? = null,

    @field:Json(name = "Past3Hours")
		var past3Hours: Past3Hours? = null,

    @field:Json(name = "PastHour")
		var pastHour: PastHour? = null,

    @field:Json(name = "Past18Hours")
		var past18Hours: Past18Hours? = null,

    @field:Json(name = "Past24Hours")
		var past24Hours: Past24Hours? = null,

    @field:Json(name = "Past12Hours")
		var past12Hours: Past12Hours? = null
)

@JsonClass(generateAdapter = true)
data class Past3Hours(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDirection(

    @field:Json(name = "English")
		var english: String? = null,

    @field:Json(name = "Degrees")
		var degrees: Int? = null,

    @field:Json(name = "Localized")
		var localized: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentCeiling(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Past12HourRange(

    @field:Json(name = "Minimum")
		var minimum: CurrentMinimum? = null,

    @field:Json(name = "Maximum")
		var maximum: CurrentMaximum? = null
)

@JsonClass(generateAdapter = true)
data class Past6Hours(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Past24Hours(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Imperial(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentRealFeelTemperature(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDewPoint(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Past18Hours(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class PressureTendency(

    @field:Json(name = "Code")
		var code: String? = null,

    @field:Json(name = "LocalizedText")
		var localizedText: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWetBulbTemperature(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentVisibility(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentTemperature(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWindGust(

    @field:Json(name = "Speed")
		var speed: CurrentSpeed? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWind(

    @field:Json(name = "Speed")
		var speed: CurrentSpeed? = null,

    @field:Json(name = "Direction")
		var direction: CurrentDirection? = null
)

@JsonClass(generateAdapter = true)
data class Precipitation(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class ApparentTemperature(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Past24HourRange(

    @field:Json(name = "Minimum")
		var minimum: CurrentMinimum? = null,

    @field:Json(name = "Maximum")
		var maximum: CurrentMaximum? = null
)

@JsonClass(generateAdapter = true)
data class Past24HourTemperatureDeparture(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class Past9Hours(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)

@JsonClass(generateAdapter = true)
data class CurrentsResponseItem(

    @field:Json(name = "Wind")
		var wind: CurrentWind? = null,

    @field:Json(name = "Temperature")
		var temperature: CurrentTemperature? = null,

    @field:Json(name = "Past24HourTemperatureDeparture")
		var past24HourTemperatureDeparture: Past24HourTemperatureDeparture? = null,

    @field:Json(name = "PressureTendency")
		var pressureTendency: PressureTendency? = null,

    @field:Json(name = "ObstructionsToVisibility")
		var obstructionsToVisibility: String? = null,

    @field:Json(name = "Ceiling")
		var ceiling: CurrentCeiling? = null,

    @field:Json(name = "RealFeelTemperatureShade")
		var realFeelTemperatureShade: CurrentRealFeelTemperatureShade? = null,

    @field:Json(name = "EpochTime")
		var epochTime: Long? = null,

    @field:Json(name = "RealFeelTemperature")
		var realFeelTemperature: CurrentRealFeelTemperature? = null,

    @field:Json(name = "PrecipitationType")
		var precipitationType: String? = null,

    @field:Json(name = "HasPrecipitation")
		var hasPrecipitation: Boolean? = null,

    @field:Json(name = "RelativeHumidity")
		var relativeHumidity: Int? = null,

    @field:Json(name = "PrecipitationSummary")
		var precipitationSummary: PrecipitationSummary? = null,

    @field:Json(name = "TemperatureSummary")
		var temperatureSummary: TemperatureSummary? = null,

    @field:Json(name = "LocalObservationDateTime")
		var localObservationDateTime: String? = null,

    @field:Json(name = "UVIndexText")
		var uVIndexText: String? = null,

    @field:Json(name = "WeatherText")
		var weatherText: String? = null,

    @field:Json(name = "CloudCover")
		var cloudCover: Int? = null,

    @field:Json(name = "WindGust")
		var windGust: CurrentWindGust? = null,

    @field:Json(name = "UVIndex")
		var uVIndex: Float? = null,

    @field:Json(name = "Precip1hr")
		var precip1hr: Precip1hr? = null,

    @field:Json(name = "WeatherIcon")
		var weatherIcon: Int? = null,

    @field:Json(name = "DewPoint")
		var dewPoint: CurrentDewPoint? = null,

    @field:Json(name = "Pressure")
		var pressure: Pressure? = null,

    @field:Json(name = "IsDayTime")
		var isDayTime: Boolean? = null,

    @field:Json(name = "IndoorRelativeHumidity")
		var indoorRelativeHumidity: Int? = null,

    @field:Json(name = "ApparentTemperature")
		var apparentTemperature: ApparentTemperature? = null,

    @field:Json(name = "WetBulbTemperature")
		var wetBulbTemperature: CurrentWetBulbTemperature? = null,

    @field:Json(name = "Visibility")
		var visibility: CurrentVisibility? = null,

    @field:Json(name = "WindChillTemperature")
		var windChillTemperature: WindChillTemperature? = null,

    @field:Json(name = "Link")
		var link: String? = null,

    @field:Json(name = "MobileLink")
		var mobileLink: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentRealFeelTemperatureShade(

    @field:Json(name = "Metric")
		var metric: Metric? = null,

    @field:Json(name = "Imperial")
		var imperial: Imperial? = null
)
