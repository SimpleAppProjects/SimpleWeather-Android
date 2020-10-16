package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.weatherdata.Astronomy;

import org.threeten.bp.format.DateTimeFormatter;

public class SunPhaseViewModel {
    private String sunrise;
    private String sunset;

    public SunPhaseViewModel(Astronomy astronomy) {
        DateTimeFormatter fmt;
        if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
            fmt = DateTimeUtils.ofPatternForInvariantLocale(DateTimeConstants.CLOCK_FORMAT_24HR);
        } else {
            fmt = DateTimeUtils.ofPatternForInvariantLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM);
        }
        sunrise = astronomy.getSunrise().format(fmt);
        sunset = astronomy.getSunset().format(fmt);
    }

    public String getSunrise() {
        return sunrise;
    }

    public String getSunset() {
        return sunset;
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
