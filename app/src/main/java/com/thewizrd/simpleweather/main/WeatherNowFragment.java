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
import android.transition.Transition;
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
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableFloat;
import androidx.databinding.ObservableInt;
import androidx.databinding.library.baseAdapters.BR;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.JSONParser;
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
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter;
import com.thewizrd.simpleweather.controls.ForecastGraphPanel;
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView;
import com.thewizrd.simpleweather.controls.SunPhaseView;
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding;
import com.thewizrd.simpleweather.databinding.WeathernowAqicontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowBeaufortcontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowMoonphasecontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowRadarcontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowUvcontrolBinding;
import com.thewizrd.simpleweather.fragments.WindowColorFragment;
import com.thewizrd.simpleweather.helpers.DarkMode;
import com.thewizrd.simpleweather.helpers.RadarWebClient;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.helpers.TransitionHelper;
import com.thewizrd.simpleweather.helpers.WebViewHelper;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WeatherNowFragment extends WindowColorFragment
        implements WeatherRequest.WeatherErrorListener {
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
    private DataBindingComponent dataBindingComponent =
            new WeatherFragmentDataBindingComponent(this);

    private AppCompatActivity mActivity;
    private SystemBarColorManager mSysBarColorsIface;
    private CancellationTokenSource cts;
    private SnackbarManager mSnackMgr;

    // Views
    private FragmentWeatherNowBinding binding;

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
        setArguments(new Bundle());
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
            if (fragment.getArguments() == null) {
                fragment.setArguments(new Bundle());
            }
            fragment.getArguments()
                    .putString(Constants.KEY_DATA, JSONParser.serializer(data, LocationData.class));
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
        if (fragment.getArguments() == null) {
            fragment.setArguments(args);
        } else {
            fragment.getArguments().putAll(args);
        }
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

    private void onWeatherLoaded(final LocationData location, final Weather weather) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

                if (weather != null && weather.isValid()) {
                    weatherView.updateView(weather);
                    weatherView.updateBackground();
                    if (binding.imageView.getDrawable() == null && binding.imageView.getTag(R.id.glide_custom_view_target_tag) == null) {
                        String backgroundUri = weatherView.getImageData() != null ? weatherView.getImageData().getImageURI() : null;
                        loadBackgroundImage(backgroundUri, false);
                    }
                    binding.refreshLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.refreshLayout.setRefreshing(false);
                        }
                    });

                    if (wm.supportsAlerts()) {
                        if (weather.getWeatherAlerts() != null && !weather.getWeatherAlerts().isEmpty()) {
                            // Alerts are posted to the user here. Set them as notified.
                            AsyncTask.run(new Runnable() {
                                @Override
                                public void run() {
                                    if (BuildConfig.DEBUG) {
                                        WeatherAlertHandler.postAlerts(location, weather.getWeatherAlerts());
                                    }
                                    WeatherAlertHandler.setAsNotified(location, weather.getWeatherAlerts());
                                }
                            });
                        }
                    }

                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            Context context = App.getInstance().getAppContext();

                            if (Settings.getHomeData().equals(location)) {
                                // Update widgets if they haven't been already
                                if (Duration.between(LocalDateTime.now(), Settings.getUpdateTime()).toMinutes() > Settings.getRefreshInterval()) {
                                    WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
                                } else {
                                    // Update ongoing notification
                                    if (Settings.showOngoingNotification()) {
                                        WeatherNotificationService.enqueueWork(context, new Intent(context, WeatherNotificationService.class)
                                                .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION));
                                    }

                                    // Update widgets anyway
                                    WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                                            .setAction(WeatherWidgetService.ACTION_REFRESHGPSWIDGETS));
                                }
                            } else {
                                // Update widgets anyway
                                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                                        .setAction(WeatherWidgetService.ACTION_REFRESHWIDGETS)
                                        .putExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY, location.getQuery()));
                            }
                        }
                    });
                }
            }
        });
    }

    public void onWeatherError(final WeatherException wEx) {
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

    private void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = new SnackbarManager(binding.getRoot());
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (location != null) {
            outState.putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionHelper.onCreate(this);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_DATA)) {
            location = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData.class);
        } else if (requireArguments().containsKey(Constants.KEY_DATA)) {
            location = JSONParser.deserializer(requireArguments().getString(Constants.KEY_DATA), LocationData.class);
            requireArguments().remove(Constants.KEY_DATA);
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
                                wLoader = new WeatherDataLoader(location);

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
                                wLoader = new WeatherDataLoader(WeatherNowFragment.this.location);

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
        weatherView = new ViewModelProvider(mActivity).get(WeatherNowViewModel.class);
        weatherView.setObserved(true);
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false,
                dataBindingComponent);

        binding.setWeatherView(weatherView);
        binding.setBackgroundAlpha(backgroundAlpha);
        binding.setGradientAlpha(gradientAlpha);
        binding.setDarkMode(mDarkThemeMode);
        binding.setLifecycleOwner(this);

        View view = binding.getRoot();
        // Request focus away from RecyclerView
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        // Setup ActionBar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.appBar.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    // L, T, R, B
                    outline.setRect(view.getPaddingStart(), view.getHeight(), view.getWidth() + view.getPaddingEnd(), view.getHeight() + 1);
                    outline.setAlpha(1.0f);
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.gradientView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.topMargin = -insets.getSystemWindowInsetTop();
                layoutParams.bottomMargin = -insets.getSystemWindowInsetBottom();
                return insets;
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.imageView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.topMargin = -insets.getSystemWindowInsetTop();
                layoutParams.bottomMargin = -insets.getSystemWindowInsetBottom();
                return insets;
            }
        });

        binding.toolbarTitle.setText(R.string.title_activity_weather_now);

        ViewCompat.setOnApplyWindowInsetsListener(binding.refreshLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        binding.scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(final NestedScrollView v, int scrollX, final int scrollY, int oldScrollX, final int oldScrollY) {
                // Default adj = 1.25
                float adj = 2.5f;
                int backAlpha = 0xFF - (int) (0xFF * adj * scrollY / (v.getChildAt(0).getHeight() - v.getHeight()));
                float gradAlpha = 1.0f - (1.0f * adj * scrollY / (binding.refreshLayout.getHeight()));
                backgroundAlpha.set(Math.max(backAlpha, 0x25));
                gradientAlpha.set(Math.max(gradAlpha, 0));

                if (!WeatherNowFragment.this.isHidden() && WeatherNowFragment.this.isVisible() && mSysBarColorsIface != null) {
                    if (gradientAlpha.get() >= 0.5f) {
                        mSysBarColorsIface.setSystemBarColors(mBackgroundColor, Colors.TRANSPARENT, mSystemBarColor, Colors.TRANSPARENT);
                    } else if (gradientAlpha.get() == 0.0f) {
                        mSysBarColorsIface.setSystemBarColors(mSystemBarColor);
                    }
                }
            }
        });
        binding.scrollView.setOnFlingListener(new ObservableNestedScrollView.OnFlingListener() {
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

            @SuppressLint("RestrictedApi")
            @Override
            public void onFlingStopped(int scrollY) {
                int condPnlHeight = binding.refreshLayout.getHeight();
                int THRESHOLD = condPnlHeight / 2;
                int scrollOffset = binding.scrollView.computeVerticalScrollOffset();
                int dY = scrollY - oldScrollY;
                boolean mScrollHandled = false;

                if (dY == 0) return;

                Log.d("ScrollView", String.format("onFlingStopped: height: %d; offset|scrollY: %d; prevScrollY: %d; dY: %d;", condPnlHeight, scrollOffset, oldScrollY, dY));

                if (dY < 0 && scrollOffset < condPnlHeight - THRESHOLD) {
                    binding.scrollView.smoothScrollTo(0, 0);
                    mScrollHandled = true;
                } else if (scrollOffset < condPnlHeight && scrollOffset >= condPnlHeight - THRESHOLD) {
                    binding.scrollView.smoothScrollTo(0, condPnlHeight);
                    mScrollHandled = true;
                } else if (dY > 0 && scrollOffset < condPnlHeight - THRESHOLD) {
                    binding.scrollView.smoothScrollTo(0, condPnlHeight);
                    mScrollHandled = true;
                }

                if (!mScrollHandled && scrollOffset < condPnlHeight) {
                    int animDY = (int) getSplineFlingDistance(startvelocityY);
                    int animScrollY = oldScrollY + animDY;

                    Log.d("ScrollView", String.format("onFlingStopped: height: %d; animScrollY: %d; prevScrollY: %d; animDY: %d;", condPnlHeight, animScrollY, oldScrollY, animDY));

                    if (startvelocityY < 0 && animScrollY < condPnlHeight - THRESHOLD) {
                        binding.scrollView.smoothScrollTo(0, 0);
                    } else if (animScrollY < condPnlHeight && animScrollY >= condPnlHeight - THRESHOLD) {
                        binding.scrollView.smoothScrollTo(0, condPnlHeight);
                    } else if (startvelocityY > 0 && animScrollY < condPnlHeight - THRESHOLD) {
                        binding.scrollView.smoothScrollTo(0, condPnlHeight);
                    }
                }
            }
        });
        binding.scrollView.setTouchScrollListener(new ObservableNestedScrollView.OnTouchScrollChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onTouchScrollChange(int scrollY, int oldScrollY) {
                int condPnlHeight = binding.refreshLayout.getHeight();
                int THRESHOLD = condPnlHeight / 2;
                int scrollOffset = binding.scrollView.computeVerticalScrollOffset();
                int dY = scrollY - oldScrollY;

                if (dY == 0) return;

                Log.d("ScrollView", String.format("onTouchScrollChange: height: %d; offset: %d; scrollY: %d; prevScrollY: %d; dY: %d",
                        condPnlHeight, scrollOffset, scrollY, oldScrollY, dY));

                if (dY < 0 && scrollY < condPnlHeight - THRESHOLD) {
                    binding.scrollView.smoothScrollTo(0, 0);
                } else if (scrollY < condPnlHeight && scrollY >= condPnlHeight - THRESHOLD) {
                    binding.scrollView.smoothScrollTo(0, condPnlHeight);
                } else if (dY > 0 && scrollY < condPnlHeight) {
                    binding.scrollView.smoothScrollTo(0, condPnlHeight);
                }
            }
        });

        // Forecast
        binding.forecastGraphPanel.setOnClickPositionListener(new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(View view, int position) {
                Fragment fragment = WeatherListFragment.newInstance(location, WeatherListType.FORECAST);
                Bundle args = new Bundle();
                args.putInt(Constants.KEY_POSITION, position);
                fragment.setArguments(args);

                if (mActivity != null) {
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, fragment)
                            .hide(WeatherNowFragment.this)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        // Hourly Forecast
        binding.hourlyForecastGraphPanel.setOnClickPositionListener(new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(View view, int position) {
                if (!WeatherAPI.YAHOO.equals(weatherView.getWeatherSource())) {
                    Fragment fragment = WeatherListFragment.newInstance(location, WeatherListType.HOURLYFORECAST);
                    Bundle args = new Bundle();
                    args.putInt(Constants.KEY_POSITION, position);
                    fragment.setArguments(args);

                    if (mActivity != null) {
                        mActivity.getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, fragment)
                                .hide(WeatherNowFragment.this)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            }
        });

        // Condition
        binding.bgAttribution.setMovementMethod(LinkMovementMethod.getInstance());

        // Alerts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.alertButton.setBackgroundTintList(ColorStateList.valueOf(Colors.ORANGERED));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable drawable = binding.alertButton.getBackground().mutate();
            drawable.setColorFilter(Colors.ORANGERED, PorterDuff.Mode.SRC_IN);
            binding.alertButton.setBackground(drawable);
        } else {
            Drawable origDrawable = ContextCompat.getDrawable(mActivity, R.drawable.light_round_corner_bg);
            Drawable compatDrawable = DrawableCompat.wrap(origDrawable);
            DrawableCompat.setTint(compatDrawable, Colors.ORANGERED);
            binding.alertButton.setBackground(compatDrawable);
        }

        binding.alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show Alert Fragment
                if (mActivity != null)
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, WeatherListFragment.newInstance(location, WeatherListType.ALERTS))
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_left)
                            .hide(WeatherNowFragment.this)
                            .addToBackStack(null)
                            .commit();
            }
        });

        // Details
        binding.detailsContainer.setAdapter(new DetailsItemGridAdapter());

        // Disable touch events on container
        // View does not scroll
        binding.detailsContainer.setFocusable(false);
        binding.detailsContainer.setFocusableInTouchMode(false);
        binding.detailsContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        // SwipeRefresh
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(mActivity, R.color.invButtonColor));
        binding.refreshLayout.setColorSchemeColors(ActivityUtils.getColor(mActivity, R.attr.colorPrimary));
        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        if (Settings.useFollowGPS() && updateLocation())
                            // Setup loader from updated location
                            wLoader = new WeatherDataLoader(location);

                        refreshWeather(true);
                    }
                });
            }
        });

        // UV
        binding.uvControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowUvcontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);
            }
        });

        // Beaufort
        binding.beaufortControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowBeaufortcontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);
            }
        });

        // Air Quality
        binding.aqiControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowAqicontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);
            }
        });

        // Moon Phase
        binding.moonphaseControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowMoonphasecontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);
            }
        });

        // Radar
        binding.radarControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowRadarcontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);

                WebViewHelper.disableInteractions(binding.radarWebview);
                WebViewHelper.restrictWebView(binding.radarWebview);
                WebViewHelper.enableJS(binding.radarWebview, true);

                binding.radarWebview.setWebViewClient(new RadarWebClient(true));
                binding.radarWebviewCover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mActivity != null) {
                            mActivity.getSupportFragmentManager().beginTransaction()
                                    .add(R.id.fragment_container, new WeatherRadarFragment())
                                    .hide(WeatherNowFragment.this)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                });
                binding.radarWebview.setBackgroundColor(Colors.BLACK);

                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);
            }
        });

        loaded = true;
        binding.refreshLayout.setRefreshing(true);

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
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adjustDetailsLayout();
        // Sun Phase
        resizeSunPhasePanel();

        // Set property change listeners
        weatherView.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (!WeatherNowFragment.this.isHidden() && WeatherNowFragment.this.isVisible()) {
                    if (propertyId == 0) {
                        updateView();
                    } else if (propertyId == BR.imageData) {
                        // Background
                        adjustGradientView();
                    } else if (propertyId == BR.pendingBackground) {
                        updateWindowColors();
                    } else if (propertyId == BR.location) {
                        adjustConditionPanelLayout();
                    }
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewGroupCompat.setTransitionGroup((ViewGroup) binding.getRoot(), false);
            ViewGroupCompat.setTransitionGroup(binding.appBar, false);
            ViewGroupCompat.setTransitionGroup(binding.toolbar, false);
            ViewGroupCompat.setTransitionGroup(binding.alertButton, true);

            TransitionHelper.onViewCreated(this, (ViewGroup) view.getParent(), new TransitionHelper.OnPrepareTransitionListener() {
                @Override
                public void prepareTransitions(@Nullable Transition enterTransition, @Nullable Transition exitTransition, @Nullable Transition reenterTransition, @Nullable Transition returnTransition) {
                    if (enterTransition != null) {
                        enterTransition
                                .excludeTarget(R.id.image_view, true)
                                .excludeTarget(R.id.gradient_view, true)
                                .excludeTarget(ForecastGraphPanel.class, true)
                                .excludeChildren(ForecastGraphPanel.class, true)
                                .excludeTarget(GridView.class, true)
                                .excludeChildren(GridView.class, true)
                                .excludeTarget(RecyclerView.class, true)
                                .excludeChildren(RecyclerView.class, true)
                                .excludeTarget(SunPhaseView.class, true)
                                .excludeTarget(NestedScrollView.class, true)
                                .excludeTarget(SwipeRefreshLayout.class, true);
                    }
                    if (exitTransition != null) {
                        exitTransition
                                .excludeTarget(R.id.image_view, true)
                                .excludeTarget(R.id.gradient_view, true)
                                .excludeTarget(ForecastGraphPanel.class, true)
                                .excludeChildren(ForecastGraphPanel.class, true)
                                .excludeTarget(GridView.class, true)
                                .excludeChildren(GridView.class, true)
                                .excludeTarget(RecyclerView.class, true)
                                .excludeChildren(RecyclerView.class, true)
                                .excludeTarget(SunPhaseView.class, true)
                                .excludeTarget(NestedScrollView.class, true)
                                .excludeTarget(SwipeRefreshLayout.class, true);
                    }
                    if (reenterTransition != null) {
                        reenterTransition
                                .excludeTarget(R.id.image_view, true)
                                .excludeTarget(R.id.gradient_view, true)
                                .excludeTarget(ForecastGraphPanel.class, true)
                                .excludeChildren(ForecastGraphPanel.class, true)
                                .excludeTarget(GridView.class, true)
                                .excludeChildren(GridView.class, true)
                                .excludeTarget(RecyclerView.class, true)
                                .excludeChildren(RecyclerView.class, true)
                                .excludeTarget(SunPhaseView.class, true)
                                .excludeTarget(NestedScrollView.class, true)
                                .excludeTarget(SwipeRefreshLayout.class, true);
                    }
                    if (returnTransition != null) {
                        returnTransition
                                .excludeTarget(R.id.image_view, true)
                                .excludeTarget(R.id.gradient_view, true)
                                .excludeTarget(ForecastGraphPanel.class, true)
                                .excludeChildren(ForecastGraphPanel.class, true)
                                .excludeTarget(GridView.class, true)
                                .excludeChildren(GridView.class, true)
                                .excludeTarget(RecyclerView.class, true)
                                .excludeChildren(RecyclerView.class, true)
                                .excludeTarget(SunPhaseView.class, true)
                                .excludeTarget(NestedScrollView.class, true)
                                .excludeTarget(SwipeRefreshLayout.class, true);
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
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

        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(mActivity, R.color.invButtonColor));
        binding.refreshLayout.setColorSchemeColors(ActivityUtils.getColor(mActivity, R.attr.colorPrimary));

        // Resize necessary views
        ViewTreeObserver observer = binding.getRoot().getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);

                adjustGradientView();
                adjustConditionPanelLayout();
                adjustDetailsLayout();
                resizeAlertPanel();
                resizeSunPhasePanel();
            }
        });

        String backgroundUri = weatherView.getImageData() != null ? weatherView.getImageData().getImageURI() : null;
        loadBackgroundImage(backgroundUri, true);

        // Reload Webview
        if (binding.radarControl.getBinding() != null) {
            final WeathernowRadarcontrolBinding radarcontrolBinding = (WeathernowRadarcontrolBinding) binding.radarControl.getBinding();
            WebViewHelper.forceReload(radarcontrolBinding.radarWebview, weatherView.getRadarURL());
        }
    }

    private void loadBackgroundImage(final String imageURI, final boolean skipCache) {
        binding.imageView.post(new Runnable() {
            @Override
            public void run() {
                // Reload background image
                if (mActivity != null && !ObjectsCompat.equals(binding.imageView.getTag(), imageURI)) {
                    binding.imageView.setTag(imageURI);
                    if (!StringUtils.isNullOrWhitespace(imageURI)) {
                        Glide.with(WeatherNowFragment.this)
                                .load(imageURI)
                                .apply(RequestOptions.centerCropTransform()
                                        .format(DecodeFormat.PREFER_RGB_565)
                                        .skipMemoryCache(skipCache))
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        // Perform manual shared element transition
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            TransitionHelper.performElementTransition(WeatherNowFragment.this, binding.imageView);
                                        }
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        // Perform manual shared element transition
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            TransitionHelper.performElementTransition(WeatherNowFragment.this, binding.imageView);
                                        }
                                        return false;
                                    }
                                })
                                .into(binding.imageView);
                    } else {
                        Glide.with(WeatherNowFragment.this)
                                .clear(binding.imageView);
                    }
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
            binding.sunPhaseView.post(new Runnable() {
                @Override
                public void run() {
                    View view = WeatherNowFragment.this.getView();

                    if (view == null || view.getWidth() <= 0)
                        return;

                    int viewWidth = view.getWidth();

                    if (viewWidth <= 600)
                        binding.sunPhaseView.getLayoutParams().width = viewWidth;
                    else if (viewWidth <= 1200)
                        binding.sunPhaseView.getLayoutParams().width = (int) (viewWidth * (0.75));
                    else
                        binding.sunPhaseView.getLayoutParams().width = (int) (viewWidth * (0.50));
                }
            });
        }
    }

    private void resizeAlertPanel() {
        if (mActivity != null && ActivityUtils.isLargeTablet(mActivity)) {
            binding.alertButton.post(new Runnable() {
                @Override
                public void run() {
                    View view = WeatherNowFragment.this.getView();

                    if (view == null || view.getWidth() <= 0)
                        return;

                    int viewWidth = view.getWidth();

                    if (viewWidth <= 600)
                        binding.alertButton.getLayoutParams().width = viewWidth;
                    else if (viewWidth <= 1200)
                        binding.alertButton.getLayoutParams().width = (int) (viewWidth * (0.75));
                    else
                        binding.alertButton.getLayoutParams().width = (int) (viewWidth * (0.50));
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

                boolean locationChanged = false;
                if (!loaded) {
                    // Load new favorite location if argument data is present
                    if (requireArguments().containsKey(Constants.KEY_DATA)) {
                        LocationData locationData =
                                JSONParser.deserializer(requireArguments().getString(Constants.KEY_DATA), LocationData.class);
                        if (!ObjectsCompat.equals(locationData, location)) {
                            location = JSONParser.deserializer(requireArguments().getString(Constants.KEY_DATA), LocationData.class);
                            requireArguments().remove(Constants.KEY_DATA);
                            weatherView.reset();
                            locationChanged = true;
                        }
                    } else if (requireArguments().getBoolean(Constants.FRAGTAG_HOME)) {
                        // Check if home location changed
                        // For ex. due to GPS setting change
                        LocationData homeData = Settings.getHomeData();
                        if (!ObjectsCompat.equals(location, homeData)) {
                            location = homeData;
                            weatherView.reset();
                            updateWindowColors();
                            locationChanged = true;
                        }
                    }
                }

                // New fragment instance -> loaded = true
                // Navigating back to existing fragment instance => loaded = false
                // Weather location changed (ex. due to GPS setting) -> locationChanged = true
                if (loaded || locationChanged || wLoader == null) {
                    restore();
                } else {
                    // Refresh current fragment instance
                    ULocale currentLocale = ULocale.forLocale(Locale.getDefault());
                    String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

                    // Check current weather source (API)
                    // Reset if source OR locale is different
                    if (!Settings.getAPI().equals(weatherView.getWeatherSource())
                            || wm.supportsWeatherLocale() && !locale.equals(weatherView.getWeatherLocale())) {
                        restore();
                    } else {
                        // Update weather if needed on resume
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = new WeatherDataLoader(location);
                        }

                        if (ctsToken.isCancellationRequested())
                            return;

                        refreshWeather(false);
                    }
                }

                loaded = true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            adjustConditionPanelLayout();
            adjustDetailsLayout();
            initSnackManager();

            if (weatherView != null) {
                resume();
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
                binding.refreshLayout.setRefreshing(false);
            }
        }

        if (!hidden && weatherView != null && this.isVisible()) {
            adjustConditionPanelLayout();
            adjustDetailsLayout();

            initSnackManager();

            if (requireArguments().containsKey(Constants.ARGS_BACKGROUND)) {
                loadBackgroundImage(requireArguments().getString(Constants.ARGS_BACKGROUND), false);
                requireArguments().remove(Constants.ARGS_BACKGROUND);
            }

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
            binding.refreshLayout.setRefreshing(false);
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

                } else if (location == null && wLoader == null) {
                    // Weather was loaded before. Lets load it up...
                    location = Settings.getHomeData();
                }

                if (isCtsCancelRequested())
                    return;

                if (location != null)
                    wLoader = new WeatherDataLoader(location);

                // Load up weather data
                refreshWeather(forceRefresh);
            }
        });
    }

    private void refreshWeather(final boolean forceRefresh) {
        if (mActivity != null) {
            binding.refreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    binding.refreshLayout.setRefreshing(true);
                }
            });
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    if (wLoader != null && !isCtsCancelRequested())
                        wLoader.loadWeatherData(new WeatherRequest.Builder()
                                .forceRefresh(forceRefresh)
                                .loadAlerts()
                                .setErrorListener(WeatherNowFragment.this)
                                .build())
                                .addOnSuccessListener(mActivity, new OnSuccessListener<Weather>() {
                                    @Override
                                    public void onSuccess(final Weather weather) {
                                        onWeatherLoaded(location, weather);
                                    }
                                });
                }
            });
        }
    }

    private void adjustConditionPanelLayout() {
        binding.conditionPanel.post(new Runnable() {
            @Override
            public void run() {
                if (mActivity != null) {
                    int height = binding.getRoot().getMeasuredHeight() - binding.appBar.getMeasuredHeight();
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.conditionPanel.getLayoutParams();
                    if (height > 0 && lp.height != height) {
                        lp.height = height;
                        binding.conditionPanel.setLayoutParams(lp);
                    }
                }
            }
        });

        binding.weatherIcon.post(new Runnable() {
            @Override
            public void run() {
                binding.weatherIcon.setLayoutParams(binding.weatherIcon.getLayoutParams());
            }
        });

        binding.scrollView.post(new Runnable() {
            @Override
            public void run() {
                if (binding != null)
                    binding.scrollView.scrollTo(0, 0);
            }
        });
    }

    private void adjustDetailsLayout() {
        if (mActivity != null) {
            binding.detailsContainer.post(new Runnable() {
                @Override
                public void run() {
                    View mainView = WeatherNowFragment.this.getView();

                    if (mainView == null)
                        return;

                    DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
                    float pxWidth = displayMetrics.widthPixels;

                    int minColumns = ActivityUtils.isLargeTablet(mActivity) ? 3 : 2;

                    // Minimum width for ea. card
                    int minWidth = mActivity.getResources().getDimensionPixelSize(R.dimen.detail_grid_column_width);
                    // Available columns based on min card width
                    int availColumns = ((int) (pxWidth / minWidth)) <= 1 ? minColumns : (int) (pxWidth / minWidth);

                    binding.detailsContainer.setNumColumns(availColumns);

                    int horizMargin = 16;
                    if (ActivityUtils.isLargeTablet(mActivity)) horizMargin = 24;
                    int itemSpacing = availColumns < 3 ? horizMargin * (availColumns - 1) : horizMargin * 4;
                    binding.detailsContainer.setHorizontalSpacing(itemSpacing);
                    binding.detailsContainer.setVerticalSpacing(itemSpacing);
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
            binding.gradientView.post(new Runnable() {
                @Override
                public void run() {
                    int height = binding.getRoot().getHeight();
                    int width = binding.getRoot().getWidth();

                    if (binding.gradientView.getBackground() instanceof GradientDrawable && height > 0 && width > 0) {
                        GradientDrawable drawable = ((GradientDrawable) binding.gradientView.getBackground().mutate());
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
        binding.getRoot().post(new Runnable() {
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

                if (mDarkThemeMode != null) {
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
                    mSysBarColorsIface.setSystemBarColors(mBackgroundColor, Colors.TRANSPARENT, mSystemBarColor, Colors.TRANSPARENT);
                }
                binding.getRoot().setBackgroundColor(mBackgroundColor);
            }
        });
    }

    private void updateView() {
        if (isCtsCancelRequested())
            return;

        // Condition Panel & Scroll view
        adjustConditionPanelLayout();

        // Alerts
        resizeAlertPanel();

        // Sun View
        resizeSunPhasePanel();
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (mActivity != null && Settings.useFollowGPS() && (location == null || location.getLocationType() == LocationType.GPS)) {
                    if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                        }
                        return false;
                    }

                    Location location = null;

                    if (isCtsCancelRequested())
                        return false;

                    LocationManager locMan = null;
                    if (mActivity != null)
                        locMan = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.LONG), null);

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
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.LONG), null);
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
                            showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT), null);
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
        public void updateDetailsContainer(final GridView view, final List<DetailItemViewModel> models) {
            if (view.getAdapter() instanceof DetailsItemGridAdapter) {
                ((DetailsItemGridAdapter) view.getAdapter()).updateItems(models);
            }
        }

        @BindingAdapter("forecast_data")
        public <T extends BaseForecastItemViewModel> void updateForecastGraph(final ForecastGraphPanel view, final List<T> forecasts) {
            view.updateForecasts((List<BaseForecastItemViewModel>) forecasts);
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
        public void setBackgroundColor(View view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            int color = enableDarkMode ? Colors.WHITE : Colors.BLACK;
            if (!(view.getBackground() instanceof ColorDrawable)) {
                view.setBackgroundColor(color);
            } else {
                ColorDrawable drawable = (ColorDrawable) view.getBackground();
                if (drawable.getColor() != color) {
                    view.setBackgroundColor(color);
                }
            }
        }

        @BindingAdapter("darkModeEnabled")
        public void updateForecastGraphColors(ForecastGraphPanel view, boolean enabled) {
            boolean enableDarkMode = enabled || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            view.updateColors(enableDarkMode);
        }

        @BindingAdapter("darkMode")
        public void updateRecyclerViewColors(GridView view, DarkMode mode) {
            if (view.getAdapter() instanceof DetailsItemGridAdapter) {
                ((DetailsItemGridAdapter) view.getAdapter()).setDarkThemeMode(mode);
            }
        }

        @BindingAdapter("itemColor")
        public void updateRecyclerViewColors(GridView view, @ColorInt int color) {
            if (view.getAdapter() instanceof DetailsItemGridAdapter) {
                ((DetailsItemGridAdapter) view.getAdapter()).setItemColor(color);
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

        @BindingAdapter("progressColor")
        public void updateProgressColor(ProgressBar progressBar, @ColorInt int progressColor) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                progressBar.setProgressTintList(ColorStateList.valueOf(progressColor));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Drawable drawable = progressBar.getProgressDrawable().mutate();
                drawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);
                progressBar.setProgressDrawable(drawable);
            } else {
                Drawable origDrawable = progressBar.getProgressDrawable().mutate();
                Drawable compatDrawable = DrawableCompat.wrap(origDrawable);
                DrawableCompat.setTint(compatDrawable, progressColor);
                progressBar.setProgressDrawable(compatDrawable);
            }
        }

        @BindingAdapter("navigateUrl")
        public void updateNavigateUri(final WebView webView, final String url) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    if (!StringUtils.isNullOrWhitespace(url)) {
                        webView.loadUrl(url);
                    } else {
                        webView.stopLoading();
                        WebViewHelper.loadBlank(webView);
                        webView.freeMemory();
                    }
                }
            });
        }

        @BindingAdapter("imageData")
        public void getBackgroundAttribution(TextView view, ImageDataViewModel imageData) {
            if (imageData != null && !StringUtils.isNullOrWhitespace(imageData.getOriginalLink())) {
                view.setText(HtmlCompat.fromHtml(String.format("<a href=\"%s\">%s %s (%s)</a>",
                        imageData.getOriginalLink(), view.getContext().getString(R.string.attrib_prefix), imageData.getArtistName(), imageData.getSiteName()),
                        HtmlCompat.FROM_HTML_MODE_COMPACT));
                view.setVisibility(View.VISIBLE);
            } else {
                view.setText("");
                view.setVisibility(View.GONE);
            }
        }

        @BindingAdapter("imageData")
        public void loadBackground(ImageView view, ImageDataViewModel imageData) {
            String backgroundUri = imageData != null ? imageData.getImageURI() : null;
            loadBackgroundImage(backgroundUri, false);
        }
    }
}