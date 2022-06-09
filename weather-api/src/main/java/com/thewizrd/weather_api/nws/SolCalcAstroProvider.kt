package com.thewizrd.weather_api.nws

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.AstroDataDateProvider
import com.thewizrd.shared_resources.weatherdata.AstroDataProvider
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

class SolCalcAstroProvider : AstroDataProvider, AstroDataDateProvider {

    // Calculations from NOAA Solar Calculator: https://www.esrl.noaa.gov/gmd/grad/solcalc/
    override suspend fun getAstronomyData(location: LocationData): Astronomy {
        return getAstronomyData(location, ZonedDateTime.now())
    }

    override suspend fun getAstronomyData(location: LocationData, date: ZonedDateTime): Astronomy {
        val astroData = getSunriseSetTimeUTC(date, location.latitude, location.longitude)
        val astronomy = Astronomy()
        astronomy.sunrise =
            astroData.sunriseUTC!!.plusSeconds(location.tzOffset.totalSeconds.toLong())
        astronomy.sunset =
            astroData.sunsetUTC!!.plusSeconds(location.tzOffset.totalSeconds.toLong())
        astronomy.moonrise = DateTimeUtils.LOCALDATETIME_MIN
        astronomy.moonset = DateTimeUtils.LOCALDATETIME_MIN
        return astronomy
    }

    private class AstroData {
        var sunriseUTC: LocalDateTime? = null
        var sunsetUTC: LocalDateTime? = null
    }

