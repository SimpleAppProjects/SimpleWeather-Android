package com.thewizrd.simpleweather;

import android.support.v4.util.Pair;

public class LocationPanelModel {
    public Object Weather;
    public Pair<Integer, Object> Pair;

    public LocationPanelModel(Pair<Integer, Object> pair, Object weather) {
        this.Pair = pair;
        this.Weather = weather;
    }
}
