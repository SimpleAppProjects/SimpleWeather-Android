package com.thewizrd.shared_resources.icons;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.utils.Settings;

public final class WeatherIconsManager implements WeatherIconsProviderInterface {
    private static WeatherIconsManager instance;
    private static WeatherIconsProviderInterface iconsProvider;

    // Prevent instance from being created outside of this class
    private WeatherIconsManager() {
        updateIconProvider();
    }

    public static synchronized WeatherIconsManager getInstance() {
        if (instance == null)
            instance = new WeatherIconsManager();

        return instance;
    }

    public void updateIconProvider() {
        String iconsSource = Settings.getIconsProvider();
        iconsProvider = getProvider(iconsSource);
    }

    private static WeatherIconsProviderInterface getProvider(String iconsSource) {
        WeatherIconsProviderInterface iconsProvider = null;

        switch (iconsSource) {
            default:
                iconsProvider = new WeatherIconsProvider();
                break;
        }

        return iconsProvider;
    }

    @Override
    public int getWeatherIconResource(@NonNull String icon) {
        return iconsProvider.getWeatherIconResource(icon);
    }
}