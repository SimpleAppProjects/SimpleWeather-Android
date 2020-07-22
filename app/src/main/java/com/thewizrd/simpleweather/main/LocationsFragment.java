package com.thewizrd.simpleweather.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ListChangedAction;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.databinding.FragmentLocationsBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperCallback;
import com.thewizrd.simpleweather.helpers.OffsetMargin;
import com.thewizrd.simpleweather.helpers.SwipeToDeleteOffSetItemDecoration;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LocationsFragment extends ToolbarFragment
        implements WeatherRequest.WeatherErrorListener {
    private boolean mLoaded = false;
    private boolean mEditMode = false;
    private boolean mDataChanged = false;
    private boolean mHomeChanged = false;
    private boolean[] mErrorCounter;

    // Views
    private FragmentLocationsBinding binding;
    private LocationPanelAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelperCallback mITHCallback;
    private BottomNavigationView mBottomNavView;

    // GPS Location
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;
    private CancellationTokenSource cts;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    private static final int ANIMATION_DURATION = 240;
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int MAX_LOCATIONS = Settings.getMaxLocations();

    // OptionsMenu
    private Menu optionsMenu;

    private WeatherManager wm;

    public LocationsFragment() {
        // Required empty public constructor
        wm = WeatherManager.getInstance();
    }

    @Override
    protected int getTitle() {
        return R.string.label_nav_locations;
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    private void onWeatherLoaded(final LocationData location, final Weather weather) {
        if (isCtsCancelRequested()) return;

        final List<LocationPanelViewModel> dataSet = mAdapter.getDataset();

        if (weather != null && weather.isValid()) {
            // Update panel weather
            LocationPanelViewModel panel = null;

            if (location.getLocationType() == LocationType.GPS) {
                for (LocationPanelViewModel panelVM : dataSet) {
                    if (panelVM.getLocationData().getLocationType().equals(LocationType.GPS)) {
                        panel = panelVM;
                        break;
                    }
                }
            } else {
                for (LocationPanelViewModel panelVM : dataSet) {
                    if (!panelVM.getLocationData().getLocationType().equals(LocationType.GPS)
                            && panelVM.getLocationData().getQuery().equals(location.getQuery())) {
                        panel = panelVM;
                        break;
                    }
                }
            }

            // Just in case
            if (panel == null) {
                AnalyticsLogger.logEvent("LocationsFragment: panel == null");
                for (LocationPanelViewModel panelVM : dataSet) {
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
                panel.updateBackground();
                mAdapter.notifyItemChanged(mAdapter.getViewPosition(panel));
            } else {
                Logger.writeLine(Log.WARN, "LocationsFragment: Location panel not found");
                Logger.writeLine(Log.WARN, "LocationsFragment: LocationData: %s", location.toString());
                Logger.writeLine(Log.WARN, "LocationsFragment: Dumping adapter data...");
                for (int i = 0; i < dataSet.size(); i++) {
                    LocationPanelViewModel vm = dataSet.get(i);
                    Logger.writeLine(Log.WARN, "LocationsFragment: Panel: %d; data: %s", i, vm.getLocationData().toString());
                }
            }
        }
    }

    public void onWeatherError(final WeatherException wEx) {
        if (isCtsCancelRequested())
            return;

        switch (wEx.getErrorStatus()) {
            case NETWORKERROR:
            case NOWEATHER:
                // Show error message and prompt to refresh
                // Only warn once
                if (!mErrorCounter[wEx.getErrorStatus().getValue()]) {
                    Snackbar snackbar = Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG);
                    snackbar.setAction(R.string.action_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Reset counter to allow retry
                            mErrorCounter[wEx.getErrorStatus().getValue()] = false;
                            refreshLocations();
                        }
                    });
                    showSnackbar(snackbar, null);
                    mErrorCounter[wEx.getErrorStatus().getValue()] = true;
                }
                break;
            case QUERYNOTFOUND:
                if (!mErrorCounter[wEx.getErrorStatus().getValue()] && WeatherAPI.NWS.equals(Settings.getAPI())) {
                    showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null);
                    mErrorCounter[wEx.getErrorStatus().getValue()] = true;
                    break;
                }
            default:
                // Show error message
                // Only warn once
                if (!mErrorCounter[wEx.getErrorStatus().getValue()]) {
                    showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG), null);
                    mErrorCounter[wEx.getErrorStatus().getValue()] = true;
                }
                break;
        }
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(getRootView());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }

    // For LocationPanels
    private RecyclerOnClickListenerInterface onRecyclerClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(View view, int position) {
            AnalyticsLogger.logEvent("LocationsFragment: recycler click");

            if (view != null && view.isEnabled() && view.getTag() instanceof LocationData) {
                LocationData locData = (LocationData) view.getTag();
                LocationPanelViewModel vm = mAdapter.getPanelViewModel(position);

                boolean isHome = ObjectsCompat.equals(locData, Settings.getHomeData());

                LocationsFragmentDirections.ActionLocationsFragmentToWeatherNowFragment args =
                        LocationsFragmentDirections.actionLocationsFragmentToWeatherNowFragment(
                                JSONParser.serializer(locData, LocationData.class),
                                vm.getImageData() != null ? vm.getImageData().getImageURI() : null
                        ).setHome(isHome);

                Navigation.findNavController(binding.getRoot()).navigate(args);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        mLoaded = true;
        AnalyticsLogger.logEvent("LocationsFragment: onCreate");

        mErrorCounter = new boolean[WeatherUtils.ErrorStatus.values().length];

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        addGPSPanel();
                    } else {
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                    }

                    new AsyncTask<Void>().await(new Callable<Void>() {
                        @Override
                        public Void call() {
                            try {
                                return Tasks.await(mFusedLocationClient.removeLocationUpdates(mLocCallback));
                            } catch (ExecutionException | InterruptedException e) {
                                Logger.writeLine(Log.ERROR, e);
                            }

                            return null;
                        }
                    });
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
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                    }
                }
            };
        } else {
            mLocListnr = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    addGPSPanel();
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
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "LocationsFragment: stopLocationUpdates: updates never requested, no-op.");
            return;
        }

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
    public boolean onBackPressed() {
        if (mEditMode) {
            toggleEditMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        binding = FragmentLocationsBinding.inflate(inflater, root, true);
        // Request focus away from RecyclerView
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        mBottomNavView = getAppCompatActivity().findViewById(R.id.bottom_nav_bar);

        /*
           Capture touch events on RecyclerView
           Expand or collapse FAB (MaterialButton) based on scroll direction
           Collapse FAB if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
           Expand FAB if we're scrolling to the top (items at the top are already visible)
        */
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int scrollState = RecyclerView.SCROLL_STATE_IDLE;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                scrollState = newState;
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                    if (dy < 0) {
                        binding.fab.extend();
                    } else {
                        binding.fab.shrink();
                    }
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, new OnApplyWindowInsetsListener() {
            private int paddingStart = ViewCompat.getPaddingStart(binding.recyclerView);
            private int paddingTop = binding.recyclerView.getPaddingTop();
            private int paddingEnd = ViewCompat.getPaddingEnd(binding.recyclerView);
            private int paddingBottom = binding.recyclerView.getPaddingBottom();

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.getSystemWindowInsetLeft(),
                        paddingTop,
                        paddingEnd + insets.getSystemWindowInsetRight(), paddingBottom);
                return insets;
            }
        });

        getToolbar().setOnMenuItemClickListener(menuItemClickListener);

        // FAB
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(binding.getRoot())
                        .navigate(LocationsFragmentDirections.actionLocationsFragmentToLocationSearchFragment());
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.fab, new OnApplyWindowInsetsListener() {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) binding.fab.getLayoutParams();
            private int marginStart = MarginLayoutParamsCompat.getMarginStart(layoutParams);
            private int marginEnd = MarginLayoutParamsCompat.getMarginEnd(layoutParams);

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

                MarginLayoutParamsCompat.setMarginStart(layoutParams, marginStart + insets.getSystemWindowInsetLeft());
                MarginLayoutParamsCompat.setMarginEnd(layoutParams, marginEnd + insets.getSystemWindowInsetRight());

                v.setLayoutParams(layoutParams);
                return insets;
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getAppCompatActivity()) {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        };
        binding.recyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationPanelAdapter();
        mAdapter.setOnClickListener(onRecyclerClickListener);
        mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
        mAdapter.setOnListChangedCallback(onListChangedListener);
        binding.recyclerView.setAdapter(mAdapter);
        mITHCallback = new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mITHCallback);
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView);
        SwipeToDeleteOffSetItemDecoration swipeDecor =
                new SwipeToDeleteOffSetItemDecoration(binding.recyclerView.getContext(), 2f,
                        OffsetMargin.TOP | OffsetMargin.BOTTOM);
        mITHCallback.setItemTouchHelperCallbackListener(swipeDecor);
        binding.recyclerView.addItemDecoration(swipeDecor);
        SimpleItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        binding.recyclerView.setItemAnimator(animator);

        // Turn off by default
        mITHCallback.setLongPressDragEnabled(false);
        mITHCallback.setItemViewSwipeEnabled(false);

        mLoaded = true;

        // Create options menu
        createOptionsMenu();

        // Add Adapter as Lifecycle observer
        this.getLifecycle().addObserver(mAdapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroupCompat.setTransitionGroup(binding.recyclerView, true);
    }

    @Override
    public void onDestroyView() {
        this.getLifecycle().removeObserver(mAdapter);
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), LocationPanelAdapter.Payload.IMAGE_UPDATE);
    }

    private void createOptionsMenu() {
        // Inflate the menu; this adds items to the action bar if it is present.
        Menu menu = getToolbar().getMenu();
        optionsMenu = menu;
        menu.clear();
        getToolbar().inflateMenu(R.menu.locations);

        boolean onlyHomeIsLeft = (mAdapter.getFavoritesCount() == 1);
        MenuItem editMenuBtn = optionsMenu.findItem(R.id.action_editmode);
        if (editMenuBtn != null) {
            editMenuBtn.setVisible(!onlyHomeIsLeft);
            // Change EditMode button drwble
            editMenuBtn.setIcon(mEditMode ? R.drawable.ic_done_white_24dp : R.drawable.ic_mode_edit_white_24dp);
            // Change EditMode button label
            editMenuBtn.setTitle(mEditMode ? R.string.abc_action_mode_done : R.string.action_editmode);
            MenuItemCompat.setIconTintList(editMenuBtn, ColorStateList.valueOf(ContextCompat.getColor(getAppCompatActivity(), R.color.invButtonColorText)));
        }
    }

    private Toolbar.OnMenuItemClickListener menuItemClickListener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent AppCompatActivity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_editmode) {
                toggleEditMode();
                return true;
            }

            return false;
        }
    };

    private void resume() {
        cts = new CancellationTokenSource();

        // Update view on resume
        // ex. If temperature unit changed
        if (mAdapter.getDataCount() == 0) {
            // New instance; Get locations and load up weather data
            loadLocations();
        } else if (!mLoaded) {
            // Refresh view
            refreshLocations();
            mLoaded = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            AnalyticsLogger.logEvent("LocationsFragment: onResume");
            resume();
        }
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("LocationsFragment: onPause");
        // Cancel pending actions
        if (cts != null) cts.cancel();

        // Remove location updates to save battery.
        stopLocationUpdates();
        mLoaded = false;
        // Reset error counter
        Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);

        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            // Cancel pending actions
            if (cts != null) cts.cancel();
        }

        if (!hidden && this.isVisible()) {
            AnalyticsLogger.logEvent("LocationsFragment: onHiddenChanged");
            resume();
        } else if (hidden) {
            if (mEditMode) toggleEditMode();
            mLoaded = false;
            // Reset error counter
            Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);
        }
    }

    private void loadLocations() {
        if (getAppCompatActivity() != null) {
            // Load up saved locations
            List<LocationData> locations = new ArrayList<>(Settings.getFavorites());
            mAdapter.removeAll();

            if (isCtsCancelRequested())
                return;

            // Setup saved favorite locations
            LocationData gpsData = null;
            if (Settings.useFollowGPS()) {
                gpsData = getGPSPanel();

                if (gpsData != null) {
                    final LocationPanelViewModel gpsPanelViewModel = new LocationPanelViewModel();
                    gpsPanelViewModel.setLocationData(gpsData);

                    mAdapter.add(0, gpsPanelViewModel);
                }
            }

            for (LocationData location : locations) {
                final LocationPanelViewModel panel = new LocationPanelViewModel();
                panel.setLocationData(location);
                mAdapter.add(panel);
            }

            if (isCtsCancelRequested())
                return;

            if (gpsData != null)
                locations.add(0, gpsData);

            for (final LocationData location : locations) {
                WeatherDataLoader wLoader = new WeatherDataLoader(location);
                wLoader.loadWeatherData(new WeatherRequest.Builder()
                        .forceRefresh(false)
                        .setErrorListener(LocationsFragment.this)
                        .build())
                        .addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<Weather>() {
                            @Override
                            public void onSuccess(final Weather weather) {
                                onWeatherLoaded(location, weather);
                            }
                        });
            }
        }
    }

    private LocationData getGPSPanel() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() {
                // Setup gps panel
                if (getAppCompatActivity() != null && Settings.useFollowGPS()) {
                    LocationData locData = Settings.getLastGPSLocData();

                    if (isCtsCancelRequested())
                        return null;

                    if (locData == null || locData.getQuery() == null) {
                        locData = updateLocation();
                    }

                    if (isCtsCancelRequested())
                        return null;

                    if (locData != null && locData.getQuery() != null) {
                        return locData;
                    }
                }
                return null;
            }
        });
    }

    private void refreshLocations() {
        // Reload all panels if needed
        List<LocationData> locations = new ArrayList<>(Settings.getLocationData());
        if (Settings.useFollowGPS()) {
            LocationData homeData = Settings.getLastGPSLocData();
            locations.add(0, homeData);
        }
        LocationPanelViewModel gpsPanelViewModel = mAdapter.getGPSPanel();

        boolean reload = (locations.size() != mAdapter.getDataCount() ||
                Settings.useFollowGPS() && gpsPanelViewModel == null || !Settings.useFollowGPS() && gpsPanelViewModel != null);

        // Reload if weather source differs
        if ((gpsPanelViewModel != null && !Settings.getAPI().equals(gpsPanelViewModel.getWeatherSource())) ||
                (mAdapter.getFavoritesCount() > 0 && mAdapter.getFirstFavPanel() != null && !Settings.getAPI().equals(mAdapter.getFirstFavPanel().getWeatherSource())))
            reload = true;

        if (Settings.useFollowGPS()) {
            if (!reload && (gpsPanelViewModel != null && !locations.get(0).getQuery().equals(gpsPanelViewModel.getLocationData().getQuery())))
                reload = true;
        }

        if (isCtsCancelRequested())
            return;

        if (reload) {
            mAdapter.removeAll();
            loadLocations();
        } else {
            List<LocationPanelViewModel> dataset = mAdapter.getDataset();

            for (final LocationPanelViewModel view : dataset) {
                WeatherDataLoader wLoader = new WeatherDataLoader(view.getLocationData());
                wLoader.loadWeatherData(new WeatherRequest.Builder()
                        .forceRefresh(false)
                        .setErrorListener(LocationsFragment.this)
                        .build())
                        .addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<Weather>() {
                            @Override
                            public void onSuccess(final Weather weather) {
                                onWeatherLoaded(view.getLocationData(), weather);
                            }
                        });
            }
        }
    }

    private void addGPSPanel() {
        // Setup saved favorite locations
        final LocationData gpsData;
        if (Settings.useFollowGPS()) {
            gpsData = getGPSPanel();

            if (gpsData != null) {
                final LocationPanelViewModel gpsPanelViewModel = new LocationPanelViewModel();
                gpsPanelViewModel.setLocationData(gpsData);
                mAdapter.add(0, gpsPanelViewModel);
            }
        } else {
            gpsData = null;
        }

        if (isCtsCancelRequested())
            return;

        if (gpsData != null) {
            WeatherDataLoader wLoader = new WeatherDataLoader(gpsData);
            wLoader.loadWeatherData(new WeatherRequest.Builder()
                    .forceRefresh(false)
                    .setErrorListener(LocationsFragment.this)
                    .build())
                    .addOnSuccessListener(getAppCompatActivity(), new OnSuccessListener<Weather>() {
                        @Override
                        public void onSuccess(final Weather weather) {
                            onWeatherLoaded(gpsData, weather);
                        }
                    });
        }
    }

    private void removeGPSPanel() {
        if (mAdapter != null && mAdapter.hasGPSHeader()) {
            mAdapter.removeGPSPanel();
        }
    }

    private LocationData updateLocation() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() {
                LocationData locationData = null;

                if (Settings.useFollowGPS()) {
                    if (getAppCompatActivity() != null && ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                        }
                        return null;
                    }

                    Location location = null;

                    if (isCtsCancelRequested())
                        return null;

                    LocationManager locMan = null;
                    if (getAppCompatActivity() != null)
                        locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.LONG), null);
                                removeGPSPanel();
                            }
                        });

                        // Disable GPS feature if location is not enabled
                        Settings.setFollowGPS(false);
                        return null;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() {
                                Location result = null;
                                try {
                                    result = AsyncTask.await(mFusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
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
                                    showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                                    removeGPSPanel();
                                }
                            });
                        }
                    }

                    if (location != null && !mRequestingLocationUpdates) {
                        LocationQueryViewModel view = null;

                        if (isCtsCancelRequested())
                            return null;

                        try {
                            view = wm.getLocation(location);
                        } catch (WeatherException e) {
                            Logger.writeLine(Log.ERROR, e);
                            // Stop since there is no valid query
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    removeGPSPanel();
                                }
                            });
                            return null;
                        }

                        if (isCtsCancelRequested())
                            return null;

                        if (StringUtils.isNullOrEmpty(view.getLocationQuery()))
                            view = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    removeGPSPanel();
                                }
                            });
                            return null;
                        }

                        // Save location as last known
                        locationData = new LocationData(view, location);
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
                    LocationData locData = updateLocation();
                    if (locData != null) {
                        Settings.saveLastGPSLocData(locData);
                        refreshLocations();

                        LocalBroadcastManager.getInstance(getAppCompatActivity())
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                    } else {
                        removeGPSPanel();
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Settings.setFollowGPS(false);
                    removeGPSPanel();
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null);
                }
                return;
            }
            default:
                break;
        }
    }

    private OnListChangedListener<LocationPanelViewModel> onListChangedListener = new OnListChangedListener<LocationPanelViewModel>() {
        @Override
        public void onChanged(final ArrayList<LocationPanelViewModel> sender, final ListChangedArgs<LocationPanelViewModel> e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (binding == null) return;
                    boolean dataMoved = (e.action == ListChangedAction.REMOVE || e.action == ListChangedAction.MOVE);
                    boolean onlyHomeIsLeft = (mAdapter.getFavoritesCount() == 1);

                    // Flag that data has changed
                    if (mEditMode && dataMoved)
                        mDataChanged = true;

                    if (mEditMode && (e.newStartingIndex == App.HOMEIDX || e.oldStartingIndex == App.HOMEIDX))
                        mHomeChanged = true;

                    // Hide FAB; Don't allow adding more locations
                    if (mAdapter.getDataCount() >= MAX_LOCATIONS) {
                        binding.fab.hide();
                    } else {
                        binding.fab.show();
                    }

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
            });
        }
    };

    private RecyclerOnClickListenerInterface onRecyclerLongClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(View view, int position) {
            if (mAdapter.getItemViewType(position) == LocationPanelAdapter.ItemType.SEARCH_PANEL) {
                if (!mEditMode && mAdapter.getFavoritesCount() > 1) toggleEditMode();
            }
        }
    };

    private void toggleEditMode() {
        // Toggle EditMode
        mEditMode = !mEditMode;

        MenuItem editMenuBtn = optionsMenu.findItem(R.id.action_editmode);
        if (editMenuBtn != null) {
            // Change EditMode button drwble
            editMenuBtn.setIcon(mEditMode ? R.drawable.ic_done_white_24dp : R.drawable.ic_mode_edit_white_24dp);
            // Change EditMode button label
            editMenuBtn.setTitle(mEditMode ? R.string.abc_action_mode_done : R.string.action_editmode);
            MenuItemCompat.setIconTintList(editMenuBtn, ColorStateList.valueOf(ContextCompat.getColor(getAppCompatActivity(), R.color.invButtonColorText)));
        }

        // Set Drag & Swipe ability
        mITHCallback.setLongPressDragEnabled(mEditMode);
        mITHCallback.setItemViewSwipeEnabled(mEditMode);

        if (mEditMode) {
            // Unregister events
            mAdapter.setOnClickListener(null);
            mAdapter.setOnLongClickListener(null);
        } else {
            // Register events
            mAdapter.setOnClickListener(onRecyclerClickListener);
            mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
        }

        for (LocationPanelViewModel view : mAdapter.getDataset()) {
            view.setEditMode(mEditMode);
            mAdapter.notifyItemChanged(mAdapter.getViewPosition(view));

            if (view.getLocationType() != LocationType.GPS.getValue() && !mEditMode && (mDataChanged || mHomeChanged)) {
                final String query = view.getLocationData().getQuery();
                int dataPosition = mAdapter.getDataset().indexOf(view);
                final int pos = mAdapter.hasGPSHeader() ? --dataPosition : dataPosition;
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        Settings.moveLocation(query, pos);
                    }
                });
            }
        }

        if (!mEditMode && mHomeChanged) {
            LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                    .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
            LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                    .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE));
        }

        mDataChanged = false;
        mHomeChanged = false;
    }
}