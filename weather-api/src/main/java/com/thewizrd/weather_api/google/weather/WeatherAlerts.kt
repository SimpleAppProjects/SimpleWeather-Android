package com.thewizrd.weather_api.google.weather

import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.ZonedDateTime

fun createWeatherAlerts(response: AlertsResponse?): Collection<WeatherAlert>? {
    return response?.weatherAlerts?.map {
        createWeatherAlert(it)
    }
}

fun createWeatherAlert(alert: PublicAlerts): WeatherAlert {
    return WeatherAlert().apply {
        title = alert.alertTitle?.text
        message = alert.description
        attribution = alert.dataSource?.name
        date = ZonedDateTime.parse(alert.startTime)
        expiresDate = ZonedDateTime.parse(alert.expirationTime)
        severity = when (alert.severity) {
            "EXTREME" -> WeatherAlertSeverity.EXTREME
            "SEVERE" -> WeatherAlertSeverity.SEVERE
            "MODERATE" -> WeatherAlertSeverity.MODERATE
            "MINOR" -> WeatherAlertSeverity.MINOR
            else -> WeatherAlertSeverity.UNKNOWN
        }
        type = when (alert.eventType) {
            "ACID_RAIN", "AVALANCHE", "DROUGHT",
            "HUMIDITY", "LANDSLIDE", "MONSOON", "OUTFLOW", "RADIATION", "RAIN_EVENT" -> WeatherAlertType.SPECIALWEATHERALERT

            "BLIZZARD", "BLOWING_SNOW", "COLD", "FREEZING", "FREEZING_AIR_TEMPERATURE",
            "FREEZING_DRIZZLE", "FREEZING_RAIN_EVENT", "FROST", "GLAZE", "ICE_STORM",
            "LAKE_EFFECT_SNOW", "SNOWSQUALL", "SNOW_EVENT", "WIND_CHILL", "WINTER_STORM" -> WeatherAlertType.WINTERWEATHER

            "BUSHFIRE", "FIRE", "FIRE_WEATHER", "INDUSTRIAL_FIRE", "WILDFIRE" -> WeatherAlertType.FIRE
            "COASTAL_FLOOD", "COASTAL_HAZARD", "FLASH_FLOOD", "FLOOD", "MUDDY_FLOOD", "RIVER_FLOODING" -> WeatherAlertType.FLOODWARNING
            "DUST_STORM" -> WeatherAlertType.DUSTADVISORY
            "AFTERSHOCK", "EARTHQUAKE" -> WeatherAlertType.EARTHQUAKEWARNING
            "FOG" -> WeatherAlertType.DENSEFOG
            "GALE" -> WeatherAlertType.GALEWARNING
            "HEAT" -> WeatherAlertType.HEAT
            "HURRICANE", "CYCLONE", "EXTRATROPICAL_CYCLONE", "TROPICAL_CYCLONE",
            "TROPICAL_CYCLONE_WARNINGS_AND_WATCHES", "TROPICAL_DISTURBANCE", "TROPICAL_STORM", "TYPHOON" -> WeatherAlertType.HURRICANEWINDWARNING

            "SEVERE_THUNDERSTORM_WARNING", "THUNDER", "THUNDERSTORM", "HAIL" -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            "STORM", "STORM_SURGE" -> WeatherAlertType.STORMWARNING
            "TORNADO", "TORNADO_WARNING" -> WeatherAlertType.TORNADOWARNING
            "TSUNAMI" -> WeatherAlertType.TSUNAMIWARNING
            "VOLCANIC_ASH", "VOLCANIC_ERUPTION" -> WeatherAlertType.VOLCANO
            "WIND" -> WeatherAlertType.HIGHWIND
            "HAZARDOUS_SEAS", "WIND_WAVE" -> WeatherAlertType.SMALLCRAFT
            else -> WeatherAlertType.SPECIALWEATHERALERT
        }
    }
}