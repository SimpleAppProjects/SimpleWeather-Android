package com.thewizrd.simpleweather.radar.openweather;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider;
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider;

import java.util.Locale;

import timber.log.Timber;

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
    public void onMapReady(GoogleMap googleMap) {
        final Configuration currentConfig = getContext().getResources().getConfiguration();
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getContext(), isNightMode ? R.raw.gmap_dark_style : R.raw.gmap_light_style));

            if (!success) {
                Timber.tag("RadarView").e("Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Timber.tag("RadarView").e(e, "Can't find style.");
        }

        CameraPosition cameraPosition = getMapCameraPosition();
        if (cameraPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            if (interactionsEnabled()) {
                if (locationMarker == null) {
                    locationMarker = googleMap.addMarker(new MarkerOptions().position(cameraPosition.target));
                } else {
                    locationMarker.setPosition(cameraPosition.target);
                }
            }
        }

        if (tileProvider == null) {
            tileProvider = new OWMTileProvider(getContext());
            googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        }

        UiSettings mapUISettings = googleMap.getUiSettings();
        mapUISettings.setScrollGesturesEnabled(interactionsEnabled());
    }

    private static class OWMTileProvider extends CachingUrlTileProvider {
        public OWMTileProvider(@NonNull Context context) {
            super(context, 256, 256);
        }

        @Override
        public String getTileUrl(int x, int y, int zoom) {
            if (!checkTileExists(x, y, zoom)) {
                return null;
            }

            /* Define the URL pattern for the tile images */
            return String.format(Locale.ROOT, "https://tile.openweathermap.org/map/precipitation_new/%d/%d/%d.png?appid=%s", zoom, x, y, Keys.getOWMKey());
        }

        /*
         * Check that the tile server supports the requested x, y and zoom.
         * Complete this stub according to the tile range you support.
         * If you support a limited range of tiles at different zoom levels, then you
         * need to define the supported x, y range at each zoom level.
         */
        private boolean checkTileExists(int x, int y, int zoom) {
            int minZoom = 6;
            int maxZoom = 6;

            return (zoom >= minZoom && zoom <= maxZoom);
        }
    }
}
