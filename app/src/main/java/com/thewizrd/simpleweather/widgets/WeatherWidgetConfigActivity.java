package com.thewizrd.simpleweather.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.TransparentOverlay;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.fragments.LocationSearchFragment;
import com.thewizrd.simpleweather.preferences.ArrayListPreference;
import com.thewizrd.simpleweather.preferences.CustomListPreferenceDialogFragment;
import com.thewizrd.simpleweather.setup.SetupActivity;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.thewizrd.simpleweather.widgets.WidgetUtils.getWidgetTypeFromID;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isForecastWidget;

public class WeatherWidgetConfigActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Widget id for ConfigurationActivity
        int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

        // Find the widget id from the intent.
        if (getIntent() != null && getIntent().getExtras() != null) {
            mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If they gave us an intent without the widget id, just bail.
            finish();
        }

        setContentView(R.layout.activity_widget_setup);

        View mRootView = (View) findViewById(R.id.fragment_container).getParent();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mRootView.setFitsSystemWindows(true);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            Bundle args = new Bundle();
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            if (getIntent() != null
                    && !StringUtils.isNullOrWhitespace(getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY))) {
                String locName = getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONNAME);
                String locQuery = getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY);
                args.putString(WeatherWidgetService.EXTRA_LOCATIONNAME, locName);
                args.putString(WeatherWidgetService.EXTRA_LOCATIONQUERY, locQuery);
            }

            fragment = WeatherWidgetPreferenceFragment.newInstance(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        OnBackPressedFragmentListener fragBackPressedListener = null;
        if (current instanceof OnBackPressedFragmentListener)
            fragBackPressedListener = (OnBackPressedFragmentListener) current;

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed();
        }
    }

    public static class WeatherWidgetPreferenceFragment extends PreferenceFragmentCompat implements OnBackPressedFragmentListener {
        // Widget id for ConfigurationActivity
        private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        private WidgetType mWidgetType = WidgetType.Unknown;
        private Intent resultValue;

        // Location Search
        private Collection<LocationData> favorites;
        private LocationQueryViewModel query_vm = null;

        private AppCompatActivity mActivity;
        private FusedLocationProviderClient mFusedLocationClient;
        private CancellationTokenSource cts;

        // Weather
        private WeatherManager wm;

        // Views
        private View mRootView;
        private AppBarLayout appBarLayout;
        private Toolbar mToolbar;
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

        private static final String KEY_SEARCHUI = "SearchUI";

        private static final String KEY_SEARCH = "Search";
        private static final String KEY_GPS = "GPS";

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

        protected void runOnUiThread(Runnable action) {
            if (mActivity != null)
                mActivity.runOnUiThread(action);
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mActivity = (AppCompatActivity) context;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mActivity = null;
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
        public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
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
            mSearchFragmentContainer = root.findViewById(R.id.search_fragment_container);

            mRootView = (View) mActivity.findViewById(R.id.fragment_container).getParent();
            // Make full transparent statusBar
            updateWindowColors();

            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                    return insets;
                }
            });

            ViewCompat.setOnApplyWindowInsetsListener(mScrollView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });

            ViewCompat.setOnApplyWindowInsetsListener(mSearchFragmentContainer, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });

            mActivity.setSupportActionBar(mToolbar);
            mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mActivity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);

            mSearchFragmentContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exitSearchUi(false);
                }
            });

            cts = new CancellationTokenSource();

            // Location Listener
            if (WearableHelper.isGooglePlayServicesInstalled()) {
                mFusedLocationClient = new FusedLocationProviderClient(mActivity);
            }

            if (!Settings.isWeatherLoaded()) {
                Toast.makeText(mActivity, R.string.prompt_setup_app_first, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(mActivity, SetupActivity.class)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                startActivityForResult(intent, SETUP_REQUEST_CODE);
            }

            return root;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_widgetconfig, null);

            locationPref = findPreference(KEY_LOCATION);
            locationPref.addEntry(R.string.pref_item_gpslocation, KEY_GPS);
            locationPref.addEntry(R.string.label_btn_add_location, KEY_SEARCH);

            List<LocationData> favs = Settings.getFavorites();
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
                    if (KEY_SEARCH.equals(selectedValue)) {
                        // Setup search UI
                        prepareSearchUI();
                        query_vm = null;
                        return false;
                    } else {
                        mLastSelectedValue = selectedValue;
                    }

                    query_vm = null;
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
            if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_SEARCHUI, false)) {
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
            resizeWidgetContainer();
        }

        private void initializeWidget() {
            widgetContainer.removeAllViews();

            int widgetLayoutRes = 0;
            float viewWidth = 0;
            float viewHeight = 0;
            float widgetBlockSize = ActivityUtils.dpToPx(mActivity, 90);

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

            View widgetView = View.inflate(mActivity, widgetLayoutRes, widgetContainer);
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

                updateTime.setText(String.format("%s %s", mActivity.getString(R.string.widget_updateprefix), updatetime));
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
                                    ContextCompat.getColorStateList(mActivity, android.R.color.white),
                                    ContextCompat.getColorStateList(mActivity, android.R.color.white)),
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

                ViewGroup container = (ViewGroup) View.inflate(mActivity, R.layout.app_widget_forecast_layout_container, null);

                for (int i = 0; i < forecastLength; i++) {
                    View forecastPanel = null;

                    if (mWidgetType == WidgetType.Widget4x1)
                        forecastPanel = View.inflate(mActivity, R.layout.app_widget_forecast_panel_4x1, null);
                    else
                        forecastPanel = View.inflate(mActivity, R.layout.app_widget_forecast_panel_4x2, null);

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
            locationView.setText(mLastSelectedValue != null ? locationPref.findEntryFromValue(mLastSelectedValue) : mActivity.getString(R.string.pref_location));
        }

        private void updateWidgetView() {
            if (!isWidgetInit) return;

            updateLocationView();

            final int currentNightMode = mActivity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            boolean isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

            if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
                int backgroundColor = WidgetUtils.getBackgroundColor(mActivity, mWidgetBackground);
                ImageView pandaBG = widgetContainer.findViewById(R.id.panda_background);
                ImageView widgetBG = widgetContainer.findViewById(R.id.widgetBackground);

                if (mWidgetBackground == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                    if (pandaBG != null) {
                        if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                            pandaBG.setColorFilter(isNightMode ? Colors.BLACK : Colors.WHITE);
                            pandaBG.setImageResource(R.drawable.widget_background_bottom_corners);
                        } else if (mWidgetBGStyle == WidgetUtils.WidgetBackgroundStyle.PENDINGCOLOR) {
                            pandaBG.setColorFilter(0xFF20A8D8);
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

                    Glide.with(mActivity)
                            .load("file:///android_asset/backgrounds/DaySky.jpg")
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
        public void onDisplayPreferenceDialog(Preference preference) {
            final String TAG = "CustomListPreferenceDialogFragment";

            // check if dialog is already showing
            if (getFragmentManager().findFragmentByTag(TAG) != null)
                return;

            if ((preference instanceof ListPreference)) {
                final CustomListPreferenceDialogFragment f = CustomListPreferenceDialogFragment.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                if (ActivityUtils.isSmallestWidth(mActivity, 400) &&
                        (ActivityUtils.getOrientation(mActivity) != Configuration.ORIENTATION_LANDSCAPE || ActivityUtils.isLargeTablet(mActivity))) {
                    f.show(getFragmentManager(), TAG);
                } else {
                    f.showFullScreen(getFragmentManager(), android.R.id.content, null);
                }
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public boolean onBackPressed() {
            if (inSearchUI) {
                // We should let the user go back to usual screens with tabs.
                exitSearchUi(false);
                return true;
            } else {
                mActivity.setResult(RESULT_CANCELED, resultValue);
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
            outState.putBoolean(KEY_SEARCHUI, inSearchUI);

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
            enterSearchUiTransition(null);
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

        private void enterSearchUiTransition(Animation.AnimationListener enterAnimationListener) {
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
            mSearchFragmentContainer.startAnimation(fragmentAniSet);
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
            mSearchFragment.setUserVisibleHint(false);
            final FragmentTransaction transaction = getChildFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            mSearchFragment = null;
            transaction.commitAllowingStateLoss();
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

            hideInputMethod(mActivity == null ? null : mActivity.getCurrentFocus());
            inSearchUI = false;

            // Reset to last selected item
            if (query_vm == null && mLastSelectedValue != null)
                locationPref.setValue(mLastSelectedValue.toString());
        }

        private void exitSearchUiTransition(Animation.AnimationListener exitAnimationListener) {
            // FragmentContainer fade/translation animation
            AnimationSet fragmentAniSet = new AnimationSet(true);
            fragmentAniSet.setInterpolator(new DecelerateInterpolator());
            AlphaAnimation fragFadeAni = new AlphaAnimation(1.0f, 0.0f);
            TranslateAnimation fragmentAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.ABSOLUTE, 0,
                    Animation.ABSOLUTE, mSearchFragmentContainer.getRootView().getHeight());
            fragmentAniSet.setDuration(ANIMATION_DURATION);
            fragmentAniSet.setFillEnabled(false);
            fragmentAniSet.addAnimation(fragFadeAni);
            fragmentAniSet.addAnimation(fragmentAnimation);
            fragmentAniSet.setAnimationListener(exitAnimationListener);

            mSearchFragmentContainer.startAnimation(fragmentAniSet);
        }

        private RecyclerOnClickListenerInterface recyclerClickListener = new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(final View view, final int position) {
                if (mSearchFragment == null)
                    return;

                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        final LocationQueryAdapter adapter = mSearchFragment.getAdapter();
                        LocationQuery v = (LocationQuery) view;
                        LocationQueryViewModel query_vm = null;

                        try {
                            if (!StringUtils.isNullOrEmpty(adapter.getDataset().get(position).getLocationQuery()))
                                query_vm = adapter.getDataset().get(position);
                        } catch (Exception e) {
                            query_vm = null;
                        } finally {
                            if (query_vm == null)
                                query_vm = new LocationQueryViewModel();
                        }

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return;
                        }

                        // Cancel other tasks
                        mSearchFragment.ctsCancel();

                        mSearchFragment.showLoading(true);

                        if (mSearchFragment.ctsCancelRequested()) {
                            mSearchFragment.showLoading(false);
                            query_vm = null;
                            return;
                        }

                        // Need to get FULL location data for HERE API
                        // Data provided is incomplete
                        if (WeatherAPI.HERE.equals(query_vm.getLocationSource())
                                && query_vm.getLocationLat() == -1 && query_vm.getLocationLong() == -1
                                && query_vm.getLocationTZLong() == null) {
                            final LocationQueryViewModel loc = query_vm;
                            try {
                                query_vm = new AsyncTaskEx<LocationQueryViewModel, WeatherException>().await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                                    @Override
                                    public LocationQueryViewModel call() throws WeatherException {
                                        return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                                    }
                                });
                            } catch (final WeatherException wEx) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(mRootView, wEx.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                                mSearchFragment.showLoading(false);
                                query_vm = null;
                                return;
                            }
                        }

                        // Check if location already exists
                        LocationData loc = null;
                        boolean exists = false;
                        for (LocationData l : favorites) {
                            if (l.getQuery().equals(query_vm.getLocationQuery())) {
                                loc = l;
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showLoading(false);
                                    exitSearchUi(false);
                                }
                            });

                            // Set selection
                            query_vm = null;
                            locationPref.setValue(loc.getQuery());
                            return;
                        }

                        if (mSearchFragment.ctsCancelRequested()) {
                            mSearchFragment.showLoading(false);
                            query_vm = null;
                            return;
                        }

                        // We got our data so disable controls just in case
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.getDataset().clear();
                                adapter.notifyDataSetChanged();

                                if (mSearchFragment != null && mSearchFragment.getView() != null &&
                                        mSearchFragment.getView().findViewById(R.id.recycler_view) instanceof RecyclerView) {
                                    RecyclerView recyclerView = mSearchFragment.getView().findViewById(R.id.recycler_view);
                                    recyclerView.setEnabled(false);
                                }
                            }
                        });

                        // Save data
                        WeatherWidgetPreferenceFragment.this.query_vm = query_vm;
                        final ComboBoxItem item = new ComboBoxItem(query_vm.getLocationName(), query_vm.getLocationQuery());
                        final int idx = locationPref.getEntryCount() - 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                locationPref.insertEntry(idx, item.getDisplay(), item.getValue());
                                locationPref.setValueIndex(idx);
                                if (locationPref.getEntryCount() > MAX_LOCATIONS) {
                                    locationPref.removeEntry(locationPref.getEntryCount() - 1);
                                }
                                locationPref.callChangeListener(item.getValue());
                            }
                        });

                        // Hide dialog
                        mSearchFragment.showLoading(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                exitSearchUi(false);
                            }
                        });
                    }
                });
            }
        };

        private void updateWindowColors() {
            final Configuration config = this.getResources().getConfiguration();

            // Set user theme
            int bg_color;
            if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
                appBarLayout.setBackgroundColor(Colors.BLACK);
                ActivityUtils.setTransparentWindow(mActivity.getWindow(), bg_color,
                        Colors.BLACK, /* StatusBar */
                        Colors.TRANSPARENT /* NavBar */,
                        false);
            } else {
                final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
                boolean isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

                bg_color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
                int colorPrimary = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
                int color = isDarkMode ? bg_color : colorPrimary;
                appBarLayout.setBackgroundColor(color);
                ActivityUtils.setTransparentWindow(mActivity.getWindow(), bg_color,
                        color, /* StatusBar */
                        config.orientation == Configuration.ORIENTATION_PORTRAIT || ActivityUtils.isLargeTablet(mActivity) ? Colors.TRANSPARENT : color /* NavBar */,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
            }
            mRootView.setBackgroundColor(bg_color);
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

            widgetFrameContainer.post(new Runnable() {
                @Override
                public void run() {
                    if (mActivity != null) {
                        int height = mScrollView.getMeasuredHeight();
                        int width = mScrollView.getMeasuredWidth();

                        int preferredHeight = (int) ActivityUtils.dpToPx(mActivity, 225);
                        int minHeight = (int) (ActivityUtils.dpToPx(mActivity, 90));

                        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x2) {
                            minHeight *= 2.5f;
                        }

                        TransitionManager.beginDelayedTransition(mScrollView, new AutoTransition());

                        ViewGroup.LayoutParams layoutParams = widgetFrameContainer.getLayoutParams();

                        if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
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
                }
            });
        }

        @Override
        public void onPause() {
            if (cts != null) cts.cancel();
            super.onPause();
        }

        @Override
        public void onDestroy() {
            if (cts != null) cts.cancel();
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
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == SETUP_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    // Get result data
                    String dataJson = (data == null || !data.hasExtra("data")) ? null : data.getStringExtra("data");

                    if (!StringUtils.isNullOrWhitespace(dataJson)) {
                        JsonReader reader = new JsonReader(new StringReader(dataJson));
                        LocationData locData = LocationData.fromJson(reader);

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
                    mActivity.setResult(RESULT_CANCELED, resultValue);
                    mActivity.finish();
                }
            }
        }

        @Override
        public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            super.onCreateOptionsMenu(menu, menuInflater);
            // Inflate the menu; this adds items to the action bar if it is present.
            menu.clear();
            menuInflater.inflate(R.menu.menu_widgetsetup, menu);
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
                        mActivity.setResult(RESULT_CANCELED, resultValue);
                        mActivity.finish();
                    }
                    return true;
                case R.id.action_done:
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            prepareWidget();
                        }
                    });
                    return true;
                default:
                    break;
            }

            return super.onOptionsItemSelected(item);
        }

        private void prepareWidget() {
            // Update Settings
            try {
                int refreshValue = Integer.valueOf(refreshPref.getValue());
                Settings.setRefreshInterval(refreshValue);
            } catch (NumberFormatException e) {
                // DO nothing
            }

            // Get location data
            if (locationPref.getValue() != null) {
                String locationItemValue = locationPref.getValue();
                LocationData locData = null;

                // Widget ID exists in prefs
                if (WidgetUtils.exists(mAppWidgetId)) {
                    locData = WidgetUtils.getLocationData(mAppWidgetId);

                    // Handle location changes
                    if (KEY_GPS.equals(locationItemValue)) {
                        // Changing location to GPS
                        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                            return;
                        }

                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check if last location exists
                        if (lastGPSLocData == null && !updateLocation()) {
                            Snackbar.make(mRootView, R.string.error_retrieve_location, Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        Settings.setFollowGPS(true);

                        // Reset data for widget
                        WidgetUtils.deleteWidget(mAppWidgetId);
                        WidgetUtils.saveLocationData(mAppWidgetId, null);
                        WidgetUtils.addWidgetId(KEY_GPS, mAppWidgetId);
                    } else {
                        // Changing location to whatever
                        if (locData == null || !locationItemValue.equals(locData.getQuery())) {
                            // Get location data
                            String itemValue = locationPref.getValue();
                            boolean exists = false;
                            for (LocationData loc : favorites) {
                                if (loc.getQuery().equals(itemValue)) {
                                    locData = loc;
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                locData = null;
                            }

                            if (locData == null && query_vm != null) {
                                locData = new LocationData(query_vm);

                                if (!locData.isValid()) {
                                    mActivity.setResult(RESULT_CANCELED, resultValue);
                                    mActivity.finish();
                                    return;
                                }

                                // Add location to favs
                                Settings.addLocation(locData);
                            } else if (locData == null) {
                                mActivity.setResult(RESULT_CANCELED, resultValue);
                                mActivity.finish();
                                return;
                            }

                            // Save locdata for widget
                            WidgetUtils.deleteWidget(mAppWidgetId);
                            WidgetUtils.saveLocationData(mAppWidgetId, locData);
                            WidgetUtils.addWidgetId(locData.getQuery(), mAppWidgetId);
                        }
                    }
                } else {
                    if (KEY_GPS.equals(locationItemValue)) {
                        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                            return;
                        }

                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check if last location exists
                        if (lastGPSLocData == null && !updateLocation()) {
                            Snackbar.make(mRootView, R.string.error_retrieve_location, Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        Settings.setFollowGPS(true);

                        // Save locdata for widget
                        WidgetUtils.deleteWidget(mAppWidgetId);
                        WidgetUtils.saveLocationData(mAppWidgetId, null);
                        WidgetUtils.addWidgetId(KEY_GPS, mAppWidgetId);
                    } else {
                        // Get location data
                        String itemValue = locationPref.getValue();
                        boolean exists = false;
                        for (LocationData loc : favorites) {
                            if (loc.getQuery().equals(itemValue)) {
                                locData = loc;
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            locData = null;
                        }

                        if (locData == null && query_vm != null) {
                            locData = new LocationData(query_vm);

                            if (!locData.isValid()) {
                                mActivity.setResult(RESULT_CANCELED, resultValue);
                                mActivity.finish();
                                return;
                            }

                            // Add location to favs
                            Settings.addLocation(locData);
                        } else if (locData == null) {
                            mActivity.setResult(RESULT_CANCELED, resultValue);
                            mActivity.finish();
                            return;
                        }

                        // Save locdata for widget
                        WidgetUtils.deleteWidget(mAppWidgetId);
                        WidgetUtils.saveLocationData(mAppWidgetId, locData);
                        WidgetUtils.addWidgetId(locData.getQuery(), mAppWidgetId);
                    }
                }

                // Save widget preferences
                WidgetUtils.setWidgetBackground(mAppWidgetId, Integer.parseInt(bgColorPref.getValue()));
                WidgetUtils.setBackgroundStyle(mAppWidgetId, Integer.parseInt(bgStylePref.getValue()));
                WidgetUtils.setTapToSwitchEnabled(mAppWidgetId, tap2SwitchPref.isChecked());

                // Trigger widget service to update widget
                WeatherWidgetService.enqueueWork(mActivity,
                        new Intent(mActivity, WeatherWidgetService.class)
                                .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS, new int[]{mAppWidgetId})
                                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, mWidgetType.getValue()));

                // Create return intent
                mActivity.setResult(RESULT_OK, resultValue);
                mActivity.finish();
            } else {
                mActivity.setResult(RESULT_CANCELED, resultValue);
                mActivity.finish();
            }
        }

        private boolean updateLocation() {
            return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    boolean locationChanged = false;

                    if (Settings.useFollowGPS()) {
                        if (ContextCompat.checkSelfPermission(App.getInstance().getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(App.getInstance().getAppContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return false;
                        }

                        Location location = null;

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
                        } else {
                            LocationManager locMan = (LocationManager) App.getInstance().getAppContext().getSystemService(Context.LOCATION_SERVICE);
                            boolean isGPSEnabled = false;
                            boolean isNetEnabled = false;
                            if (locMan != null) {
                                isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                                isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                            }

                            if (isGPSEnabled || isNetEnabled && !isCtsCancelRequested()) {
                                Criteria locCriteria = new Criteria();
                                locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                                locCriteria.setCostAllowed(false);
                                locCriteria.setPowerRequirement(Criteria.POWER_LOW);
                                String provider = locMan.getBestProvider(locCriteria, true);
                                location = locMan.getLastKnownLocation(provider);
                            }
                        }

                        if (location != null && !isCtsCancelRequested()) {
                            LocationQueryViewModel query_vm = null;

                            TaskCompletionSource<LocationQueryViewModel> tcs = new TaskCompletionSource<>(cts.getToken());
                            tcs.setResult(wm.getLocation(location));
                            try {
                                query_vm = Tasks.await(tcs.getTask());
                            } catch (ExecutionException e) {
                                query_vm = new LocationQueryViewModel();
                                Logger.writeLine(Log.ERROR, e.getCause());
                            } catch (InterruptedException e) {
                                return false;
                            }

                            if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                                query_vm = new LocationQueryViewModel();

                            if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                                // Stop since there is no valid query
                                return false;
                            }

                            if (isCtsCancelRequested()) return locationChanged;

                            // Save location as last known
                            Settings.saveLastGPSLocData(new LocationData(query_vm, location));

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
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        Snackbar.make(mRootView, R.string.error_location_denied, Snackbar.LENGTH_SHORT).show();
                    }
                    return;
                }
                default:
                    break;
            }
        }

        private void showInputMethod(View view) {
            if (mActivity != null) {
                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if (imm != null && view != null) {
                    imm.showSoftInput(view, 0);
                }
            }
        }

        private void hideInputMethod(View view) {
            if (mActivity != null) {
                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if (imm != null && view != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
    }
}
