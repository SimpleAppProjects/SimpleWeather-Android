package com.thewizrd.simpleweather;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter;

public class WeatherListFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView = null;

    private WearableRecyclerView recyclerView;

    private WeatherListType weatherType;

    public static WeatherListFragment newInstance(WeatherListType weatherType, WeatherNowViewModel weatherViewModel) {
        WeatherListFragment fragment = new WeatherListFragment();
        if (weatherViewModel != null) {
            fragment.weatherView = weatherViewModel;
        }

        Bundle args = new Bundle();
        args.putInt("WeatherListType", weatherType.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_weather_list, (ViewGroup) outerView, true);

        recyclerView = view.findViewById(R.id.recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);

        recyclerView.requestFocus();

        return outerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (this.isHidden())
            return;
        else
            initialize();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            initialize();
        }
    }

    public void initialize() {
        if (weatherView != null && mActivity != null) {
            if (getView() != null)
                getView().setBackgroundColor(weatherView.getPendingBackground());
            // specify an adapter (see also next example)
            RecyclerView.Adapter adapter = null;

            if (getArguments() != null)
                weatherType = WeatherListType.valueOf(getArguments().getInt("WeatherListType", 0));

            switch (weatherType) {
                default:
                case FORECAST:
                    recyclerView.setLayoutManager(new WearableLinearLayoutManager(mActivity));
                    adapter = new ForecastItemAdapter(weatherView.getForecasts());
                    break;
                case HOURLYFORECAST:
                    recyclerView.setLayoutManager(new WearableLinearLayoutManager(mActivity));
                    adapter = new HourlyForecastItemAdapter(weatherView.getExtras().getHourlyForecast());
                    break;
                case ALERTS:
                    recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                    adapter = new WeatherAlertPanelAdapter(weatherView.getExtras().getAlerts());
                    break;
            }

            recyclerView.setAdapter(adapter);
        }
    }
}
