package com.thewizrd.simpleweather.main;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;

import java.io.StringReader;

public abstract class WeatherListFragment extends ToolbarFragment {
    protected LocationData location = null;
    protected WeatherNowViewModel weatherView = null;

    protected MaterialCardView locationHeader;
    protected TextView locationName;
    protected RecyclerView recyclerView;
    protected LinearLayoutManager layoutManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (location == null && savedInstanceState != null) {
            String json = savedInstanceState.getString("data", null);
            location = LocationData.fromJson(new JsonReader(new StringReader(json)));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Use this to return your custom view for this Fragment
        View view = inflater.inflate(R.layout.fragment_weather_list, root, true);

        // Setup Actionbar
        getToolbar().setNavigationIcon(
                ActivityUtils.getResourceId(getAppCompatActivity(), R.attr.homeAsUpIndicator));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getAppCompatActivity() != null) getAppCompatActivity().onBackPressed();
            }
        });

        locationHeader = view.findViewById(R.id.location_header);
        locationName = view.findViewById(R.id.location_name);
        recyclerView = view.findViewById(R.id.recycler_view);
        locationHeader.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
                layoutParams.topMargin = locationHeader.getHeight();
                recyclerView.setLayoutParams(layoutParams);
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(getAppCompatActivity()));

        ViewCompat.setOnApplyWindowInsetsListener(locationHeader, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                locationHeader.setContentPadding(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

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
        updateWindowColors();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putString("data", location.toJson());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void updateWindowColors() {
        super.updateWindowColors();

        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        int bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES && Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            bg_color = Colors.BLACK;
        }
        locationHeader.setCardBackgroundColor(bg_color);
        recyclerView.setBackgroundColor(bg_color);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        recyclerView.setAdapter(recyclerView.getAdapter());
        updateWindowColors();
    }
}
