package com.thewizrd.common.controls;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.weatherdata.model.Astronomy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SunPhaseViewModel {
    private final LocalDateTime sunriseTime;
    private final LocalDateTime sunsetTime;

    private final String sunrise;
    private final String sunset;
    private final DateTimeFormatter formatter;

    public SunPhaseViewModel(@NonNull Astronomy astronomy) {
        sunriseTime = astronomy.getSunrise();
        sunsetTime = astronomy.getSunset();

        if (DateFormat.is24HourFormat(SharedModuleKt.getSharedDeps().getContext())) {
            formatter = DateTimeUtils.ofPatternForInvariantLocale(DateTimeConstants.CLOCK_FORMAT_24HR);
        } else {
            formatter = DateTimeUtils.ofPatternForInvariantLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM);
        }
        sunrise = sunriseTime.format(formatter);
        sunset = sunsetTime.format(formatter);
    }

    public LocalDateTime getSunriseTime() {
        return sunriseTime;
    }

    public LocalDateTime getSunsetTime() {
        return sunsetTime;
    }

    public String getSunrise() {
        return sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public DateTimeFormatter getFormatter() {
        return formatter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SunPhaseViewModel that = (SunPhaseViewModel) o;

        if (sunrise != null ? !sunrise.equals(that.sunrise) : that.sunrise != null) return false;
        return sunset != null ? sunset.equals(that.sunset) : that.sunset == null;
    }

    @Override
    public int hashCode() {
        int result = sunrise != null ? sunrise.hashCode() : 0;
        result = 31 * result + (sunset != null ? sunset.hashCode() : 0);
        return result;
    }
}
