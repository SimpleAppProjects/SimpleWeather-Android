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

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public abstract class MapTileRadarViewProvider extends RadarViewProvider implements OnMapReadyCallback {
    private SupportMapFragment mapFragment;
    private WeatherUtils.Coordinate locationCoords;
    protected Marker locationMarker = null;
    protected TileProvider tileProvider = null;

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
    public void onDestroyView() {
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

    private SupportMapFragment createMapFragment() {
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

    protected CameraPosition getMapCameraPosition() {
        if (locationCoords != null) {
            return CameraPosition.builder()
                    .target(new LatLng(locationCoords.getLatitude(), locationCoords.getLongitude()))
                    .zoom(6)
                    .build();
        }

        return null;
    }
}
