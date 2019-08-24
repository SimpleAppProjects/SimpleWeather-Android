package com.thewizrd.simpleweather.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DarkMode;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.LocationSearchFragment;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.SetupActivity;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.preferences.ListAdapterPreference;
import com.thewizrd.simpleweather.preferences.ListAdapterPreferenceDialogFragment;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);

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
                    .replace(android.R.id.content, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(android.R.id.content);
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
        private LocationSearchFragment mSearchFragment;
        private CollapsingToolbarLayout collapsingToolbar;
        private ComboBoxItem selectedItem;
        private boolean inSearchUI;

        private ListAdapterPreference locationPref;
        private ListPreference refreshPref;
        private ListPreference bgColorPref;

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

                // Set the result value for WidgetConfigActivity
                resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            }

            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
            ViewGroup root = (ViewGroup) inflater.inflate(R.layout.activity_widget_setup, container, false);

            View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

            CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            inflatedView.setLayoutParams(lp);

            root.addView(inflatedView);

            // Set fragment view
            wm = WeatherManager.getInstance();

            setHasOptionsMenu(true);

            appBarLayout = root.findViewById(R.id.app_bar);
            mToolbar = root.findViewById(R.id.toolbar);
            collapsingToolbar = root.findViewById(R.id.collapsing_toolbar);
            mSearchFragmentContainer = root.findViewById(R.id.search_fragment_container);

            mRootView = (View) appBarLayout.getParent();
            // Make full transparent statusBar
            updateWindowColors();

            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, new OnApplyWindowInsetsListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    appBarLayout.setPaddingRelative(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(), 0);
                    return insets.consumeSystemWindowInsets();
                }
            });

            // Disable drag on AppBarLayout
            CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
            AppBarLayout.Behavior appBarBehavior = new AppBarLayout.Behavior();
            appBarBehavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                @Override
                public boolean canDrag(@NonNull final AppBarLayout appBarLayout) {
                    return false;
                }
            });
            appBarLayoutParams.setBehavior(appBarBehavior);

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

            locationPref = (ListAdapterPreference) findPreference(KEY_LOCATION);
            List<ComboBoxItem> comboList = new ArrayList<>();
            comboList.add(new ComboBoxItem(getString(R.string.pref_item_gpslocation), KEY_GPS));
            comboList.add(new ComboBoxItem(getString(R.string.label_btn_add_location), KEY_SEARCH));
            List<LocationData> favs = Settings.getFavorites();
            favorites = new ArrayList<>(favs);
            for (LocationData location : favorites) {
                comboList.add(comboList.size() - 1, new ComboBoxItem(location.getName(), location.getQuery()));
            }
            if (comboList.size() > MAX_LOCATIONS)
                comboList.remove(comboList.size() - 1);

            locationPref.getAdapter().addAll(comboList);
            locationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ctsCancel();

                    if (newValue instanceof ComboBoxItem) {
                        ComboBoxItem item = (ComboBoxItem) newValue;
                        if (KEY_SEARCH.equals(item.getValue())) {
                            // Setup search UI
                            prepareSearchUI();
                            query_vm = null;
                            return false;
                        } else {
                            selectedItem = item;
                        }
                    } else {
                        selectedItem = null;
                    }

                    query_vm = null;
                    return true;
                }
            });

            if (getArguments() != null
                    && !StringUtils.isNullOrWhitespace(getArguments().getString(WeatherWidgetService.EXTRA_LOCATIONQUERY))) {
                String locName = getArguments().getString(WeatherWidgetService.EXTRA_LOCATIONNAME);
                String locQuery = getArguments().getString(WeatherWidgetService.EXTRA_LOCATIONQUERY);

                if (locName != null) {
                    locationPref.setValue(new ComboBoxItem(locName, locQuery));
                } else {
                    locationPref.setValueIndex(0);
                }
            } else {
                locationPref.setValueIndex(0);
            }

            // Setup interval spinner
            refreshPref = (ListPreference) findPreference(KEY_REFRESHINTERVAL);
            refreshPref.setValue(Integer.toString(Settings.getRefreshInterval()));

            // Setup widget background spinner
            bgColorPref = (ListPreference) findPreference(KEY_BGCOLOR);
            bgColorPref.setValueIndex(WidgetUtils.getWidgetBackground(mAppWidgetId).getValue());

            // Get SearchUI state
            if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_SEARCHUI, false)) {
                inSearchUI = true;

                // Restart SearchUI
                prepareSearchUI();
            }
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            final String TAG = "ListAdapterPreferenceDialogFragment";

            // check if dialog is already showing
            if (getFragmentManager().findFragmentByTag(TAG) != null)
                return;

            if (preference instanceof ListAdapterPreference && KEY_LOCATION.equals(preference.getKey())) {
                final DialogFragment f = ListAdapterPreferenceDialogFragment.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                f.show(getFragmentManager(), TAG);
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
            if (inSearchUI && query_vm == null && selectedItem != null)
                locationPref.setValue(selectedItem);

            super.onSaveInstanceState(outState);
        }

        private void prepareSearchUI() {
            // Unset scroll flag
            AppBarLayout.LayoutParams toolbarParams = (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
            toolbarParams.setScrollFlags(toolbarParams.getScrollFlags() & ~AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);

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

            // Set scroll flag
            AppBarLayout.LayoutParams toolbarParams = (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
            toolbarParams.setScrollFlags(toolbarParams.getScrollFlags() | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
            Configuration config = mActivity.getResources().getConfiguration();
            appBarLayout.setExpanded(config.orientation == Configuration.ORIENTATION_PORTRAIT || ActivityUtils.isLargeTablet(mActivity), false);

            hideInputMethod(mActivity == null ? null : mActivity.getCurrentFocus());
            inSearchUI = false;

            // Reset to last selected item
            if (query_vm == null && selectedItem != null)
                locationPref.setValue(selectedItem);
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
                            query_vm = new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
                                @Override
                                public LocationQueryViewModel call() throws Exception {
                                    return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                                }
                            });
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
                            locationPref.setValue(new ComboBoxItem(loc.getName(), loc.getQuery()));
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
                        final int idx = locationPref.getAdapter().getCount() - 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayAdapter<ComboBoxItem> locAdapter = locationPref.getAdapter();

                                locAdapter.insert(item, idx);
                                locationPref.setValueIndex(idx);

                                if (locAdapter.getCount() > MAX_LOCATIONS) {
                                    locAdapter.remove(locAdapter.getItem(locAdapter.getCount() - 1));
                                }
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
            if (Settings.getUserThemeMode() == DarkMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
                appBarLayout.setBackgroundColor(Colors.BLACK);
                collapsingToolbar.setStatusBarScrimColor(Colors.BLACK);
                ActivityUtils.setTransparentWindow(mActivity.getWindow(),
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? -1 /* Opaque (Default) */ : Colors.TRANSPARENT, /* StatusBar */
                        Colors.TRANSPARENT /* NavBar */);
            } else {
                bg_color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
                int colorPrimary = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
                appBarLayout.setBackgroundColor(colorPrimary);
                collapsingToolbar.setStatusBarScrimColor(colorPrimary);
                ActivityUtils.setTransparentWindow(mActivity.getWindow(), bg_color,
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? -1 /* Opaque (Default) */ : Colors.TRANSPARENT, /* StatusBar */
                        config.orientation == Configuration.ORIENTATION_PORTRAIT ? Colors.TRANSPARENT : colorPrimary /* NavBar */);
            }
            mRootView.setBackgroundColor(bg_color);
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            super.onConfigurationChanged(newConfig);

            appBarLayout.setExpanded(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || ActivityUtils.isLargeTablet(mActivity), true);
            updateWindowColors();
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
                            ComboBoxItem item = new ComboBoxItem(locData.getName(), locData.getQuery());
                            int idx = locationPref.getAdapter().getCount() - 1;
                            locationPref.getAdapter().insert(item, idx);
                            locationPref.setValueIndex(idx);
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
                ComboBoxItem locationItem = locationPref.getValue();
                LocationData locData = null;

                // Widget ID exists in prefs
                if (WidgetUtils.exists(mAppWidgetId)) {
                    locData = WidgetUtils.getLocationData(mAppWidgetId);

                    // Handle location changes
                    if (KEY_GPS.equals(locationItem.getValue())) {
                        // Changing location to GPS
                        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                            return;
                        }

                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check if last location exists
                        if (lastGPSLocData == null && !updateLocation()) {
                            Toast.makeText(mActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Settings.setFollowGPS(true);

                        // Reset data for widget
                        WidgetUtils.deleteWidget(mAppWidgetId);
                        WidgetUtils.saveLocationData(mAppWidgetId, null);
                        WidgetUtils.addWidgetId(KEY_GPS, mAppWidgetId);
                    } else {
                        // Changing location to whatever
                        if (locData == null || !locationItem.getValue().equals(locData.getQuery())) {
                            // Get location data
                            ComboBoxItem item = locationPref.getValue();
                            boolean exists = false;
                            for (LocationData loc : favorites) {
                                if (loc.getQuery().equals(item.getValue())) {
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
                    switch (locationItem.getValue()) {
                        case KEY_GPS:
                            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSION_LOCATION_REQUEST_CODE);
                                return;
                            }

                            LocationData lastGPSLocData = Settings.getLastGPSLocData();

                            // Check if last location exists
                            if (lastGPSLocData == null && !updateLocation()) {
                                Toast.makeText(mActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Settings.setFollowGPS(true);

                            // Save locdata for widget
                            WidgetUtils.deleteWidget(mAppWidgetId);
                            WidgetUtils.saveLocationData(mAppWidgetId, null);
                            WidgetUtils.addWidgetId(KEY_GPS, mAppWidgetId);
                            break;
                        default:
                            // Get location data
                            ComboBoxItem item = locationPref.getValue();
                            boolean exists = false;
                            for (LocationData loc : favorites) {
                                if (loc.getQuery().equals(item.getValue())) {
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
                            break;
                    }
                }

                // Save widget preferences
                WidgetUtils.setWidgetBackground(mAppWidgetId, Integer.parseInt(bgColorPref.getValue()));

                // Trigger widget service to update widget
                WeatherWidgetService.enqueueWork(mActivity,
                        new Intent(mActivity, WeatherWidgetService.class)
                                .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS, new int[]{mAppWidgetId}));

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
                        Toast.makeText(mActivity, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
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
