package com.thewizrd.simpleweather;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wear.widget.SwipeDismissFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SwipeDismissFragment extends Fragment {
    private SwipeDismissFrameLayout.Callback swipeCallback;
    private SwipeDismissFrameLayout swipeLayout;

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        swipeLayout = (SwipeDismissFrameLayout) inflater.inflate(R.layout.activity_settings, container, false);

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

