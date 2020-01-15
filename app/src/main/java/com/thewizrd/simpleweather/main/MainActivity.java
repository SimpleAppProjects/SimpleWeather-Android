package com.thewizrd.simpleweather.main;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.preferences.SettingsFragment;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.io.StringReader;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
        SystemBarColorManager, UserThemeMode.OnThemeChangeListener {

    private BottomNavigationView mBottomNavView;
    private View mFragmentContainer;
    private View mRootView;
    private boolean isSystemNightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make full transparent statusBar
        ActivityUtils.setTransparentWindow(getWindow(), Colors.SIMPLEBLUE, Colors.TRANSPARENT, Colors.TRANSPARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        setContentView(R.layout.activity_main);

        mFragmentContainer = findViewById(R.id.fragment_container);
        mRootView = (View) mFragmentContainer.getParent();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mRootView.setFitsSystemWindows(true);
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
                        .replace(R.id.fragment_container, fragment, Constants.FRAGTAG_NOTIFICATION)
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
            if (getIntent() != null && getIntent().hasExtra(Constants.KEY_DATA))
                fragment = WeatherNowFragment.newInstance(getIntent().getExtras());
            else
                fragment = new WeatherNowFragment();

            // Navigate to WeatherNowFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, Constants.FRAGTAG_HOME)
                    .commitNow();
        }

        // Shortcut intent: from app shortcuts
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_SHORTCUTDATA)) {
            JsonReader reader = new JsonReader(
                    new StringReader(getIntent().getStringExtra(Constants.KEY_SHORTCUTDATA)));
            LocationData locData = LocationData.fromJson(reader);

            // Navigate to WeatherNowFragment
            Fragment newFragment = WeatherNowFragment.newInstance(locData);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newFragment, Constants.FRAGTAG_SHORTCUT)
                    .commitNow();
        }

        // Check nav item in bottom nav view
        // based on current fragment
        refreshNavViewCheckedItem();

        // Update app shortcuts
        ShortcutCreatorWorker.requestUpdateShortcuts(this);

        final int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isSystemNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
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
            if (current != null) {
                if (current.getTag() == null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(current)
                            .commit();
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .detach(current)
                            .commit();
                }
            }

            // If sub-fragment exist: pop those one by one
            int backstackCount = getSupportFragmentManager().getBackStackEntryCount();
            if (current != null
                    && (current.getClass().getName().contains(SettingsFragment.class.getName() + "$")
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
        final int id = item.getItemId();
        final int currentId;

        FragmentTransaction transaction = addCustomAnimations(getSupportFragmentManager().beginTransaction());
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        Fragment fragment = null;

        if (current instanceof WeatherNowFragment) {
            currentId = R.id.nav_weathernow;
        } else if (current instanceof LocationsFragment) {
            currentId = R.id.nav_locations;
        } else if (current instanceof SettingsFragment) {
            currentId = R.id.nav_settings;
        } else {
            currentId = -1;
        }

        // Make sure we're not navigating to the same type of fragment
        if (current != null && currentId != id) {
            if (id == R.id.nav_weathernow) {
                // Pop all since we're going home
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else if (id == R.id.nav_locations) {
                /* NOTE: KEEP THIS IT WORKS FINE */
                if (current instanceof WeatherNowFragment) {
                    int backstackCount = getSupportFragmentManager().getBackStackEntryCount();

                    // Hide home frag
                    if (Constants.FRAGTAG_HOME.equals(current.getTag()) || backstackCount == 0) {
                        transaction.hide(current);
                    } else {
                        /*
                         * NOTE
                         * Destroy lingering WNow frag and commit transaction
                         * This is to avoid adding the fragment again from the backstack
                         */
                        getSupportFragmentManager().beginTransaction()
                                .remove(current)
                                .commitAllowingStateLoss();
                        getSupportFragmentManager().popBackStack();
                    }
                }
                /* NOTE: KEEP ABOVE */
                else {
                    // If current frag is Settings sub-fragment pop all off
                    if (current.getClass().getName().contains(SettingsFragment.class.getName() + "$"))
                        getSupportFragmentManager().popBackStack(Constants.FRAGTAG_SETTINGS, 0);
                    /* NOTE: Don't pop here so we don't trigger onResume/onHiddenChanged of root fragment */
                    // If a Settings fragment exists remove it
                    if (getSupportFragmentManager().findFragmentByTag(Constants.FRAGTAG_SETTINGS) != null) {
                        current = getSupportFragmentManager().findFragmentByTag(Constants.FRAGTAG_SETTINGS);
                        getSupportFragmentManager().beginTransaction()
                                .remove(current)
                                .commitAllowingStateLoss();
                    }
                    // If an extra WeatherNowFragment exists remove it
                    if (getSupportFragmentManager().findFragmentByTag(Constants.FRAGTAG_FAVORITES) != null) {
                        current = getSupportFragmentManager().findFragmentByTag(Constants.FRAGTAG_FAVORITES);
                        getSupportFragmentManager().beginTransaction()
                                .remove(current)
                                .commitAllowingStateLoss();
                    }
                    // If current frag is Weather(Alerts||Details)Fragment remove it
                    if (current instanceof WeatherAlertsFragment || current instanceof WeatherDetailsFragment) {
                        getSupportFragmentManager().beginTransaction()
                                .remove(current)
                                .commitAllowingStateLoss();
                    }
                }

                fragment = getSupportFragmentManager().findFragmentByTag(Constants.FRAGTAG_LOCATIONS);
                if (fragment != null) {
                    /*
                     * If the fragment exists and has not yet at least started,
                     * remove and create a new instance
                     *
                     * This avoids the following exception:
                     * java.lang.IllegalStateException: Restarter must be created only during owner's initialization stage
                     * https://stackoverflow.com/a/56783167
                     */
                    if (!fragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        getSupportFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commitAllowingStateLoss();
                        fragment = new LocationsFragment();
                    }

                    if (fragment.isAdded()) {
                        // Show LocationsFragment if it exists
                        transaction
                                .show(fragment)
                                .addToBackStack(null);
                    } else {
                        transaction
                                .add(R.id.fragment_container, fragment, Constants.FRAGTAG_LOCATIONS)
                                .addToBackStack(null);
                    }
                } else {
                    // Add LocFrag if not in backstack/DNE
                    fragment = new LocationsFragment();
                    transaction
                            .add(R.id.fragment_container, fragment, Constants.FRAGTAG_LOCATIONS)
                            .addToBackStack(null);
                }

                // Commit the transaction
                transaction.commit();
            } else if (id == R.id.nav_settings) {
                fragment = new SettingsFragment();

                // Commit the transaction if current frag is not a SettingsFragment sub-fragment
                if (!current.getClass().getName().contains(SettingsFragment.class.getName())) {
                    transaction.hide(current);
                    /*
                     * NOTE
                     * If current fragment is not WNow Fragment commit and recreate transaction
                     * This is to avoid showing the fragment again from the backstack
                     */
                    if (!(current instanceof WeatherNowFragment)) {
                        transaction.commit();
                        transaction = addCustomAnimations(getSupportFragmentManager().beginTransaction());
                    }
                    transaction
                            .add(R.id.fragment_container, fragment, Constants.FRAGTAG_SETTINGS)
                            .addToBackStack(Constants.FRAGTAG_SETTINGS)
                            .commit();
                }
            }
        }

        return true;
    }

    private FragmentTransaction addCustomAnimations(@NonNull FragmentTransaction transaction) {
        return transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
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
        } else if (fragment != null && fragment.getClass().getName().contains(SettingsFragment.class.getName())) {
            checkedItemId = R.id.nav_settings;
        }

        MenuItem item = mBottomNavView.getMenu().findItem(checkedItemId);
        if (item != null) {
            item.setChecked(true);
        }
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
                Configuration config = getResources().getConfiguration();
                final boolean isLandscapeMode = config.orientation != Configuration.ORIENTATION_PORTRAIT && !ActivityUtils.isLargeTablet(MainActivity.this);

                ActivityUtils.setTransparentWindow(getWindow(), backgroundColor, statusBarColor, isLandscapeMode ? navBarColor : Colors.TRANSPARENT, isLandscapeMode);
                mRootView.setBackgroundColor(backgroundColor);

                if (!ColorsUtils.isSuperLight(navBarColor)) {
                    mBottomNavView.setItemRippleColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_ripple_color_dark));
                } else {
                    mBottomNavView.setItemRippleColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_ripple_color_light));
                }

                mBottomNavView.setBackgroundColor(toolbarColor);

                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                        isSystemNightMode && ColorUtils.calculateContrast(Colors.SIMPLEBLUELIGHT, toolbarColor) > 4.0f) {
                    mBottomNavView.setItemIconTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_darkcolor));
                    mBottomNavView.setItemTextColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_darkcolor));
                } else {
                    if (!ColorsUtils.isSuperLight(toolbarColor)) {
                        mBottomNavView.setItemIconTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_dark));
                        mBottomNavView.setItemTextColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_dark));
                    } else {
                        mBottomNavView.setItemIconTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_light));
                        mBottomNavView.setItemTextColor(ContextCompat.getColorStateList(MainActivity.this, R.color.btm_nav_item_tint_light));
                    }
                }
            }
        });
    }

    private void updateWindowColors() {
        int color = ActivityUtils.getColor(this, android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        mRootView.setBackgroundColor(color);
        getWindow().getDecorView().setBackgroundColor(color);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        // Update before we send the configuration to all other fragments
        final int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isSystemNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        super.onConfigurationChanged(newConfig);

        updateWindowColors();
    }

    @Override
    public void onThemeChanged(UserThemeMode mode) {
        int color = ActivityUtils.getColor(MainActivity.this, android.R.attr.colorBackground);
        if (mode == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        mRootView.setBackgroundColor(color);
    }
}
