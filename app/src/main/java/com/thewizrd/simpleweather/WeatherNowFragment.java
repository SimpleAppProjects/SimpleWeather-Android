package com.thewizrd.simpleweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableFloat;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.stream.JsonReader;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.helpers.WeatherViewLoadedListener;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DarkMode;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherErrorListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherLoadedListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.adapters.ColorModeRecyclerViewAdapter;
import com.thewizrd.simpleweather.adapters.DetailItemAdapter;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastGraphPagerAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter;
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView;
import com.thewizrd.simpleweather.controls.SunPhaseView;
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding;
import com.thewizrd.simpleweather.fragments.WindowColorFragment;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.LocationPanelOffsetDecoration;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBuilder;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherNowFragment extends WindowColorFragment
        implements WeatherLoadedListenerInterface, WeatherErrorListenerInterface {
    private LocationData location = null;
    private boolean loaded = false;
    private ObservableInt backgroundAlpha;
    private ObservableFloat gradientAlpha;

    private WeatherManager wm;
    private WeatherDataLoader wLoader = null;
    private WeatherNowViewModel weatherView = null;
    private androidx.databinding.DataBindingComponent dataBindingComponent =
            new WeatherFragmentDataBindingComponent(this);

    private AppCompatActivity mActivity;
    private SystemBarColorManager mSysBarColorsIface;
    private WeatherViewLoadedListener mCallback;
    private CancellationTokenSource cts;

    // Views
    private View mRootView;
    private AppBarLayout mAppBarLayout;
    private TextView mTitleView;
    private ImageView mImageView;
    private View mGradView;
    private float mAppBarElevation;
    private SwipeRefreshLayout refreshLayout;
    private ObservableNestedScrollView scrollView;
    private View conditionPanel;
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

    public WeatherNowFragment() {
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

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    public void onWeatherLoaded(final LocationData location, final Weather weather) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

                if (weather != null && weather.isValid()) {
                    weatherView.updateView(weather);

                    if (mCallback != null) mCallback.onWeatherViewUpdated(weatherView);

                    if (Settings.getHomeData().equals(location)) {
                        // Update widgets if they haven't been already
                        if (Duration.between(LocalDateTime.now(), Settings.getUpdateTime()).toMinutes() > Settings.getRefreshInterval()) {
                            WeatherWidgetService.enqueueWork(App.getInstance().getAppContext(), new Intent(App.getInstance().getAppContext(), WeatherWidgetService.class)
                                    .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
                        }

                        // Update ongoing notification if its not showing
                        if (Settings.showOngoingNotification() && !WeatherNotificationBuilder.isShowing()) {
                            WeatherNotificationService.enqueueWork(App.getInstance().getAppContext(), new Intent(App.getInstance().getAppContext(), WeatherNotificationService.class)
                                    .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION));
                        }
                    }

                    if (wm.supportsAlerts() && Settings.useAlerts()
                            && weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
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
                if (isCtsCancelRequested())
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
        mCallback = (WeatherViewLoadedListener) context;
        mSysBarColorsIface = (SystemBarColorManager) context;
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
        mSysBarColorsIface = null;
        cts = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        wLoader = null;
        weatherView = null;
        mActivity = null;
        mCallback = null;
        mSysBarColorsIface = null;
        cts = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        if (getArguments() != null) {
            try {
                JsonReader jsonReader = new JsonReader(new StringReader(getArguments().getString("data", null)));
                location = LocationData.fromJson(jsonReader);
                jsonReader.close();
            } catch (Exception e) {
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
                    if (isCtsCancelRequested())
                        return;

                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
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
        loaded = true;

        // Setup ViewModel
        weatherView = ViewModelProviders.of(this).get(WeatherNowViewModel.class);
        backgroundAlpha = new ObservableInt(0xFF); // int: 255
        gradientAlpha = new ObservableFloat(1.0f);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentWeatherNowBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false,
                dataBindingComponent);

        binding.setWeatherView(weatherView);
        binding.setBackgroundAlpha(backgroundAlpha);
        binding.setGradientAlpha(gradientAlpha);
        binding.setLifecycleOwner(this);

        View view = binding.getRoot();
        mRootView = view;
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
                    outline.setRect(view.getPaddingStart(), view.getHeight(), view.getWidth() + view.getPaddingEnd(), view.getHeight() + 1);
                    outline.setAlpha(0.5f);
                }
            });
        }
        ViewCompat.setElevation(mAppBarLayout, 0);

        ViewCompat.setOnApplyWindowInsetsListener(mAppBarLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        mGradView = view.findViewById(R.id.gradient_view);
        ViewCompat.setOnApplyWindowInsetsListener(mGradView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.topMargin = -insets.getSystemWindowInsetTop();
                layoutParams.bottomMargin = -insets.getSystemWindowInsetBottom();
                return insets;
            }
        });
        mImageView = view.findViewById(R.id.image_view);
        ViewCompat.setOnApplyWindowInsetsListener(mImageView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.topMargin = -insets.getSystemWindowInsetTop();
                layoutParams.bottomMargin = -insets.getSystemWindowInsetBottom();
                return insets;
            }
        });

        mTitleView = view.findViewById(R.id.toolbar_title);
        mTitleView.setText(R.string.title_activity_weather_now);

        conditionPanel = view.findViewById(R.id.condition_panel);

        refreshLayout = view.findViewById(R.id.refresh_layout);
        ViewCompat.setOnApplyWindowInsetsListener(refreshLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        scrollView = view.findViewById(R.id.fragment_weather_now);
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(final NestedScrollView v, int scrollX, final int scrollY, int oldScrollX, final int oldScrollY) {
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

                        final int currentNightMode = AppCompatDelegate.getDefaultNightMode();
                        if (currentNightMode != AppCompatDelegate.MODE_NIGHT_YES) {
                            // Default adj = 1.25
                            float adj = 2.5f;
                            int backAlpha = 0xFF - (int) (0xFF * adj * scrollY / (v.getChildAt(0).getHeight() - v.getHeight()));
                            float gradAlpha = 1.0f - (1.0f * adj * scrollY / (conditionPanel.getHeight()));
                            backgroundAlpha.set(Math.max(backAlpha, 0x25));
                            gradientAlpha.set(Math.max(gradAlpha, 0));
                        }

                        if (mSysBarColorsIface != null && mRootView.getBackground() instanceof ColorDrawable) {
                            int color = ((ColorDrawable) mRootView.getBackground()).getColor();

                            if (gradientAlpha.get() == 1.0f) {
                                mSysBarColorsIface.setSystemBarColors(Colors.TRANSPARENT, color);
                            } else if (gradientAlpha.get() == 0.0f) {
                                mSysBarColorsIface.setSystemBarColors(color);
                            }
                        }
                    }
                });
            }
        });
        scrollView.setOnFlingListener(new ObservableNestedScrollView.OnFlingListener() {
            private int oldScrollY;
            private int startvelocityY;

            /*
             * Values from OverScroller class
             */
            private final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
            private final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
            // Fling friction
            private float mFlingFriction = ViewConfiguration.getScrollFriction();
            private final float ppi = mActivity.getResources().getDisplayMetrics().density * 160.0f;
            private float mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                    * 39.37f // inch/meter
                    * ppi
                    * 0.84f; // look and feel tuning

            private double getSplineDeceleration(int velocity) {
                return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
            }

            private double getSplineFlingDistance(int velocity) {
                final double l = getSplineDeceleration(velocity);
                final double decelMinusOne = DECELERATION_RATE - 1.0;
                return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
            }
            /*
             * End of values from OverScroller class
             */

            @Override
            public void onFlingStarted(int startScrollY, int velocityY) {
                oldScrollY = startScrollY;
                startvelocityY = velocityY;
            }

            @Override
            public void onFlingStopped(int scrollY) {
                int condPnlHeight = conditionPanel.getHeight();
                int THRESHOLD = condPnlHeight / 2;
                int scrollOffset = scrollView.computeVerticalScrollOffset();
                int dY = scrollY - oldScrollY;
                boolean mScrollHandled = false;

                Log.d("ScrollView", String.format("onFlingStopped: height: %d; offset|scrollY: %d; prevScrollY: %d; dY: %d;", condPnlHeight, scrollOffset, oldScrollY, dY));

                if (dY < 0 && scrollOffset < condPnlHeight - THRESHOLD) {
                    scrollView.smoothScrollTo(0, 0);
                    mScrollHandled = true;
                } else if (scrollOffset < condPnlHeight && scrollOffset >= condPnlHeight - THRESHOLD) {
                    scrollView.smoothScrollTo(0, condPnlHeight);
                    mScrollHandled = true;
                } else if (dY > 0 && scrollOffset < condPnlHeight - THRESHOLD) {
                    scrollView.smoothScrollTo(0, condPnlHeight);
                    mScrollHandled = true;
                }

                if (!mScrollHandled && scrollOffset < condPnlHeight) {
                    int animDY = (int) getSplineFlingDistance(startvelocityY);
                    int animScrollY = oldScrollY + animDY;

                    Log.d("ScrollView", String.format("onFlingStopped: height: %d; animScrollY: %d; prevScrollY: %d; animDY: %d;", condPnlHeight, animScrollY, oldScrollY, animDY));

                    if (startvelocityY < 0 && animScrollY < condPnlHeight - THRESHOLD) {
                        scrollView.smoothScrollTo(0, 0);
                    } else if (animScrollY < condPnlHeight && animScrollY >= condPnlHeight - THRESHOLD) {
                        scrollView.smoothScrollTo(0, condPnlHeight);
                    } else if (startvelocityY > 0 && animScrollY < condPnlHeight - THRESHOLD) {
                        scrollView.smoothScrollTo(0, condPnlHeight);
                    }
                }
            }
        });
        scrollView.setTouchScrollListener(new ObservableNestedScrollView.OnTouchScrollChangeListener() {
            @Override
            public void onTouchScrollChange(int scrollY, int oldScrollY) {
                int condPnlHeight = conditionPanel.getHeight();
                int THRESHOLD = condPnlHeight / 2;
                int scrollOffset = scrollView.computeVerticalScrollOffset();
                int dY = scrollY - oldScrollY;

                Log.d("ScrollView", String.format("onTouchScrollChange: height: %d; offset: %d; scrollY: %d; prevScrollY: %d; dY: %d",
                        condPnlHeight, scrollOffset, scrollY, oldScrollY, dY));

                if (dY < 0 && scrollY < condPnlHeight - THRESHOLD) {
                    scrollView.smoothScrollTo(0, 0);
                } else if (scrollY < condPnlHeight && scrollY >= condPnlHeight - THRESHOLD) {
                    scrollView.smoothScrollTo(0, condPnlHeight);
                } else if (dY > 0 && scrollY < condPnlHeight) {
                    scrollView.smoothScrollTo(0, condPnlHeight);
                }
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
        mLayoutManager = new GridLayoutManager(mActivity, 4, RecyclerView.VERTICAL, false) {
            @Override
            // View should not scroll
            public boolean canScrollVertically() {
                return false;
            }
        };
        detailsContainer.setLayoutManager(mLayoutManager);

        int horizMargin = 16;
        if (ActivityUtils.isLargeTablet(mActivity)) horizMargin = 24;

        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, horizMargin, mActivity.getResources().getDisplayMetrics());

        detailsContainer.addItemDecoration(new LocationPanelOffsetDecoration(margin));
        detailsAdapter = new DetailItemAdapter();
        detailsContainer.setAdapter(detailsAdapter);

        // Disable touch events on container
        // View does not scroll
        detailsContainer.setFocusable(false);
        detailsContainer.setFocusableInTouchMode(false);
        detailsContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

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

        loaded = true;
        refreshLayout.setRefreshing(true);

        // Set property change listeners
        weatherView.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                updateView(weatherView);
            }
        });

        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Resize necessary views
        ViewTreeObserver observer = mRootView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                adjustConditionPanelLayout();
                adjustDetailsLayout();
                resizeAlertPanel();
                resizeSunPhasePanel();
            }
        });

        int currentNightMode = AppCompatDelegate.getDefaultNightMode();

        if (currentNightMode != AppCompatDelegate.MODE_NIGHT_YES) {
            mImageView.post(new Runnable() {
                @Override
                public void run() {
                    // Reload background image
                    if (weatherView != null) {
                        Glide.with(mActivity)
                                .load(weatherView.getBackground())
                                .apply(new RequestOptions().centerCrop()
                                        .format(DecodeFormat.PREFER_RGB_565)
                                        .skipMemoryCache(true))
                                .into(mImageView);
                    }
                }
            });
        } else {
            mImageView.setImageDrawable(null);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "WeatherNow: stopLocationUpdates: updates never requested, no-op.");
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

                    if (view == null || view.getWidth() <= 0)
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
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    private void resume() {
        cts = new CancellationTokenSource();

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
            adjustConditionPanelLayout();
            adjustDetailsLayout();

            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                mImageView.setImageDrawable(null);
                backgroundAlpha.set(0xFF);
                bgAttribution.setVisibility(View.INVISIBLE);
            }

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
        // Remove location updates to save battery.
        stopLocationUpdates();
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
                        if (!Settings.getAPI().equals(locData.getWeatherSource()))
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

                if (isCtsCancelRequested())
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
                    if (wLoader != null && !isCtsCancelRequested())
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
                    int height = mRootView.getHeight() - mAppBarLayout.getHeight();
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) conditionPanel.getLayoutParams();
                    lp.height = height;
                    conditionPanel.setLayoutParams(lp);
                }
            }
        });

        weatherIcon.post(new Runnable() {
            @Override
            public void run() {
                weatherIcon.setLayoutParams(weatherIcon.getLayoutParams());
            }
        });

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, 0);
            }
        });

        adjustGradientView();
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
                    int availColumns = ((int) (dpWidth / minWidth)) <= 1 ? 2 : (int) (dpWidth / minWidth);

                    mLayoutManager.setSpanCount(availColumns);
                }
            });
        }
    }

    private void adjustGradientView() {
        /*
         * NOTE
         *
         * BUG: Re-set the radius for the background_overlay drawable
         * For some reason the %p suffix does not work on pre-Lollipop devices
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mGradView.post(new Runnable() {
                @Override
                public void run() {
                    int height = mRootView.getHeight();
                    int width = mRootView.getWidth();

                    if (mGradView.getBackground() instanceof GradientDrawable && height > 0 && width > 0) {
                        GradientDrawable drawable = ((GradientDrawable) mGradView.getBackground().mutate());
                        float radius = 1.5f;
                        radius *= Math.min(width, height);
                        drawable.setGradientRadius(radius);
                    }
                }
            });
        }
    }

    @Override
    public void updateWindowColors() {
        // Background
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        int color;
        if (currentNightMode != AppCompatDelegate.MODE_NIGHT_YES) {
            if (weatherView != null && weatherView.getPendingBackground() != -1)
                color = weatherView.getPendingBackground();
            else
                color = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
        } else {
            color = Settings.getUserThemeMode() == DarkMode.AMOLED_DARK ?
                    Colors.BLACK :
                    ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
        }
        if (mSysBarColorsIface != null) {
            mSysBarColorsIface.setSystemBarColors(Colors.TRANSPARENT, color, color);
        }
        mRootView.setBackgroundColor(color);
    }

    private void updateView(final WeatherNowViewModel weatherView) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

                // Background
                updateWindowColors();

                int currentNightMode = AppCompatDelegate.getDefaultNightMode();

                if (currentNightMode != AppCompatDelegate.MODE_NIGHT_YES) {
                    Glide.with(mActivity)
                            .load(weatherView.getBackground())
                            .apply(new RequestOptions().centerCrop()
                                    .format(DecodeFormat.PREFER_RGB_565))
                            .into(mImageView);

                    bgAttribution.setVisibility(View.VISIBLE);
                } else {
                    mImageView.setImageDrawable(null);
                    backgroundAlpha.set(0xFF);

                    bgAttribution.setVisibility(View.INVISIBLE);
                }

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

                // Condition Panel & Scroll view
                adjustConditionPanelLayout();

                // Alerts
                resizeAlertPanel();

                // Sun View
                resizeSunPhasePanel();
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
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
                        return false;
                    }

                    Location location = null;

                    if (isCtsCancelRequested())
                        return false;

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() throws Exception {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
                                } catch (TimeoutException e) {
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
                            mLocationRequest.setInterval(10000);
                            mLocationRequest.setFastestInterval(1000);
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            mRequestingLocationUpdates = true;
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocCallback, Looper.getMainLooper());
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
                            return null;

                        view = wm.getLocation(location);
                        if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (isCtsCancelRequested())
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
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            updateLocation();
                        }
                    });
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

    public class WeatherFragmentDataBindingComponent implements androidx.databinding.DataBindingComponent {
        private final WeatherFragmentBindingAdapter mAdapter;

        public WeatherFragmentDataBindingComponent(WeatherNowFragment fragment) {
            this.mAdapter = new WeatherFragmentBindingAdapter(fragment);
        }

        public WeatherFragmentBindingAdapter getWeatherFragmentBindingAdapter() {
            return mAdapter;
        }
    }

    public class WeatherFragmentBindingAdapter {
        private WeatherNowFragment fragment;

        public WeatherFragmentBindingAdapter(WeatherNowFragment fragment) {
            this.fragment = fragment;
        }

        @BindingAdapter("details_data")
        public void updateDetailsContainer(RecyclerView view, WeatherNowViewModel model) {
            if (view.getAdapter() instanceof DetailItemAdapter) {
                ((DetailItemAdapter) view.getAdapter()).updateItems(model);
            }
        }

        @BindingAdapter("forecast_data")
        public void updateForecasePanel(RecyclerView view, List<ForecastItemViewModel> forecasts) {
            if (view.getAdapter() instanceof ForecastItemAdapter) {
                ((ForecastItemAdapter) view.getAdapter()).updateItems(forecasts);
            }
        }

        @BindingAdapter("hrforecast_data")
        public void updateHourlyForecastPanel(RecyclerView view, List<HourlyForecastItemViewModel> hr_forecasts) {
            if (view.getAdapter() instanceof HourlyForecastItemAdapter) {
                ((HourlyForecastItemAdapter) view.getAdapter()).updateItems(hr_forecasts);
            }
        }

        @BindingAdapter("hrforecast_data")
        public void updateHourlyForecastGraph(ViewPager view, List<HourlyForecastItemViewModel> hr_forecasts) {
            if (view.getAdapter() instanceof HourlyForecastGraphPagerAdapter) {
                ((HourlyForecastGraphPagerAdapter) view.getAdapter()).updateDataset(hr_forecasts);
            }
        }

        /* BindingAdapters for dark mode (text color for views) */
        @BindingAdapter("darkModeEnabled")
        public void setTextColor(TextView view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.setTextColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
            view.setLinkTextColor(enableDarkMode ? Colors.SIMPLEBLUELIGHT : Colors.SIMPLEBLUEDARK);
            view.setShadowLayer(view.getShadowRadius(), view.getShadowDx(), view.getShadowDy(), enableDarkMode ? Colors.BLACK : Colors.GRAY);
        }

        @BindingAdapter("darkModeEnabled")
        public void setTextColor(PagerTabStrip view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.setTextColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
            view.setTabIndicatorColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
        }

        @BindingAdapter("darkModeEnabled")
        public void setTextColor(SwitchCompat view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.setTrackTintList(ContextCompat.getColorStateList(view.getContext(), enableDarkMode ? R.color.switch_track_tint_dark : R.color.switch_track_tint_light));
            view.setThumbTintList(ContextCompat.getColorStateList(view.getContext(), enableDarkMode ? R.color.switch_thumb_tint_dark : R.color.switch_thumb_tint_light));
            view.setTextColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
        }

        @BindingAdapter("darkModeEnabled")
        public void setBackgroundColor(View view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.setBackgroundColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
        }

        @BindingAdapter("darkModeEnabled")
        public void updateHourlyForecastGraphColors(ViewPager view, boolean enabled) {
            if (view.getAdapter() instanceof HourlyForecastGraphPagerAdapter) {
                boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
                ((HourlyForecastGraphPagerAdapter) view.getAdapter()).updateColors(enableDarkMode);
            }
        }

        @BindingAdapter("darkModeEnabled")
        public void updateRecyclerViewColors(RecyclerView view, boolean enabled) {
            if (view.getAdapter() instanceof ColorModeRecyclerViewAdapter) {
                boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
                ((ColorModeRecyclerViewAdapter) view.getAdapter()).updateColors(enableDarkMode);
            }
        }

        @BindingAdapter("darkModeEnabled")
        public void updateSunPhaseViewColors(SunPhaseView view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.setTextColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
            view.setPhaseArcColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
            view.setPaintColor(enableDarkMode ? Colors.YELLOW : Colors.ORANGE);
        }
        /* End of BindingAdapters for dark mode */

        @BindingAdapter("backgroundURI")
        public void getBackgroundAttribution(TextView view, String backgroundURI) {
            if (!StringUtils.isNullOrWhitespace(backgroundURI)) {
                if (backgroundURI.contains("DaySky")) {
                    view.setText(R.string.attrib_daysky);
                } else if (backgroundURI.contains("Dust")) {
                    view.setText(R.string.attrib_dust);
                } else if (backgroundURI.contains("FoggySky")) {
                    view.setText(R.string.attrib_foggysky);
                } else if (backgroundURI.contains("MostlyCloudy-Night")) {
                    view.setText(R.string.attrib_mcloudynt);
                } else if (backgroundURI.contains("NightSky")) {
                    view.setText(R.string.attrib_nightsky);
                } else if (backgroundURI.contains("PartlyCloudy-Day")) {
                    view.setText(R.string.attrib_ptcloudyday);
                } else if (backgroundURI.contains("PartlyCloudy-Night")) {
                    view.setText(R.string.attrib_ptcloudynt);
                } else if (backgroundURI.contains("RainyDay")) {
                    view.setText(R.string.attrib_rainyday);
                } else if (backgroundURI.contains("RainyNight")) {
                    view.setText(R.string.attrib_rainynt);
                } else if (backgroundURI.contains("Snow-Windy")) {
                    view.setText(R.string.attrib_snowwindy);
                } else if (backgroundURI.contains("Snow")) {
                    view.setText(R.string.attrib_snow);
                } else if (backgroundURI.contains("StormySky")) {
                    view.setText(R.string.attrib_stormy);
                } else if (backgroundURI.contains("Thunderstorm-Day")) {
                    view.setText(R.string.attrib_tstormday);
                } else if (backgroundURI.contains("Thunderstorm-Night")) {
                    view.setText(R.string.attrib_tstormnt);
                } else {
                    view.setText("");
                }
            }
        }

        @BindingAdapter(value = {"sunrise", "sunset"}, requireAll = true)
        public void updateSunPhasePanel(SunPhaseView view, String sunrise, String sunset) {
            if (!StringUtils.isNullOrWhitespace(sunrise) && !StringUtils.isNullOrWhitespace(sunset) && fragment.location != null) {
                DateTimeFormatter fmt;
                if (DateFormat.is24HourFormat(view.getContext())) {
                    fmt = DateTimeFormatter.ofPattern("HH:mm");
                } else {
                    fmt = DateTimeFormatter.ofPattern("h:mm a");
                }
                view.setSunriseSetTimes(LocalTime.parse(sunrise, fmt),
                        LocalTime.parse(sunset, fmt),
                        fragment.location.getTzOffset());
            }
        }
    }
}