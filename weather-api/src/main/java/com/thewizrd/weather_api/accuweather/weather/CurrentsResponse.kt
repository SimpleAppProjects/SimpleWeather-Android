package com.thewizrd.weather_api.accuweather.weather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class CurrentsResponse(

		var currentsResponse: List<CurrentsResponseItem?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentMinimum(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Precip1hr(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past6HourRange(

    @field:SerializedName("Minimum")
		var minimum: CurrentMinimum? = null,

    @field:SerializedName("Maximum")
		var maximum: CurrentMaximum? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class PastHour(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Metric(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentSpeed(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class TemperatureSummary(

    @field:SerializedName("Past6HourRange")
		var past6HourRange: Past6HourRange? = null,

    @field:SerializedName("Past24HourRange")
		var past24HourRange: Past24HourRange? = null,

    @field:SerializedName("Past12HourRange")
		var past12HourRange: Past12HourRange? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past12Hours(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentMaximum(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Pressure(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class WindChillTemperature(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class PrecipitationSummary(

    @field:SerializedName("Past6Hours")
		var past6Hours: Past6Hours? = null,

    @field:SerializedName("Precipitation")
		var precipitation: Precipitation? = null,

    @field:SerializedName("Past9Hours")
		var past9Hours: Past9Hours? = null,

    @field:SerializedName("Past3Hours")
		var past3Hours: Past3Hours? = null,

    @field:SerializedName("PastHour")
		var pastHour: PastHour? = null,

    @field:SerializedName("Past18Hours")
		var past18Hours: Past18Hours? = null,

    @field:SerializedName("Past24Hours")
		var past24Hours: Past24Hours? = null,

    @field:SerializedName("Past12Hours")
		var past12Hours: Past12Hours? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past3Hours(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentDirection(

		@field:SerializedName("English")
		var english: String? = null,

		@field:SerializedName("Degrees")
		var degrees: Int? = null,

		@field:SerializedName("Localized")
		var localized: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentCeiling(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past12HourRange(

    @field:SerializedName("Minimum")
		var minimum: CurrentMinimum? = null,

    @field:SerializedName("Maximum")
		var maximum: CurrentMaximum? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past6Hours(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past24Hours(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Imperial(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentRealFeelTemperature(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentDewPoint(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past18Hours(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class PressureTendency(

		@field:SerializedName("Code")
		var code: String? = null,

		@field:SerializedName("LocalizedText")
		var localizedText: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentWetBulbTemperature(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentVisibility(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentTemperature(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentWindGust(

		@field:SerializedName("Speed")
		var speed: CurrentSpeed? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentWind(

    @field:SerializedName("Speed")
		var speed: CurrentSpeed? = null,

    @field:SerializedName("Direction")
		var direction: CurrentDirection? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Precipitation(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ApparentTemperature(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past24HourRange(

    @field:SerializedName("Minimum")
		var minimum: CurrentMinimum? = null,

    @field:SerializedName("Maximum")
		var maximum: CurrentMaximum? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past24HourTemperatureDeparture(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Past9Hours(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentsResponseItem(

    @field:SerializedName("Wind")
		var wind: CurrentWind? = null,

    @field:SerializedName("Temperature")
		var temperature: CurrentTemperature? = null,

    @field:SerializedName("Past24HourTemperatureDeparture")
		var past24HourTemperatureDeparture: Past24HourTemperatureDeparture? = null,

    @field:SerializedName("PressureTendency")
		var pressureTendency: PressureTendency? = null,

    @field:SerializedName("ObstructionsToVisibility")
		var obstructionsToVisibility: String? = null,

    @field:SerializedName("Ceiling")
		var ceiling: CurrentCeiling? = null,

    @field:SerializedName("RealFeelTemperatureShade")
		var realFeelTemperatureShade: CurrentRealFeelTemperatureShade? = null,

    @field:SerializedName("EpochTime")
		var epochTime: Long? = null,

    @field:SerializedName("RealFeelTemperature")
		var realFeelTemperature: CurrentRealFeelTemperature? = null,

    @field:SerializedName("PrecipitationType")
		var precipitationType: String? = null,

    @field:SerializedName("HasPrecipitation")
		var hasPrecipitation: Boolean? = null,

    @field:SerializedName("RelativeHumidity")
		var relativeHumidity: Int? = null,

    @field:SerializedName("PrecipitationSummary")
		var precipitationSummary: PrecipitationSummary? = null,

    @field:SerializedName("TemperatureSummary")
		var temperatureSummary: TemperatureSummary? = null,

    @field:SerializedName("LocalObservationDateTime")
		var localObservationDateTime: String? = null,

    @field:SerializedName("UVIndexText")
		var uVIndexText: String? = null,

    @field:SerializedName("WeatherText")
		var weatherText: String? = null,

    @field:SerializedName("CloudCover")
		var cloudCover: Int? = null,

    @field:SerializedName("WindGust")
		var windGust: CurrentWindGust? = null,

    @field:SerializedName("UVIndex")
		var uVIndex: Float? = null,

    @field:SerializedName("Precip1hr")
		var precip1hr: Precip1hr? = null,

    @field:SerializedName("WeatherIcon")
		var weatherIcon: Int? = null,

    @field:SerializedName("DewPoint")
		var dewPoint: CurrentDewPoint? = null,

    @field:SerializedName("Pressure")
		var pressure: Pressure? = null,

    @field:SerializedName("IsDayTime")
		var isDayTime: Boolean? = null,

    @field:SerializedName("IndoorRelativeHumidity")
		var indoorRelativeHumidity: Int? = null,

    @field:SerializedName("ApparentTemperature")
		var apparentTemperature: ApparentTemperature? = null,

    @field:SerializedName("WetBulbTemperature")
		var wetBulbTemperature: CurrentWetBulbTemperature? = null,

    @field:SerializedName("Visibility")
		var visibility: CurrentVisibility? = null,

    @field:SerializedName("WindChillTemperature")
		var windChillTemperature: WindChillTemperature? = null,

    @field:SerializedName("Link")
		var link: String? = null,

    @field:SerializedName("MobileLink")
		var mobileLink: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentRealFeelTemperatureShade(

    @field:SerializedName("Metric")
		var metric: Metric? = null,

    @field:SerializedName("Imperial")
		var imperial: Imperial? = null
)
