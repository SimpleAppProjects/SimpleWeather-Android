package com.thewizrd.simpleweather.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.bumptech.glide.load.DecodeFormat;
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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CustomException;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.TransparentOverlay;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.GlideApp;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding;
import com.thewizrd.simpleweather.preferences.ArrayListPreference;
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat;
import com.thewizrd.simpleweather.setup.SetupActivity;
import com.thewizrd.simpleweather.snackbar.Snackbar;

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

public class WeatherWidgetPreferenceFragment extends ToolbarPreferenceFragmentCompat {
    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetType mWidgetType = WidgetType.Unknown;
    private Intent resultValue;

    private WeatherWidgetPreferenceFragmentArgs args;

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
    private FragmentWidgetSetupBinding binding;

    private CharSequence mLastSelectedValue;

    private boolean isWidgetInit;
    private WidgetUtils.WidgetBackground mWidgetBackground;
    private WidgetUtils.WidgetBackgroundStyle mWidgetBGStyle;

    private ArrayListPreference locationPref;
    private SwitchPreference hideLocNamePref;
    private SwitchPreference hideSettingsBtnPref;

    private SwitchPreference useTimeZonePref;
    private Preference clockPref;
    private Preference calPref;

    private ListPreference bgColorPref;
    private ListPreference bgStylePref;

    private ListPreference fcastOptPref;

    private static final int MAX_LOCATIONS = Settings.getMaxLocations();
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int SETUP_REQUEST_CODE = 10;

    // Preference Keys
    private static final String KEY_CATGENERAL = "key_catgeneral";
    private static final String KEY_LOCATION = "key_location";
    private static final String KEY_HIDELOCNAME = "key_hidelocname";
    private static final String KEY_HIDESETTINGSBTN = "key_hidesettingsbtn";

    private static final String KEY_CATCLOCKDATE = "key_catclockdate";
    private static final String KEY_USETIMEZONE = "key_usetimezone";
    private static final String KEY_CLOCKAPP = "key_clockapp";
    private static final String KEY_CALENDARAPP = "key_calendarapp";

    private static final String KEY_BACKGROUND = "key_background";
    private static final String KEY_BGCOLOR = "key_bgcolor";
    private static final String KEY_BGSTYLE = "key_bgstyle";

    private static final String KEY_FORECAST = "key_forecast";
    private static final String KEY_FORECASTOPTION = "key_fcastoption";

    public WeatherWidgetPreferenceFragment() {
        setArguments(new Bundle());
    }

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

