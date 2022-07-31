package com.thewizrd.common.controls;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.weatherdata.model.Astronomy;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class SunPhaseViewModel {
    @NonNull
    private final LocalDateTime sunriseTime;
    @NonNull
    private final LocalDateTime sunsetTime;

    @NonNull
    private final String sunrise;
    @NonNull
    private final String sunset;
    @NonNull
    private final DateTimeFormatter formatter;

    @NonNull
    private final ZoneOffset tzOffset;

    public SunPhaseViewModel(@NonNull Astronomy astronomy, @NonNull ZoneOffset offset) {
        sunriseTime = astronomy.getSunrise();
        sunsetTime = astronomy.getSunset();

        if (DateFormat.is24HourFormat(SharedModuleKt.getSharedDeps().getContext())) {
            formatter = DateTimeUtils.ofPatternForInvariantLocale(DateTimeConstants.CLOCK_FORMAT_24HR);
        } else {
            formatter = DateTimeUtils.ofPatternForInvariantLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM);
        }
        sunrise = sunriseTime.format(formatter);
        sunset = sunsetTime.format(formatter);

        tzOffset = offset;
    }

    @NonNull
    public LocalDateTime getSunriseTime() {
        return sunriseTime;
    }

    @NonNull
    public LocalDateTime getSunsetTime() {
        return sunsetTime;
    }

    @NonNull
    public String getSunrise() {
        return sunrise;
    }

    @NonNull
    public String getSunset() {
        return sunset;
    }

    @NonNull
    public DateTimeFormatter getFormatter() {
        return formatter;
    }

    @NonNull
    public ZoneOffset getTzOffset() {
        return tzOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SunPhaseViewModel that = (SunPhaseViewModel) o;
        return sunrise.equals(that.sunrise) && sunset.equals(that.sunset) && tzOffset.equals(that.tzOffset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sunrise, sunset, tzOffset);
    }
}
