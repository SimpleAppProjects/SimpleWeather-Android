package com.thewizrd.simpleweather;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.helpers.WindowColorsInterface;

import java.io.StringReader;
import java.util.List;

public class WeatherAlertsFragment extends Fragment {
    private LocationData location = null;
    private WeatherNowViewModel weatherView = null;

    private Toolbar toolbar;
    private TextView locationHeader;
    private RecyclerView recyclerView;

    private AppCompatActivity mActivity;
    private WindowColorsInterface mWindowColorsIface;

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
                if (mActivity != null) mActivity.onBackPressed();
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mWindowColorsIface != null)
                        mWindowColorsIface.setWindowBarColors(Colors.SIMPLEBLUE);
                }
            });
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

        if (weatherView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    locationHeader.setText(weatherView.getLocation());
                    // use this setting to improve performance if you know that changes
                    // in content do not change the layout size of the RecyclerView
                    recyclerView.setHasFixedSize(true);
                    // use a linear layout manager
                    recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
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

    private void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }
}
