package com.thewizrd.simpleweather.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.ConfirmationOverlay;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.widget.drawer.WearableActionDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.google.android.wearable.intent.RemoteIntent;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.WeatherViewLoadedListener;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;
import com.thewizrd.simpleweather.preferences.SettingsActivity;
import com.thewizrd.simpleweather.setup.SetupActivity;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class MainActivity extends FragmentActivity implements MenuItem.OnMenuItemClickListener,
        WearableNavigationDrawerView.OnItemSelectedListener,
        WeatherViewLoadedListener {

    private WearableNavigationDrawerView mWearableNavigationDrawer;
    private WearableActionDrawerView mWearableActionDrawer;
    private NavDrawerAdapter mNavDrawerAdapter;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWearableActionDrawer = findViewById(R.id.bottom_action_drawer);
        mWearableActionDrawer.setOnMenuItemClickListener(this);
        mWearableActionDrawer.setPeekOnScrollDownEnabled(true);

        mWearableNavigationDrawer = findViewById(R.id.top_nav_drawer);
        mWearableNavigationDrawer.addOnItemSelectedListener(this);
        mWearableNavigationDrawer.setPeekOnScrollDownEnabled(true);
        mNavDrawerAdapter = new NavDrawerAdapter(this);
        mWearableNavigationDrawer.setAdapter(mNavDrawerAdapter);

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
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Check if fragment exists
        if (fragment == null) {
            fragment = new WeatherNowFragment();

            // Navigate to WeatherNowFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, "home")
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
            current.onDestroy();
            current = null;

            // Reset to home
            mWearableNavigationDrawer.setCurrentItem(0, false);
        } else
            super.onBackPressed();
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

    public void onItemSelected(int position) {
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

        if (WeatherNowFragment.class.equals(targetFragmentType)) {
            if (!WeatherNowFragment.class.equals(current.getClass())) {
                // Pop all since we're going home
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        } else if (WeatherListFragment.class.equals(targetFragmentType)) {
            if (!targetFragmentType.equals(current.getClass())) {
                // Add fragment to backstack
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.fragment_container,
                        WeatherListFragment.newInstance(weatherListType, mNavDrawerAdapter.weatherNowView),
                        null)
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
            } else if (current instanceof WeatherListFragment) {
                WeatherListFragment forecastFragment = (WeatherListFragment) current;
                if (forecastFragment.getArguments() != null) {
                    Bundle args = forecastFragment.getArguments();
                    if (WeatherListType.valueOf(args.getInt("WeatherListType", 0)) != weatherListType) {
                        args.putInt("WeatherListType", weatherListType.getValue());
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
                ft.add(R.id.fragment_container, WeatherDetailsFragment.newInstance(mNavDrawerAdapter.weatherNowView), null)
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

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onWeatherViewUpdated(final WeatherNowViewModel weatherNowView) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNavDrawerAdapter.updateNavDrawerItems(weatherNowView);
                mWearableActionDrawer.setBackgroundColor(weatherNowView.getPendingBackground());
                mWearableNavigationDrawer.setBackgroundColor(weatherNowView.getPendingBackground());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mWearableActionDrawer != null) {
            MenuItem menuItem = null;

            if (mWearableActionDrawer.getMenu() != null) {
                menuItem = mWearableActionDrawer.getMenu().findItem(R.id.menu_changelocation);
            }

            if (Settings.getDataSync() != WearableDataSync.OFF && menuItem != null) {
                // remove change location if exists
                mWearableActionDrawer.getMenu().removeItem(R.id.menu_changelocation);
            } else if (Settings.getDataSync() == WearableDataSync.OFF && menuItem == null) {
                // restore all menu options
                mWearableActionDrawer.getMenu().clear();
                getMenuInflater().inflate(R.menu.main_botton_drawer_menu, mWearableActionDrawer.getMenu());
            }
        }
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
        private WeatherNowViewModel weatherNowView;

        public NavDrawerAdapter(Context context) {
            mContext = context;
            navItems = navDrawerItems;
        }

        public WeatherNowViewModel getWeatherNowView() {
            return weatherNowView;
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

        public void updateNavDrawerItems(final WeatherNowViewModel weatherNowView) {
            this.weatherNowView = weatherNowView;

            navItems = new AsyncTask<List<NavDrawerItem>>().await(new Callable<List<NavDrawerItem>>() {
                @Override
                public List<NavDrawerItem> call() throws Exception {
                    List<NavDrawerItem> items = new ArrayList<>(navDrawerItems);
                    if (weatherNowView.getExtras().getAlerts().size() == 0) {
                        List<NavDrawerItem> tmp = new ArrayList<>();
                        for (NavDrawerItem item : items) {
                            if (item.titleString != R.string.title_fragment_alerts)
                                tmp.add(item);
                        }
                        items = tmp;
                    }
                    if (weatherNowView.getExtras().getHourlyForecast().size() == 0) {
                        List<NavDrawerItem> tmp = new ArrayList<>();
                        for (NavDrawerItem item : items) {
                            if (item.titleString != R.string.label_hourlyforecast)
                                tmp.add(item);
                        }
                        items = tmp;
                    }

                    return items;
                }
            });
            notifyDataSetChanged();
        }
    }

    private class NavDrawerItem {
        private int titleString;
        private int drawableIcon;

        public NavDrawerItem(int titleString, int drawableIcon) {
            this.titleString = titleString;
            this.drawableIcon = drawableIcon;
        }
    }
}
