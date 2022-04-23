package com.thewizrd.common.controls;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.weatherdata.model.Astronomy;
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MoonPhaseViewModel {
    private DetailItemViewModel moonPhase;
    private MoonPhase.MoonPhaseType phaseType;

    private LocalTime moonriseTime;
    private LocalTime moonsetTime;

    private String moonrise;
    private String moonset;
    private final DateTimeFormatter formatter;

    public MoonPhaseViewModel(@NonNull Astronomy astronomy) {
        if (DateFormat.is24HourFormat(SharedModuleKt.getSharedDeps().getContext())) {
            formatter = DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR);
        } else {
            formatter = DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM);
        }

        if (astronomy.getMoonrise() != null && !Objects.equals(astronomy.getMoonrise(), DateTimeUtils.getLocalDateTimeMIN())) {
            moonriseTime = astronomy.getMoonrise().toLocalTime();
            moonrise = moonriseTime.format(formatter);
        }

        if (astronomy.getMoonset() != null && !Objects.equals(astronomy.getMoonset(), DateTimeUtils.getLocalDateTimeMIN())) {
            moonsetTime = astronomy.getMoonset().toLocalTime();
            moonset = moonsetTime.format(formatter);
        }

        if (astronomy.getMoonPhase() != null) {
            this.phaseType = astronomy.getMoonPhase().getPhase();
            this.moonPhase = new DetailItemViewModel(astronomy.getMoonPhase().getPhase());
        }
    }

    public LocalTime getMoonriseTime() {
        return moonriseTime;
    }

    public LocalTime getMoonsetTime() {
        return moonsetTime;
    }

    public String getMoonrise() {
        return moonrise;
    }

    public String getMoonset() {
        return moonset;
    }

    public DateTimeFormatter getFormatter() {
        return formatter;
    }

    public DetailItemViewModel getMoonPhase() {
        return moonPhase;
    }

    public void setMoonPhase(DetailItemViewModel moonPhase) {
        this.moonPhase = moonPhase;
    }

    public MoonPhase.MoonPhaseType getPhaseType() {
        return phaseType;
    }

    public void setPhaseType(MoonPhase.MoonPhaseType phaseType) {
        this.phaseType = phaseType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoonPhaseViewModel that = (MoonPhaseViewModel) o;

        return phaseType == that.phaseType &&
                Objects.equals(moonrise, that.moonrise) &&
                Objects.equals(moonset, that.moonset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phaseType, moonrise, moonset);
    }
}
