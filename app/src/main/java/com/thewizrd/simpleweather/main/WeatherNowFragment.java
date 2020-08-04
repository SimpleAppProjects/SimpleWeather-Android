package com.thewizrd.simpleweather.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.WrappedDrawable;
import androidx.core.location.LocationManagerCompat;
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
import androidx.databinding.library.baseAdapters.BR;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.SunPhaseViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter;
import com.thewizrd.simpleweather.controls.ForecastGraphPanel;
import com.thewizrd.simpleweather.controls.ForecastGraphViewModel;
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView;
import com.thewizrd.simpleweather.controls.SunPhaseView;
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding;
import com.thewizrd.simpleweather.databinding.WeathernowAqicontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowBeaufortcontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowDetailscontainerBinding;
import com.thewizrd.simpleweather.databinding.WeathernowForecastgraphpanelBinding;
import com.thewizrd.simpleweather.databinding.WeathernowHrforecastgraphpanelBinding;
import com.thewizrd.simpleweather.databinding.WeathernowMoonphasecontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowRadarcontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowSunphasecontrolBinding;
import com.thewizrd.simpleweather.databinding.WeathernowUvcontrolBinding;
import com.thewizrd.simpleweather.fragments.WindowColorFragment;
import com.thewizrd.simpleweather.helpers.RadarWebClient;
import com.thewizrd.simpleweather.helpers.WebViewHelper;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.preferences.FeatureSettings;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WeatherNowFragment extends WindowColorFragment
        implements WeatherRequest.WeatherErrorListener {
    private WeatherNowFragmentArgs args;

    private WeatherManager wm;
    private WeatherDataLoader wLoader;

    // Views
    private FragmentWeatherNowBinding binding;
    private DataBindingComponent dataBindingComponent =
            new WeatherFragmentDataBindingComponent(this);

    // Data
    private LocationData locationData;
    private MutableLiveData<Weather> weatherLiveData;

    // View Models
    private WeatherNowFragmentStateModel wNowViewModel;
    private WeatherNowViewModel weatherView;
    private ForecastGraphViewModel forecastsView;
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

    public WeatherNowFragment() {
        wm = WeatherManager.getInstance();
        setArguments(new Bundle());
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    private Observer<Weather> weatherObserver = new Observer<Weather>() {
        @Override
        public void onChanged(final Weather weather) {
            if (weather != null && weather.isValid()) {
                weatherView.updateView(weather);
                weatherView.updateBackground();

                String backgroundUri = weatherView.getImageData() != null ? weatherView.getImageData().getImageURI() : null;
                if (FeatureSettings.isBackgroundImageEnabled() && (!ObjectsCompat.equals(binding.imageView.getTag(), backgroundUri) || binding.imageView.getTag(R.id.glide_custom_view_target_tag) == null)) {
                    loadBackgroundImage(backgroundUri, false);
                } else {
                    binding.refreshLayout.setRefreshing(false);
                    binding.scrollView.setVisibility(View.VISIBLE);
                }
                forecastsView.updateForecasts(locationData);
                alertsView.updateAlerts(locationData);

                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        Context context = App.getInstance().getAppContext();

                        if (wm.supportsAlerts()) {
                            if (weather.getWeatherAlerts() != null && !weather.getWeatherAlerts().isEmpty()) {
                                // Alerts are posted to the user here. Set them as notified.
                                if (BuildConfig.DEBUG) {
                                    WeatherAlertHandler.postAlerts(locationData, weather.getWeatherAlerts());
                                }
                                WeatherAlertHandler.setAsNotified(locationData, weather.getWeatherAlerts());
                            }
                        }

                        if (Settings.getHomeData().equals(locationData)) {
                            // Update widgets if they haven't been already
                            if (Duration.between(LocalDateTime.now(ZoneOffset.UTC), Settings.getUpdateTime()).toMinutes() > Settings.getRefreshInterval()) {
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
                                    .putExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY, locationData.getQuery()));
                        }
                    }
                });
            }
        }
    };

    public void onWeatherError(final WeatherException wEx) {
        switch (wEx.getErrorStatus()) {
            case NETWORKERROR:
            case NOWEATHER:
                // Show error message and prompt to refresh
                Snackbar snackBar = Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG);
                snackBar.setAction(R.string.action_retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshWeather(false);
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isAlive()) {
                    binding.refreshLayout.setRefreshing(false);
                }
            }
        });
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(binding.getRoot());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }

    @Override
    public void onDestroy() {
        wLoader = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
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

        setEnterTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());

        args = WeatherNowFragmentArgs.fromBundle(requireArguments());

        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY_DATA)) {
            locationData = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData.class);
        } else if (args.getData() != null) {
            locationData = JSONParser.deserializer(args.getData(), LocationData.class);
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    if (Settings.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = new WeatherDataLoader(locationData);

                        refreshWeather(false);
                    }

                    stopLocationUpdates();
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (Settings.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = new WeatherDataLoader(locationData);

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

        mRequestingLocationUpdates = false;

        // Setup ViewModel
        ViewModelProvider vmProvider = new ViewModelProvider(getAppCompatActivity());
        weatherView = vmProvider.get(WeatherNowViewModel.class);
        forecastsView = vmProvider.get(ForecastGraphViewModel.class);
        alertsView = vmProvider.get(WeatherAlertsViewModel.class);
        wNowViewModel = new ViewModelProvider(this).get(WeatherNowFragmentStateModel.class);

        // Live Data
        weatherLiveData = new MutableLiveData<>();
        weatherLiveData.observe(this, weatherObserver);

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void load() {
                resume();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false,
                dataBindingComponent);

        binding.setWeatherView(weatherView);
        binding.setForecastsView(forecastsView);
        binding.setAlertsView(alertsView);
        binding.setLifecycleOwner(this);

        final View view = binding.getRoot();
        // Request focus away from RecyclerView
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        ViewGroupCompat.setTransitionGroup((ViewGroup) view, true);

        // Setup ActionBar
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, new OnApplyWindowInsetsListener() {
            private int paddingStart = ViewCompat.getPaddingStart(binding.toolbar);
            private int paddingTop = binding.toolbar.getPaddingTop();
            private int paddingEnd = ViewCompat.getPaddingEnd(binding.toolbar);
            private int paddingBottom = binding.toolbar.getPaddingBottom();

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.getSystemWindowInsetLeft(),
                        paddingTop + insets.getSystemWindowInsetTop(),
                        paddingEnd + insets.getSystemWindowInsetRight(),
                        paddingBottom);
                return insets;
            }
        });

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        binding.scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onScrollChange(final NestedScrollView v, int scrollX, final int scrollY, int oldScrollX, final int oldScrollY) {
                if (binding == null) return;

                // Default adj = 1.25
                float adj = 1.25f;
                int backAlpha = 0xFF - (int) (0xFF * adj * scrollY / (binding.refreshLayout.getHeight()));
                float gradAlpha = 1.0f - (1.0f * adj * scrollY / (binding.refreshLayout.getHeight()));
                binding.imageView.setImageAlpha(Math.max(backAlpha, 0x25));
                binding.gradientView.setAlpha(Math.max(gradAlpha, 0));

                int offset = v.computeVerticalScrollOffset();
                if (offset > 0) {
                    ViewCompat.setElevation(binding.toolbar, ActivityUtils.dpToPx(getAppCompatActivity(), 4));
                } else {
                    ViewCompat.setElevation(binding.toolbar, 0);
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
            private final float ppi = getAppCompatActivity().getResources().getDisplayMetrics().density * 160.0f;
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
                if (binding == null || !FeatureSettings.isBackgroundImageEnabled()) return;

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
                if (binding == null || !FeatureSettings.isBackgroundImageEnabled()) return;

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
            Drawable origDrawable = ContextCompat.getDrawable(getAppCompatActivity(), R.drawable.light_round_corner_bg);
            Drawable compatDrawable = DrawableCompat.wrap(origDrawable);
            DrawableCompat.setTint(compatDrawable, Colors.ORANGERED);
            binding.alertButton.setBackground(compatDrawable);
        }

        binding.alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsLogger.logEvent("WeatherNowFragment: alerts click");
                // Show Alert Fragment
                WeatherNowFragmentDirections.ActionWeatherNowFragmentToWeatherListFragment args =
                        WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                                .setData(JSONParser.serializer(locationData, LocationData.class))
                                .setWeatherListType(WeatherListType.ALERTS);
                Navigation.findNavController(v).navigate(args);
            }
        });

        // SwipeRefresh
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getAppCompatActivity(), R.color.invButtonColor));
        binding.refreshLayout.setColorSchemeColors(ActivityUtils.getColor(getAppCompatActivity(), R.attr.colorPrimary));
        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AnalyticsLogger.logEvent("WeatherNowFragment: onRefresh");

                if (Settings.useFollowGPS() && updateLocation())
                    // Setup loader from updated location
                    wLoader = new WeatherDataLoader(locationData);

                refreshWeather(true);
            }
        });

        // Forecast
        binding.forecastGraphPanel.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                final WeathernowForecastgraphpanelBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setForecastsView(forecastsView);
                binding.setLifecycleOwner(WeatherNowFragment.this);

                binding.forecastGraphPanel.setOnClickPositionListener(new RecyclerOnClickListenerInterface() {
                    @Override
                    public void onClick(View view, int position) {
                        AnalyticsLogger.logEvent("WeatherNowFragment: fcast graph click");

                        WeatherNowFragmentDirections.ActionWeatherNowFragmentToWeatherListFragment args =
                                WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                                        .setData(JSONParser.serializer(locationData, LocationData.class))
                                        .setWeatherListType(WeatherListType.FORECAST)
                                        .setPosition(position);
                        Navigation.findNavController(view).navigate(args);
                    }
                });
            }
        });

        // Hourly Forecast
        binding.hourlyForecastGraphPanel.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                final WeathernowHrforecastgraphpanelBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setForecastsView(forecastsView);
                binding.setLifecycleOwner(WeatherNowFragment.this);

                binding.hourlyForecastGraphPanel.setOnClickPositionListener(new RecyclerOnClickListenerInterface() {
                    @Override
                    public void onClick(View view, int position) {
                        AnalyticsLogger.logEvent("WeatherNowFragment: hrf graph click");
                        if (!WeatherAPI.YAHOO.equals(weatherView.getWeatherSource())) {
                            WeatherNowFragmentDirections.ActionWeatherNowFragmentToWeatherListFragment args =
                                    WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                                            .setData(JSONParser.serializer(locationData, LocationData.class))
                                            .setWeatherListType(WeatherListType.HOURLYFORECAST)
                                            .setPosition(position);
                            Navigation.findNavController(view).navigate(args);
                        }
                    }
                });
            }
        });

        binding.detailsControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowDetailscontainerBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);

                // Details
                binding.detailsContainer.setAdapter(new DetailsItemGridAdapter());

                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);

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

                adjustDetailsLayout();
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

        // Sun Phase
        binding.sunphaseControl.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                WeathernowSunphasecontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);
                binding.setWeatherView(weatherView);
                binding.setLifecycleOwner(WeatherNowFragment.this);
            }
        });

        // Radar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            binding.radarControl.setOnInflateListener(new ViewStub.OnInflateListener() {
                @Override
                public void onInflate(ViewStub stub, final View inflated) {
                    final WeathernowRadarcontrolBinding binding = DataBindingUtil.bind(inflated, dataBindingComponent);

                    binding.radarWebviewCover.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AnalyticsLogger.logEvent("WeatherNowFragment: radar view click");

                            Navigation.findNavController(v)
                                    .navigate(
                                            WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherRadarFragment(),
                                            new FragmentNavigator.Extras.Builder()
                                                    .addSharedElement(inflated, "radar")
                                                    .build()
                                    );
                        }
                    });

                    ViewCompat.setTransitionName(inflated, "radar");

                    /*
                     * NOTE
                     * Compat issue: bring container to the front
                     * This is handled on API 21+ with the translationZ attribute
                     */
                    binding.radarWebviewCover.bringToFront();

                    binding.setWeatherView(weatherView);
                    binding.setLifecycleOwner(WeatherNowFragment.this);

                    navigateToRadarURL();
                }
            });
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        args = WeatherNowFragmentArgs.fromBundle(requireArguments());

        binding.refreshLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                binding.refreshLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                binding.refreshLayout.setRefreshing(true);

                return true;
            }
        });

        adjustConditionPanelLayout();

        // Set property change listeners
        weatherView.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, final int propertyId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!WeatherNowFragment.this.isHidden() && WeatherNowFragment.this.isVisible()) {
                            if (propertyId == 0) {
                                // Condition Panel & Scroll view
                                adjustConditionPanelLayout();
                            } else if (propertyId == BR.location) {
                                adjustConditionPanelLayout();
                            } else if (propertyId == BR.radarURL) {
                                // Restrict control to Kitkat+ for Chromium WebView
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    if (FeatureSettings.isRadarEnabled() && weatherView.getRadarURL() != null) {
                                        navigateToRadarURL();
                                    }
                                }
                            }
                        }
                    }
                });
            }
        });

        binding.scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                binding.scrollView.getViewTreeObserver().removeOnPreDrawListener(this);
                binding.scrollView.postOnAnimationDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isAlive())
                            binding.scrollView.smoothScrollTo(0, wNowViewModel.getScrollViewPosition());
                    }
                }, 100);
                return true;
            }
        });

        // Restrict control to Kitkat+ for Chromium WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (FeatureSettings.isRadarEnabled() && weatherView.getRadarURL() != null) {
                navigateToRadarURL();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                WebViewHelper.loadBlank(webView);
                if (binding.radarControl.getBinding() instanceof WeathernowRadarcontrolBinding) {
                    ((WeathernowRadarcontrolBinding) binding.radarControl.getBinding()).radarWebviewContainer.removeAllViews();
                }
                webView.destroy();
            }
            wNowViewModel.setScrollViewPosition(binding.scrollView.computeVerticalScrollOffset());
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        weatherView.notifyChange();

        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getAppCompatActivity(), R.color.invButtonColor));
        binding.refreshLayout.setColorSchemeColors(ActivityUtils.getColor(getAppCompatActivity(), R.attr.colorPrimary));

        // Resize necessary views
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!isAlive()) return;
                binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);

                adjustConditionPanelLayout();
            }
        });

        String backgroundUri = weatherView.getImageData() != null ? weatherView.getImageData().getImageURI() : null;
        loadBackgroundImage(backgroundUri, true);

        // Reload Webview
        if (binding.radarControl.getBinding() != null) {
            WebView radarWebview = getRadarWebView();
            if (radarWebview != null) {
                WebViewHelper.forceReload(radarWebview, weatherView.getRadarURL());
            }
        }
    }

    private void loadBackgroundImage(final String imageURI, final boolean skipCache) {
        if (isAlive()) {
            binding.imageView.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    if (!isAlive()) return;

                    // Reload background image
                    if (FeatureSettings.isBackgroundImageEnabled()) {
                        if (!ObjectsCompat.equals(binding.imageView.getTag(), imageURI)) {
                            binding.imageView.setTag(imageURI);
                            if (!StringUtils.isNullOrWhitespace(imageURI)) {
                                Glide.with(WeatherNowFragment.this)
                                        .load(imageURI)
                                        .apply(RequestOptions.centerCropTransform()
                                                .format(DecodeFormat.PREFER_RGB_565)
                                                .skipMemoryCache(skipCache))
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .addListener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                binding.scrollView.setVisibility(View.VISIBLE);
                                                return false;
                                            }

                                            @Override
                                            public boolean onResourceReady(final Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                binding.refreshLayout.postOnAnimation(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (!isAlive()) return;
                                                        binding.scrollView.setVisibility(View.VISIBLE);
                                                        binding.refreshLayout.setRefreshing(false);
                                                    }
                                                });
                                                return false;
                                            }
                                        })
                                        .into(binding.imageView);
                            } else {
                                Glide.with(WeatherNowFragment.this).clear(binding.imageView);
                                binding.imageView.setTag(null);
                                if (weatherView.isValid()) {
                                    binding.scrollView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    } else {
                        Glide.with(WeatherNowFragment.this).clear(binding.imageView);
                        binding.imageView.setTag(null);
                        if (weatherView.isValid()) {
                            binding.scrollView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
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
        if (getAppCompatActivity() != null && mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocCallback)
                    .addOnCompleteListener(getAppCompatActivity(), new OnCompleteListener<Void>() {
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

        if (binding != null) {
            final WebView webView = getRadarWebView();
            if (webView != null) {
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        webView.onResume();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            navigateToRadarURL();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherNowFragment: onPause");

        if (binding != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                webView.onPause();
            }
        }

        // Remove location updates to save battery.
        stopLocationUpdates();
        super.onPause();
    }

    private boolean verifyLocationData() {
        boolean locationChanged = false;

        // Check if current location still exists (is valid)
        if (locationData != null && locationData.getLocationType() == LocationType.SEARCH) {
            if (Settings.getLocation(locationData.getQuery()) == null) {
                locationData = null;
                wLoader = null;
                locationChanged = true;
            }
        }
        // Load new favorite location if argument data is present
        if (args.getHome()) {
            // Check if home location changed
            // For ex. due to GPS setting change
            LocationData homeData = Settings.getHomeData();
            if (!ObjectsCompat.equals(locationData, homeData)) {
                locationData = homeData;
                locationChanged = true;
            }
        } else if (args.getData() != null) {
            LocationData location = new AsyncTask<LocationData>().await(new Callable<LocationData>() {
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
                            Settings.saveLastGPSLocData(new LocationData());

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
        }).addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean forceRefresh) {
                // Load up weather data
                refreshWeather(forceRefresh);
            }
        }).addOnFailureListener(getAppCompatActivity(), new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void refreshWeather(final boolean forceRefresh) {
        if (isAlive()) {
            if (wLoader == null && locationData != null) {
                wLoader = new WeatherDataLoader(locationData);
            }

            if (wLoader != null) {
                binding.refreshLayout.setRefreshing(true);

                wLoader.loadWeatherData(new WeatherRequest.Builder()
                        .forceRefresh(forceRefresh)
                        .setErrorListener(WeatherNowFragment.this)
                        .build())
                        .addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<Weather>() {
                            @Override
                            public void onSuccess(final Weather weather) {
                                weatherLiveData.setValue(weather);
                            }
                        });
            }
        }
    }

    private void adjustConditionPanelLayout() {
        binding.conditionPanel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                binding.conditionPanel.getViewTreeObserver().removeOnPreDrawListener(this);

                int height = binding.refreshLayout.getMeasuredHeight();
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.conditionPanel.getLayoutParams();
                if (FeatureSettings.isBackgroundImageEnabled() && height > 0) {
                    lp.height = height;
                } else {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                binding.conditionPanel.setLayoutParams(lp);

                return true;
            }
        });

        binding.weatherIcon.setLayoutParams(binding.weatherIcon.getLayoutParams());

        binding.scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                binding.scrollView.getViewTreeObserver().removeOnPreDrawListener(this);

                if (ActivityUtils.isLargeTablet(getAppCompatActivity())) {
                    if (binding.scrollView.getChildCount() < 1) {
                        int viewWidth = binding.scrollView.getWidth();

                        ViewGroup.LayoutParams lp = binding.scrollView.getChildAt(0).getLayoutParams();
                        boolean isLandscape = ActivityUtils.getOrientation(getAppCompatActivity()) == Configuration.ORIENTATION_LANDSCAPE;

                        if (isLandscape)
                            lp.width = (int) (viewWidth * (0.75));
                        else
                            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    }
                }

                return true;
            }
        });

        adjustDetailsLayout();
    }

    private void adjustDetailsLayout() {
        if (!isAlive() || binding.detailsControl.getBinding() == null || binding.scrollView.getChildCount() != 1)
            return;

        final WeathernowDetailscontainerBinding detailsBinding = (WeathernowDetailscontainerBinding) binding.detailsControl.getBinding();

        detailsBinding.detailsContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                detailsBinding.detailsContainer.getViewTreeObserver().removeOnPreDrawListener(this);

                float pxWidth = binding.scrollView.getChildAt(0).getWidth();

                int minColumns = ActivityUtils.isLargeTablet(getAppCompatActivity()) ? 3 : 2;

                // Minimum width for ea. card
                int minWidth = getAppCompatActivity().getResources().getDimensionPixelSize(R.dimen.detail_grid_column_width);
                // Available columns based on min card width
                int availColumns = ((int) (pxWidth / minWidth)) <= 1 ? minColumns : (int) (pxWidth / minWidth);

                detailsBinding.detailsContainer.setNumColumns(availColumns);

                boolean isLandscape = ActivityUtils.getOrientation(getAppCompatActivity()) == Configuration.ORIENTATION_LANDSCAPE;

                int horizMargin = 16;
                int marginMultiplier = isLandscape ? 2 : 3;
                int itemSpacing = availColumns < 3 ? horizMargin * (availColumns - 1) : horizMargin * marginMultiplier;
                detailsBinding.detailsContainer.setHorizontalSpacing(itemSpacing);
                detailsBinding.detailsContainer.setVerticalSpacing(itemSpacing);

                return true;
            }
        });
    }

    @Override
    public void updateWindowColors() {
        if (!isAlive()) return;

        int color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }

        binding.toolbar.setBackgroundColor(color);
        binding.rootView.setBackgroundColor(color);
        binding.rootView.setStatusBarBackgroundColor(color);
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (getAppCompatActivity() != null && Settings.useFollowGPS() && (locationData == null || locationData.getLocationType() == LocationType.GPS)) {
                    if (ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

                    LocationManager locMan = null;
                    if (getAppCompatActivity() != null)
                        locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        locationData = Settings.getLastGPSLocData();
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

                        LocationQueryViewModel view;

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

                        // Save location as last known
                        lastGPSLocData.setData(view, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        LocalBroadcastManager.getInstance(getAppCompatActivity())
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));

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
                    if (Settings.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = new WeatherDataLoader(locationData);
                    }

                    refreshWeather(false);
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

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private void navigateToRadarURL() {
        if (weatherView == null || weatherView.getRadarURL() == null ||
                binding == null || binding.radarControl.getBinding() == null ||
                !FeatureSettings.isRadarEnabled())
            return;

        WeathernowRadarcontrolBinding radarcontrolBinding = (WeathernowRadarcontrolBinding) binding.radarControl.getBinding();
        WebView webView = (WebView) radarcontrolBinding.radarWebviewContainer.getChildAt(0);

        if (webView == null) {
            radarcontrolBinding.radarWebviewContainer.addView(webView = createWebView());
        }

        if (!isHidden() && !StringUtils.isNullOrWhitespace(weatherView.getRadarURL())) {
            if (!ObjectsCompat.equals(webView.getOriginalUrl(), weatherView.getRadarURL())) {
                WebViewHelper.loadUrl(webView, weatherView.getRadarURL());
            }
        } else {
            webView.stopLoading();
            WebViewHelper.loadBlank(webView);
            webView.freeMemory();
        }
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private WebView createWebView() {
        WebView webView = new WebView(this.getContext());

        WebViewHelper.disableInteractions(webView);
        WebViewHelper.restrictWebView(webView);
        WebViewHelper.enableJS(webView, true);

        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        }

        webView.setWebViewClient(new RadarWebClient(true) {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Bundle args = new Bundle();
                    args.putBoolean("didCrash", detail.didCrash());
                    args.putInt("renderPriorityAtExit", detail.rendererPriorityAtExit());
                    AnalyticsLogger.logEvent("WeatherNow: radarWebView render gone", args);
                } else {
                    AnalyticsLogger.logEvent("WeatherNow: radarWebView render gone");
                }

                if (binding != null) {
                    WeathernowRadarcontrolBinding radarcontrolBinding = (WeathernowRadarcontrolBinding) binding.radarControl.getBinding();

                    if (radarcontrolBinding != null) {
                        WebView wv = getRadarWebView();

                        if (wv == view) {
                            radarcontrolBinding.radarWebviewContainer.removeAllViews();
                            wv = null;
                            view.loadUrl("about:blank");
                            view.onPause();
                            view.destroy();
                            navigateToRadarURL();
                            return true;
                        }
                    }
                }

                return super.onRenderProcessGone(view, detail);
            }
        });
        webView.setBackgroundColor(Colors.BLACK);
        webView.resumeTimers();

        return webView;
    }

    private WebView getRadarWebView() {
        if (binding != null && binding.radarControl.getBinding() != null) {
            WeathernowRadarcontrolBinding radarcontrolBinding = (WeathernowRadarcontrolBinding) binding.radarControl.getBinding();

            if (radarcontrolBinding != null)
                return (WebView) radarcontrolBinding.radarWebviewContainer.getChildAt(0);
        }

        return null;
    }

    public static class WeatherNowFragmentStateModel extends ViewModel {
        private int scrollViewPosition;

        public int getScrollViewPosition() {
            return scrollViewPosition;
        }

        public void setScrollViewPosition(int scrollViewPosition) {
            this.scrollViewPosition = scrollViewPosition;
        }
    }

    public static class WeatherFragmentDataBindingComponent implements androidx.databinding.DataBindingComponent {
        private final WeatherNowFragmentBindingAdapter mAdapter;

        public WeatherFragmentDataBindingComponent(WeatherNowFragment fragment) {
            this.mAdapter = new WeatherNowFragmentBindingAdapter(fragment);
        }

        @Override
        public WeatherNowFragmentBindingAdapter getWeatherNowFragmentBindingAdapter() {
            return mAdapter;
        }
    }

    public static class WeatherNowFragmentBindingAdapter {
        private WeatherNowFragment fragment;

        public WeatherNowFragmentBindingAdapter(WeatherNowFragment fragment) {
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

        @BindingAdapter("sunPhase")
        public void updateSunPhasePanel(SunPhaseView view, SunPhaseViewModel sunPhase) {
            if (sunPhase != null && !StringUtils.isNullOrWhitespace(sunPhase.getSunrise()) && !StringUtils.isNullOrWhitespace(sunPhase.getSunset()) && fragment.locationData != null) {
                DateTimeFormatter fmt;
                if (DateFormat.is24HourFormat(view.getContext())) {
                    fmt = DateTimeFormatter.ofPattern("HH:mm");
                } else {
                    fmt = DateTimeFormatter.ofPattern("h:mm a");
                }
                view.setSunriseSetTimes(LocalTime.parse(sunPhase.getSunrise(), fmt),
                        LocalTime.parse(sunPhase.getSunset(), fmt),
                        fragment.locationData.getTzOffset());
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

        @BindingAdapter("progressBackgroundColor")
        public void updateProgressBackgroundColor(ProgressBar progressBar, @ColorInt int progressBackgroundColor) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(progressBackgroundColor));
            } else {
                LayerDrawable drawable = null;
                if (progressBar.getProgressDrawable() instanceof LayerDrawable) {
                    drawable = (LayerDrawable) progressBar.getProgressDrawable();
                } else if (progressBar.getProgressDrawable() instanceof WrappedDrawable) {
                    Drawable unwrapped = DrawableCompat.unwrap(progressBar.getProgressDrawable());
                    if (unwrapped instanceof LayerDrawable) {
                        drawable = (LayerDrawable) unwrapped;
                    }
                }
                if (drawable != null) {
                    GradientDrawable background = (GradientDrawable) drawable.findDrawableByLayerId(android.R.id.background);
                    background.setColorFilter(progressBackgroundColor, PorterDuff.Mode.SRC_IN);
                }
            }
        }

        @BindingAdapter("imageData")
        public void getBackgroundAttribution(final TextView view, final ImageDataViewModel imageData) {
            if (imageData != null && !StringUtils.isNullOrWhitespace(imageData.getOriginalLink())) {
                SpannableString text = new SpannableString(String.format("%s %s (%s)",
                        view.getContext().getString(R.string.attrib_prefix), imageData.getArtistName(), imageData.getSiteName()));
                text.setSpan(new UnderlineSpan(), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                view.setText(text);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(imageData.getOriginalLink())));
                    }
                });
            } else {
                view.setText("");
                view.setOnClickListener(null);
            }
        }

        @BindingAdapter(value = {"tempTextColor", "tempUnit"}, requireAll = false)
        public void tempTextColor(TextView view, CharSequence temp, String tempUnit) {
            String temp_str = temp != null ? StringUtils.removeNonDigitChars(temp.toString()) : null;
            if (!StringUtils.isNullOrWhitespace(temp_str)) {
                float temp_f;
                if (ObjectsCompat.equals(tempUnit, Settings.CELSIUS) || temp.toString().endsWith(WeatherIcons.CELSIUS)) {
                    temp_f = ConversionMethods.CtoF(Float.parseFloat(temp_str));
                } else {
                    temp_f = Float.parseFloat(temp_str);
                }

                view.setTextColor(WeatherUtils.getColorFromTempF(temp_f));
            } else {
                view.setTextColor(ContextCompat.getColor(view.getContext(), R.color.colorTextPrimary));
            }
        }
    }
}