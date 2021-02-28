package com.thewizrd.simpleweather.radar.rainviewer;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.material.slider.Slider;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.thewizrd.extras.ExtrasLibrary;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding;
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider;
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider;
import com.thewizrd.simpleweather.stag.generated.Stag;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
    private final List<RadarFrame> availableRadarFrames;
    private final Map<Long, TileOverlay> radarLayers;

    private GoogleMap googleMap;
    private RadarAnimateContainerBinding radarContainerBinding;

    private int animationPosition = 0;
    private final Handler mMainHandler;

    private final Gson gson;

    public RainViewerViewProvider(@NonNull Context context, @NonNull ViewGroup rootView) {
        super(context, rootView);
        availableRadarFrames = new ArrayList<>();
        radarLayers = new HashMap<>();
        mMainHandler = new Handler(Looper.getMainLooper());

        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new Stag.Factory())
                .create();
    }

    @Override
    public void onCreateView(@Nullable Bundle savedInstanceState) {
        super.onCreateView(savedInstanceState);

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

        if (radarContainerBinding.radarContainer.getChildCount() == 0) {
            radarContainerBinding.radarContainer.addView(getMapView());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove animation callbacks
        radarContainerBinding.playButton.setChecked(false);
    }

    @Override
    public void onViewCreated(WeatherUtils.Coordinate coordinates) {
        super.onViewCreated(coordinates);
    }

    @Override
    public void updateRadarView() {
        radarContainerBinding.radarToolbar.setVisibility(interactionsEnabled() && ExtrasLibrary.Companion.isEnabled() ? View.VISIBLE : View.GONE);
        getMapView().getMapAsync(this);
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

        getRadarFrames();
    }

    private void getRadarFrames() {
        OkHttpClient httpClient = SimpleLibrary.getInstance().getHttpClient();

        Request request = new Request.Builder()
                .get()
                .url(HttpUrl.get("https://api.rainviewer.com/public/weather-maps.json"))
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
                    WeatherMapsResponse root = gson.fromJson(new JsonReader(new InputStreamReader(stream)), WeatherMapsResponse.class);

                    availableRadarFrames.clear();

                    if (root != null && root.getRadar() != null) {
                        if (root.getRadar().getPast() != null && !root.getRadar().getPast().isEmpty()) {
                            root.getRadar().getPast().removeAll(Collections.singleton(null));
                            availableRadarFrames.addAll(Collections2.transform(root.getRadar().getPast(), input ->
                                    new RadarFrame(input.getTime(), root.getHost(), input.getPath()))
                            );
                        }

                        if (root.getRadar().getNowcast() != null && !root.getRadar().getNowcast().isEmpty()) {
                            root.getRadar().getNowcast().removeAll(Collections.singleton(null));
                            availableRadarFrames.addAll(Collections2.transform(root.getRadar().getNowcast(), input ->
                                    new RadarFrame(input.getTime(), root.getHost(), input.getPath()))
                            );
                        }
                    }

                    // Remove already added tile overlays
                    Iterator<Map.Entry<Long, TileOverlay>> it = radarLayers.entrySet().iterator();
                    while (it.hasNext()) {
                        TileOverlay overlay = it.next().getValue();
                        if (overlay != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    overlay.remove();
                                }
                            });
                        }
                        it.remove();
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
                    response.close();
                }
            }
        });
    }

    private void addLayer(@NonNull RadarFrame mapFrame) {
        if (!radarLayers.containsKey(mapFrame.getTimeStamp())) {
            TileOverlay overlay = googleMap.addTileOverlay(
                    new TileOverlayOptions().tileProvider(new RainViewTileProvider(getContext(), mapFrame))
                            .transparency(1f));
            radarLayers.put(mapFrame.getTimeStamp(), overlay);
        }

        radarContainerBinding.animationSeekbar.setStepSize(1);
        radarContainerBinding.animationSeekbar.setValueFrom(0);
        radarContainerBinding.animationSeekbar.setValueTo(availableRadarFrames.size() - 1);
    }

    private void changeRadarPosition(int position) {
        changeRadarPosition(position, false);
    }

    private void changeRadarPosition(int position, boolean preloadOnly) {
        while (position >= availableRadarFrames.size()) {
            position -= availableRadarFrames.size();
        }
        while (position < 0) {
            position += availableRadarFrames.size();
        }

        if (availableRadarFrames.isEmpty() || animationPosition >= availableRadarFrames.size() || position >= availableRadarFrames.size()) {
            return;
        }

        final RadarFrame currentFrame = availableRadarFrames.get(animationPosition);
        long currentTimeStamp = currentFrame.getTimeStamp();

        final RadarFrame nextFrame = availableRadarFrames.get(position);
        long nextTimeStamp = nextFrame.getTimeStamp();

        addLayer(nextFrame);

        if (preloadOnly) {
            return;
        }

        animationPosition = position;

        // 0 is opaque; 1 is transparent
        if (radarLayers.containsKey(currentTimeStamp)) {
            TileOverlay currentOverlay = radarLayers.get(currentTimeStamp);
            if (currentOverlay != null) {
                currentOverlay.setTransparency(1);
            }
        }
        TileOverlay nextOverlay = radarLayers.get(nextTimeStamp);
        if (nextOverlay != null) {
            nextOverlay.setTransparency(0);
        }

        updateToolbar(position, nextFrame);
    }

    private void updateToolbar(int position) {
        updateToolbar(position, availableRadarFrames.get(position));
    }

    private void updateToolbar(int position, @NonNull RadarFrame mapFrame) {
        radarContainerBinding.animationSeekbar.setValue(position);

        ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(mapFrame.getTimeStamp()), ZoneOffset.systemDefault());
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
        private final RadarFrame mapFrame;

        public RainViewTileProvider(@NonNull Context context, @NonNull RadarFrame mapFrame) {
            super(context, 256, 256);
            this.mapFrame = mapFrame;
        }

        @Override
        public String getTileUrl(int x, int y, int zoom) {
            if (!checkTileExists(x, y, zoom)) {
                return null;
            }

            if (mapFrame != null) {
                /* Define the URL pattern for the tile images */
                return String.format(Locale.ROOT, "%s%s/256/%d/%d/%d/1/1_1.png", mapFrame.getHost(), mapFrame.getPath(), zoom, x, y);
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
