package com.thewizrd.shared_resources.weatherdata.here

import androidx.annotation.IntRange
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.NumberUtils
import com.thewizrd.shared_resources.utils.StringUtils.unescapeUnicode
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

fun createWeatherAlerts(root: Rootobject, lat: Float, lon: Float): Collection<WeatherAlert>? {
    var weatherAlerts: Collection<WeatherAlert>? = null

    if (!root.alerts?.alerts.isNullOrEmpty()) {
        weatherAlerts = ArrayList(root.alerts.alerts.size)
        for (result in root.alerts.alerts) {
            weatherAlerts.add(createWeatherAlert(result))
        }
    } else if (root.nwsAlerts?.watch != null || root.nwsAlerts?.warning != null) {
        val numOfAlerts = (root.nwsAlerts?.watch?.size
                ?: 0) + (root.nwsAlerts?.warning?.size ?: 0)

        weatherAlerts = HashSet(numOfAlerts)

        if (root.nwsAlerts?.watch != null) {
            for (watchItem in root.nwsAlerts.watch) {
                // Add watch item if location is within 20km of the center of the alert zone
                if (ConversionMethods.calculateHaversine(lat.toDouble(), lon.toDouble(), watchItem.latitude, watchItem.longitude) < 20000) {
                    weatherAlerts.add(createWeatherAlert(watchItem))
                }
            }
        }
        if (root.nwsAlerts?.warning != null) {
            for (warningItem in root.nwsAlerts.warning) {
                // Add warning item if location is within 25km of the center of the alert zone
                if (ConversionMethods.calculateHaversine(lat.toDouble(), lon.toDouble(), warningItem.latitude, warningItem.longitude) < 25000) {
                    weatherAlerts.add(createWeatherAlert(warningItem))
                }
            }
        }
    }

    return weatherAlerts
}

// HERE GlobalAlerts
fun createWeatherAlert(alert: AlertsItem): WeatherAlert {
    return WeatherAlert().apply {
        // Alert Type
        when (alert.type) {
            /* 1: Strong Thunderstorms Anticipated */
            "1" -> {
                type = WeatherAlertType.SEVERETHUNDERSTORMWATCH
                severity = WeatherAlertSeverity.MODERATE
            }
            /* 2: Severe Thunderstorms Anticipated */
            "2" -> {
                type = WeatherAlertType.SEVERETHUNDERSTORMWARNING
                severity = WeatherAlertSeverity.EXTREME
            }
            /* 3: Tornadoes Possible */
            "3" -> {
                type = WeatherAlertType.TORNADOWARNING
                severity = WeatherAlertSeverity.EXTREME
            }
            /* 4: Heavy Rain Anticipated */
            "4" -> {
                type = WeatherAlertType.FLOODWATCH
                severity = WeatherAlertSeverity.SEVERE
            }
            /*
             * 5: Floods Anticipated
             * 6: Flash Floods Anticipated
             */
            "5", "6" -> {
                type = WeatherAlertType.FLOODWARNING
                severity = WeatherAlertSeverity.EXTREME
            }
            /* 7: High Winds Anticipated */
            "7" -> {
                type = WeatherAlertType.HIGHWIND
                severity = WeatherAlertSeverity.MODERATE
            }
            /*
             * 8: Heavy Snow Anticipated
             * 11: Freezing Rain Anticipated
             * 12: Ice Storm Anticipated
             */
            "8", "11", "12" -> {
                type = WeatherAlertType.WINTERWEATHER
                severity = WeatherAlertSeverity.SEVERE
            }
            /*
             * 9: Blizzard Conditions Anticipated
             * 10: Blowing Snow Anticipated
             */
            "9", "10" -> {
                type = WeatherAlertType.WINTERWEATHER
                severity = WeatherAlertSeverity.EXTREME
            }
            /*
             * 13: Snow Advisory
             * 14: Winter Weather Advisory
             * 17: Wind Chill Alert
             * 18: Frost Advisory
             * 19: Freeze Advisory
             */
            "13", "14", "17", "18", "19" -> {
                type = WeatherAlertType.WINTERWEATHER
                severity = WeatherAlertSeverity.MODERATE
            }
            /* 15: Heat Advisory */
            "15" -> {
                type = WeatherAlertType.HEAT
                severity = WeatherAlertSeverity.MODERATE
            }
            /* 16: Excessive Heat Alert */
            "16" -> {
                type = WeatherAlertType.HEAT
                severity = WeatherAlertSeverity.EXTREME
            }
            /*
             * 20: Fog Anticipated
             * 22: Smog Anticipated
             */
            "20", "22" -> {
                type = WeatherAlertType.DENSEFOG
                severity = WeatherAlertSeverity.MODERATE
            }
            /* 21: Dense Fog Anticipated */
            "21" -> {
                type = WeatherAlertType.DENSEFOG
                severity = WeatherAlertSeverity.SEVERE
            }
            /* 30: Tropical Cyclone Conditions Anticipated */
            "30" -> {
                type = WeatherAlertType.HURRICANEWINDWARNING
                severity = WeatherAlertSeverity.SEVERE
            }
            /* 31: Hurricane Conditions Anticipated */
            "31" -> {
                type = WeatherAlertType.HURRICANEWINDWARNING
                severity = WeatherAlertSeverity.EXTREME
            }
            /* 32: Small Craft Advisory Anticipated */
            "32" -> {
                type = WeatherAlertType.SMALLCRAFT
                severity = WeatherAlertSeverity.MODERATE
            }
            /* 33: Gale Warning Anticipated */
            "33" -> {
                type = WeatherAlertType.GALEWARNING
                severity = WeatherAlertSeverity.SEVERE
            }
            /* 34: High Winds Anticipated (Winds greater than 35 || 50 mph anticipated) */
            "34" -> {
                type = WeatherAlertType.HIGHWIND
                severity = WeatherAlertSeverity.SEVERE
            }
            /*
             * 23: Unknown
             * 24: Unknown
             * 25: Unknown
             * 26: Unknown
             * 27: Unknown
             * 28: Unknown
             * 29: Unknown
             * 35: Heavy Surf Advisory
             * 36: Beach Erosion Advisory
             */
            "23", "24", "25", "26", "27", "28", "29", "35", "36" -> {
                type = WeatherAlertType.SPECIALWEATHERALERT
                severity = WeatherAlertSeverity.SEVERE
            }
            else -> {
                type = WeatherAlertType.SPECIALWEATHERALERT
                severity = WeatherAlertSeverity.SEVERE
            }
        }

        // NOTE: Alert description may be encoded; unescape encoded characters
        title = alert.description.unescapeUnicode().also { message = it }

        setDateTimeFromSegment(alert.timeSegment)

        attribution = "HERE Weather"
    }
}

