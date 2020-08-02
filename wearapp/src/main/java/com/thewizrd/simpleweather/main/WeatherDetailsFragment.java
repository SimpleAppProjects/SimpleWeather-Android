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
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("WeatherDetails: onCreate");

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void load() {
                initialize();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWeatherListBinding.inflate(inflater, (ViewGroup) outerView, true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);
        binding.recyclerView.setLayoutManager(new WearableLinearLayoutManager(getFragmentActivity()));

        binding.recyclerView.requestFocus();

        return outerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.weatherView = new ViewModelProvider(getFragmentActivity()).get(WeatherNowViewModel.class);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
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

    private Observable.OnPropertyChangedCallback propertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, final int propertyId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (propertyId == BR.weatherDetails) {
                        initialize();
                    }
                }
            });
        }
    };

    public void initialize() {
        if (isAlive() && weatherView != null && getView() != null) {
            binding.recyclerView.requestFocus();

            // specify an adapter (see also next example)
            if (mAdapter == null) {
                mAdapter = new DetailItemAdapter();
                binding.recyclerView.setAdapter(mAdapter);
            }
            mAdapter.updateItems(weatherView.getWeatherDetails());
        }
    }
}
