package com.thewizrd.simpleweather.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CustomException;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.TransparentOverlay;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.fragments.LocationSearchFragment;
import com.thewizrd.simpleweather.preferences.ArrayListPreference;
import com.thewizrd.simpleweather.preferences.CustomPreferenceFragmentCompat;
import com.thewizrd.simpleweather.setup.SetupActivity;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.thewizrd.simpleweather.widgets.WidgetUtils.getWidgetTypeFromID;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isForecastWidget;

public class WeatherWidgetPreferenceFragment extends CustomPreferenceFragmentCompat implements OnBackPressedFragmentListener {
    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetType mWidgetType = WidgetType.Unknown;
    private Intent resultValue;

    // Location Search
    private Collection<LocationData> favorites;
    private LocationQueryViewModel query_vm = null;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private CancellationTokenSource cts;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    // Weather
    private WeatherManager wm;

    // Views
    private View mRootView;
    private AppBarLayout appBarLayout;
    private Toolbar mToolbar;
    private View mDialogFragmentContainer;
    private View mSearchFragmentContainer;
    private NestedScrollView mScrollView;
    private LocationSearchFragment mSearchFragment;
    private CharSequence mLastSelectedValue;
    private boolean inSearchUI;

    private ViewGroup widgetContainer;
    private boolean isWidgetInit;
    private WidgetUtils.WidgetBackground mWidgetBackground;
    private WidgetUtils.WidgetBackgroundStyle mWidgetBGStyle;

    private ArrayListPreference locationPref;
    private ListPreference refreshPref;
    private ListPreference bgColorPref;
    private ListPreference bgStylePref;
    private SwitchPreference tap2SwitchPref;

    private static final int ANIMATION_DURATION = 240;

    private static final int MAX_LOCATIONS = Settings.getMaxLocations();
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int SETUP_REQUEST_CODE = 10;

    // Preference Keys
    private static final String KEY_LOCATION = "key_location";
    private static final String KEY_REFRESHINTERVAL = "key_refreshinterval";
    private static final String KEY_BGCOLOR = "key_bgcolor";
    private static final String KEY_BGSTYLE = "key_bgstyle";
    private static final String KEY_HRFLIPBUTTON = "key_hrflipbutton";

