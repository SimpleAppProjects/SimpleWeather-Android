package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;

import java.io.StringReader;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Back stack listener
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                refreshNavViewCheckedItem();
            }
        });

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Alerts
        if (getIntent() != null && "".equals(getIntent().getAction())) {
            Fragment newFragment = WeatherNowFragment.newInstance(getIntent().getExtras());

            if (fragment == null) {
                fragment = newFragment;
                // Navigate to WeatherNowFragment
                // Make sure we exit if location is not home
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment, "notification")
                        .commit();
            } else {
                // Navigate to WeatherNowFragment
                // Make sure we exit if location is not home
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, newFragment)
                        .addToBackStack(null)
                        .commit();
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
                    .commit();
        }

        if (getIntent() != null && getIntent().hasExtra("shortcut-data")) {
            JsonReader reader = new JsonReader(
                    new StringReader(getIntent().getStringExtra("shortcut-data")));
            LocationData locData = LocationData.fromJson(reader);

            // Navigate to WeatherNowFragment
            Fragment newFragment = WeatherNowFragment.newInstance(locData);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newFragment, "shortcut")
                    .commit();

            // Disable navigation
            toggle.setDrawerIndicatorEnabled(false);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        navigationView.setCheckedItem(R.id.nav_weathernow);

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                ShortcutCreator.updateShortcuts();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            // Destroy untagged fragments onbackpressed
            if (current != null && current.getTag() == null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(current)
                        .commit();
                current = null;
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;

        if (id == R.id.nav_weathernow) {
            fragment = new WeatherNowFragment();
        } else if (id == R.id.nav_locations) {
            fragment = new LocationsFragment();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            drawer.closeDrawer(GravityCompat.START);
            return false;
        }

        if (fragment != null) {
            if (fragment.getClass() != getSupportFragmentManager().findFragmentById(R.id.fragment_container).getClass()) {
                if (fragment instanceof WeatherNowFragment) {
                    // Pop all since we're going home
                    fragment = null;
                    transaction.commit();
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else if (fragment instanceof LocationsFragment) {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                    if (current instanceof WeatherNowFragment) {
                        // Hide home frag
                        if ("home".equals(current.getTag()))
                            transaction.hide(current);
                        else {
                            // Destroy lingering WNow frag
                            getSupportFragmentManager().beginTransaction()
                                    .remove(current)
                                    .commit();
                            current.onDestroy();
                            current = null;
                            getSupportFragmentManager().popBackStack();
                        }
                    }

                    if (getSupportFragmentManager().findFragmentByTag("locations") != null) {
                        // Pop all frags if LocFrag in backstack
                        fragment = null;
                        transaction.commit();
                    } else {
                        // Add LocFrag if not in backstack
                        // Commit the transaction
                        transaction
                                .add(R.id.fragment_container, fragment, "locations")
                                .addToBackStack(null)
                                .commit();
                    }
                }
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        refreshNavViewCheckedItem();
    }

    private void refreshNavViewCheckedItem() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof WeatherNowFragment) {
            navigationView.setCheckedItem(R.id.nav_weathernow);
            getSupportActionBar().setTitle(getString(R.string.title_activity_weather_now));
        } else if (fragment instanceof LocationsFragment) {
            navigationView.setCheckedItem(R.id.nav_locations);
            getSupportActionBar().setTitle(getString(R.string.label_nav_locations));
        } else if (fragment instanceof WeatherAlertsFragment) {
            navigationView.setCheckedItem(R.id.nav_weathernow);
            getSupportActionBar().setTitle(getString(R.string.title_fragment_alerts));
        }

        if (fragment instanceof WeatherAlertsFragment) {
            getSupportActionBar().hide();
        } else {
            if (!getSupportActionBar().isShowing())
                getSupportActionBar().show();

            if (getIntent() != null && getIntent().hasExtra("shortcut-data"))
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            else
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }
}
