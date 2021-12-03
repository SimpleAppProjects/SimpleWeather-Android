package com.thewizrd.shared_resources.weatherdata.tomorrow

import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.ZonedDateTime

fun createWeatherAlerts(alertRoot: AlertsRootobject?): Collection<WeatherAlert>? {
    var weatherAlerts: Collection<WeatherAlert>? = null

    if (!alertRoot?.data?.events.isNullOrEmpty()) {
        weatherAlerts = HashSet(alertRoot!!.data.events.size)

        for (event in alertRoot.data.events) {
            // Skip "Active Fire" alerts (not enough info)
            if (event.eventValues.origin != "VIIRS") {
                weatherAlerts.add(createWeatherAlert(event))
            }
        }
    }

    return weatherAlerts
}

fun createWeatherAlert(event: EventsItem): WeatherAlert {
    return WeatherAlert().apply {
        date = ZonedDateTime.parse(event.startTime)
        expiresDate = ZonedDateTime.parse(event.endTime)

        type = when (event.insight) {
            "fires" -> WeatherAlertType.FIRE
            "wind" -> WeatherAlertType.HIGHWIND
            "winter" -> WeatherAlertType.WINTERWEATHER
            "thunderstorms" -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            "floods" -> WeatherAlertType.FLOODWARNING
            "tropical" -> WeatherAlertType.HURRICANEWINDWARNING
            "fog" -> WeatherAlertType.DENSEFOG
            "tornado" -> WeatherAlertType.TORNADOWARNING
            else -> WeatherAlertType.SPECIALWEATHERALERT
        }

        if (type == WeatherAlertType.SPECIALWEATHERALERT && !event.eventValues.title.isNullOrBlank()) {
            if (event.eventValues.title.contains("Heat")) {
                type = WeatherAlertType.HEAT
            } else if (event.eventValues.title.contains("Cold") ||
                event.eventValues.title.contains("Freeze") ||
                event.eventValues.title.contains("Frost")
            ) {
                type = WeatherAlertType.WINTERWEATHER
            } else if (event.eventValues.title.contains("Smoke")) {
                type = WeatherAlertType.DENSESMOKE
            } else if (event.eventValues.title.contains("Dust")) {
                type = WeatherAlertType.DUSTADVISORY
            } else if (event.eventValues.title.contains("Small Craft")) {
                type = WeatherAlertType.SMALLCRAFT
            } else if (event.eventValues.title.contains("Gale")) {
                type = WeatherAlertType.GALEWARNING
            } else if (event.eventValues.title.contains("Storm")) {
                type = WeatherAlertType.STORMWARNING
            } else if (event.eventValues.title.contains("Tsunami")) {
                type = WeatherAlertType.TSUNAMIWARNING
            }
        }

        severity = when (event.severity) {
            "minor" -> WeatherAlertSeverity.MINOR
            "moderate" -> WeatherAlertSeverity.MODERATE
            "severe" -> WeatherAlertSeverity.SEVERE
            "extreme" -> WeatherAlertSeverity.EXTREME
            else -> WeatherAlertSeverity.UNKNOWN
        }

        title = event.eventValues.title

        val messageStr = StringBuilder()

        if (!event.eventValues.headline.isNullOrBlank()) {
            messageStr.appendLine(event.eventValues.headline)
        }

        if (!event.eventValues.description.isNullOrBlank()) {
            if (messageStr.isNotEmpty()) {
                messageStr.appendLine()
            }
            messageStr.appendLine(event.eventValues.description)
        }

        val instruction = event.eventValues.response?.firstOrNull()?.instruction
        if (!instruction.isNullOrBlank()) {
            if (messageStr.isNotEmpty()) {
                messageStr.appendLine()
            }
            messageStr.appendLine(instruction)
        }

        message = messageStr.toString()

        attribution = event.eventValues.origin ?: "tomorrow.io"
    }
}
