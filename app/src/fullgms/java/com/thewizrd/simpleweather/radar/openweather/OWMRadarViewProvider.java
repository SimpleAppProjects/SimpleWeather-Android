package com.thewizrd.simpleweather.radar.openweather;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.bumptech.glide.load.model.GlideUrl;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.thewizrd.shared_resources.di.UtilsModuleKt;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider;
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider;
import com.thewizrd.weather_api.keys.Keys;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class OWMRadarViewProvider extends MapTileRadarViewProvider {
    private TileProvider tileProvider;

    public OWMRadarViewProvider(@NonNull Context context, @NonNull ViewGroup rootView) {
        super(context, rootView);
    }

    @Override
    public void onCreateView(@Nullable Bundle savedInstanceState) {
        super.onCreateView(savedInstanceState);
        if (getViewContainer().getChildCount() == 0) {
            getViewContainer().addView(getMapView());
        }
    }

    @Override
    public void updateRadarView() {
        getMapView().getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        super.onMapReady(googleMap);

        if (tileProvider == null) {
            tileProvider = new OWMTileProvider(getContext());
            googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        }
    }

    private static class OWMTileProvider extends CachingUrlTileProvider {
        public OWMTileProvider(@NonNull Context context) {
            super(context, 256, 256);
        }

        @Override
        public GlideUrl getTileUrl(int x, int y, int zoom) {
            if (!checkTileExists(x, y, zoom)) {
                return null;
            }

            /* Define the URL pattern for the tile images */
            return new GlideUrl(
                    String.format(
                            Locale.ROOT, "https://tile.openweathermap.org/map/precipitation_new/%d/%d/%d.png?appid=%s", zoom, x, y,
                            getKey()
                    )
            );
        }

        /*
         * Check that the tile server supports the requested x, y and zoom.
         * Complete this stub according to the tile range you support.
         * If you support a limited range of tiles at different zoom levels, then you
         * need to define the supported x, y range at each zoom level.
         */
        private boolean checkTileExists(int x, int y, int zoom) {
            return (zoom >= MIN_ZOOM_LEVEL && zoom <= MAX_ZOOM_LEVEL);
        }

        private String getKey() {
            String key = UtilsModuleKt.getSettingsManager().getAPIKey(WeatherAPI.OPENWEATHERMAP);

            if (StringUtils.isNullOrWhitespace(key))
                return Keys.getOWMKey();

            return key;
        }
    }
}
