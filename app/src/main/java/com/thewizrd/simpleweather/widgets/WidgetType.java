package com.thewizrd.simpleweather.widgets;

import android.util.SparseArray;

public enum WidgetType {
    Unknown(-1),
    Widget1x1(0),
    Widget2x2(1),
    Widget4x1(2),
    Widget4x2(3),
    Widget4x1Google(4),
    Widget4x1Notification(5),
    Widget4x2Clock(6),
    Widget4x2Huawei(7),
    Widget2x2MaterialYou(8),
    Widget4x2MaterialYou(9),
    Widget4x4MaterialYou(10),
    Widget2x2PillMaterialYou(11),
    Widget4x3Locations(12),
    Widget3x1MaterialYou(13),
    Widget4x2Graph(14),
    Widget4x2Tomorrow(15);

    private final int value;

    public int getValue() {
        return value;
    }

    WidgetType(int value) {
        this.value = value;
    }

    private static final SparseArray<WidgetType> map = new SparseArray<>();

    static {
        for (WidgetType widgetType : values()) {
            map.put(widgetType.value, widgetType);
        }
    }

    public static WidgetType valueOf(int value) {
        return map.get(value);
    }
}
