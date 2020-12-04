package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.thewizrd.shared_resources.utils.WeatherUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class MapTileRadarViewProvider extends RadarViewProvider implements OnMapReadyCallback {
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private MapView mapView;

    private WeatherUtils.Coordinate locationCoords;
    protected Marker locationMarker = null;

    private boolean isViewAlive = false;

    public MapTileRadarViewProvider(@NonNull Context context, @NonNull ViewGroup rootView) {
        super(context, rootView);
    }

    @Override
    public void updateCoordinates(@NonNull WeatherUtils.Coordinate coordinates, boolean updateView) {
        locationCoords = coordinates;
        if (updateView) updateRadarView();
    }

    @Override
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView = createMapView();
        mapView.onCreate(mapViewBundle);
    }

    @Override
    @CallSuper
    public void onCreateView(@Nullable Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView = createMapView();
        mapView.onCreate(mapViewBundle);
    }

    @Override
    @CallSuper
    public void onStart() {
        mapView.onStart();
    }

    @Override
    @CallSuper
    public void onResume() {
        isViewAlive = true;
        mapView.onResume();
    }

    @Override
    @CallSuper
    public void onPause() {
        mapView.onPause();
        isViewAlive = false;
    }

    @Override
    @CallSuper
    public void onStop() {
        mapView.onStop();
    }

    @Override
    @CallSuper
    public void onDestroyView() {
        isViewAlive = false;
        if (getViewContainer() != null) {
            mapView.onDestroy();
            mapView = null;
            getViewContainer().removeAllViews();
        }
    }

    @Override
    @CallSuper
    public void onDestroy() {
        mapView.onDestroy();
        mapView = null;
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
        }
        mapView.onSaveInstanceState(mapViewBundle);
        outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
    }

    @Override
    @CallSuper
    public void onLowMemory() {
        mapView.onLowMemory();
    }

    protected final boolean isViewAlive() {
        return isViewAlive;
    }

    protected final MapView getMapView() {
        return mapView;
    }

    private MapView createMapView() {
        GoogleMapOptions mapOptions = new GoogleMapOptions()
                .compassEnabled(false)
                .mapToolbarEnabled(false)
                .mapType(GoogleMap.MAP_TYPE_NORMAL)
                .rotateGesturesEnabled(false)
                .zoomControlsEnabled(false)
                .zoomGesturesEnabled(false)
                .tiltGesturesEnabled(false);

        if (locationCoords != null) {
            CameraPosition cameraPosition = CameraPosition.builder()
                    .target(new LatLng(locationCoords.getLatitude(), locationCoords.getLongitude()))
                    .zoom(6)
                    .build();
            mapOptions.camera(cameraPosition);
        }

        return new MapView(getContext(), mapOptions);
    }

    protected final CameraPosition getMapCameraPosition() {
        if (locationCoords != null) {
            return CameraPosition.builder()
                    .target(new LatLng(locationCoords.getLatitude(), locationCoords.getLongitude()))
                    .zoom(6)
                    .build();
        }

        return null;
    }
}
