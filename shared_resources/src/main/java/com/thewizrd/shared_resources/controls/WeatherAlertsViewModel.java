package com.thewizrd.shared_resources.controls;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeatherAlertsViewModel extends ObservableViewModel {
    private Handler mMainHandler;
    private String locationKey;

    private MutableLiveData<List<WeatherAlertViewModel>> alerts;

    private LiveData<List<WeatherAlertViewModel>> currentAlertsData;

    public WeatherAlertsViewModel() {
        alerts = new MutableLiveData<>();

        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<List<WeatherAlertViewModel>> getAlerts() {
        return alerts;
    }

    public void updateAlerts(@NonNull LocationData location) {
        if (!ObjectsCompat.equals(this.locationKey, location.getQuery())) {
            this.locationKey = location.getQuery();

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (currentAlertsData != null) {
                        currentAlertsData.removeObserver(alertObserver);
                    }
                    currentAlertsData = Transformations.map(Settings.getWeatherDAO().getLiveWeatherAlertData(locationKey),
                            new Function<WeatherAlerts, List<WeatherAlertViewModel>>() {
                                @Override
                                public List<WeatherAlertViewModel> apply(WeatherAlerts weatherAlerts) {
                                    List<WeatherAlertViewModel> alerts;

                                    if (weatherAlerts != null && weatherAlerts.getAlerts() != null && weatherAlerts.getAlerts().size() > 0) {
                                        alerts = new ArrayList<>(weatherAlerts.getAlerts().size());

                                        for (WeatherAlert alert : weatherAlerts.getAlerts()) {
                                            // Skip if alert has expired
                                            if (alert.getExpiresDate().compareTo(ZonedDateTime.now()) <= 0)
                                                continue;

                                            WeatherAlertViewModel alertView = new WeatherAlertViewModel(alert);
                                            alerts.add(alertView);
                                        }

                                        return alerts;
                                    }

                                    return Collections.emptyList();
                                }
                            });
                    currentAlertsData.observeForever(alertObserver);
                    if (alerts != null) {
                        alerts.setValue(currentAlertsData.getValue());
                    }
                }
            });
        }
    }

    private Observer<List<WeatherAlertViewModel>> alertObserver = new Observer<List<WeatherAlertViewModel>>() {
        @Override
        public void onChanged(List<WeatherAlertViewModel> alertViewModels) {
            if (alerts != null) {
                alerts.setValue(alertViewModels);
            }
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();

        locationKey = null;

        if (currentAlertsData != null)
            currentAlertsData.removeObserver(alertObserver);

        currentAlertsData = null;

        alerts = null;
    }
}
