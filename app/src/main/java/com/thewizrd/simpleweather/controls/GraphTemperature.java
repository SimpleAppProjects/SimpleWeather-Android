package com.thewizrd.simpleweather.controls;

import com.thewizrd.simpleweather.controls.graphs.YEntryData;

public class GraphTemperature {
    private YEntryData hiTempData;
    private YEntryData loTempData;

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
}