    @Override
    protected int getTitle() {
        return R.string.widget_configure_prompt;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
         * This should be before the super call,
         * so this is setup before onCreatePreferences is called
         */
        args = WeatherWidgetPreferenceFragmentArgs.fromBundle(requireArguments());

        // Find the widget id from the intent.
        mAppWidgetId = args.getAppWidgetId();
        mWidgetType = getWidgetTypeFromID(mAppWidgetId);
        // Set the result value for WidgetConfigActivity
        resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

        wm = WeatherManager.getInstance();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        View inflatedView = root.getChildAt(root.getChildCount() - 1);
        root.removeView(inflatedView);
        binding = FragmentWidgetSetupBinding.inflate(inflater, root, true);
        binding.layoutContainer.addView(inflatedView);

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(root, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        if (getListView() != null)
            ViewCompat.setNestedScrollingEnabled(getListView(), false);

        // Set fragment view
        setHasOptionsMenu(true);

        getAppCompatActivity().setSupportActionBar(getToolbar());
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Context context = root.getContext();
        Drawable navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp));
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText));
        getAppCompatActivity().getSupportActionBar().setHomeAsUpIndicator(navIcon);

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
                    Navigation.findNavController(getView())
                            .navigate(WeatherWidgetPreferenceFragmentDirections.actionWeatherWidgetPreferenceFragmentToLocationSearchFragment2());
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

        if (!StringUtils.isNullOrWhitespace(args.getSimpleWeatherDroidExtraLOCATIONQUERY())) {
            String locName = args.getSimpleWeatherDroidExtraLOCATIONNAME();
            String locQuery = args.getSimpleWeatherDroidExtraLOCATIONQUERY();

            if (locName != null) {
                mLastSelectedValue = locQuery;
                locationPref.setValue(mLastSelectedValue.toString());
            } else {
                locationPref.setValueIndex(0);
            }
        } else {
            locationPref.setValueIndex(0);
        }

        hideLocNamePref = findPreference(KEY_HIDELOCNAME);
        hideSettingsBtnPref = findPreference(KEY_HIDESETTINGSBTN);

        hideLocNamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                hideLocNamePref.setChecked((boolean) newValue);
                updateLocationView();
                return true;
            }
        });

        hideSettingsBtnPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                hideSettingsBtnPref.setChecked((boolean) newValue);
                updateWidgetView();
                return true;
            }
        });

        if (WidgetUtils.isLocationNameOptionalWidget(mWidgetType)) {
            hideLocNamePref.setChecked(WidgetUtils.isLocationNameHidden(mAppWidgetId));
            hideLocNamePref.setVisible(true);
        } else {
            hideLocNamePref.setChecked(false);
            hideLocNamePref.setVisible(false);
        }

        hideSettingsBtnPref.setChecked(WidgetUtils.isSettingsButtonHidden(mAppWidgetId));

        // Time and Date
        clockPref = findPreference(KEY_CLOCKAPP);
        calPref = findPreference(KEY_CALENDARAPP);
        useTimeZonePref = findPreference(KEY_USETIMEZONE);

        clockPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AppChoiceDialogBuilder(requireContext())
                        .setOnItemSelectedListener(new AppChoiceDialogBuilder.OnAppSelectedListener() {
                            @Override
                            public void onItemSelected(@Nullable String key) {
                                WidgetUtils.setOnClickClockApp(key);
                                updateClockPreference();
                            }
                        }).show();
                return true;
            }
        });
        calPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AppChoiceDialogBuilder(requireContext())
                        .setOnItemSelectedListener(new AppChoiceDialogBuilder.OnAppSelectedListener() {
                            @Override
                            public void onItemSelected(@Nullable String key) {
                                WidgetUtils.setOnClickCalendarApp(key);
                                updateCalPreference();
                            }
                        }).show();
                return true;
            }
        });

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            updateClockPreference();
            clockPref.setVisible(true);
        } else {
            clockPref.setVisible(false);
        }

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            updateCalPreference();
            calPref.setVisible(true);
        } else {
            calPref.setVisible(false);
        }

        if (WidgetUtils.isClockWidget(mWidgetType) || WidgetUtils.isDateWidget(mWidgetType)) {
            useTimeZonePref.setChecked(WidgetUtils.useTimeZone(mAppWidgetId));
            findPreference(KEY_CATCLOCKDATE).setVisible(true);
        } else {
            findPreference(KEY_CATCLOCKDATE).setVisible(false);
        }

        // Widget background style
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

        if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
            bgColorPref.setValueIndex(WidgetUtils.getWidgetBackground(mAppWidgetId).getValue());
            bgColorPref.callChangeListener(bgColorPref.getValue());

            bgStylePref.setValueIndex(WidgetUtils.getBackgroundStyle(mAppWidgetId).getValue());
            bgStylePref.callChangeListener(bgStylePref.getValue());
            findPreference(KEY_BACKGROUND).setVisible(true);
        } else {
            bgColorPref.setValueIndex(WidgetUtils.WidgetBackground.TRANSPARENT.getValue());
            findPreference(KEY_BACKGROUND).setVisible(false);
        }

        // Forecast Preferences
        fcastOptPref = findPreference(KEY_FORECASTOPTION);
        fcastOptPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateWidgetView();
                return true;
            }
        });

        if (!WeatherAPI.YAHOO.equals(wm.getWeatherAPI()) && isForecastWidget(mWidgetType)) {
            fcastOptPref.setValueIndex(WidgetUtils.getForecastOption(mAppWidgetId).getValue());
            fcastOptPref.callChangeListener(fcastOptPref.getValue());
            findPreference(KEY_FORECAST).setVisible(true);
        } else {
            fcastOptPref.setValueIndex(WidgetUtils.ForecastOption.FULL.getValue());
            findPreference(KEY_FORECAST).setVisible(false);
        }
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        args = WeatherWidgetPreferenceFragmentArgs.fromBundle(requireArguments());

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
                        query_vm = new LocationQueryViewModel(data);
                        final ComboBoxItem item = new ComboBoxItem(query_vm.getLocationName(), query_vm.getLocationQuery());
                        final int idx = locationPref.getEntryCount() - 1;
                        locationPref.insertEntry(idx, item.getDisplay(), item.getValue());
                        locationPref.setValueIndex(idx);
                        if (locationPref.getEntryCount() > MAX_LOCATIONS) {
                            locationPref.removeEntry(locationPref.getEntryCount() - 1);
                        }
                        locationPref.callChangeListener(item.getValue());

                        Navigation.findNavController(view).getCurrentBackStackEntry()
                                .getSavedStateHandle().remove(Constants.KEY_DATA);
                    } else {
                        query_vm = null;
                    }
                }
            }
        });

        initializeWidget();
        // Resize necessary views
        binding.getRoot().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                binding.getRoot().getViewTreeObserver().removeOnPreDrawListener(this);
                resizeWidgetContainer();
                return true;
            }
        });
    }

    private void updateClockPreference() {
        ComponentName componentName = WidgetUtils.getClockAppComponent(getAppCompatActivity());
        if (componentName != null) {
            try {
                ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(componentName.getPackageName(), 0);
                CharSequence appLabel = getContext().getPackageManager().getApplicationLabel(appInfo);
                clockPref.setSummary(appLabel);
                return;
            } catch (PackageManager.NameNotFoundException e) {
                // App not available
                WidgetUtils.setOnClickClockApp(null);
            }
        }

        clockPref.setSummary(R.string.summary_default);
    }

    private void updateCalPreference() {
        ComponentName componentName = WidgetUtils.getCalendarAppComponent(getAppCompatActivity());
        if (componentName != null) {
            try {
                ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(componentName.getPackageName(), 0);
                CharSequence appLabel = getContext().getPackageManager().getApplicationLabel(appInfo);
                calPref.setSummary(appLabel);
                return;
            } catch (PackageManager.NameNotFoundException e) {
                // App not available
                WidgetUtils.setOnClickClockApp(null);
            }
        }

        calPref.setSummary(R.string.summary_default);
    }

    private void initializeWidget() {
        binding.widgetContainer.removeAllViews();

        int widgetLayoutRes = 0;
        float viewWidth = 0;
        float viewHeight = 0;
        float widgetBlockSize = ActivityUtils.dpToPx(getAppCompatActivity(), 96);

        switch (mWidgetType) {
            case Widget1x1:
                widgetLayoutRes = R.layout.app_widget_1x1;
                viewHeight = viewWidth = widgetBlockSize * 1f;
                break;
            case Widget2x2:
                widgetLayoutRes = R.layout.app_widget_2x2;
                viewHeight = viewWidth = widgetBlockSize * 2.5f;
                break;
            case Widget4x1:
                widgetLayoutRes = R.layout.app_widget_4x1;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize * 1.5f;
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
            case Widget4x1Notification:
                widgetLayoutRes = R.layout.app_widget_4x1_notification;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize * 1.25f;
                break;
            case Widget4x2Clock:
                widgetLayoutRes = R.layout.app_widget_4x2_clock;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize * 2.25f;
                break;
            case Widget4x2Huawei:
                widgetLayoutRes = R.layout.app_widget_4x2_huawei;
                viewWidth = widgetBlockSize * 4;
                viewHeight = widgetBlockSize * 2f;
                break;
        }

        if (widgetLayoutRes == 0) {
            binding.widgetContainer.setVisibility(View.GONE);
            return;
        }

        View widgetView = View.inflate(getAppCompatActivity(), widgetLayoutRes, binding.widgetContainer);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) widgetView.getLayoutParams();

        layoutParams.height = (int) viewHeight;
        layoutParams.width = (int) viewWidth;
        layoutParams.gravity = Gravity.CENTER;

        widgetView.setLayoutParams(layoutParams);

        if (mWidgetType == WidgetType.Widget2x2) {
            ViewGroup notif_layout = widgetView.findViewById(R.id.weather_notif_layout);
            View.inflate(getAppCompatActivity(), R.layout.app_widget_2x2_notif_layout, notif_layout);
        }

        updateTimeAndDate();

        TextView conditionText = widgetView.findViewById(R.id.condition_weather);
        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x1Notification) {
            TextView conditionHi = widgetView.findViewById(R.id.condition_hi);
            TextView conditionlo = widgetView.findViewById(R.id.condition_lo);

            conditionText.setText("70°F - Sunny");
            conditionHi.setText("79°");
            conditionlo.setText("65°");
        } else if (mWidgetType == WidgetType.Widget4x2 || mWidgetType == WidgetType.Widget4x2Clock) {
            conditionText.setText("Sunny");
        } else if (mWidgetType == WidgetType.Widget4x2Huawei) {
            TextView conditionHiLo = widgetView.findViewById(R.id.condition_hilo);
            conditionHiLo.setText("79° | 65°");
        }

        if (mWidgetType != WidgetType.Widget2x2 && mWidgetType != WidgetType.Widget4x1Notification && mWidgetType != WidgetType.Widget4x1) {
            TextView tempView = widgetView.findViewById(R.id.condition_temp);

            SpannableStringBuilder str = new SpannableStringBuilder()
                    .append("70");
            int idx = str.length();
            str.append("°F");
            if (mWidgetType != WidgetType.Widget4x1Google) {
                str.setSpan(new RelativeSizeSpan(0.60f), idx, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                str.setSpan(new SuperscriptSpan(), idx, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            tempView.setText(str);
        }

        if (mWidgetType != WidgetType.Widget4x1) {
            ImageView iconView = widgetView.findViewById(R.id.weather_icon);
            iconView.setImageResource(R.drawable.day_sunny);
        }

        if (isForecastWidget(mWidgetType)) {
            ViewGroup forecastLayout = binding.widgetContainer.findViewById(R.id.forecast_layout);
            forecastLayout.removeAllViews();

            int forecastLength = 3;
            if (mWidgetType == WidgetType.Widget4x2) {
                forecastLength = 5;
            }

            ViewGroup container = (ViewGroup) View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_layout_container, null);

            for (int i = 0; i < forecastLength; i++) {
                View forecastPanel;

                if (mWidgetType == WidgetType.Widget4x1)
                    forecastPanel = View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_panel_4x1, null);
                else
                    forecastPanel = View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_panel_4x2, null);

                forecastPanel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                TextView forecastDate = forecastPanel.findViewById(R.id.forecast_date);
                forecastDate.setText(LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("eee")));

                TextView forecastHi = forecastPanel.findViewById(R.id.forecast_hi);
                forecastHi.setText(75 + i + "°");

                TextView forecastLo = forecastPanel.findViewById(R.id.forecast_lo);
                forecastLo.setText(65 - i + "°");

                ImageView forecastIcon = forecastPanel.findViewById(R.id.forecast_icon);
                forecastIcon.setImageResource(R.drawable.day_sunny);

                container.addView(forecastPanel);
            }

            forecastLayout.addView(container);
        }

        isWidgetInit = true;
        updateWidgetView();
    }

    private void updateLocationView() {
        if (binding == null) return;
        TextView locationView = binding.widgetContainer.findViewById(R.id.location_name);
        locationView.setText(mLastSelectedValue != null ? locationPref.findEntryFromValue(mLastSelectedValue) : this.getString(R.string.pref_location));
        locationView.setVisibility(hideLocNamePref.isChecked() ? View.GONE : View.VISIBLE);
    }

    private void updateTimeAndDate() {
        if (binding == null) return;

        LocalDateTime now = LocalDateTime.now();

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                DateTimeFormatter dtfm;
                if (mWidgetType == WidgetType.Widget2x2) {
                    dtfm = DateTimeFormatter.ofPattern("eeee, MMMM dd");
                } else if (mWidgetType == WidgetType.Widget4x1Google) {
                    dtfm = DateTimeFormatter.ofPattern("eeee, MMM dd");
                } else {
                    dtfm = DateTimeFormatter.ofPattern("eee, MMM dd");
                }

                TextView dateText = binding.widgetContainer.findViewById(R.id.date_panel);
                dateText.setText(now.format(dtfm));
            }
        }

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                SpannableString timeStr;

                if (DateFormat.is24HourFormat(App.getInstance().getAppContext())) {
                    timeStr = new SpannableString(now.format(DateTimeFormatter.ofPattern("HH:mm")));
                } else {
                    timeStr = new SpannableString(now.format(DateTimeFormatter.ofPattern("h:mm")));
                }

                TextView clockView = binding.widgetContainer.findViewById(R.id.clock_panel);
                clockView.setText(timeStr);
            }
        }
    }

    private void updateWidgetView() {
        if (!isWidgetInit) return;

        updateLocationView();

        final int currentNightMode = getAppCompatActivity().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
            ViewGroup bgContainer = binding.widgetContainer.findViewById(R.id.panda_container);
            if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x2) {
                bgContainer.removeAllViews();
                View.inflate(getAppCompatActivity(), R.layout.layout_panda_bg, bgContainer);
            }

            int backgroundColor = WidgetUtils.getBackgroundColor(getAppCompatActivity(), mWidgetBackground);
            ImageView pandaBG = binding.widgetContainer.findViewById(R.id.panda_background);
            ImageView widgetBG = binding.widgetContainer.findViewById(R.id.widgetBackground);

            if (mWidgetBackground == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                if (pandaBG != null) {
                    if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                        pandaBG.setImageResource(R.drawable.widget_background);
                        pandaBG.setColorFilter(isNightMode ? Colors.BLACK : Colors.WHITE);
                    } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.PENDINGCOLOR) {
                        pandaBG.setImageResource(R.drawable.widget_background);
                        pandaBG.setColorFilter(0xFF698AC1);
                    } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                        pandaBG.setImageResource(R.drawable.widget_background);
                        pandaBG.setColorFilter(Colors.WHITE);
                    } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.DARK) {
                        pandaBG.setImageResource(R.drawable.widget_background);
                        pandaBG.setColorFilter(Colors.BLACK);
                    } else {
                        if (bgContainer != null) bgContainer.removeAllViews();
                    }
                }

                widgetBG.setColorFilter(backgroundColor);
                widgetBG.setImageAlpha(0xFF);

                GlideApp.with(this)
                        .load("file:///android_asset/backgrounds/day.jpg")
                        .format(DecodeFormat.PREFER_RGB_565)
                        .centerCrop()
                        .transform(new TransparentOverlay(0x33))
                        .thumbnail(0.75f)
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

        if (mWidgetType != WidgetType.Widget2x2 &&
                mWidgetType != WidgetType.Widget4x1Google &&
                mWidgetType != WidgetType.Widget4x1Notification &&
                mWidgetType != WidgetType.Widget4x2Clock &&
                mWidgetType != WidgetType.Widget4x1) {
            TextView tempView = binding.widgetContainer.findViewById(R.id.condition_temp);
            tempView.setTextColor(textColor);
        }

        boolean is4x2 = mWidgetType == WidgetType.Widget4x2;

        if (mWidgetType != WidgetType.Widget4x1) {
            ImageView iconView = binding.widgetContainer.findViewById(R.id.weather_icon);
            iconView.setColorFilter(is4x2 ? textColor : panelTextColor);
        }

        TextView locationView = binding.widgetContainer.findViewById(R.id.location_name);
        locationView.setTextColor(is4x2 ? textColor : panelTextColor);

        if (mWidgetType != WidgetType.Widget4x1Google && mWidgetType != WidgetType.Widget4x1 && mWidgetType != WidgetType.Widget1x1 && mWidgetType != WidgetType.Widget4x2Huawei) {
            TextView conditionText = binding.widgetContainer.findViewById(R.id.condition_weather);
            conditionText.setTextColor(is4x2 ? textColor : panelTextColor);
        }

        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x1Notification) {
            TextView conditionHi = binding.widgetContainer.findViewById(R.id.condition_hi);
            TextView divider = binding.widgetContainer.findViewById(R.id.divider);
            TextView conditionLo = binding.widgetContainer.findViewById(R.id.condition_lo);
            ImageView hiIconView = binding.widgetContainer.findViewById(R.id.hi_icon);
            ImageView loIconView = binding.widgetContainer.findViewById(R.id.lo_icon);

            conditionHi.setTextColor(panelTextColor);
            divider.setTextColor(panelTextColor);
            conditionLo.setTextColor(panelTextColor);
            hiIconView.setColorFilter(panelTextColor);
            loIconView.setColorFilter(panelTextColor);
        }

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            TextView dateText = binding.widgetContainer.findViewById(R.id.date_panel);
            dateText.setTextColor(textColor);
        }

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            TextView clockView = binding.widgetContainer.findViewById(R.id.clock_panel);
            clockView.setTextColor(textColor);
        }

        if (WidgetUtils.isForecastWidget(mWidgetType)) {
            ViewGroup forecastLayout = binding.widgetContainer.findViewById(R.id.forecast_layout);
            if (forecastLayout != null) {
                updateTextViewColor(forecastLayout, panelTextColor);
            }
        }

        ImageView settButton = binding.widgetContainer.findViewById(R.id.settings_button);
        settButton.setColorFilter(textColor);
        settButton.setVisibility(hideSettingsBtnPref.isChecked() ? View.GONE : View.VISIBLE);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Reset to last selected item
        if (query_vm == null && mLastSelectedValue != null)
            locationPref.setValue(mLastSelectedValue.toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Resize necessary views
        ViewTreeObserver observer = binding.getRoot().getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                resizeWidgetContainer();
            }
        });
    }

    private void resizeWidgetContainer() {
        final View widgetFrameContainer = binding.scrollView.findViewById(R.id.widget_frame_container);
        final View widgetView = binding.widgetContainer.findViewById(R.id.widget);

        int width = binding.scrollView.getMeasuredWidth();

        int preferredHeight = (int) ActivityUtils.dpToPx(getAppCompatActivity(), 225);
        int minHeight = (int) (ActivityUtils.dpToPx(getAppCompatActivity(), 96));

        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x2) {
            minHeight *= 2f;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(binding.scrollView, new AutoTransition());
        }

        if (getAppCompatActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            widgetFrameContainer.setMinimumHeight(preferredHeight);
        } else {
            if (mWidgetType == WidgetType.Widget1x1 || mWidgetType == WidgetType.Widget4x1Google) {
                minHeight *= 1.5f;
            }
            widgetFrameContainer.setMinimumHeight(minHeight);
        }

        if (widgetView != null) {
            FrameLayout.LayoutParams widgetParams = (FrameLayout.LayoutParams) widgetView.getLayoutParams();
            if (widgetView.getMeasuredWidth() > width) {
                widgetParams.width = width;
            }
            widgetParams.gravity = Gravity.CENTER;
            widgetView.setLayoutParams(widgetParams);
        }
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
                getAppCompatActivity().setResult(Activity.RESULT_CANCELED, resultValue);
                getAppCompatActivity().finish();
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
        WidgetUtils.setLocationNameHidden(mAppWidgetId, hideLocNamePref.isChecked());
        WidgetUtils.setSettingsButtonHidden(mAppWidgetId, hideSettingsBtnPref.isChecked());
        WidgetUtils.setForecastOption(mAppWidgetId, Integer.parseInt(fcastOptPref.getValue()));
        WidgetUtils.setUseTimeZone(mAppWidgetId, useTimeZonePref.isChecked());

        // Trigger widget service to update widget
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
}
