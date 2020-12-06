package com.thewizrd.simpleweather.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.util.ObjectsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.wear.widget.drawer.WearableActionDrawerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.ForecastsViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.shared_resources.weatherdata.WeatherResult;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel;
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding;
import com.thewizrd.simpleweather.fragments.CustomFragment;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.wearable.WearableWorker;
import com.thewizrd.simpleweather.wearable.WeatherComplicationWorker;
import com.thewizrd.simpleweather.wearable.WeatherTileWorker;

import org.threeten.bp.Duration;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WeatherNowFragment extends CustomFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener, WeatherRequest.WeatherErrorListener {
    private WeatherNowFragmentArgs args;

    private WeatherManager wm;
    private WeatherDataLoader wLoader = null;

    // Views
    private FragmentWeatherNowBinding binding;
    private WearableActionDrawerView mDrawerView;

    // Data
    private LocationData locationData;
    private MutableLiveData<Weather> weatherLiveData;

    // View Models
    private WeatherNowViewModel weatherView;
    private ForecastsViewModel forecastsView;
    private ForecastPanelsViewModel forecastPanelsView;
    private WeatherAlertsViewModel alertsView;

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

    // Data sync
    private BroadcastReceiver syncDataReceiver;
    private boolean syncReceiverRegistered = false;
    // Timer for timing out of operations
    private Timer syncTimer;
    private boolean syncTimerEnabled;

    public WeatherNowFragment() {
        wm = WeatherManager.getInstance();
        setArguments(new Bundle());
    }

    private final Observer<Weather> weatherObserver = new Observer<Weather>() {
        @Override
        public void onChanged(final Weather weather) {
            if (weather != null && weather.isValid()) {
                weatherView.updateView(weather);

                if (locationData != null) {
                    forecastPanelsView.updateForecasts(locationData);
                    forecastsView.updateForecasts(locationData);
                }

                binding.swipeRefreshLayout.setRefreshing(false);

                Context context = App.getInstance().getAppContext();

                Duration span = Duration.between(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime(), Settings.getUpdateTime());
                if (Settings.getDataSync() != WearableDataSync.OFF && span.toMinutes() > Settings.DEFAULTINTERVAL) {
                    WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
                } else {
                    // Update complications if they haven't been already
                    WeatherComplicationWorker.enqueueAction(context, new Intent(WeatherComplicationWorker.ACTION_UPDATECOMPLICATIONS));

                    // Update tile if it hasn't been already
                    WeatherTileWorker.enqueueAction(context, new Intent(WeatherTileWorker.ACTION_UPDATETILES));
                }
            }
        }
    };

    public void onWeatherError(final WeatherException wEx) {
        runWithView(new Runnable() {
            @Override
            public void run() {
                if (wEx != null) {
                    if (wEx.getErrorStatus() == WeatherUtils.ErrorStatus.QUERYNOTFOUND && WeatherAPI.NWS.equals(Settings.getAPI())) {
                        Toast.makeText(getFragmentActivity(), R.string.error_message_weather_us_only, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getFragmentActivity(), wEx.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        App.getInstance().getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        wLoader = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        App.getInstance().getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        wLoader = null;
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (locationData != null) {
            outState.putString(Constants.KEY_DATA, JSONParser.serializer(locationData, LocationData.class));
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("WeatherNowFragment: onCreate");

        args = WeatherNowFragmentArgs.fromBundle(requireArguments());

        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_DATA)) {
            locationData = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData.class);
        } else if (args.getData() != null) {
            locationData = JSONParser.deserializer(args.getData(), LocationData.class);
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getFragmentActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            if (Settings.useFollowGPS() && updateLocation()) {
                                // Setup loader from updated location
                                wLoader = new WeatherDataLoader(locationData);

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
                public void onLocationChanged(final Location location) {
                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            if (Settings.useFollowGPS() && updateLocation()) {
                                // Setup loader from updated location
                                wLoader = new WeatherDataLoader(WeatherNowFragment.this.locationData);

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

        syncDataReceiver = new BroadcastReceiver() {
            private boolean locationDataReceived = false;
            private boolean weatherDataReceived = false;

            @Override
            public void onReceive(final Context context, final Intent intent) {
                runWithView(new Runnable() {
                    @Override
                    public void run() {
                        if (WearableHelper.LocationPath.equals(intent.getAction()) || WearableHelper.WeatherPath.equals(intent.getAction())) {
                            if (WearableHelper.WeatherPath.equals(intent.getAction())) {
                                weatherDataReceived = true;
                            }

                            if (WearableHelper.LocationPath.equals(intent.getAction())) {
                                // We got the location data
                                locationData = Settings.getHomeData();
                                locationDataReceived = true;
                            }

                            Log.d("SyncDataReceiver", "Action: " + intent.getAction());

                            if (locationDataReceived && weatherDataReceived || (weatherDataReceived && locationData != null)) {
                                if (syncTimerEnabled)
                                    cancelTimer();

                                Log.d("SyncDataReceiver", "Loading data...");

                                // We got all our data; now load the weather
                                if (!binding.swipeRefreshLayout.isRefreshing()) {
                                    binding.swipeRefreshLayout.setRefreshing(true);
                                }
                                wLoader = new WeatherDataLoader(locationData);
                                wLoader.loadWeatherData(new WeatherRequest.Builder()
                                        .forceLoadSavedData()
                                        .loadAlerts()
                                        .setErrorListener(WeatherNowFragment.this)
                                        .build())
                                        .addOnSuccessListener(new OnSuccessListener<Weather>() {
                                            @Override
                                            public void onSuccess(final Weather weather) {
                                                weatherLiveData.setValue(weather);
                                            }
                                        });

                                weatherDataReceived = false;
                                locationDataReceived = false;
                            }
                        } else if (WearableHelper.ErrorPath.equals(intent.getAction())) {
                            // An error occurred; cancel the sync operation
                            weatherDataReceived = false;
                            locationDataReceived = false;
                            cancelDataSync();
                        }
                    }
                });
            }
        };

        // Setup ViewModel
        ViewModelProvider vmProvider = new ViewModelProvider(getFragmentActivity());
        weatherView = vmProvider.get(WeatherNowViewModel.class);
        forecastsView = vmProvider.get(ForecastsViewModel.class);
        forecastPanelsView = vmProvider.get(ForecastPanelsViewModel.class);
        alertsView = vmProvider.get(WeatherAlertsViewModel.class);

        // Live Data
        weatherLiveData = new MutableLiveData<>();
        weatherLiveData.observe(this, weatherObserver);

        getLifecycle().addObserver(new LifecycleObserver() {
            private boolean wasStarted = false;

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void onStart() {
                // Use normal if sync is off
                if (Settings.getDataSync() == WearableDataSync.OFF) {
                    resume();
                } else {
                    dataSyncResume();
                }

                wasStarted = true;
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            private void onResume() {
                if (!wasStarted) onStart();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            private void onPause() {
                wasStarted = false;
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false);

        binding.setWeatherView(weatherView);
        binding.setAlertsView(alertsView);
        binding.setForecastsView(forecastPanelsView);
        binding.setLifecycleOwner(this);

        View view = binding.getRoot();

        mDrawerView = getFragmentActivity().findViewById(R.id.bottom_action_drawer);

        // SwipeRefresh
        binding.swipeRefreshLayout.setColorSchemeColors(ActivityUtils.getColor(getFragmentActivity(), R.attr.colorPrimary));
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AnalyticsLogger.logEvent("WeatherNowFragment: onRefresh");

                if (Settings.useFollowGPS() && updateLocation())
                    // Setup loader from updated location
                    wLoader = new WeatherDataLoader(locationData);

                refreshWeather(true);
            }
        });

        binding.scrollView.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {

                    // Don't forget the negation here
                    float delta = -RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(getFragmentActivity());

                    // Swap these axes if you want to do horizontal scrolling instead
                    v.scrollBy(0, Math.round(delta));

                    return true;
                }

                return false;
            }
        });

        binding.alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(WeatherNowFragmentDirections.actionGlobalWeatherAlertsFragment());
            }
        });

        binding.conditionDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(WeatherNowFragmentDirections.actionGlobalWeatherDetailsFragment());
            }
        });

        binding.forecastContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(WeatherNowFragmentDirections.actionGlobalWeatherForecastFragment());
            }
        });

        binding.hourlyForecastContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(WeatherNowFragmentDirections.actionGlobalWeatherHrForecastFragment());
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        args = WeatherNowFragmentArgs.fromBundle(requireArguments());

        binding.swipeRefreshLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            /* BoxInsetLayout impl */
            private static final float FACTOR = 0.146447f; //(1 - sqrt(2)/2)/2
            private final boolean mIsRound = getResources().getConfiguration().isScreenRound();

            @Override
            public boolean onPreDraw() {
                if (isViewAlive()) {
                    binding.swipeRefreshLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                    binding.swipeRefreshLayout.setRefreshing(true);

                    final View innerLayout = binding.scrollView.getChildAt(0);
                    final View peekContainer = mDrawerView.findViewById(R.id.ws_drawer_view_peek_container);

                    innerLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isViewAlive()) {
                                int verticalPadding = getResources().getDimensionPixelSize(R.dimen.inner_frame_layout_padding);

                                int mScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                                int mScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

                                int rightEdge = Math.min(binding.swipeRefreshLayout.getMeasuredWidth(), mScreenWidth);
                                int bottomEdge = Math.min(binding.swipeRefreshLayout.getMeasuredHeight(), mScreenHeight);
                                int verticalInset = (int) (FACTOR * Math.max(rightEdge, bottomEdge));

                                innerLayout.setPaddingRelative(
                                        innerLayout.getPaddingStart(),
                                        verticalPadding,
                                        innerLayout.getPaddingEnd(),
                                        mIsRound ? verticalInset : peekContainer.getHeight());
                            }
                        }
                    });
                }

                return true;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        if (getFragmentActivity() != null && mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocCallback)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRequestingLocationUpdates = false;
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("WeatherNowFragment: onResume");

        binding.scrollView.requestFocus();
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherNowFragment: onPause");

        if (syncReceiverRegistered) {
            LocalBroadcastManager.getInstance(getFragmentActivity())
                    .unregisterReceiver(syncDataReceiver);
            syncReceiverRegistered = false;
        }

        if (syncTimerEnabled)
            cancelTimer();

        // Remove location updates to save battery.
        stopLocationUpdates();

        super.onPause();
    }

    private boolean verifyLocationData() {
        boolean locationChanged = false;

        if (args.getData() != null) {
            LocationData location = AsyncTask.await(new Callable<LocationData>() {
                @Override
                public LocationData call() {
                    return JSONParser.deserializer(args.getData(), LocationData.class);
                }
            });

            if (!ObjectsCompat.equals(location, locationData)) {
                locationData = location;
                locationChanged = true;
            }
        }

        return locationChanged;
    }

    private void resume() {
        boolean locationChanged = verifyLocationData();

        if (locationChanged || wLoader == null) {
            restore();
        } else {
            ULocale currentLocale = ULocale.forLocale(LocaleUtils.getLocale());
            String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

            // Reset if source || locale is different
            if (!Settings.getAPI().equals(weatherView.getWeatherSource())
                    || wm.supportsWeatherLocale() && !locale.equals(weatherView.getWeatherLocale())) {
                restore();
            } else {
                // Update weather if needed on resume
                if (Settings.useFollowGPS() && updateLocation()) {
                    // Setup loader from updated location
                    wLoader = new WeatherDataLoader(locationData);
                }

                refreshWeather(false);
            }
        }
    }

    private void restore() {
        AsyncTask.create(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean forceRefresh = false;

                // GPS Follow location
                if (Settings.useFollowGPS() && (locationData == null || locationData.getLocationType() == LocationType.GPS)) {
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
                            locationData = locData;
                        }
                    }
                } else if (locationData == null && wLoader == null) {
                    // Weather was loaded before. Lets load it up...
                    locationData = Settings.getHomeData();
                }

                if (locationData != null)
                    wLoader = new WeatherDataLoader(locationData);

                return forceRefresh;
            }
        }).addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean forceRefresh) {
                // Load up weather data
                refreshWeather(forceRefresh);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void refreshWeather(final boolean forceRefresh) {
        runWithView(new Runnable() {
            @Override
            public void run() {
                binding.swipeRefreshLayout.setRefreshing(true);

                if (Settings.getDataSync() == WearableDataSync.OFF && wLoader != null) {
                    wLoader.loadWeatherResult(new WeatherRequest.Builder()
                            .forceRefresh(forceRefresh)
                            .setErrorListener(WeatherNowFragment.this)
                            .build())
                            .addOnSuccessListener(new OnSuccessListener<WeatherResult>() {
                                @Override
                                public void onSuccess(final WeatherResult weather) {
                                    weatherLiveData.setValue(weather.getWeather());
                                }
                            })
                            .continueWithTask(new Continuation<WeatherResult, Task<Collection<WeatherAlert>>>() {
                                @Override
                                public Task<Collection<WeatherAlert>> then(@NonNull Task<WeatherResult> task) {
                                    if (task.isSuccessful()) {
                                        runWithView(new Runnable() {
                                            @Override
                                            public void run() {
                                                binding.alertButton.setVisibility(View.GONE);
                                            }
                                        });
                                        return wLoader.loadWeatherAlerts(task.getResult().isSavedData());
                                    } else {
                                        return Tasks.forCanceled();
                                    }
                                }
                            })
                            .addOnCompleteListener(new OnCompleteListener<Collection<WeatherAlert>>() {
                                @Override
                                public void onComplete(@NonNull final Task<Collection<WeatherAlert>> task) {
                                    runWithView(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (locationData != null) {
                                                alertsView.updateAlerts(locationData);
                                            }

                                            if (task.isSuccessful()) {
                                                final Collection<WeatherAlert> weatherAlerts = task.getResult();
                                                if (weatherAlerts != null && !weatherAlerts.isEmpty()) {
                                                    binding.alertButton.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                } else if (Settings.getDataSync() != WearableDataSync.OFF) {
                    dataSyncResume(forceRefresh);
                }
            }
        });
    }

    private boolean updateLocation() {
        return AsyncTask.await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (Settings.getDataSync() == WearableDataSync.OFF &&
                        Settings.useFollowGPS() && (locationData == null || locationData.getLocationType() == LocationType.GPS)) {
                    if (getFragmentActivity() != null && ContextCompat.checkSelfPermission(getFragmentActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getFragmentActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
                        return false;
                    }

                    Location location = null;

                    LocationManager locMan = null;
                    if (getFragmentActivity() != null)
                        locMan = (LocationManager) getFragmentActivity().getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        locationData = Settings.getHomeData();
                        return false;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = AsyncTask.await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 10, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    Logger.writeLine(Log.ERROR, e);
                                }
                                return result;
                            }
                        });

                        /*
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
                            runWithView(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getFragmentActivity(), R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
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

                        LocationQueryViewModel view;

                        try {
                            view = wm.getLocation(location);
                        } catch (WeatherException e) {
                            // Stop since there is no valid query
                            Logger.writeLine(Log.ERROR, e);
                            return false;
                        }

                        if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        // Save location as last known
                        lastGPSLocData.setData(view, location);
                        Settings.saveHomeData(lastGPSLocData);

                        locationData = lastGPSLocData;
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
                    updateLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Settings.setFollowGPS(false);
                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getFragmentActivity(), R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return;
            }
            default:
                break;
        }
    }

    /* Data Sync */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (StringUtils.isNullOrWhitespace(key))
            return;

        switch (key) {
            case Settings.KEY_DATASYNC:
                // If data sync settings changes,
                // reset so we can properly reload
                wLoader = null;
                locationData = null;
                break;
            default:
                break;
        }
    }

    private void dataSyncRestore() {
        dataSyncRestore(false);
    }

    private void dataSyncRestore(final boolean forceRefresh) {
        runWithView(new Runnable() {
            @Override
            public void run() {
                // Send request to service to get weather data
                binding.swipeRefreshLayout.setRefreshing(true);

                // Check data map if data is available to load
                wLoader = null;
                locationData = null;
                WearableWorker.enqueueAction(getFragmentActivity(), WearableWorker.ACTION_REQUESTUPDATE, forceRefresh);

                // Start timeout timer
                resetTimer();
            }
        });
    }

    private void dataSyncResume() {
        dataSyncResume(false);
    }

    private void dataSyncResume(boolean forceRefresh) {
        if (!isViewAlive()) {
            cancelDataSync();
            return;
        }

        if (!syncReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WearableHelper.LocationPath);
            filter.addAction(WearableHelper.WeatherPath);
            filter.addAction(WearableHelper.IsSetupPath);

            LocalBroadcastManager.getInstance(getFragmentActivity())
                    .registerReceiver(syncDataReceiver, filter);
            syncReceiverRegistered = true;
        }

        if (wLoader == null || forceRefresh) {
            dataSyncRestore(forceRefresh);
        } else {
            // Update weather if needed on resume
            if (locationData == null || !locationData.equals(Settings.getHomeData()))
                locationData = Settings.getHomeData();

            binding.swipeRefreshLayout.setRefreshing(true);

            wLoader = new WeatherDataLoader(locationData);
            wLoader.loadWeatherData(new WeatherRequest.Builder()
                    .forceLoadSavedData()
                    .loadAlerts()
                    .setErrorListener(WeatherNowFragment.this)
                    .build())
                    .addOnSuccessListener(new OnSuccessListener<Weather>() {
                        @Override
                        public void onSuccess(final Weather weather) {
                            if (weather != null) {
                                weatherLiveData.setValue(weather);
                            } else {
                                // Data is null; restore
                                dataSyncRestore();
                            }
                        }
                    });
        }
    }

    private void cancelDataSync() {
        if (syncTimerEnabled)
            cancelTimer();

        if (isViewAlive() && Settings.getDataSync() != WearableDataSync.OFF) {
            if (locationData == null) {
                // Load whatever we have available
                locationData = Settings.getHomeData();
            }

            if (locationData != null) {
                binding.swipeRefreshLayout.setRefreshing(true);

                wLoader = new WeatherDataLoader(locationData);
                wLoader.loadWeatherData(new WeatherRequest.Builder()
                        .forceLoadSavedData()
                        .loadAlerts()
                        .setErrorListener(WeatherNowFragment.this)
                        .build())
                        .addOnSuccessListener(new OnSuccessListener<Weather>() {
                            @Override
                            public void onSuccess(final Weather weather) {
                                weatherLiveData.setValue(weather);
                            }
                        });
            } else {
                binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getFragmentActivity(), R.string.error_syncing, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void resetTimer() {
        if (syncTimerEnabled)
            cancelTimer();
        syncTimer = new Timer();
        syncTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // We hit the interval
                // Data syncing is taking a long time to setup
                // Stop and load saved data
                cancelDataSync();
            }
        }, 35000); // 35sec
        syncTimerEnabled = true;
    }

    private void cancelTimer() {
        syncTimer.cancel();
        syncTimer.purge();
        syncTimerEnabled = false;
    }
}
