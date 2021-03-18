package com.thewizrd.simpleweather.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding;

public class SwipeDismissFragment extends CustomFragment {
    private ActivitySettingsBinding binding;
    private SwipeDismissFrameLayout.Callback swipeCallback;

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = ActivitySettingsBinding.inflate(inflater, container, false);

        binding.swipeLayout.setSwipeable(true);
        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                if (getFragmentActivity() != null) getFragmentActivity().onBackPressed();
            }
        };
        binding.swipeLayout.addCallback(swipeCallback);

        return binding.swipeLayout;
    }

    @Override
    public void onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback);
        super.onDestroyView();
        binding = null;
    }
}

