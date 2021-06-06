package com.thewizrd.shared_resources.weatherdata.meteofrance

import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

fun createWeatherAlerts(alertRoot: AlertsResponse?): Collection<WeatherAlert>? {
    var weatherAlerts: Collection<WeatherAlert>? = null

    if (alertRoot?.colorMax != null && alertRoot.colorMax!! > 1 && alertRoot.phenomenonsItems != null) {
        weatherAlerts = HashSet(alertRoot.phenomenonsItems!!.size)

        for (phenom in alertRoot.phenomenonsItems!!) {
            // phenomenon_max_color_id == 1 -> OK / No Warning or Watches
            if (phenom!!.phenomenonMaxColorId != null && phenom.phenomenonMaxColorId!! > 1) {
                weatherAlerts.add(createWeatherAlert(phenom, alertRoot))
            }
        }
    }

    return weatherAlerts
}

fun createWeatherAlert(phenom: PhenomenonsItemsItem, alertRoot: AlertsResponse): WeatherAlert {
    return WeatherAlert().apply {
        val title_en: String
        val title_fr: String
        val message_en: String
        val message_fr: String

        /*
         * phenomenon_max_color_id (Severity)
         */
        when (phenom.phenomenonMaxColorId) {
            /* 1 - Green */
            1 -> {
                severity = WeatherAlertSeverity.MINOR
                message_fr = "Pas de vigilance particulière"
                message_en = "No particular awareness of the weather is required."
            }
            /* 2 - Yellow */
            2 -> {
                severity = WeatherAlertSeverity.MODERATE
                message_fr = "Soyez attentif"
                message_en = "The weather is potentially dangerous."
            }
            /* 3 - Orange */
            3 -> {
                severity = WeatherAlertSeverity.SEVERE
                message_fr = "Soyez très vigilant"
                message_en = "The weather is dangerous."
            }
            /* 4 - Red */
            4 -> {
                severity = WeatherAlertSeverity.EXTREME
                message_fr = "Une vigilance Absolue s'impose"
                message_en = "The weather is very dangerous."
            }
            else -> {
                severity = WeatherAlertSeverity.MINOR
                message_fr = "Pas de vigilance particulière"
                message_en = "No particular awareness of the weather is required."
            }
        }

        /*
         * phenomenon_id (Alert Type)
         */
        when (phenom.phenomenonId) {
            /* 1 - Wind */
            1 -> {
                type = WeatherAlertType.HIGHWIND
                title_fr = "Vent violent"
                title_en = "High Winds"
            }
            /* 2 - Rain/Flood */
            2 -> {
                type = WeatherAlertType.FLOODWATCH
                title_fr = "Pluie-inondation"
                title_en = "Rain/Flood"
            }
            /* 3 - Thunderstorm */
            3 -> {
                type = WeatherAlertType.SEVERETHUNDERSTORMWARNING
                title_fr = "Orages"
                title_en = "Thunderstorms"
            }
            /* 4 - Flood */
            4 -> {
                type = WeatherAlertType.FLOODWARNING
                title_fr = "Crues"
                title_en = "Floods"
            }
            /* 5 - Snow/Ice */
            5 -> {
                type = WeatherAlertType.WINTERWEATHER
                title_fr = "Neige-verglas"
                title_en = "Snow/Ice"
            }
            /* 6 - Extreme high temp */
            6 -> {
                type = WeatherAlertType.HEAT
                title_fr = "Canicule"
                title_en = "Extreme high temperatures"
            }
            /* 7 - Extreme low temp */
            7 -> {
                type = WeatherAlertType.WINTERWEATHER
                title_fr = "Grand-froid"
                title_en = "Extreme low temperatures"
            }
            /* 8 - Avalanches */
            8 -> {
                type = WeatherAlertType.SPECIALWEATHERALERT
                title_fr = "Avalanches"
                title_en = "Avalanches"
            }
            /* 9 - Coastal event */
            9 -> {
                type = WeatherAlertType.SPECIALWEATHERALERT
                title_fr = "Vagues-submersion"
                title_en = "Coastal Event"
            }
            else -> {
                type = WeatherAlertType.SPECIALWEATHERALERT
                title_fr = "Alerte météo"
                title_en = "Weather Alert"
            }
        }

        title = title_fr

        message = "français:" +
                StringUtils.lineSeparator() +
                title_fr + " - " + message_fr +
                StringUtils.lineSeparator() +
                StringUtils.lineSeparator() +
                "english:" +
                StringUtils.lineSeparator() +
                title_en + " - " + message_en

        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(alertRoot.updateTime!!), ZoneOffset.UTC)
        expiresDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(alertRoot.endValidityTime!!), ZoneOffset.UTC)

        attribution = "Meteo France"
    }
}