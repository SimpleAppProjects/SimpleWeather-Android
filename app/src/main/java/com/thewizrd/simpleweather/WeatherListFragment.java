package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DarkMode;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.WindowColorsInterface;

import java.io.StringReader;

public abstract class WeatherListFragment extends Fragment {
    protected LocationData location = null;
    protected WeatherNowViewModel weatherView = null;

    protected Toolbar toolbar;
    protected MaterialCardView locationHeader;
    protected TextView locationName;
    protected RecyclerView recyclerView;
    protected LinearLayoutManager layoutManager;

    protected AppCompatActivity mActivity;
    protected WindowColorsInterface mWindowColorsIface;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (location == null && savedInstanceState != null) {
            String json = savedInstanceState.getString("data", null);
            location = LocationData.fromJson(new JsonReader(new StringReader(json)));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View view = inflater.inflate(R.layout.fragment_weather_alerts, container, false);

        // Setup Actionbar
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity != null) mActivity.onBackPressed();
            }
        });

        locationHeader = view.findViewById(R.id.location_header);
        locationName = view.findViewById(R.id.location_name);
        recyclerView = view.findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(mActivity));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isHidden())
            return;
        else
            initialize();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && isVisible())
            initialize();
    }

    // Initialize views here
    @CallSuper
    protected void initialize() {
        int bg_color = Settings.getUserThemeMode() != DarkMode.AMOLED_DARK ?
                ActivityUtils.getColor(mActivity, android.R.attr.colorBackground) : Colors.BLACK;
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        int bar_color = currentNightMode <= AppCompatDelegate.MODE_NIGHT_NO ?
                ActivityUtils.getColor(mActivity, R.attr.colorPrimary) :
                bg_color;
        // Setup ActionBar
        if (mWindowColorsIface != null) {
            mWindowColorsIface.setWindowBarColors(bar_color);
        }
        getView().setBackgroundColor(bg_color);
        toolbar.setBackgroundColor(Settings.getUserThemeMode() == DarkMode.AMOLED_DARK ? Colors.BLACK : bar_color);
        locationHeader.setCardBackgroundColor(bg_color);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putString("data", location.toJson());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
        mWindowColorsIface = (WindowColorsInterface) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity = null;
        mWindowColorsIface = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
        mWindowColorsIface = null;
    }

    protected void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int bg_color = Settings.getUserThemeMode() != DarkMode.AMOLED_DARK ?
                ActivityUtils.getColor(mActivity, android.R.attr.colorBackground) : Colors.BLACK;
        int controlColor = ActivityUtils.getColor(mActivity, R.attr.colorControlNormal);
        int colorPrimary = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
        int txtColorPrimary = ActivityUtils.getColor(mActivity, android.R.attr.textColorPrimary);
        // Setup ActionBar
        if (mWindowColorsIface != null) {
            int currentNightMode = AppCompatDelegate.getDefaultNightMode();
            if (currentNightMode < AppCompatDelegate.MODE_NIGHT_NO) {
                mWindowColorsIface.setWindowBarColors(colorPrimary);
            } else {
                mWindowColorsIface.setWindowBarColors(bg_color);
            }
        }
        getView().setBackgroundColor(bg_color);
        toolbar.setBackgroundColor(Settings.getUserThemeMode() == DarkMode.AMOLED_DARK ? Colors.BLACK : colorPrimary);
        locationHeader.setCardBackgroundColor(bg_color);
        locationName.setTextColor(txtColorPrimary);

        recyclerView.setAdapter(recyclerView.getAdapter());
    }
}
