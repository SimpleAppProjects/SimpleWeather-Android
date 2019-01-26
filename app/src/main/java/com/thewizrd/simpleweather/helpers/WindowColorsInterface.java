package com.thewizrd.simpleweather.helpers;

import android.support.annotation.ColorInt;

public interface WindowColorsInterface {
    void setWindowBarColors(@ColorInt int color);

    void setWindowBarColors(@ColorInt int statusBarColor, @ColorInt int navBarColor);
}
