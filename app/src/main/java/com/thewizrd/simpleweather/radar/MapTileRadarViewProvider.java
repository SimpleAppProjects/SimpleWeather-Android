package com.thewizrd.simpleweather.radar;

import android.os.Build;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileProvider;
import com.thewizrd.shared_resources.utils.WeatherUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class MapTileRadarViewProvider extends RadarViewProvider implements OnMapReadyCallback {
    protected SupportMapFragment mapFragment;
    private WeatherUtils.Coordinate locationCoords;
    protected Marker locationMarker = null;
    protected TileProvider tileProvider = null;

    private boolean isViewAlive = false;

    public MapTileRadarViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
        super(fragment, rootView);
    }

    @Override
    public void updateCoordinates(@NonNull WeatherUtils.Coordinate coordinates, boolean updateView) {
        locationCoords = coordinates;
        if (updateView) updateRadarView();
    }

    @Override
    public void updateRadarView() {
        if (mapFragment == null) {
            mapFragment = createMapFragment();

            getParentFragment().getChildFragmentManager()
                    .beginTransaction()
                    .replace(getViewContainer().getId(), mapFragment)
                    .commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        isViewAlive = true;
    }

    @Override
    public void onPause() {
        isViewAlive = false;
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        isViewAlive = false;
        if (getViewContainer() != null) {
            if (mapFragment != null) {
                getParentFragment().getChildFragmentManager()
                        .beginTransaction()
                        .remove(mapFragment)
                        .commitAllowingStateLoss();
            }
            getViewContainer().removeAllViews();
        }
    }

    protected final boolean isViewAlive() {
        return isViewAlive;
    }

    protected final SupportMapFragment createMapFragment() {
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
            mapOptions = mapOptions.camera(cameraPosition);
        }

        return SupportMapFragment.newInstance(mapOptions);
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
