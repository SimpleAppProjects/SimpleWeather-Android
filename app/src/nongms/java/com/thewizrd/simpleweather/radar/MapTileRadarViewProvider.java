package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.thewizrd.shared_resources.utils.Coordinate;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class MapTileRadarViewProvider extends RadarViewProvider implements MapView.OnFirstLayoutListener {
    private MapView mapView;

    private Coordinate locationCoords;
    protected Marker locationMarker = null;

    private boolean isViewAlive = false;

    public MapTileRadarViewProvider(@NonNull Context context, @NonNull ViewGroup rootView) {
        super(context, rootView);
    }

    @Override
    public void updateCoordinates(@NonNull Coordinate coordinates, boolean updateView) {
        locationCoords = coordinates;
        if (updateView) updateRadarView();
    }

    @Override
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mapView = createMapView();
    }

    @Override
    @CallSuper
    public void onCreateView(@Nullable Bundle savedInstanceState) {
        mapView = createMapView();
        mapView.addOnFirstLayoutListener(this);
    }

    @Override
    public final void onFirstLayout(View v, int left, int top, int right, int bottom) {
        onMapReady();
    }

    @Override
    @CallSuper
    public void updateRadarView() {
        if (getMapView().isLayoutOccurred()) {
            onMapReady();
            getMapView().invalidate();
        }
    }

    protected abstract void onMapReady();

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
    public void onDestroyView() {
        isViewAlive = false;
        if (getViewContainer() != null) {
            mapView = null;
            getViewContainer().removeAllViews();
        }
    }

    @Override
    @CallSuper
    public void onDestroy() {
        mapView = null;
    }

    protected final boolean isViewAlive() {
        return isViewAlive;
    }

    protected final MapView getMapView() {
        return mapView;
    }

    @NonNull
    private MapView createMapView() {
        MapView mv = new MapView(getContext());
        mv.setMultiTouchControls(true);
        mv.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        if (locationCoords != null) {
            IMapController mapController = mv.getController();
            mapController.setZoom(6.0);
            mapController.setCenter(new GeoPoint(locationCoords.getLatitude(), locationCoords.getLongitude()));
        }

        mv.setTileSource(TileSourceFactory.MAPNIK);

        return new MapView(getContext());
    }

    @Nullable
    protected final IGeoPoint getMapCameraPosition() {
        if (locationCoords != null) {
            return new GeoPoint(locationCoords.getLatitude(), locationCoords.getLongitude());
        }

        return null;
    }
}
