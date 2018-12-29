package com.thewizrd.simpleweather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.CancellationToken;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ListChangedAction;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherErrorListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherLoadedListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter;
import com.thewizrd.simpleweather.controls.LocationPanel;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperCallback;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class LocationsFragment extends Fragment
        implements WeatherLoadedListenerInterface, WeatherErrorListenerInterface,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private boolean mLoaded = false;
    private boolean mEditMode = false;
    private boolean mDataChanged = false;
    private boolean mHomeChanged = false;
    private boolean[] mErrorCounter;

    AppCompatActivity appCompatActivity;

    // Views
    private View mMainView;
    private RecyclerView mRecyclerView;
    private LocationPanelAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelperCallback mITHCallback;
    private FloatingActionButton addLocationsButton;

    // Search
    private LocationSearchFragment mSearchFragment;
    private ActionMode mActionMode;
    private View searchViewLayout;
    private EditText searchView;
    private TextView clearButtonView;
    private ProgressBar progressBar;
    private boolean inSearchUI;

    // GPS Location
    View gpsPanelLayout;
    LocationPanel gpsPanel;
    LocationPanelViewModel gpsPanelViewModel;
    private LocationListener mLocListnr;
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (searchViewLayout == null)
                searchViewLayout = appCompatActivity.getLayoutInflater().inflate(R.layout.search_action_bar, null);

            mode.setCustomView(searchViewLayout);
            enterSearchUi();
            // Hide FAB in actionmode
            if (addLocationsButton != null)
                addLocationsButton.setVisibility(View.GONE);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            exitSearchUi();
            if (addLocationsButton != null)
                addLocationsButton.setVisibility(View.VISIBLE);
            mActionMode = null;
        }
    };

    // OptionsMenu
    private Menu optionsMenu;

    private WeatherManager wm;

    public LocationsFragment() {
        // Required empty public constructor
        wm = WeatherManager.getInstance();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        appCompatActivity = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        appCompatActivity = null;
    }

    public void onWeatherLoaded(final LocationData location, final Weather weather) {
        if (appCompatActivity != null) {
            appCompatActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (weather != null && weather.isValid()) {
                        if (Settings.useFollowGPS() && location.getLocationType() == LocationType.GPS) {
                            if (gpsPanelViewModel != null) {
                                gpsPanelViewModel.setWeather(weather);
                                gpsPanel.setWeatherBackground(gpsPanelViewModel);
                                gpsPanel.setWeather(gpsPanelViewModel);
                            }
                        } else {
                            // Update panel weather
                            LocationPanelViewModel panel = null;
                            for (LocationPanelViewModel panelVM : mAdapter.getDataset()) {
                                if (panelVM.getLocationData().getQuery().equals(location.getQuery())) {
                                    panel = panelVM;
                                    break;
                                }
                            }
                            // Just in case
                            if (panel == null) {
                                for (LocationPanelViewModel panelVM : mAdapter.getDataset()) {
                                    if (panelVM.getLocationData().getName().equals(location.getName()) &&
                                            panelVM.getLocationData().getLatitude() == location.getLatitude() &&
                                            panelVM.getLocationData().getLongitude() == location.getLongitude() &&
                                            panelVM.getLocationData().getTzLong().equals(location.getTzLong())) {
                                        panel = panelVM;
                                        break;
                                    }
                                }
                            }
                            if (panel != null) {
                                panel.setWeather(weather);
                                mAdapter.notifyItemChanged(mAdapter.getDataset().indexOf(panel));
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onWeatherError(WeatherException wEx) {
        switch (wEx.getErrorStatus()) {
            case NETWORKERROR:
            case NOWEATHER:
                // Show error message and prompt to refresh
                // Only warn once
                if (!mErrorCounter[wEx.getErrorStatus().getValue()]) {
                    Snackbar snackbar = Snackbar.make(mMainView, wEx.getMessage(), Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.action_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refreshLocations();
                        }
                    });
                    snackbar.show();
                    mErrorCounter[wEx.getErrorStatus().getValue()] = true;
                }
                break;
            default:
                // Show error message
                // Only warn once
                if (!mErrorCounter[wEx.getErrorStatus().getValue()]) {
                    Snackbar.make(mMainView, wEx.getMessage(), Snackbar.LENGTH_LONG).show();
                    mErrorCounter[wEx.getErrorStatus().getValue()] = true;
                }
                break;
        }
    }

    // For LocationPanels
    private View.OnClickListener onPanelClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view != null && view.isEnabled() && view.getTag() instanceof LocationData) {
                LocationData locData = (LocationData) view.getTag();

                if (locData.equals(Settings.getHomeData())) {
                    // Pop all since we're going home
                    appCompatActivity.getSupportFragmentManager()
                            .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    // Navigate to WeatherNowFragment
                    Fragment fragment = WeatherNowFragment.newInstance(locData);
                    appCompatActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, fragment, null)
                            .hide(LocationsFragment.this)
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    };

    private RecyclerOnClickListenerInterface onRecyclerClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(View view, int position) {
            onPanelClickListener.onClick(view);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        mLoaded = true;

        // Get ActionMode state
        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchUI", false)) {
            inSearchUI = true;

            // Restart ActionMode
            mActionMode = appCompatActivity.startSupportActionMode(mActionModeCallback);
        }

        mErrorCounter = new boolean[WeatherUtils.ErrorStatus.values().length];

        mLocListnr = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_locations, container, false);
        mMainView = view;
        view.findViewById(R.id.search_fragment_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi();
            }
        });
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && mEditMode) {
                    toggleEditMode();
                    return true;
                } else {
                    return false;
                }
            }
        });

        // Setup ActionBar
        setHasOptionsMenu(true);

        mRecyclerView = view.findViewById(R.id.locations_container);

        addLocationsButton = view.findViewById(R.id.locations_add);
        addLocationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActionMode = appCompatActivity.startSupportActionMode(mActionModeCallback);
            }
        });

        gpsPanelLayout = view.findViewById(R.id.gps_follow_layout);
        gpsPanel = view.findViewById(R.id.gps_panel);
        gpsPanel.setOnClickListener(onPanelClickListener);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(appCompatActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationPanelAdapter(Glide.with(this.getContext()), new ArrayList<LocationPanelViewModel>());
        mAdapter.setOnClickListener(onRecyclerClickListener);
        mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
        mAdapter.setOnListChangedCallback(onListChangedListener);
        mRecyclerView.setAdapter(mAdapter);
        mITHCallback = new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mITHCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        // Turn off by default
        mITHCallback.setLongPressDragEnabled(false);
        mITHCallback.setItemViewSwipeEnabled(false);

        mLoaded = true;

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        optionsMenu = menu;
        menu.clear();
        inflater.inflate(R.menu.locations, menu);

        boolean onlyHomeIsLeft = (mAdapter.getItemCount() == 1);
        MenuItem editMenuBtn = optionsMenu.findItem(R.id.action_editmode);
        if (editMenuBtn != null)
            editMenuBtn.setVisible(!onlyHomeIsLeft);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent AppCompatActivity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_editmode) {
            toggleEditMode();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void resume() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Update view on resume
                // ex. If temperature unit changed
                if (appCompatActivity != null) {
                    appCompatActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ColorDrawable colorDrawable = new ColorDrawable(
                                    ContextCompat.getColor(appCompatActivity, R.color.colorPrimary));
                            appCompatActivity.getSupportActionBar().setBackgroundDrawable(colorDrawable);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                appCompatActivity.getWindow().setStatusBarColor(
                                        ContextCompat.getColor(appCompatActivity, R.color.colorPrimaryDark));
                            }

                            if (Settings.useFollowGPS()) {
                                gpsPanelLayout.setVisibility(View.VISIBLE);
                            } else {
                                gpsPanelViewModel = null;
                                gpsPanelLayout.setVisibility(View.GONE);
                            }
                        }
                    });

                    if (mAdapter.getItemCount() == 0 || Settings.useFollowGPS() && gpsPanelViewModel == null) {
                        // New instance; Get locations and load up weather data
                        loadLocations();
                    } else if (!mLoaded) {
                        // Refresh view
                        refreshLocations();
                        mLoaded = true;
                    }
                }
                return null;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (this.isHidden())
            return;
        else
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });

        // Title
        if (appCompatActivity != null)
            appCompatActivity.getSupportActionBar().setTitle(R.string.label_nav_locations);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLoaded = false;

        // Reset error counter
        Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            resume();
        } else if (hidden) {
            mLoaded = false;
            // Reset error counter
            Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save ActionMode state
        outState.putBoolean("SearchUI", inSearchUI);

        if (inSearchUI)
            exitSearchUi();

        super.onSaveInstanceState(outState);
    }

    private void loadLocations() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (appCompatActivity != null) {
                    // Load up saved locations
                    List<LocationData> locations = Settings.getFavorites();
                    appCompatActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.removeAll();
                        }
                    });

                    // Setup saved favorite locations
                    loadGPSPanel();
                    for (LocationData location : locations) {
                        final LocationPanelViewModel panel = new LocationPanelViewModel();
                        panel.setLocationData(location);
                        appCompatActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.add(panel);
                            }
                        });
                    }

                    for (final LocationData location : locations) {
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                WeatherDataLoader wLoader = new WeatherDataLoader(location, LocationsFragment.this, LocationsFragment.this);
                                wLoader.loadWeatherData(false);
                            }
                        });
                    }
                }
                return null;
            }
        });
    }

    private void loadGPSPanel() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Setup gps panel
                if (appCompatActivity != null && Settings.useFollowGPS()) {
                    appCompatActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gpsPanelLayout.setVisibility(View.VISIBLE);
                        }
                    });
                    LocationData locData = Settings.getLastGPSLocData();

                    if (locData == null || locData.getQuery() == null) {
                        locData = updateLocation();
                    }

                    if (locData != null && locData.getQuery() != null) {
                        gpsPanelViewModel = new LocationPanelViewModel();
                        gpsPanelViewModel.setLocationData(locData);

                        final LocationData finalLocData = locData;
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                WeatherDataLoader wLoader = new WeatherDataLoader(finalLocData, LocationsFragment.this, LocationsFragment.this);
                                wLoader.loadWeatherData(false);
                            }
                        });
                    }
                }
                return null;
            }
        });
    }

    private void refreshLocations() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Reload all panels if needed
                List<LocationData> locations = Settings.getLocationData();
                LocationData homeData = Settings.getLastGPSLocData();
                boolean reload = (locations.size() != mAdapter.getItemCount() || Settings.useFollowGPS() && gpsPanelViewModel == null);

                // Reload if weather source differs
                if ((gpsPanelViewModel != null && !Settings.getAPI().equals(gpsPanelViewModel.getWeatherSource())) ||
                        (mAdapter.getItemCount() >= 1 && !Settings.getAPI().equals(mAdapter.getDataset().get(0).getWeatherSource())))
                    reload = true;

                if (!reload && (gpsPanelViewModel != null && !homeData.getQuery().equals(gpsPanelViewModel.getLocationData().getQuery())))
                    reload = true;

                if (reload) {
                    appCompatActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.removeAll();
                        }
                    });
                    loadLocations();
                } else {
                    List<LocationPanelViewModel> dataset = mAdapter.getDataset();
                    if (gpsPanelViewModel != null)
                        dataset.add(gpsPanelViewModel);

                    for (final LocationPanelViewModel view : dataset) {
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                WeatherDataLoader wLoader = new WeatherDataLoader(view.getLocationData(), LocationsFragment.this, LocationsFragment.this);
                                wLoader.loadWeatherData(false);
                            }
                        });
                    }
                }
                return null;
            }
        });
    }

    private LocationData updateLocation() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() throws Exception {
                LocationData locationData = null;

                if (Settings.useFollowGPS()) {
                    if (appCompatActivity != null && ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(appCompatActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
                        return null;
                    }

                    LocationManager locMan = (LocationManager) appCompatActivity.getSystemService(Context.LOCATION_SERVICE);
                    boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                    Location location = null;

                    if (isGPSEnabled || isNetEnabled) {
                        Criteria locCriteria = new Criteria();
                        locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                        locCriteria.setCostAllowed(false);
                        locCriteria.setPowerRequirement(Criteria.POWER_LOW);

                        String provider = locMan.getBestProvider(locCriteria, true);
                        location = locMan.getLastKnownLocation(provider);

                        if (location == null)
                            locMan.requestSingleUpdate(provider, mLocListnr, Looper.getMainLooper());
                        else {
                            LocationQueryViewModel view = null;

                            view = wm.getLocation(location);

                            if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                                view = new LocationQueryViewModel();

                            if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                                // Stop since there is no valid query
                                appCompatActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        gpsPanelViewModel = null;
                                        gpsPanelLayout.setVisibility(View.GONE);
                                    }
                                });
                                return null;
                            }

                            // Save location as last known
                            locationData = new LocationData(view, location);
                        }
                    } else {
                        appCompatActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(appCompatActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show();
                                gpsPanelViewModel = null;
                                gpsPanelLayout.setVisibility(View.GONE);
                            }
                        });
                    }
                }

                return locationData;
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
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            LocationData locData = updateLocation();
                            if (locData != null) {
                                Settings.saveLastGPSLocData(locData);
                                loadGPSPanel();

                                App.getInstance().getAppContext().startService(
                                        new Intent(App.getInstance().getAppContext(), WearableDataListenerService.class)
                                                .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                            } else {
                                if (appCompatActivity != null) {
                                    appCompatActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            gpsPanelViewModel = null;
                                            gpsPanelLayout.setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }
                        }
                    });
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Settings.setFollowGPS(false);
                    gpsPanelViewModel = null;
                    gpsPanelLayout.setVisibility(View.GONE);
                    Toast.makeText(appCompatActivity, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        if (childFragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) childFragment;

            if (inSearchUI)
                setupSearchUi();
        }
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
        setupSearchUi();
    }

    private void setupSearchUi() {
        if (searchView == null) {
            prepareSearchView();
        }
        searchView.requestFocus();
    }

    private void showLoading(final boolean show) {
        if (mSearchFragment == null)
            return;

        appCompatActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

                if (show || (!show && StringUtils.isNullOrEmpty(searchView.getText().toString())))
                    clearButtonView.setVisibility(View.GONE);
                else
                    clearButtonView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addSearchFragment() {
        if (mSearchFragment != null) {
            return;
        }
        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        final LocationSearchFragment searchFragment = new LocationSearchFragment();
        searchFragment.setRecyclerOnClickListener(new RecyclerOnClickListenerInterface() {
            @Override
            public void onClick(final View view, final int position) {
                if (mSearchFragment == null)
                    return;

                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        final LocationQueryAdapter adapter = searchFragment.getAdapter();
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
                        CancellationToken ctsToken = mSearchFragment.getCancellationTokenSource().getToken();

                        showLoading(true);

                        if (ctsToken.isCancellationRequested()) {
                            showLoading(false);
                            return;
                        }

                        // Check if location already exists
                        List<LocationData> locData = Settings.getLocationData();
                        boolean exists = false;
                        for (LocationData l : locData) {
                            if (l.getQuery().equals(query_vm.getLocationQuery())) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            appCompatActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showLoading(false);
                                    exitSearchUi();
                                }
                            });
                            return;
                        }

                        if (ctsToken.isCancellationRequested()) {
                            showLoading(false);
                            return;
                        }

                        LocationData location = new LocationData(query_vm);
                        if (!location.isValid()) {
                            appCompatActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(App.getInstance().getAppContext(), R.string.werror_noweather, Toast.LENGTH_SHORT).show();
                                    showLoading(false);
                                }
                            });
                            return;
                        }
                        Weather weather = Settings.getWeatherData(location.getQuery());
                        if (weather == null) {
                            try {
                                weather = wm.getWeather(location);
                            } catch (final WeatherException wEx) {
                                weather = null;
                                appCompatActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(App.getInstance().getAppContext(), wEx.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        if (weather == null) {
                            showLoading(false);
                            return;
                        }

                        // We got our data so disable controls just in case
                        appCompatActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.getDataset().clear();
                                mAdapter.notifyDataSetChanged();
                            }
                        });

                        if (mSearchFragment != null && mSearchFragment.getView() != null &&
                                mSearchFragment.getView().findViewById(R.id.recycler_view) instanceof RecyclerView) {
                            appCompatActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    RecyclerView recyclerView = mSearchFragment.getView().findViewById(R.id.recycler_view);
                                    recyclerView.setEnabled(false);
                                }
                            });
                        }

                        // Save data
                        Settings.addLocation(location);
                        if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                            Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                        Settings.saveWeatherData(weather);

                        final LocationPanelViewModel panel = new LocationPanelViewModel(weather);
                        panel.setLocationData(location);

                        // Set properties if necessary
                        if (mEditMode) panel.setEditMode(true);

                        appCompatActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int index = mAdapter.getDataset().size();
                                mAdapter.add(panel);
                            }
                        });

                        // Update shortcuts
                        ShortcutCreator.updateShortcuts();

                        // Hide dialog
                        appCompatActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showLoading(false);
                                exitSearchUi();
                            }
                        });
                    }
                });
            }
        });
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        searchView = searchViewLayout.findViewById(R.id.search_view);
        clearButtonView = searchViewLayout.findViewById(R.id.search_close_button);
        progressBar = searchViewLayout.findViewById(R.id.search_progressBar);
        clearButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mSearchFragment != null) {
                    clearButtonView.setVisibility(StringUtils.isNullOrEmpty(s.toString()) ? View.GONE : View.VISIBLE);
                    mSearchFragment.fetchLocations(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable e) {
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

    private void exitSearchUi() {
        searchView.setText("");

        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);

            final FragmentTransaction transaction = getChildFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            mSearchFragment = null;
            transaction.commitAllowingStateLoss();
        }

        hideInputMethod(appCompatActivity == null ? null : appCompatActivity.getCurrentFocus());
        if (searchView != null) searchView.clearFocus();
        if (mActionMode != null) mActionMode.finish();
        if (mMainView != null) mMainView.requestFocus();
        inSearchUI = false;
    }

    private void showInputMethod(View view) {
        if (appCompatActivity != null) {
            InputMethodManager imm = (InputMethodManager) appCompatActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void hideInputMethod(View view) {
        if (appCompatActivity != null) {
            InputMethodManager imm = (InputMethodManager) appCompatActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private OnListChangedListener<LocationPanelViewModel> onListChangedListener = new OnListChangedListener<LocationPanelViewModel>() {
        @Override
        public void onChanged(ArrayList<LocationPanelViewModel> sender, ListChangedArgs e) {
            boolean dataMoved = (e.action == ListChangedAction.REMOVE || e.action == ListChangedAction.MOVE);
            boolean onlyHomeIsLeft = (mAdapter.getItemCount() == 1);

            // Flag that data has changed
            if (mEditMode && dataMoved)
                mDataChanged = true;

            if (mEditMode && (e.newStartingIndex == App.HOMEIDX || e.oldStartingIndex == App.HOMEIDX))
                mHomeChanged = true;

            // Cancel edit Mode
            if (mEditMode && onlyHomeIsLeft)
                toggleEditMode();

            // Disable EditMode if only single location
            if (optionsMenu != null) {
                MenuItem editMenuBtn = optionsMenu.findItem(R.id.action_editmode);
                if (editMenuBtn != null)
                    editMenuBtn.setVisible(!onlyHomeIsLeft);
            }
        }
    };

    private RecyclerOnClickListenerInterface onRecyclerLongClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(View view, int position) {
            if (!mEditMode && mAdapter.getItemCount() > 1) toggleEditMode();
        }
    };

    private void toggleEditMode() {
        // Toggle EditMode
        mEditMode = !mEditMode;

        MenuItem editMenuBtn = optionsMenu.findItem(R.id.action_editmode);
        // Change EditMode button drwble
        editMenuBtn.setIcon(mEditMode ? R.drawable.ic_done_white_24dp : R.drawable.ic_mode_edit_white_24dp);
        // Change EditMode button label
        editMenuBtn.setTitle(mEditMode ? R.string.abc_action_mode_done : R.string.action_editmode);

        // Set Drag & Swipe ability
        mITHCallback.setLongPressDragEnabled(mEditMode);
        mITHCallback.setItemViewSwipeEnabled(mEditMode);

        if (mEditMode) {
            // Unregister events
            gpsPanel.setOnClickListener(null);
            mAdapter.setOnClickListener(null);
            mAdapter.setOnLongClickListener(null);
        } else {
            // Register events
            gpsPanel.setOnClickListener(onPanelClickListener);
            mAdapter.setOnClickListener(onRecyclerClickListener);
            mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
        }

        for (LocationPanelViewModel view : mAdapter.getDataset()) {
            view.setEditMode(mEditMode);
            mAdapter.notifyItemChanged(mAdapter.getDataset().indexOf(view));

            if (!mEditMode && mDataChanged) {
                final String query = view.getLocationData().getQuery();
                final int pos = mAdapter.getDataset().indexOf(view);
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        Settings.moveLocation(query, pos);
                    }
                });
            }
        }

        if (!mEditMode && mHomeChanged) {
            WeatherWidgetService.enqueueWork(appCompatActivity, new Intent(appCompatActivity, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));

            App.getInstance().getAppContext().startService(
                    new Intent(App.getInstance().getAppContext(), WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
            App.getInstance().getAppContext().startService(
                    new Intent(App.getInstance().getAppContext(), WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
        }

        mDataChanged = false;
        mHomeChanged = false;
    }
}