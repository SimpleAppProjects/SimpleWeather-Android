package com.thewizrd.simpleweather.setup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.lifecycle.LifecycleRunnable;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
import com.thewizrd.shared_resources.tasks.TaskUtils;
import com.thewizrd.shared_resources.tzdb.TZDBCache;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.CustomException;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocationUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupBinding;
import com.thewizrd.simpleweather.fragments.CustomFragment;
import com.thewizrd.simpleweather.helpers.AcceptDenyDialogBuilder;
import com.thewizrd.simpleweather.main.MainActivity;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;

public class SetupFragment extends CustomFragment {

    private FragmentSetupBinding binding;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int REQUEST_CODE_SYNC_ACTIVITY = 10;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    private CancellationTokenSource cts = new CancellationTokenSource();

    private WeatherManager wm = WeatherManager.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Settings.isWeatherLoaded()) {
            // Verify provider key
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
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        mLocation = null;
                    else
                        mLocation = locationResult.getLastLocation();

                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            if (mLocation == null) {
                                enableControls(true);
                                Toast.makeText(getFragmentActivity(), R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                            } else {
                                fetchGeoLocation();
                            }
                        }
                    });

                    stopLocationUpdates();
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
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "SetupActivity: stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocCallback)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    private void resetTokenSource() {
        if (cts != null) cts.cancel();
        cts = new CancellationTokenSource();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupBinding.inflate(inflater, container, false);

        // Controls
        binding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(SetupFragmentDirections.actionSetupFragmentToLocationSearchFragment());
            }
        });
        binding.locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchGeoLocation();
            }
        });
        binding.setupPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AcceptDenyDialogBuilder(requireActivity(), new AcceptDenyDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            startActivityForResult(new Intent(requireActivity(), SetupSyncActivity.class), REQUEST_CODE_SYNC_ACTIVITY);
                        }
                    }
                }).setMessage(R.string.prompt_confirmsetup)
                        .show();
            }
        });

        binding.progressBar.setVisibility(View.GONE);

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SYNC_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    if (Settings.getHomeData() != null) {
                        Settings.setDataSync(WearableDataSync.DEVICEONLY);
                        Settings.setWeatherLoaded(true);
                        // Start WeatherNow Activity
                        startActivity(new Intent(requireActivity(), MainActivity.class));
                        requireActivity().finishAffinity();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("SetupFragment: onResume");
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("SetupFragment: onPause");
        cts.cancel();
        // Remove location updates to save battery.
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        cts.cancel();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        cts.cancel();
        super.onDestroy();
    }

    private void enableControls(final boolean enable) {
        binding.searchButton.setEnabled(enable);
        binding.locationButton.setEnabled(enable);
        if (enable) {
            binding.progressBar.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void fetchGeoLocation() {
        runWithView(new LifecycleRunnable(getViewLifecycleOwner().getLifecycle()) {
            @Override
            public void run() {
                // Show loading bar
                binding.locationButton.setEnabled(false);
                enableControls(false);

                if (mLocation == null) {
                    AsyncTask.create(new Callable<Void>() {
                        @Override
                        public Void call() throws CustomException {
                            updateLocation();
                            return null;
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull final Exception e) {
                            if (isActive() && isViewAlive()) {
                                // Restore controls
                                enableControls(true);
                                Settings.setFollowGPS(false);
                                Settings.setWeatherLoaded(false);

                                if (getFragmentActivity() != null) {
                                    if (e instanceof WeatherException || e instanceof CustomException) {
                                        Toast.makeText(getFragmentActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getFragmentActivity(), R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    });
                } else {
                    // Cancel other tasks
                    resetTokenSource();
                    final CancellationToken token = cts.getToken();

                    AsyncTask.create(new Callable<LocationData>() {
                        @Override
                        public LocationData call() throws InterruptedException, WeatherException, CustomException, ExecutionException {
                            LocationQueryViewModel view;

                            TaskUtils.throwIfCancellationRequested(token);

                            view = wm.getLocation(mLocation);

                            if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                                view = new LocationQueryViewModel();
                            } else if (StringUtils.isNullOrWhitespace(view.getLocationTZLong()) && view.getLocationLat() != 0 && view.getLocationLong() != 0) {
                                String tzId = TZDBCache.getTimeZone(view.getLocationLat(), view.getLocationLong());
                                if (!"unknown".equals(tzId))
                                    view.setLocationTZLong(tzId);
                            }

                            if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                                throw new CustomException(R.string.error_retrieve_location);
                            }

                            final boolean isUS = LocationUtils.isUS(view.getLocationCountry());

                            if (!Settings.isWeatherLoaded()) {
                                // Default US provider to NWS
                                if (isUS) {
                                    Settings.setAPI(WeatherAPI.NWS);
                                    view.updateWeatherSource(WeatherAPI.NWS);
                                } else {
                                    Settings.setAPI(WeatherAPI.YAHOO);
                                    view.updateWeatherSource(WeatherAPI.YAHOO);
                                }
                                wm.updateAPI();
                            }

                            if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                                throw new CustomException(R.string.werror_invalidkey);
                            }

                            TaskUtils.throwIfCancellationRequested(token);

                            if (WeatherAPI.NWS.equals(Settings.getAPI()) && !isUS) {
                                throw new CustomException(R.string.error_message_weather_us_only);
                            }

                            // Get Weather Data
                            final LocationData location = new LocationData(view, mLocation);
                            if (!location.isValid()) {
                                throw new CustomException(R.string.werror_noweather);
                            }

                            TaskUtils.throwIfCancellationRequested(token);

                            Weather weather = Settings.getWeatherData(location.getQuery());
                            if (weather == null) {
                                TaskUtils.throwIfCancellationRequested(token);

                                weather = AsyncTask.await(new CallableEx<Weather, WeatherException>() {
                                    @Override
                                    public Weather call() throws WeatherException {
                                        return wm.getWeather(location);
                                    }
                                }, token);
                            }

                            if (weather == null) {
                                throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
                            } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                                weather.setWeatherAlerts(wm.getAlerts(location));
                            }

                            TaskUtils.throwIfCancellationRequested(token);

                            // Save weather data
                            Settings.saveHomeData(location);
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

                            // If we're changing locations, trigger an update
                            if (Settings.isWeatherLoaded()) {
                                LocalBroadcastManager.getInstance(getFragmentActivity())
                                        .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                            }

                            Settings.setFollowGPS(true);
                            Settings.setWeatherLoaded(true);
                            Settings.setDataSync(WearableDataSync.OFF);

                            return location;
                        }
                    }).addOnSuccessListener(new OnSuccessListener<LocationData>() {
                        @Override
                        public void onSuccess(LocationData locationData) {
                            if (isActive() && isViewAlive()) {
                                if (locationData != null) {
                                    // Start WeatherNow Activity with weather data
                                    Intent intent = new Intent(getFragmentActivity(), MainActivity.class);
                                    intent.putExtra(Constants.KEY_DATA, JSONParser.serializer(locationData, LocationData.class));

                                    startActivity(intent);
                                    getFragmentActivity().finishAffinity();
                                } else {
                                    enableControls(true);
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (isActive() && isViewAlive()) {
                                // Restore controls
                                enableControls(true);
                                Settings.setFollowGPS(false);
                                Settings.setWeatherLoaded(false);

                                if (getFragmentActivity() != null) {
                                    if (e instanceof WeatherException || e instanceof CustomException) {
                                        Toast.makeText(getFragmentActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getFragmentActivity(), R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void updateLocation() throws CustomException {
        if (ContextCompat.checkSelfPermission(getFragmentActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getFragmentActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION_REQUEST_CODE);
            return;
        }

        Location location = null;

        LocationManager locMan = (LocationManager) getFragmentActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw new CustomException(R.string.error_enable_location_services);
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

            if (isGPSEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location == null)
                    location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null)
                    locMan.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocListnr, Looper.getMainLooper());
            } else if (isNetEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null)
                    locMan.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocListnr, Looper.getMainLooper());
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
                    if (isViewAlive()) {
                        enableControls(true);
                        Toast.makeText(getFragmentActivity(), R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
