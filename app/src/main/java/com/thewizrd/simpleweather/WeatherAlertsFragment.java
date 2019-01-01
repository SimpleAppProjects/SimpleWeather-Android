package com.thewizrd.simpleweather;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;

import java.io.StringReader;
import java.util.List;

public class WeatherAlertsFragment extends Fragment {
    private AppCompatActivity appCompatActivity;
    private LocationData location = null;
    private WeatherNowViewModel weatherView = null;

    private Toolbar toolbar;
    private TextView locationHeader;
    private RecyclerView recyclerView;

    public static WeatherAlertsFragment newInstance(LocationData location) {
        WeatherAlertsFragment fragment = new WeatherAlertsFragment();
        if (location != null) {
            fragment.location = location;
        }
        return fragment;
    }

    public static WeatherAlertsFragment newInstance(LocationData location, WeatherNowViewModel weatherViewModel) {
        WeatherAlertsFragment fragment = new WeatherAlertsFragment();
        if (location != null && weatherViewModel != null) {
            fragment.location = location;
            fragment.weatherView = weatherViewModel;
        }
        return fragment;
    }

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
                if (appCompatActivity != null) appCompatActivity.onBackPressed();
            }
        });

        locationHeader = view.findViewById(R.id.location_name);
        recyclerView = view.findViewById(R.id.recycler_view);

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

    private void initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (appCompatActivity != null) {
                appCompatActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appCompatActivity.getWindow().setStatusBarColor(
                                ContextCompat.getColor(appCompatActivity, R.color.colorPrimaryDark));
                    }
                });
            }
        }

        if (weatherView == null) {
            if (location == null)
                location = Settings.getHomeData();

            Weather weather = Settings.getWeatherData(location.getQuery());
            if (weather != null && weather.isValid()) {
                weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));
                weatherView = new WeatherNowViewModel(weather);
            }
        }

        if (weatherView != null && appCompatActivity != null) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    locationHeader.setText(weatherView.getLocation());
                    // use this setting to improve performance if you know that changes
                    // in content do not change the layout size of the RecyclerView
                    recyclerView.setHasFixedSize(true);
                    // use a linear layout manager
                    recyclerView.setLayoutManager(new LinearLayoutManager(appCompatActivity));
                    // specify an adapter (see also next example)
                    List<WeatherAlertViewModel> alerts = null;
                    if (weatherView.getExtras() != null && weatherView.getExtras().getAlerts() != null)
                        alerts = weatherView.getExtras().getAlerts();
                    recyclerView.setAdapter(new WeatherAlertPanelAdapter(alerts));
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putString("data", location.toJson());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        appCompatActivity = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        appCompatActivity = null;
    }
}
