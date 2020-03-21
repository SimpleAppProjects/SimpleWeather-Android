package com.thewizrd.simpleweather.setup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupLocationBinding;
import com.thewizrd.simpleweather.fragments.LocationSearchFragment;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface;
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SetupLocationFragment extends Fragment implements Step, OnBackPressedFragmentListener, SnackbarManagerInterface {

    private LocationSearchFragment mSearchFragment;
    private boolean inSearchUI;

    private static final int ANIMATION_DURATION = 240;

    private SnackbarManager mSnackMgr;

    // Views
    private FragmentSetupLocationBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private CancellationTokenSource cts;
    private LocationData location = null;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    private AppCompatActivity mActivity;
    private StepperDataManager mDataManager;
    private View mStepperNavBar;
    private StepperLayout mStepperLayout;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    private WeatherManager wm;

    @Override
    public void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = new SnackbarManager(binding.getRoot());
            mSnackMgr.setSwipeDismissEnabled(true);
            mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
            mSnackMgr.setAnchorView(mStepperNavBar);
        }
    }

    @Override
    public void showSnackbar(final com.thewizrd.simpleweather.snackbar.Snackbar snackbar, final com.google.android.material.snackbar.Snackbar.Callback callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.show(snackbar, callback);
            }
        });
    }

    @Override
    public void dismissAllSnackbars() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.dismissAll();
            }
        });
    }

    @Override
    public void unloadSnackManager() {
        dismissAllSnackbars();
        mSnackMgr = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false);
        wm = WeatherManager.getInstance();

        mStepperLayout = mActivity.findViewById(R.id.stepperLayout);
        mStepperNavBar = mActivity.findViewById(R.id.ms_bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchFragmentContainer, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        binding.progressBar.setVisibility(View.GONE);

        // NOTE: Bug: Explicitly set tintmode on Lollipop devices
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
            binding.progressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);

        // Tint drawable in button view
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            for (Drawable drawable : binding.gpsFollow.getCompoundDrawables()) {
                if (drawable != null) {
                    Configuration config = mActivity.getResources().getConfiguration();
                    final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

                    drawable.setColorFilter(new PorterDuffColorFilter(
                            currentNightMode == Configuration.UI_MODE_NIGHT_YES ? Colors.WHITE : Colors.SIMPLEBLUE, PorterDuff.Mode.SRC_IN));
                    TextViewCompat.setCompoundDrawablesRelative(binding.gpsFollow, drawable, null, null, null);
                }
            }
        }

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Setup search UI
                prepareSearchUI();
            }
        });

        binding.searchFragmentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi(false);
            }
        });

        binding.gpsFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchGeoLocation();
            }
        });

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(mActivity);
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        mLocation = null;
                    else
                        mLocation = locationResult.getLastLocation();

                    if (mLocation == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableControls(true);
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                            }
                        });
                    } else {
                        fetchGeoLocation();
                    }

                    stopLocationUpdates();
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    new AsyncTask<Void>().await(new Callable<Void>() {
                        @Override
                        public Void call() {
                            try {
                                return Tasks.await(mFusedLocationClient.flushLocations());
                            } catch (ExecutionException | InterruptedException e) {
                                Logger.writeLine(Log.ERROR, e);
                            }

                            return null;
                        }
                    });

                    if (!locationAvailability.isLocationAvailable()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopLocationUpdates();
                                enableControls(true);
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                            }
                        });
                    }
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mLocation = location;
                    fetchGeoLocation();
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

        // Reset focus
        binding.getRoot().requestFocus();

        // Set default API to HERE
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        if (wm.isKeyRequired() && StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
            // If (internal) key doesn't exist, fallback to Yahoo
            Settings.setAPI(WeatherAPI.YAHOO);
            wm.updateAPI();
            Settings.setPersonalKey(true);
            Settings.setKeyVerified(false);
        } else {
            // If key exists, go ahead
            Settings.setPersonalKey(false);
            Settings.setKeyVerified(true);
        }

        // Get SearchUI state
        if (savedInstanceState != null && savedInstanceState.getBoolean(Constants.KEY_SEARCHUI, false)) {
            inSearchUI = true;

            // Restart SearchUI
            prepareSearchUI();
        }

        return binding.getRoot();
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
        mFusedLocationClient.removeLocationUpdates(mLocCallback)
                .addOnCompleteListener(mActivity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    private void ctsCancel() {
        if (cts != null) cts.cancel();
        cts = new CancellationTokenSource();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mActivity = (AppCompatActivity) context;
        mDataManager = (StepperDataManager) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActivity = null;
        mDataManager = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        initSnackManager();
    }

    @Override
    public void onPause() {
        ctsCancel();
        super.onPause();
        // Remove location updates to save battery.
        stopLocationUpdates();
        unloadSnackManager();
    }

    @Override
    public void onDestroy() {
        // Cancel pending actions
        ctsCancel();

        super.onDestroy();
        mActivity = null;
        mDataManager = null;
    }

    private void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    @Override
    public boolean onBackPressed() {
        if (inSearchUI) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi(false);
            return true;
        }

        return false;
    }

    private void enableControls(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.searchBar.searchViewContainer.setEnabled(enable);
                binding.gpsFollow.setEnabled(enable);
                binding.progressBar.setVisibility(enable ? View.GONE : View.VISIBLE);
            }
        });
    }

    private RecyclerOnClickListenerInterface recyclerClickInterface = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(final View view, final int position) {
            if (mSearchFragment == null)
                return;

            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    if (mActivity != null) {
                        // Get selected query view
                        final LocationQueryAdapter adapter = mSearchFragment.getAdapter();
                        LocationQueryViewModel query_vm = null;

                        try {
                            if (!StringUtils.isNullOrEmpty(adapter.getDataset().get(position).getLocationQuery()))
                                query_vm = adapter.getDataset().get(position);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            query_vm = null;
                        } finally {
                            if (query_vm == null)
                                query_vm = new LocationQueryViewModel();
                        }

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return;
                        }

                        if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showSnackbar(Snackbar.make(R.string.werror_invalidkey, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(mActivity));
                                }
                            });
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        // Cancel pending search
                        mSearchFragment.ctsCancel();
                        CancellationToken ctsToken = mSearchFragment.getCancellationTokenSource().getToken();

                        mSearchFragment.showLoading(true);

                        if (ctsToken.isCancellationRequested()) {
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        String country_code = query_vm.getLocationCountry();
                        if (!StringUtils.isNullOrWhitespace(country_code))
                            country_code = country_code.toLowerCase();

                        if (WeatherAPI.NWS.equals(Settings.getAPI()) && !("usa".equals(country_code) || "us".equals(country_code))) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(mActivity));
                                }
                            });
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        // Need to get FULL location data for HERE API
                        // Data provided is incomplete
                        if (WeatherAPI.HERE.equals(query_vm.getLocationSource())
                                && query_vm.getLocationLat() == -1 && query_vm.getLocationLong() == -1
                                && query_vm.getLocationTZLong() == null) {
                            final LocationQueryViewModel loc = query_vm;
                            try {
                                query_vm = new AsyncTaskEx<LocationQueryViewModel, WeatherException>().await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                                    @Override
                                    public LocationQueryViewModel call() throws WeatherException {
                                        return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                                    }
                                });
                            } catch (final WeatherException wEx) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSearchFragment.showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(mActivity));
                                    }
                                });
                                mSearchFragment.showLoading(false);
                                return;
                            }
                        }

                        // Get weather data
                        location = new LocationData(query_vm);
                        if (!location.isValid()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showSnackbar(Snackbar.make(R.string.werror_noweather, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(mActivity));
                                }
                            });
                            mSearchFragment.showLoading(false);
                            return;
                        }
                        Weather weather = Settings.getWeatherData(location.getQuery());
                        if (weather == null) {
                            try {
                                weather = wm.getWeather(location);
                            } catch (final WeatherException wEx) {
                                weather = null;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSearchFragment.showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(mActivity));
                                        mSearchFragment.showLoading(false);
                                    }
                                });
                            }
                        }

                        if (weather == null) {
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        // We got our data so disable controls just in case
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.getDataset().clear();
                                adapter.notifyDataSetChanged();
                                if (mSearchFragment != null) {
                                    mSearchFragment.disableRecyclerView();
                                }
                            }
                        });

                        // Save weather data
                        Settings.deleteLocations();
                        Settings.addLocation(location);
                        if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                            Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                        Settings.saveWeatherData(weather);
                        Settings.saveWeatherForecasts(new Forecasts(weather.getQuery(), weather.getForecast(), weather.getTxtForecast()));
                        final Weather finalWeather = weather;
                        Settings.saveWeatherForecasts(location.getQuery(), weather.getHrForecast() == null ? null :
                                Collections2.transform(weather.getHrForecast(), new Function<HourlyForecast, HourlyForecasts>() {
                                    @NullableDecl
                                    @Override
                                    public HourlyForecasts apply(@NullableDecl HourlyForecast input) {
                                        return new HourlyForecasts(finalWeather.getQuery(), input);
                                    }
                                }));

                        // If we're using search
                        // make sure gps feature is off
                        Settings.setFollowGPS(false);
                        Settings.setWeatherLoaded(true);

                        // Send data for wearables
                        if (mActivity != null) {
                            WearableDataListenerService.enqueueWork(mActivity,
                                    new Intent(mActivity, WearableDataListenerService.class)
                                            .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                            WearableDataListenerService.enqueueWork(mActivity,
                                    new Intent(mActivity, WearableDataListenerService.class)
                                            .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                            WearableDataListenerService.enqueueWork(mActivity,
                                    new Intent(mActivity, WearableDataListenerService.class)
                                            .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
                        }

                        // Setup complete
                        mDataManager.getArguments().putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mStepperLayout.proceed();
                            }
                        });
                    }
                }
            });
        }
    };

    private void fetchGeoLocation() {
        binding.gpsFollow.setEnabled(false);

        // Show loading bar
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.progressBar.setVisibility(View.VISIBLE);
            }
        });

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mLocation != null) {
                        LocationQueryViewModel view = null;

                        // Cancel other tasks
                        ctsCancel();
                        CancellationToken ctsToken = cts.getToken();

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        // Show loading bar
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.progressBar.setVisibility(View.VISIBLE);
                            }
                        });

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        view = wm.getLocation(mLocation);

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            enableControls(true);
                            return;
                        }

                        if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSnackbar(Snackbar.make(R.string.werror_invalidkey, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(mActivity));
                                }
                            });
                            enableControls(true);
                            return;
                        }

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        String country_code = view.getLocationCountry();
                        if (!StringUtils.isNullOrWhitespace(country_code))
                            country_code = country_code.toLowerCase();

                        if (WeatherAPI.NWS.equals(Settings.getAPI()) && !("usa".equals(country_code) || "us".equals(country_code))) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(mActivity));
                                }
                            });
                            enableControls(true);
                            return;
                        }

                        // Get Weather Data
                        location = new LocationData(view, mLocation);
                        if (!location.isValid()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSnackbar(Snackbar.make(R.string.werror_noweather, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(mActivity));
                                }
                            });
                            enableControls(true);
                            return;
                        }

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        Weather weather = Settings.getWeatherData(location.getQuery());
                        if (weather == null) {
                            if (ctsToken.isCancellationRequested())
                                throw new InterruptedException();

                            try {
                                TaskCompletionSource<Weather> tcs = new TaskCompletionSource<>(ctsToken);
                                tcs.setResult(wm.getWeather(location));
                                weather = Tasks.await(tcs.getTask());
                            } catch (final WeatherException wEx) {
                                weather = null;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(mActivity));
                                    }
                                });
                            }
                        }

                        if (weather == null) {
                            enableControls(true);
                            return;
                        }

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        // We got our data so disable controls just in case
                        enableControls(false);

                        // Save weather data
                        Settings.saveLastGPSLocData(location);
                        Settings.deleteLocations();
                        Settings.addLocation(new LocationData(view));
                        if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                            Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                        Settings.saveWeatherData(weather);
                        Settings.saveWeatherForecasts(new Forecasts(weather.getQuery(), weather.getForecast(), weather.getTxtForecast()));
                        final Weather finalWeather = weather;
                        Settings.saveWeatherForecasts(location.getQuery(), weather.getHrForecast() == null ? null :
                                Collections2.transform(weather.getHrForecast(), new Function<HourlyForecast, HourlyForecasts>() {
                                    @NullableDecl
                                    @Override
                                    public HourlyForecasts apply(@NullableDecl HourlyForecast input) {
                                        return new HourlyForecasts(finalWeather.getQuery(), input);
                                    }
                                }));

                        Settings.setFollowGPS(true);
                        Settings.setWeatherLoaded(true);

                        // Send data for wearables
                        if (mActivity != null) {
                            mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                            mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                            mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
                        }

                        // Setup complete
                        mDataManager.getArguments().putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mStepperLayout.proceed();
                            }
                        });
                    } else {
                        updateLocation();
                    }
                } catch (Exception e) {
                    // Restore controls
                    enableControls(true);
                    Settings.setFollowGPS(false);
                    Settings.setWeatherLoaded(false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                    new SnackbarWindowAdjustCallback(mActivity));
                        }
                    });
                }
            }
        });
    }

    private void updateLocation() {
        if (mActivity != null && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERMISSION_LOCATION_REQUEST_CODE);
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_LOCATION_REQUEST_CODE);
            }
            return;
        }

        Location location = null;

        LocationManager locMan = null;
        if (mActivity != null)
            locMan = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.LONG),
                            new SnackbarWindowAdjustCallback(mActivity));
                }
            });

            // Disable GPS feature if location is not enabled
            enableControls(true);
            Settings.setFollowGPS(false);
            return;
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            location = new AsyncTask<Location>().await(new Callable<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public Location call() {
                    Location result = null;
                    try {
                        result = Tasks.await(mFusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
                    } catch (Exception e) {
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

            if (isGPSEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location == null)
                    location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null)
                    locMan.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocListnr, Looper.getMainLooper());
            } else if (isNetEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null)
                    locMan.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocListnr, null);
            } else {
                enableControls(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                new SnackbarWindowAdjustCallback(mActivity));
                    }
                });
            }
        }

        if (location != null && !mRequestingLocationUpdates) {
            mLocation = location;
            fetchGeoLocation();
        }
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
                    fetchGeoLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    enableControls(true);
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT),
                            new SnackbarWindowAdjustCallback(mActivity));
                }
                return;
            }
        }
    }

    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        if (childFragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) childFragment;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save SearchUI state
        outState.putBoolean(Constants.KEY_SEARCHUI, inSearchUI);

        super.onSaveInstanceState(outState);
    }

    private void prepareSearchUI() {
        ctsCancel();

        enterSearchUi();
        enterSearchUiTransition(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mSearchFragment != null)
                    mSearchFragment.requestSearchbarFocus();
                // Hide stepper nav bar
                mStepperLayout.setShowBottomNavigation(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void enterSearchUi() {
        inSearchUI = true;
        if (mSearchFragment == null) {
            addSearchFragment();
            return;
        }
        if (mSearchFragment.getRecyclerOnClickListener() == null)
            mSearchFragment.setRecyclerOnClickListener(recyclerClickInterface);
        mSearchFragment.setUserVisibleHint(true);
        final FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        getChildFragmentManager().executePendingTransactions();
    }

    private void enterSearchUiTransition(final Animation.AnimationListener enterAnimationListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MaterialContainerTransform transition = new MaterialContainerTransform(requireContext());
            transition.setStartView(binding.searchBar.searchViewContainer);
            transition.setEndView(binding.searchFragmentContainer);
            transition.setPathMotion(null);
            transition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {

                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    if (enterAnimationListener != null)
                        enterAnimationListener.onAnimationEnd(null);
                }

                @Override
                public void onTransitionCancel(Transition transition) {

                }

                @Override
                public void onTransitionPause(Transition transition) {

                }

                @Override
                public void onTransitionResume(Transition transition) {

                }
            });

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), transition);
            binding.searchFragmentContainer.setVisibility(View.VISIBLE);
            binding.searchBar.searchViewContainer.setVisibility(View.INVISIBLE);
        } else {
            binding.searchFragmentContainer.setVisibility(View.VISIBLE);
            if (enterAnimationListener != null)
                enterAnimationListener.onAnimationEnd(null);
        }
    }

    private void addSearchFragment() {
        if (mSearchFragment != null) {
            return;
        }
        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        final LocationSearchFragment searchFragment = new LocationSearchFragment();
        searchFragment.setRecyclerOnClickListener(recyclerClickInterface);
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitNowAllowingStateLoss();
    }

    private void removeSearchFragment() {
        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);
            final FragmentTransaction transaction = getChildFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            transaction.commitAllowingStateLoss();
            mSearchFragment = null;
        }
    }

    private void exitSearchUi(boolean skipAnimation) {
        if (mSearchFragment != null) {
            // Exit transition
            if (skipAnimation) {
                removeSearchFragment();
            } else {
                exitSearchUiTransition(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Remove fragment once animation ends
                        removeSearchFragment();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        }

        hideInputMethod(mActivity == null ? null : mActivity.getCurrentFocus());
        mStepperLayout.setShowBottomNavigation(true);
        if (binding.getRoot() != null) binding.getRoot().requestFocus();
        inSearchUI = false;
    }

    private void exitSearchUiTransition(final Animation.AnimationListener exitAnimationListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MaterialContainerTransform transition = new MaterialContainerTransform(requireContext());
            transition.setStartView(binding.searchFragmentContainer);
            transition.setEndView(binding.searchBar.searchViewContainer);
            transition.setPathMotion(null);
            transition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {

                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    if (exitAnimationListener != null)
                        exitAnimationListener.onAnimationEnd(null);
                }

                @Override
                public void onTransitionCancel(Transition transition) {

                }

                @Override
                public void onTransitionPause(Transition transition) {

                }

                @Override
                public void onTransitionResume(Transition transition) {

                }
            });

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), transition);
            binding.searchFragmentContainer.setVisibility(View.GONE);
            binding.searchBar.searchViewContainer.setVisibility(View.VISIBLE);
        } else {
            binding.searchFragmentContainer.setVisibility(View.GONE);
            binding.searchBar.searchViewContainer.setVisibility(View.VISIBLE);
            if (exitAnimationListener != null)
                exitAnimationListener.onAnimationEnd(null);
        }
    }

    private void showInputMethod(View view) {
        if (mActivity != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.showSoftInput(view, 0);
            }
        }
    }

    private void hideInputMethod(View view) {
        if (mActivity != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Nullable
    @Override
    public VerificationError verifyStep() {
        if (mDataManager != null) {
            if (Settings.isWeatherLoaded()) {
                if (location != null)
                    mDataManager.getArguments().putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));

                if (inSearchUI) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            exitSearchUi(true);
                        }
                    });
                }
                return null;
            }
        }
        return new VerificationError("Invalid activity");
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onError(@NonNull VerificationError error) {

    }
}
