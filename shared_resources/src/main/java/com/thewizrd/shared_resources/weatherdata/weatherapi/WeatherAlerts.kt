package com.thewizrd.shared_resources.weatherdata.weatherapi

import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun createWeatherAlerts(alerts: Alerts?): Collection<WeatherAlert>? {
    if (alerts?.alert.isNullOrEmpty()) return null

    val weatherAlerts = ArrayList<WeatherAlert>(alerts!!.alert!!.size)

    for (alert in alerts.alert!!) {
        weatherAlerts.add(createWeatherAlert(alert))
    }

    return weatherAlerts
}

fun createWeatherAlert(alert: AlertItem): WeatherAlert {
    return WeatherAlert().apply {
        type = when {
            alert.event!!.contains("Hurricane") -> WeatherAlertType.HURRICANEWINDWARNING
            alert.event!!.contains("Tornado") -> WeatherAlertType.TORNADOWARNING
            alert.event!!.contains("Thunderstorm") -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            alert.event!!.contains("Flood") -> WeatherAlertType.FLOODWARNING
            alert.event!!.contains("Wind") -> WeatherAlertType.HIGHWIND
            alert.event!!.contains("Fog") -> WeatherAlertType.DENSEFOG
            alert.event!!.contains("Volcano") -> WeatherAlertType.VOLCANO
            alert.event!!.contains("Earthquake") -> WeatherAlertType.EARTHQUAKEWARNING
            alert.event!!.contains("Storm") -> WeatherAlertType.STORMWARNING
            alert.event!!.contains("Tsunami") -> WeatherAlertType.TSUNAMIWARNING
            else -> WeatherAlertType.SPECIALWEATHERALERT
        }

        severity = when (alert.severity) {
            "Moderate" -> WeatherAlertSeverity.MODERATE
            "Severe" -> WeatherAlertSeverity.SEVERE
            "Extreme" -> WeatherAlertSeverity.EXTREME
            else -> WeatherAlertSeverity.MINOR
        }

        title = alert.event
        message = StringBuilder()
                .appendLine(alert.headline)
                .appendLine()
                .appendLine(alert.desc)
                .appendLine()
                .appendLine(alert.instruction)
                .toString()

        attribution = alert.note

        date = ZonedDateTime.parse(alert.effective,
                DateTimeFormatter.ISO_ZONED_DATE_TIME)
        expiresDate = ZonedDateTime.parse(alert.expires,
                DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
}