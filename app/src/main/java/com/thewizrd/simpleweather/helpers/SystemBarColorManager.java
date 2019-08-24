package com.thewizrd.simpleweather.helpers;

import androidx.annotation.ColorInt;

public interface SystemBarColorManager {
    void setSystemBarColors(@ColorInt int statusBarColor, @ColorInt int navBarColor);

    void setSystemBarColors(@ColorInt int statusBarColor, @ColorInt int toolbarColor, @ColorInt int navBarColor);
}
