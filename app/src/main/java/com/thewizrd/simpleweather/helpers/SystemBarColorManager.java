package com.thewizrd.simpleweather.helpers;

import androidx.annotation.ColorInt;

public interface SystemBarColorManager {
    void setSystemBarColors(@ColorInt int color);

    void setSystemBarColors(@ColorInt int color, boolean setColors);

    void setSystemBarColors(@ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int toolbarColor, @ColorInt int navBarColor);

    void setSystemBarColors(@ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int toolbarColor, @ColorInt int navBarColor, boolean setColors);
}
