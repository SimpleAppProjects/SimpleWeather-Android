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
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ListChangedAction;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.CommonActions;
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
import com.thewizrd.shared_resources.weatherdata.WeatherErrorListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherLoadedListenerInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.fragments.LocationSearchFragment;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperCallback;
import com.thewizrd.simpleweather.helpers.OffsetMargin;
import com.thewizrd.simpleweather.helpers.SwipeToDeleteOffSetItemDecoration;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface;
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LocationsFragment extends ToolbarFragment
        implements WeatherLoadedListenerInterface, WeatherErrorListenerInterface, SnackbarManagerInterface {
    private boolean mLoaded = false;
    private boolean mEditMode = false;
    private boolean mDataChanged = false;
    private boolean mHomeChanged = false;
    private boolean[] mErrorCounter;

    private SnackbarManager mSnackMgr;

    // Views
    private NestedScrollView mScrollView;
    private RecyclerView mRecyclerView;
    private LocationPanelAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelperCallback mITHCallback;
    private ExtendedFloatingActionButton addLocationsButton;
    private BottomNavigationView mBottomNavView;

    // Search
    private View mSearchFragmentContainer;
    private LocationSearchFragment mSearchFragment;
    private boolean inSearchUI;

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

    @Override
    public void onDestroy() {
        // Cancel pending actions
        if (inSearchUI) exitSearchUi(true);
        if (cts != null) cts.cancel();
        if (mSearchFragment != null) mSearchFragment.ctsCancel();

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if (inSearchUI) exitSearchUi(true);
        if (mSearchFragment != null) mSearchFragment.ctsCancel();
        super.onDetach();
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    public void onWeatherLoaded(final LocationData location, final Weather weather) {
        final List<LocationPanelViewModel> dataSet = mAdapter.getDataset();

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (isCtsCancelRequested())
                    return;

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
                        final LocationPanelViewModel finalPanel = panel;
                        mRecyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyItemChanged(mAdapter.getViewPosition(finalPanel));
                            }
                        });
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
        });
    }

    @Override
    public void onWeatherError(final WeatherException wEx) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    public void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = new SnackbarManager(getRootView());
            mSnackMgr.setSwipeDismissEnabled(true);
            mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        }
    }

    @Override
    public void showSnackbar(com.thewizrd.simpleweather.snackbar.Snackbar snackbar, com.google.android.material.snackbar.Snackbar.Callback callback) {
        if (mSnackMgr != null) mSnackMgr.show(snackbar, callback);
    }

    @Override
    public void dismissAllSnackbars() {
        if (mSnackMgr != null) mSnackMgr.dismissAll();
    }

    @Override
    public void unloadSnackManager() {
        dismissAllSnackbars();
        mSnackMgr = null;
    }

    // For LocationPanels
    private RecyclerOnClickListenerInterface onRecyclerClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(View view, int position) {
            if (view != null && view.isEnabled() && view.getTag() instanceof LocationData) {
                LocationData locData = (LocationData) view.getTag();

                if (locData.equals(Settings.getHomeData())) {
                    FragmentManager fragMgr = getAppCompatActivity().getSupportFragmentManager();
                    Fragment home = fragMgr.findFragmentByTag(Constants.FRAGTAG_HOME);

                    // Pop all since we're going home
                    fragMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    if (home == null) {
                        fragMgr.beginTransaction()
                                .replace(R.id.fragment_container, new WeatherNowFragment(), Constants.FRAGTAG_HOME)
                                .commit();
                    }
                } else {
                    /*
                     * NOTE
                     * Hide current fragment and commit transaction
                     * This is to avoid showing the fragment again from the backstack
                     */
                    getAppCompatActivity().getSupportFragmentManager().beginTransaction()
                            .remove(LocationsFragment.this)
                            .commit();

                    // Navigate to WeatherNowFragment
                    Fragment fragment = WeatherNowFragment.newInstance(locData);

                    getAppCompatActivity().getSupportFragmentManager().beginTransaction()
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .add(R.id.fragment_container, fragment, Constants.FRAGTAG_FAVORITES)
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your fragment here
        mLoaded = true;

        mErrorCounter = new boolean[WeatherUtils.ErrorStatus.values().length];

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        addGPSPanel();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                            }
                        });
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                            }
                        });
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
        if (inSearchUI) {
            exitSearchUi(false);
            return true;
        }
        if (mEditMode) {
            toggleEditMode();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_locations, root, true);
        // Request focus away from RecyclerView
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        mBottomNavView = getAppCompatActivity().findViewById(R.id.bottom_nav_bar);

        mScrollView = view.findViewById(R.id.scrollView);
        /*
           Capture touch events on ScrollView
           Expand or collapse FAB (MaterialButton) based on scroll direction
           Collapse FAB if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
           Expand FAB if we're scrolling to the top (items at the top are already visible)
        */
        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView nestedScrollView, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int dY = scrollY - oldScrollY;
                if (dY < 0) {
                    addLocationsButton.extend();
                } else {
                    addLocationsButton.shrink();
                }
            }
        });

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
                        paddingEnd + insets.getSystemWindowInsetRight(), paddingBottom);
                return insets;
            }
        });

        getToolbar().setOnMenuItemClickListener(menuItemClickListener);
        mSearchFragmentContainer = view.findViewById(R.id.search_fragment_container);

        mSearchFragmentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi(false);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(mSearchFragmentContainer, new OnApplyWindowInsetsListener() {
            private int paddingStart = ViewCompat.getPaddingStart(mSearchFragmentContainer);
            private int paddingTop = mSearchFragmentContainer.getPaddingTop();
            private int paddingEnd = ViewCompat.getPaddingEnd(mSearchFragmentContainer);
            private int paddingBottom = mSearchFragmentContainer.getPaddingBottom();

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.getSystemWindowInsetLeft(),
                        paddingTop + insets.getSystemWindowInsetTop(),
                        paddingEnd + insets.getSystemWindowInsetRight(),
                        paddingBottom + insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        addLocationsButton = view.findViewById(R.id.fab);
        addLocationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide FAB in actionmode
                v.setVisibility(View.GONE);
                prepareSearchUI();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(addLocationsButton, new OnApplyWindowInsetsListener() {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) addLocationsButton.getLayoutParams();
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

        mRecyclerView = view.findViewById(R.id.locations_container);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getAppCompatActivity()) {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationPanelAdapter();
        mAdapter.setOnClickListener(onRecyclerClickListener);
        mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
        mAdapter.setOnListChangedCallback(onListChangedListener);
        mRecyclerView.setAdapter(mAdapter);
        mITHCallback = new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mITHCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        SwipeToDeleteOffSetItemDecoration swipeDecor =
                new SwipeToDeleteOffSetItemDecoration(mRecyclerView.getContext(), 2f,
                        OffsetMargin.TOP | OffsetMargin.BOTTOM);
        mITHCallback.setItemTouchHelperCallbackListener(swipeDecor);
        mRecyclerView.addItemDecoration(swipeDecor);
        SimpleItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        mRecyclerView.setItemAnimator(animator);

        // Turn off by default
        mITHCallback.setLongPressDragEnabled(false);
        mITHCallback.setItemViewSwipeEnabled(false);

        mLoaded = true;

        // Get SearchUI state
        if (savedInstanceState != null && savedInstanceState.getBoolean(Constants.KEY_SEARCHUI, false)) {
            inSearchUI = true;

            // Restart SearchUI
            prepareSearchUI();
        }

        // Create options menu
        createOptionsMenu();

        // Add Adapter as Lifecycle observer
        this.getLifecycle().addObserver(mAdapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        this.getLifecycle().removeObserver(mAdapter);
        super.onDestroyView();
    }

    @Override
    public void updateWindowColors() {
        super.updateWindowColors();

        addLocationsButton.setBackgroundTintList(ColorStateList.valueOf(ActivityUtils.getColor(getAppCompatActivity(), R.attr.colorPrimary)));
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

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
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
        }, 500, cts.getToken()); // Add a minor delay for a smoother transition
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            initSnackManager();
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });
        }
    }

    @Override
    public void onPause() {
        if (inSearchUI) exitSearchUi(true);
        // Cancel pending actions
        if (cts != null) cts.cancel();
        if (mSearchFragment != null) mSearchFragment.ctsCancel();

        unloadSnackManager();

        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();

        mLoaded = false;

        // Reset error counter
        Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            // Cancel pending actions
            if (cts != null) cts.cancel();
            if (mSearchFragment != null) mSearchFragment.ctsCancel();
        }

        if (!hidden && this.isVisible()) {
            initSnackManager();
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    resume();
                }
            });
        } else if (hidden) {
            if (inSearchUI) exitSearchUi(true);
            if (mEditMode) toggleEditMode();

            unloadSnackManager();

            mLoaded = false;
            // Reset error counter
            Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save ActionMode state
        outState.putBoolean(Constants.KEY_SEARCHUI, inSearchUI);

        super.onSaveInstanceState(outState);
    }

    private void loadLocations() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (getAppCompatActivity() != null) {
                    // Load up saved locations
                    List<LocationData> locations = new ArrayList<>(Settings.getFavorites());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.removeAll();
                        }
                    });

                    if (isCtsCancelRequested())
                        return;

                    // Setup saved favorite locations
                    LocationData gpsData = null;
                    if (Settings.useFollowGPS()) {
                        gpsData = getGPSPanel();

                        if (gpsData != null) {
                            final LocationPanelViewModel gpsPanelViewModel = new LocationPanelViewModel();
                            gpsPanelViewModel.setLocationData(gpsData);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.add(0, gpsPanelViewModel);
                                }
                            });
                        }
                    }

                    for (LocationData location : locations) {
                        final LocationPanelViewModel panel = new LocationPanelViewModel();
                        panel.setLocationData(location);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.add(panel);
                            }
                        });
                    }

                    if (isCtsCancelRequested())
                        return;

                    if (gpsData != null)
                        locations.add(0, gpsData);

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
            }
        });
    }

    private LocationData getGPSPanel() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() throws Exception {
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
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
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
                        (mAdapter.getFavoritesCount() > 0 && !Settings.getAPI().equals(mAdapter.getFirstFavPanel().getWeatherSource())))
                    reload = true;

                if (Settings.useFollowGPS()) {
                    if (!reload && (gpsPanelViewModel != null && !locations.get(0).getQuery().equals(gpsPanelViewModel.getLocationData().getQuery())))
                        reload = true;
                }

                if (isCtsCancelRequested())
                    return;

                if (reload) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.removeAll();
                        }
                    });
                    loadLocations();
                } else {
                    List<LocationPanelViewModel> dataset = mAdapter.getDataset();

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
            }
        });
    }

    private void addGPSPanel() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                // Setup saved favorite locations
                LocationData gpsData = null;
                if (Settings.useFollowGPS()) {
                    gpsData = getGPSPanel();

                    if (gpsData != null) {
                        final LocationPanelViewModel gpsPanelViewModel = new LocationPanelViewModel();
                        gpsPanelViewModel.setLocationData(gpsData);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.add(0, gpsPanelViewModel);
                            }
                        });
                    }
                }

                if (isCtsCancelRequested())
                    return;

                if (gpsData != null) {
                    WeatherDataLoader wLoader = new WeatherDataLoader(gpsData, LocationsFragment.this, LocationsFragment.this);
                    wLoader.loadWeatherData(false);
                }
            }
        });
    }

    private void removeGPSPanel() {
        if (mAdapter != null && mAdapter.hasGPSHeader()) {
            mAdapter.removeGPSPanel();
        }
    }

    private LocationData updateLocation() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() throws Exception {
                LocationData locationData = null;

                if (Settings.useFollowGPS()) {
                    if (getAppCompatActivity() != null && ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION_REQUEST_CODE);
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

                        view = wm.getLocation(location);

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
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            LocationData locData = updateLocation();
                            if (locData != null) {
                                Settings.saveLastGPSLocData(locData);
                                refreshLocations();

                                LocalBroadcastManager.getInstance(getAppCompatActivity())
                                        .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        removeGPSPanel();
                                    }
                                });
                            }
                        }
                    });
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

    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        if (childFragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) childFragment;
        }
    }

    private void prepareSearchUI() {
        mBottomNavView.setVisibility(View.GONE);
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
        transaction.commitNowAllowingStateLoss();
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
        fragmentAniSet.setDuration(ANIMATION_DURATION);
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

                        mSearchFragment.showLoading(true);

                        if (ctsToken.isCancellationRequested()) {
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        String country_code = query_vm.getLocationCountry();
                        if (!StringUtils.isNullOrWhitespace(country_code))
                            country_code = country_code.toLowerCase();

                        if (WeatherAPI.NWS.equals(Settings.getAPI()) && !("usa".equals(country_code) || "us".equals(country_code))) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                                    mSearchFragment.showLoading(false);
                                }
                            });
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
                                        mSearchFragment.showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                                        mSearchFragment.showLoading(false);
                                    }
                                });
                                return;
                            }
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showLoading(false);
                                    exitSearchUi(false);
                                }
                            });
                            return;
                        }

                        if (ctsToken.isCancellationRequested()) {
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        LocationData location = new LocationData(query_vm);
                        if (!location.isValid()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSearchFragment.showSnackbar(Snackbar.make(R.string.werror_noweather, Snackbar.Duration.SHORT),
                                            new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                                    mSearchFragment.showLoading(false);
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
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSearchFragment.showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                                    }
                                });
                            }
                        }

                        if (weather == null) {
                            mSearchFragment.showLoading(false);
                            return;
                        }

                        // We got our data so disable controls just in case
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.getDataset().clear();
                                adapter.notifyDataSetChanged();
                            }
                        });

                        if (mSearchFragment != null && mSearchFragment.getView() != null &&
                                mSearchFragment.getView().findViewById(R.id.recycler_view) instanceof RecyclerView) {
                            runOnUiThread(new Runnable() {
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

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.add(panel);
                            }
                        });

                        // Update shortcuts
                        ShortcutCreator.updateShortcuts();

                        // Hide dialog
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSearchFragment.showLoading(false);
                                exitSearchUi(false);
                            }
                        });
                    }
                });
            }
        });
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitNowAllowingStateLoss();
    }

    private void removeSearchFragment() {
        mSearchFragment.setUserVisibleHint(false);
        final FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction();
        transaction.remove(mSearchFragment);
        mSearchFragment = null;
        transaction.commitNowAllowingStateLoss();
    }

    private void exitSearchUi(boolean skipAnimation) {
        if (mSearchFragment != null) {
            // Exit transition
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

        if (mAdapter.getDataCount() < MAX_LOCATIONS)
            addLocationsButton.show();

        mBottomNavView.setVisibility(View.VISIBLE);

        hideInputMethod(getAppCompatActivity() == null ? null : getAppCompatActivity().getCurrentFocus());
        if (getRootView() != null) getRootView().requestFocus();
        inSearchUI = false;
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

    private OnListChangedListener<LocationPanelViewModel> onListChangedListener = new OnListChangedListener<LocationPanelViewModel>() {
        @Override
        public void onChanged(ArrayList<LocationPanelViewModel> sender, ListChangedArgs e) {
            final boolean dataMoved = (e.action == ListChangedAction.REMOVE || e.action == ListChangedAction.MOVE);
            final boolean onlyHomeIsLeft = (mAdapter.getFavoritesCount() == 1);

            // Flag that data has changed
            if (mEditMode && dataMoved)
                mDataChanged = true;

            if (mEditMode && (e.newStartingIndex == App.HOMEIDX || e.oldStartingIndex == App.HOMEIDX))
                mHomeChanged = true;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Hide FAB; Don't allow adding more locations
                    if (mAdapter.getDataCount() >= MAX_LOCATIONS) {
                        addLocationsButton.hide();
                    } else {
                        addLocationsButton.show();
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

            if (view.getLocationType() != LocationType.GPS.getValue() && !mEditMode && mDataChanged) {
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