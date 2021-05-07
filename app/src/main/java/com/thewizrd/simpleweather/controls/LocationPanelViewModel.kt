package com.thewizrd.simpleweather.controls

import androidx.annotation.DrawableRes
import androidx.core.util.ObjectsCompat
import com.thewizrd.shared_resources.controls.ImageDataViewModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getImageData
import com.thewizrd.shared_resources.weatherdata.LocationType
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationPanelViewModel {
    private val settingsManager = App.instance.settingsManager

    private var weather: Weather? = null
    private var unitCode: String? = null
    private var localeCode: String? = null

    var locationName: String? = null
        private set
    var currTemp: String? = null
        private set
    var currWeather: String? = null
        private set
    var weatherIcon: String? = null
        private set
    var hiTemp: String? = null
        private set
    var loTemp: String? = null
        private set
    var isShowHiLo = false
        private set
    var pop: String? = null
        private set

    @DrawableRes
    var popIcon = 0
        private set
    var windDir = 0
        private set
    var windSpeed: String? = null
        private set
    var imageData: ImageDataViewModel? = null
        private set
    var locationData: LocationData? = null
    val locationType = LocationType.SEARCH.value
        get() = locationData?.locationType?.value ?: field
    var weatherSource: String? = null
        private set

    var isEditMode = false
    var isChecked = false

    fun setWeather(weather: Weather?) {
        if (weather != null && weather.isValid) {
            if (!ObjectsCompat.equals(this.weather, weather)) {
                this.weather = weather

                imageData = null

                locationName = weather.location.name

                if (weather.precipitation != null) {
                    if (weather.precipitation.pop != null) {
                        pop = weather.precipitation.pop.toString() + "%"
                        popIcon = R.drawable.wi_umbrella
                    } else if (weather.precipitation.cloudiness != null) {
                        pop = weather.precipitation.cloudiness.toString() + "%"
                        popIcon = R.drawable.wi_cloudy
                    }
                } else {
                    pop = null
                }

                weatherIcon = weather.condition.icon
                weatherSource = weather.source

                if (locationData == null) {
                    locationData = LocationData(weather)
                }

                // Refresh locale/unit dependent values
                refreshView()
            } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) || !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode())) {
                refreshView()
            }
        }
    }

    private fun refreshView() {
        val provider = WeatherManager.getProvider(weather!!.source)
        val context = App.instance.appContext

        val isFahrenheit = Units.FAHRENHEIT == settingsManager.getTemperatureUnit()
        unitCode = settingsManager.getUnitString()
        localeCode = LocaleUtils.getLocaleCode()

        currTemp = if (weather?.condition?.tempF != null && !ObjectsCompat.equals(weather?.condition?.tempF, weather?.condition?.tempC)) {
            val temp = if (isFahrenheit) Math.round(weather!!.condition.tempF) else Math.round(weather!!.condition.tempC)
            val unitTemp = if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS

            String.format(LocaleUtils.getLocale(), "%d°%s", temp, unitTemp)
        } else {
            WeatherIcons.PLACEHOLDER
        }

        currWeather = if (provider.supportsWeatherLocale()) weather!!.condition.weather else provider.getWeatherCondition(weather!!.condition.icon)

        hiTemp = if (weather?.condition?.highF != null && !ObjectsCompat.equals(weather!!.condition.highF, weather!!.condition.highC)) {
            val temp = if (isFahrenheit) Math.round(weather!!.condition.highF) else Math.round(weather!!.condition.highC)
            String.format(LocaleUtils.getLocale(), "%d°", temp)
        } else {
            WeatherIcons.PLACEHOLDER
        }

        loTemp = if (weather?.condition?.lowF != null && !ObjectsCompat.equals(weather!!.condition.lowF, weather!!.condition.lowC)) {
            val temp = if (isFahrenheit) Math.round(weather!!.condition.lowF) else Math.round(weather!!.condition.lowC)
            String.format(LocaleUtils.getLocale(), "%d°", temp)
        } else {
            WeatherIcons.PLACEHOLDER
        }

        isShowHiLo = !ObjectsCompat.equals(hiTemp, loTemp)

        if ((weather?.condition?.windMph ?: -1f) >= 0 && (weather?.condition?.windDegrees
                        ?: -1) >= 0) {
            val unit = settingsManager.getSpeedUnit()
            val speedVal: Int
            val speedUnit: String

            when (unit) {
                Units.MILES_PER_HOUR -> {
                    speedVal = Math.round(weather!!.condition.windMph)
                    speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_mph)
                }
                Units.KILOMETERS_PER_HOUR -> {
                    speedVal = Math.round(weather!!.condition.windKph)
                    speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_kph)
                }
                Units.METERS_PER_SECOND -> {
                    speedVal = Math.round(ConversionMethods.kphToMsec(weather!!.condition.windKph))
                    speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_msec)
                }
                else -> {
                    speedVal = Math.round(weather!!.condition.windMph)
                    speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_mph)
                }
            }

            windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit)
            windDir = weather!!.condition.windDegrees + 180
        } else {
            windSpeed = null
            windDir = 0
        }
    }

    suspend fun updateBackground() = withContext(Dispatchers.IO) {
        if (imageData == null) {
            imageData = weather?.getImageData()
        }
    }
}