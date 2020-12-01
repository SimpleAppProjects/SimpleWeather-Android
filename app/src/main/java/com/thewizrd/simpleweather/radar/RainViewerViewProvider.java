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
import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.R;

import org.threeten.bp.Clock;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(value = Build.VERSION_CODES.KITKAT)
public class RainViewerViewProvider extends MapTileRadarViewProvider {

    public RainViewerViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
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
                            getContext(), isNightMode ? R.raw.gmap_dark_style : R.raw.gmap_light_style)
            );

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
            tileProvider = new RainViewTileProvider(getContext());
            googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        }

        UiSettings mapUISettings = googleMap.getUiSettings();
        mapUISettings.setScrollGesturesEnabled(interactionsEnabled());
    }

    private static class RainViewTileProvider extends CachingUrlTileProvider {
        private List<Long> availableTimestamps;

        public RainViewTileProvider(@NonNull Context context) {
            super(context, 256, 256);
            availableTimestamps = new ArrayList<>();
        }

        @Override
        public String getTileUrl(int x, int y, int zoom) {
            if (!checkTileExists(x, y, zoom)) {
                return null;
            }

            // Check timestamps
            long lastTimestamp = -1;
            if (availableTimestamps != null && !availableTimestamps.isEmpty()) {
                lastTimestamp = Iterables.getLast(availableTimestamps);
                if (lastTimestamp > 0) {
                    Instant instantNow = Instant.now(Clock.systemUTC());
                    Instant lastInstant = Instant.ofEpochSecond(lastTimestamp);
                    if (Duration.between(lastInstant, instantNow).abs().toMinutes() > 15) {
                        refreshTimeStamps();
                    }
                }
            } else {
                refreshTimeStamps();
            }
            lastTimestamp = Iterables.getLast(availableTimestamps, 0L);

            if (lastTimestamp > 0) {
                /* Define the URL pattern for the tile images */
                String s = String.format(Locale.ROOT, "https://tilecache.rainviewer.com/v2/radar/%d/256/%d/%d/%d/1/1_1.png", lastTimestamp, zoom, x, y);
                return s;
            }

            return null;
        }

        private void refreshTimeStamps() {
            OkHttpClient httpClient = SimpleLibrary.getInstance().getHttpClient();
            Response response = null;

            try {
                Request request = new Request.Builder()
                        .get()
                        .url("https://api.rainviewer.com/public/maps.json")
                        .build();

                // Connect to webstream
                response = httpClient.newCall(request).execute();
                final InputStream stream = response.body().byteStream();

                // Load data
                Type arrListType = new TypeToken<ArrayList<Long>>() {
                }.getType();
                List<Long> root = JSONParser.deserializer(stream, arrListType);

                availableTimestamps = new ArrayList<>(root);

                // End Stream
                stream.close();
            } catch (Exception ex) {
                Logger.writeLine(Log.ERROR, ex);
            } finally {
                if (response != null)
                    response.close();
            }
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
