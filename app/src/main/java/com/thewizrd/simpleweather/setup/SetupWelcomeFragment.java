package com.thewizrd.simpleweather.setup;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.transition.MaterialSharedAxis;
import com.thewizrd.simpleweather.databinding.FragmentSetupWelcomeBinding;
import com.thewizrd.simpleweather.fragments.CustomFragment;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

public class SetupWelcomeFragment extends CustomFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentSetupWelcomeBinding.inflate(inflater, container, false).getRoot();
    }

    @Nullable
    @Override
    public SnackbarManager createSnackManager(@NonNull Activity activity) {
        return null;
    }
}
