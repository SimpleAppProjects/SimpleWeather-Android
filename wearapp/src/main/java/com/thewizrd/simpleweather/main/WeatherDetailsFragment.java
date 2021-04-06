package com.thewizrd.simpleweather.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.thewizrd.shared_resources.BR;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.simpleweather.adapters.DetailItemAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment;

public class WeatherDetailsFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView;

    private FragmentWeatherListBinding binding;
    private DetailItemAdapter mAdapter;

    public WeatherDetailsFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("WeatherDetails: onCreate");

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void load() {
                mAdapter.submitList(weatherView.getWeatherDetails());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWeatherListBinding.inflate(inflater, (ViewGroup) outerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);
        binding.recyclerView.setLayoutManager(new WearableLinearLayoutManager(getFragmentActivity()));

        binding.recyclerView.requestFocus();

        // specify an adapter (see also next example)
        mAdapter = new DetailItemAdapter();
        binding.recyclerView.setAdapter(mAdapter);

        return outerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        weatherView = new ViewModelProvider(getFragmentActivity()).get(WeatherNowViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("WeatherDetails: onResume");
        weatherView.addOnPropertyChangedCallback(propertyChangedCallback);
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherDetails: onPause");
        weatherView.removeOnPropertyChangedCallback(propertyChangedCallback);
        super.onPause();
    }

    private final Observable.OnPropertyChangedCallback propertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, final int propertyId) {
            runWithView(() -> {
                if (propertyId == BR.weatherDetails) {
                    mAdapter.submitList(weatherView.getWeatherDetails());
                }
            });
        }
    };
}
