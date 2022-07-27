package com.thewizrd.weather_api.accuweather.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DailyResponse(

    @field:Json(name = "Headline")
		var headline: Headline? = null,

    @field:Json(name = "DailyForecasts")
		var dailyForecasts: List<DailyForecastsItem?>? = null
)

@JsonClass(generateAdapter = true)
data class AirAndPollenItem(

    @field:Json(name = "Category")
		var category: String? = null,

    @field:Json(name = "Value")
		var value: Int? = null,

    @field:Json(name = "CategoryValue")
		var categoryValue: Int? = null,

    @field:Json(name = "Name")
		var name: String? = null,

    @field:Json(name = "Type")
		var type: String? = null
)

@JsonClass(generateAdapter = true)
data class RealFeelTemperatureShade(

    @field:Json(name = "Minimum")
		var minimum: Minimum? = null,

    @field:Json(name = "Maximum")
		var maximum: Maximum? = null
)

@JsonClass(generateAdapter = true)
data class DegreeDaySummary(

    @field:Json(name = "Cooling")
		var cooling: Cooling? = null,

    @field:Json(name = "Heating")
		var heating: Heating? = null
)

@JsonClass(generateAdapter = true)
data class DailyForecastsItem(

    @field:Json(name = "Temperature")
		var temperature: DailyTemperature? = null,

    @field:Json(name = "Night")
		var night: Night? = null,

    @field:Json(name = "EpochDate")
		var epochDate: Long? = null,

    @field:Json(name = "Moon")
		var moon: Moon? = null,

    @field:Json(name = "DegreeDaySummary")
		var degreeDaySummary: DegreeDaySummary? = null,

    @field:Json(name = "RealFeelTemperatureShade")
		var realFeelTemperatureShade: RealFeelTemperatureShade? = null,

    @field:Json(name = "AirAndPollen")
		var airAndPollen: List<AirAndPollenItem?>? = null,

    @field:Json(name = "HoursOfSun")
		var hoursOfSun: Float? = null,

    @field:Json(name = "Sun")
		var sun: Sun? = null,

    @field:Json(name = "Sources")
		var sources: List<String?>? = null,

    @field:Json(name = "Date")
		var date: String? = null,

    @field:Json(name = "RealFeelTemperature")
		var realFeelTemperature: DailyRealFeelTemperature? = null,

    @field:Json(name = "Day")
		var day: Day? = null,

    @field:Json(name = "Link")
		var link: String? = null,

    @field:Json(name = "MobileLink")
		var mobileLink: String? = null
)

@JsonClass(generateAdapter = true)
data class Day(

    @field:Json(name = "RainProbability")
		var rainProbability: Int? = null,

    @field:Json(name = "Wind")
		var wind: Wind? = null,

    @field:Json(name = "SnowProbability")
		var snowProbability: Int? = null,

    @field:Json(name = "Snow")
		var snow: Snow? = null,

    @field:Json(name = "TotalLiquid")
		var totalLiquid: TotalLiquid? = null,

    @field:Json(name = "ShortPhrase")
		var shortPhrase: String? = null,

    @field:Json(name = "Ice")
		var ice: Ice? = null,

    @field:Json(name = "HoursOfRain")
		var hoursOfRain: Float? = null,

    @field:Json(name = "HoursOfIce")
		var hoursOfIce: Float? = null,

    @field:Json(name = "Rain")
		var rain: Rain? = null,

    @field:Json(name = "PrecipitationProbability")
		var precipitationProbability: Int? = null,

    @field:Json(name = "HasPrecipitation")
		var hasPrecipitation: Boolean? = null,

    @field:Json(name = "ThunderstormProbability")
		var thunderstormProbability: Int? = null,

    @field:Json(name = "IceProbability")
		var iceProbability: Int? = null,

    @field:Json(name = "IconPhrase")
		var iconPhrase: String? = null,

    @field:Json(name = "CloudCover")
		var cloudCover: Int? = null,

    @field:Json(name = "LongPhrase")
		var longPhrase: String? = null,

    @field:Json(name = "Icon")
		var icon: Int? = null,

    @field:Json(name = "WindGust")
		var windGust: WindGust? = null,

    @field:Json(name = "HoursOfSnow")
		var hoursOfSnow: Float? = null,

    @field:Json(name = "HoursOfPrecipitation")
		var hoursOfPrecipitation: Float? = null,

    @field:Json(name = "PrecipitationIntensity")
		var precipitationIntensity: String? = null,

    @field:Json(name = "PrecipitationType")
		var precipitationType: String? = null
)

