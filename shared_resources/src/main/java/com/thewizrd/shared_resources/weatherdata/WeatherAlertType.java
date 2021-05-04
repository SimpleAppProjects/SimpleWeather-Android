package com.thewizrd.shared_resources.weatherdata;

import android.util.SparseArray;

public enum WeatherAlertType {
    SPECIALWEATHERALERT(0),
    HURRICANELOCALSTATEMENT(1),
    HURRICANEWINDWARNING(2),
    TORNADOWARNING(3),
    TORNADOWATCH(4),
    SEVERETHUNDERSTORMWARNING(5),
    SEVERETHUNDERSTORMWATCH(6),
    WINTERWEATHER(7),
    FLOODWARNING(8),
    FLOODWATCH(9),
    HIGHWIND(10),
    SEVEREWEATHER(11),
    HEAT(12),
    DENSEFOG(13),
    FIRE(14),
    VOLCANO(15),
    DENSESMOKE(16),
    DUSTADVISORY(17),
    EARTHQUAKEWARNING(18),
    GALEWARNING(19),
    SMALLCRAFT(20),
    STORMWARNING(21),
    TSUNAMIWATCH(22),
    TSUNAMIWARNING(23);

    private final int value;

    public int getValue() {
        return value;
    }

    private WeatherAlertType(int value) {
        this.value = value;
    }

    private static final SparseArray<WeatherAlertType> map = new SparseArray<>();

    static {
        for (WeatherAlertType alertType : values()) {
            map.put(alertType.value, alertType);
        }
    }

    public static WeatherAlertType valueOf(int value) {
        return map.get(value);
    }
}
