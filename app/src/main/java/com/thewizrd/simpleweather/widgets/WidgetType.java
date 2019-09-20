package com.thewizrd.simpleweather.widgets;

import android.util.SparseArray;

public enum WidgetType {
    Unknown(-1),
    Widget1x1(0),
    Widget2x2(1),
    Widget4x1(2),
    Widget4x2(3),
    Widget4x1Google(4);

    private final int value;

    public int getValue() {
        return value;
    }

    private WidgetType(int value) {
        this.value = value;
    }

    private static SparseArray<WidgetType> map = new SparseArray<>();

    static {
        for (WidgetType widgetType : values()) {
            map.put(widgetType.value, widgetType);
        }
    }

    public static WidgetType valueOf(int value) {
        return map.get(value);
    }
}
