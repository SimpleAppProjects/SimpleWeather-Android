package com.thewizrd.simpleweather.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.wearable.input.RotaryEncoder;
import android.support.wearable.view.ConfirmationOverlay;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.databinding.Observable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.paging.PagedList;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.widget.drawer.WearableDrawerLayout;
import androidx.wear.widget.drawer.WearableDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.google.android.wearable.intent.RemoteIntent;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.BR;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastsViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.ActivityMainBinding;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;
import com.thewizrd.simpleweather.preferences.SettingsActivity;
import com.thewizrd.simpleweather.setup.SetupActivity;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class MainActivity extends FragmentActivity implements MenuItem.OnMenuItemClickListener,
        WearableNavigationDrawerView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private NavDrawerAdapter mNavDrawerAdapter;
    private BroadcastReceiver mBroadcastReceiver;

    private int mNavViewSelectedIdx = 0;
    private Runnable mItemSelectedRunnable;

    private WeatherNowViewModel weatherNowView;
    private ForecastsViewModel forecastsView;
    private WeatherAlertsViewModel alertsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("MainActivity: onCreate");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WearableDrawerLayout mWearableDrawerLayout = binding.activityMain;

        mWearableDrawerLayout.setDrawerStateCallback(new WearableDrawerLayout.DrawerStateCallback() {
            @Override
            public void onDrawerOpened(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                super.onDrawerOpened(layout, drawerView);
                drawerView.requestFocus();
            }

            @Override
            public void onDrawerClosed(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                super.onDrawerClosed(layout, drawerView);
                drawerView.clearFocus();
            }

            @Override
            public void onDrawerStateChanged(WearableDrawerLayout layout, int newState) {
                super.onDrawerStateChanged(layout, newState);

                if (newState != WearableDrawerView.STATE_IDLE && mItemSelectedRunnable != null) {
                    mItemSelectedRunnable.run();
                    mItemSelectedRunnable = null;
                }
            }
        });

        binding.bottomActionDrawer.setOnMenuItemClickListener(this);
        binding.bottomActionDrawer.setPeekOnScrollDownEnabled(true);

        binding.topNavDrawer.addOnItemSelectedListener(this);
        binding.topNavDrawer.setPeekOnScrollDownEnabled(true);
        binding.topNavDrawer.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            final ViewPager pager = binding.topNavDrawer.findViewById(R.id.ws_navigation_drawer_view_pager);
            final CountDownTimer timer = new CountDownTimer(200, 200) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    if (pager != null && pager.isFakeDragging()) {
                        pager.endFakeDrag();
                    }
                    xTotalOffset = 0;
                }
            };
            float xTotalOffset = 0;

            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (pager != null && event.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {
                    timer.cancel();
                    // Send event to postpone auto close of drawer
                    binding.topNavDrawer.onInterceptTouchEvent(event);

                    // Don't forget the negation here
                    float delta = RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(MainActivity.this);
                    if (Math.signum(delta) != Math.signum(xTotalOffset)) {
                        timer.onFinish();
                        xTotalOffset = delta * 1.5f;
                    } else {
                        xTotalOffset += delta * 1.5f;
                    }

                    if (!pager.isFakeDragging())
                        pager.beginFakeDrag();

                    pager.fakeDragBy(xTotalOffset);
                    if (Math.abs(xTotalOffset) >= pager.getMeasuredWidth()) {
                        timer.onFinish();
                    } else {
                        timer.start();
                    }

                    return true;
                }

                return false;
            }
        });
        mNavDrawerAdapter = new NavDrawerAdapter(this);
        binding.topNavDrawer.setAdapter(mNavDrawerAdapter);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WearableDataListenerService.ACTION_SHOWSTORELISTING.equals(intent.getAction())) {
                    Intent intentAndroid = new Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(WearableHelper.getPlayStoreURI());

                    RemoteIntent.startRemoteActivity(MainActivity.this, intentAndroid,
                            new ConfirmationResultReceiver(MainActivity.this));
                } else if (WearableDataListenerService.ACTION_OPENONPHONE.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(WearableDataListenerService.EXTRA_SUCCESS, false);

                    new ConfirmationOverlay()
                            .setType(success ? ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION : ConfirmationOverlay.FAILURE_ANIMATION)
                            .showOn(MainActivity.this);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WearableDataListenerService.ACTION_SHOWSTORELISTING);
        filter.addAction(WearableDataListenerService.ACTION_OPENONPHONE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        // Create your application here
        final ViewModelProvider vmProvider = new ViewModelProvider(this);
        this.weatherNowView = vmProvider.get(WeatherNowViewModel.class);
        this.forecastsView = vmProvider.get(ForecastsViewModel.class);
        this.alertsView = vmProvider.get(WeatherAlertsViewModel.class);
        weatherNowView.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, final int propertyId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (binding == null) return;
                        if (propertyId == BR.pendingBackground) {
                            binding.bottomActionDrawer.setBackgroundColor(weatherNowView.getPendingBackground());
                            binding.topNavDrawer.setBackgroundColor(weatherNowView.getPendingBackground());
                        }
                    }
                });
            }
        });
        forecastsView.getForecasts().observe(this, new Observer<PagedList<ForecastItemViewModel>>() {
            @Override
            public void onChanged(PagedList<ForecastItemViewModel> forecastItemViewModels) {
                mNavDrawerAdapter.updateNavDrawerItems();
            }
        });
        forecastsView.getHourlyForecasts().observe(this, new Observer<PagedList<HourlyForecastItemViewModel>>() {
            @Override
            public void onChanged(PagedList<HourlyForecastItemViewModel> forecastItemViewModels) {
                mNavDrawerAdapter.updateNavDrawerItems();
            }
        });
        alertsView.getAlerts().observe(this, new Observer<List<WeatherAlertViewModel>>() {
            @Override
            public void onChanged(List<WeatherAlertViewModel> alertViewModels) {
                mNavDrawerAdapter.updateNavDrawerItems();
            }
        });
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Check if fragment exists
        if (fragment == null) {
            fragment = new WeatherNowFragment();

            // Navigate to WeatherNowFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, Constants.FRAGTAG_HOME)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // Destroy untagged fragments onbackpressed
        if (current != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(current)
                    .commit();

            // Reset to home
            int drawerState = binding.topNavDrawer.getDrawerState();
            binding.topNavDrawer.setCurrentItem(0, false);
            if (mItemSelectedRunnable != null && drawerState == WearableNavigationDrawerView.STATE_IDLE) {
                mItemSelectedRunnable.run();
            }
        }

        // If backstack entry exists pop all and goto first (home) fragment
        if (getSupportFragmentManager().getBackStackEntryCount() >= 1) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else { // Otherwise fallback
            super.onBackPressed();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_changelocation:
                startActivity(new Intent(this, SetupActivity.class));
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.menu_openonphone:
                startService(new Intent(this, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_OPENONPHONE));
                break;
        }

        return true;
    }

    public void onItemSelected(final int position) {
        mNavViewSelectedIdx = position;

        mItemSelectedRunnable = new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) return;

                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                Class targetFragmentType = null;
                WeatherListType weatherListType = WeatherListType.valueOf(0);

                if (mNavDrawerAdapter != null) {
                    switch (mNavDrawerAdapter.getStringId(position)) {
                        case R.string.label_condition:
                        default:
                            targetFragmentType = WeatherNowFragment.class;
                            break;
                        case R.string.title_fragment_alerts:
                            targetFragmentType = WeatherListFragment.class;
                            weatherListType = WeatherListType.ALERTS;
                            break;
                        case R.string.label_forecast:
                            targetFragmentType = WeatherListFragment.class;
                            weatherListType = WeatherListType.FORECAST;
                            break;
                        case R.string.label_hourlyforecast:
                            targetFragmentType = WeatherListFragment.class;
                            weatherListType = WeatherListType.HOURLYFORECAST;
                            break;
                        case R.string.label_details:
                            targetFragmentType = WeatherDetailsFragment.class;
                            break;
                    }
                }

                if (current == null) return;

                if (WeatherNowFragment.class.equals(targetFragmentType)) {
                    if (!WeatherNowFragment.class.equals(current.getClass())) {
                        // Pop all since we're going home
                        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                } else if (WeatherListFragment.class.equals(targetFragmentType)) {
                    if (!targetFragmentType.equals(current.getClass())) {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        int backstackCount = getSupportFragmentManager().getBackStackEntryCount();

                        /*
                         * NOTE
                         * Destroy lingering frag and commit transaction
                         * This is to avoid adding the fragment again from the backstack
                         */
                        if (backstackCount > 0) {
                            getSupportFragmentManager().beginTransaction()
                                    .remove(current)
                                    .commitAllowingStateLoss();
                        } else {
                            // Hide home frag
                            ft.hide(current);
                        }

                        // Add fragment to backstack
                        ft.add(R.id.fragment_container,
                                WeatherListFragment.newInstance(weatherListType),
                                null)
                                .addToBackStack(null);

                        ft.commit();
                    } else if (current instanceof WeatherListFragment) {
                        WeatherListFragment forecastFragment = (WeatherListFragment) current;
                        if (forecastFragment.getArguments() != null) {
                            Bundle args = forecastFragment.getArguments();
                            if (WeatherListType.valueOf(args.getInt(Constants.ARGS_WEATHERLISTTYPE, 0)) != weatherListType) {
                                args.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherListType.getValue());
                                // Note: Causes IllegalStateException if args already set (not null)
                                // forecastFragment.setArguments(args);
                                forecastFragment.initialize();
                            }
                        }
                    }
                } else if (WeatherDetailsFragment.class.equals(targetFragmentType)) {
                    if (!WeatherDetailsFragment.class.equals(current.getClass())) {
                        // Add fragment to backstack
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.add(R.id.fragment_container, WeatherDetailsFragment.newInstance(), null)
                                .addToBackStack(null);

                        /*
                         * NOTE
                         * Destroy lingering frag and commit transaction
                         * This is to avoid adding the fragment again from the backstack
                         */
                        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getSupportFragmentManager().beginTransaction()
                                    .remove(current)
                                    .commitAllowingStateLoss();
                        }

                        ft.commit();
                    }
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("MainActivity: onResume");

        if (binding != null) {
            MenuItem menuItem = null;

            if (binding.bottomActionDrawer.getMenu() != null) {
                menuItem = binding.bottomActionDrawer.getMenu().findItem(R.id.menu_changelocation);
            }

            if (Settings.getDataSync() != WearableDataSync.OFF && menuItem != null) {
                // remove change location if exists
                binding.bottomActionDrawer.getMenu().removeItem(R.id.menu_changelocation);
            } else if (Settings.getDataSync() == WearableDataSync.OFF && menuItem == null) {
                // restore all menu options
                binding.bottomActionDrawer.getMenu().clear();
                getMenuInflater().inflate(R.menu.main_botton_drawer_menu, binding.bottomActionDrawer.getMenu());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AnalyticsLogger.logEvent("MainActivity: onPause");
    }

    private class NavDrawerAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
        private Context mContext;
        private final List<NavDrawerItem> navDrawerItems = Arrays.asList(
                new NavDrawerItem(R.string.label_condition, R.drawable.day_cloudy),
                new NavDrawerItem(R.string.title_fragment_alerts, R.drawable.ic_error_white),
                new NavDrawerItem(R.string.label_forecast, R.drawable.ic_date_range_black_24dp),
                new NavDrawerItem(R.string.label_hourlyforecast, R.drawable.ic_access_time_black_24dp),
                new NavDrawerItem(R.string.label_details, R.drawable.ic_list_black_24dp)
        );
        private List<NavDrawerItem> navItems;

        public NavDrawerAdapter(Context context) {
            mContext = context;
            navItems = navDrawerItems;
        }

        @Override
        public int getCount() {
            return navItems.size();
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            Drawable drawable = mContext.getDrawable(navItems.get(pos).drawableIcon);
            drawable.setTint(mContext.getColor(android.R.color.white));
            return drawable;
        }

        @Override
        public CharSequence getItemText(int pos) {
            return mContext.getString(navItems.get(pos).titleString);
        }

        public int getStringId(int pos) {
            return navItems.get(pos).titleString;
        }

        public void updateNavDrawerItems() {
            navItems = new AsyncTask<List<NavDrawerItem>>().await(new Callable<List<NavDrawerItem>>() {
                @Override
                public List<NavDrawerItem> call() {
                    List<NavDrawerItem> items = new ArrayList<>(navDrawerItems);
                    for (NavDrawerItem item : navDrawerItems) {
                        if (item.titleString == R.string.title_fragment_alerts &&
                                (alertsView.getAlerts().getValue() == null || alertsView.getAlerts().getValue().size() == 0)) {
                            items.remove(item);
                        }
                        if (item.titleString == R.string.label_forecast &&
                                (forecastsView.getForecasts().getValue() == null || forecastsView.getForecasts().getValue().size() == 0)) {
                            items.remove(item);
                        }
                        if (item.titleString == R.string.label_hourlyforecast &&
                                (forecastsView.getHourlyForecasts().getValue() == null || forecastsView.getHourlyForecasts().getValue().size() == 0)) {
                            items.remove(item);
                        }
                    }

                    return items;
                }
            });
            notifyDataSetChanged();
        }
    }

    private static class NavDrawerItem {
        private int titleString;
        private int drawableIcon;

        public NavDrawerItem(int titleString, int drawableIcon) {
            this.titleString = titleString;
            this.drawableIcon = drawableIcon;
        }
    }
}
