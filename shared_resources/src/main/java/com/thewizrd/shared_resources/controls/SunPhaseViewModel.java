package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;

import androidx.lifecycle.ViewModel;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.weatherdata.Astronomy;

import org.threeten.bp.format.DateTimeFormatter;

public class SunPhaseViewModel extends ViewModel {
    private String sunrise;
    private String sunset;

    public SunPhaseViewModel(Astronomy astronomy) {
        if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
            sunrise = astronomy.getSunrise().format(DateTimeFormatter.ofPattern("HH:mm"));
            sunset = astronomy.getSunset().format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            sunrise = astronomy.getSunrise().format(DateTimeFormatter.ofPattern("h:mm a"));
            sunset = astronomy.getSunset().format(DateTimeFormatter.ofPattern("h:mm a"));
        }
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
