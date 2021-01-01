package com.thewizrd.simpleweather.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ListChangedAction;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.lifecycle.LifecycleRunnable;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tzdb.TZDBCache;
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
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.databinding.FragmentLocationsBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.helpers.ItemTouchCallbackListener;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperCallback;
import com.thewizrd.simpleweather.helpers.LocationPanelOffsetDecoration;
import com.thewizrd.simpleweather.helpers.OffsetMargin;
import com.thewizrd.simpleweather.helpers.SwipeToDeleteOffSetItemDecoration;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class LocationsFragment extends ToolbarFragment
        implements WeatherRequest.WeatherErrorListener {
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
    private ActionMode actionMode;

    // GPS Location
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocCallback;
    private LocationListener mLocListnr;

    /**
     * Tracks the status of the location updates request.
     */
    private boolean mRequestingLocationUpdates;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int MAX_LOCATIONS = Settings.getMaxLocations();

    // OptionsMenu
    private Menu optionsMenu;

    private OnBackPressedCallback onBackPressedCallback;

    private WeatherManager wm;

    public LocationsFragment() {
        // Required empty public constructor
        wm = WeatherManager.getInstance();
    }

    @Override
    protected int getTitle() {
        return R.string.label_nav_locations;
    }

    private void onWeatherLoaded(final LocationData location, final Weather weather) {
        runWithView(new Runnable() {
            @Override
            public void run() {
                final List<LocationPanelViewModel> dataSet = mAdapter.getDataset();

                if (weather != null && weather.isValid()) {
                    // Update panel weather
                    LocationPanelViewModel panel;

                    if (location.getLocationType() == LocationType.GPS) {
                        panel = Iterables.find(dataSet, new Predicate<LocationPanelViewModel>() {
                            @Override
                            public boolean apply(@NullableDecl LocationPanelViewModel input) {
                                return input != null && input.getLocationData().getLocationType().equals(LocationType.GPS);
                            }
                        }, null);
                    } else {
                        panel = Iterables.find(dataSet, new Predicate<LocationPanelViewModel>() {
                            @Override
                            public boolean apply(@NullableDecl LocationPanelViewModel input) {
                                return input != null && !input.getLocationData().getLocationType().equals(LocationType.GPS) && input.getLocationData().getQuery().equals(location.getQuery());
                            }
                        }, null);
                    }

                    // Just in case
                    if (panel == null) {
                        AnalyticsLogger.logEvent("LocationsFragment: panel == null");
                        panel = Iterables.find(dataSet, new Predicate<LocationPanelViewModel>() {
                            @Override
                            public boolean apply(@NullableDecl LocationPanelViewModel input) {
                                return input != null && input.getLocationData().getName().equals(location.getName()) &&
                                        input.getLocationData().getLatitude() == location.getLatitude() &&
                                        input.getLocationData().getLongitude() == location.getLongitude() &&
                                        input.getLocationData().getTzLong().equals(location.getTzLong());
                            }
                        }, null);
                    }

                    if (panel != null) {
                        panel.setWeather(weather);
                        mAdapter.notifyItemChanged(mAdapter.getViewPosition(panel));

                        final LocationPanelViewModel finalPanel = panel;
                        AsyncTask.create(new Callable<Void>() {
                            @Override
                            public Void call() {
                                finalPanel.updateBackground();
                                return null;
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                runWithView(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAdapter.notifyItemChanged(mAdapter.getViewPosition(finalPanel), LocationPanelAdapter.Payload.IMAGE_UPDATE);
                                    }
                                });
                            }
                        });
                    } else if (BuildConfig.DEBUG) {
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

    public void onWeatherError(final WeatherException wEx) {
        runWithView(new Runnable() {
            @Override
            public void run() {
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
                        LocationsFragmentDirections.actionLocationsFragmentToWeatherNowFragment()
                                .setData(JSONParser.serializer(locData, LocationData.class))
                                .setBackground(vm.getImageData() != null ? vm.getImageData().getImageURI() : null)
                                .setHome(isHome);

                Navigation.findNavController(binding.getRoot()).navigate(args);
            }
        }
    };

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(!mAdapter.getSelectedItems().isEmpty() ? Integer.toString(mAdapter.getSelectedItems().size()) : "");

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.locations_context, menu);

            MenuItem deleteBtnItem = menu.findItem(R.id.action_delete);
            if (deleteBtnItem != null) {
                deleteBtnItem.setVisible(!mAdapter.getSelectedItems().isEmpty());
            }

            if (!mEditMode) toggleEditMode();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItemCompat.setIconTintList(menu.getItem(i), ColorStateList.valueOf(ContextCompat.getColor(getAppCompatActivity(), R.color.invButtonColorText)));
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    mAdapter.removeSelectedItems();
                    return true;
                case R.id.action_done:
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            if (mEditMode) toggleEditMode();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setExitTransition(new MaterialFadeThrough());
        setEnterTransition(new MaterialFadeThrough());

        // Create your fragment here
        AnalyticsLogger.logEvent("LocationsFragment: onCreate");

        mErrorCounter = new boolean[WeatherUtils.ErrorStatus.values().length];

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getAppCompatActivity());
            mLocCallback = new LocationCallback() {
                @Override
                public void onLocationResult(final LocationResult locationResult) {
                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            if (locationResult != null && locationResult.getLastLocation() != null) {
                                addGPSPanel();
                            } else {
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                            }

                            stopLocationUpdates();
                        }
                    });
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

        onBackPressedCallback = new OnBackPressedCallback(mEditMode) {
            @Override
            public void handleOnBackPressed() {
                if (mEditMode) {
                    toggleEditMode();
                }
            }
        };
        getAppCompatActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
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
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        binding = FragmentLocationsBinding.inflate(inflater, root, true);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        // Request focus away from RecyclerView
        root.setFocusableInTouchMode(true);
        root.requestFocus();

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

        getToolbar().setOnMenuItemClickListener(menuItemClickListener);

        // FAB
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(binding.getRoot())
                        .navigate(
                                LocationsFragmentDirections.actionLocationsFragmentToLocationSearchFragment(),
                                new FragmentNavigator.Extras.Builder()
                                        .addSharedElement(binding.fab, Constants.SHARED_ELEMENT)
                                        .build()
                        );
            }
        });
        ViewCompat.setTransitionName(binding.fab, Constants.SHARED_ELEMENT);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);

        if (ActivityUtils.isLargeTablet(getAppCompatActivity())) {
            // use a linear layout manager
            final GridLayoutManager gridLayoutManager = new GridLayoutManager(getAppCompatActivity(), 2, GridLayoutManager.VERTICAL, false) {
                @Override
                public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                    return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            };
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    switch (mAdapter.getItemViewType(position)) {
                        case LocationPanelAdapter.ItemType.HEADER_FAV:
                        case LocationPanelAdapter.ItemType.HEADER_GPS:
                            return gridLayoutManager.getSpanCount();
                        default:
                            return 1;
                    }
                }
            });
            mLayoutManager = gridLayoutManager;
            binding.recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    super.getItemOffsets(outRect, view, parent, state);
                }
            });
        } else {
            // use a linear layout manager
            mLayoutManager = new LinearLayoutManager(getAppCompatActivity()) {
                @Override
                public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                    return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            };
        }
        binding.recyclerView.setLayoutManager(mLayoutManager);

        // Setup RecyclerView
        mAdapter = new LocationPanelAdapter(new LocationPanelAdapter.ViewHolderLongClickListener() {
            @Override
            public void onLongClick(RecyclerView.ViewHolder holder) {
                mItemTouchHelper.startDrag(holder);
            }
        });
        mAdapter.setOnClickListener(onRecyclerClickListener);
        mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
        mAdapter.setOnListChangedCallback(onListChangedListener);
        mAdapter.setOnSelectionChangedCallback(onSelectionChangedListener);
        binding.recyclerView.setAdapter(mAdapter);
        mITHCallback = new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mITHCallback);
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView);
        mITHCallback.addItemTouchHelperCallbackListener(new ItemTouchCallbackListener() {
            private Handler mMainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public void onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                mDataChanged = true;
                if (mEditMode) {
                    toggleEditMode();
                } else {
                    final List<LocationPanelViewModel> dataSet = mAdapter.getDataset();
                    for (LocationPanelViewModel view : dataSet) {
                        if (view.getLocationType() != LocationType.GPS.getValue()) {
                            updateFavoritesPosition(view);
                        }
                    }

                    if (!mAdapter.hasGPSHeader() && mAdapter.hasSearchHeader()) {
                        int firstFavPosition = mAdapter.getViewPosition(mAdapter.getFirstFavPanel());

                        if (viewHolder.getAdapterPosition() == firstFavPosition || target.getAdapterPosition() == firstFavPosition) {
                            mMainHandler.removeCallbacks(sendUpdateRunner);
                            mMainHandler.postDelayed(sendUpdateRunner, 2500);
                        }
                    }
                }
            }

            @Override
            public void onClearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            }

            private Runnable sendUpdateRunner = new Runnable() {
                @Override
                public void run() {
                    // Home has changed send notice
                    Log.d("LocationsFragment", "Home changed; sending update");
                    LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                            .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                    LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                            .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE));
                }
            };
        });
        if (!ActivityUtils.isLargeTablet(getAppCompatActivity())) {
            SwipeToDeleteOffSetItemDecoration swipeDecor =
                    new SwipeToDeleteOffSetItemDecoration(binding.recyclerView.getContext(), 2f,
                            OffsetMargin.TOP | OffsetMargin.BOTTOM);
            mITHCallback.addItemTouchHelperCallbackListener(swipeDecor);
            binding.recyclerView.addItemDecoration(swipeDecor);
        } else {
            binding.recyclerView.addItemDecoration(new LocationPanelOffsetDecoration(binding.recyclerView.getContext(), 2f));
        }
        SimpleItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        binding.recyclerView.setItemAnimator(animator);

        // Enable touch actions
        mITHCallback.setItemViewSwipeEnabled(false);

        // Create options menu
        createOptionsMenu();

        // Add Adapter as Lifecycle observer
        this.getLifecycle().addObserver(mAdapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adjustPanelContainer();
    }

    private void adjustPanelContainer() {
        if (ActivityUtils.isLargeTablet(getAppCompatActivity())) {
            binding.recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (isViewAlive()) {
                        binding.recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);

                        boolean isLandscape = ActivityUtils.getOrientation(getAppCompatActivity()) == Configuration.ORIENTATION_LANDSCAPE;
                        int viewWidth = binding.recyclerView.getMeasuredWidth();
                        int minColumns = isLandscape ? 2 : 1;

                        // Minimum width for ea. card
                        int minWidth = getAppCompatActivity().getResources().getDimensionPixelSize(R.dimen.location_panel_minwidth);
                        // Available columns based on min card width
                        int availColumns = ((int) (viewWidth / minWidth)) <= 1 ? minColumns : (int) (viewWidth / minWidth);

                        if (binding.recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                            ((GridLayoutManager) binding.recyclerView.getLayoutManager()).setSpanCount(availColumns);
                        }
                    }

                    return true;
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        this.getLifecycle().removeObserver(mAdapter);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustPanelContainer();
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
                actionMode = getAppCompatActivity().startSupportActionMode(actionModeCallback);
                return true;
            }

            return false;
        }
    };

    private void resume() {
        // Update view on resume
        // ex. If temperature unit changed
        if (mAdapter.getDataCount() == 0) {
            // New instance; Get locations and load up weather data
            loadLocations();
        } else {
            // Refresh view
            refreshLocations();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("LocationsFragment: onResume");

        resume();
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("LocationsFragment: onPause");

        // End actionmode
        if (actionMode != null) {
            actionMode.finish();
        }

        // Remove location updates to save battery.
        stopLocationUpdates();

        // Reset error counter
        Arrays.fill(mErrorCounter, 0, mErrorCounter.length, false);

        super.onPause();
    }

    private void loadLocations() {
        runWithView(new LifecycleRunnable(getViewLifecycleOwner().getLifecycle()) {
            @Override
            public void run() {
                // Load up saved locations
                List<LocationData> locations = new ArrayList<>(Settings.getFavorites());
                mAdapter.removeAll();

                if (!isActive() || !isViewAlive()) return;

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

                for (final LocationData location : locations) {
                    final LocationPanelViewModel panel = new LocationPanelViewModel();
                    panel.setLocationData(location);
                    mAdapter.add(panel);

                    WeatherDataLoader wLoader = new WeatherDataLoader(location);
                    wLoader.loadWeatherData(new WeatherRequest.Builder()
                            .forceRefresh(false)
                            .setErrorListener(LocationsFragment.this)
                            .build())
                            .addOnSuccessListener(new OnSuccessListener<Weather>() {
                                @Override
                                public void onSuccess(final Weather weather) {
                                    onWeatherLoaded(location, weather);
                                }
                            });
                }

                if (!isActive() || !isViewAlive()) return;

                if (gpsData != null) {
                    locations.add(0, gpsData);

                    final LocationData finalGpsData = gpsData;
                    WeatherDataLoader wLoader = new WeatherDataLoader(finalGpsData);
                    wLoader.loadWeatherData(new WeatherRequest.Builder()
                            .forceRefresh(false)
                            .setErrorListener(LocationsFragment.this)
                            .build())
                            .addOnSuccessListener(new OnSuccessListener<Weather>() {
                                @Override
                                public void onSuccess(final Weather weather) {
                                    onWeatherLoaded(finalGpsData, weather);
                                }
                            });
                }
            }
        });
    }

    private LocationData getGPSPanel() {
        return AsyncTask.await(new Callable<LocationData>() {
            @Override
            public LocationData call() {
                // Setup gps panel
                if (getAppCompatActivity() != null && Settings.useFollowGPS()) {
                    LocationData locData = Settings.getLastGPSLocData();

                    if (locData == null || locData.getQuery() == null) {
                        locData = updateLocation();
                    }

                    if (locData != null && locData.getQuery() != null) {
                        return locData;
                    }
                }
                return null;
            }
        });
    }

    private void refreshLocations() {
        runWithView(new LifecycleRunnable(getViewLifecycleOwner().getLifecycle()) {
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
                        (mAdapter.getFavoritesCount() > 0 && mAdapter.getFirstFavPanel() != null && !Settings.getAPI().equals(mAdapter.getFirstFavPanel().getWeatherSource())))
                    reload = true;

                if (Settings.useFollowGPS()) {
                    if (!reload && (gpsPanelViewModel != null && !ObjectsCompat.equals(locations.get(0).getQuery(), gpsPanelViewModel.getLocationData().getQuery())))
                        reload = true;
                }

                if (!isActive() || !isViewAlive()) return;

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
                                .addOnSuccessListener(new OnSuccessListener<Weather>() {
                                    @Override
                                    public void onSuccess(final Weather weather) {
                                        onWeatherLoaded(view.getLocationData(), weather);
                                    }
                                });
                    }
                }
            }
        });
    }

    private void addGPSPanel() {
        runWithView(new LifecycleRunnable(getViewLifecycleOwner().getLifecycle()) {
            @Override
            public void run() {
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

                if (!isActive() || !isViewAlive()) return;

                if (gpsData != null) {
                    WeatherDataLoader wLoader = new WeatherDataLoader(gpsData);
                    wLoader.loadWeatherData(new WeatherRequest.Builder()
                            .forceRefresh(false)
                            .setErrorListener(LocationsFragment.this)
                            .build())
                            .addOnSuccessListener(new OnSuccessListener<Weather>() {
                                @Override
                                public void onSuccess(final Weather weather) {
                                    onWeatherLoaded(gpsData, weather);
                                }
                            });
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
        return AsyncTask.await(new Callable<LocationData>() {
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

                    LocationManager locMan = null;
                    if (getAppCompatActivity() != null)
                        locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        return null;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = AsyncTask.await(new Callable<Location>() {
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
                            runWithView(new Runnable() {
                                @Override
                                public void run() {
                                    showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null);
                                    removeGPSPanel();
                                }
                            });
                        }
                    }

                    if (location != null && !mRequestingLocationUpdates) {
                        LocationQueryViewModel view;

                        try {
                            view = wm.getLocation(location);
                        } catch (WeatherException e) {
                            Logger.writeLine(Log.ERROR, e);
                            // Stop since there is no valid query
                            runWithView(new Runnable() {
                                @Override
                                public void run() {
                                    removeGPSPanel();
                                }
                            });
                            return null;
                        }

                        if (StringUtils.isNullOrEmpty(view.getLocationQuery())) {
                            view = new LocationQueryViewModel();
                        } else if (StringUtils.isNullOrWhitespace(view.getLocationTZLong()) && view.getLocationLat() != 0 && view.getLocationLong() != 0) {
                            String tzId = TZDBCache.getTimeZone(view.getLocationLat(), view.getLocationLong());
                            if (!"unknown".equals(tzId))
                                view.setLocationTZLong(tzId);
                        }

                        if (StringUtils.isNullOrWhitespace(view.getLocationQuery())) {
                            // Stop since there is no valid query
                            runWithView(new Runnable() {
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

                        Log.d("LocationsFragment", "Location changed; sending update");
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
            runWithView(new Runnable() {
                @Override
                public void run() {
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

    private OnListChangedListener<LocationPanelViewModel> onSelectionChangedListener = new OnListChangedListener<LocationPanelViewModel>() {
        @Override
        public void onChanged(final ArrayList<LocationPanelViewModel> sender, ListChangedArgs<LocationPanelViewModel> args) {
            runWithView(new Runnable() {
                @Override
                public void run() {
                    if (actionMode != null) {
                        actionMode.setTitle(!sender.isEmpty() ? Integer.toString(sender.size()) : "");

                        MenuItem deleteBtnItem = actionMode.getMenu().findItem(R.id.action_delete);
                        if (deleteBtnItem != null) {
                            deleteBtnItem.setVisible(!sender.isEmpty());
                        }
                    }
                }
            });
        }
    };

    private RecyclerOnClickListenerInterface onRecyclerLongClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(View view, int position) {
            if (mAdapter.getItemViewType(position) == LocationPanelAdapter.ItemType.SEARCH_PANEL) {
                if (!mEditMode && mAdapter.getFavoritesCount() > 1) {
                    actionMode = getAppCompatActivity().startSupportActionMode(actionModeCallback);

                    LocationPanelViewModel model = mAdapter.getPanelViewModel(position);
                    if (model != null) {
                        model.setChecked(true);
                        mAdapter.notifyItemChanged(position);
                    }
                }
            }
        }
    };

    private void toggleEditMode() {
        // Toggle EditMode
        mEditMode = !mEditMode;
        onBackPressedCallback.setEnabled(mEditMode);
        mAdapter.setInEditMode(mEditMode);

        // Set Drag & Swipe ability
        mITHCallback.setItemViewSwipeEnabled(mEditMode);

        if (mEditMode) {
            // Unregister events
            mAdapter.setOnClickListener(null);
            mAdapter.setOnLongClickListener(null);
        } else {
            // Register events
            mAdapter.setOnClickListener(onRecyclerClickListener);
            mAdapter.setOnLongClickListener(onRecyclerLongClickListener);
            mAdapter.clearSelection();
            if (actionMode != null) actionMode.finish();
        }

        for (final LocationPanelViewModel view : mAdapter.getDataset()) {
            view.setEditMode(mEditMode);
            if (!mEditMode) view.setChecked(false);
            binding.recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    if (isViewAlive()) {
                        mAdapter.notifyItemChanged(mAdapter.getViewPosition(view));
                    }
                }
            });

            if (view.getLocationType() != LocationType.GPS.getValue() && !mEditMode && (mDataChanged || mHomeChanged)) {
                updateFavoritesPosition(view);
            }
        }

        if (!mEditMode && mHomeChanged) {
            Log.d("LocationsFragment", "Home changed; sending update");
            LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                    .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
            LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                    .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE));
        }

        mDataChanged = false;
        mHomeChanged = false;
    }

    private void updateFavoritesPosition(@NonNull LocationPanelViewModel view) {
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