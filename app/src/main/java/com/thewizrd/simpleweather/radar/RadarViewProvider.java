package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.os.Build;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.thewizrd.shared_resources.utils.WeatherUtils;

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
public abstract class RadarViewProvider {
    private final Fragment fragment;
    private final ViewGroup mRootLayout;

    private boolean enableInteractions = true;

    public RadarViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
        this.fragment = fragment;
        this.mRootLayout = rootView;
    }

    public final Fragment getParentFragment() {
        return fragment;
    }

    @Nullable
    public final Context getContext() {
        return fragment.getContext();
    }

    public final ViewGroup getViewContainer() {
        return mRootLayout;
    }

    public abstract void updateCoordinates(@NonNull WeatherUtils.Coordinate coordinates, boolean updateView);

    public abstract void updateRadarView();

    public void onViewCreated(WeatherUtils.Coordinate coordinates) {
        updateCoordinates(coordinates, false);
    }

    public final void enableInteractions(boolean enable) {
        enableInteractions = enable;
    }

    public final boolean interactionsEnabled() {
        return enableInteractions;
    }

    public void onDestroyView() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onConfigurationChanged() {
    }
}
