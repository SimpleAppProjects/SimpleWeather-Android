package com.thewizrd.shared_resources.weatherdata.smc

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ErrorStatus
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderDateInterface
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderInterface
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase.MoonPhaseType
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalculator.Ephemeris
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*

class SunMoonCalcProvider : AstroDataProviderInterface, AstroDataProviderDateInterface {
    @Throws(WeatherException::class)
    override suspend fun getAstronomyData(location: LocationData): Astronomy {
        return getAstronomyData(location, ZonedDateTime.now())
    }

    @Throws(WeatherException::class)
    override suspend fun getAstronomyData(location: LocationData, date: ZonedDateTime): Astronomy =
            withContext(Dispatchers.Default) {
                val astroData = Astronomy()

                try {
                    val utc = date.withZoneSameInstant(ZoneOffset.UTC)

                    val smc = SunMoonCalculator(utc.year, utc.monthValue, utc.dayOfMonth,
                            utc.hour, utc.minute, utc.second,
                            location.longitude * SunMoonCalculator.DEG_TO_RAD, location.latitude * SunMoonCalculator.DEG_TO_RAD, 0)

                    smc.calcSunAndMoon()

                    // "YYYY/MM/DD HH:MM:SS UT"
                    val fmt = DateTimeFormatterBuilder()
                            .appendValue(ChronoField.YEAR, 4)
                            .appendLiteral('/')
                            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                            .appendLiteral('/')
                            .appendValue(ChronoField.DAY_OF_MONTH, 2)
                            .appendLiteral(' ')
                            .appendValue(ChronoField.HOUR_OF_DAY, 2)
                            .appendLiteral(':')
                            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                            .appendLiteral(':')
                            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                            .appendLiteral(" UT")
                            .toFormatter(Locale.ROOT)

                    val sunrise = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.sun.rise), fmt)
                    val sunset = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.sun.set), fmt)
                    val moonrise = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.moon.rise), fmt)
                    val moonset = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.moon.set), fmt)

                    val offsetSecs = location.tzOffset.totalSeconds

                    astroData.sunrise = sunrise.plusSeconds(offsetSecs.toLong())
                    astroData.sunset = sunset.plusSeconds(offsetSecs.toLong())
                    astroData.moonrise = moonrise.plusSeconds(offsetSecs.toLong())
                    astroData.moonset = moonset.plusSeconds(offsetSecs.toLong())

                    val moonPhaseType = getMoonPhase(smc.moonAge)
                    astroData.moonPhase = MoonPhase(moonPhaseType)
                } catch (e: Exception) {
                    throw WeatherException(ErrorStatus.UNKNOWN)
                }

                return@withContext astroData
            }

    // Based on calculations from: https://github.com/mourner/suncalc
    private fun getMoonPhase(sun: Ephemeris, moon: Ephemeris): MoonPhaseType {
        val phi = Math.acos(Math.sin(sun.declination) * Math.sin(moon.declination) + Math.cos(sun.declination) * Math.cos(moon.declination) * Math.cos(sun.rightAscension - moon.rightAscension))
        val inc = Math.atan2(sun.distance * SunMoonCalculator.AU * Math.sin(phi), moon.distance * SunMoonCalculator.AU - sun.distance * SunMoonCalculator.AU * Math.cos(phi))
        val angle = Math.atan2(Math.cos(sun.declination) * Math.sin(sun.rightAscension - moon.rightAscension), Math.sin(sun.declination) * Math.cos(moon.declination) - Math.cos(sun.declination) * Math.sin(moon.declination) * Math.cos(sun.rightAscension - moon.rightAscension))
        val illuminationFraction = 0.5 + 0.5 * inc * (if (angle < 0) -1 else 1) / Math.PI
        val phasePct = illuminationFraction * 100

        return if (phasePct >= 2 && phasePct < 23) {
            MoonPhaseType.WAXING_CRESCENT
        } else if (phasePct >= 23 && phasePct < 26) {
            MoonPhaseType.FIRST_QTR
        } else if (phasePct >= 26 && phasePct < 48) {
            MoonPhaseType.WAXING_GIBBOUS
        } else if (phasePct >= 48 && phasePct < 52) {
            MoonPhaseType.FULL_MOON
        } else if (phasePct >= 52 && phasePct < 73) {
            MoonPhaseType.WANING_GIBBOUS
        } else if (phasePct >= 73 && phasePct < 76) {
            MoonPhaseType.LAST_QTR
        } else if (phasePct >= 76 && phasePct < 98) {
            MoonPhaseType.WANING_CRESCENT
        } else { // 0, 1, 98, 99, 100
            MoonPhaseType.NEWMOON
        }
    }

    private fun getMoonPhase(moonAge: Double): MoonPhaseType {
        val moonCycle = 29.530588853
        val moonPhaseLength = moonCycle / 8
        val newMoonStart = moonCycle - moonPhaseLength / 2
        val newMoonEnd = moonPhaseLength / 2
        val waxingCrescentEnd = newMoonEnd + moonPhaseLength
        val firstQuarterEnd = waxingCrescentEnd + moonPhaseLength
        val waxingGibbousEnd = firstQuarterEnd + moonPhaseLength
        val fullMoonEnd = waxingGibbousEnd + moonPhaseLength
        val waningGibbousEnd = fullMoonEnd + moonPhaseLength
        val lastQuarterEnd = waningGibbousEnd + moonPhaseLength

        return if (moonAge >= newMoonEnd && moonAge <= waxingCrescentEnd) {
            MoonPhaseType.WAXING_CRESCENT
        } else if (moonAge >= waxingCrescentEnd && moonAge <= firstQuarterEnd) {
            MoonPhaseType.FIRST_QTR
        } else if (moonAge >= firstQuarterEnd && moonAge <= waxingGibbousEnd) {
            MoonPhaseType.WAXING_GIBBOUS
        } else if (moonAge >= waxingGibbousEnd && moonAge <= fullMoonEnd) {
            MoonPhaseType.FULL_MOON
        } else if (moonAge >= fullMoonEnd && moonAge <= waningGibbousEnd) {
            MoonPhaseType.WANING_GIBBOUS
        } else if (moonAge >= waningGibbousEnd && moonAge <= lastQuarterEnd) {
            MoonPhaseType.LAST_QTR
        } else if (moonAge >= lastQuarterEnd && moonAge <= newMoonStart) {
            MoonPhaseType.WANING_CRESCENT
        } else {
            MoonPhaseType.NEWMOON
        }
    }
}