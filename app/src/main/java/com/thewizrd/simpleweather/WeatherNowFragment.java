package com.thewizrd.simpleweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.stream.JsonReader;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.helpers.WeatherViewLoadedListener;
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
import com.thewizrd.simpleweather.adapters.DetailItemAdapter;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastGraphPagerAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter;
import com.thewizrd.simpleweather.controls.SunPhaseView;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.LocationPanelOffsetDecoration;
import com.thewizrd.simpleweather.helpers.WindowColorsInterface;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBuilder;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WeatherNowFragment extends Fragment implements WeatherLoadedListenerInterface,
        WeatherErrorListenerInterface, ActivityCompat.OnRequestPermissionsResultCallback {
    private LocationData location = null;
    private boolean loaded = false;
    private int bgAlpha = 255;

    private WeatherManager wm;
    private WeatherDataLoader wLoader = null;
    private WeatherNowViewModel weatherView = null;
    private AppCompatActivity mActivity;
    private WindowColorsInterface mWindowColorsIface;
    private WeatherViewLoadedListener mCallback;
    private CancellationTokenSource cts;

    // Views
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private TextView mTitleView;
    private ImageView mImageView;
    private float mAppBarElevation;
    private SwipeRefreshLayout refreshLayout;
    private NestedScrollView scrollView;
    private View conditionPanel;
    private BitmapImageViewTarget imageViewTarget;
    // Condition
    private TextView updateTime;
    private TextView weatherIcon;
    private TextView weatherCondition;
    private TextView weatherTemp;
    private TextView bgAttribution;
    // Details
    private RecyclerView detailsContainer;
    private DetailItemAdapter detailsAdapter;
    private GridLayoutManager mLayoutManager;
    // Forecast
    private ConstraintLayout forecastPanel;
    private RecyclerView forecastView;
    private ForecastItemAdapter forecastAdapter;
    // Additional Details
    private ConstraintLayout hrforecastPanel;
    private SwitchCompat hrforecastSwitch;
    private RecyclerView hrforecastView;
    private HourlyForecastItemAdapter hrforecastAdapter;
    private ViewPager hrForecastGraphView;
    private HourlyForecastGraphPagerAdapter hrGraphAdapter;
    private SunPhaseView sunView;
    // Alerts
    private View alertButton;
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

    private void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    public void onWeatherLoaded(final LocationData location, final Weather weather) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (cts.getToken().isCancellationRequested())
                    return;

                if (weather != null && weather.isValid()) {
                    wm.updateWeather(weather);
                    weatherView.updateView(weather);
                    updateView(weatherView);
                    if (mCallback != null) mCallback.onWeatherViewUpdated(weatherView);

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

    @Override
    public void onWeatherError(final WeatherException wEx) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (cts.getToken().isCancellationRequested())
                    return;

                switch (wEx.getErrorStatus()) {
                    case NETWORKERROR:
                    case NOWEATHER:
                        // Show error message and prompt to refresh
                        Snackbar snackBar = Snackbar.make(scrollView, wEx.getMessage(), Snackbar.LENGTH_LONG);
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
                        Snackbar.make(scrollView, wEx.getMessage(), Snackbar.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
        mCallback = (WeatherViewLoadedListener) context;
        mWindowColorsIface = (WindowColorsInterface) context;
    }

    @Override
    public void onDestroy() {
        // Cancel pending actions
        if (cts != null) cts.cancel();

        super.onDestroy();
        wLoader = null;
        weatherView = null;
        mActivity = null;
        mCallback = null;
        mWindowColorsIface = null;
        cts = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        wLoader = null;
        weatherView = null;
        mActivity = null;
        mCallback = null;
        mWindowColorsIface = null;
        cts = null;
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

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(mActivity);
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            if (cts.getToken().isCancellationRequested())
                                return;

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
                    if (cts.getToken().isCancellationRequested())
                        return;

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
        // Request focus away from RecyclerView
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        // Setup ActionBar
        setHasOptionsMenu(true);
        mAppBarElevation = mActivity.getResources().getDimension(R.dimen.appbar_elevation);
        mAppBarLayout = view.findViewById(R.id.app_bar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAppBarLayout.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    // L, T, R, B
                    outline.setRect(0, mAppBarLayout.getHeight(), view.getWidth(), mAppBarLayout.getHeight() + 1);
                    outline.setAlpha(0.5f);
                }
            });
        }
        ViewCompat.setElevation(mAppBarLayout, 0);
        mImageView = view.findViewById(R.id.image_view);
        mTitleView = view.findViewById(R.id.toolbar_title);
        mTitleView.setText(R.string.title_activity_weather_now);
        mToolbar = view.findViewById(R.id.toolbar);
        imageViewTarget = new BitmapImageViewTarget(mImageView) {
            /* Testing Only
            @Override
            protected void setResource(final Bitmap resource) {
                super.setResource(resource);
                if (resource != null) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            int color = weatherView.getPendingBackground();
                            Palette p = Palette.from(resource).generate();
                            Palette.Swatch swatch = ColorsUtils.getPreferredSwatch(p);
                            if (swatch != null) color = swatch.getRgb();

                            final int finalColor = color;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mWindowColorsIface != null)
                                        mWindowColorsIface.setWindowBarColors(finalColor);
                                    // Background
                                    View mainView = WeatherNowFragment.this.getView();
                                    if (mainView != null) {
                                        mainView.setBackgroundColor(finalColor);
                                    }
                                }
                            });
                        }
                    });
                }
            }
            */
        };

        conditionPanel = view.findViewById(R.id.condition_panel);

        if (mWindowColorsIface != null)
            mWindowColorsIface.setWindowBarColors(Colors.SIMPLEBLUE);

        refreshLayout = view.findViewById(R.id.refresh_layout);
        scrollView = view.findViewById(R.id.fragment_weather_now);
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(final NestedScrollView v, int scrollX, final int scrollY, int oldScrollX, int oldScrollY) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mAppBarLayout != null) {
                            if (scrollY != 0 || v.canScrollVertically(-1)) {
                                ViewCompat.setElevation(mAppBarLayout, mAppBarElevation);
                            } else {
                                ViewCompat.setElevation(mAppBarLayout, 0);
                            }
                        }
                        if (mImageView != null) {
                            // Default adj = 1.25
                            float adj = 2.5f;
                            int alpha = 255 - (int) (255 * adj * scrollY / (v.getChildAt(0).getHeight() - v.getHeight()));
                            if (alpha >= 0)
                                mImageView.setImageAlpha(bgAlpha = alpha);
                            else
                                mImageView.setImageAlpha(bgAlpha = 0);
                        }
                    }
                });
            }
        });
        // Condition
        updateTime = view.findViewById(R.id.label_updatetime);
        weatherIcon = view.findViewById(R.id.weather_icon);
        weatherCondition = view.findViewById(R.id.weather_condition);
        weatherTemp = view.findViewById(R.id.weather_temp);
        bgAttribution = view.findViewById(R.id.bg_attribution);
        bgAttribution.setMovementMethod(LinkMovementMethod.getInstance());
        // Details
        detailsContainer = view.findViewById(R.id.details_container);
        detailsContainer.setHasFixedSize(false);
        mLayoutManager = new GridLayoutManager(mActivity, 4, LinearLayoutManager.VERTICAL, false);
        detailsContainer.setLayoutManager(mLayoutManager);

        int horizMargin = 16;
        if (ActivityUtils.isLargeTablet(mActivity)) horizMargin = 24;

        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, horizMargin, mActivity.getResources().getDisplayMetrics());

        detailsContainer.addItemDecoration(new LocationPanelOffsetDecoration(margin));
        detailsAdapter = new DetailItemAdapter();
        detailsContainer.setAdapter(detailsAdapter);

        // Forecast
        forecastPanel = view.findViewById(R.id.forecast_panel);
        forecastPanel.setVisibility(View.INVISIBLE);
        forecastView = view.findViewById(R.id.forecast_view);
        // Additional Details
        hrforecastPanel = view.findViewById(R.id.hourly_forecast_panel);
        hrforecastPanel.setVisibility(View.GONE);
        hrforecastView = view.findViewById(R.id.hourly_forecast_view);
        // Alerts
        alertButton = view.findViewById(R.id.alert_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alertButton.setBackgroundTintList(ColorStateList.valueOf(Colors.ORANGERED));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable drawable = alertButton.getBackground().mutate();
            drawable.setColorFilter(Colors.ORANGERED, PorterDuff.Mode.SRC_IN);
            alertButton.setBackground(drawable);
        } else {
            Drawable origDrawable = ContextCompat.getDrawable(mActivity, R.drawable.light_round_corner_bg);
            Drawable compatDrawable = DrawableCompat.wrap(origDrawable);
            DrawableCompat.setTint(compatDrawable, Colors.ORANGERED);
            alertButton.setBackground(compatDrawable);
        }

        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show Alert Fragment
                if (weatherView.getExtras().getAlerts().size() > 0)
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, WeatherAlertsFragment.newInstance(location, weatherView))
                            .hide(WeatherNowFragment.this)
                            .addToBackStack(null)
                            .commit();
            }
        });
        alertButton.setVisibility(View.INVISIBLE);

        forecastView.setHasFixedSize(true);
        forecastView.addItemDecoration(new DividerItemDecoration(mActivity, DividerItemDecoration.HORIZONTAL));
        forecastAdapter = new ForecastItemAdapter();
        forecastView.setAdapter(forecastAdapter);

        hrforecastView.setHasFixedSize(true);
        hrforecastView.addItemDecoration(new DividerItemDecoration(mActivity, DividerItemDecoration.HORIZONTAL));
        hrforecastAdapter = new HourlyForecastItemAdapter();
        hrforecastView.setAdapter(hrforecastAdapter);

        hrForecastGraphView = view.findViewById(R.id.hourly_forecast_viewpgr);
        hrGraphAdapter = new HourlyForecastGraphPagerAdapter(mActivity);
        hrForecastGraphView.setAdapter(hrGraphAdapter);

        hrforecastSwitch = view.findViewById(R.id.hourly_forecast_switch);
        hrforecastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hrforecastSwitch.setText(isChecked ? "Details" : "Summary");
                hrforecastView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                hrForecastGraphView.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            }
        });
        hrforecastSwitch.setChecked(true);

        sunView = view.findViewById(R.id.sun_phase_view);

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

        weatherCredit = view.findViewById(R.id.weather_credit);

        loaded = true;
        refreshLayout.setRefreshing(true);

        return view;
    }

    private void resizeSunPhasePanel() {
        if (mActivity != null && ActivityUtils.isLargeTablet(mActivity)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = WeatherNowFragment.this.getView();

                    if (view == null || view.getWidth() <= 0)
                        return;

                    int viewWidth = view.getWidth();

                    if (viewWidth <= 600)
                        sunView.getLayoutParams().width = viewWidth;
                    else if (viewWidth <= 1200)
                        sunView.getLayoutParams().width = (int) (viewWidth * (0.75));
                    else
                        sunView.getLayoutParams().width = (int) (viewWidth * (0.50));
                }
            });
        }
    }

    private void resizeAlertPanel() {
        if (mActivity != null && ActivityUtils.isLargeTablet(mActivity)) {
            runOnUiThread(new Runnable() {
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
        cts = new CancellationTokenSource();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (scrollView != null) scrollView.scrollTo(0, 0);
            }
        });

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
                                if (mCallback != null) mCallback.onWeatherViewUpdated(weatherView);
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

        adjustConditionPanelLayout();
        adjustDetailsLayout();

        // Go straight to alerts here
        if (getArguments() != null && getArguments().getBoolean(WeatherWidgetService.ACTION_SHOWALERTS, false)) {
            // Remove key from Arguments
            getArguments().remove(WeatherWidgetService.ACTION_SHOWALERTS);

            // Show Alert Fragment
            mActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, WeatherAlertsFragment.newInstance(location))
                    .hide(this)
                    .addToBackStack(null)
                    .commit();

            return;
        }

        // Don't resume if fragment is hidden
        if (this.isHidden())
            return;
        else if (weatherView != null)
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });
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
                    resume();
                }
            });
        } else if (hidden) {
            loaded = false;
        }
    }

    @Override
    public void onPause() {
        // Cancel pending actions
        if (cts != null) {
            cts.cancel();
            refreshLayout.setRefreshing(false);
        }

        super.onPause();
        loaded = false;
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

                if (cts.getToken().isCancellationRequested())
                    return null;

                // Load up weather data
                refreshWeather(forceRefresh);

                return null;
            }
        });
    }

    private void refreshWeather(final boolean forceRefresh) {
        if (mActivity != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(true);
                }
            });
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    if (cts != null && !cts.getToken().isCancellationRequested())
                        wLoader.loadWeatherData(forceRefresh);
                    return null;
                }
            });
        }
    }

    private void adjustConditionPanelLayout() {
        conditionPanel.post(new Runnable() {
            @Override
            public void run() {
                if (mActivity != null) {
                    View bottomAppBar = mActivity.findViewById(R.id.bottom_nav_bar);
                    float height = Math.abs((mAppBarLayout.getY() + mAppBarLayout.getHeight() / 2f - mAppBarElevation) - (bottomAppBar.getY() - bottomAppBar.getHeight()));
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) conditionPanel.getLayoutParams();
                    lp.height = (int) height;
                    lp.bottomMargin = bottomAppBar.getHeight();
                }
            }
        });
    }

    private void adjustDetailsLayout() {
        if (mActivity != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View mainView = WeatherNowFragment.this.getView();

                    if (mainView == null)
                        return;

                    DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
                    float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

                    /*
                    int maxNumOfCol = 4;

                    if (dpWidth >= 600)
                        maxNumOfCol = 8;
                    else if (dpWidth >= 840)
                        maxNumOfCol = 12;
                    */

                    // Minimum width for ea. card
                    int minWidth = (int) ActivityUtils.dpToPx(mActivity, 125f); // Default: 125f
                    // Available columns based on min card width
                    int availColumns = ((int) (dpWidth / minWidth)) <= 1 ? 3 : (int) (dpWidth / minWidth);

                    mLayoutManager.setSpanCount(availColumns);
                }
            });
        }
    }

    private void updateView(final WeatherNowViewModel weatherView) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (cts.getToken().isCancellationRequested())
                    return;

                // Background
                View mainView = WeatherNowFragment.this.getView();
                if (mainView != null) {
                    mainView.setBackgroundColor(weatherView.getPendingBackground());
                }

                mImageView.setImageAlpha(bgAlpha);
                Glide.with(mActivity)
                        .asBitmap()
                        .load(weatherView.getBackground())
                        .apply(new RequestOptions().centerCrop())
                        .into(imageViewTarget);

                // Location
                mTitleView.setText(weatherView.getLocation());

                // Date Updated
                updateTime.setText(weatherView.getUpdateDate());

                // Update Current Condition
                weatherTemp.setText(weatherView.getCurTemp());
                weatherCondition.setText(weatherView.getCurCondition());
                weatherIcon.setText(weatherView.getWeatherIcon());
                bgAttribution.setText(getBackgroundAttribution(weatherView.getBackground()));

                // WeatherDetails
                detailsAdapter.updateItems(weatherView);

                // Add UI elements
                forecastAdapter.updateItems(weatherView.getForecasts());
                forecastPanel.setVisibility(View.VISIBLE);

                if (WeatherAPI.HERE.equals(weatherView.getWeatherSource())
                        || WeatherAPI.WEATHERUNDERGROUND.equals(weatherView.getWeatherSource())) {
                    forecastAdapter.setOnClickListener(new RecyclerOnClickListenerInterface() {
                        @Override
                        public void onClick(View view, int position) {
                            Fragment fragment = WeatherDetailsFragment.newInstance(location, weatherView, false);
                            Bundle args = new Bundle();
                            args.putInt("position", position);
                            fragment.setArguments(args);

                            mActivity.getSupportFragmentManager().beginTransaction()
                                    .add(R.id.fragment_container, fragment)
                                    .hide(WeatherNowFragment.this)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                } else {
                    forecastAdapter.setOnClickListener(null);
                }

                // Additional Details
                if (weatherView.getExtras().getHourlyForecast().size() >= 1) {
                    hrforecastAdapter.updateItems(weatherView.getExtras().getHourlyForecast());
                    hrforecastPanel.setVisibility(View.VISIBLE);

                    hrGraphAdapter.updateDataset(weatherView.getExtras().getHourlyForecast());

                    if (!WeatherAPI.YAHOO.equals(weatherView.getWeatherSource())) {
                        RecyclerOnClickListenerInterface onClickListener = new RecyclerOnClickListenerInterface() {
                            @Override
                            public void onClick(View view, int position) {
                                Fragment fragment = WeatherDetailsFragment.newInstance(location, weatherView, true);
                                Bundle args = new Bundle();
                                args.putInt("position", position);
                                fragment.setArguments(args);

                                mActivity.getSupportFragmentManager().beginTransaction()
                                        .add(R.id.fragment_container, fragment)
                                        .hide(WeatherNowFragment.this)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        };

                        hrGraphAdapter.setOnClickListener(onClickListener);
                        hrforecastAdapter.setOnClickListener(onClickListener);
                    }
                } else {
                    hrforecastAdapter.setOnClickListener(null);
                    hrGraphAdapter.setOnClickListener(null);
                    hrforecastPanel.setVisibility(View.GONE);
                }

                // Alerts
                if (wm.supportsAlerts() && weatherView.getExtras().getAlerts().size() > 0) {
                    alertButton.setVisibility(View.VISIBLE);
                    resizeAlertPanel();
                } else {
                    alertButton.setVisibility(View.INVISIBLE);
                }

                // Sun View
                resizeSunPhasePanel();
                DateTimeFormatter fmt;
                if (DateFormat.is24HourFormat(mActivity)) {
                    fmt = DateTimeFormatter.ofPattern("HH:mm");
                } else {
                    fmt = DateTimeFormatter.ofPattern("h:mm a");
                }
                sunView.setSunriseSetTimes(LocalTime.parse(weatherView.getSunrise(), fmt),
                        LocalTime.parse(weatherView.getSunset(), fmt),
                        location.getTzOffset());

                weatherCredit.setText(weatherView.getWeatherCredit());

                if (scrollView.getVisibility() != View.VISIBLE)
                    scrollView.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (mActivity != null && Settings.useFollowGPS() && (location == null || location.getLocationType() == LocationType.GPS)) {
                    if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
                        return false;
                    }

                    Location location = null;

                    if (cts.getToken().isCancellationRequested())
                        return false;

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() throws Exception {
                                return Tasks.await(mFusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
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
                        LocationManager locMan = null;
                        if (mActivity != null)
                            locMan = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
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

                        if (cts.getToken().isCancellationRequested())
                            return null;

                        view = wm.getLocation(location);
                        if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (cts.getToken().isCancellationRequested())
                            return false;

                        // Save oldkey
                        String oldkey = lastGPSLocData.getQuery();

                        // Save location as last known
                        lastGPSLocData.setData(view, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        WearableDataListenerService.enqueueWork(App.getInstance().getAppContext(),
                                new Intent(App.getInstance().getAppContext(), WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));

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

    private CharSequence getBackgroundAttribution(String backgroundURI) {
        CharSequence attrib = "";

        if (!StringUtils.isNullOrWhitespace(backgroundURI)) {
            if (backgroundURI.contains("DaySky")) {
                attrib = mActivity.getText(R.string.attrib_daysky);
            } else if (backgroundURI.contains("FoggySky")) {
                attrib = mActivity.getText(R.string.attrib_foggysky);
            } else if (backgroundURI.contains("NightSky")) {
                attrib = mActivity.getText(R.string.attrib_nightsky);
            } else if (backgroundURI.contains("PartlyCloudy-Day")) {
                attrib = mActivity.getText(R.string.attrib_ptcloudyday);
            } else if (backgroundURI.contains("RainyDay")) {
                attrib = mActivity.getText(R.string.attrib_rainyday);
            } else if (backgroundURI.contains("RainyNight")) {
                attrib = mActivity.getText(R.string.attrib_rainynt);
            } else if (backgroundURI.contains("Snow-Windy")) {
                attrib = mActivity.getText(R.string.attrib_snowwindy);
            } else if (backgroundURI.contains("Snow")) {
                attrib = mActivity.getText(R.string.attrib_snow);
            } else if (backgroundURI.contains("StormySky")) {
                attrib = mActivity.getText(R.string.attrib_stormy);
            } else if (backgroundURI.contains("Thunderstorm-Day")) {
                attrib = mActivity.getText(R.string.attrib_tstormday);
            } else if (backgroundURI.contains("Thunderstorm-Night")) {
                attrib = mActivity.getText(R.string.attrib_tstormnt);
            }
        }

        return attrib;
    }
}