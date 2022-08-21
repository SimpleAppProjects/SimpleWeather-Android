package com.thewizrd.common.controls

import android.annotation.SuppressLint
import android.text.format.DateFormat
import androidx.annotation.RestrictTo
import androidx.core.util.ObjectsCompat
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Units.TemperatureUnits
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.weatherModule
import java.text.DecimalFormat
import kotlin.math.roundToInt

class WeatherUiModel() {
    @set:RestrictTo(RestrictTo.Scope.LIBRARY, RestrictTo.Scope.TESTS)
    var location: String? = null

    var updateDate: String? = null
        private set

    // Current Condition
    var curTemp: String? = null
        private set

    var curCondition: String? = null
        private set

    var weatherIcon: String = WeatherIcons.NA
        private set

    var hiTemp: String? = null
        private set

    var loTemp: String? = null
        private set

    var isShowHiLo = false
        private set

    var weatherSummary: String? = null
        private set

    // Weather Details
    var sunPhase: SunPhaseViewModel? = null
        private set

    var uvIndex: UVIndexViewModel? = null
        private set

    var beaufort: BeaufortViewModel? = null
        private set

    var moonPhase: MoonPhaseViewModel? = null
        private set

    var airQuality: AirQualityViewModel? = null
        private set

    var pollen: PollenViewModel? = null
        private set

    // Radar
    val locationCoord: Coordinate

    var weatherCredit: String? = null
        private set

    var weatherSource: String? = null
        private set

    var weatherLocale: String? = null
        private set

    val weatherDetailsMap: MutableMap<WeatherDetailsType, DetailItemViewModel>

    val query: String?
        get() = weatherData?.query

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var weatherData: Weather? = null
        private set

    @get:TemperatureUnits
    var tempUnit: String? = null
        private set

    private var unitCode: String? = null
    private var localeCode: String? = null
    var iconProvider: String? = null
        private set

    private val context
        get() = sharedDeps.context
    private val isPhone
        get() = appLib.isPhone

    init {
        weatherDetailsMap = LinkedHashMap(WeatherDetailsType.values().size)
        locationCoord = Coordinate(0.0, 0.0)
    }

    constructor(weather: Weather?) : this() {
        updateView(weather)
    }

    @SuppressLint("RestrictedApi")
    fun updateView(weather: Weather?) {
        if (weather != null && weather.isValid) {
            if (weatherData != weather) {
                weatherData = weather

                // Location
                location = weather.location.name

                // Summary
                weatherSummary = weather.condition.summary

                // Additional Details
                if (weather.location.latitude != null && weather.location.longitude != null) {
                    locationCoord.setCoordinate(
                        weather.location.latitude.toDouble(),
                        weather.location.longitude.toDouble()
                    )
                } else {
                    locationCoord.setCoordinate(0.0, 0.0)
                }

                // Additional Details
                weatherSource = weather.source

                // Language
                weatherLocale = weather.locale

                // Refresh locale/unit dependent values
                refreshView()
            } else if (unitCode != settingsManager.getUnitString() ||
                localeCode != LocaleUtils.getLocaleCode() ||
                iconProvider != settingsManager.getIconsProvider()
            ) {
                refreshView()
            }
        }
    }

