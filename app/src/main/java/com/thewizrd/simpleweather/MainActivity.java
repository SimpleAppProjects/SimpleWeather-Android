package com.thewizrd.simpleweather;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.helpers.WeatherViewLoadedListener;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DarkMode;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.preferences.SettingsFragment;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.io.StringReader;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
        WeatherViewLoadedListener, SystemBarColorManager, DarkMode.OnThemeChangeListener {

    private BottomNavigationView mBottomNavView;
    private View mFragmentContainer;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make full transparent statusBar
        ActivityUtils.setTransparentWindow(getWindow(), Colors.SIMPLEBLUE, Colors.TRANSPARENT, Colors.TRANSPARENT);

        setContentView(R.layout.activity_main);

        mFragmentContainer = findViewById(R.id.fragment_container);
        mRootView = (View) mFragmentContainer.getParent();
        updateWindowColors();

        mBottomNavView = findViewById(R.id.bottom_nav_bar);
        mBottomNavView.setOnNavigationItemSelectedListener(this);

        // Back stack listener
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                refreshNavViewCheckedItem();
            }
        });

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Alerts: from weather alert notification
        if (getIntent() != null && WeatherWidgetService.ACTION_SHOWALERTS.equals(getIntent().getAction())) {
            Fragment newFragment = WeatherNowFragment.newInstance(getIntent().getExtras());

            if (fragment == null) {
                fragment = newFragment;
                // Navigate to WeatherNowFragment
                // Make sure we exit if location is not home
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment, "notification")
                        .commitNow();
            } else {
                // Navigate to WeatherNowFragment
                // Make sure we exit if location is not home
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, newFragment)
                        .addToBackStack(null) // allow exit
                        .commitNow();
            }
        }
        // Check if fragment exists
        if (fragment == null) {
            if (getIntent() != null && getIntent().hasExtra("data"))
                fragment = WeatherNowFragment.newInstance(getIntent().getExtras());
            else
                fragment = new WeatherNowFragment();

            // Navigate to WeatherNowFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, "home")
                    .commitNow();
        }

        // Shortcut intent: from app shortcuts
        if (getIntent() != null && getIntent().hasExtra("shortcut-data")) {
            JsonReader reader = new JsonReader(
                    new StringReader(getIntent().getStringExtra("shortcut-data")));
            LocationData locData = LocationData.fromJson(reader);

            // Navigate to WeatherNowFragment
            Fragment newFragment = WeatherNowFragment.newInstance(locData);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newFragment, "shortcut")
                    .commitNow();
        }

        // Check nav item in bottom nav view
        // based on current fragment
        refreshNavViewCheckedItem();

        // Update app shortcuts
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                ShortcutCreator.updateShortcuts();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        OnBackPressedFragmentListener fragBackPressedListener = null;
        if (current instanceof OnBackPressedFragmentListener)
            fragBackPressedListener = (OnBackPressedFragmentListener) current;

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            // Destroy untagged fragments onbackpressed
            if (current != null && current.getTag() == null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(current)
                        .commit();
            }

            // If sub-fragment exist: pop those one by one
            int backstackCount = getSupportFragmentManager().getBackStackEntryCount();
            if (current != null
                    && (current.getClass().getName().contains("SettingsFragment$")
                    || current instanceof WeatherAlertsFragment || current instanceof WeatherDetailsFragment)) {
                getSupportFragmentManager().popBackStack();
            } else if (backstackCount >= 1) { // If backstack entry exists pop all and goto first (home) fragment
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else { // Otherwise fallback
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        Fragment fragment = null;

        if (id == R.id.nav_weathernow) {
            fragment = new WeatherNowFragment();
        } else if (id == R.id.nav_locations) {
            fragment = new LocationsFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        }

        // Make sure we're not navigating to the same type of fragment
        if (fragment != null && current != null && fragment.getClass() != current.getClass()) {
            if (fragment instanceof WeatherNowFragment) {
                // Pop all since we're going home
                fragment = null;
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else if (fragment instanceof LocationsFragment) {
                /* NOTE: KEEP THIS IT WORKS FINE */
                if (current instanceof WeatherNowFragment) {
                    // Hide home frag
                    if ("home".equals(current.getTag()))
                        transaction.hide(current);
                    else {
                        // Destroy lingering WNow frag
                        transaction.remove(current);
                        getSupportFragmentManager().popBackStack();
                    }
                    /* NOTE: KEEP ABOVE */
                } else {
                    // If current frag is Settings sub-fragment pop all off
                    if (current.getClass().getName().contains("SettingsFragment$"))
                        getSupportFragmentManager().popBackStack("settings", 0);
                    /* NOTE: Don't pop here so we don't trigger onResume/onHiddenChanged of root fragment */
                    // If a Settings fragment exists remove it
                    if (getSupportFragmentManager().findFragmentByTag("settings") != null) {
                        current = getSupportFragmentManager().findFragmentByTag("settings");
                        transaction.remove(current);
                    }
                    // If an extra WeatherNowFragment exists remove it
                    if (getSupportFragmentManager().findFragmentByTag("favorites") != null) {
                        current = getSupportFragmentManager().findFragmentByTag("favorites");
                        transaction.remove(current);
                    }
                    // If current frag is Weather(Alerts||Details)Fragment remove it
                    if (current instanceof WeatherAlertsFragment || current instanceof WeatherDetailsFragment) {
                        transaction.remove(current);
                    }
                }

                if (getSupportFragmentManager().findFragmentByTag("locations") != null) {
                    fragment = getSupportFragmentManager().findFragmentByTag("locations");
                    // Show LocationsFragment if it exists
                    transaction
                            .show(fragment)
                            .addToBackStack(null);
                } else {
                    // Add LocFrag if not in backstack/DNE
                    transaction
                            .add(R.id.fragment_container, fragment, "locations")
                            .addToBackStack(null);
                }

                // Commit the transaction
                transaction.commit();
            } else if (fragment instanceof SettingsFragment) {
                // Commit the transaction if current frag is not a SettingsFragment sub-fragment
                if (!current.getClass().getName().contains("SettingsFragment")) {
                    transaction
                            .add(R.id.fragment_container, fragment, "settings")
                            .hide(current)
                            .addToBackStack("settings")
                            .commit();
                }
            }
        }

        return true;
    }

    protected void onResumeFragments() {
        super.onResumeFragments();
        refreshNavViewCheckedItem();
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

    private void refreshNavViewCheckedItem() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        int checkedItemId = -1;

        if (fragment instanceof WeatherNowFragment) {
            checkedItemId = R.id.nav_weathernow;
        } else if (fragment instanceof LocationsFragment) {
            checkedItemId = R.id.nav_locations;
        } else if (fragment instanceof WeatherAlertsFragment || fragment instanceof WeatherDetailsFragment) {
            checkedItemId = R.id.nav_weathernow;
        } else if (fragment != null && fragment.getClass().getName().contains("SettingsFragment")) {
            checkedItemId = R.id.nav_settings;
        }

        MenuItem item = mBottomNavView.getMenu().findItem(checkedItemId);
        if (item != null) {
            item.setChecked(true);
        }
    }

    @Override
    public void onWeatherViewUpdated(final WeatherNowViewModel weatherNowView) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Actionbar, BottonNavBar & StatusBar
                int currentNightMode = AppCompatDelegate.getDefaultNightMode();
                if (currentNightMode != AppCompatDelegate.MODE_NIGHT_YES)
                    setSystemBarColors(Colors.TRANSPARENT, weatherNowView.getPendingBackground(), Colors.TRANSPARENT);
                else {
                    int color = Settings.getUserThemeMode() == DarkMode.AMOLED_DARK ?
                            Colors.BLACK :
                            ActivityUtils.getColor(MainActivity.this, android.R.attr.colorBackground);
                    setSystemBarColors(Colors.TRANSPARENT, color, Colors.TRANSPARENT);
                }
            }
        });
    }

    @Override
    public void setSystemBarColors(@ColorInt final int color) {
        setSystemBarColors(color, color, color, color);
    }

    @Override
    public void setSystemBarColors(@ColorInt final int statusBarColor, @ColorInt final int navBarColor) {
        setSystemBarColors(statusBarColor, navBarColor, navBarColor);
    }

    @Override
    public void setSystemBarColors(@ColorInt final int statusBarColor, @ColorInt final int toolbarColor, @ColorInt final int navBarColor) {
        setSystemBarColors(Colors.TRANSPARENT, statusBarColor, toolbarColor, navBarColor);
    }

    @Override
    public void setSystemBarColors(@ColorInt final int backgroundColor, @ColorInt final int statusBarColor, @ColorInt final int toolbarColor, @ColorInt final int navBarColor) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Actionbar, BottomNavBar & StatusBar
                ActivityUtils.setTransparentWindow(getWindow(), backgroundColor, statusBarColor, navBarColor, false);

                if (!ColorsUtils.isSuperLight(navBarColor)) {
                    mBottomNavView.setItemRippleColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_ripple_color_dark));
                } else {
                    mBottomNavView.setItemRippleColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_ripple_color_light));
                }

                mBottomNavView.setBackgroundColor(toolbarColor);

                if (!ColorsUtils.isSuperLight(toolbarColor)) {
                    mBottomNavView.setItemIconTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_dark));
                    mBottomNavView.setItemTextColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_dark));
                } else {
                    mBottomNavView.setItemIconTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_light));
                    mBottomNavView.setItemTextColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_light));
                }
            }
        });
    }

    private void updateWindowColors() {
        int color = ActivityUtils.getColor(this, android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == DarkMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        mRootView.setBackgroundColor(color);
        getWindow().getDecorView().setBackgroundColor(color);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateWindowColors();
    }

    @Override
    public void onThemeChanged(DarkMode mode) {
        int color = ActivityUtils.getColor(MainActivity.this, android.R.attr.colorBackground);
        if (mode == DarkMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        mRootView.setBackgroundColor(color);
    }
}
