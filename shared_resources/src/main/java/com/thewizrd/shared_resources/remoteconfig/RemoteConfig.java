package com.thewizrd.shared_resources.remoteconfig;

import androidx.annotation.NonNull;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider;
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

public final class RemoteConfig {
    private static final String DEFAULT_WEATHERPROVIDER_KEY = "default_weather_provider";

    public static LocationProviderImpl getLocationProvider(@NonNull String weatherAPI) {
        final String configJson = FirebaseRemoteConfig.getInstance().getString(weatherAPI);

        WeatherProviderConfig config = JSONParser.deserializer(configJson, WeatherProviderConfig.class);

        if (config != null) {
            switch (config.getLocSource()) {
                case WeatherAPI.HERE:
                    return new HERELocationProvider();
                case WeatherAPI.LOCATIONIQ:
                    return new LocationIQProvider();
                case WeatherAPI.GOOGLE:
                    return new GoogleLocationProvider();
                case WeatherAPI.WEATHERAPI:
                    return new WeatherApiLocationProvider();
            }
        }

        return null;
    }

    public static boolean isProviderEnabled(@NonNull String weatherAPI) {
        final String configJson = FirebaseRemoteConfig.getInstance().getString(weatherAPI);

        WeatherProviderConfig config = JSONParser.deserializer(configJson, WeatherProviderConfig.class);

        if (config != null) {
            return config.isEnabled();
        }

        return true;
    }

    public static boolean updateWeatherProvider() {
        final String API = Settings.getAPI();

        final String configJson = FirebaseRemoteConfig.getInstance().getString(API);
        WeatherProviderConfig config = JSONParser.deserializer(configJson, WeatherProviderConfig.class);

        if (config != null) {
            final boolean isEnabled = config.isEnabled();

            if (!isEnabled) {
                if (!StringUtils.isNullOrWhitespace(config.getNewWeatherSource())) {
                    Settings.setAPI(config.getNewWeatherSource());
                    WeatherManager.getInstance().updateAPI();
                } else {
                    Settings.setAPI(getDefaultWeatherProvider());
                    WeatherManager.getInstance().updateAPI();
                }
                return true;
            }
        }

        return false;
    }

    @WeatherAPI.WeatherAPIs
    public static String getDefaultWeatherProvider() {
        return FirebaseRemoteConfig.getInstance().getString(DEFAULT_WEATHERPROVIDER_KEY);
    }

    public static void checkConfig() {
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
                .addOnCompleteListener(task -> {
                    // Update weather provider if needed
                    updateWeatherProvider();
                });
    }
}
