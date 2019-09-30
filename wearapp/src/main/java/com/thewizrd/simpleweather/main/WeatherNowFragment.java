package com.thewizrd.simpleweather.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.input.RotaryEncoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.stream.JsonReader;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.WeatherViewLoadedListener;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearConnectionStatus;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherErrorListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherLoadedListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.wearable.WeatherComplicationIntentService;

import org.threeten.bp.Duration;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherNowFragment extends Fragment implements WeatherLoadedListenerInterface,
        WeatherErrorListenerInterface, SharedPreferences.OnSharedPreferenceChangeListener {
    private LocationData location = null;
    private boolean loaded = false;

    private WeatherManager wm;
    private WeatherDataLoader wLoader = null;
    private WeatherNowViewModel weatherView = null;

    private FragmentActivity mActivity;
    private WeatherViewLoadedListener mCallback;
    private CancellationTokenSource cts;

    // Views
    private SwipeRefreshLayout refreshLayout;
    private NestedScrollView scrollView;

    // GPS location
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    // Data
    private BroadcastReceiver dataReceiver;
    private boolean receiverRegistered = false;
    // Timer for timing out of operations
    private Timer timer;
    private boolean timerEnabled;

    public WeatherNowFragment() {
        // Required empty public constructor
        wm = WeatherManager.getInstance();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param data Location for weather data
     * @return A new instance of fragment WeatherNowFragment.
     */
    public static WeatherNowFragment newInstance(LocationData data) {
        WeatherNowFragment fragment = new WeatherNowFragment();
        if (data != null) {
            Bundle args = new Bundle();
            args.putString(Constants.KEY_DATA, data.toJson());
            fragment.setArguments(args);
        }
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param args Bundle to pass to fragment
     * @return A new instance of fragment WeatherNowFragment.
     */
    public static WeatherNowFragment newInstance(Bundle args) {
        WeatherNowFragment fragment = new WeatherNowFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    public void onWeatherLoaded(final LocationData location, final Weather weather) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

                if (weather != null && weather.isValid()) {
                    weatherView.updateView(weather);

                    if (mCallback != null) mCallback.onWeatherViewUpdated(weatherView);

                    // Update complications if they haven't been already
                    WeatherComplicationIntentService.enqueueWork(mActivity,
                            new Intent(mActivity, WeatherComplicationIntentService.class)
                                    .setAction(WeatherComplicationIntentService.ACTION_UPDATECOMPLICATIONS));

                    if (!loaded) {
                        Duration span = Duration.between(ZonedDateTime.now(), weather.getUpdateTime()).abs();
                        if (Settings.getDataSync() != WearableDataSync.OFF && span.toMinutes() > Settings.getRefreshInterval()) {
                            // send request to refresh data on connected device
                            mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE)
                                    .putExtra(WearableDataListenerService.EXTRA_FORCEUPDATE, true));
                        }

                        loaded = true;
                    }
                }
            }
        });
    }

    @Override
    public void onWeatherError(final WeatherException wEx) {
        if (wEx != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isCtsCancelRequested()) {
                        if (wEx.getErrorStatus() == WeatherUtils.ErrorStatus.QUERYNOTFOUND && WeatherAPI.NWS.equals(Settings.getAPI())) {
                            Toast.makeText(mActivity, R.string.error_message_weather_us_only, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mActivity, wEx.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        if (getArguments() != null) {
            JsonReader jsonReader = new JsonReader(new StringReader(getArguments().getString(Constants.KEY_DATA, null)));
            location = LocationData.fromJson(jsonReader);
            try {
                jsonReader.close();
            } catch (IOException e) {
                Logger.writeLine(Log.ERROR, e);
            }

            if (location != null && wLoader == null)
                wLoader = new WeatherDataLoader(location, this, this);
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(mActivity);
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            if (isCtsCancelRequested())
                                return;

                            if (Settings.useFollowGPS() && updateLocation()) {
                                // Setup loader from updated location
                                wLoader = new WeatherDataLoader(WeatherNowFragment.this.location,
                                        WeatherNowFragment.this, WeatherNowFragment.this);

                                refreshWeather(false);
                            }

                            stopLocationUpdates();
                        }
                    });
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            if (isCtsCancelRequested())
                                return;

                            if (Settings.useFollowGPS() && updateLocation()) {
                                // Setup loader from updated location
                                wLoader = new WeatherDataLoader(WeatherNowFragment.this.location,
                                        WeatherNowFragment.this, WeatherNowFragment.this);

                                refreshWeather(false);
                            }
                        }
                    });
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
        }

        mRequestingLocationUpdates = false;

        dataReceiver = new BroadcastReceiver() {
            private boolean locationDataReceived = false;
            private boolean weatherDataReceived = false;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (isCtsCancelRequested())
                    return;

                if (WearableHelper.LocationPath.equals(intent.getAction()) || WearableHelper.WeatherPath.equals(intent.getAction())) {
                    if (WearableHelper.WeatherPath.equals(intent.getAction()) ||
                            (!loaded && location != null)) {
                        if (timerEnabled)
                            cancelTimer();

                        weatherDataReceived = true;
                    }

                    if (WearableHelper.LocationPath.equals(intent.getAction())) {
                        // We got the location data
                        location = Settings.getHomeData();
                        loaded = false;
                        locationDataReceived = true;
                    }

                    if (locationDataReceived && weatherDataReceived) {
                        // We got all our data; now load the weather
                        wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);
                        wLoader.forceLoadSavedWeatherData();

                        weatherDataReceived = false;
                        locationDataReceived = false;
                    }
                } else if (WearableHelper.ErrorPath.equals(intent.getAction())) {
                    // An error occurred; cancel the sync operation
                    weatherDataReceived = false;
                    locationDataReceived = false;
                    cancelDataSync();
                } else if (WearableHelper.IsSetupPath.equals(intent.getAction())) {
                    if (Settings.getDataSync() != WearableDataSync.OFF) {
                        boolean isDeviceSetup = intent.getBooleanExtra(WearableDataListenerService.EXTRA_DEVICESETUPSTATUS, false);
                        WearConnectionStatus connStatus = WearConnectionStatus.valueOf(intent.getIntExtra(WearableDataListenerService.EXTRA_CONNECTIONSTATUS, 0));

                        if (isDeviceSetup &&
                                connStatus == WearConnectionStatus.CONNECTED) {
                            // Device is setup and connected; proceed with sync
                            if (mActivity != null) {
                                mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_REQUESTSETTINGSUPDATE));
                                mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_REQUESTLOCATIONUPDATE));
                                mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE));
                            }

                            resetTimer();
                        } else {
                            // Device is not connected; cancel sync
                            weatherDataReceived = false;
                            locationDataReceived = false;
                            cancelDataSync();
                        }
                    }
                } else if (WearableHelper.SettingsPath.equals(intent.getAction()) && loaded && wLoader != null) {
                    // Refresh weather in case the unit changed
                    wLoader.forceLoadSavedWeatherData();
                }
            }
        };

        loaded = true;

        // Setup ViewModel
        weatherView = ViewModelProviders.of(this).get(WeatherNowViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentWeatherNowBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false);

        binding.setWeatherView(weatherView);
        binding.setLifecycleOwner(this);

        View view = binding.getRoot();
        view.setFocusableInTouchMode(true);
        view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {

                    // Don't forget the negation here
                    float delta = -RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(mActivity);

                    // Swap these axes if you want to do horizontal scrolling instead
                    scrollView.scrollBy(0, Math.round(delta));

                    return true;
                }

                return false;
            }
        });

        refreshLayout = (SwipeRefreshLayout) view;
        scrollView = view.findViewById(R.id.fragment_weather_now);

        // SwipeRefresh
        refreshLayout.setColorSchemeColors(Colors.SIMPLEBLUE);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        if (Settings.useFollowGPS() && updateLocation())
                            // Setup loader from updated location
                            wLoader = new WeatherDataLoader(WeatherNowFragment.this.location,
                                    WeatherNowFragment.this, WeatherNowFragment.this);

                        refreshWeather(true);
                    }
                });
            }
        });

        loaded = true;
        refreshLayout.setRefreshing(true);

        // Set property change listeners
        weatherView.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (!WeatherNowFragment.this.isHidden() && WeatherNowFragment.this.isVisible()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateWindowColors();
                            refreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });

        return view;
    }

    private void updateWindowColors() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

                int color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
                if (weatherView != null && weatherView.getPendingBackground() != -1) {
                    color = weatherView.getPendingBackground();
                }
                refreshLayout.setBackgroundColor(color);
            }
        });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "SetupLocationFragment: stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocCallback)
                .addOnCompleteListener(mActivity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mActivity = (FragmentActivity) context;
        mCallback = (WeatherViewLoadedListener) context;
        App.getInstance().getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActivity = null;
        mCallback = null;
        App.getInstance().getPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        // Cancel pending actions
        if (cts != null) cts.cancel();

        super.onDestroy();
        mActivity = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    // Use normal if sync is off
                    if (Settings.getDataSync() == WearableDataSync.OFF) {
                        resume();
                    } else {
                        dataSyncResume();
                    }
                }
            });
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            // Cancel pending actions
            if (cts != null) {
                cts.cancel();
                refreshLayout.setRefreshing(false);
            }
        }

        if (!hidden && weatherView != null && this.isVisible()) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    // Use normal if sync is off
                    if (Settings.getDataSync() == WearableDataSync.OFF) {
                        resume();
                    } else {
                        dataSyncResume();
                    }
                }
            });
        } else if (hidden) {
            loaded = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Cancel pending actions
        if (cts != null) {
            cts.cancel();
            refreshLayout.setRefreshing(false);
        }

        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(mActivity)
                    .unregisterReceiver(dataReceiver);
            receiverRegistered = false;
        }

        if (timerEnabled)
            cancelTimer();

        // Remove location updates to save battery.
        stopLocationUpdates();

        loaded = false;
    }

    private void restore() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                boolean forceRefresh = false;

                // GPS Follow location
                if (Settings.useFollowGPS() && (location == null || location.getLocationType() == LocationType.GPS)) {
                    LocationData locData = Settings.getLastGPSLocData();

                    if (locData == null) {
                        // Update location if not setup
                        updateLocation();
                        forceRefresh = true;
                    } else {
                        // Reset locdata if source is different
                        if (!Settings.getAPI().equals(locData.getWeatherSource()))
                            Settings.saveHomeData(new LocationData());

                        if (updateLocation()) {
                            // Setup loader from updated location
                            forceRefresh = true;
                        } else {
                            // Setup loader saved location data
                            location = locData;
                        }
                    }
                } else if (wLoader == null) {
                    // Weather was loaded before. Lets load it up...
                    location = Settings.getHomeData();
                }

                if (isCtsCancelRequested())
                    return;

                if (location != null)
                    wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);

                // Load up weather data
                refreshWeather(forceRefresh);
            }
        });
    }

    private void resume() {
        cts = new CancellationTokenSource();

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                CancellationToken ctsToken = cts.getToken();

                /* Update view on resume
                 * ex. If temperature unit changed
                 */
                // New Page = loaded - true
                // Navigating back to frag = !loaded - false
                if (loaded || wLoader == null) {
                    restore();
                    loaded = true;
                } else if (wLoader != null && !loaded) {
                    ULocale currentLocale = ULocale.forLocale(Locale.getDefault());
                    String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

                    // Reset if source || locale is different
                    if (!Settings.getAPI().equals(weatherView.getWeatherSource())
                            || wm.supportsWeatherLocale() && !locale.equals(weatherView.getWeatherLocale())) {
                        restore();
                        loaded = true;
                    } else if (wLoader.getWeather() != null && wLoader.getWeather().isValid()) {
                        Weather weather = wLoader.getWeather();

                        // Update weather if needed on resume
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = new WeatherDataLoader(WeatherNowFragment.this.location, WeatherNowFragment.this, WeatherNowFragment.this);
                            refreshWeather(false);
                            loaded = true;
                        } else {
                            // Check weather data expiration
                            Duration span = Duration.between(ZonedDateTime.now(), weather.getUpdateTime()).abs();
                            if (span.toMinutes() > Settings.getRefreshInterval()) {
                                refreshWeather(false);
                            } else {
                                if (ctsToken.isCancellationRequested())
                                    return;

                                weatherView.updateView(wLoader.getWeather());

                                if (mCallback != null) mCallback.onWeatherViewUpdated(weatherView);
                                loaded = true;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (StringUtils.isNullOrWhitespace(key))
            return;

        switch (key) {
            case Settings.KEY_DATASYNC:
                // If data sync settings changes,
                // reset so we can properly reload
                wLoader = null;
                location = null;
                break;
            default:
                break;
        }
    }

    private void dataSyncRestore() {
        if (mActivity != null) {
            // Send request to service to get weather data
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(true);
                }
            });
            mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                    .setAction(WearableDataListenerService.ACTION_REQUESTSETUPSTATUS));
        }

        // Start timeout timer
        resetTimer();
    }

    private void dataSyncResume() {
        cts = new CancellationTokenSource();

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                CancellationToken ctsToken = cts.getToken();

                if (mActivity == null) {
                    cancelDataSync();
                    return;
                }

                if (!receiverRegistered) {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(WearableHelper.SettingsPath);
                    filter.addAction(WearableHelper.LocationPath);
                    filter.addAction(WearableHelper.WeatherPath);
                    filter.addAction(WearableHelper.IsSetupPath);

                    LocalBroadcastManager.getInstance(mActivity)
                            .registerReceiver(dataReceiver, filter);
                    receiverRegistered = true;
                }

                /* Update view on resume
                 * ex. If temperature unit changed
                 */
                // New Page = loaded - true
                // Navigating back to frag = !loaded - false
                if (loaded || wLoader == null) {
                    dataSyncRestore();
                } else if (wLoader != null && !loaded) {
                    if (wLoader.getWeather() != null && wLoader.getWeather().isValid()) {
                        Weather weather = wLoader.getWeather();

                        /*
                            DateTime < 0 - This instance is earlier than value.
                            DateTime == 0 - This instance is the same as value.
                            DateTime > 0 - This instance is later than value.
                        */
                        if (Settings.getUpdateTime().compareTo(
                                weather.getUpdateTime().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()) > 0) {
                            // Data was updated while we we're away; loaded it up
                            if (location == null || !location.equals(Settings.getHomeData()))
                                location = Settings.getHomeData();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshLayout.setRefreshing(true);
                                }
                            });

                            wLoader = new WeatherDataLoader(WeatherNowFragment.this.location, WeatherNowFragment.this, WeatherNowFragment.this);
                            wLoader.forceLoadSavedWeatherData();
                        } else {
                            // Check weather data expiration
                            Duration span = Duration.between(ZonedDateTime.now(), weather.getUpdateTime()).abs();
                            if (span.toMinutes() > Settings.getRefreshInterval() && mActivity != null) {
                                // send request to refresh data on connected device
                                mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE)
                                        .putExtra(WearableDataListenerService.EXTRA_FORCEUPDATE, true));
                            }

                            if (ctsToken.isCancellationRequested())
                                return;

                            weatherView.updateView(wLoader.getWeather());

                            if (mCallback != null) mCallback.onWeatherViewUpdated(weatherView);
                            loaded = true;
                        }
                    } else {
                        // Data is null; restore
                        dataSyncRestore();
                    }
                }
            }
        });
    }

    private void cancelDataSync() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (timerEnabled)
                    cancelTimer();

                if (Settings.getDataSync() == WearableDataSync.DEVICEONLY) {
                    if (location == null && Settings.getHomeData() != null) {
                        // Load whatever we have available
                        location = Settings.getHomeData();
                        wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);
                        wLoader.forceLoadSavedWeatherData();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mActivity, R.string.werror_noweather, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        });
    }

    private void resetTimer() {
        if (timerEnabled)
            cancelTimer();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // We hit the interval
                // Data syncing is taking a long time to setup
                // Stop and load saved data
                cancelDataSync();
            }
        }, 30000); // 30sec
        timerEnabled = true;
    }

    private void cancelTimer() {
        timer.cancel();
        timer.purge();
        timerEnabled = false;
    }

    private void refreshWeather(final boolean forceRefresh) {
        if (mActivity != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(true);
                }
            });
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    if (isCtsCancelRequested())
                        return;

                    if (Settings.getDataSync() == WearableDataSync.OFF && wLoader != null)
                        wLoader.loadWeatherData(forceRefresh);
                    else
                        dataSyncResume();
                }
            });
        }
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean locationChanged = false;

                if (Settings.getDataSync() == WearableDataSync.OFF &&
                        Settings.useFollowGPS() && (location == null || location.getLocationType() == LocationType.GPS)) {
                    if (mActivity != null && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
                        return false;
                    }

                    Location location = null;

                    if (isCtsCancelRequested())
                        return false;

                    LocationManager locMan = null;
                    if (mActivity != null)
                        locMan = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mActivity, R.string.error_enable_location_services, Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Disable GPS feature if location is not enabled
                        Settings.setFollowGPS(false);
                        WeatherNowFragment.this.location = Settings.getHomeData();
                        return false;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() throws Exception {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 10, TimeUnit.SECONDS);
                                } catch (TimeoutException e) {
                                    Logger.writeLine(Log.ERROR, e);
                                }
                                return result;
                            }
                        });

                        /**
                         * Request start of location updates. Does nothing if
                         * updates have already been requested.
                         */
                        if (location == null && !mRequestingLocationUpdates) {
                            final LocationRequest mLocationRequest = new LocationRequest();
                            mLocationRequest.setNumUpdates(1);
                            mLocationRequest.setInterval(10000);
                            mLocationRequest.setFastestInterval(1000);
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            mRequestingLocationUpdates = true;
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocCallback, Looper.getMainLooper());
                        }
                    } else {
                        boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                        if (isGPSEnabled || isNetEnabled) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);

                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);

                            if (location == null)
                                locMan.requestSingleUpdate(provider, mLocListnr, Looper.getMainLooper());
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    if (location != null && !mRequestingLocationUpdates) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                mLocation != null && ConversionMethods.calculateGeopositionDistance(mLocation, location) < 1600) {
                            return false;
                        }

                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
                        }

                        LocationQueryViewModel view = null;

                        if (isCtsCancelRequested())
                            return false;

                        view = wm.getLocation(location);
                        if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (isCtsCancelRequested())
                            return false;

                        // Save location as last known
                        lastGPSLocData.setData(view, location);
                        Settings.saveHomeData(lastGPSLocData);

                        WeatherNowFragment.this.location = lastGPSLocData;
                        mLocation = location;
                        locationChanged = true;
                    }
                }

                return locationChanged;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    // Do the task you need to do.
                    //FetchGeoLocation();
                    updateLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Settings.setFollowGPS(false);
                    Toast.makeText(mActivity, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                break;
        }
    }
}
