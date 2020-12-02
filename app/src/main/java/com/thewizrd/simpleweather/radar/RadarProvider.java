package com.thewizrd.simpleweather.radar;

import android.content.SharedPreferences;
import android.os.Build;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringDef;
import androidx.core.util.ObjectsCompat;
import androidx.fragment.app.Fragment;

import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.simpleweather.App;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static java.util.Arrays.asList;

public final class RadarProvider {
    public static final String KEY_RADARPROVIDER = "key_radarprovider";

    public static final String EARTHWINDMAP = "nullschool";
    public static final String RAINVIEWER = "rainviewer";
    public static final String OPENWEATHERMAP = "openweather";

    @StringDef({
            EARTHWINDMAP, RAINVIEWER, OPENWEATHERMAP
    })
    @Retention(RetentionPolicy.CLASS)
    public @interface RadarProviders {
    }

    public static final String EARTHWINDMAP_DEFAULT_URL = "https://earth.nullschool.net/#current/wind/surface/level/overlay=precip_3hr";
    public static final String EARTHWINDMAP_URL_FORMAT = EARTHWINDMAP_DEFAULT_URL + "/orthographic=%s,%s,3000";

    public static List<ProviderEntry> RadarProviders = asList(
            new ProviderEntry("EarthWindMap Project", EARTHWINDMAP,
                    "https://earth.nullschool.net/", "https://earth.nullschool.net/"),
            new ProviderEntry("RainViewer", RAINVIEWER,
                    "https://www.rainviewer.com/", "https://www.rainviewer.com/api.html"),
            new ProviderEntry("OpenWeatherMap", OPENWEATHERMAP,
                    "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up")
    );

    @RadarProviders
    public static String getRadarProvider() {
        SharedPreferences prefs = App.getInstance().getPreferences();
        return prefs.getString(KEY_RADARPROVIDER, EARTHWINDMAP);
    }

    @RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
    public static RadarViewProvider getRadarViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
        if (ObjectsCompat.equals(RadarProvider.getRadarProvider(), RadarProvider.RAINVIEWER)) {
            return new RainViewerViewProvider(fragment, rootView);
        } else if (ObjectsCompat.equals(RadarProvider.getRadarProvider(), RadarProvider.OPENWEATHERMAP)) {
            return new OWMRadarViewProvider(fragment, rootView);
        } else {
            return new EarthWindMapViewProvider(fragment, rootView);
        }
    }
}