    companion object {
        // Calculations from NOAA: https://www.esrl.noaa.gov/gmd/grad/solcalc/
        private fun getSunriseSetTimeUTC(date: ZonedDateTime, lat: Double, lon: Double): AstroData {
            val jday = getJD(date.year, date.monthValue, date.dayOfMonth)
            val rise = calcSunriseSet(true, jday, lat, lon) // in minutes
            val set = calcSunriseSet(false, jday, lat, lon) // in minutes
            val sunriseTime = toLocalTime(rise)
            val sunsetTime = toLocalTime(set)
            val astroData = AstroData()
            // Add time in minutes
            astroData.sunriseUTC = date.toLocalDate().atTime(sunriseTime)
            astroData.sunsetUTC = date.toLocalDate().atTime(sunsetTime)
            return astroData
        }

        private fun toLocalTime(timeInMins: Double): LocalTime {
            if (timeInMins >= 0) {
                val floatHour = timeInMins / 60.0
                var hour = Math.floor(floatHour)
                val floatMinute = 60.0 * (floatHour - Math.floor(floatHour))
                var minute = Math.floor(floatMinute)
                val floatSec = 60.0 * (floatMinute - Math.floor(floatMinute))
                var second = Math.floor(floatSec + 0.5)
                if (second > 59) {
                    second = 0.0
                    minute += 1.0
                }
                if (second >= 30) minute++
                if (minute > 59) {
                    minute = 0.0
                    hour += 1.0
                }
                return LocalTime.MIDNIGHT.plusHours(hour.toLong()).plusMinutes(minute.toLong())
            }
            return LocalTime.MIDNIGHT
        }

        // rise = 1 for sunrise, 0 for sunset
        private fun calcSunriseSet(rise: Boolean, JD: Double, latitude: Double, longitude: Double
        ): Double {
            val timeUTC = calcSunriseSetUTC(rise, JD, latitude, longitude) // in minutes
            return calcSunriseSetUTC(rise, JD + timeUTC / 1440.0, latitude, longitude)
        }

        private fun calcSunriseSetUTC(rise: Boolean, JD: Double, latitude: Double,
                                      longitude: Double
        ): Double {
            val t = calcTimeJulianCent(JD)
            val eqTime = calcEquationOfTime(t)
            val solarDec = calcSunDeclination(t)
            var hourAngle = calcHourAngleSunrise(latitude, solarDec)
            if (!rise) hourAngle = -hourAngle
            val delta = longitude + ConversionMethods.toDegrees(hourAngle)
            return 720 - 4.0 * delta - eqTime
        }

        private fun calcHourAngleSunrise(lat: Double, solarDec: Double): Double {
            val latRad = ConversionMethods.toRadians(lat)
            val sdRad = ConversionMethods.toRadians(solarDec)
            val HAarg = Math.cos(ConversionMethods.toRadians(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad)
            return Math.acos(HAarg) // in radians (for sunset, use -HA)
        }

        private fun getJD(year: Int, month: Int, day: Int): Double {
            var year = year
            var month = month
            if (month <= 2) {
                year -= 1
                month += 12
            }
            val A = Math.floor(year.toDouble() / 100)
            val B = 2 - A + Math.floor(A / 4)
            return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + B - 1524.5
        }

        private fun calcTimeJulianCent(jd: Double): Double {
            return (jd - 2451545.0) / 36525.0
        }

        private fun calcSunDeclination(t: Double): Double {
            val e = calcObliquityCorrection(t)
            val lambda = calcSunApparentLong(t)
            val sint = Math.sin(ConversionMethods.toRadians(e)) * Math.sin(ConversionMethods.toRadians(lambda))
            return ConversionMethods.toDegrees(Math.asin(sint)) // in degrees
        }

        private fun calcSunApparentLong(t: Double): Double {
            val o = calcSunTrueLong(t)
            val omega = 125.04 - 1934.136 * t
            return o - 0.00569 - 0.00478 * Math.sin(ConversionMethods.toRadians(omega)) // in degrees
        }

        private fun calcSunTrueLong(t: Double): Double {
            val l0 = calcGeomMeanLongSun(t)
            val c = calcSunEqOfCenter(t)
            return l0 + c // in degrees
        }

        private fun calcSunEqOfCenter(t: Double): Double {
            val m = calcGeomMeanAnomalySun(t)
            val mrad = ConversionMethods.toRadians(m)
            val sinm = Math.sin(mrad)
            val sin2m = Math.sin(mrad + mrad)
            val sin3m = Math.sin(mrad + mrad + mrad)
            return sinm * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289 // in degrees
        }

        private fun calcEquationOfTime(t: Double): Double {
            val epsilon = calcObliquityCorrection(t)
            val l0 = calcGeomMeanLongSun(t)
            val e = calcEccentricityEarthOrbit(t)
            val m = calcGeomMeanAnomalySun(t)
            var y = Math.tan(ConversionMethods.toRadians(epsilon) / 2.0)
            y *= y
            val sin2l0 = Math.sin(2.0 * ConversionMethods.toRadians(l0))
            val sinm = Math.sin(ConversionMethods.toRadians(m))
            val cos2l0 = Math.cos(2.0 * ConversionMethods.toRadians(l0))
            val sin4l0 = Math.sin(4.0 * ConversionMethods.toRadians(l0))
            val sin2m = Math.sin(2.0 * ConversionMethods.toRadians(m))
            val Etime = y * sin2l0 - 2.0 * e * sinm + 4.0 * e * y * sinm * cos2l0 - 0.5 * y * y * sin4l0 - 1.25 * e * e * sin2m
            return ConversionMethods.toDegrees(Etime) * 4.0 // in minutes of time
        }

        private fun calcGeomMeanAnomalySun(t: Double): Double {
            return 357.52911 + t * (35999.05029 - 0.0001537 * t) // in degrees
        }

        private fun calcEccentricityEarthOrbit(t: Double): Double {
            return 0.016708634 - t * (0.000042037 + 0.0000001267 * t) // unitless
        }

        private fun calcGeomMeanLongSun(t: Double): Double {
            var L0 = 280.46646 + t * (36000.76983 + t * 0.0003032)
            while (L0 > 360.0) {
                L0 -= 360.0
            }
            while (L0 < 0.0) {
                L0 += 360.0
            }
            return L0 // in degrees
        }

        private fun calcObliquityCorrection(t: Double): Double {
            val e0 = calcMeanObliquityOfEcliptic(t)
            val omega = 125.04 - 1934.136 * t
            return e0 + 0.00256 * Math.cos(ConversionMethods.toRadians(omega)) // in degrees
        }

        private fun calcMeanObliquityOfEcliptic(t: Double): Double {
            val seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
            return 23.0 + (26.0 + seconds / 60.0) / 60.0 // in degrees
        }
    }
}