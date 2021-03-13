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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
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
import android.widget.TextClock;
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
import androidx.lifecycle.SavedStateHandle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
import com.thewizrd.shared_resources.tzdb.TZDBCache;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.CustomException;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.TransparentOverlay;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.GlideApp;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding;
import com.thewizrd.simpleweather.preferences.ArrayListPreference;
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat;
import com.thewizrd.simpleweather.setup.SetupActivity;
import com.thewizrd.simpleweather.snackbar.Snackbar;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

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
    private CancellationTokenSource cts = new CancellationTokenSource();

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // Weather
    private final WeatherManager wm = WeatherManager.getInstance();

    // Views
    private FragmentWidgetSetupBinding binding;

    private CharSequence mLastSelectedValue;

    private boolean isWidgetInit;
    private WidgetUtils.WidgetBackground mWidgetBackground;
    private WidgetUtils.WidgetBackgroundStyle mWidgetBGStyle;
    private int mWidgetBackgroundColor;
    private int mWidgetTextColor;

    private ArrayListPreference locationPref;
    private SwitchPreference hideLocNamePref;
    private SwitchPreference hideSettingsBtnPref;

    private SwitchPreference useTimeZonePref;
    private Preference clockPref;
    private Preference calPref;

    private ListPreference bgChoicePref;
    private ColorPreference bgColorPref;
    private ColorPreference txtColorPref;
    private ListPreference bgStylePref;

    private ListPreference fcastOptPref;

    private static final int MAX_LOCATIONS = Settings.getMaxLocations();
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int PERMISSION_BGLOCATION_REQUEST_CODE = 1;
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
    private static final String KEY_BGCOLORCODE = "key_bgcolorcode";
    private static final String KEY_TXTCOLORCODE = "key_txtcolorcode";
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
        cts.cancel();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        cts.cancel();
        super.onDestroy();
    }

    private void resetTokenSource() {
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

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    stopLocationUpdates();
                    mMainHandler.removeCallbacks(cancelLocRequestRunner);

                    Timber.tag("WidgetPrefFrag").i("Fused: Location update received...");

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
                }

                @Override
                public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                    if (!locationAvailability.isLocationAvailable()) {
                        stopLocationUpdates();
                        mMainHandler.removeCallbacks(cancelLocRequestRunner);

                        Timber.tag("WidgetPrefFrag").i("Fused: Location update unavailable...");

                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                    }
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mMainHandler.removeCallbacks(cancelLocRequestRunner);
                    stopLocationUpdates();

                    Timber.tag("WidgetPrefFrag").i("LocMan: Location update received...");

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
    @SuppressLint("MissingPermission")
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "LocationsFragment: stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        if (mLocCallback != null && mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocCallback)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRequestingLocationUpdates = false;
                        }
                    });
        }

        if (mLocListnr != null && getAppCompatActivity() != null) {
            LocationManager locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);
            if (locMan != null) {
                locMan.removeUpdates(mLocListnr);
            }
            mRequestingLocationUpdates = false;
        }
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
                resetTokenSource();

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
        bgChoicePref = findPreference(KEY_BGCOLOR);
        bgStylePref = findPreference(KEY_BGSTYLE);
        bgColorPref = findPreference(KEY_BGCOLORCODE);
        txtColorPref = findPreference(KEY_TXTCOLORCODE);

        bgChoicePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mWidgetBackground = WidgetUtils.WidgetBackground.valueOf(Integer.parseInt(newValue.toString()));
                updateWidgetView();

                bgColorPref.setVisible(mWidgetBackground == WidgetUtils.WidgetBackground.CUSTOM);
                txtColorPref.setVisible(mWidgetBackground == WidgetUtils.WidgetBackground.CUSTOM);

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

        bgColorPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mWidgetBackgroundColor = (Integer) newValue;
                updateWidgetView();
                return true;
            }
        });

        txtColorPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mWidgetTextColor = (Integer) newValue;
                updateWidgetView();
                return true;
            }
        });

        WidgetUtils.WidgetBackgroundStyle[] styles = WidgetUtils.WidgetBackgroundStyle.values();
        CharSequence[] styleEntries = new CharSequence[styles.length];
        CharSequence[] styleEntryValues = new CharSequence[styles.length];
        for (int i = 0; i < styles.length; i++) {
            WidgetUtils.WidgetBackgroundStyle style = styles[i];
            switch (style) {
                case FULLBACKGROUND:
                    styleEntries[i] = requireContext().getString(R.string.label_style_fullbg);
                    styleEntryValues[i] = Integer.toString(style.getValue());
                    break;
                case PANDA:
                    styleEntries[i] = requireContext().getString(R.string.label_style_panda);
                    styleEntryValues[i] = Integer.toString(style.getValue());
                    bgStylePref.setDefaultValue(styleEntryValues[i]);
                    break;
                case PENDINGCOLOR:
                    styleEntries[i] = requireContext().getText(R.string.label_style_pendingcolor);
                    styleEntryValues[i] = Integer.toString(style.getValue());
                    break;
                case DARK:
                    styleEntries[i] = requireContext().getText(R.string.label_style_dark);
                    styleEntryValues[i] = Integer.toString(style.getValue());
                    break;
                case LIGHT:
                    styleEntries[i] = requireContext().getText(R.string.label_style_light);
                    styleEntryValues[i] = Integer.toString(style.getValue());
                    break;
            }
        }
        bgStylePref.setEntries(styleEntries);
        bgStylePref.setEntryValues(styleEntryValues);

        mWidgetBackground = WidgetUtils.getWidgetBackground(mAppWidgetId);
        mWidgetBGStyle = WidgetUtils.getBackgroundStyle(mAppWidgetId);
        mWidgetBackgroundColor = WidgetUtils.getBackgroundColor(mAppWidgetId);
        mWidgetTextColor = WidgetUtils.getTextColor(mAppWidgetId);

        if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
            bgChoicePref.setValueIndex(mWidgetBackground.getValue());
            bgChoicePref.callChangeListener(bgChoicePref.getValue());

            bgStylePref.setValueIndex(Arrays.asList(WidgetUtils.WidgetBackgroundStyle.values()).indexOf(mWidgetBGStyle));
            bgStylePref.callChangeListener(bgStylePref.getValue());

            bgColorPref.setColor(mWidgetBackgroundColor);
            bgColorPref.callChangeListener(bgColorPref.getColor());
            txtColorPref.setColor(mWidgetTextColor);
            txtColorPref.callChangeListener(txtColorPref.getColor());

            findPreference(KEY_BACKGROUND).setVisible(true);
        } else {
            bgChoicePref.setValueIndex(WidgetUtils.WidgetBackground.TRANSPARENT.getValue());
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

        if (isForecastWidget(mWidgetType)) {
            fcastOptPref.setValueIndex(WidgetUtils.getForecastOption(mAppWidgetId).getValue());
            fcastOptPref.callChangeListener(fcastOptPref.getValue());
            findPreference(KEY_FORECAST).setVisible(true);
        } else {
            fcastOptPref.setValueIndex(WidgetUtils.ForecastOption.FULL.getValue());
            findPreference(KEY_FORECAST).setVisible(false);
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorPreference) {
            ColorPreferenceDialogFragment f = ColorPreferenceDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), ColorPreferenceDialogFragment.class.getName());
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        args = WeatherWidgetPreferenceFragmentArgs.fromBundle(requireArguments());

        final SavedStateHandle savedStateHandle = Navigation.findNavController(view).getCurrentBackStackEntry()
                .getSavedStateHandle();
        final MutableLiveData<String> liveData = savedStateHandle.getLiveData(Constants.KEY_DATA);

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

                        savedStateHandle.remove(Constants.KEY_DATA);
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
                if (isViewAlive()) {
                    binding.getRoot().getViewTreeObserver().removeOnPreDrawListener(this);
                    resizeWidgetContainer();
                }
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
        float widgetBlockSize = ContextUtils.dpToPx(getAppCompatActivity(), 96);

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

            tempView.setText(str);
        }

        if (mWidgetType != WidgetType.Widget4x1) {
            ImageView iconView = widgetView.findViewById(R.id.weather_icon);
            iconView.setImageResource(R.drawable.wi_day_sunny);
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
                View forecastPanel = View.inflate(getAppCompatActivity(), R.layout.app_widget_forecast_item, null);

                forecastPanel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                TextView forecastDate = forecastPanel.findViewById(R.id.forecast_date);
                forecastDate.setText(LocalDateTime.now().plusDays(i).format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK)));

                TextView forecastHi = forecastPanel.findViewById(R.id.forecast_hi);
                forecastHi.setText(75 + i + "°");

                TextView forecastLo = forecastPanel.findViewById(R.id.forecast_lo);
                forecastLo.setText(65 - i + "°");

                ImageView forecastIcon = forecastPanel.findViewById(R.id.forecast_icon);
                forecastIcon.setImageResource(R.drawable.wi_day_sunny);

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
        if (locationView != null) {
            locationView.setText(mLastSelectedValue != null ? locationPref.findEntryFromValue(mLastSelectedValue) : this.getString(R.string.pref_location));
            locationView.setVisibility(hideLocNamePref.isChecked() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateTimeAndDate() {
        if (binding == null) return;

        LocalDateTime now = LocalDateTime.now();

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            String datePattern;
            if (mWidgetType == WidgetType.Widget2x2) {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_LONG_DATE_FORMAT);
            } else if (mWidgetType == WidgetType.Widget4x1Google) {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_WDAY_ABBR_MONTH_FORMAT);
            } else {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_SHORT_DATE_FORMAT);
            }

            TextClock dateClock = binding.widgetContainer.findViewById(R.id.date_panel);
            dateClock.setFormat12Hour(datePattern);
            dateClock.setFormat24Hour(datePattern);
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

            int backgroundColor;
            switch (mWidgetBackground) {
                case CUSTOM:
                    backgroundColor = mWidgetBackgroundColor;
                    break;
                case TRANSPARENT:
                default:
                    backgroundColor = Colors.TRANSPARENT;
                    break;
            }

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
                widgetBG.setImageDrawable(new ColorDrawable(backgroundColor));
                widgetBG.setColorFilter(Colors.TRANSPARENT);
                widgetBG.setImageAlpha(0xFF);
                if (pandaBG != null) {
                    pandaBG.setColorFilter(Colors.TRANSPARENT);
                    pandaBG.setImageBitmap(null);
                }
            }
        }

        // Set text color
        int textColor = mWidgetBackground != WidgetUtils.WidgetBackground.CUSTOM ? WidgetUtils.getTextColor(mAppWidgetId, mWidgetBackground) : mWidgetTextColor;
        int panelTextColor = mWidgetBackground != WidgetUtils.WidgetBackground.CUSTOM ? WidgetUtils.getPanelTextColor(mAppWidgetId, mWidgetBackground, mWidgetBGStyle, isNightMode) : mWidgetTextColor;

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
                if (isViewAlive()) {
                    binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    resizeWidgetContainer();
                }
            }
        });
    }

    private void resizeWidgetContainer() {
        final View widgetFrameContainer = binding.scrollView.findViewById(R.id.widget_frame_container);
        final View widgetView = binding.widgetContainer.findViewById(R.id.widget);

        int width = binding.scrollView.getMeasuredWidth();

        int preferredHeight = (int) ContextUtils.dpToPx(getAppCompatActivity(), 225);
        int minHeight = (int) (ContextUtils.dpToPx(getAppCompatActivity(), 96));

        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x2) {
            minHeight *= 2f;
        }

        TransitionManager.beginDelayedTransition(binding.scrollView, new AutoTransition());

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
                resetTokenSource();
                final CancellationToken token = cts.getToken();

                // Check location
                AsyncTask.create(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws CustomException, InterruptedException {
                        // Changing location to GPS
                        if (ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
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
                }).addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean success) {
                        if (success && !token.isCancellationRequested()) {
                            Settings.setFollowGPS(true);

                            // Reset data for widget
                            WidgetUtils.deleteWidget(mAppWidgetId);
                            WidgetUtils.saveLocationData(mAppWidgetId, null);
                            WidgetUtils.addWidgetId(Constants.KEY_GPS, mAppWidgetId);

                            finalizeWidgetUpdate();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
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
                }).addOnSuccessListener(new OnSuccessListener<LocationData>() {
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
                }).addOnFailureListener(new OnFailureListener() {
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
        WidgetUtils.setWidgetBackground(mAppWidgetId, mWidgetBackground.getValue());
        WidgetUtils.setBackgroundColor(mAppWidgetId, mWidgetBackgroundColor);
        WidgetUtils.setTextColor(mAppWidgetId, mWidgetTextColor);
        WidgetUtils.setBackgroundStyle(mAppWidgetId, mWidgetBGStyle.getValue());
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
        return AsyncTask.await(new CallableEx<Boolean, CustomException>() {
            @SuppressLint("MissingPermission")
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
                    location = AsyncTask.await(new Callable<Location>() {
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
                        mLocationRequest.setNumUpdates(1);
                        mLocationRequest.setInterval(5000);
                        mLocationRequest.setFastestInterval(1000);
                        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        mRequestingLocationUpdates = true;
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocCallback, Looper.getMainLooper());
                        mMainHandler.postDelayed(cancelLocRequestRunner, 30000);
                    }
                } else {
                    boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                    if (isGPSEnabled) {
                        location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }

                    if (isGPSEnabled || isNetEnabled) {
                        String provider = isGPSEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

                        if ((isGPSEnabled && location == null) || (!isGPSEnabled && isNetEnabled)) {
                            location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }

                        if (location == null) {
                            Timber.tag("WidgetPrefFrag").i("LocMan: Requesting location update...");

                            mRequestingLocationUpdates = true;
                            locMan.requestSingleUpdate(provider, mLocListnr, Looper.getMainLooper());
                            mMainHandler.postDelayed(cancelLocRequestRunner, 30000);
                        }
                    }
                }

                resetTokenSource();
                CancellationToken token = cts.getToken();

                if (location != null && !token.isCancellationRequested()) {
                    LocationQueryViewModel query_vm;

                    try {
                        final Location finalLocation = location;
                        query_vm = AsyncTask.await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                            @Override
                            public LocationQueryViewModel call() throws WeatherException {
                                return wm.getLocation(finalLocation);
                            }
                        }, token);
                    } catch (WeatherException e) {
                        Logger.writeLine(Log.ERROR, e);
                        return false;
                    }

                    if (query_vm == null || StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                        // Stop since there is no valid query
                        return false;
                    } else if (StringUtils.isNullOrWhitespace(query_vm.getLocationTZLong()) && query_vm.getLocationLat() != 0 && query_vm.getLocationLong() != 0) {
                        String tzId = TZDBCache.getTimeZone(query_vm.getLocationLat(), query_vm.getLocationLong());
                        if (!"unknown".equals(tzId))
                            query_vm.setLocationTZLong(tzId);
                    }

                    if (token.isCancellationRequested()) return false;

                    // Save location as last known
                    Settings.saveLastGPSLocData(new LocationData(query_vm, location));

                    LocalBroadcastManager.getInstance(getAppCompatActivity())
                            .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));

                    locationChanged = true;
                }

                return locationChanged;
            }
        });
    }

    private final Runnable cancelLocRequestRunner = () -> {
        stopLocationUpdates();
        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE:
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
                break;
            default:
                break;
        }
    }
}