    private fun refreshView() {
        val provider = weatherModule.weatherManager.getWeatherProvider(weatherData!!.source)

        val isFahrenheit = Units.FAHRENHEIT == settingsManager.getTemperatureUnit()

        val df = DecimalFormat.getInstance(LocaleUtils.getLocale()) as DecimalFormat
        df.applyPattern("0.##")

        tempUnit = settingsManager.getTemperatureUnit()
        unitCode = settingsManager.getUnitString()

        localeCode = LocaleUtils.getLocaleCode()
        iconProvider = settingsManager.getIconsProvider()

        // Date Updated
        updateDate = getLastBuildDate(weatherData!!)

        // Update current condition
        curTemp = if (weatherData?.condition?.tempF != null &&
            weatherData!!.condition.tempF != weatherData!!.condition.tempC
        ) {
            val temp = if (isFahrenheit) Math.round(weatherData!!.condition.tempF) else Math.round(
                weatherData!!.condition.tempC
            )
            String.format(LocaleUtils.getLocale(), "%d°%s", temp, tempUnit)
        } else {
            WeatherIcons.PLACEHOLDER
        }

        val weatherCondition =
            if (provider.supportsWeatherLocale()) weatherData!!.condition.weather else provider.getWeatherCondition(
                weatherData!!.condition.icon
            )
        curCondition =
            if (weatherCondition.isNullOrBlank()) WeatherIcons.EM_DASH else weatherCondition

        weatherIcon = weatherData!!.condition.icon

        run {
            var shouldHideHi = false
            var shouldHideLo = false

            val newHiTemp: String
            if (weatherData?.condition?.highF != null &&
                weatherData!!.condition.highF != weatherData!!.condition.highC
            ) {
                newHiTemp =
                    (if (isFahrenheit) Math.round(weatherData!!.condition.highF) else Math.round(
                        weatherData!!.condition.highC
                    )).toString() + "°"
            } else {
                newHiTemp = WeatherIcons.PLACEHOLDER
                shouldHideHi = true
            }
            hiTemp = newHiTemp

            val newLoTemp: String
            if (weatherData?.condition?.lowF != null &&
                weatherData!!.condition.lowF != weatherData!!.condition.lowC) {
                newLoTemp = (if (isFahrenheit) Math.round(weatherData!!.condition.lowF) else Math.round(weatherData!!.condition.lowC)).toString() + "°"
            } else {
                newLoTemp = WeatherIcons.PLACEHOLDER
                shouldHideLo = true
            }
            loTemp = newLoTemp

            this.isShowHiLo = (!shouldHideHi || !shouldHideLo) && hiTemp != loTemp
        }

        // WeatherDetails
        weatherDetailsMap.clear()
        // Precipitation
        if (weatherData?.precipitation != null) {
            if (weatherData?.precipitation?.pop != null && weatherData!!.precipitation.pop >= 0) {
                weatherDetailsMap[WeatherDetailsType.POPCHANCE] =
                    DetailItemViewModel(
                        WeatherDetailsType.POPCHANCE,
                        weatherData!!.precipitation.pop.toString() + "%"
                    )
            }
            if (weatherData?.precipitation?.qpfRainIn != null && weatherData!!.precipitation.qpfRainIn >= 0) {
                val unit = settingsManager.getPrecipitationUnit()
                val precipValue: Float
                val precipUnit: String

                when (unit) {
                    Units.INCHES -> {
                        precipValue = weatherData!!.precipitation.qpfRainIn
                        precipUnit = context.getString(R.string.unit_in)
                    }
                    Units.MILLIMETERS -> {
                        precipValue = weatherData!!.precipitation.qpfRainMm
                        precipUnit = context.getString(R.string.unit_mm)
                    }
                    else -> {
                        precipValue = weatherData!!.precipitation.qpfRainIn
                        precipUnit = context.getString(R.string.unit_in)
                    }
                }
                weatherDetailsMap[WeatherDetailsType.POPRAIN] = DetailItemViewModel(
                    WeatherDetailsType.POPRAIN,
                    String.format(
                        LocaleUtils.getLocale(),
                        "%s %s",
                        df.format(precipValue.toDouble()),
                        precipUnit
                    )
                )
            }
            if (weatherData?.precipitation?.qpfSnowIn != null && weatherData!!.precipitation.qpfSnowIn >= 0) {
                val unit = settingsManager.getPrecipitationUnit()
                val precipValue: Float
                val precipUnit: String

                when (unit) {
                    Units.INCHES -> {
                        precipValue = weatherData!!.precipitation.qpfSnowIn
                        precipUnit = context.getString(R.string.unit_in)
                    }
                    Units.MILLIMETERS -> {
                        precipValue = weatherData!!.precipitation.qpfSnowCm * 10
                        precipUnit = context.getString(R.string.unit_mm)
                    }
                    else -> {
                        precipValue = weatherData!!.precipitation.qpfSnowIn
                        precipUnit = context.getString(R.string.unit_in)
                    }
                }
                weatherDetailsMap.put(
                    WeatherDetailsType.POPSNOW, DetailItemViewModel(
                        WeatherDetailsType.POPSNOW,
                        String.format(
                            LocaleUtils.getLocale(),
                            "%s %s",
                            df.format(precipValue.toDouble()),
                            precipUnit
                        )
                    )
                )
            }
            if (weatherData?.precipitation?.cloudiness != null && weatherData!!.precipitation.cloudiness >= 0) {
                weatherDetailsMap.put(
                    WeatherDetailsType.POPCLOUDINESS,
                    DetailItemViewModel(
                        WeatherDetailsType.POPCLOUDINESS,
                        weatherData!!.precipitation.cloudiness.toString() + "%"
                    )
                )
            }
        }

        // Atmosphere
        if (weatherData?.atmosphere?.pressureMb != null) {
            val unit = settingsManager.getPressureUnit()
            val pressureVal: Float
            val pressureUnit: String

            when (unit) {
                Units.INHG -> {
                    pressureVal = weatherData!!.atmosphere.pressureIn
                    pressureUnit = context.getString(R.string.unit_inHg)
                }
                Units.MILLIBAR -> {
                    pressureVal = weatherData!!.atmosphere.pressureMb
                    pressureUnit = context.getString(R.string.unit_mBar)
                }
                else -> {
                    pressureVal = weatherData!!.atmosphere.pressureIn
                    pressureUnit = context.getString(R.string.unit_inHg)
                }
            }

            weatherDetailsMap[WeatherDetailsType.PRESSURE] = DetailItemViewModel(
                WeatherDetailsType.PRESSURE,
                String.format(
                    LocaleUtils.getLocale(),
                    "%s %s",
                    df.format(pressureVal.toDouble()),
                    pressureUnit
                )
            )
        }

        if (weatherData?.atmosphere?.humidity != null) {
            weatherDetailsMap.put(
                WeatherDetailsType.HUMIDITY, DetailItemViewModel(
                    WeatherDetailsType.HUMIDITY,
                    String.format(
                        LocaleUtils.getLocale(),
                        "%d%%",
                        weatherData!!.atmosphere.humidity
                    )
                )
            )
        }

        if (weatherData?.atmosphere?.dewpointF != null && !ObjectsCompat.equals(weatherData!!.atmosphere.dewpointF, weatherData!!.atmosphere.dewpointC)) {
            weatherDetailsMap.put(
                WeatherDetailsType.DEWPOINT, DetailItemViewModel(
                    WeatherDetailsType.DEWPOINT,
                    String.format(
                        LocaleUtils.getLocale(), "%d°",
                        if (isFahrenheit) {
                            Math.round(weatherData!!.atmosphere.dewpointF)
                        } else {
                            Math.round(weatherData!!.atmosphere.dewpointC)
                        }
                    )
                )
            )
        }

        if (weatherData?.atmosphere?.visibilityMi != null && weatherData!!.atmosphere.visibilityMi >= 0) {
            val unit = settingsManager.getDistanceUnit()
            val visibilityVal: Int
            val visibilityUnit: String

            when (unit) {
                Units.MILES -> {
                    visibilityVal = Math.round(weatherData!!.atmosphere.visibilityMi)
                    visibilityUnit = context.getString(R.string.unit_miles)
                }
                Units.KILOMETERS -> {
                    visibilityVal = Math.round(weatherData!!.atmosphere.visibilityKm)
                    visibilityUnit = context.getString(R.string.unit_kilometers)
                }
                else -> {
                    visibilityVal = Math.round(weatherData!!.atmosphere.visibilityMi)
                    visibilityUnit = context.getString(R.string.unit_miles)
                }
            }

            weatherDetailsMap.put(
                WeatherDetailsType.VISIBILITY, DetailItemViewModel(
                    WeatherDetailsType.VISIBILITY,
                    String.format(LocaleUtils.getLocale(), "%d %s", visibilityVal, visibilityUnit)
                )
            )
        }

        if (weatherData?.condition?.uv?.index != null) {
            if (isPhone) {
                uvIndex = UVIndexViewModel(weatherData!!.condition.uv)
            } else {
                weatherDetailsMap[WeatherDetailsType.UV] =
                    DetailItemViewModel(weatherData!!.condition.uv)
            }
        } else {
            uvIndex = null
        }

        // Additional Details
        if (weatherData?.condition?.airQuality?.index != null) {
            if (isPhone) {
                airQuality = AirQualityViewModel(weatherData!!.condition.airQuality)
            } else {
                weatherDetailsMap[WeatherDetailsType.AIRQUALITY] =
                    DetailItemViewModel(weatherData!!.condition.airQuality)
            }
        } else {
            airQuality = null
        }

        if (weatherData?.condition?.feelslikeF != null &&
            weatherData!!.condition.feelslikeF != weatherData!!.condition.feelslikeC) {
            val value =
                if (isFahrenheit) Math.round(weatherData!!.condition.feelslikeF) else Math.round(
                    weatherData!!.condition.feelslikeC
                )

            weatherDetailsMap[WeatherDetailsType.FEELSLIKE] = DetailItemViewModel(
                WeatherDetailsType.FEELSLIKE,
                String.format(LocaleUtils.getLocale(), "%d°", value)
            )
        }

        // Wind
        if (weatherData?.condition?.windMph != null &&
            weatherData!!.condition.windMph != weatherData!!.condition.windKph) {
            val unit = settingsManager.getSpeedUnit()
            val speedVal: Int
            val speedUnit: String

            when (unit) {
                Units.MILES_PER_HOUR -> {
                    speedVal = Math.round(weatherData!!.condition.windMph)
                    speedUnit = context.getString(R.string.unit_mph)
                }
                Units.KILOMETERS_PER_HOUR -> {
                    speedVal = Math.round(weatherData!!.condition.windKph)
                    speedUnit = context.getString(R.string.unit_kph)
                }
                Units.METERS_PER_SECOND -> {
                    speedVal =
                        ConversionMethods.kphToMsec(weatherData!!.condition.windKph).roundToInt()
                    speedUnit = context.getString(R.string.unit_msec)
                }
                else -> {
                    speedVal = Math.round(weatherData!!.condition.windMph)
                    speedUnit = context.getString(R.string.unit_mph)
                }
            }

            if (weatherData!!.condition.windDegrees != null) {
                weatherDetailsMap[WeatherDetailsType.WINDSPEED] = DetailItemViewModel(
                    WeatherDetailsType.WINDSPEED,
                    String.format(
                        LocaleUtils.getLocale(),
                        "%d %s, %s",
                        speedVal,
                        speedUnit,
                        getWindDirection(weatherData!!.condition.windDegrees.toFloat())
                    ),
                    weatherData!!.condition.windDegrees + 180
                )
            } else {
                weatherDetailsMap[WeatherDetailsType.WINDSPEED] = DetailItemViewModel(
                    WeatherDetailsType.WINDSPEED,
                    String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit), 180
                )
            }
        }

