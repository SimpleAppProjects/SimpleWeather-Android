package com.thewizrd.shared_resources.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public interface WeatherIconsProviderInterface {
    @DrawableRes
    int getWeatherIconResource(@NonNull String icon);

    boolean isFontIcon();
}
