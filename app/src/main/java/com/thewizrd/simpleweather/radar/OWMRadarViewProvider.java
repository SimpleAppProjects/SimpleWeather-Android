package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.simpleweather.R;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class OWMRadarViewProvider extends MapTileRadarViewProvider {
    public OWMRadarViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
        super(fragment, rootView);
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
                Log.e("RadarView", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("RadarView", "Can't find style. Error: ", e);
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
            String s = String.format(Locale.ROOT, "https://tile.openweathermap.org/map/precipitation_new/%d/%d/%d.png?appid=%s", zoom, x, y, Keys.getOWMKey());
            return s;
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
