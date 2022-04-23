package com.thewizrd.weather_api.accuweather.location

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class GeopositionResponse(

    @field:SerializedName("AdministrativeArea")
        var administrativeArea: AdministrativeArea? = null,

    @field:SerializedName("ParentCity")
        var parentCity: ParentCity? = null,

    @field:SerializedName("LocalizedName")
        var localizedName: String? = null,

    @field:SerializedName("SupplementalAdminAreas")
        var supplementalAdminAreas: List<SupplementalAdminAreasItem?>? = null,

    @field:SerializedName("DataSets")
        var dataSets: List<String?>? = null,

    @field:SerializedName("Rank")
        var rank: Int? = null,

    @field:SerializedName("IsAlias")
        var isAlias: Boolean? = null,

    @field:SerializedName("Type")
        var type: String? = null,

    @field:SerializedName("TimeZone")
        var timeZone: TimeZone? = null,

    @field:SerializedName("Version")
        var version: Int? = null,

    @field:SerializedName("PrimaryPostalCode")
        var primaryPostalCode: String? = null,

    @field:SerializedName("Region")
        var region: Region? = null,

    @field:SerializedName("Country")
        var country: Country? = null,

    @field:SerializedName("GeoPosition")
        var geoPosition: GeoPosition? = null,

    @field:SerializedName("Key")
        var key: String? = null,

    @field:SerializedName("EnglishName")
        var englishName: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ParentCity(

        @field:SerializedName("LocalizedName")
        var localizedName: String? = null,

        @field:SerializedName("Key")
        var key: String? = null,

        @field:SerializedName("EnglishName")
        var englishName: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class SupplementalAdminAreasItem(

        @field:SerializedName("LocalizedName")
        var localizedName: String? = null,

        @field:SerializedName("Level")
        var level: Int? = null,

        @field:SerializedName("EnglishName")
        var englishName: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Imperial(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Int? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class AdministrativeArea(

        @field:SerializedName("CountryID")
        var countryID: String? = null,

        @field:SerializedName("LocalizedType")
        var localizedType: String? = null,

        @field:SerializedName("LocalizedName")
        var localizedName: String? = null,

        @field:SerializedName("Level")
        var level: Int? = null,

        @field:SerializedName("ID")
        var iD: String? = null,

        @field:SerializedName("EnglishType")
        var englishType: String? = null,

        @field:SerializedName("EnglishName")
        var englishName: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class TimeZone(

        @field:SerializedName("NextOffsetChange")
        var nextOffsetChange: String? = null,

        @field:SerializedName("GmtOffset")
        var gmtOffset: Int? = null,

        @field:SerializedName("Code")
        var code: String? = null,

        @field:SerializedName("IsDaylightSaving")
        var isDaylightSaving: Boolean? = null,

        @field:SerializedName("Name")
        var name: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class GeoPosition(

    @field:SerializedName("Elevation")
        var elevation: Elevation? = null,

    @field:SerializedName("Latitude")
        var latitude: Double? = null,

    @field:SerializedName("Longitude")
        var longitude: Double? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Region(

        @field:SerializedName("LocalizedName")
        var localizedName: String? = null,

        @field:SerializedName("ID")
        var iD: String? = null,

        @field:SerializedName("EnglishName")
        var englishName: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Country(

        @field:SerializedName("LocalizedName")
        var localizedName: String? = null,

        @field:SerializedName("ID")
        var iD: String? = null,

        @field:SerializedName("EnglishName")
        var englishName: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Metric(

        @field:SerializedName("UnitType")
        var unitType: Int? = null,

        @field:SerializedName("Value")
        var value: Int? = null,

        @field:SerializedName("Unit")
        var unit: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Elevation(

    @field:SerializedName("Metric")
        var metric: Metric? = null,

    @field:SerializedName("Imperial")
        var imperial: Imperial? = null
)
