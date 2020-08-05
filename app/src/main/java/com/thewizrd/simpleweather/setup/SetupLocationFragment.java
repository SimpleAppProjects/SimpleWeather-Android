package com.thewizrd.simpleweather.setup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.Hold;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CustomException;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupLocationBinding;
import com.thewizrd.simpleweather.fragments.CustomFragment;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback;
import com.thewizrd.simpleweather.wearable.WearableWorker;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SetupLocationFragment extends CustomFragment {

    // Views
    private FragmentSetupLocationBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private CancellationTokenSource cts;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    private SetupViewModel viewModel;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    private WeatherManager wm;

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        View mStepperNavBar = getAppCompatActivity().findViewById(R.id.bottom_nav_bar);
        SnackbarManager mSnackMgr = new SnackbarManager(binding.getRoot());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        mSnackMgr.setAnchorView(mStepperNavBar);
        return mSnackMgr;
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("SetupLocation: onCreate");

        // Hold fragment in place for MaterialContainerTransform
        setExitTransition(new Hold().setDuration(Constants.ANIMATION_DURATION));

        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false);
        wm = WeatherManager.getInstance();

        binding.progressBar.setVisibility(View.GONE);

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Setup search UI
                View bottomNavBar = getAppCompatActivity().findViewById(R.id.bottom_nav_bar);
                bottomNavBar.setVisibility(View.GONE);

                Navigation.findNavController(v)
                        .navigate(
                                SetupLocationFragmentDirections.actionSetupLocationFragmentToLocationSearchFragment3(),
                                new FragmentNavigator.Extras.Builder().addSharedElement(v, Constants.SHARED_ELEMENT)
                                        .build()
                        );
            }
        });
        ViewCompat.setTransitionName(binding.searchBar.searchViewContainer, Constants.SHARED_ELEMENT);

        binding.gpsFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchGeoLocation();
            }
        });

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        mLocation = null;
                    else
                        mLocation = locationResult.getLastLocation();

                    if (mLocation == null) {
                        enableControls(true);
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
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
                        stopLocationUpdates();
                        enableControls(true);
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
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

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(getAppCompatActivity()).get(SetupViewModel.class);

        MutableLiveData<String> liveData =
                Navigation.findNavController(view).getCurrentBackStackEntry()
                        .getSavedStateHandle()
                        .getLiveData(Constants.KEY_DATA);
        liveData.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String result) {
                // Do something with the result.
                if (result != null) {
                    // Save data
                    LocationData data = JSONParser.deserializer(result, LocationData.class);
                    if (data != null) {
                        // Setup complete
                        viewModel.setLocationData(data);
                        Navigation.findNavController(binding.getRoot())
                                .navigate(SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment());
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
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
                .addOnCompleteListener(getAppCompatActivity(), new OnCompleteListener<Void>() {
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
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("SetupLocation: onResume");
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("SetupLocation: onPause");
        ctsCancel();
        // Remove location updates to save battery.
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Cancel pending actions
        ctsCancel();
        super.onDestroy();
    }

    private void enableControls(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isAlive()) return;
                binding.searchBar.searchViewContainer.setEnabled(enable);
                binding.gpsFollow.setEnabled(enable);
                binding.progressBar.setVisibility(enable ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void fetchGeoLocation() {
        if (!isAlive()) return;

        binding.gpsFollow.setEnabled(false);

        // Show loading bar
        binding.progressBar.setVisibility(View.VISIBLE);

        if (mLocation == null) {
            AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() throws CustomException {
                    updateLocation();
                    return null;
                }
            }).addOnFailureListener(getAppCompatActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Restore controls
                    enableControls(true);
                    Settings.setFollowGPS(false);
                    Settings.setWeatherLoaded(false);

                    if (e instanceof WeatherException || e instanceof CustomException) {
                        showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT),
                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                    } else {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                    }
                }
            });
        } else {
            AsyncTask.create(new Callable<LocationData>() {
                @Override
                public LocationData call() throws InterruptedException, WeatherException, ExecutionException, CustomException {
                    LocationQueryViewModel view;

                    // Cancel other tasks
                    ctsCancel();
                    CancellationToken ctsToken = cts.getToken();

                    if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                    view = wm.getLocation(mLocation);

                    if (StringUtils.isNullOrWhitespace(view.getLocationQuery()))
                        view = new LocationQueryViewModel();

                    if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                        throw new CustomException(R.string.error_retrieve_location);
                    }

                    if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                        throw new CustomException(R.string.werror_invalidkey);
                    }

                    if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                    String country_code = view.getLocationCountry();
                    if (!StringUtils.isNullOrWhitespace(country_code))
                        country_code = country_code.toLowerCase();

                    if (WeatherAPI.NWS.equals(Settings.getAPI()) && !("usa".equals(country_code) || "us".equals(country_code))) {
                        throw new CustomException(R.string.error_message_weather_us_only);
                    }

                    // Get Weather Data
                    LocationData location = new LocationData(view, mLocation);
                    if (!location.isValid()) {
                        throw new CustomException(R.string.werror_noweather);
                    }

                    if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                    Weather weather = Settings.getWeatherData(location.getQuery());
                    if (weather == null) {
                        if (ctsToken.isCancellationRequested())
                            throw new InterruptedException();

                        TaskCompletionSource<Weather> tcs = new TaskCompletionSource<>(ctsToken);
                        tcs.setResult(wm.getWeather(location));
                        weather = Tasks.await(tcs.getTask());
                    }

                    if (weather == null) {
                        throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
                    }

                    if (ctsToken.isCancellationRequested()) throw new InterruptedException();

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
                                @NonNull
                                @Override
                                public HourlyForecasts apply(@NullableDecl HourlyForecast input) {
                                    return new HourlyForecasts(finalWeather.getQuery(), input);
                                }
                            }));

                    Settings.setFollowGPS(true);
                    Settings.setWeatherLoaded(true);

                    // Send data for wearables
                    if (getAppCompatActivity() != null) {
                        WearableWorker.enqueueAction(getAppCompatActivity(), WearableWorker.ACTION_SENDUPDATE);
                    }

                    return location;
                }
            }).addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<LocationData>() {
                @Override
                public void onSuccess(LocationData data) {
                    if (data != null && data.isValid()) {
                        // Setup complete
                        viewModel.setLocationData(data);
                        Navigation.findNavController(binding.getRoot())
                                .navigate(SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment());
                    } else {
                        enableControls(true);
                        Settings.setFollowGPS(false);

                        LocationManager locMan = null;
                        if (getAppCompatActivity() != null)
                            locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

                        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                            showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.LONG),
                                    new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                        } else {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                    new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                        }
                    }
                }
            }).addOnFailureListener(getAppCompatActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Restore controls
                    enableControls(true);
                    Settings.setFollowGPS(false);
                    Settings.setWeatherLoaded(false);

                    if (e instanceof WeatherException || e instanceof CustomException) {
                        showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT),
                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                    } else {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                    }
                }
            });
        }
    }

    private void updateLocation() throws CustomException {
        if (getAppCompatActivity() != null && ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        if (getAppCompatActivity() != null)
            locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            // Disable GPS feature if location is not enabled
            enableControls(true);
            Settings.setFollowGPS(false);
            throw new CustomException(R.string.error_enable_location_services);
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
                            new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                }
            }
        }
    }
}
