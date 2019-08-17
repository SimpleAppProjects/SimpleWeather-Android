package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DarkMode;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.WindowColorsInterface;

public abstract class CustomPreferenceFragmentCompat extends PreferenceFragmentCompat
        implements OnBackPressedFragmentListener {

    protected Toolbar mToolbar;
    protected AppCompatActivity mActivity;
    protected WindowColorsInterface mWindowColorsIface;

    private Configuration prevConfig;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
        mWindowColorsIface = (WindowColorsInterface) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
        mWindowColorsIface = null;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    protected abstract @StringRes
    int getTitle();

    @Override
    public void onResume() {
        super.onResume();

        prevConfig = new Configuration(getResources().getConfiguration());

        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        int color = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
        if (currentNightMode > AppCompatDelegate.MODE_NIGHT_NO) {
            if (Settings.getUserThemeMode() == DarkMode.AMOLED_DARK) {
                color = Colors.BLACK;
            } else {
                color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
            }
        }
        if (mWindowColorsIface != null) {
            mWindowColorsIface.setWindowBarColors(color);
        }

        // Toolbar
        mToolbar.setTitle(getTitle());
        mToolbar.setBackgroundColor(color);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_settings, container, false);

        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        mToolbar = root.findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onBackPressed();
            }
        });

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        inflatedView.setLayoutParams(lp);

        root.addView(inflatedView);

        return root;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int diff = newConfig.diff(prevConfig);
        if ((diff & ActivityInfo.CONFIG_UI_MODE) != 0) {
            int currentNightMode = AppCompatDelegate.getDefaultNightMode();
            int color = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
            if (currentNightMode > AppCompatDelegate.MODE_NIGHT_NO) {
                if (Settings.getUserThemeMode() == DarkMode.AMOLED_DARK) {
                    color = Colors.BLACK;
                } else {
                    color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
                }
            }
            if (mWindowColorsIface != null) {
                mWindowColorsIface.setWindowBarColors(color);
            }
            mToolbar.setBackgroundColor(color);

            getListView().setAdapter(getListView().getAdapter());
        }

        prevConfig = new Configuration(newConfig);
    }
}
