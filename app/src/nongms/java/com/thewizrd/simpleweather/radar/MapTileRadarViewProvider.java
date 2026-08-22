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
import com.thewizrd.simpleweather.extras.ExtrasKt;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class MapTileRadarViewProvider extends RadarViewProvider implements MapView.OnFirstLayoutListener {
    private MapView mapView;
    protected static final int MIN_ZOOM_LEVEL = 2;
    protected static final int MAX_ZOOM_LEVEL = 18;
    protected static final int DEFAULT_ZOOM_LEVEL = 8;

    private Coordinate locationCoords;
    protected Marker locationMarker = null;

    private boolean isViewAlive = false;

    public MapTileRadarViewProvider(@NonNull Context context, @NonNull ViewGroup rootView) {
        super(context, rootView);

        OSMUtilsKt.initializeMap(context);
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
            getMapView().postInvalidate();
        }
    }

    @CallSuper
    protected void onMapReady() {
        android.content.res.Configuration currentConfig = getContext().getResources().getConfiguration();
        int systemNightMode = currentConfig.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = systemNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        getMapView().getMapOverlay().setColorFilter(isNightMode ? TilesOverlay.INVERT_COLORS : null);

        IGeoPoint mapCameraPosition = getMapCameraPosition();
        if (mapCameraPosition != null) {
            mapView.getController().setCenter(mapCameraPosition);
            mapView.getController().setZoom((double) DEFAULT_ZOOM_LEVEL);
        }

        mapView.setMultiTouchControls(interactionsEnabled() && ExtrasKt.isRadarInteractionEnabled());
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
        mv.setMultiTouchControls(false);
        mv.setHorizontalMapRepetitionEnabled(false);
        mv.setVerticalMapRepetitionEnabled(false);
        mv.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mv.setMinZoomLevel((double) MIN_ZOOM_LEVEL);
        mv.setMaxZoomLevel((double) MAX_ZOOM_LEVEL);

        if (locationCoords != null) {
            IMapController mapController = mv.getController();
            mapController.setZoom((double) DEFAULT_ZOOM_LEVEL);
            mapController.setCenter(new GeoPoint(locationCoords.getLatitude(), locationCoords.getLongitude()));
        }

        mv.setTileSource(TileSourceFactory.USGS_TOPO);

        // Remove any stale additional http request properties
        Configuration.getInstance().getAdditionalHttpRequestProperties().clear();

        configureMapView(mv);

        return mv;
    }

    @CallSuper
    protected void configureMapView(@NonNull MapView mapView) {}

    @Nullable
    protected final IGeoPoint getMapCameraPosition() {
        if (locationCoords != null) {
            return new GeoPoint(locationCoords.getLatitude(), locationCoords.getLongitude());
        }

        return null;
    }
}
