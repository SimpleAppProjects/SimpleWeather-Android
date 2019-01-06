package com.thewizrd.simpleweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

import java.util.concurrent.Callable;

public class SetupActivity extends AppCompatActivity {

    private LocationSearchFragment mSearchFragment;
    private ActionMode mActionMode;
    private View searchViewLayout;
    private View searchViewContainer;
    private EditText searchView;
    private TextView clearButtonView;
    private boolean inSearchUI;

    private Button gpsFollowButton;
    private ProgressBar progressBar;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private CancellationTokenSource cts;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (searchViewLayout == null)
                searchViewLayout = getLayoutInflater().inflate(R.layout.search_action_bar, null);

            mode.setCustomView(searchViewLayout);
            enterSearchUi();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            exitSearchUi();
            mActionMode = null;
        }
    };

    private WeatherManager wm;

    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if this activity was started from adding a new widget
        if (getIntent() != null && AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(getIntent().getAction())) {
            mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            if (Settings.isWeatherLoaded() || mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                // This shouldn't happen, but just in case
                setResult(RESULT_OK);
                finish();
                // Return if we're finished
                return;
            }

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                // Set the result to CANCELED.  This will cause the widget host to cancel
                // out of the widget placement if they press the back button.
                setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        }

        setContentView(R.layout.activity_setup);

        // Get ActionMode state
        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchUI", false)) {
            inSearchUI = true;

            // Restart ActionMode
            mActionMode = startSupportActionMode(mActionModeCallback);
        }

        wm = WeatherManager.getInstance();

        searchViewContainer = findViewById(R.id.search_bar);
        gpsFollowButton = findViewById(R.id.gps_follow);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // NOTE: Bug: Explicitly set tintmode on Lollipop devices
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
            progressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);

        /* Event Listeners */
        searchViewContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActionMode = startSupportActionMode(mActionModeCallback);
            }
        });

        findViewById(R.id.search_fragment_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi();
            }
        });

        gpsFollowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchGeoLocation();
            }
        });

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(this);
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        mLocation = null;
                    else
                        mLocation = locationResult.getLastLocation();

                    if (mLocation == null) {
                        enableControls(true);
                        Toast.makeText(SetupActivity.this, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                    } else {
                        fetchGeoLocation();
                    }

                    new AsyncTask<Void>().await(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            return Tasks.await(mFusedLocationClient.removeLocationUpdates(mLocCallback));
                        }
                    });
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    new AsyncTask<Void>().await(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            return Tasks.await(mFusedLocationClient.flushLocations());
                        }
                    });

                    if (!locationAvailability.isLocationAvailable()) {
                        enableControls(true);
                        Toast.makeText(SetupActivity.this, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
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

        // Reset focus
        findViewById(R.id.activity_setup).requestFocus();

        // Set default API to HERE
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        if (StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
            // If (internal) key doesn't exist, fallback to Met.no
            Settings.setAPI(WeatherAPI.METNO);
            wm.updateAPI();
            Settings.setPersonalKey(true);
            Settings.setKeyVerified(false);
        } else {
            // If key exists, go ahead
            Settings.setPersonalKey(false);
            Settings.setKeyVerified(true);
        }
    }

    private void enableControls(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchViewContainer.setEnabled(enable);
                gpsFollowButton.setEnabled(enable);
                progressBar.setVisibility(enable ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void fetchGeoLocation() {
        gpsFollowButton.setEnabled(false);

        // Show loading bar
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    if (mLocation != null) {
                        LocationQueryViewModel view = null;

                        // Cancel other tasks
                        if (cts != null) cts.cancel();
                        cts = new CancellationTokenSource();
                        CancellationToken ctsToken = cts.getToken();

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        // Show loading bar
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        });

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        view = wm.getLocation(mLocation);

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            enableControls(true);
                            return null;
                        }

                        if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), R.string.werror_invalidkey, Toast.LENGTH_SHORT).show();
                                }
                            });
                            enableControls(true);
                            return null;
                        }

                        if (ctsToken.isCancellationRequested()) throw new InterruptedException();

                        // Get Weather Data
                        LocationData location = new LocationData(view, mLocation);
                        if (!location.isValid()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), getString(R.string.werror_noweather), Toast.LENGTH_SHORT).show();
                                }
                            });
                            enableControls(true);
                            return null;
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
                                        Toast.makeText(getApplicationContext(), wEx.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        if (weather == null) {
                            enableControls(true);
                            return null;
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

                        Settings.setFollowGPS(true);
                        Settings.setWeatherLoaded(true);

                        // Send data for wearables
                        startService(new Intent(getApplicationContext(), WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                        startService(new Intent(getApplicationContext(), WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                        startService(new Intent(getApplicationContext(), WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));

                        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                            // Start WeatherNow Activity with weather data
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("data", location.toJson());

                            startActivity(intent);
                            finishAffinity();
                        } else {
                            // Create return intent
                            Intent resultValue = new Intent();
                            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                            resultValue.putExtra("data", location.toJson());
                            setResult(RESULT_OK, resultValue);
                            finish();
                        }
                    } else {
                        updateLocation();
                    }
                } catch (Exception e) {
                    // Restore controls
                    enableControls(true);
                    Settings.setFollowGPS(false);
                    Settings.setWeatherLoaded(false);
                }
                return null;
            }
        });
    }

    private void updateLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION_REQUEST_CODE);
            return;
        }

        Location location = null;

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            location = new AsyncTask<Location>().await(new Callable<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public Location call() throws Exception {
                    return Tasks.await(mFusedLocationClient.getLastLocation());
                }
            });

            if (location == null) {
                final LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(10000);
                mLocationRequest.setFastestInterval(1000);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                new AsyncTask<Void>().await(new Callable<Void>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public Void call() throws Exception {
                        return Tasks.await(mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocCallback, Looper.getMainLooper()));
                    }
                });
                new AsyncTask<Void>().await(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        return Tasks.await(mFusedLocationClient.flushLocations());
                    }
                });
            }
        } else {
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = false;
            boolean isNetEnabled = false;
            if (locMan != null) {
                isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }

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
                        Toast.makeText(SetupActivity.this, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        if (location != null) {
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
                    // TODO: logger any errors
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    enableControls(true);
                    Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) fragment;
            setupSearchUi();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save ActionMode state
        outState.putBoolean("SearchUI", inSearchUI);

        if (inSearchUI)
            exitSearchUi();

        super.onSaveInstanceState(outState);
    }

    private void enterSearchUi() {
        inSearchUI = true;
        if (mSearchFragment == null) {
            addSearchFragment();
            return;
        }
        mSearchFragment.setUserVisibleHint(true);
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        setupSearchUi();
    }

    private void setupSearchUi() {
        if (searchView == null) {
            prepareSearchView();
        }
        searchView.requestFocus();
    }

    private void addSearchFragment() {
        if (mSearchFragment != null) {
            return;
        }
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Fragment searchFragment = new LocationSearchFragment();
        searchFragment.setUserVisibleHint(false);

        // Add AppWidgetId to fragment args
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Bundle args = new Bundle();
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            searchFragment.setArguments(args);
        }

        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        searchView = searchViewLayout.findViewById(R.id.search_view);
        clearButtonView = searchViewLayout.findViewById(R.id.search_close_button);
        clearButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String newText = s.toString();

                if (mSearchFragment != null) {
                    // Cancel pending searches
                    if (cts != null) cts.cancel();
                    progressBar.setVisibility(View.GONE);

                    // If we're using searchfragment
                    // make sure gps feature is off
                    Settings.setFollowGPS(false);

                    clearButtonView.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
                    mSearchFragment.fetchLocations(newText);
                }
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
        clearButtonView.setVisibility(View.GONE);
        searchView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (mSearchFragment != null) {
                        mSearchFragment.fetchLocations(v.getText().toString());
                        hideInputMethod(v);

                        // If we're using searchfragment
                        // make sure gps feature is off
                        Settings.setFollowGPS(false);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (inSearchUI) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi();
        } else {
            super.onBackPressed();
        }
    }

    private void exitSearchUi() {
        searchView.setText("");

        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);

            final FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            mSearchFragment = null;
            transaction.commitAllowingStateLoss();
        }

        hideInputMethod(getCurrentFocus());
        searchView.clearFocus();
        mActionMode.finish();
        inSearchUI = false;
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
