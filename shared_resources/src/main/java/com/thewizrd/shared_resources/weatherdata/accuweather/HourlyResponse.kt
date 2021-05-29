package com.thewizrd.shared_resources.weatherdata.accuweather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class HourlyResponse(

        var hourlyResponse: List<HourlyResponseItem?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class WetBulbTemperature(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Float? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DewPoint(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Float? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Visibility(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Float? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Temperature(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Float? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class HourlyResponseItem(

        @field:SerializedName("RainProbability")
        var rainProbability: Int? = null,

        @field:SerializedName("Wind")
        var wind: Wind? = null,

        @field:SerializedName("Temperature")
        var temperature: Temperature? = null,

        @field:SerializedName("SnowProbability")
        var snowProbability: Int? = null,

        @field:SerializedName("Snow")
        var snow: Snow? = null,

        @field:SerializedName("TotalLiquid")
        var totalLiquid: TotalLiquid? = null,

        @field:SerializedName("Ceiling")
        var ceiling: Ceiling? = null,

        @field:SerializedName("DateTime")
        var dateTime: String? = null,

        @field:SerializedName("RealFeelTemperature")
        var realFeelTemperature: RealFeelTemperature? = null,

        @field:SerializedName("Rain")
        var rain: Rain? = null,

        @field:SerializedName("PrecipitationProbability")
        var precipitationProbability: Int? = null,

        @field:SerializedName("HasPrecipitation")
        var hasPrecipitation: Boolean? = null,

        @field:SerializedName("RelativeHumidity")
        var relativeHumidity: Int? = null,

        @field:SerializedName("UVIndexText")
        var uVIndexText: String? = null,

        @field:SerializedName("IconPhrase")
        var iconPhrase: String? = null,

        @field:SerializedName("CloudCover")
        var cloudCover: Int? = null,

        @field:SerializedName("WindGust")
        var windGust: WindGust? = null,

        @field:SerializedName("UVIndex")
        var uVIndex: Float? = null,

        @field:SerializedName("WeatherIcon")
        var weatherIcon: Int? = null,

        @field:SerializedName("Ice")
        var ice: Ice? = null,

        @field:SerializedName("DewPoint")
        var dewPoint: DewPoint? = null,

        @field:SerializedName("IndoorRelativeHumidity")
        var indoorRelativeHumidity: Int? = null,

        @field:SerializedName("IceProbability")
        var iceProbability: Int? = null,

        @field:SerializedName("EpochDateTime")
        var epochDateTime: Long? = null,

        @field:SerializedName("WetBulbTemperature")
        var wetBulbTemperature: WetBulbTemperature? = null,

        @field:SerializedName("Visibility")
        var visibility: Visibility? = null,

        @field:SerializedName("IsDaylight")
        var isDaylight: Boolean? = null,

        @field:SerializedName("Link")
        var link: String? = null,

        @field:SerializedName("MobileLink")
        var mobileLink: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Ceiling(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Float? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class RealFeelTemperature(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Float? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)
