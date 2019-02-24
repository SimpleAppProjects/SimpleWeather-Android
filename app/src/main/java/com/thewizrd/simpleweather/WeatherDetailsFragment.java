package com.thewizrd.simpleweather;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter;
import com.thewizrd.simpleweather.helpers.WindowColorsInterface;

import java.io.StringReader;

public class WeatherDetailsFragment extends Fragment {
    private LocationData location = null;
    private WeatherNowViewModel weatherView = null;
    private boolean isHourly = false;

    private Toolbar toolbar;
    private TextView locationHeader;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private AppCompatActivity mActivity;
    private WindowColorsInterface mWindowColorsIface;

    public static WeatherDetailsFragment newInstance(LocationData location, WeatherNowViewModel weatherViewModel, boolean isHourly) {
        WeatherDetailsFragment fragment = new WeatherDetailsFragment();
        if (location != null && weatherViewModel != null) {
            fragment.location = location;
            fragment.weatherView = weatherViewModel;
            fragment.isHourly = isHourly;
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (location == null) {
                String json = savedInstanceState.getString("data", null);
                location = LocationData.fromJson(new JsonReader(new StringReader(json)));
            }

            isHourly = savedInstanceState.getBoolean("isHourly", false);
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
        toolbar.setTitle(R.string.label_forecast);

        locationHeader = view.findViewById(R.id.location_name);
        recyclerView = view.findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(mActivity));
        recyclerView.addItemDecoration(new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL));

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
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mWindowColorsIface != null)
                            mWindowColorsIface.setWindowBarColors(Colors.SIMPLEBLUE);
                    }
                });
            }
        }

        if (weatherView == null) {
            if (location == null)
                location = Settings.getHomeData();

            Weather weather = Settings.getWeatherData(location.getQuery());
            if (weather != null && weather.isValid()) {
                weatherView = new WeatherNowViewModel(weather);
            }
        }

        if (weatherView != null && mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    locationHeader.setText(weatherView.getLocation());
                    // specify an adapter (see also next example)
                    if (isHourly && weatherView.getExtras().getHourlyForecast().size() > 0)
                        recyclerView.setAdapter(new WeatherDetailsAdapter(weatherView.getExtras().getHourlyForecast()));
                    else if (!isHourly) {
                        recyclerView.setAdapter(new WeatherDetailsAdapter(weatherView.getForecasts()));
                    }

                    if (getArguments() != null) {
                        int scrollToPosition = getArguments().getInt("position", 0);
                        layoutManager.scrollToPosition(scrollToPosition);
                    }
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putString("data", location.toJson());
        outState.putBoolean("isHourly", isHourly);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
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
}
