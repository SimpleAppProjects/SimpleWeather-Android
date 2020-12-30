package com.thewizrd.shared_resources.weatherdata.smc;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderDateInterface;
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderInterface;
import com.thewizrd.shared_resources.weatherdata.Astronomy;
import com.thewizrd.shared_resources.weatherdata.MoonPhase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

public class SunMoonCalcProvider implements AstroDataProviderInterface, AstroDataProviderDateInterface {
    @Override
    public Astronomy getAstronomyData(LocationData location) throws WeatherException {
        return getAstronomyData(location, ZonedDateTime.now());
    }

    @Override
    public Astronomy getAstronomyData(LocationData location, ZonedDateTime date) throws WeatherException {
        Astronomy astroData = new Astronomy();

        try {
            ZonedDateTime utc = date.withZoneSameInstant(ZoneOffset.UTC);

            SunMoonCalculator smc = new SunMoonCalculator(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
                    utc.getHour(), utc.getMinute(), utc.getSecond(),
                    location.getLongitude() * SunMoonCalculator.DEG_TO_RAD, location.getLatitude() * SunMoonCalculator.DEG_TO_RAD, 0);

            smc.calcSunAndMoon();

            // "YYYY/MM/DD HH:MM:SS UT"
            DateTimeFormatter fmt = new DateTimeFormatterBuilder()
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
                    .toFormatter(Locale.ROOT);

            LocalDateTime sunrise = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.sun.rise), fmt);
            LocalDateTime sunset = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.sun.set), fmt);
            LocalDateTime moonrise = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.moon.rise), fmt);
            LocalDateTime moonset = LocalDateTime.parse(SunMoonCalculator.getDateAsString(smc.moon.set), fmt);

            final int offsetSecs = location.getTzOffset().getTotalSeconds();

            astroData.setSunrise(sunrise.plusSeconds(offsetSecs));
            astroData.setSunset(sunset.plusSeconds(offsetSecs));
            astroData.setMoonrise(moonrise.plusSeconds(offsetSecs));
            astroData.setMoonset(moonset.plusSeconds(offsetSecs));

            MoonPhase.MoonPhaseType moonPhaseType = getMoonPhase(smc.moonAge);
            astroData.setMoonPhase(new MoonPhase(moonPhaseType));
        } catch (Exception e) {
            throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
        }

        return astroData;
    }

    // Based on calculations from: https://github.com/mourner/suncalc
    private MoonPhase.MoonPhaseType getMoonPhase(SunMoonCalculator.Ephemeris sun, SunMoonCalculator.Ephemeris moon) {
        double phi = Math.acos(Math.sin(sun.declination) * Math.sin(moon.declination) + Math.cos(sun.declination) * Math.cos(moon.declination) * Math.cos(sun.rightAscension - moon.rightAscension));
        double inc = Math.atan2((sun.distance * SunMoonCalculator.AU) * Math.sin(phi), (moon.distance * SunMoonCalculator.AU) - (sun.distance * SunMoonCalculator.AU) * Math.cos(phi));
        double angle = Math.atan2(Math.cos(sun.declination) * Math.sin(sun.rightAscension - moon.rightAscension), Math.sin(sun.declination) * Math.cos(moon.declination) - Math.cos(sun.declination) * Math.sin(moon.declination) * Math.cos(sun.rightAscension - moon.rightAscension));
        double illuminationFraction = 0.5 + 0.5 * inc * (angle < 0 ? -1 : 1) / Math.PI;
        double phasePct = illuminationFraction * 100;

        MoonPhase.MoonPhaseType moonPhaseType;
        if (phasePct >= 2 && phasePct < 23) {
            moonPhaseType = MoonPhase.MoonPhaseType.WAXING_CRESCENT;
        } else if (phasePct >= 23 && phasePct < 26) {
            moonPhaseType = MoonPhase.MoonPhaseType.FIRST_QTR;
        } else if (phasePct >= 26 && phasePct < 48) {
            moonPhaseType = MoonPhase.MoonPhaseType.WAXING_GIBBOUS;
        } else if (phasePct >= 48 && phasePct < 52) {
            moonPhaseType = MoonPhase.MoonPhaseType.FULL_MOON;
        } else if (phasePct >= 52 && phasePct < 73) {
            moonPhaseType = MoonPhase.MoonPhaseType.WANING_GIBBOUS;
        } else if (phasePct >= 73 && phasePct < 76) {
            moonPhaseType = MoonPhase.MoonPhaseType.LAST_QTR;
        } else if (phasePct >= 76 && phasePct < 98) {
            moonPhaseType = MoonPhase.MoonPhaseType.WANING_CRESCENT;
        } else { // 0, 1, 98, 99, 100
            moonPhaseType = MoonPhase.MoonPhaseType.NEWMOON;
        }

        return moonPhaseType;
    }

    private MoonPhase.MoonPhaseType getMoonPhase(final double moonAge) {
        final double moonCycle = 29.530588853;
        final double moonPhaseLength = (moonCycle / 8);
        final double newMoonStart = moonCycle - (moonPhaseLength / 2);
        final double newMoonEnd = moonPhaseLength / 2;
        final double waxingCrescentStart = newMoonEnd;
        final double waxingCrescentEnd = waxingCrescentStart + moonPhaseLength;
        final double firstQuarterStart = waxingCrescentEnd;
        final double firstQuarterEnd = firstQuarterStart + moonPhaseLength;
        final double waxingGibbousStart = firstQuarterEnd;
        final double waxingGibbousEnd = waxingGibbousStart + moonPhaseLength;
        final double fullMoonStart = waxingGibbousEnd;
        final double fullMoonEnd = fullMoonStart + moonPhaseLength;
        final double waningGibbousStart = fullMoonEnd;
        final double waningGibbousEnd = waningGibbousStart + moonPhaseLength;
        final double lastQuarterStart = waningGibbousEnd;
        final double lastQuarterEnd = lastQuarterStart + moonPhaseLength;
        final double waningCrescentStart = lastQuarterEnd;
        final double waningCrescentEnd = newMoonStart;

        MoonPhase.MoonPhaseType moonPhaseType;
        if ((moonAge >= waxingCrescentStart) && (moonAge <= waxingCrescentEnd)) {
            moonPhaseType = MoonPhase.MoonPhaseType.WAXING_CRESCENT;
        } else if ((moonAge >= firstQuarterStart) && (moonAge <= firstQuarterEnd)) {
            moonPhaseType = MoonPhase.MoonPhaseType.FIRST_QTR;
        } else if ((moonAge >= waxingGibbousStart) && (moonAge <= waxingGibbousEnd)) {
            moonPhaseType = MoonPhase.MoonPhaseType.WAXING_GIBBOUS;
        } else if ((moonAge >= fullMoonStart) && (moonAge <= fullMoonEnd)) {
            moonPhaseType = MoonPhase.MoonPhaseType.FULL_MOON;
        } else if (moonAge >= waningGibbousStart && moonAge <= waningGibbousEnd) {
            moonPhaseType = MoonPhase.MoonPhaseType.WANING_GIBBOUS;
        } else if (moonAge >= lastQuarterStart && moonAge <= lastQuarterEnd) {
            moonPhaseType = MoonPhase.MoonPhaseType.LAST_QTR;
        } else if (moonAge >= waningCrescentStart && moonAge <= waningCrescentEnd) {
            moonPhaseType = MoonPhase.MoonPhaseType.WANING_CRESCENT;
        } else {
            moonPhaseType = MoonPhase.MoonPhaseType.NEWMOON;
        }

        return moonPhaseType;
    }
}
