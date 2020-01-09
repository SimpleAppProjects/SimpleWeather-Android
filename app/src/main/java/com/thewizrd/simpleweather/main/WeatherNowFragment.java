package com.thewizrd.simpleweather.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PorterDuff;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableFloat;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.gson.stream.JsonReader;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
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
import com.thewizrd.simpleweather.adapters.ColorModeRecyclerViewAdapter;
import com.thewizrd.simpleweather.adapters.DetailItemAdapter;
import com.thewizrd.simpleweather.adapters.ForecastGraphPagerAdapter;
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView;
import com.thewizrd.simpleweather.controls.SunPhaseView;
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding;
import com.thewizrd.simpleweather.fragments.WindowColorFragment;
import com.thewizrd.simpleweather.helpers.DarkMode;
import com.thewizrd.simpleweather.helpers.LocationPanelOffsetDecoration;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
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

public class WeatherNowFragment extends WindowColorFragment
        implements WeatherLoadedListenerInterface, WeatherErrorListenerInterface {
    private LocationData location = null;
    private boolean loaded = false;
    private ObservableInt backgroundAlpha;
    private ObservableFloat gradientAlpha;
    private ObservableField<DarkMode> mDarkThemeMode;
    private @ColorInt
    int mSystemBarColor;
    private @ColorInt
    int mBackgroundColor;

    private WeatherManager wm;
    private WeatherDataLoader wLoader = null;
    private WeatherNowViewModel weatherView = null;
    private androidx.databinding.DataBindingComponent dataBindingComponent =
            new WeatherFragmentDataBindingComponent(this);

    private AppCompatActivity mActivity;
    private SystemBarColorManager mSysBarColorsIface;
    private CancellationTokenSource cts;
    private SnackbarManager mSnackMgr;

    // Views
    private View mRootView;
    private AppBarLayout mAppBarLayout;
    private TextView mTitleView;
    private ImageView mImageView;
    private View mGradView;
    private SwipeRefreshLayout refreshLayout;
    private ObservableNestedScrollView scrollView;
    private View conditionPanel;
    // Condition
    private TextView weatherIcon;
    private TextView bgAttribution;
    // Details
    private RecyclerView detailsContainer;
    private DetailItemAdapter detailsAdapter;
    private GridLayoutManager mLayoutManager;
    // Forecast
    private ViewPager forecastGraphView;
    private ForecastGraphPagerAdapter forecastGraphAdapter;
    // Additional Details
    private ConstraintLayout hrforecastPanel;
    private ViewPager hrForecastGraphView;
    private ForecastGraphPagerAdapter hrGraphAdapter;
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
        if (loaded && cts == null)
            cts = new CancellationTokenSource();

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

                    if (Settings.getHomeData().equals(location)) {
                        // Update widgets if they haven't been already
                        if (Duration.between(LocalDateTime.now(), Settings.getUpdateTime()).toMinutes() > Settings.getRefreshInterval()) {
                            WeatherUpdaterWorker.enqueueAction(App.getInstance().getAppContext(), WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
                        }

                        // Update ongoing notification if its not showing
                        if (Settings.showOngoingNotification() && !WeatherNotificationService.isNotificationShowing()) {
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
                        Snackbar snackBar = Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG);
                        snackBar.setAction(R.string.action_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AsyncTask.run(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshWeather(false);
                                    }
                                });
                            }
                        });
                        showSnackbar(snackBar, null);
                        break;
                    case QUERYNOTFOUND:
                        if (WeatherAPI.NWS.equals(Settings.getAPI())) {
                            showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null);
                            break;
                        }
                    default:
                        // Show error message
                        showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG), null);
                        break;
                }
            }
        });
    }

    private void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = new SnackbarManager(mRootView);
            mSnackMgr.setSwipeDismissEnabled(true);
            mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        }
    }

    private void showSnackbar(final Snackbar snackbar, final com.google.android.material.snackbar.Snackbar.Callback callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.show(snackbar, callback);
            }
        });
    }

    private void dismissAllSnackbars() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.dismissAll();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
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
        mSysBarColorsIface = null;
        cts = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        wLoader = null;
        weatherView = null;
        mActivity = null;
        mSysBarColorsIface = null;
        cts = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        if (getArguments() != null) {
            try {
                JsonReader jsonReader = new JsonReader(new StringReader(getArguments().getString(Constants.KEY_DATA, null)));
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

        final int systemNightMode = mActivity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        // Setup ViewModel
        weatherView = ViewModelProviders.of(this).get(WeatherNowViewModel.class);
        backgroundAlpha = new ObservableInt(0xFF); // int: 255
        gradientAlpha = new ObservableFloat(1.0f);
        // Dark Mode fields
        if (systemNightMode == Configuration.UI_MODE_NIGHT_YES) {
            mDarkThemeMode = new ObservableField<>(Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK ? DarkMode.AMOLED_DARK : DarkMode.ON);
        } else {
            mDarkThemeMode = new ObservableField<>(DarkMode.OFF);
        }
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
        binding.setDarkMode(mDarkThemeMode);
        binding.setLifecycleOwner(this);

        View view = binding.getRoot();
        mRootView = view;
        // Request focus away from RecyclerView
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        // Setup ActionBar
        setHasOptionsMenu(true);
        mAppBarLayout = view.findViewById(R.id.app_bar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAppBarLayout.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    // L, T, R, B
                    outline.setRect(view.getPaddingStart(), view.getHeight(), view.getWidth() + view.getPaddingEnd(), view.getHeight() + 1);
                    outline.setAlpha(1.0f);
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(mAppBarLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
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
                        // Default adj = 1.25
                        float adj = 2.5f;
                        int backAlpha = 0xFF - (int) (0xFF * adj * scrollY / (v.getChildAt(0).getHeight() - v.getHeight()));
                        float gradAlpha = 1.0f - (1.0f * adj * scrollY / (conditionPanel.getHeight()));
                        backgroundAlpha.set(Math.max(backAlpha, 0x25));
                        gradientAlpha.set(Math.max(gradAlpha, 0));

                        if (!WeatherNowFragment.this.isHidden() && WeatherNowFragment.this.isVisible() && mSysBarColorsIface != null) {
                            if (gradientAlpha.get() >= 0.5f) {
                                mSysBarColorsIface.setSystemBarColors(mBackgroundColor, Colors.TRANSPARENT, mSystemBarColor, mSystemBarColor);
                            } else if (gradientAlpha.get() == 0.0f) {
                                mSysBarColorsIface.setSystemBarColors(mSystemBarColor);
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

                if (dY == 0) return;

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

                if (dY == 0) return;

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
        weatherIcon = view.findViewById(R.id.weather_icon);
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

        detailsContainer.addItemDecoration(new LocationPanelOffsetDecoration(mActivity, horizMargin / 2f));
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
        forecastGraphView = view.findViewById(R.id.forecast_viewpgr);
        forecastGraphAdapter = new ForecastGraphPagerAdapter();
        forecastGraphView.setAdapter(forecastGraphAdapter);
        forecastGraphView.setOffscreenPageLimit(1);
        // Additional Details
        hrforecastPanel = view.findViewById(R.id.hourly_forecast_panel);
        hrforecastPanel.setVisibility(View.GONE);
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
                if (mActivity != null && weatherView.getExtras().getAlerts().size() > 0)
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_right, R.anim.slide_out_left)
                            .add(R.id.fragment_container, WeatherAlertsFragment.newInstance(location, weatherView))
                            .hide(WeatherNowFragment.this)
                            .addToBackStack(null)
                            .commit();
            }
        });

        hrForecastGraphView = view.findViewById(R.id.hourly_forecast_viewpgr);
        hrGraphAdapter = new ForecastGraphPagerAdapter();
        hrForecastGraphView.setAdapter(hrGraphAdapter);
        hrForecastGraphView.setOffscreenPageLimit(1);

        sunView = view.findViewById(R.id.sun_phase_view);

        // SwipeRefresh
        refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(mActivity, R.color.invButtonColor));
        refreshLayout.setColorSchemeColors(ActivityUtils.getColor(mActivity, R.attr.colorPrimary));
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
                    updateView(weatherView);
                }
            }
        });

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                updateWindowColors();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        final int systemNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (systemNightMode == Configuration.UI_MODE_NIGHT_YES) {
            mDarkThemeMode.set(Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK ? DarkMode.AMOLED_DARK : DarkMode.ON);
        } else {
            mDarkThemeMode.set(DarkMode.OFF);
        }

        weatherView.updatePendingBackground(mActivity.getApplicationContext(), true);
        weatherView.notifyChange();

        refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(mActivity, R.color.invButtonColor));
        refreshLayout.setColorSchemeColors(ActivityUtils.getColor(mActivity, R.attr.colorPrimary));

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

        mImageView.post(new Runnable() {
            @Override
            public void run() {
                // Reload background image
                if (mActivity != null && weatherView != null) {
                    Glide.with(mActivity)
                            .load(weatherView.getBackground())
                            .apply(RequestOptions.centerCropTransform()
                                    .format(DecodeFormat.PREFER_RGB_565)
                                    .skipMemoryCache(true))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(mImageView);
                }
            }
        });
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
        if (mActivity != null) {
            mFusedLocationClient.removeLocationUpdates(mLocCallback)
                    .addOnCompleteListener(mActivity, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRequestingLocationUpdates = false;
                        }
                    });
        }
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

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                CancellationToken ctsToken = cts.getToken();

                /* Update view on resume
                 * ex. If temperature unit changed
                 */
                LocationData homeData = Settings.getHomeData();

                // Did home change?
                boolean homeChanged = false;
                if (location != null && getFragmentManager().getBackStackEntryCount() == 0) {
                    if (!location.equals(homeData) && Constants.FRAGTAG_HOME.equals(getTag())) {
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
                            } finally {
                                ttl = Math.max(ttl, Settings.getRefreshInterval());
                            }
                            Duration span = Duration.between(ZonedDateTime.now(), weather.getUpdateTime()).abs();
                            if (span.toMinutes() > ttl) {
                                refreshWeather(false);
                            } else {
                                if (ctsToken.isCancellationRequested())
                                    return;

                                weatherView.updateView(wLoader.getWeather());

                                loaded = true;
                            }
                        }
                    }
                }
            }
        }, 500, cts.getToken()); // Add a minor delay for a smoother transition
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            adjustConditionPanelLayout();
            adjustDetailsLayout();

            // Go straight to alerts here
            if (mActivity != null && getArguments() != null && getArguments().getBoolean(WeatherWidgetService.ACTION_SHOWALERTS, false)) {
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

            initSnackManager();

            if (weatherView != null) {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        resume();
                    }
                });
            }
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
            adjustConditionPanelLayout();
            adjustDetailsLayout();

            initSnackManager();

            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });
        } else if (hidden) {
            dismissAllSnackbars();
            mSnackMgr = null;
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

        dismissAllSnackbars();
        mSnackMgr = null;

        super.onPause();
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
                            Settings.saveLastGPSLocData(new LocationData());

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
                    if (wLoader != null && !isCtsCancelRequested())
                        wLoader.loadWeatherData(forceRefresh);
                }
            });
        }
    }

    private void adjustConditionPanelLayout() {
        conditionPanel.post(new Runnable() {
            @Override
            public void run() {
                if (mActivity != null) {
                    int height = mRootView.getMeasuredHeight() - mAppBarLayout.getMeasuredHeight();
                    if (height > 0) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) conditionPanel.getLayoutParams();
                        lp.height = height;
                        conditionPanel.setLayoutParams(lp);
                    }
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
                    float pxWidth = displayMetrics.widthPixels;

                    boolean isLargeTablet = ActivityUtils.isLargeTablet(mActivity);

                    int minColumns = isLargeTablet ? 3 : 2;
                    int minWidthDp = 125; // Default: 125f
                    if (isLargeTablet) minWidthDp *= 1.5f;

                    // Minimum width for ea. card
                    int minWidth = (int) ActivityUtils.dpToPx(mActivity, minWidthDp);
                    // Available columns based on min card width
                    int availColumns = ((int) (pxWidth / minWidth)) <= 1 ? minColumns : (int) (pxWidth / minWidth);

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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

                Configuration config = mActivity.getResources().getConfiguration();
                final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
                final UserThemeMode userNightMode = Settings.getUserThemeMode();

                DarkMode mode;
                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    mode = Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK ? DarkMode.AMOLED_DARK : DarkMode.ON;
                } else {
                    mode = DarkMode.OFF;
                }

                if (mDarkThemeMode != null && mDarkThemeMode.get() != mode) {
                    mDarkThemeMode.set(mode);
                }

                if (currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
                    if (weatherView != null && weatherView.getPendingBackground() != -1) {
                        mSystemBarColor = weatherView.getPendingBackground();
                        mBackgroundColor = mSystemBarColor;
                    } else {
                        mSystemBarColor = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
                        mBackgroundColor = mSystemBarColor;
                    }
                } else {
                    if (userNightMode == UserThemeMode.AMOLED_DARK) {
                        mBackgroundColor = mSystemBarColor = Colors.BLACK;
                    } else {
                        mSystemBarColor = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
                        if (weatherView != null && weatherView.getPendingBackground() != -1) {
                            mBackgroundColor = ColorUtils.blendARGB(weatherView.getPendingBackground(), Colors.BLACK, 0.75f);
                            if (userNightMode == UserThemeMode.FOLLOW_SYSTEM) {
                                mSystemBarColor = ColorUtils.blendARGB(weatherView.getPendingBackground(), Colors.BLACK, 0.5f);
                            }
                        } else {
                            mBackgroundColor = mSystemBarColor;
                        }
                    }
                }

                if (!WeatherNowFragment.this.isHidden() && WeatherNowFragment.this.isVisible() && mSysBarColorsIface != null) {
                    mSysBarColorsIface.setSystemBarColors(mBackgroundColor, Colors.TRANSPARENT, mSystemBarColor, mSystemBarColor);
                }
                mRootView.setBackgroundColor(mBackgroundColor);
            }
        });
    }

    private void updateView(final WeatherNowViewModel weatherView) {
        if (isCtsCancelRequested())
            return;

        // Background
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(mActivity)
                        .load(weatherView.getBackground())
                        .apply(RequestOptions.centerCropTransform()
                                .format(DecodeFormat.PREFER_RGB_565))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(mImageView);
            }
        });

        forecastGraphAdapter.setOnClickListener(new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(View view, int position) {
                Fragment fragment = WeatherDetailsFragment.newInstance(location, weatherView, false);
                Bundle args = new Bundle();
                args.putInt(Constants.KEY_POSITION, position);
                fragment.setArguments(args);

                if (mActivity != null) {
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .add(R.id.fragment_container, fragment)
                            .hide(WeatherNowFragment.this)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        // Additional Details
        if (weatherView.getExtras().getHourlyForecast().size() >= 1) {
            if (!WeatherAPI.YAHOO.equals(weatherView.getWeatherSource())) {
                RecyclerOnClickListenerInterface onClickListener = new RecyclerOnClickListenerInterface() {
                    @Override
                    public void onClick(View view, int position) {
                        Fragment fragment = WeatherDetailsFragment.newInstance(location, weatherView, true);
                        Bundle args = new Bundle();
                        args.putInt(Constants.KEY_POSITION, position);
                        fragment.setArguments(args);

                        if (mActivity != null) {
                            mActivity.getSupportFragmentManager().beginTransaction()
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .add(R.id.fragment_container, fragment)
                                    .hide(WeatherNowFragment.this)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                };

                hrGraphAdapter.setOnClickListener(onClickListener);
            }
        } else {
            hrGraphAdapter.setOnClickListener(null);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hrforecastPanel.setVisibility(View.GONE);
                }
            });
        }

        if (isCtsCancelRequested())
            return;

        updateWindowColors();

        // Condition Panel & Scroll view
        adjustConditionPanelLayout();

        // Alerts
        resizeAlertPanel();

        // Sun View
        resizeSunPhasePanel();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
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

                    LocationManager locMan = null;
                    if (mActivity != null)
                        locMan = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.LONG), null);
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
                                    showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.LONG), null);
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

                        try {
                            view = wm.getLocation(location);
                        } catch (final WeatherException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT), null);
                                }
                            });
                            return false;
                        }

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (isCtsCancelRequested())
                            return false;

                        // Save location as last known
                        lastGPSLocData.setData(view, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        LocalBroadcastManager.getInstance(mActivity)
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));

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
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null);
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
        @SuppressWarnings("unchecked")
        public <T extends BaseForecastItemViewModel> void updateForecastGraph(ViewPager view, List<T> forecasts) {
            if (view.getAdapter() instanceof ForecastGraphPagerAdapter) {
                ((ForecastGraphPagerAdapter<T>) view.getAdapter()).updateDataset(forecasts);
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
            view.setDrawFullUnderline(false);
        }

        @BindingAdapter("darkModeEnabled")
        public void setBackgroundColor(View view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.setBackgroundColor(enableDarkMode ? Colors.WHITE : Colors.BLACK);
        }

        @BindingAdapter("darkModeEnabled")
        public void updateForecastGraphColors(ViewPager view, boolean enabled) {
            if (view.getAdapter() instanceof ForecastGraphPagerAdapter) {
                boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
                ((ForecastGraphPagerAdapter) view.getAdapter()).updateColors(enableDarkMode);
            }
        }

        @BindingAdapter("darkMode")
        public void updateRecyclerViewColors(RecyclerView view, DarkMode mode) {
            if (view.getAdapter() instanceof ColorModeRecyclerViewAdapter) {
                ((ColorModeRecyclerViewAdapter) view.getAdapter()).setDarkThemeMode(mode);
            }
        }

        @BindingAdapter("itemColor")
        public void updateRecyclerViewColors(RecyclerView view, @ColorInt int color) {
            if (view.getAdapter() instanceof ColorModeRecyclerViewAdapter) {
                ((ColorModeRecyclerViewAdapter) view.getAdapter()).setItemColor(color);
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