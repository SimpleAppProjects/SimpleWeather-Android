package com.thewizrd.simpleweather;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.wear.widget.SwipeDismissFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SwipeDismissPreferenceFragment extends PreferenceFragment {
    private SwipeDismissFrameLayout.Callback swipeCallback;
    private SwipeDismissFrameLayout swipeLayout;

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        swipeLayout = (SwipeDismissFrameLayout) inflater.inflate(R.layout.activity_settings, container, false);

        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);
        swipeLayout.addView(inflatedView);
        swipeLayout.setSwipeable(true);
        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                getActivity().onBackPressed();
            }
        };
        swipeLayout.addCallback(swipeCallback);

        return swipeLayout;
    }

    @Override
    public void onDestroyView() {
        swipeLayout.removeCallback(swipeCallback);
        super.onDestroyView();
    }
}
