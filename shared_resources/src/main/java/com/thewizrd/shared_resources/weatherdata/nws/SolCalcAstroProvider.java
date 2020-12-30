package com.thewizrd.shared_resources.weatherdata.nws;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderDateInterface;
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderInterface;
import com.thewizrd.shared_resources.weatherdata.Astronomy;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class SolCalcAstroProvider implements AstroDataProviderInterface, AstroDataProviderDateInterface {

    // Calculations from NOAA Solar Calculator: https://www.esrl.noaa.gov/gmd/grad/solcalc/
    @Override
    public Astronomy getAstronomyData(LocationData location) throws WeatherException {
        return getAstronomyData(location, ZonedDateTime.now());
    }

    @Override
    public Astronomy getAstronomyData(LocationData location, ZonedDateTime date) throws WeatherException {
        AstroData astroData = getSunriseSetTimeUTC(date, location.getLatitude(), location.getLongitude());

        Astronomy astronomy = new Astronomy();
        astronomy.setSunrise(astroData.sunriseUTC.plusSeconds(location.getTzOffset().getTotalSeconds()));
        astronomy.setSunset(astroData.sunsetUTC.plusSeconds(location.getTzOffset().getTotalSeconds()));
        astronomy.setMoonrise(DateTimeUtils.getLocalDateTimeMIN());
        astronomy.setMoonset(DateTimeUtils.getLocalDateTimeMIN());
        return astronomy;
    }

    private static class AstroData {
        LocalDateTime sunriseUTC;
        LocalDateTime sunsetUTC;
    }

    // Calculations from NOAA: https://www.esrl.noaa.gov/gmd/grad/solcalc/
    private static AstroData getSunriseSetTimeUTC(ZonedDateTime date, double lat, double lon) {
        ZonedDateTime now = date;

        double jday = getJD(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        double rise = calcSunriseSet(true, jday, lat, lon); // in minutes
        double set = calcSunriseSet(false, jday, lat, lon); // in minutes

        LocalTime sunriseTime = toLocalTime(rise);
        LocalTime sunsetTime = toLocalTime(set);

        AstroData astroData = new AstroData();
        // Add time in minutes
        astroData.sunriseUTC = now.toLocalDate().atTime(sunriseTime);
        astroData.sunsetUTC = now.toLocalDate().atTime(sunsetTime);

        return astroData;
    }

    private static LocalTime toLocalTime(double timeInMins) {
        if (timeInMins >= 0) {
            double floatHour = timeInMins / 60.0;
            double hour = Math.floor(floatHour);
            double floatMinute = 60.0 * (floatHour - Math.floor(floatHour));
            double minute = Math.floor(floatMinute);
            double floatSec = 60.0 * (floatMinute - Math.floor(floatMinute));
            double second = Math.floor(floatSec + 0.5);
            if (second > 59) {
                second = 0;
                minute += 1;
            }
            if (second >= 30) minute++;
            if (minute > 59) {
                minute = 0;
                hour += 1;
            }

            return LocalTime.MIDNIGHT.plusHours((long) hour).plusMinutes((long) minute);
        }

        return LocalTime.MIDNIGHT;
    }

    // rise = 1 for sunrise, 0 for sunset
    private static double calcSunriseSet(boolean rise, double JD, double latitude, double longitude) {
        double timeUTC = calcSunriseSetUTC(rise, JD, latitude, longitude);    // in minutes
        double newTimeUTC = calcSunriseSetUTC(rise, JD + timeUTC / 1440.0, latitude, longitude);    // in minutes

        return newTimeUTC;
    }

    private static double calcSunriseSetUTC(boolean rise, double JD, double latitude, double longitude) {
        double t = calcTimeJulianCent(JD);
        double eqTime = calcEquationOfTime(t);
        double solarDec = calcSunDeclination(t);
        double hourAngle = calcHourAngleSunrise(latitude, solarDec);
        if (!rise) hourAngle = -hourAngle;
        double delta = longitude + ConversionMethods.toDegrees(hourAngle);
        double timeUTC = 720 - (4.0 * delta) - eqTime; // in minutes

        return timeUTC;
    }

    private static double calcHourAngleSunrise(double lat, double solarDec) {
        double latRad = ConversionMethods.toRadians(lat);
        double sdRad = ConversionMethods.toRadians(solarDec);
        double HAarg = (Math.cos(ConversionMethods.toRadians(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad));
        double HA = Math.acos(HAarg);
        return HA;        // in radians (for sunset, use -HA)
    }

    private static double getJD(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double A = Math.floor((double) year / 100);
        double B = 2 - A + Math.floor(A / 4);
        double JD = Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + B - 1524.5;
        return JD;
    }

    private static double calcTimeJulianCent(double jd) {
        return (jd - 2451545.0) / 36525.0;
    }

    private static double calcSunDeclination(double t) {
        double e = calcObliquityCorrection(t);
        double lambda = calcSunApparentLong(t);
        double sint = Math.sin(ConversionMethods.toRadians(e)) * Math.sin(ConversionMethods.toRadians(lambda));
        double theta = ConversionMethods.toDegrees(Math.asin(sint));
        return theta;        // in degrees
    }

    private static double calcSunApparentLong(double t) {
        double o = calcSunTrueLong(t);
        double omega = 125.04 - 1934.136 * t;
        double lambda = o - 0.00569 - 0.00478 * Math.sin(ConversionMethods.toRadians(omega));
        return lambda;        // in degrees
    }

    private static double calcSunTrueLong(double t) {
        double l0 = calcGeomMeanLongSun(t);
        double c = calcSunEqOfCenter(t);
        double O = l0 + c;
        return O;        // in degrees
    }

    private static double calcSunEqOfCenter(double t) {
        double m = calcGeomMeanAnomalySun(t);
        double mrad = ConversionMethods.toRadians(m);
        double sinm = Math.sin(mrad);
        double sin2m = Math.sin(mrad + mrad);
        double sin3m = Math.sin(mrad + mrad + mrad);
        double C = sinm * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
        return C;        // in degrees
    }

    private static double calcEquationOfTime(double t) {
        double epsilon = calcObliquityCorrection(t);
        double l0 = calcGeomMeanLongSun(t);
        double e = calcEccentricityEarthOrbit(t);
        double m = calcGeomMeanAnomalySun(t);

        double y = Math.tan(ConversionMethods.toRadians(epsilon) / 2.0);
        y *= y;

        double sin2l0 = Math.sin(2.0 * ConversionMethods.toRadians(l0));
        double sinm = Math.sin(ConversionMethods.toRadians(m));
        double cos2l0 = Math.cos(2.0 * ConversionMethods.toRadians(l0));
        double sin4l0 = Math.sin(4.0 * ConversionMethods.toRadians(l0));
        double sin2m = Math.sin(2.0 * ConversionMethods.toRadians(m));

        double Etime = y * sin2l0 - 2.0 * e * sinm + 4.0 * e * y * sinm * cos2l0 - 0.5 * y * y * sin4l0 - 1.25 * e * e * sin2m;
        return ConversionMethods.toDegrees(Etime) * 4.0;    // in minutes of time
    }

    private static double calcGeomMeanAnomalySun(double t) {
        double M = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        return M;        // in degrees
    }

    private static double calcEccentricityEarthOrbit(double t) {
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
        return e;        // unitless
    }

    private static double calcGeomMeanLongSun(double t) {
        double L0 = 280.46646 + t * (36000.76983 + t * (0.0003032));

        while (L0 > 360.0) {
            L0 -= 360.0;
        }
        while (L0 < 0.0) {
            L0 += 360.0;
        }
        return L0;        // in degrees
    }

    private static double calcObliquityCorrection(double t) {
        double e0 = calcMeanObliquityOfEcliptic(t);
        double omega = 125.04 - 1934.136 * t;
        double e = e0 + 0.00256 * Math.cos(ConversionMethods.toRadians(omega));
        return e;        // in degrees
    }

    private static double calcMeanObliquityOfEcliptic(double t) {
        double seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * (0.001813)));
        double e0 = 23.0 + (26.0 + (seconds / 60.0)) / 60.0;
        return e0;        // in degrees
    }
}
