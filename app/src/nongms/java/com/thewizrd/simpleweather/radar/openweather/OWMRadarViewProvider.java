package com.thewizrd.simpleweather.radar.openweather;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class OWMRadarViewProvider extends MapTileRadarViewProvider {
    private TilesOverlay tilesOverlay;

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
    public void onMapReady() {
        super.onMapReady();

        IGeoPoint cameraPosition = getMapCameraPosition();
        if (cameraPosition != null) {
            if (interactionsEnabled()) {
                if (locationMarker == null) {
                    locationMarker = new Marker(getMapView());
                    locationMarker.setDefaultIcon();
                    getMapView().getOverlays().add(locationMarker);
                }

                locationMarker.setPosition(new GeoPoint(cameraPosition.getLatitude(), cameraPosition.getLongitude()));
            }
        }

        if (tilesOverlay == null) {
            MapTileProviderBase tileProvider = new MapTileProviderBasic(getContext(), new OWMTileProvider());
            tilesOverlay = new TilesOverlay(tileProvider, getContext(), false, false);
            getMapView().getOverlays().add(tilesOverlay);
        }

        getMapView().postInvalidate();
    }

    private static class OWMTileProvider extends XYTileSource {
        public OWMTileProvider() {
            super("OWM", DEFAULT_ZOOM_LEVEL, DEFAULT_ZOOM_LEVEL, 256, ".png",
                    new String[]{"https://tile.openweathermap.org/"});
        }

        @Override
        public String getTileURLString(long pMapTileIndex) {
            int zoom = MapTileIndex.getZoom(pMapTileIndex);
            int x = MapTileIndex.getX(pMapTileIndex);
            int y = MapTileIndex.getY(pMapTileIndex);

            /* Define the URL pattern for the tile images */
            return String.format(Locale.ROOT, "https://tile.openweathermap.org/map/precipitation_new/%d/%d/%d.png?appid=%s", zoom, x, y, Keys.getOWMKey());
        }
    }
}
