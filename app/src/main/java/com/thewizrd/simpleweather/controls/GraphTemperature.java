package com.thewizrd.simpleweather.controls;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.controls.graphs.YEntryData;

public class GraphTemperature {
    private YEntryData hiTempData;
    private YEntryData loTempData;
    private final String tempUnit;

    GraphTemperature(boolean isFahrenheit) {
        tempUnit = isFahrenheit ? Settings.FAHRENHEIT : Settings.CELSIUS;
    }

    public YEntryData getHiTempData() {
        return hiTempData;
    }

    public void setHiTempData(YEntryData hiTempData) {
        this.hiTempData = hiTempData;
    }

    public YEntryData getLoTempData() {
        return loTempData;
    }

    public void setLoTempData(YEntryData loTempData) {
        this.loTempData = loTempData;
    }

    @NonNull
    public String getTempUnit() {
        return tempUnit;
    }
}
