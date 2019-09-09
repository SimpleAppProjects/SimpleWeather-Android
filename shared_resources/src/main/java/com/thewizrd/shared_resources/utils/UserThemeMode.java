package com.thewizrd.shared_resources.utils;

import android.util.SparseArray;

public enum UserThemeMode {
    FOLLOW_SYSTEM(0),
    DARK(1),
    AMOLED_DARK(2);

    private final int value;

    public int getValue() {
        return value;
    }

    private UserThemeMode(int value) {
        this.value = value;
    }

    private static SparseArray<UserThemeMode> map = new SparseArray<>();

    static {
        for (UserThemeMode mode : values()) {
            map.put(mode.value, mode);
        }
    }

    public static UserThemeMode valueOf(int value) {
        return map.get(value);
    }

    public interface OnThemeChangeListener {
        void onThemeChanged(UserThemeMode mode);
    }
}
