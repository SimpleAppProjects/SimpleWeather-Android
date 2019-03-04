package com.thewizrd.simpleweather.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.LocationSearchFragment;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.SetupActivity;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class WeatherWidgetConfigActivity extends AppCompatActivity {
    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Intent resultValue;

    // Location Search
    private Collection<LocationData> favorites;
    private LocationQueryViewModel query_vm = null;
    private LocationQueryViewModel gpsQuery_vm = null;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private CancellationTokenSource cts;

    // Weather
    private WeatherManager wm;

    // Views
    private NestedScrollView scrollView;
    private Spinner locSpinner;
    private ArrayAdapter<ComboBoxItem> locAdapter;
    private TextView locSummary;
    private Spinner refreshSpinner;
    private Spinner bgSpinner;
    private TextView bgSummary;
    private AppBarLayout appBarLayout;
    private Toolbar mToolbar;
    private View searchBarContainer;
    private View mSearchFragmentContainer;
    private LocationSearchFragment mSearchFragment;
    private EditText searchView;
    private TextView clearButtonView;
    private TextView backButtonView;
    private ProgressBar progressBar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ComboBoxItem selectedItem;
    private boolean inSearchUI;

    private static final int ANIMATION_DURATION = 240;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int SETUP_REQUEST_CODE = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Find the widget id from the intent.
        if (getIntent() != null && getIntent().getExtras() != null) {
            mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If they gave us an intent without the widget id, just bail.
            finish();
        }

        setContentView(R.layout.activity_widget_setup);

        // Make full transparent statusBar
        ActivityUtils.setTransparentWindow(getWindow(),
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? -1 /* Opaque (Default) */ : Colors.TRANSPARENT, /* StatusBar */
                Colors.TRANSPARENT /* NavBar */);

        wm = WeatherManager.getInstance();

        appBarLayout = findViewById(R.id.app_bar);
        mToolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        mSearchFragmentContainer = findViewById(R.id.search_fragment_container);
        scrollView = findViewById(R.id.scrollView);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int padding = getResources().getDimensionPixelSize(R.dimen.toolbar_horizontal_inset_padding);
        mToolbar.setContentInsetsRelative(padding, padding);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);

        if (searchBarContainer == null) {
            searchBarContainer = getLayoutInflater().inflate(R.layout.search_action_bar, mToolbar, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                searchBarContainer.setElevation(getResources().getDimension(R.dimen.appbar_elevation) + 2);
            }
            mToolbar.addView(searchBarContainer, 0);
        }

        mSearchFragmentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi(false);
            }
        });

        // Setup location spinner
        locSpinner = findViewById(R.id.location_pref_spinner);
        locSummary = findViewById(R.id.location_pref_summary);

        List<ComboBoxItem> comboList = new ArrayList<>();
        comboList.add(new ComboBoxItem(getString(R.string.pref_item_gpslocation), "GPS"));
        comboList.add(new ComboBoxItem(getString(R.string.label_btn_add_location), "Search"));
        List<LocationData> favs = Settings.getFavorites();
        favorites = new ArrayList<>(favs);
        for (LocationData location : favorites) {
            comboList.add(comboList.size() - 1, new ComboBoxItem(location.getName(), location.getQuery()));
        }

        locAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                comboList);
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locAdapter.setNotifyOnChange(true);
        locSpinner.setAdapter(locAdapter);

        findViewById(R.id.location_pref).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locSpinner.performClick();
            }
        });
        locSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ctsCancel();

                if (locSpinner.getSelectedItem() instanceof ComboBoxItem) {
                    ComboBoxItem item = (ComboBoxItem) locSpinner.getSelectedItem();
                    locSummary.setText(item.getDisplay());

                    if ("Search".equals(item.getValue()))
                        // Setup search UI
                        prepareSearchUI();
                    else
                        selectedItem = item;
                } else
                    selectedItem = null;
                query_vm = null;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (getIntent() != null
                && !StringUtils.isNullOrWhitespace(getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY))) {
            String locName = getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONNAME);
            String locQuery = getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY);

            if (locName != null)
                locSpinner.setSelection(locAdapter.getPosition(new ComboBoxItem(locName, locQuery)));
            else
                locSpinner.setSelection(0);
        } else {
            locSpinner.setSelection(0);
        }

        // Setup interval spinner
        refreshSpinner = findViewById(R.id.interval_pref_spinner);
        final TextView refreshSummary = findViewById(R.id.interval_pref_summary);
        List<ComboBoxItem> refreshList = new ArrayList<>();
        String[] refreshEntries = getResources().getStringArray(R.array.refreshinterval_entries);
        String[] refreshValues = getResources().getStringArray(R.array.refreshinterval_values);
        for (int i = 0; i < refreshEntries.length; i++) {
            refreshList.add(new ComboBoxItem(refreshEntries[i], refreshValues[i]));
        }
        ArrayAdapter<ComboBoxItem> refreshAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                refreshList);
        refreshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        refreshSpinner.setAdapter(refreshAdapter);
        findViewById(R.id.interval_pref).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshSpinner.performClick();
            }
        });
        refreshSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (refreshSpinner.getSelectedItem() instanceof ComboBoxItem) {
                    ComboBoxItem item = (ComboBoxItem) refreshSpinner.getSelectedItem();
                    refreshSummary.setText(item.getDisplay());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int index = 0;
        for (ComboBoxItem item : refreshList) {
            if (Integer.toString(Settings.getRefreshInterval()).equals(item.getValue())) {
                index = refreshList.indexOf(item);
                break;
            }
        }
        refreshSpinner.setSelection(index);

        // Setup widget background spinner
        bgSpinner = findViewById(R.id.bgcolor_pref_spinner);
        bgSummary = findViewById(R.id.bgcolor_pref_summary);
        List<ComboBoxItem> bgList = new ArrayList<>();
        String[] bgEntries = getResources().getStringArray(R.array.bgcolor_entries);
        String[] bgValues = getResources().getStringArray(R.array.bgcolor_values);
        for (int i = 0; i < bgEntries.length; i++) {
            bgList.add(new ComboBoxItem(bgEntries[i], bgValues[i]));
        }
        ArrayAdapter<ComboBoxItem> bgAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                bgList);
        bgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bgSpinner.setAdapter(bgAdapter);
        findViewById(R.id.bgcolor_pref).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgSpinner.performClick();
            }
        });
        bgSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bgSpinner.getSelectedItem() instanceof ComboBoxItem) {
                    ComboBoxItem item = (ComboBoxItem) bgSpinner.getSelectedItem();
                    bgSummary.setText(item.getDisplay());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        bgSpinner.setSelection(WidgetUtils.getWidgetBackground(mAppWidgetId).getValue());

        cts = new CancellationTokenSource();

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(this);
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null)
                        mLocation = null;
                    else
                        mLocation = locationResult.getLastLocation();

                    if (mLocation == null) {
                        Toast.makeText(WeatherWidgetConfigActivity.this, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                    } else {
                        fetchGeoLocation();
                    }

                    new AsyncTask<Void>().await(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            return Tasks.await(mFusedLocationClient.removeLocationUpdates(mLocCallback));
                        }
                    });
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    new AsyncTask<Void>().await(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            return Tasks.await(mFusedLocationClient.flushLocations());
                        }
                    });

                    if (!locationAvailability.isLocationAvailable()) {
                        Toast.makeText(WeatherWidgetConfigActivity.this, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                    }
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mLocation = location;
                    fetchGeoLocation();
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

        if (!Settings.isWeatherLoaded()) {
            Toast.makeText(this, R.string.prompt_setup_app_first, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, SetupActivity.class)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            startActivityForResult(intent, SETUP_REQUEST_CODE);
        }
    }

    @Override
    protected void onPause() {
        if (cts != null) cts.cancel();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (cts != null) cts.cancel();
        super.onDestroy();
    }

    private void prepareSearchUI() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mToolbar.getMenu().clear();
        mToolbar.setContentInsetsRelative(0, 0);
        ViewCompat.setNestedScrollingEnabled(scrollView, false);
        // Unset scroll flag
        AppBarLayout.LayoutParams toolbarParams = (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
        toolbarParams.setScrollFlags(toolbarParams.getScrollFlags() & ~AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        toolbarParams.height = AppBarLayout.LayoutParams.WRAP_CONTENT;

        enterSearchUi();
        enterSearchUiTransition(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ViewCompat.setElevation(appBarLayout, getResources().getDimension(R.dimen.appbar_elevation) + 1);
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
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
        setupSearchUi();
    }

    private void enterSearchUiTransition(Animation.AnimationListener enterAnimationListener) {
        // FragmentContainer fade/translation animation
        AnimationSet fragmentAniSet = new AnimationSet(true);
        fragmentAniSet.setInterpolator(new DecelerateInterpolator());
        AlphaAnimation fragFadeAni = new AlphaAnimation(0.0f, 1.0f);
        TranslateAnimation fragmentAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, locSpinner.getY(),
                Animation.ABSOLUTE, 0);
        fragmentAniSet.setDuration(ANIMATION_DURATION);
        fragmentAniSet.setFillEnabled(false);
        fragmentAniSet.addAnimation(fragFadeAni);
        fragmentAniSet.addAnimation(fragmentAnimation);
        fragmentAniSet.setAnimationListener(enterAnimationListener);

        // SearchActionBarContainer fade/translation animation
        AnimationSet searchBarAniSet = new AnimationSet(true);
        searchBarAniSet.setInterpolator(new DecelerateInterpolator());
        AlphaAnimation searchBarFadeAni = new AlphaAnimation(0.0f, 1.0f);
        TranslateAnimation searchBarAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, mToolbar.getHeight(),
                Animation.ABSOLUTE, 0);
        searchBarAniSet.setDuration((long) (ANIMATION_DURATION * 1.5));
        searchBarAniSet.setFillEnabled(false);
        searchBarAniSet.addAnimation(searchBarFadeAni);
        searchBarAniSet.addAnimation(searchBarAnimation);

        mSearchFragmentContainer.startAnimation(fragmentAniSet);
        searchBarContainer.setVisibility(View.VISIBLE);
        searchBarContainer.startAnimation(searchBarAniSet);
    }

    private void setupSearchUi() {
        if (searchView == null) {
            prepareSearchView();
        }
        searchView.requestFocus();
    }

    private void showLoading(boolean show) {
        if (mSearchFragment == null)
            return;

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show || (!show && StringUtils.isNullOrEmpty(searchView.getText().toString())))
            clearButtonView.setVisibility(View.GONE);
        else
            clearButtonView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) fragment;
            setupSearchUi();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (inSearchUI)
            exitSearchUi(true);

        super.onSaveInstanceState(outState);
    }

    private void addSearchFragment() {
        if (mSearchFragment != null) {
            return;
        }
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final LocationSearchFragment searchFragment = new LocationSearchFragment();
        searchFragment.setRecyclerOnClickListener(new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(View view, int position) {
                if (mSearchFragment == null)
                    return;

                LocationQueryAdapter adapter = searchFragment.getAdapter();
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

                showLoading(true);

                if (mSearchFragment.ctsCancelRequested()) {
                    showLoading(false);
                    query_vm = null;
                    return;
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
                    showLoading(false);
                    exitSearchUi(false);

                    // Set selection
                    query_vm = null;
                    locSpinner.setSelection(
                            locAdapter.getPosition(new ComboBoxItem(loc.getName(), loc.getQuery())));
                    return;
                }

                if (mSearchFragment.ctsCancelRequested()) {
                    showLoading(false);
                    query_vm = null;
                    return;
                }

                // We got our data so disable controls just in case
                adapter.getDataset().clear();
                adapter.notifyDataSetChanged();

                if (mSearchFragment != null && mSearchFragment.getView() != null &&
                        mSearchFragment.getView().findViewById(R.id.recycler_view) instanceof RecyclerView) {
                    RecyclerView recyclerView = mSearchFragment.getView().findViewById(R.id.recycler_view);
                    recyclerView.setEnabled(false);
                }

                // Save data
                WeatherWidgetConfigActivity.this.query_vm = query_vm;
                ComboBoxItem item = new ComboBoxItem(query_vm.getLocationName(), query_vm.getLocationQuery());
                int idx = locAdapter.getCount() - 1;
                locAdapter.insert(item, idx);
                locSpinner.setSelection(idx);
                locSummary.setText(item.getDisplay());

                // Hide dialog
                showLoading(false);
                exitSearchUi(false);
            }
        });
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        searchView = mToolbar.findViewById(R.id.search_view);
        clearButtonView = mToolbar.findViewById(R.id.search_close_button);
        backButtonView = mToolbar.findViewById(R.id.search_back_button);
        progressBar = mToolbar.findViewById(R.id.search_progressBar);
        backButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi(false);
            }
        });
        clearButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 1000; // milliseconds

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing to do here
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                // user is typing: reset already started timer (if existing)
                if (timer != null) {
                    timer.cancel();
                }
            }

            @Override
            public void afterTextChanged(final Editable e) {
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String newText = e.toString();

                                        if (mSearchFragment != null) {
                                            clearButtonView.setVisibility(StringUtils.isNullOrEmpty(newText) ? View.GONE : View.VISIBLE);
                                            mSearchFragment.fetchLocations(newText);
                                        }
                                    }
                                });
                            }
                        }, DELAY
                );
            }
        });
        clearButtonView.setVisibility(View.GONE);
        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (mSearchFragment != null) {
                        mSearchFragment.fetchLocations(v.getText().toString());
                        hideInputMethod(v);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void removeSearchFragment() {
        mSearchFragment.setUserVisibleHint(false);
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.remove(mSearchFragment);
        mSearchFragment = null;
        transaction.commitAllowingStateLoss();
    }

    private void exitSearchUi(boolean skipAnimation) {
        searchView.setText("");

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int padding = getResources().getDimensionPixelSize(R.dimen.toolbar_horizontal_inset_padding);
        mToolbar.setContentInsetsRelative(padding, padding);
        invalidateOptionsMenu();
        ViewCompat.setNestedScrollingEnabled(scrollView, true);
        // Set scroll flag
        AppBarLayout.LayoutParams toolbarParams = (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
        toolbarParams.height = getResources().getDimensionPixelSize(R.dimen.collapsingtoolbar_maxHeight);
        toolbarParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);

        hideInputMethod(getCurrentFocus());
        searchView.clearFocus();
        if (searchBarContainer != null) searchBarContainer.setVisibility(View.GONE);
        ViewCompat.setElevation(appBarLayout, 0);
        inSearchUI = false;

        // Reset to last selected item
        if (query_vm == null && selectedItem != null)
            locSpinner.setSelection(locAdapter.getPosition(selectedItem));
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
                Animation.ABSOLUTE, locSpinner.getY());
        fragmentAniSet.setDuration(ANIMATION_DURATION);
        fragmentAniSet.setFillEnabled(false);
        fragmentAniSet.addAnimation(fragFadeAni);
        fragmentAniSet.addAnimation(fragmentAnimation);
        fragmentAniSet.setAnimationListener(exitAnimationListener);

        mSearchFragmentContainer.startAnimation(fragmentAniSet);
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                        int idx = locAdapter.getCount() - 1;
                        locAdapter.insert(item, idx);
                        locSpinner.setSelection(idx);
                    } else {
                        // GPS; set to first selection
                        locSpinner.setSelection(0);
                    }
                }
            } else {
                // Setup was cancelled. Cancel widget setup
                setResult(RESULT_CANCELED, resultValue);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (inSearchUI) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi(false);
        } else {
            setResult(RESULT_CANCELED, resultValue);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_widgetsetup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (inSearchUI) {
                    // We should let the user go back to usual screens with tabs.
                    exitSearchUi(false);
                } else {
                    setResult(RESULT_CANCELED, resultValue);
                    finish();
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
        if (refreshSpinner.getSelectedItem() instanceof ComboBoxItem) {
            ComboBoxItem refreshItem = (ComboBoxItem) refreshSpinner.getSelectedItem();

            try {
                int refreshValue = Integer.valueOf(refreshItem.getValue());
                Settings.setRefreshInterval(refreshValue);
            } catch (NumberFormatException e) {
                // DO nothing
            }
        }

        // Get location data
        if (locSpinner.getSelectedItem() instanceof ComboBoxItem) {
            ComboBoxItem locationItem = (ComboBoxItem) locSpinner.getSelectedItem();
            LocationData locData = null;

            // Widget ID exists in prefs
            if (WidgetUtils.exists(mAppWidgetId)) {
                locData = WidgetUtils.getLocationData(mAppWidgetId);

                // Handle location changes
                if ("GPS".equals(locationItem.getValue())) {
                    // Changing location to GPS
                    // Reset data for widget
                    WidgetUtils.deleteWidget(mAppWidgetId);
                    WidgetUtils.saveLocationData(mAppWidgetId, null);
                    WidgetUtils.addWidgetId("GPS", mAppWidgetId);
                } else {
                    // Changing location to whatever
                    if (locData == null || !locationItem.getValue().equals(locData.getQuery())) {
                        // Get location data
                        ComboBoxItem item = (ComboBoxItem) locSpinner.getSelectedItem();
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
                                setResult(RESULT_CANCELED, resultValue);
                                finish();
                                return;
                            }

                            // Add location to favs
                            Settings.addLocation(locData);
                        } else if (locData == null) {
                            setResult(RESULT_CANCELED, resultValue);
                            finish();
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
                    case "GPS":
                        Settings.setFollowGPS(true);

                        if (gpsQuery_vm == null || mLocation == null) {
                            fetchGeoLocation();
                        } else {
                            locData = new LocationData(gpsQuery_vm, mLocation);
                            Settings.saveLastGPSLocData(locData);

                            // Save locdata for widget
                            WidgetUtils.deleteWidget(mAppWidgetId);
                            WidgetUtils.saveLocationData(mAppWidgetId, null);
                            WidgetUtils.addWidgetId("GPS", mAppWidgetId);
                        }
                        break;
                    default:
                        // Get location data
                        ComboBoxItem item = (ComboBoxItem) locSpinner.getSelectedItem();
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
                                setResult(RESULT_CANCELED, resultValue);
                                finish();
                                return;
                            }

                            // Add location to favs
                            Settings.addLocation(locData);
                        } else if (locData == null) {
                            setResult(RESULT_CANCELED, resultValue);
                            finish();
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
            WidgetUtils.setWidgetBackground(mAppWidgetId, bgSpinner.getSelectedItemPosition());

            // Trigger widget service to update widget
            WeatherWidgetService.enqueueWork(this,
                    new Intent(this, WeatherWidgetService.class)
                            .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                            .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS, new int[]{mAppWidgetId}));

            // Create return intent
            setResult(RESULT_OK, resultValue);
            finish();
        } else {
            setResult(RESULT_CANCELED, resultValue);
            finish();
        }
    }

    private void ctsCancel() {
        if (cts != null) cts.cancel();
        cts = new CancellationTokenSource();
    }

    private void fetchGeoLocation() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (cts.getToken().isCancellationRequested()) {
                    return null;
                }

                if (mLocation != null) {
                    LocationQueryViewModel view = null;

                    // Cancel other tasks
                    ctsCancel();
                    CancellationToken ctsToken = cts.getToken();

                    if (ctsToken.isCancellationRequested()) {
                        return null;
                    }

                    // Get geo location
                    view = wm.getLocation(mLocation);

                    if (StringUtils.isNullOrWhitespace(view.getLocationQuery()))
                        view = new LocationQueryViewModel();

                    if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                        // Stop since there is no valid query
                        return null;
                    }

                    if (ctsToken.isCancellationRequested()) {
                        return null;
                    }

                    // Set gps location data
                    gpsQuery_vm = view;

                    // We got our location data, so setup the widget
                    prepareWidget();
                } else {
                    updateLocation();
                }

                return null;
            }
        });
    }

    private void updateLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION_REQUEST_CODE);
            return;
        }

        Location location = null;

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            location = new AsyncTask<Location>().await(new Callable<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public Location call() throws Exception {
                    return Tasks.await(mFusedLocationClient.getLastLocation());
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
            } else {
                mLocation = location;
                fetchGeoLocation();
            }
        } else {
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = false;
            boolean isNetEnabled = false;
            if (locMan != null) {
                isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }

            if (isGPSEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location == null)
                    location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null)
                    locMan.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocListnr, Looper.getMainLooper());
                else {
                    mLocation = location;
                    fetchGeoLocation();
                }
            } else if (isNetEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location == null)
                    locMan.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocListnr, Looper.getMainLooper());
                else {
                    mLocation = location;
                    fetchGeoLocation();
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherWidgetConfigActivity.this, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
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
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            fetchGeoLocation();
                        }
                    });
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                break;
        }
    }
}