        if (weatherData?.condition?.windGustMph != null && weatherData!!.condition.windGustKph != null &&
            weatherData!!.condition.windGustMph != weatherData!!.condition.windGustKph) {
            val unit = settingsManager.getSpeedUnit()
            val speedVal: Int
            val speedUnit: String

            when (unit) {
                Units.MILES_PER_HOUR -> {
                    speedVal = Math.round(weatherData!!.condition.windGustMph)
                    speedUnit = context.getString(R.string.unit_mph)
                }
                Units.KILOMETERS_PER_HOUR -> {
                    speedVal = Math.round(weatherData!!.condition.windGustKph)
                    speedUnit = context.getString(R.string.unit_kph)
                }
                Units.METERS_PER_SECOND -> {
                    speedVal =
                        Math.round(ConversionMethods.kphToMsec(weatherData!!.condition.windGustKph))
                    speedUnit = context.getString(R.string.unit_msec)
                }
                else -> {
                    speedVal = Math.round(weatherData!!.condition.windGustMph)
                    speedUnit = context.getString(R.string.unit_mph)
                }
            }

            weatherDetailsMap[WeatherDetailsType.WINDGUST] = DetailItemViewModel(
                WeatherDetailsType.WINDGUST,
                String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit)
            )
        }

        if (weatherData!!.condition.beaufort != null) {
            if (isPhone) {
                beaufort = BeaufortViewModel(weatherData!!.condition.beaufort)
            } else {
                weatherDetailsMap.put(
                    WeatherDetailsType.BEAUFORT,
                    DetailItemViewModel(weatherData!!.condition.beaufort.scale)
                )
            }
        } else {
            beaufort = null
        }

        if (weatherData!!.condition?.pollen != null) {
            val pollenVM = PollenViewModel(weatherData!!.condition.pollen)
            if (isPhone) {
                pollen = pollenVM
            } else {
                weatherDetailsMap[WeatherDetailsType.TREEPOLLEN] = DetailItemViewModel(
                    WeatherDetailsType.TREEPOLLEN,
                    pollenVM.treePollenDesc.toString(),
                    0
                )
                weatherDetailsMap[WeatherDetailsType.GRASSPOLLEN] = DetailItemViewModel(
                    WeatherDetailsType.GRASSPOLLEN,
                    pollenVM.grassPollenDesc.toString(),
                    0
                )
                weatherDetailsMap[WeatherDetailsType.RAGWEEDPOLLEN] = DetailItemViewModel(
                    WeatherDetailsType.RAGWEEDPOLLEN,
                    pollenVM.ragweedPollenDesc.toString(),
                    0
                )
            }
        } else {
            pollen = null
        }

        // Astronomy
        if (weatherData?.astronomy != null) {
            sunPhase = SunPhaseViewModel(weatherData!!.astronomy, weatherData!!.location.tzOffset)

            weatherDetailsMap[WeatherDetailsType.SUNRISE] =
                DetailItemViewModel(WeatherDetailsType.SUNRISE, sunPhase!!.sunrise)
            weatherDetailsMap[WeatherDetailsType.SUNSET] =
                DetailItemViewModel(WeatherDetailsType.SUNSET, sunPhase!!.sunset)

            moonPhase = MoonPhaseViewModel(weatherData!!.astronomy)

            if (weatherData?.astronomy?.moonrise != null && weatherData?.astronomy?.moonset != null) {
                if (DateFormat.is24HourFormat(context)) {
                    if (weatherData!!.astronomy.moonrise.isAfter(DateTimeUtils.LOCALDATETIME_MIN)) {
                        weatherDetailsMap[WeatherDetailsType.MOONRISE] = DetailItemViewModel(
                            WeatherDetailsType.MOONRISE,
                            weatherData!!.astronomy.moonrise.format(
                                DateTimeUtils.ofPatternForUserLocale(
                                    DateTimeConstants.CLOCK_FORMAT_24HR
                                )
                            )
                        )
                    }
                    if (weatherData!!.astronomy.moonset.isAfter(DateTimeUtils.LOCALDATETIME_MIN)) {
                        weatherDetailsMap[WeatherDetailsType.MOONSET] = DetailItemViewModel(
                            WeatherDetailsType.MOONSET,
                            weatherData!!.astronomy.moonset.format(
                                DateTimeUtils.ofPatternForUserLocale(
                                    DateTimeConstants.CLOCK_FORMAT_24HR
                                )
                            )
                        )
                    }
                } else {
                    if (weatherData!!.astronomy.moonrise.isAfter(DateTimeUtils.LOCALDATETIME_MIN)) {
                        weatherDetailsMap[WeatherDetailsType.MOONRISE] = DetailItemViewModel(
                            WeatherDetailsType.MOONRISE,
                            weatherData!!.astronomy.moonrise.format(
                                DateTimeUtils.ofPatternForUserLocale(
                                    DateTimeConstants.CLOCK_FORMAT_12HR_AMPM
                                )
                            )
                        )
                    }
                    if (weatherData!!.astronomy.moonset.isAfter(DateTimeUtils.LOCALDATETIME_MIN)) {
                        weatherDetailsMap[WeatherDetailsType.MOONSET] = DetailItemViewModel(
                            WeatherDetailsType.MOONSET,
                            weatherData!!.astronomy.moonset.format(
                                DateTimeUtils.ofPatternForUserLocale(
                                    DateTimeConstants.CLOCK_FORMAT_12HR_AMPM
                                )
                            )
                        )
                    }
                }
            }

            if (weatherData?.astronomy?.moonPhase != null) {
                if (!isPhone) {
                    weatherDetailsMap[WeatherDetailsType.MOONPHASE] =
                        DetailItemViewModel(weatherData!!.astronomy.moonPhase.phase)
                }
            }
        } else {
            sunPhase = null
            moonPhase = null
        }

        val entry = WeatherAPI.APIs.find { wapi -> weatherSource == wapi.value }
        weatherCredit = String.format(
            "%s %s",
            context.getString(R.string.credit_prefix),
            entry?.toString() ?: WeatherIcons.EM_DASH
        )
    }

    val isValid: Boolean
        get() = weatherData?.isValid == true
}

fun Weather.toUiModel(): WeatherUiModel {
    return WeatherUiModel(this)
}