package com.thewizrd.shared_resources.weatherdata.nws.alerts

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.ZonedDateTime
import java.util.*

fun createWeatherAlerts(root: AlertRootobject): Collection<WeatherAlert>? {
    val alerts = ArrayList<WeatherAlert>(root.graph.size)

    for (result in root.graph) {
        alerts.add(createWeatherAlert(result))
    }

    return alerts
}

@SuppressLint("VisibleForTests")
fun createWeatherAlert(alert: GraphItem): WeatherAlert {
    return WeatherAlert().apply {
        // Alert Type
        type = when (alert.event) {
            "Hurricane Local Statement" -> WeatherAlertType.HURRICANELOCALSTATEMENT

            "Hurricane Force Wind Watch",
            "Hurricane Watch",
            "Hurricane Force Wind Warning",
            "Hurricane Warning" -> {
                WeatherAlertType.HURRICANEWINDWARNING
            }

            "Tornado Warning" -> WeatherAlertType.TORNADOWARNING
            "Tornado Watch" -> WeatherAlertType.TORNADOWATCH
            "Severe Thunderstorm Warning" -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
            "Severe Thunderstorm Watch" -> WeatherAlertType.SEVERETHUNDERSTORMWATCH
            "Excessive Heat Warning",
            "Excessive Heat Watch",

            "Heat Advisory" -> {
                WeatherAlertType.HEAT
            }

            "Dense Fog Advisory" -> WeatherAlertType.DENSEFOG
            "Dense Smoke Advisory" -> WeatherAlertType.DENSESMOKE

            "Extreme Fire Danger",
            "Fire Warning",
            "Fire Weather Watch" -> {
                WeatherAlertType.FIRE
            }

            "Volcano Warning" -> WeatherAlertType.VOLCANO

            "Extreme Wind Warning",
            "High Wind Warning",
            "High Wind Watch",
            "Lake Wind Advisory",
            "Wind Advisory" -> {
                WeatherAlertType.HIGHWIND
            }

            "Lake Effect Snow Advisory",
            "Lake Effect Snow Warning",
            "Lake Effect Snow Watch",
            "Snow Squall Warning",
            "Ice Storm Warning",
            "Winter Storm Warning",
            "Winter Storm Watch",
            "Winter Weather Advisory" -> {
                WeatherAlertType.WINTERWEATHER
            }

            "Earthquake Warning" -> WeatherAlertType.EARTHQUAKEWARNING
            "Gale Warning", "Gale Watch" -> WeatherAlertType.GALEWARNING

            else -> {
                if (alert.event.contains("Flood Warning"))
                    WeatherAlertType.FLOODWARNING
                else if (alert.event.contains("Flood"))
                    WeatherAlertType.FLOODWATCH
                else if (alert.event.contains("Snow") || alert.event.contains("Blizzard") ||
                        alert.event.contains("Winter") || alert.event.contains("Ice") ||
                        alert.event.contains("Avalanche") || alert.event.contains("Cold") ||
                        alert.event.contains("Freez") || alert.event.contains("Frost") ||
                        alert.event.contains("Chill")) {
                    WeatherAlertType.WINTERWEATHER
                } else if (alert.event.contains("Dust"))
                    WeatherAlertType.DUSTADVISORY
                else if (alert.event.contains("Small Craft"))
                    WeatherAlertType.SMALLCRAFT
                else if (alert.event.contains("Storm"))
                    WeatherAlertType.STORMWARNING
                else if (alert.event.contains("Tsunami"))
                    WeatherAlertType.TSUNAMIWARNING
                else
                    WeatherAlertType.SPECIALWEATHERALERT
            }
        }

        severity = when (alert.severity) {
            "Minor" -> WeatherAlertSeverity.MINOR
            "Moderate" -> WeatherAlertSeverity.MODERATE
            "Severe" -> WeatherAlertSeverity.SEVERE
            "Extreme" -> WeatherAlertSeverity.EXTREME
            "Unknown" -> WeatherAlertSeverity.UNKNOWN
            else -> WeatherAlertSeverity.UNKNOWN
        }

        title = alert.event
        message = String.format("%s\n%s", alert.description, alert.instruction)

        date = ZonedDateTime.parse(alert.sent)
        expiresDate = ZonedDateTime.parse(alert.expires)

        attribution = "U.S. National Weather Service"
    }
}