// HERE NWS Alerts
fun createWeatherAlert(alert: WatchItem): WeatherAlert {
    return WeatherAlert().apply {
        type = getAlertType(NumberUtils.tryParseInt(alert.type, -1), alert.description)
        severity = getAlertSeverity(alert.severity)

        title = alert.description
        message = alert.message

        date = ZonedDateTime.parse(alert.validFromTimeLocal)
        expiresDate = ZonedDateTime.parse(alert.validUntilTimeLocal)

        attribution = "U.S. National Weather Service"
    }
}

// HERE NWS Alerts
fun createWeatherAlert(alert: WarningItem): WeatherAlert {
    return WeatherAlert().apply {
        type = getAlertType(NumberUtils.tryParseInt(alert.type, -1), alert.description)
        severity = getAlertSeverity(alert.severity)

        title = alert.description
        message = alert.message

        date = ZonedDateTime.parse(alert.validFromTimeLocal)
        expiresDate = ZonedDateTime.parse(alert.validUntilTimeLocal)

        attribution = "U.S. National Weather Service"
    }
}

private fun getAlertType(@IntRange(from = 0, to = 38) type: Int, alertDescription: String): WeatherAlertType? {
    return when (type) {
        /*
         * 2: Coastal Flood Warning, Watch, or Statement
         * 5: Flash Flood Warning
         * 7: Flood Warning
         * 8: Urban and Small Stream Flood Advisory        
         */
        2, 5, 7, 8 -> WeatherAlertType.FLOODWARNING
        /*
         * 3: Flash Flood Watch
         * 4: Flash Flood Statement
         * 6: Flood Statement
         */
        3, 4, 6 -> WeatherAlertType.FLOODWATCH
        /* 9: Hurricane Local Statement */
        9 -> WeatherAlertType.HURRICANELOCALSTATEMENT
        /*
         * 15: River Ice Statement
         * 18: Snow Avalanche Bulletin
         * 37: Winter Weather Warning, Watch, or Advisory
         */
        15, 18, 37 -> WeatherAlertType.WINTERWEATHER
        /*
         * 21: Severe Local Storm Watch or Watch Cancellation
         * 23: Severe Local Storm Watch and Areal Outline
         * 26: Storm Strike Probability Bulletin from the TPC
         */
        21, 23, 26 -> WeatherAlertType.SEVERETHUNDERSTORMWATCH
        /* 24: Marine Subtropical Storm Advisory */
        24 -> WeatherAlertType.STORMWARNING
        /* 28: Severe Thunderstorm Warning */
        28 -> WeatherAlertType.SEVERETHUNDERSTORMWARNING
        /* 29: Severe Weather Statement */
        29 -> WeatherAlertType.SEVEREWEATHER
        /*
         * 30: Tropical Cyclone Advisory
         * 31: Tropical Cyclone Advisory for Marine and Aviation Interests
         * 32: Public Tropical Cyclone Advisory
         * 33: Tropical Cyclone Update
         */
        30, 31, 32, 33 -> WeatherAlertType.HURRICANEWINDWARNING
        /* 34: Tornado Warning */
        34 -> WeatherAlertType.TORNADOWARNING
        /* 35: Tsunami Watch or Warning */
        35 -> WeatherAlertType.TSUNAMIWARNING
        /* 36: Volcanic Activity Advisory */
        36 -> WeatherAlertType.VOLCANO
        /*
         * 0: Aviation Weather Warning
         * 1: Civil Emergency Message
         * 10: Lakeshore Warning or Statement
         * 11: Marine Weather Statement
         * 12: Non Precipitation Warning, Watch, or Statement
         * 13: Public Severe Weather Alert
         * 14: Red Flag Warning
         * 16: River Recreation Statement
         * 17: River Statement
         * 19: Preliminary Notice of Watch Cancellation - Aviation Message
         * 20: Special Dispersion Statement
         * 22: SPC Watch Point Information Message
         * 25: Special Marine Warning
         * 27: Special Weather Statement
         * 38: Air Stagnation Advisory
         */
        else -> {
            if (alertDescription.contains("Hurricane".lowercase(Locale.ROOT))) {
                return WeatherAlertType.HURRICANEWINDWARNING
            } else if (alertDescription.contains("Tornado".lowercase(Locale.ROOT))) {
                return WeatherAlertType.TORNADOWARNING
            } else if (alertDescription.contains("Heat".lowercase(Locale.ROOT))) {
                return WeatherAlertType.HEAT
            } else if (alertDescription.contains("Dense Fog".lowercase(Locale.ROOT))) {
                return WeatherAlertType.DENSEFOG
            } else if (alertDescription.contains("Dense Smoke".lowercase(Locale.ROOT))) {
                return WeatherAlertType.DENSESMOKE
            } else if (alertDescription.contains("Fire".lowercase(Locale.ROOT))) {
                return WeatherAlertType.FIRE
            } else if (alertDescription.contains("Wind".lowercase(Locale.ROOT))) {
                return WeatherAlertType.HIGHWIND
            } else if (alertDescription.contains("Snow".lowercase(Locale.ROOT)) || alertDescription.contains(
                    "Blizzard".lowercase(Locale.ROOT)
                ) ||
                alertDescription.contains("Winter".lowercase(Locale.ROOT)) || alertDescription.contains(
                    "Ice".lowercase(Locale.ROOT)
                ) ||
                alertDescription.contains("Ice".lowercase(Locale.ROOT)) || alertDescription.contains(
                    "Ice".lowercase(Locale.ROOT)
                ) ||
                alertDescription.contains("Avalanche".lowercase(Locale.ROOT)) || alertDescription.contains(
                    "Cold".lowercase(Locale.ROOT)
                ) ||
                alertDescription.contains("Freez".lowercase(Locale.ROOT)) || alertDescription.contains(
                    "Frost".lowercase(Locale.ROOT)
                ) ||
                alertDescription.contains("Chill".lowercase(Locale.ROOT))
            ) {
                return WeatherAlertType.WINTERWEATHER
            } else if (alertDescription.contains("Earthquake".lowercase(Locale.ROOT))) {
                return WeatherAlertType.EARTHQUAKEWARNING
            } else if (alertDescription.contains("Gale".lowercase(Locale.ROOT))) {
                return WeatherAlertType.GALEWARNING
            } else if (alertDescription.contains("Dust".lowercase(Locale.ROOT))) {
                return WeatherAlertType.DUSTADVISORY
            } else if (alertDescription.contains("Small Craft".lowercase(Locale.ROOT))) {
                return WeatherAlertType.SMALLCRAFT
            } else if (alertDescription.contains("Storm".lowercase(Locale.ROOT))) {
                return WeatherAlertType.STORMWARNING
            } else if (alertDescription.contains("Tsunami".lowercase(Locale.ROOT))) {
                return WeatherAlertType.TSUNAMIWARNING
            }

            return WeatherAlertType.SPECIALWEATHERALERT
        }
    }
}