@JsonClass(generateAdapter = true)
data class Night(

    @field:Json(name = "RainProbability")
		var rainProbability: Int? = null,

    @field:Json(name = "Wind")
		var wind: Wind? = null,

    @field:Json(name = "SnowProbability")
		var snowProbability: Int? = null,

    @field:Json(name = "Snow")
		var snow: Snow? = null,

    @field:Json(name = "TotalLiquid")
		var totalLiquid: TotalLiquid? = null,

    @field:Json(name = "ShortPhrase")
		var shortPhrase: String? = null,

    @field:Json(name = "Ice")
		var ice: Ice? = null,

    @field:Json(name = "HoursOfRain")
		var hoursOfRain: Float? = null,

    @field:Json(name = "HoursOfIce")
		var hoursOfIce: Float? = null,

    @field:Json(name = "Rain")
		var rain: Rain? = null,

    @field:Json(name = "PrecipitationProbability")
		var precipitationProbability: Int? = null,

    @field:Json(name = "HasPrecipitation")
		var hasPrecipitation: Boolean? = null,

    @field:Json(name = "ThunderstormProbability")
		var thunderstormProbability: Int? = null,

    @field:Json(name = "IceProbability")
		var iceProbability: Int? = null,

    @field:Json(name = "IconPhrase")
		var iconPhrase: String? = null,

    @field:Json(name = "CloudCover")
		var cloudCover: Int? = null,

    @field:Json(name = "LongPhrase")
		var longPhrase: String? = null,

    @field:Json(name = "Icon")
		var icon: Int? = null,

    @field:Json(name = "WindGust")
		var windGust: WindGust? = null,

    @field:Json(name = "HoursOfSnow")
		var hoursOfSnow: Float? = null,

    @field:Json(name = "HoursOfPrecipitation")
		var hoursOfPrecipitation: Float? = null,

    @field:Json(name = "PrecipitationIntensity")
		var precipitationIntensity: String? = null,

    @field:Json(name = "PrecipitationType")
		var precipitationType: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyTemperature(

    @field:Json(name = "Minimum")
		var minimum: Minimum? = null,

    @field:Json(name = "Maximum")
		var maximum: Maximum? = null
)

@JsonClass(generateAdapter = true)
data class Rain(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class WindGust(

    @field:Json(name = "Speed")
		var speed: Speed? = null,

    @field:Json(name = "Direction")
		var direction: Direction? = null
)

@JsonClass(generateAdapter = true)
data class Direction(

    @field:Json(name = "English")
		var english: String? = null,

    @field:Json(name = "Degrees")
		var degrees: Float? = null,

    @field:Json(name = "Localized")
		var localized: String? = null
)

@JsonClass(generateAdapter = true)
data class Speed(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Ice(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Sun(

    @field:Json(name = "EpochSet")
		var epochSet: Long? = null,

    @field:Json(name = "Set")
		var set: String? = null,

    @field:Json(name = "EpochRise")
		var epochRise: Long? = null,

    @field:Json(name = "Rise")
		var rise: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyRealFeelTemperature(

    @field:Json(name = "Minimum")
		var minimum: Minimum? = null,

    @field:Json(name = "Maximum")
		var maximum: Maximum? = null
)

@JsonClass(generateAdapter = true)
data class Cooling(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Snow(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Headline(

    @field:Json(name = "Category")
		var category: String? = null,

    @field:Json(name = "EndEpochDate")
		var endEpochDate: Long? = null,

    @field:Json(name = "EffectiveEpochDate")
		var effectiveEpochDate: Long? = null,

    @field:Json(name = "Severity")
		var severity: Int? = null,

    @field:Json(name = "Text")
		var text: String? = null,

    @field:Json(name = "EndDate")
		var endDate: String? = null,

    @field:Json(name = "Link")
		var link: String? = null,

    @field:Json(name = "EffectiveDate")
		var effectiveDate: String? = null,

    @field:Json(name = "MobileLink")
		var mobileLink: String? = null
)

@JsonClass(generateAdapter = true)
data class TotalLiquid(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Maximum(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Wind(

    @field:Json(name = "Speed")
		var speed: Speed? = null,

    @field:Json(name = "Direction")
		var direction: Direction? = null
)

@JsonClass(generateAdapter = true)
data class Moon(

    @field:Json(name = "EpochSet")
		var epochSet: Long? = null,

    @field:Json(name = "Set")
		var set: String? = null,

    @field:Json(name = "Phase")
		var phase: String? = null,

    @field:Json(name = "EpochRise")
		var epochRise: Long? = null,

    @field:Json(name = "Age")
		var age: Int? = null,

    @field:Json(name = "Rise")
		var rise: String? = null
)

@JsonClass(generateAdapter = true)
data class Heating(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Minimum(

    @field:Json(name = "UnitType")
		var unitType: Int? = null,

    @field:Json(name = "Value")
		var value: Float? = null,

    @field:Json(name = "Unit")
		var unit: String? = null
)
