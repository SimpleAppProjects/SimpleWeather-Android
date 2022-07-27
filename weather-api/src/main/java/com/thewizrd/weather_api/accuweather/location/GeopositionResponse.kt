package com.thewizrd.weather_api.accuweather.location

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeopositionResponse(

    @field:Json(name = "AdministrativeArea")
        var administrativeArea: AdministrativeArea? = null,

    @field:Json(name = "ParentCity")
        var parentCity: ParentCity? = null,

    @field:Json(name = "LocalizedName")
        var localizedName: String? = null,

    @field:Json(name = "SupplementalAdminAreas")
        var supplementalAdminAreas: List<SupplementalAdminAreasItem?>? = null,

    @field:Json(name = "DataSets")
        var dataSets: List<String?>? = null,

    @field:Json(name = "Rank")
        var rank: Int? = null,

    @field:Json(name = "IsAlias")
        var isAlias: Boolean? = null,

    @field:Json(name = "Type")
        var type: String? = null,

    @field:Json(name = "TimeZone")
        var timeZone: TimeZone? = null,

    @field:Json(name = "Version")
        var version: Int? = null,

    @field:Json(name = "PrimaryPostalCode")
        var primaryPostalCode: String? = null,

    @field:Json(name = "Region")
        var region: Region? = null,

    @field:Json(name = "Country")
        var country: Country? = null,

    @field:Json(name = "GeoPosition")
        var geoPosition: GeoPosition? = null,

    @field:Json(name = "Key")
        var key: String? = null,

    @field:Json(name = "EnglishName")
        var englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class ParentCity(

    @field:Json(name = "LocalizedName")
        var localizedName: String? = null,

    @field:Json(name = "Key")
        var key: String? = null,

    @field:Json(name = "EnglishName")
        var englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class SupplementalAdminAreasItem(

    @field:Json(name = "LocalizedName")
        var localizedName: String? = null,

    @field:Json(name = "Level")
        var level: Int? = null,

    @field:Json(name = "EnglishName")
        var englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class Imperial(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Int? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class AdministrativeArea(

    @field:Json(name = "CountryID")
        var countryID: String? = null,

    @field:Json(name = "LocalizedType")
        var localizedType: String? = null,

    @field:Json(name = "LocalizedName")
        var localizedName: String? = null,

    @field:Json(name = "Level")
        var level: Int? = null,

    @field:Json(name = "ID")
        var iD: String? = null,

    @field:Json(name = "EnglishType")
        var englishType: String? = null,

    @field:Json(name = "EnglishName")
        var englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class TimeZone(

    @field:Json(name = "NextOffsetChange")
        var nextOffsetChange: String? = null,

    @field:Json(name = "GmtOffset")
        var gmtOffset: Int? = null,

    @field:Json(name = "Code")
        var code: String? = null,

    @field:Json(name = "IsDaylightSaving")
        var isDaylightSaving: Boolean? = null,

    @field:Json(name = "Name")
        var name: String? = null
)

@JsonClass(generateAdapter = true)
data class GeoPosition(

    @field:Json(name = "Elevation")
        var elevation: Elevation? = null,

    @field:Json(name = "Latitude")
        var latitude: Double? = null,

    @field:Json(name = "Longitude")
        var longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class Region(

    @field:Json(name = "LocalizedName")
        var localizedName: String? = null,

    @field:Json(name = "ID")
        var iD: String? = null,

    @field:Json(name = "EnglishName")
        var englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class Country(

    @field:Json(name = "LocalizedName")
        var localizedName: String? = null,

    @field:Json(name = "ID")
        var iD: String? = null,

    @field:Json(name = "EnglishName")
        var englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class Metric(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Int? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Elevation(

    @field:Json(name = "Metric")
        var metric: Metric? = null,

    @field:Json(name = "Imperial")
        var imperial: Imperial? = null
)
