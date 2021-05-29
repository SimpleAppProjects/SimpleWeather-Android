package com.thewizrd.shared_resources.weatherdata.accuweather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class DailyResponse(

		@field:SerializedName("Headline")
		var headline: Headline? = null,

		@field:SerializedName("DailyForecasts")
		var dailyForecasts: List<DailyForecastsItem?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class AirAndPollenItem(

		@field:SerializedName("Category")
		var category: String? = null,

		@field:SerializedName("Value")
		var value: Int? = null,

		@field:SerializedName("CategoryValue")
		var categoryValue: Int? = null,

		@field:SerializedName("Name")
		var name: String? = null,

		@field:SerializedName("Type")
		var type: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class RealFeelTemperatureShade(

		@field:SerializedName("Minimum")
		var minimum: Minimum? = null,

		@field:SerializedName("Maximum")
		var maximum: Maximum? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DegreeDaySummary(

		@field:SerializedName("Cooling")
		var cooling: Cooling? = null,

		@field:SerializedName("Heating")
		var heating: Heating? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DailyForecastsItem(

		@field:SerializedName("Temperature")
		var temperature: DailyTemperature? = null,

		@field:SerializedName("Night")
		var night: Night? = null,

		@field:SerializedName("EpochDate")
		var epochDate: Long? = null,

		@field:SerializedName("Moon")
		var moon: Moon? = null,

		@field:SerializedName("DegreeDaySummary")
		var degreeDaySummary: DegreeDaySummary? = null,

		@field:SerializedName("RealFeelTemperatureShade")
		var realFeelTemperatureShade: RealFeelTemperatureShade? = null,

		@field:SerializedName("AirAndPollen")
		var airAndPollen: List<AirAndPollenItem?>? = null,

		@field:SerializedName("HoursOfSun")
		var hoursOfSun: Float? = null,

		@field:SerializedName("Sun")
		var sun: Sun? = null,

		@field:SerializedName("Sources")
		var sources: List<String?>? = null,

		@field:SerializedName("Date")
		var date: String? = null,

		@field:SerializedName("RealFeelTemperature")
		var realFeelTemperature: DailyRealFeelTemperature? = null,

		@field:SerializedName("Day")
		var day: Day? = null,

		@field:SerializedName("Link")
		var link: String? = null,

		@field:SerializedName("MobileLink")
		var mobileLink: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Day(

		@field:SerializedName("RainProbability")
		var rainProbability: Int? = null,

		@field:SerializedName("Wind")
		var wind: Wind? = null,

		@field:SerializedName("SnowProbability")
		var snowProbability: Int? = null,

		@field:SerializedName("Snow")
		var snow: Snow? = null,

		@field:SerializedName("TotalLiquid")
		var totalLiquid: TotalLiquid? = null,

		@field:SerializedName("ShortPhrase")
		var shortPhrase: String? = null,

		@field:SerializedName("Ice")
		var ice: Ice? = null,

		@field:SerializedName("HoursOfRain")
		var hoursOfRain: Float? = null,

		@field:SerializedName("HoursOfIce")
		var hoursOfIce: Float? = null,

		@field:SerializedName("Rain")
		var rain: Rain? = null,

		@field:SerializedName("PrecipitationProbability")
		var precipitationProbability: Int? = null,

		@field:SerializedName("HasPrecipitation")
		var hasPrecipitation: Boolean? = null,

		@field:SerializedName("ThunderstormProbability")
		var thunderstormProbability: Int? = null,

		@field:SerializedName("IceProbability")
		var iceProbability: Int? = null,

		@field:SerializedName("IconPhrase")
		var iconPhrase: String? = null,

		@field:SerializedName("CloudCover")
		var cloudCover: Int? = null,

		@field:SerializedName("LongPhrase")
		var longPhrase: String? = null,

		@field:SerializedName("Icon")
		var icon: Int? = null,

		@field:SerializedName("WindGust")
		var windGust: WindGust? = null,

		@field:SerializedName("HoursOfSnow")
		var hoursOfSnow: Float? = null,

		@field:SerializedName("HoursOfPrecipitation")
		var hoursOfPrecipitation: Float? = null,

		@field:SerializedName("PrecipitationIntensity")
		var precipitationIntensity: String? = null,

		@field:SerializedName("PrecipitationType")
		var precipitationType: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Night(

		@field:SerializedName("RainProbability")
		var rainProbability: Int? = null,

		@field:SerializedName("Wind")
		var wind: Wind? = null,

		@field:SerializedName("SnowProbability")
		var snowProbability: Int? = null,

		@field:SerializedName("Snow")
		var snow: Snow? = null,

		@field:SerializedName("TotalLiquid")
		var totalLiquid: TotalLiquid? = null,

		@field:SerializedName("ShortPhrase")
		var shortPhrase: String? = null,

		@field:SerializedName("Ice")
		var ice: Ice? = null,

		@field:SerializedName("HoursOfRain")
		var hoursOfRain: Float? = null,

		@field:SerializedName("HoursOfIce")
		var hoursOfIce: Float? = null,

		@field:SerializedName("Rain")
		var rain: Rain? = null,

		@field:SerializedName("PrecipitationProbability")
		var precipitationProbability: Int? = null,

		@field:SerializedName("HasPrecipitation")
		var hasPrecipitation: Boolean? = null,

		@field:SerializedName("ThunderstormProbability")
		var thunderstormProbability: Int? = null,

		@field:SerializedName("IceProbability")
		var iceProbability: Int? = null,

		@field:SerializedName("IconPhrase")
		var iconPhrase: String? = null,

		@field:SerializedName("CloudCover")
		var cloudCover: Int? = null,

		@field:SerializedName("LongPhrase")
		var longPhrase: String? = null,

		@field:SerializedName("Icon")
		var icon: Int? = null,

		@field:SerializedName("WindGust")
		var windGust: WindGust? = null,

		@field:SerializedName("HoursOfSnow")
		var hoursOfSnow: Float? = null,

		@field:SerializedName("HoursOfPrecipitation")
		var hoursOfPrecipitation: Float? = null,

		@field:SerializedName("PrecipitationIntensity")
		var precipitationIntensity: String? = null,

		@field:SerializedName("PrecipitationType")
		var precipitationType: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DailyTemperature(

		@field:SerializedName("Minimum")
		var minimum: Minimum? = null,

		@field:SerializedName("Maximum")
		var maximum: Maximum? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Rain(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class WindGust(

		@field:SerializedName("Speed")
		var speed: Speed? = null,

		@field:SerializedName("Direction")
		var direction: Direction? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Direction(

		@field:SerializedName("English")
		var english: String? = null,

		@field:SerializedName("Degrees")
		var degrees: Float? = null,

		@field:SerializedName("Localized")
		var localized: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Speed(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Ice(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Sun(

		@field:SerializedName("EpochSet")
		var epochSet: Long? = null,

		@field:SerializedName("Set")
		var set: String? = null,

		@field:SerializedName("EpochRise")
		var epochRise: Long? = null,

		@field:SerializedName("Rise")
		var rise: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DailyRealFeelTemperature(

		@field:SerializedName("Minimum")
		var minimum: Minimum? = null,

		@field:SerializedName("Maximum")
		var maximum: Maximum? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Cooling(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Snow(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Headline(

		@field:SerializedName("Category")
		var category: String? = null,

		@field:SerializedName("EndEpochDate")
		var endEpochDate: Long? = null,

		@field:SerializedName("EffectiveEpochDate")
		var effectiveEpochDate: Long? = null,

		@field:SerializedName("Severity")
		var severity: Int? = null,

		@field:SerializedName("Text")
		var text: String? = null,

		@field:SerializedName("EndDate")
		var endDate: String? = null,

		@field:SerializedName("Link")
		var link: String? = null,

		@field:SerializedName("EffectiveDate")
		var effectiveDate: String? = null,

		@field:SerializedName("MobileLink")
		var mobileLink: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class TotalLiquid(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Maximum(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Wind(

		@field:SerializedName("Speed")
		var speed: Speed? = null,

		@field:SerializedName("Direction")
		var direction: Direction? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Moon(

		@field:SerializedName("EpochSet")
		var epochSet: Long? = null,

		@field:SerializedName("Set")
		var set: String? = null,

		@field:SerializedName("Phase")
		var phase: String? = null,

		@field:SerializedName("EpochRise")
		var epochRise: Long? = null,

		@field:SerializedName("Age")
		var age: Int? = null,

		@field:SerializedName("Rise")
		var rise: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Heating(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Minimum(

		@field:SerializedName("UnitType")
		var unitType: Int? = null,

		@field:SerializedName("Value")
		var value: Float? = null,

		@field:SerializedName("Unit")
		var unit: String? = null
)
