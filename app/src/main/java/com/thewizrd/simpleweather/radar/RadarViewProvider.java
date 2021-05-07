package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.thewizrd.shared_resources.utils.Coordinate;

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
public abstract class RadarViewProvider {
    private final Context mContext;
    private final ViewGroup mRootLayout;

    private boolean enableInteractions = true;

    public RadarViewProvider(@NonNull Context context, @NonNull ViewGroup rootView) {
        this.mContext = context;
        this.mRootLayout = rootView;
    }

    @NonNull
    public final Context getContext() {
        return mContext;
    }

    public final ViewGroup getViewContainer() {
        return mRootLayout;
    }

    public abstract void updateCoordinates(@NonNull Coordinate coordinates, boolean updateView);

    public abstract void updateRadarView();

    public final void enableInteractions(boolean enable) {
        enableInteractions = enable;
    }

    public final boolean interactionsEnabled() {
        return enableInteractions;
    }

    /* Lifecycle Hooks */
    public void onCreate(@Nullable Bundle savedInstanceState) {
    }

    public void onCreateView(@Nullable Bundle savedInstanceState) {
    }

    public void onViewCreated(Coordinate coordinates) {
        updateCoordinates(coordinates, false);
    }

    public void onStart() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onStop() {
    }

    public void onDestroyView() {
    }

    public void onDestroy() {
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
    }

    public void onLowMemory() {
    }

    public void onConfigurationChanged() {
    }
}
