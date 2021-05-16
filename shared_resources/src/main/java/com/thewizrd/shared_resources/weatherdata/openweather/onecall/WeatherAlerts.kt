package com.thewizrd.shared_resources.weatherdata.openweather.onecall

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun createWeatherAlerts(alerts: List<AlertsItem>?): Collection<WeatherAlert>? {
    if (alerts.isNullOrEmpty()) return null

    val weatherAlerts = ArrayList<WeatherAlert>(alerts.size)
    for (alert in alerts) {
        weatherAlerts.add(createWeatherAlert(alert))
    }

    return weatherAlerts
}

@SuppressLint("VisibleForTests")
fun createWeatherAlert(alert: AlertsItem): WeatherAlert {
    return WeatherAlert().apply {
        // OWM does not define alert type as it can come from multiple alert data sources
        // so just define as normal alert
        type = WeatherAlertType.SPECIALWEATHERALERT
        severity = WeatherAlertSeverity.MODERATE

        title = alert.event
        message = alert.description

        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(alert.start), ZoneOffset.UTC)
        expiresDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(alert.end), ZoneOffset.UTC)

        attribution = alert.senderName
    }
}