    public static WeatherWidgetPreferenceFragment newInstance(Bundle args) {
        WeatherWidgetPreferenceFragment fragment = new WeatherWidgetPreferenceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("WidgetConfig: onResume");
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WidgetConfig: onPause");
        ctsCancel();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        ctsCancel();
        super.onDestroy();
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    private void ctsCancel() {
        if (cts != null) cts.cancel();
        cts = new CancellationTokenSource();
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(mRootView);
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
         * This should be before the super call,
         * so this is setup before onCreatePreferences is called
         */
        if (getArguments() != null) {
            // Find the widget id from the intent.
            mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            mWidgetType = getWidgetTypeFromID(mAppWidgetId);

            // Set the result value for WidgetConfigActivity
            resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        }

        wm = WeatherManager.getInstance();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_widget_setup, container, false);

        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        mScrollView = root.findViewById(R.id.scrollView);
        ViewGroup layoutContainer = root.findViewById(R.id.layout_container);
        layoutContainer.addView(inflatedView);

        if (getListView() != null)
            ViewCompat.setNestedScrollingEnabled(getListView(), false);

        // Set fragment view
        setHasOptionsMenu(true);

        appBarLayout = root.findViewById(R.id.app_bar);
        mToolbar = root.findViewById(R.id.toolbar);
        mDialogFragmentContainer = root.findViewById(R.id.dialog_fragment_container);
        mSearchFragmentContainer = root.findViewById(R.id.search_fragment_container);

        mRootView = (View) getAppCompatActivity().findViewById(R.id.fragment_container).getParent();
        // Make full transparent statusBar
        updateWindowColors();

        ViewCompat.setOnApplyWindowInsetsListener(mScrollView, new OnApplyWindowInsetsListener() {
            private int paddingStart = ViewCompat.getPaddingStart(mScrollView);
            private int paddingTop = mScrollView.getPaddingTop();
            private int paddingEnd = ViewCompat.getPaddingEnd(mScrollView);
            private int paddingBottom = mScrollView.getPaddingBottom();

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.getSystemWindowInsetLeft(),
                        paddingTop,
                        paddingEnd + insets.getSystemWindowInsetRight(),
                        paddingBottom + insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(mDialogFragmentContainer, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets.consumeSystemWindowInsets();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(mSearchFragmentContainer, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        getAppCompatActivity().setSupportActionBar(mToolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Context context = root.getContext();
        Drawable navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, ActivityUtils.getResourceId(getAppCompatActivity(), R.attr.homeAsUpIndicator)));
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText));
        getAppCompatActivity().getSupportActionBar().setHomeAsUpIndicator(navIcon);

        mSearchFragmentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi(false);
            }
        });

        cts = new CancellationTokenSource();

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location mLocation = null;
                    if (locationResult != null) {
                        mLocation = locationResult.getLastLocation();
                    }

                    if (mLocation == null) {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                    } else {
                        if (isAlive()) {
                            prepareWidget();
                        }
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
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                    }
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null && isAlive()) {
                        prepareWidget();
                    } else if (location == null) {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
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

        if (!Settings.isWeatherLoaded()) {
            Toast.makeText(getAppCompatActivity(), R.string.prompt_setup_app_first, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getAppCompatActivity(), SetupActivity.class)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            startActivityForResult(intent, SETUP_REQUEST_CODE);
        }

        return root;
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_widgetconfig, null);

        locationPref = findPreference(KEY_LOCATION);
        locationPref.addEntry(R.string.pref_item_gpslocation, Constants.KEY_GPS);
        locationPref.addEntry(R.string.label_btn_add_location, Constants.KEY_SEARCH);

        Collection<LocationData> favs = Settings.getFavorites();
        favorites = new ArrayList<>(favs);
        for (LocationData location : favorites) {
            locationPref.insertEntry(locationPref.getEntryCount() - 1,
                    location.getName(), location.getQuery());
        }
        if (locationPref.getEntryCount() > MAX_LOCATIONS)
            locationPref.removeEntry(locationPref.getEntryCount() - 1);

        locationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ctsCancel();

                CharSequence selectedValue = (CharSequence) newValue;
                if (Constants.KEY_SEARCH.contentEquals(selectedValue)) {
                    // Setup search UI
                    prepareSearchUI();
                    query_vm = null;
                    return false;
                } else if (Constants.KEY_GPS.contentEquals(selectedValue)) {
                    mLastSelectedValue = null;
                    query_vm = null;
                } else {
                    mLastSelectedValue = selectedValue;
                }

                updateLocationView();
                return true;
            }
        });

        if (getArguments() != null
                && !StringUtils.isNullOrWhitespace(getArguments().getString(WeatherWidgetService.EXTRA_LOCATIONQUERY))) {
            String locName = getArguments().getString(WeatherWidgetService.EXTRA_LOCATIONNAME);
            String locQuery = getArguments().getString(WeatherWidgetService.EXTRA_LOCATIONQUERY);

            if (locName != null) {
                mLastSelectedValue = locQuery;
                locationPref.setValue(mLastSelectedValue.toString());
            } else {
                locationPref.setValueIndex(0);
            }
        } else {
            locationPref.setValueIndex(0);
        }

        // Setup interval spinner
        refreshPref = findPreference(KEY_REFRESHINTERVAL);
        refreshPref.setValue(Integer.toString(Settings.getRefreshInterval()));

        // Setup widget background spinner
        bgColorPref = findPreference(KEY_BGCOLOR);
        bgStylePref = findPreference(KEY_BGSTYLE);

        bgColorPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mWidgetBackground = WidgetUtils.WidgetBackground.valueOf(Integer.parseInt(newValue.toString()));
                updateWidgetView();

                if (mWidgetBackground == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                    if (mWidgetType == WidgetType.Widget4x2 || mWidgetType == WidgetType.Widget2x2) {
                        bgStylePref.setVisible(true);
                        return true;
                    }
                }

                bgStylePref.setValueIndex(0);
                bgStylePref.callChangeListener(bgStylePref.getValue());
                bgStylePref.setVisible(false);
                return true;
            }
        });

        bgStylePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mWidgetBGStyle = WidgetUtils.WidgetBackgroundStyle.valueOf(Integer.parseInt(newValue.toString()));
                updateWidgetView();
                return true;
            }
        });

        if (mWidgetType != WidgetType.Widget4x1Google) {
            bgColorPref.setValueIndex(WidgetUtils.getWidgetBackground(mAppWidgetId).getValue());
            bgColorPref.callChangeListener(bgColorPref.getValue());

            bgStylePref.setValueIndex(WidgetUtils.getBackgroundStyle(mAppWidgetId).getValue());
            bgStylePref.callChangeListener(bgStylePref.getValue());
        } else {
            bgColorPref.setValueIndex(WidgetUtils.WidgetBackground.TRANSPARENT.getValue());
            bgColorPref.setVisible(false);
        }

        // Forecast Preferences
        tap2SwitchPref = findPreference(KEY_HRFLIPBUTTON);
        if (!WeatherAPI.YAHOO.equals(wm.getWeatherAPI()) && isForecastWidget(mWidgetType)) {
            tap2SwitchPref.setVisible(true);
            tap2SwitchPref.setChecked(WidgetUtils.isTapToSwitchEnabled(mAppWidgetId));
        }

        // Get SearchUI state
        if (savedInstanceState != null && savedInstanceState.getBoolean(Constants.KEY_SEARCHUI, false)) {
            inSearchUI = true;

            // Restart SearchUI
            prepareSearchUI();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        widgetContainer = view.findViewById(R.id.widget_container);
        initializeWidget();
        // Resize necessary views
        mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                resizeWidgetContainer();
                return true;
            }
        });
    }

    private void initializeWidget() {
        widgetContainer.removeAllViews();

        int widgetLayoutRes = 0;
        float viewWidth = 0;
        float viewHeight = 0;
        float widgetBlockSize = ActivityUtils.dpToPx(getAppCompatActivity(), 90);

        switch (mWidgetType) {
            case Widget1x1:
                widgetLayoutRes = R.layout.app_widget_1x1;
                viewHeight = viewWidth = widgetBlockSize * 1.5f;
                break;
            case Widget2x2:
                widgetLayoutRes = R.layout.app_widget_2x2;
                viewHeight = viewWidth = widgetBlockSize * 2.25f;
                break;
            case Widget4x1:
                widgetLayoutRes = R.layout.app_widget_4x1;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize;
                break;
            case Widget4x2:
                widgetLayoutRes = R.layout.app_widget_4x2;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize * 2.25f;
                break;
            case Widget4x1Google:
                widgetLayoutRes = R.layout.app_widget_4x1_google;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize * 1.5f;
                break;
        }

        if (widgetLayoutRes == 0) {
            widgetContainer.setVisibility(View.GONE);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        View widgetView = View.inflate(getAppCompatActivity(), widgetLayoutRes, widgetContainer);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) widgetView.getLayoutParams();

        layoutParams.height = (int) viewHeight;
        layoutParams.width = (int) viewWidth;
        layoutParams.gravity = Gravity.CENTER;

        widgetView.setLayoutParams(layoutParams);

        if (mWidgetType == WidgetType.Widget4x1Google) {
            View divider = widgetView.findViewById(R.id.divider);
            divider.setVisibility(View.VISIBLE);
        }

        if (mWidgetType != WidgetType.Widget4x1Google && mWidgetType != WidgetType.Widget1x1) {
            TextView updateTime = widgetView.findViewById(R.id.update_time);

            String timeformat = now.format(DateTimeFormatter.ofPattern("h:mm a")).toLowerCase();

            if (DateFormat.is24HourFormat(App.getInstance().getAppContext()))
                timeformat = now.format(DateTimeFormatter.ofPattern("HH:mm")).toLowerCase();

            String updatetime = String.format("%s %s", now.format(DateTimeFormatter.ofPattern("eee")), timeformat);

            updateTime.setText(String.format("%s %s", getAppCompatActivity().getString(R.string.widget_updateprefix), updatetime));
        }

        TextView conditionText = widgetView.findViewById(R.id.condition_weather);
        if (mWidgetType == WidgetType.Widget2x2) {
            TextView conditionDetails = widgetView.findViewById(R.id.condition_details);

            conditionText.setText("70º - Sunny");
            conditionDetails.setText("79º | 65º");
        } else if (mWidgetType == WidgetType.Widget4x2) {
            conditionText.setText("Sunny");
        } else if (mWidgetType == WidgetType.Widget4x1) {
            widgetView.findViewById(R.id.now_date).setVisibility(View.VISIBLE);
        }

        if (mWidgetType != WidgetType.Widget2x2 && mWidgetType != WidgetType.Widget4x1Google) {
            ImageView tempView = widgetView.findViewById(R.id.condition_temp);
            tempView.setImageResource(R.drawable.notification_temp_pos70);
        } else if (mWidgetType == WidgetType.Widget4x1Google) {
            TextView tempView = widgetView.findViewById(R.id.condition_temp);
            tempView.setText("70ºF");
        }

        ImageView iconView = widgetView.findViewById(R.id.weather_icon);
        iconView.setImageResource(R.drawable.day_sunny);

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            DateTimeFormatter dtfm;
            if (mWidgetType == WidgetType.Widget2x2) {
                dtfm = DateTimeFormatter.ofPattern("eeee, MMMM dd");
            } else if (mWidgetType == WidgetType.Widget4x1Google) {
                dtfm = DateTimeFormatter.ofPattern("eeee, MMM dd");
            } else {
                dtfm = DateTimeFormatter.ofPattern("eee, MMM dd");
            }

            TextView dateText = widgetView.findViewById(R.id.date_panel);
            dateText.setText(now.format(dtfm));
        }

        if (WidgetUtils.isClockWidget(mWidgetType) && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            SpannableString timeStr;
            String timeformat = now.format(DateTimeFormatter.ofPattern("h:mma"));
            int end = timeformat.length() - 2;

            if (DateFormat.is24HourFormat(App.getInstance().getAppContext())) {
                timeformat = now.format(DateTimeFormatter.ofPattern("HH:mm"));
                end = timeformat.length() - 1;
                timeStr = new SpannableString(timeformat);
            } else {
                timeStr = new SpannableString(timeformat);
                timeStr.setSpan(new TextAppearanceSpan("sans-serif", Typeface.BOLD, 16,
                                ContextCompat.getColorStateList(getAppCompatActivity(), android.R.color.white),
                                ContextCompat.getColorStateList(getAppCompatActivity(), android.R.color.white)),
                        end, timeformat.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            TextView clockView = widgetView.findViewById(R.id.clock_panel);
            clockView.setText(timeStr);
        }

        if (isForecastWidget(mWidgetType)) {
            ViewGroup forecastLayout = widgetContainer.findViewById(R.id.forecast_layout);
            forecastLayout.removeAllViews();

            int forecastLength = 3;
            if (mWidgetType == WidgetType.Widget4x2) {
                forecastLength = 5;
            }

            ViewGroup container = (ViewGroup) View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_layout_container, null);

            for (int i = 0; i < forecastLength; i++) {
                View forecastPanel = null;

                if (mWidgetType == WidgetType.Widget4x1)
                    forecastPanel = View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_panel_4x1, null);
                else
                    forecastPanel = View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_panel_4x2, null);

                forecastPanel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                TextView forecastDate = forecastPanel.findViewById(R.id.forecast_date);
                forecastDate.setText(LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("eee")));

                TextView forecastHi = forecastPanel.findViewById(R.id.forecast_hi);
                forecastHi.setText(75 + i + "º");

                TextView forecastLo = forecastPanel.findViewById(R.id.forecast_lo);
                forecastLo.setText(65 - i + "º");

                ImageView forecastIcon = forecastPanel.findViewById(R.id.forecast_icon);
                forecastIcon.setImageResource(R.drawable.day_sunny);

                container.addView(forecastPanel);
            }

            forecastLayout.addView(container);
        }

        View refreshButton = widgetView.findViewById(R.id.refresh_button);
        View refreshProg = widgetView.findViewById(R.id.refresh_progress);
        refreshButton.setVisibility(View.VISIBLE);
        refreshProg.setVisibility(View.GONE);

        isWidgetInit = true;
        updateWidgetView();
    }

    private void updateLocationView() {
        if (widgetContainer == null) return;
        TextView locationView = widgetContainer.findViewById(R.id.location_name);
        locationView.setText(mLastSelectedValue != null ? locationPref.findEntryFromValue(mLastSelectedValue) : this.getString(R.string.pref_location));
    }

    private void updateWidgetView() {
        if (!isWidgetInit) return;

        updateLocationView();

        final int currentNightMode = getAppCompatActivity().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
            int backgroundColor = WidgetUtils.getBackgroundColor(getAppCompatActivity(), mWidgetBackground);
            ImageView pandaBG = widgetContainer.findViewById(R.id.panda_background);
            ImageView widgetBG = widgetContainer.findViewById(R.id.widgetBackground);

            if (mWidgetBackground == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                if (pandaBG != null) {
                    if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                        pandaBG.setColorFilter(isNightMode ? Colors.BLACK : Colors.WHITE);
                        pandaBG.setImageResource(R.drawable.widget_background_bottom_corners);
                    } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.PENDINGCOLOR) {
                        pandaBG.setColorFilter(0xff88b0c8);
                        pandaBG.setImageResource(R.drawable.widget_background_bottom_corners);
                    } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                        pandaBG.setColorFilter(Colors.WHITE);
                        pandaBG.setImageResource(R.drawable.widget_background_bottom_corners);
                    } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.DARK) {
                        pandaBG.setColorFilter(Colors.BLACK);
                        pandaBG.setImageResource(R.drawable.widget_background_bottom_corners);
                    } else {
                        pandaBG.setImageBitmap(null);
                    }
                }

                widgetBG.setColorFilter(backgroundColor);
                widgetBG.setImageAlpha(0xFF);

                Glide.with(this)
                        .load("file:///android_asset/backgrounds/day.jpg")
                        .apply(RequestOptions.formatOf(DecodeFormat.PREFER_RGB_565)
                                .transforms(new CenterCrop(), new TransparentOverlay(0x33)))
                        .into(widgetBG);
            } else if (mWidgetBackground == WidgetUtils.WidgetBackground.TRANSPARENT) {
                widgetBG.setImageResource(R.drawable.widget_background);
                widgetBG.setColorFilter(Colors.BLACK);
                widgetBG.setImageAlpha(0x00);
                if (pandaBG != null) {
                    pandaBG.setColorFilter(Colors.TRANSPARENT);
                    pandaBG.setImageBitmap(null);
                }
            } else {
                widgetBG.setImageResource(R.drawable.widget_background);
                widgetBG.setColorFilter(backgroundColor);
                widgetBG.setImageAlpha(0xFF);
                if (pandaBG != null) {
                    pandaBG.setColorFilter(Colors.TRANSPARENT);
                    pandaBG.setImageBitmap(null);
                }
            }
        }

        // Set text color
        int textColor = WidgetUtils.getTextColor(mWidgetBackground);
        int panelTextColor = WidgetUtils.getPanelTextColor(mWidgetBackground, mWidgetBGStyle, isNightMode);

        if (mWidgetType != WidgetType.Widget2x2 && mWidgetType != WidgetType.Widget4x1Google) {
            ImageView tempView = widgetContainer.findViewById(R.id.condition_temp);
            tempView.setColorFilter(textColor);
        }

        if (mWidgetType == WidgetType.Widget4x1) {
            TextView nowDate = widgetContainer.findViewById(R.id.now_date);
            nowDate.setTextColor(textColor);
        }

        if (mWidgetType != WidgetType.Widget4x1Google && mWidgetType != WidgetType.Widget1x1) {
            TextView update_time = widgetContainer.findViewById(R.id.update_time);
            update_time.setTextColor(textColor);
        }

        boolean is4x2 = mWidgetType == WidgetType.Widget4x2;

        ImageView iconView = widgetContainer.findViewById(R.id.weather_icon);
        iconView.setColorFilter(is4x2 ? textColor : panelTextColor);

        TextView locationView = widgetContainer.findViewById(R.id.location_name);
        locationView.setTextColor(is4x2 ? textColor : panelTextColor);

        if (mWidgetType != WidgetType.Widget4x1Google && mWidgetType != WidgetType.Widget4x1 && mWidgetType != WidgetType.Widget1x1) {
            TextView conditionText = widgetContainer.findViewById(R.id.condition_weather);
            TextView conditionDetails = widgetContainer.findViewById(R.id.condition_details);

            conditionText.setTextColor(is4x2 ? textColor : panelTextColor);
            if (conditionDetails != null)
                conditionDetails.setTextColor(is4x2 ? textColor : panelTextColor);
        }

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            TextView dateText = widgetContainer.findViewById(R.id.date_panel);
            dateText.setTextColor(textColor);
        }

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            TextView clockView = widgetContainer.findViewById(R.id.clock_panel);
            clockView.setTextColor(textColor);
        }

        if (WidgetUtils.isForecastWidget(mWidgetType)) {
            ViewGroup forecastLayout = widgetContainer.findViewById(R.id.forecast_layout);
            if (forecastLayout != null) {
                updateTextViewColor(forecastLayout, panelTextColor);
            }
        }

        ImageView refreshButton = widgetContainer.findViewById(R.id.refresh_button);
        ImageView settButton = widgetContainer.findViewById(R.id.settings_button);
        refreshButton.setColorFilter(textColor);
        settButton.setColorFilter(textColor);
    }

    private void updateTextViewColor(View view, int textColor) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();

            for (int i = 0; i < childCount; i++) {
                updateTextViewColor(viewGroup.getChildAt(i), textColor);
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(textColor);
        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            imageView.setColorFilter(textColor);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (inSearchUI) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi(false);
            return true;
        } else {
            getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
            return false;
        }
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) fragment;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save ActionMode state
        outState.putBoolean(Constants.KEY_SEARCHUI, inSearchUI);

        // Reset to last selected item
        if (inSearchUI && query_vm == null && mLastSelectedValue != null)
            locationPref.setValue(mLastSelectedValue.toString());

        super.onSaveInstanceState(outState);
    }

    private void prepareSearchUI() {
        /*
         * NOTE
         * Compat issue: bring container to the front
         * This is handled on API 21+ with the translationZ attribute
         */
        mSearchFragmentContainer.bringToFront();

        enterSearchUi();
        enterSearchUiTransition(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mSearchFragment != null)
                    mSearchFragment.requestSearchbarFocus();
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
        mSearchFragment.setUserVisibleHint(true);
        final FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        getChildFragmentManager().executePendingTransactions();
    }

    private void enterSearchUiTransition(final Animation.AnimationListener enterAnimationListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // FragmentContainer fade/translation animation
            AnimationSet fragmentAniSet = new AnimationSet(true);
            fragmentAniSet.setInterpolator(new DecelerateInterpolator());
            AlphaAnimation fragFadeAni = new AlphaAnimation(0.0f, 1.0f);
            TranslateAnimation fragmentAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.ABSOLUTE, mSearchFragmentContainer.getRootView().getHeight(),
                    Animation.ABSOLUTE, 0);
            fragmentAniSet.setDuration((long) (ANIMATION_DURATION * 1.5));
            fragmentAniSet.setFillEnabled(false);
            fragmentAniSet.addAnimation(fragFadeAni);
            fragmentAniSet.addAnimation(fragmentAnimation);
            fragmentAniSet.setAnimationListener(enterAnimationListener);
            mSearchFragmentContainer.setVisibility(View.VISIBLE);
            mSearchFragmentContainer.startAnimation(fragmentAniSet);
        } else {
            mSearchFragmentContainer.setVisibility(View.VISIBLE);
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
        searchFragment.setRecyclerOnClickListener(recyclerClickListener);
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void removeSearchFragment() {
        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);
            final FragmentTransaction transaction = getChildFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            mSearchFragment = null;
            transaction.commitAllowingStateLoss();
        }
        mSearchFragmentContainer.setVisibility(View.GONE);
    }

    private void exitSearchUi(boolean skipAnimation) {
        if (mSearchFragment != null) {
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

        updateWindowColors();
        hideInputMethod(getAppCompatActivity() == null ? null : getAppCompatActivity().getCurrentFocus());
        inSearchUI = false;

        // Reset to last selected item
        if (query_vm == null && mLastSelectedValue != null)
            locationPref.setValue(mLastSelectedValue.toString());
    }

    private void exitSearchUiTransition(final Animation.AnimationListener exitAnimationListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MaterialContainerTransform transition = new MaterialContainerTransform();
            transition.setStartView(mSearchFragmentContainer);
            transition.setEndView(getListView().getChildAt(0));
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

            TransitionManager.beginDelayedTransition((ViewGroup) getView(), transition);
            mSearchFragmentContainer.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
        } else {
            mSearchFragmentContainer.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
            if (exitAnimationListener != null)
                exitAnimationListener.onAnimationEnd(null);
        }
    }

    private RecyclerOnClickListenerInterface recyclerClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(final View view, final int position) {
            if (mSearchFragment == null || !isAlive())
                return;

            mSearchFragment.showLoading(true);
            mSearchFragment.enableRecyclerView(false);

            AsyncTask.create(new Callable<LocationQueryViewModel>() {
                @Override
                public LocationQueryViewModel call() throws WeatherException, CustomException, InterruptedException {
                    // Get selected query view
                    final LocationQueryAdapter mSearchFragmentAdapter = mSearchFragment.getAdapter();
                    LocationQueryViewModel queryResult = new LocationQueryViewModel();

                    if (!StringUtils.isNullOrEmpty(mSearchFragmentAdapter.getDataset().get(position).getLocationQuery()))
                        queryResult = mSearchFragmentAdapter.getDataset().get(position);

                    if (StringUtils.isNullOrWhitespace(queryResult.getLocationQuery())) {
                        // Stop since there is no valid query
                        throw new CustomException(R.string.error_retrieve_location);
                    }

                    // Cancel pending search
                    mSearchFragment.ctsCancel();

                    if (mSearchFragment.ctsCancelRequested()) throw new InterruptedException();

                    String country_code = queryResult.getLocationCountry();
                    if (!StringUtils.isNullOrWhitespace(country_code))
                        country_code = country_code.toLowerCase();

                    if (WeatherAPI.NWS.equals(Settings.getAPI()) && !("usa".equals(country_code) || "us".equals(country_code))) {
                        throw new CustomException(R.string.error_message_weather_us_only);
                    }

                    // Need to get FULL location data for HERE API
                    // Data provided is incomplete
                    if (WeatherAPI.HERE.equals(queryResult.getLocationSource())
                            && queryResult.getLocationLat() == -1 && queryResult.getLocationLong() == -1
                            && queryResult.getLocationTZLong() == null) {
                        final LocationQueryViewModel loc = queryResult;
                        queryResult = new AsyncTaskEx<LocationQueryViewModel, WeatherException>().await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                            @Override
                            public LocationQueryViewModel call() throws WeatherException {
                                return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                            }
                        });
                    }

                    // Check if location already exists
                    final LocationQueryViewModel finalQueryResult = queryResult;
                    LocationData loc = Iterables.find(favorites, new Predicate<LocationData>() {
                        @Override
                        public boolean apply(@NullableDecl LocationData input) {
                            return input != null && input.getQuery().equals(finalQueryResult.getLocationQuery());
                        }
                    }, null);

                    if (loc != null) {
                        // Set selection
                        locationPref.setValue(loc.getQuery());
                        return null;
                    }

                    if (mSearchFragment.ctsCancelRequested()) throw new InterruptedException();

                    return queryResult;
                }
            }).addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<LocationQueryViewModel>() {
                @Override
                public void onSuccess(LocationQueryViewModel result) {
                    if (mSearchFragment != null && mSearchFragment.getView() != null &&
                            mSearchFragment.getView().findViewById(R.id.recycler_view) instanceof RecyclerView) {
                        mSearchFragment.enableRecyclerView(false);
                    }

                    if (result != null) {
                        // Save data
                        query_vm = result;
                        final ComboBoxItem item = new ComboBoxItem(query_vm.getLocationName(), query_vm.getLocationQuery());
                        final int idx = locationPref.getEntryCount() - 1;
                        locationPref.insertEntry(idx, item.getDisplay(), item.getValue());
                        locationPref.setValueIndex(idx);
                        if (locationPref.getEntryCount() > MAX_LOCATIONS) {
                            locationPref.removeEntry(locationPref.getEntryCount() - 1);
                        }
                        locationPref.callChangeListener(item.getValue());
                    }

                    // Hide dialog
                    if (mSearchFragment != null) {
                        mSearchFragment.showLoading(false);
                    }
                    exitSearchUi(false);
                }
            }).addOnFailureListener(getAppCompatActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof WeatherException || e instanceof CustomException) {
                        if (mSearchFragment != null) {
                            mSearchFragment.showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT),
                                    new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                            mSearchFragment.showLoading(false);
                            mSearchFragment.enableRecyclerView(true);
                        }
                    } else {
                        if (mSearchFragment != null) {
                            mSearchFragment.showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                    new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                            mSearchFragment.showLoading(false);
                            mSearchFragment.enableRecyclerView(true);
                        }
                    }
                }
            });
        }
    };

    private void updateWindowColors() {
        final Configuration config = this.getResources().getConfiguration();
        final boolean isLandscapeMode = config.orientation != Configuration.ORIENTATION_PORTRAIT && !ActivityUtils.isLargeTablet(getAppCompatActivity());

        // Set user theme
        final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        @ColorInt int bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
            } else {
                bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
            }
        }

        // Actionbar, BottomNavBar & StatusBar
        mRootView.setBackgroundColor(bg_color);
        ActivityUtils.setTransparentWindow(getAppCompatActivity().getWindow(), bg_color, Colors.TRANSPARENT, isLandscapeMode ? bg_color : Colors.TRANSPARENT, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
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
                resizeWidgetContainer();
            }
        });

        updateWindowColors();
    }

    private void resizeWidgetContainer() {
        final View widgetFrameContainer = mScrollView.findViewById(R.id.widget_frame_container);
        final View widgetView = widgetContainer.findViewById(R.id.widget);

        int height = mScrollView.getMeasuredHeight();
        int width = mScrollView.getMeasuredWidth();

        int preferredHeight = (int) ActivityUtils.dpToPx(getAppCompatActivity(), 225);
        int minHeight = (int) (ActivityUtils.dpToPx(getAppCompatActivity(), 90));

        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x2) {
            minHeight *= 2.5f;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(mScrollView, new AutoTransition());
        }

        ViewGroup.LayoutParams layoutParams = widgetFrameContainer.getLayoutParams();

        if (getAppCompatActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutParams.height = preferredHeight;
        } else {
            if (mWidgetType == WidgetType.Widget1x1 || mWidgetType == WidgetType.Widget4x1Google) {
                minHeight *= 1.5f;
            }

            layoutParams.height = minHeight;
        }

        if (widgetView != null) {
            FrameLayout.LayoutParams widgetParams = (FrameLayout.LayoutParams) widgetView.getLayoutParams();
            if (widgetView.getMeasuredWidth() > width) {
                widgetParams.width = width;
            }
            widgetParams.gravity = Gravity.CENTER;
            widgetView.setLayoutParams(widgetParams);
        }

        widgetFrameContainer.setLayoutParams(layoutParams);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETUP_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Get result data
                String dataJson = (data == null || !data.hasExtra(Constants.KEY_DATA)) ? null : data.getStringExtra(Constants.KEY_DATA);

                if (!StringUtils.isNullOrWhitespace(dataJson)) {
                    LocationData locData = JSONParser.deserializer(dataJson, LocationData.class);

                    if (locData.getLocationType() == LocationType.SEARCH) {
                        // Add location to adapter and select it
                        favorites.add(locData);
                        int idx = locationPref.getEntryCount() - 1;
                        locationPref.insertEntry(idx, locData.getName(), locData.getQuery());
                        locationPref.setValueIndex(idx);
                        locationPref.callChangeListener(locData.getQuery());
                    } else {
                        // GPS; set to first selection
                        locationPref.setValueIndex(0);
                    }
                }
            } else {
                // Setup was cancelled. Cancel widget setup
                getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
                getAppCompatActivity().finish();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        menuInflater.inflate(R.menu.menu_widgetsetup, menu);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            MenuItemCompat.setIconTintList(item, ColorStateList.valueOf(ContextCompat.getColor(getAppCompatActivity(), R.color.invButtonColorText)));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (inSearchUI) {
                    // We should let the user go back to usual screens with tabs.
                    exitSearchUi(false);
                } else {
                    getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
                    getAppCompatActivity().finish();
                }
                return true;
            case R.id.action_done:
                prepareWidget();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareWidget() {
        // Update Settings
        Integer refreshValue = NumberUtils.tryParseInt(refreshPref.getValue());
        if (refreshValue != null) {
            Settings.setRefreshInterval(refreshValue);
        }

        if (!isAlive()) {
            if (getAppCompatActivity() != null) {
                getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
                getAppCompatActivity().finish();
            }
            return;
        }

        // Get location data
        if (locationPref.getValue() != null) {
            final String locationItemValue = locationPref.getValue();

            if (Constants.KEY_GPS.equals(locationItemValue)) {
                // Check location
                AsyncTask.create(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws CustomException {
                        // Changing location to GPS
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

                        LocationManager locMan = null;
                        if (getAppCompatActivity() != null)
                            locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

                        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                            // Disable GPS feature if location is not enabled
                            Settings.setFollowGPS(false);
                            throw new CustomException(R.string.error_enable_location_services);
                        }

                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check if last location exists
                        if ((lastGPSLocData == null || !lastGPSLocData.isValid()) && !updateLocation()) {
                            throw new CustomException(R.string.error_retrieve_location);
                        }

                        return true;
                    }
                }).addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean success) {
                        if (success) {
                            Settings.setFollowGPS(true);

                            // Reset data for widget
                            WidgetUtils.deleteWidget(mAppWidgetId);
                            WidgetUtils.saveLocationData(mAppWidgetId, null);
                            WidgetUtils.addWidgetId(Constants.KEY_GPS, mAppWidgetId);

                            finalizeWidgetUpdate();
                        }
                    }
                }).addOnFailureListener(getAppCompatActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof WeatherException || e instanceof CustomException) {
                            showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT), null);
                        } else {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                        }
                    }
                });
            } else {
                AsyncTask.create(new Callable<LocationData>() {
                    @Override
                    public LocationData call() {
                        LocationData locData = null;

                        // Widget ID exists in prefs
                        if (WidgetUtils.exists(mAppWidgetId)) {
                            locData = WidgetUtils.getLocationData(mAppWidgetId);
                        }

                        // Changing location to whatever
                        if (locData == null || !locationItemValue.equals(locData.getQuery())) {
                            // Get location data
                            final String itemValue = locationPref.getValue();
                            locData = Iterables.find(favorites, new Predicate<LocationData>() {
                                @Override
                                public boolean apply(@NullableDecl LocationData input) {
                                    return input != null && input.getQuery().equals(itemValue);
                                }
                            }, null);

                            if (locData == null && query_vm != null) {
                                locData = new LocationData(query_vm);

                                if (!locData.isValid()) {
                                    return null;
                                }

                                // Add location to favs
                                Settings.addLocation(locData);
                            }
                        }

                        return locData;
                    }
                }).addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<LocationData>() {
                    @Override
                    public void onSuccess(LocationData locationData) {
                        if (locationData != null) {
                            // Save locdata for widget
                            WidgetUtils.deleteWidget(mAppWidgetId);
                            WidgetUtils.saveLocationData(mAppWidgetId, locationData);
                            WidgetUtils.addWidgetId(locationData.getQuery(), mAppWidgetId);

                            finalizeWidgetUpdate();
                        } else {
                            getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
                            getAppCompatActivity().finish();
                        }
                    }
                }).addOnFailureListener(getAppCompatActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                    }
                });
            }
        } else {
            getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
            getAppCompatActivity().finish();
        }
    }

    private void finalizeWidgetUpdate() {
        // Save widget preferences
        WidgetUtils.setWidgetBackground(mAppWidgetId, Integer.parseInt(bgColorPref.getValue()));
        WidgetUtils.setBackgroundStyle(mAppWidgetId, Integer.parseInt(bgStylePref.getValue()));
        WidgetUtils.setTapToSwitchEnabled(mAppWidgetId, tap2SwitchPref.isChecked());

        // Trigger widget service to update widget
        WeatherWidgetProvider.showRefreshForWidget(getAppCompatActivity(), mAppWidgetId);
        WeatherWidgetService.enqueueWork(getAppCompatActivity(),
                new Intent(getAppCompatActivity(), WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                        .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS, new int[]{mAppWidgetId})
                        .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, mWidgetType.getValue()));

        // Create return intent
        getAppCompatActivity().setResult(Activity.RESULT_OK, resultValue);
        getAppCompatActivity().finish();
    }

    private boolean updateLocation() throws CustomException {
        return new AsyncTaskEx<Boolean, CustomException>().await(new CallableEx<Boolean, CustomException>() {
            @Override
            public Boolean call() throws CustomException {
                boolean locationChanged = false;

                if (ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }

                Location location = null;

                LocationManager locMan = null;
                if (getAppCompatActivity() != null)
                    locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

                if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                    // Disable GPS feature if location is not enabled
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
                        mLocationRequest.setInterval(5000);
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

                if (location != null && !isCtsCancelRequested()) {
                    LocationQueryViewModel query_vm;

                    TaskCompletionSource<LocationQueryViewModel> tcs = new TaskCompletionSource<>(cts.getToken());
                    try {
                        tcs.setResult(wm.getLocation(location));
                        query_vm = Tasks.await(tcs.getTask());
                    } catch (ExecutionException | WeatherException e) {
                        Logger.writeLine(Log.ERROR, e);
                        return false;
                    } catch (InterruptedException e) {
                        return false;
                    }

                    if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                        query_vm = new LocationQueryViewModel();

                    if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                        // Stop since there is no valid query
                        return false;
                    }

                    if (isCtsCancelRequested()) return false;

                    // Save location as last known
                    Settings.saveLastGPSLocData(new LocationData(query_vm, location));

                    locationChanged = true;
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
                    prepareWidget();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null);
                }
                return;
            }
            default:
                break;
        }
    }

    private void showInputMethod(View view) {
        if (getAppCompatActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getAppCompatActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.showSoftInput(view, 0);
            }
        }
    }

    private void hideInputMethod(View view) {
        if (getAppCompatActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getAppCompatActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
