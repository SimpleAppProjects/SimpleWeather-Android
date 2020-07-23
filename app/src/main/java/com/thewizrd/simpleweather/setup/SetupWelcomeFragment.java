package com.thewizrd.simpleweather.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupWelcomeBinding;
import com.thewizrd.simpleweather.fragments.WindowColorFragment;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

public class SetupWelcomeFragment extends WindowColorFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return FragmentSetupWelcomeBinding.inflate(inflater, container, false).getRoot();
    }

    @Nullable
    @Override
    public SnackbarManager createSnackManager() {
        return null;
    }

    @Override
    public void updateWindowColors() {
        if (isAlive()) {
            if (getSysBarColorMgr() != null) {
                getSysBarColorMgr().setSystemBarColors(ContextCompat.getColor(getAppCompatActivity(), R.color.colorPrimaryBackground));
            }
        }
    }
}
