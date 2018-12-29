package com.thewizrd.simpleweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.stream.JsonReader;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.TextForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherErrorListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherLoadedListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter;
import com.thewizrd.simpleweather.adapters.TextForecastPagerAdapter;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBuilder;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;
import com.thewizrd.simpleweather.widgets.WidgetUtils;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;

public class WeatherNowFragment extends Fragment implements WeatherLoadedListenerInterface,
        WeatherErrorListenerInterface, ActivityCompat.OnRequestPermissionsResultCallback {
    private LocationData location = null;
    private boolean loaded = false;
    private int bgAlpha = 255;

    private WeatherManager wm;
    private WeatherDataLoader wLoader = null;
    private WeatherNowViewModel weatherView = null;
    private AppCompatActivity appCompatActivity;

    // Views
    private SwipeRefreshLayout refreshLayout;
    private NestedScrollView mainView;
    private ImageView bgImageView;
    // Condition
    private TextView locationName;
    private TextView updateTime;
    private TextView weatherIcon;
    private TextView weatherCondition;
    private TextView weatherTemp;
    // Details
    private View detailsPanel;
    private TextView humidity;
    private TextView pressureState;
    private TextView pressure;
    private TextView visiblity;
    private TextView feelslike;
    private TextView windDirection;
    private TextView windSpeed;
    private TextView sunrise;
    private TextView sunset;
    // Forecast
    private RelativeLayout forecastPanel;
    private RecyclerView forecastView;
    private ForecastItemAdapter forecastAdapter;
    // Additional Details
    private Switch forecastSwitch;
    private ViewPager txtForecastView;
    private LinearLayout hrforecastPanel;
    private RecyclerView hrforecastView;
    private HourlyForecastItemAdapter hrforecastAdapter;
    private RelativeLayout precipitationPanel;
    private TextView chanceLabel;
    private TextView chance;
    private TextView qpfRain;
    private TextView qpfSnow;
    private TextView cloudinessLabel;
    private TextView cloudiness;
    // Alerts
    private View alertButton;
    // Nav Header View
    private View navheader;
    private TextView navLocation;
    private TextView navWeatherTemp;
    // Weather Credit
    private TextView weatherCredit;

    // GPS location
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    public WeatherNowFragment() {
        // Required empty public constructor
        weatherView = new WeatherNowViewModel();
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
            args.putString("data", data.toJson());
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

    private static boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public void onWeatherLoaded(final LocationData location, final Weather weather) {
        if (appCompatActivity != null) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (weather != null && weather.isValid()) {
                        wm.updateWeather(weather);
                        weatherView.updateView(weather);
                        updateView(weatherView);

                        if (Settings.getHomeData().equals(location)) {
                            // Update widgets if they haven't been already
                            if (Duration.between(LocalDateTime.now(), Settings.getUpdateTime()).toMinutes() > Settings.getRefreshInterval()) {
                                WeatherWidgetService.enqueueWork(App.getInstance().getAppContext(), new Intent(App.getInstance().getAppContext(), WeatherWidgetService.class)
                                        .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
                            }

                            // Update ongoing notification if its not showing
                            if (Settings.showOngoingNotification() && !WeatherNotificationBuilder.isShowing()) {
                                WeatherWidgetService.enqueueWork(App.getInstance().getAppContext(), new Intent(App.getInstance().getAppContext(), WeatherWidgetService.class)
                                        .setAction(WeatherWidgetService.ACTION_REFRESHNOTIFICATION));
                            }
                        }

                        if (wm.supportsAlerts() && Settings.useAlerts() &&
                                weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
                            // Alerts are posted to the user here. Set them as notified.
                            AsyncTask.run(new Runnable() {
                                @Override
                                public void run() {
                                    WeatherAlertHandler.setAsNotified(location, weather.getWeatherAlerts());
                                }
                            });
                        }
                    }

                    refreshLayout.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onWeatherError(final WeatherException wEx) {
        if (appCompatActivity != null) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (wEx.getErrorStatus()) {
                        case NETWORKERROR:
                        case NOWEATHER:
                            // Show error message and prompt to refresh
                            Snackbar snackBar = Snackbar.make(mainView, wEx.getMessage(), Snackbar.LENGTH_LONG);
                            snackBar.setAction(R.string.action_retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new AsyncTask<Void>().await(new Callable<Void>() {
                                        @Override
                                        public Void call() throws Exception {
                                            refreshWeather(false);
                                            return null;
                                        }
                                    });
                                }
                            });
                            snackBar.show();
                            break;
                        default:
                            // Show error message
                            Snackbar.make(mainView, wEx.getMessage(), Snackbar.LENGTH_LONG).show();
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        appCompatActivity = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        appCompatActivity = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        if (getArguments() != null) {
            JsonReader jsonReader = new JsonReader(new StringReader(getArguments().getString("data", null)));
            location = LocationData.fromJson(jsonReader);
            try {
                jsonReader.close();
            } catch (IOException e) {
                Logger.writeLine(Log.ERROR, e);
            }

            if (location != null && wLoader == null)
                wLoader = new WeatherDataLoader(location, this, this);
        }

        if (savedInstanceState != null) {
            bgAlpha = savedInstanceState.getInt("alpha", 255);
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(getActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            if (Settings.useFollowGPS() && updateLocation()) {
                                // Setup loader from updated location
                                wLoader = new WeatherDataLoader(WeatherNowFragment.this.location,
                                        WeatherNowFragment.this, WeatherNowFragment.this);

                                refreshWeather(false);
                            }

                            new AsyncTask<Void>().await(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    return Tasks.await(mFusedLocationClient.removeLocationUpdates(mLocCallback));
                                }
                            });
                        }
                    });
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (Settings.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = new WeatherDataLoader(WeatherNowFragment.this.location,
                                WeatherNowFragment.this, WeatherNowFragment.this);

                        refreshWeather(false);
                    }
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

        loaded = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weather_now, container, false);

        // Setup ActionBar
        setHasOptionsMenu(true);

        refreshLayout = (SwipeRefreshLayout) view;
        mainView = view.findViewById(R.id.fragment_weather_now);
        mainView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (bgImageView != null) {
                    int alpha = 255 - (int) (255 * 1.25 * scrollY / (v.getChildAt(0).getHeight() - v.getHeight()));
                    if (alpha >= 0)
                        bgImageView.setImageAlpha(bgAlpha = alpha);
                    else
                        bgImageView.setImageAlpha(bgAlpha = 0);
                }
            }
        });
        bgImageView = view.findViewById(R.id.image_view);
        // Condition
        locationName = view.findViewById(R.id.label_location_name);
        updateTime = view.findViewById(R.id.label_updatetime);
        weatherIcon = view.findViewById(R.id.weather_icon);
        weatherCondition = view.findViewById(R.id.weather_condition);
        weatherTemp = view.findViewById(R.id.weather_temp);
        // Details
        detailsPanel = view.findViewById(R.id.details_panel);
        humidity = view.findViewById(R.id.humidity);
        pressureState = view.findViewById(R.id.pressure_state);
        pressure = view.findViewById(R.id.pressure);
        visiblity = view.findViewById(R.id.visibility_val);
        feelslike = view.findViewById(R.id.feelslike);
        windDirection = view.findViewById(R.id.wind_direction);
        windSpeed = view.findViewById(R.id.wind_speed);
        sunrise = view.findViewById(R.id.sunrise_time);
        sunset = view.findViewById(R.id.sunset_time);
        // Forecast
        forecastPanel = view.findViewById(R.id.forecast_panel);
        forecastPanel.setVisibility(View.INVISIBLE);
        forecastView = view.findViewById(R.id.forecast_view);
        // Additional Details
        forecastSwitch = view.findViewById(R.id.forecast_switch);
        forecastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                forecastSwitch.setText(isChecked ?
                        appCompatActivity.getString(R.string.switch_details) : appCompatActivity.getString(R.string.switch_daily));
                forecastView.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                txtForecastView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        forecastSwitch.setVisibility(View.GONE);
        txtForecastView = view.findViewById(R.id.txt_forecast_viewpgr);
        txtForecastView.setAdapter(new TextForecastPagerAdapter(this.getActivity(), new ArrayList<TextForecastItemViewModel>()));
        txtForecastView.setVisibility(View.GONE);
        hrforecastPanel = view.findViewById(R.id.hourly_forecast_panel);
        hrforecastPanel.setVisibility(View.GONE);
        hrforecastView = view.findViewById(R.id.hourly_forecast_view);
        precipitationPanel = view.findViewById(R.id.precipitation_card);
        precipitationPanel.setVisibility(View.GONE);
        chanceLabel = view.findViewById(R.id.chance_label);
        chance = view.findViewById(R.id.chance_val);
        cloudinessLabel = view.findViewById(R.id.cloudiness_label);
        cloudiness = view.findViewById(R.id.cloudiness);
        qpfRain = view.findViewById(R.id.qpf_rain_val);
        qpfSnow = view.findViewById(R.id.qpf_snow_val);
        // Alerts
        alertButton = view.findViewById(R.id.alert_button);
        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show Alert Fragment
                if (weatherView.getExtras().getAlerts().size() > 0)
                    appCompatActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, WeatherAlertsFragment.newInstance(location, weatherView))
                            .hide(WeatherNowFragment.this)
                            .addToBackStack(null)
                            .commit();
            }
        });
        alertButton.setVisibility(View.INVISIBLE);

        // Cloudiness only supported by OWM
        cloudinessLabel.setVisibility(View.GONE);
        cloudiness.setVisibility(View.GONE);

        forecastView.setHasFixedSize(true);
        forecastAdapter = new ForecastItemAdapter(new ArrayList<ForecastItemViewModel>());
        forecastView.setAdapter(forecastAdapter);

        hrforecastView.setHasFixedSize(true);
        hrforecastAdapter = new HourlyForecastItemAdapter(new ArrayList<HourlyForecastItemViewModel>());
        hrforecastView.setAdapter(hrforecastAdapter);

        // SwipeRefresh
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
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

        // Nav Header View
        navheader = ((NavigationView) getActivity().findViewById(R.id.nav_view)).getHeaderView(0);
        navLocation = navheader.findViewById(R.id.nav_location);
        navWeatherTemp = navheader.findViewById(R.id.nav_weathertemp);

        weatherCredit = view.findViewById(R.id.weather_credit);

        loaded = true;
        refreshLayout.setRefreshing(true);

        return view;
    }

    private void adjustDetailsLayout() {
        if (appCompatActivity != null && isLargeTablet(appCompatActivity)) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = WeatherNowFragment.this.getView();

                    if (view == null)
                        return;

                    GridLayout panel = (GridLayout) detailsPanel;

                    // Minimum width for ea. card
                    int minWidth = 600;
                    // Size of the view
                    int viewWidth = view.getWidth() - ViewCompat.getPaddingEnd(panel) - ViewCompat.getPaddingStart(panel);
                    // Available columns based on min card width
                    int availColumns = (viewWidth / minWidth) == 0 ? 1 : viewWidth / minWidth;
                    // Maximum columns to use
                    int maxColumns = (availColumns > panel.getChildCount()) ? panel.getChildCount() : availColumns;

                    int freeSpace = viewWidth - (minWidth * maxColumns);
                    // Increase card width to fill available space
                    int itemWidth = minWidth + (freeSpace / maxColumns);

                    // Adjust GridLayout
                    // Start
                    int currCol = 0;
                    int currRow = 0;
                    for (int i = 0; i < panel.getChildCount(); i++) {
                        View innerView = panel.getChildAt(i);

                        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                                GridLayout.spec(currRow, 1.0f),
                                GridLayout.spec(currCol, 1.0f));
                        layoutParams.width = 0;
                        int paddingVert = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, innerView.getContext().getResources().getDisplayMetrics());
                        ViewCompat.setPaddingRelative(view, paddingVert, 0, paddingVert, 0); // s, t, e, b
                        view.setLayoutParams(layoutParams);
                        if (currCol == maxColumns - 1) {
                            currCol = 0;
                            currRow++;
                        } else {
                            currCol++;
                        }
                    }
                    panel.setRowCount(GridLayout.UNDEFINED);
                    panel.setColumnCount(maxColumns);
                }
            });
        }
    }

    private void resizeAlertPanel() {
        if (appCompatActivity != null && isLargeTablet(appCompatActivity)) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = WeatherNowFragment.this.getView();

                    if (view == null || view.getWidth() <= 0 || alertButton.getVisibility() != View.VISIBLE)
                        return;

                    int viewWidth = view.getWidth();

                    if (viewWidth <= 600)
                        alertButton.getLayoutParams().width = viewWidth;
                    else if (viewWidth <= 1200)
                        alertButton.getLayoutParams().width = (int) (viewWidth * (0.75));
                    else
                        alertButton.getLayoutParams().width = (int) (viewWidth * (0.50));
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    private void resume() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                /* Update view on resume
                 * ex. If temperature unit changed
                 */
                LocationData homeData = Settings.getHomeData();

                // Did home change?
                boolean homeChanged = false;
                if (location != null && getFragmentManager().getBackStackEntryCount() == 0) {
                    if (!location.equals(homeData) && "home".equals(getTag())) {
                        location = homeData;
                        wLoader = null;
                        homeChanged = true;
                    }
                }

                // New Page = loaded - true
                // Navigating back to frag = !loaded - false
                if (loaded || homeChanged || wLoader == null) {
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
                            int ttl = -1;
                            try {
                                ttl = Integer.parseInt(weather.getTtl());
                            } catch (NumberFormatException ex) {
                                ttl = Settings.DEFAULTINTERVAL;
                            }
                            Duration span = Duration.between(ZonedDateTime.now(), weather.getUpdateTime()).abs();
                            if (span.toMinutes() > ttl) {
                                refreshWeather(false);
                            } else {
                                weatherView.updateView(wLoader.getWeather());
                                updateView(weatherView);
                                loaded = true;
                            }
                        }
                    }
                }

                return null;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Go straight to alerts here
        if (getArguments() != null && getArguments().getBoolean(WeatherWidgetService.ACTION_SHOWALERTS, false)) {
            // Remove key from Arguments
            getArguments().remove(WeatherWidgetService.ACTION_SHOWALERTS);

            // Show Alert Fragment
            appCompatActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, WeatherAlertsFragment.newInstance(location))
                    .hide(this)
                    .addToBackStack(null)
                    .commit();

            return;
        }

        // Don't resume if fragment is hidden
        if (this.isHidden())
            return;
        else
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });

        // Title
        if (appCompatActivity != null)
            appCompatActivity.getSupportActionBar().setTitle(R.string.title_activity_weather_now);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && weatherView != null && this.isVisible()) {
            updateNavHeader(weatherView);
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });
        } else if (hidden) {
            loaded = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        loaded = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putInt("alpha", bgAlpha);

        super.onSaveInstanceState(outState);
    }

    private void restore() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                boolean forceRefresh = false;

                // GPS Follow location
                if (Settings.useFollowGPS() && (location == null || location.getLocationType() == LocationType.GPS)) {
                    LocationData locData = Settings.getLastGPSLocData();

                    if (locData == null) {
                        // Update location if not setup
                        updateLocation();
                        wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);
                        forceRefresh = true;
                    } else {
                        // Reset locdata if source is different
                        if (!Settings.getAPI().equals(locData.getSource()))
                            Settings.saveLastGPSLocData(new LocationData());

                        if (updateLocation()) {
                            // Setup loader from updated location
                            wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);
                            forceRefresh = true;
                        } else {
                            // Setup loader saved location data
                            location = locData;
                            wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);
                        }
                    }
                } else if (wLoader == null) {
                    // Weather was loaded before. Lets load it up...
                    location = Settings.getHomeData();
                    wLoader = new WeatherDataLoader(location, WeatherNowFragment.this, WeatherNowFragment.this);
                }

                // Load up weather data
                refreshWeather(forceRefresh);

                return null;
            }
        });
    }

    private void refreshWeather(final boolean forceRefresh) {
        if (appCompatActivity != null) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(true);
                }
            });
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    wLoader.loadWeatherData(forceRefresh);
                    return null;
                }
            });
        }
    }

    private void updateView(final WeatherNowViewModel weatherView) {
        if (appCompatActivity != null) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Background
                    refreshLayout.setBackground(new ColorDrawable(weatherView.getPendingBackground()));
                    bgImageView.setImageAlpha(bgAlpha);
                    Glide.with(appCompatActivity)
                            .load(weatherView.getBackground())
                            .apply(new RequestOptions().centerCrop())
                            .into(bgImageView);

                    // Actionbar & StatusBar
                    appCompatActivity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(weatherView.getPendingBackground()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        int r = Color.red(weatherView.getPendingBackground());
                        int g = Color.green(weatherView.getPendingBackground());
                        int b = Color.blue(weatherView.getPendingBackground());
                        appCompatActivity.getWindow().setStatusBarColor(Color.argb(255,
                                (int) (r * 0.75),
                                (int) (g * 0.75),
                                (int) (b * 0.75)));
                    }

                    // Location
                    locationName.setText(weatherView.getLocation());

                    // Date Updated
                    updateTime.setText(weatherView.getUpdateDate());

                    // Update Current Condition
                    weatherTemp.setText(weatherView.getCurTemp());
                    weatherCondition.setText(weatherView.getCurCondition());
                    weatherIcon.setText(weatherView.getWeatherIcon());

                    // WeatherDetails
                    // Astronomy
                    sunrise.setText(weatherView.getSunrise());
                    sunset.setText(weatherView.getSunset());

                    // Wind
                    feelslike.setText(weatherView.getWindChill());
                    windSpeed.setText(weatherView.getWindSpeed());
                    windDirection.setRotation(weatherView.getWindDirection());

                    // Atmosphere
                    humidity.setText(weatherView.getHumidity());
                    pressure.setText(weatherView.getPressure());

                    pressureState.setVisibility(weatherView.getRisingVisiblity());

                    visiblity.setText(weatherView.getVisibility());

                    // Add UI elements
                    forecastAdapter.updateItems(weatherView.getForecasts());
                    forecastPanel.setVisibility(View.VISIBLE);

                    // Additional Details
                    if (weatherView.getExtras().getHourlyForecast().size() >= 1) {
                        hrforecastAdapter.updateItems(weatherView.getExtras().getHourlyForecast());
                        hrforecastPanel.setVisibility(View.VISIBLE);
                    } else {
                        hrforecastPanel.setVisibility(View.GONE);
                    }

                    if (weatherView.getExtras().getTextForecast().size() >= 1) {
                        forecastSwitch.setVisibility(View.VISIBLE);
                        ((TextForecastPagerAdapter) txtForecastView.getAdapter()).updateDataset(weatherView.getExtras().getTextForecast());
                    } else {
                        forecastSwitch.setVisibility(View.GONE);
                    }

                    if (!StringUtils.isNullOrWhitespace(weatherView.getExtras().getChance())) {
                        cloudiness.setText(weatherView.getExtras().getChance());
                        chance.setText(weatherView.getExtras().getChance());
                        qpfRain.setText(weatherView.getExtras().getQpfRain());
                        qpfSnow.setText(weatherView.getExtras().getQpfSnow());

                        if (!WeatherAPI.METNO.equals(Settings.getAPI())) {
                            precipitationPanel.setVisibility(View.VISIBLE);

                            if (isLargeTablet(appCompatActivity)) {
                                // Add back panel if not present
                                GridLayout panel = (GridLayout) detailsPanel;
                                int childIdx = panel.indexOfChild(panel.findViewById(R.id.precipitation_card));
                                if (childIdx < 0)
                                    panel.addView(precipitationPanel, 0);
                            }
                        } else {
                            if (isLargeTablet(appCompatActivity)) {
                                GridLayout panel = (GridLayout) detailsPanel;
                                panel.removeView(panel.findViewById(R.id.precipitation_card));
                            } else {
                                precipitationPanel.setVisibility(View.GONE);
                            }
                        }

                        if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                            chanceLabel.setVisibility(View.GONE);
                            chance.setVisibility(View.GONE);

                            cloudinessLabel.setVisibility(View.VISIBLE);
                            cloudiness.setVisibility(View.VISIBLE);
                        } else {
                            chanceLabel.setVisibility(View.VISIBLE);
                            chance.setVisibility(View.VISIBLE);

                            cloudinessLabel.setVisibility(View.GONE);
                            cloudiness.setVisibility(View.GONE);
                        }
                    } else {
                        if (isLargeTablet(appCompatActivity)) {
                            GridLayout panel = (GridLayout) detailsPanel;
                            panel.removeView(panel.findViewById(R.id.precipitation_card));
                        } else {
                            precipitationPanel.setVisibility(View.GONE);
                        }

                        cloudinessLabel.setVisibility(View.GONE);
                        cloudiness.setVisibility(View.GONE);
                    }

                    // Alerts
                    if (wm.supportsAlerts() && weatherView.getExtras().getAlerts().size() > 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            alertButton.setBackgroundTintList(ColorStateList.valueOf(Colors.ORANGERED));
                        } else {
                            Drawable origDrawable = ContextCompat.getDrawable(appCompatActivity, R.drawable.light_round_corner_bg);
                            Drawable compatDrawable = DrawableCompat.wrap(origDrawable);
                            DrawableCompat.setTint(compatDrawable, Colors.ORANGERED);
                            alertButton.setBackground(compatDrawable);
                        }

                        alertButton.setVisibility(View.VISIBLE);
                        resizeAlertPanel();
                    } else {
                        alertButton.setVisibility(View.INVISIBLE);
                    }

                    // Fix DetailsLayout
                    adjustDetailsLayout();

                    // Nav Header View
                    updateNavHeader(weatherView);

                    weatherCredit.setText(weatherView.getWeatherCredit());
                }
            });
        }
    }

    private void updateNavHeader(WeatherNowViewModel weatherView) {
        navheader.setBackground(new ColorDrawable(weatherView.getPendingBackground()));
        navLocation.setText(weatherView.getLocation());
        navWeatherTemp.setText(weatherView.getCurTemp());
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (appCompatActivity != null && Settings.useFollowGPS() && (location == null || location.getLocationType() == LocationType.GPS)) {
                    if (ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(appCompatActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
                        return false;
                    }

                    if (!Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
                        Looper.prepare();
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
                                    return Tasks.await(mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocCallback, null));
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
                        LocationManager locMan = null;
                        if (appCompatActivity != null)
                            locMan = (LocationManager) appCompatActivity.getSystemService(Context.LOCATION_SERVICE);
                        boolean isGPSEnabled = false;
                        boolean isNetEnabled = false;
                        if (locMan != null) {
                            isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                            isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                        }

                        if (isGPSEnabled || isNetEnabled) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);

                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);

                            if (location == null)
                                locMan.requestSingleUpdate(provider, mLocListnr, null);
                        } else {
                            appCompatActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(appCompatActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    if (location != null) {
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

                        view = wm.getLocation(location);
                        if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        // Save oldkey
                        String oldkey = lastGPSLocData.getQuery();

                        // Save location as last known
                        lastGPSLocData.setData(view, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        App.getInstance().getAppContext().startService(
                                new Intent(App.getInstance().getAppContext(), WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));

                        WeatherNowFragment.this.location = lastGPSLocData;
                        mLocation = location;

                        // Update widget ids for location
                        if (oldkey != null && WidgetUtils.exists(oldkey)) {
                            WidgetUtils.updateWidgetIds(oldkey, lastGPSLocData);
                        }

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
                    Toast.makeText(appCompatActivity, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                break;
        }
    }
}
