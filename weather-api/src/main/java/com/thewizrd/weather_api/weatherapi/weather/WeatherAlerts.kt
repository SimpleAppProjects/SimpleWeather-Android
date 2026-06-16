package com.thewizrd.weather_api.weatherapi.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun createWeatherAlerts(alerts: Alerts?): Collection<WeatherAlert>? {
    if (alerts?.alert.isNullOrEmpty()) return null

    val weatherAlerts = ArrayList<WeatherAlert>(alerts.alert!!.size)

    for (alert in alerts.alert!!) {
        weatherAlerts.add(createWeatherAlert(alert))
    }

    return weatherAlerts
}

@SuppressLint("VisibleForTests")
fun createWeatherAlert(alert: AlertItem): WeatherAlert {
    return WeatherAlert().apply {
        type = when {
            alert.event?.contains("Hurricane") == true -> WeatherAlertType.HURRICANEWINDWARNING
            alert.event?.contains("Tornado") == true -> WeatherAlertType.TORNADOWARNING
            alert.event?.contains("Thunderstorm") == true -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            alert.event?.contains("Flood") == true -> WeatherAlertType.FLOODWARNING
            alert.event?.contains("Wind") == true -> WeatherAlertType.HIGHWIND
            alert.event?.contains("Fog") == true -> WeatherAlertType.DENSEFOG
            alert.event?.contains("Volcano") == true -> WeatherAlertType.VOLCANO
            alert.event?.contains("Earthquake") == true -> WeatherAlertType.EARTHQUAKEWARNING
            alert.event?.contains("Storm") == true -> WeatherAlertType.STORMWARNING
            alert.event?.contains("Tsunami") == true -> WeatherAlertType.TSUNAMIWARNING
            else -> WeatherAlertType.SPECIALWEATHERALERT
        }

        severity = when (alert.severity) {
            "Moderate" -> WeatherAlertSeverity.MODERATE
            "Severe" -> WeatherAlertSeverity.SEVERE
            "Extreme" -> WeatherAlertSeverity.EXTREME
            else -> WeatherAlertSeverity.MINOR
        }

        title = alert.event ?: alert.category
        message = StringBuilder()
                .appendLine(alert.headline)
                .appendLine()
                .appendLine(alert.desc)
                .appendLine()
                .appendLine(alert.instruction)
                .toString()

        attribution = alert.note ?: "WeatherAPI.com"

        date =
            alert.effective?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_ZONED_DATE_TIME) }
                ?: ZonedDateTime.now()
        expiresDate =
            alert.expires?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_ZONED_DATE_TIME) }
                ?: date.plusDays(1)
    }
}