package com.thewizrd.simpleweather.main;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.ObjectsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.ActivityMainBinding;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.preferences.SettingsFragment;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
        SystemBarColorManager, UserThemeMode.OnThemeChangeListener {

    private ActivityMainBinding binding;
    private NavController mNavController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("MainActivity: onCreate");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            binding.getRoot().setFitsSystemWindows(true);

        binding.bottomNavBar.setOnNavigationItemSelectedListener(this);

        // Back stack listener
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                refreshNavViewCheckedItem();
            }
        });

        updateWindowColors();

        Bundle args = new Bundle();
        if (getIntent() != null && getIntent().getExtras() != null) {
            args.putAll(getIntent().getExtras());
        }

        // Shortcut intent: from app shortcuts
        if (args.containsKey(Constants.KEY_SHORTCUTDATA)) {
            String data = args.getString(Constants.KEY_SHORTCUTDATA);
            args.remove(Constants.KEY_SHORTCUTDATA);
            args.putString(Constants.KEY_DATA, data);
        }

        if (args.containsKey(Constants.KEY_DATA)) {
            if (!args.containsKey(Constants.FRAGTAG_HOME)) {
                LocationData locData = JSONParser.deserializer(
                        args.getString(Constants.KEY_DATA), LocationData.class);

                args.putBoolean(Constants.FRAGTAG_HOME, ObjectsCompat.equals(locData, Settings.getHomeData()));
            }
        }

        NavHostFragment hostFragment = NavHostFragment.create(R.navigation.nav_graph, args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, hostFragment)
                .setPrimaryNavigationFragment(hostFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNavController = Navigation.findNavController(this, R.id.fragment_container);

        NavigationUI.setupWithNavController(binding.bottomNavBar, mNavController);
        mNavController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                refreshNavViewCheckedItem();

                binding.bottomNavBar.setVisibility(destination.getId() == R.id.locationSearchFragment ? View.GONE : View.VISIBLE);
            }
        });

        // Alerts: from weather alert notification
        if (getIntent() != null && WeatherWidgetService.ACTION_SHOWALERTS.equals(getIntent().getAction())) {
            LocationData locationData = Settings.getHomeData();
            WeatherNowFragmentDirections.ActionWeatherNowFragmentToWeatherListFragment args =
                    WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                            .setData(JSONParser.serializer(locationData, LocationData.class))
                            .setWeatherListType(WeatherListType.ALERTS);
            mNavController.navigate(args);
        }

        // Check nav item in bottom nav view
        // based on current fragment
        refreshNavViewCheckedItem();

        // Update app shortcuts
        ShortcutCreatorWorker.requestUpdateShortcuts(this);
    }

    @Override
    public void onBackPressed() {
        Fragment current = null;
        if (getSupportFragmentManager().getPrimaryNavigationFragment() != null) {
            current = getSupportFragmentManager().getPrimaryNavigationFragment().getChildFragmentManager().getPrimaryNavigationFragment();
        }
        OnBackPressedFragmentListener fragBackPressedListener = null;
        if (current instanceof OnBackPressedFragmentListener)
            fragBackPressedListener = (OnBackPressedFragmentListener) current;

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            // Go back to WeatherNow if we started from an alert notification
            if (current instanceof WeatherListFragment &&
                    getSupportFragmentManager().getBackStackEntryCount() == 0) {
                Bundle args = new Bundle();
                args.putBoolean(Constants.FRAGTAG_HOME, true);
                mNavController.navigate(R.id.weatherNowFragment, args,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.weatherNowFragment, true)
                                .build());
                return;
            }

            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        final int currentId = mNavController.getCurrentDestination().getId();

        if (id != currentId) {
            NavigationUI.onNavDestinationSelected(item, mNavController);
        }

        return true;
    }

    protected void onResumeFragments() {
        super.onResumeFragments();
        refreshNavViewCheckedItem();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("MainActivity: onResume");
        View container = binding.fragmentContainer.findViewById(R.id.radar_webview_container);
        if (container instanceof ViewGroup && ((ViewGroup) container).getChildAt(0) instanceof WebView) {
            WebView webView = (WebView) ((ViewGroup) container).getChildAt(0);
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AnalyticsLogger.logEvent("MainActivity: onPause");
        View container = binding.fragmentContainer.findViewById(R.id.radar_webview_container);
        if (container instanceof ViewGroup && ((ViewGroup) container).getChildAt(0) instanceof WebView) {
            WebView webView = (WebView) ((ViewGroup) container).getChildAt(0);
            webView.pauseTimers();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    private void refreshNavViewCheckedItem() {
        final int currentId = mNavController.getCurrentDestination().getId();
        final String currentName;
        if (mNavController.getCurrentDestination() instanceof FragmentNavigator.Destination) {
            currentName = ((FragmentNavigator.Destination) mNavController.getCurrentDestination()).getClassName();
        } else {
            currentName = mNavController.getCurrentDestination().getDisplayName();
        }
        int checkedItemId = -1;

        if (currentId == R.id.weatherNowFragment || currentId == R.id.weatherListFragment) {
            checkedItemId = R.id.weatherNowFragment;
        } else if (currentId == R.id.weatherRadarFragment) {
            checkedItemId = R.id.weatherRadarFragment;
        } else if (currentId == R.id.locationsFragment) {
            checkedItemId = R.id.locationsFragment;
        } else if (currentName.contains(SettingsFragment.class.getName())) {
            checkedItemId = R.id.settingsFragment;
        }

        MenuItem item = binding.bottomNavBar.getMenu().findItem(checkedItemId);
        if (item != null) {
            item.setChecked(true);
        }
    }

    @Override
    public void setSystemBarColors(@ColorInt final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;

                Configuration config = getResources().getConfiguration();
                final boolean isLandscapeMode = config.orientation != Configuration.ORIENTATION_PORTRAIT && !ActivityUtils.isLargeTablet(MainActivity.this);

                // Actionbar, BottomNavBar & StatusBar
                ActivityUtils.setTransparentWindow(getWindow(), color, Colors.TRANSPARENT, isLandscapeMode ? color : Colors.TRANSPARENT, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                binding.getRoot().setBackgroundColor(color);
                binding.bottomNavBar.setBackgroundColor(color);
            }
        });
    }

    private void updateWindowColors() {
        int color = ActivityUtils.getColor(this, android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        getWindow().getDecorView().setBackgroundColor(color);
        setSystemBarColors(color);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateWindowColors();
    }

    @Override
    public void onThemeChanged(UserThemeMode mode) {
        int color = ActivityUtils.getColor(MainActivity.this, android.R.attr.colorBackground);
        if (mode == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        binding.getRoot().setBackgroundColor(color);
    }
}
