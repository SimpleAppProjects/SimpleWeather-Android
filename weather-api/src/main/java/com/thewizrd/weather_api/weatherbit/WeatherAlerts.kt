package com.thewizrd.weather_api.weatherbit

import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun createWeatherAlerts(alerts: List<AlertsItem?>?, tzLong: String): Collection<WeatherAlert>? {
    if (alerts.isNullOrEmpty()) return null

    val weatherAlerts = LinkedHashSet<WeatherAlert>(alerts.size)
    val zoneId = ZoneIdCompat.of(tzLong)

    for (alert in alerts) {
        weatherAlerts.add(createWeatherAlert(alert!!).apply {
            date = date.withZoneSameInstant(zoneId)
            expiresDate = expiresDate.withZoneSameInstant(zoneId)
        })
    }

    return weatherAlerts
}

fun createWeatherAlert(alert: AlertsItem): WeatherAlert {
    return WeatherAlert().apply {
        type = when {
            alert.title!!.contains("Hurricane") -> WeatherAlertType.HURRICANEWINDWARNING
            alert.title!!.contains("Tornado") -> WeatherAlertType.TORNADOWARNING
            alert.title!!.contains("Thunderstorm") -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            alert.title!!.contains("Flood") -> WeatherAlertType.FLOODWARNING
            alert.title!!.contains("Wind") -> WeatherAlertType.HIGHWIND
            alert.title!!.contains("Fog") -> WeatherAlertType.DENSEFOG
            alert.title!!.contains("Volcano") -> WeatherAlertType.VOLCANO
            alert.title!!.contains("Earthquake") -> WeatherAlertType.EARTHQUAKEWARNING
            alert.title!!.contains("Storm") -> WeatherAlertType.STORMWARNING
            alert.title!!.contains("Tsunami") -> WeatherAlertType.TSUNAMIWARNING
            else -> WeatherAlertType.SPECIALWEATHERALERT
        }

        severity = when (alert.severity) {
            "Advisory" -> WeatherAlertSeverity.MINOR
            "Watch" -> WeatherAlertSeverity.MODERATE
            "Warning" -> WeatherAlertSeverity.SEVERE
            else -> WeatherAlertSeverity.MINOR
        }

        title = alert.title
        message = StringBuilder()
            .appendLine(alert.title)
            .appendLine()
            .appendLine(alert.description)
            .toString()

        date = LocalDateTime.parse(
            alert.effectiveUtc,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        ).atZone(ZoneOffset.UTC)
        expiresDate = LocalDateTime.parse(
            alert.expiresUtc,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        ).atZone(ZoneOffset.UTC)
    }
}