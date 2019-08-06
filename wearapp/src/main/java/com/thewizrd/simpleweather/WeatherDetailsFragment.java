package com.thewizrd.simpleweather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.simpleweather.adapters.DetailItemAdapter;

public class WeatherDetailsFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView = null;

    private WearableRecyclerView recyclerView;
    // Weather Credit
    private TextView weatherCredit;

    public static WeatherDetailsFragment newInstance(WeatherNowViewModel weatherViewModel) {
        WeatherDetailsFragment fragment = new WeatherDetailsFragment();
        if (weatherViewModel != null) {
            fragment.weatherView = weatherViewModel;
        }

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_weather_details, (ViewGroup) outerView, true);

        recyclerView = view.findViewById(R.id.recycler_view);
        weatherCredit = view.findViewById(R.id.weather_credit);

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

            //weatherCredit.setText(weatherView.getWeatherCredit());

            recyclerView.setLayoutManager(new WearableLinearLayoutManager(mActivity));
            // specify an adapter (see also next example)
            RecyclerView.Adapter adapter = new DetailItemAdapter(weatherView);
            recyclerView.setAdapter(adapter);
        }
    }
}