private fun getAlertSeverity(@IntRange(from = 0, to = 100) severity: Int): WeatherAlertSeverity {
    return when {
        severity >= 75 -> {
            WeatherAlertSeverity.EXTREME
        }
        severity >= 50 -> {
            WeatherAlertSeverity.SEVERE
        }
        severity >= 25 -> {
            WeatherAlertSeverity.MODERATE
        }
        else -> {
            WeatherAlertSeverity.MINOR
        }
    }
}

private fun getDayOfWeekFromSegment(dayofweek: String): DayOfWeek {
    return when (dayofweek) {
        "1" -> DayOfWeek.SUNDAY
        "2" -> DayOfWeek.MONDAY
        "3" -> DayOfWeek.TUESDAY
        "4" -> DayOfWeek.WEDNESDAY
        "5" -> DayOfWeek.THURSDAY
        "6" -> DayOfWeek.FRIDAY
        "7" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }
}

private fun WeatherAlert.setDateTimeFromSegment(timeSegment: List<TimeSegmentItem>) {
    if (timeSegment.size > 1) {
        val last = timeSegment.size - 1
        val startDate = DateTimeUtils.getClosestWeekday(getDayOfWeekFromSegment(timeSegment[0].dayOfWeek))
        val endDate = DateTimeUtils.getClosestWeekday(getDayOfWeekFromSegment(timeSegment[last].dayOfWeek))
        date = ZonedDateTime.of(startDate.plusSeconds(
                getTimeFromSegment(timeSegment[0].segment).toSecondOfDay().toLong()), ZoneOffset.UTC)
        expiresDate = ZonedDateTime.of(endDate.plusSeconds(
                getTimeFromSegment(timeSegment[last].segment).toSecondOfDay().toLong()), ZoneOffset.UTC)
    } else {
        val today = DateTimeUtils.getClosestWeekday(getDayOfWeekFromSegment(timeSegment[0].dayOfWeek))
        when (timeSegment[0].segment) {
            /* Morning */
            "M" -> {
                date = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("M").toSecondOfDay().toLong()), ZoneOffset.UTC)
                expiresDate = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("A").toSecondOfDay().toLong()), ZoneOffset.UTC)
            }
            /* Afternoon */
            "A" -> {
                date = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("A").toSecondOfDay().toLong()), ZoneOffset.UTC)
                expiresDate = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("E").toSecondOfDay().toLong()), ZoneOffset.UTC)
            }
            /* Evening */
            "E" -> {
                date = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("E").toSecondOfDay().toLong()), ZoneOffset.UTC)
                expiresDate = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("N").toSecondOfDay().toLong()), ZoneOffset.UTC)
            }
            /* Night */
            "N" -> {
                date = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("N").toSecondOfDay().toLong()), ZoneOffset.UTC)
                expiresDate = ZonedDateTime.of(today.plusDays(1).plusSeconds(
                        getTimeFromSegment("M").toSecondOfDay().toLong()), ZoneOffset.UTC) // The next morning
            }
            else -> {
                date = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("M").toSecondOfDay().toLong()), ZoneOffset.UTC)
                expiresDate = ZonedDateTime.of(today.plusSeconds(
                        getTimeFromSegment("A").toSecondOfDay().toLong()), ZoneOffset.UTC)
            }
        }
    }
}

private fun getTimeFromSegment(segment: String): LocalTime {
    var span = LocalTime.MIN
    when (segment) {
        /* Morning */
        "M" -> span = LocalTime.of(5, 0, 0) // hh:mm:ss
        /* Afternoon */
        "A" -> span = LocalTime.of(12, 0, 0) // hh:mm:ss
        /* Evening */
        "E" -> span = LocalTime.of(17, 0, 0) // hh:mm:ss
        /* Night */
        "N" -> span = LocalTime.of(21, 0, 0) // hh:mm:ss
    }
    return span
}
