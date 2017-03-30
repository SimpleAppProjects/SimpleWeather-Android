package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String ARG_QUERY = "query";
    private String ARG_INDEX = "index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Back stack listener
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                refreshNavViewCheckedItem();
            }
        });

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Check if fragment exists
        if (fragment == null)
        {
            int idx = getIntent().getIntExtra(ARG_INDEX, -1);
            String qry = getIntent().getStringExtra(ARG_QUERY);

            fragment = WeatherNowFragment.newInstance(qry, idx);

            // Navigate to WeatherNowFragment
            getSupportFragmentManager().beginTransaction().replace(
                    R.id.fragment_container, fragment).commit();
        }

        navigationView.setCheckedItem(R.id.nav_weathernow);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack so the user can navigate back
                transaction.replace(R.id.fragment_container, fragment);

                if (fragment instanceof WeatherNowFragment) {
                    // Pop all since we're going home
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    transaction.addToBackStack(null);
                }

                // Commit the transaction
                transaction.commit();
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
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof WeatherNowFragment) {
            navigationView.setCheckedItem(R.id.nav_weathernow);
            getSupportActionBar().setTitle(getString(R.string.title_activity_weather_now));
        } else if (fragment instanceof LocationsFragment) {
            navigationView.setCheckedItem(R.id.nav_locations);
            getSupportActionBar().setTitle(getString(R.string.label_nav_locations));
        }
    }
}
