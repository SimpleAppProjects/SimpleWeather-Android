package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.material.slider.Slider;
import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
public class RainViewerViewProvider extends MapTileRadarViewProvider {
    private final List<Long> availableTimestamps;
    private final Map<Long, TileOverlay> radarLayers;

    private GoogleMap googleMap;
    private RadarAnimateContainerBinding radarContainerBinding;

    private int animationPosition = 0;
    private final Handler mMainHandler;

    public RainViewerViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
        super(fragment, rootView);
        availableTimestamps = new ArrayList<>();
        radarLayers = new HashMap<>();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void updateRadarView() {
        if (getContext() == null) return;

        if (radarContainerBinding == null) {
            radarContainerBinding = RadarAnimateContainerBinding.inflate(LayoutInflater.from(getContext()));
            getViewContainer().addView(radarContainerBinding.getRoot());

            radarContainerBinding.playButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mMainHandler.post(animationRunnable);
                    } else {
                        mMainHandler.removeCallbacks(animationRunnable);
                    }
                }
            });

            radarContainerBinding.animationSeekbar.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                    if (fromUser) {
                        mMainHandler.removeCallbacks(animationRunnable);
                        showFrame((int) value);
                    }
                }
            });
            radarContainerBinding.animationSeekbar.setValue(0);
        }

        if (mapFragment == null) {
            mapFragment = createMapFragment();

            getParentFragment().getChildFragmentManager()
                    .beginTransaction()
                    .replace(radarContainerBinding.radarContainer.getId(), mapFragment)
                    .commit();
        }

        radarContainerBinding.radarToolbar.setVisibility(interactionsEnabled() ? View.VISIBLE : View.GONE);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        radarContainerBinding = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        final Configuration currentConfig = getContext().getResources().getConfiguration();
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        this.googleMap = googleMap;

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

        UiSettings mapUISettings = googleMap.getUiSettings();
        mapUISettings.setScrollGesturesEnabled(interactionsEnabled());

        getTimeStamps();
    }

    private void getTimeStamps() {
        OkHttpClient httpClient = SimpleLibrary.getInstance().getHttpClient();

        Request request = new Request.Builder()
                .get()
                .url(HttpUrl.parse("https://api.rainviewer.com/public/maps.json"))
                .build();

        // Connect to webstream
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.writeLine(Log.ERROR, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    final InputStream stream = response.body().byteStream();

                    // Load data
                    Type arrListType = new TypeToken<ArrayList<Long>>() {
                    }.getType();
                    List<Long> root = JSONParser.deserializer(stream, arrListType);

                    availableTimestamps.clear();
                    availableTimestamps.addAll(root);

                    for (Long key : radarLayers.keySet()) {
                        if (!availableTimestamps.contains(key)) {
                            TileOverlay overlay = radarLayers.get(key);
                            overlay.remove();
                        }
                    }

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isViewAlive()) {
                                showFrame(-1);
                            }
                        }
                    });

                    // End Stream
                    stream.close();
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex);
                } finally {
                    if (response != null)
                        response.close();
                }
            }
        });
    }

    private void addLayer(long ts) {
        if (!radarLayers.containsKey(ts)) {
            TileOverlay overlay = googleMap.addTileOverlay(
                    new TileOverlayOptions().tileProvider(new RainViewTileProvider(getContext(), ts))
                            .transparency(1f));
            radarLayers.put(ts, overlay);
        }

        radarContainerBinding.animationSeekbar.setStepSize(1);
        radarContainerBinding.animationSeekbar.setValueFrom(0);
        radarContainerBinding.animationSeekbar.setValueTo(availableTimestamps.size() - 1);
    }

    private void changeRadarPosition(int position) {
        changeRadarPosition(position, false);
    }

    private void changeRadarPosition(int position, boolean preloadOnly) {
        while (position >= availableTimestamps.size()) {
            position -= availableTimestamps.size();
        }
        while (position < 0) {
            position += availableTimestamps.size();
        }

        long currentTimeStamp = availableTimestamps.get(animationPosition);
        long nextTimeStamp = availableTimestamps.get(position);

        addLayer(nextTimeStamp);

        if (preloadOnly) {
            return;
        }

        animationPosition = position;

        if (radarLayers.containsKey(currentTimeStamp)) {
            TileOverlay currentOverlay = radarLayers.get(currentTimeStamp);
            currentOverlay.setTransparency(1);
        }
        TileOverlay nextOverlay = radarLayers.get(nextTimeStamp);
        nextOverlay.setTransparency(0);

        updateToolbar(position, nextTimeStamp);
    }

    private void updateToolbar(int position) {
        updateToolbar(position, availableTimestamps.get(position));
    }

    private void updateToolbar(int position, long timeStamp) {
        radarContainerBinding.animationSeekbar.setValue(position);

        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timeStamp), ZoneOffset.systemDefault());
        DateTimeFormatter fmt;
        if (DateFormat.is24HourFormat(getContext())) {
            fmt = DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR));
        } else {
            fmt = DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_MIN_AMPM);
        }
        radarContainerBinding.timestampText.setText(dateTime.format(fmt));
    }

    /**
     * Check avialability and show particular frame position from the timestamps list
     */
    private void showFrame(int nextPosition) {
        int preloadingDirection = nextPosition - animationPosition > 0 ? 1 : -1;

        changeRadarPosition(nextPosition);

        // preload next next frame (typically, +1 frame)
        // if don't do that, the animation will be blinking at the first loop
        changeRadarPosition(nextPosition + preloadingDirection, true);
    }

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isViewAlive()) {
                showFrame(animationPosition + 1);
                mMainHandler.postDelayed(this, 500);
            } else {
                mMainHandler.removeCallbacks(this);
            }
        }
    };

    private static class RainViewTileProvider extends CachingUrlTileProvider {
        private final long timestamp;

        public RainViewTileProvider(@NonNull Context context, long ts) {
            super(context, 256, 256);
            this.timestamp = ts;
        }

        @Override
        public String getTileUrl(int x, int y, int zoom) {
            if (!checkTileExists(x, y, zoom)) {
                return null;
            }

            if (timestamp > 0) {
                /* Define the URL pattern for the tile images */
                String s = String.format(Locale.ROOT, "https://tilecache.rainviewer.com/v2/radar/%d/256/%d/%d/%d/1/1_1.png", timestamp, zoom, x, y);
                return s;
            }

            return null;
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
