package com.thewizrd.weather_api.weatherkit

import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.ZonedDateTime

fun createWeatherAlerts(alerts: WeatherAlertCollection?): Collection<WeatherAlert>? {
    if (alerts?.alerts.isNullOrEmpty()) return null

    val weatherAlerts = ArrayList<WeatherAlert>(alerts!!.alerts.size)

    for (alert in alerts.alerts) {
        weatherAlerts.add(createWeatherAlert(alert))
    }

    return weatherAlerts
}

fun createWeatherAlert(alert: WeatherAlertSummary): WeatherAlert {
    return WeatherAlert().apply {
        title = alert.description
        message = alert.detailsUrl
        attribution = alert.source
        date = ZonedDateTime.parse(alert.effectiveTime)
        expiresDate = ZonedDateTime.parse(alert.expireTime)
        severity = when (alert.severity) {
            Severity.EXTREME -> WeatherAlertSeverity.EXTREME
            Severity.SEVERE -> WeatherAlertSeverity.SEVERE
            Severity.MODERATE -> WeatherAlertSeverity.MODERATE
            Severity.MINOR -> WeatherAlertSeverity.MINOR
            else -> WeatherAlertSeverity.UNKNOWN
        }
        type = when {
            alert.description.contains("Hurricane") -> WeatherAlertType.HURRICANEWINDWARNING
            alert.description.contains("Tornado") -> WeatherAlertType.TORNADOWARNING
            alert.description.contains("Thunderstorm") -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            alert.description.contains("Flood") -> WeatherAlertType.FLOODWARNING
            alert.description.contains("Wind") -> WeatherAlertType.HIGHWIND
            alert.description.contains("Fog") -> WeatherAlertType.DENSEFOG
            alert.description.contains("Volcano") -> WeatherAlertType.VOLCANO
            alert.description.contains("Earthquake") -> WeatherAlertType.EARTHQUAKEWARNING
            alert.description.contains("Storm") -> WeatherAlertType.STORMWARNING
            alert.description.contains("Tsunami") -> WeatherAlertType.TSUNAMIWARNING
            else -> WeatherAlertType.SPECIALWEATHERALERT
        }
    }
}