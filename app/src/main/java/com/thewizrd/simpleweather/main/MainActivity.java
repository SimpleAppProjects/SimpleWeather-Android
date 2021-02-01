package com.thewizrd.simpleweather.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.transition.TransitionManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.activity.UserLocaleActivity;
import com.thewizrd.simpleweather.databinding.ActivityMainBinding;
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationService;
import com.thewizrd.simpleweather.preferences.SettingsFragment;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker;
import com.thewizrd.simpleweather.updates.InAppUpdateManager;

public class MainActivity extends UserLocaleActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
        UserThemeMode.OnThemeChangeListener {

    private ActivityMainBinding binding;
    private NavController mNavController;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private InAppUpdateManager appUpdateManager;
    private static final int INSTALL_REQUESTCODE = 168;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("MainActivity: onCreate");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavBar.setOnNavigationItemSelectedListener(this);

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavBar, new OnApplyWindowInsetsListener() {
            private final int paddingStart = ViewCompat.getPaddingStart(binding.bottomNavBar);
            private final int paddingTop = binding.bottomNavBar.getPaddingTop();
            private final int paddingEnd = ViewCompat.getPaddingEnd(binding.bottomNavBar);
            private final int paddingBottom = binding.bottomNavBar.getPaddingBottom();

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, final WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.getSystemWindowInsetLeft(),
                        paddingTop,
                        paddingEnd + insets.getSystemWindowInsetRight(),
                        paddingBottom + insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        binding.bottomNavBar.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(view.getPaddingLeft(),
                        0,
                        view.getWidth() - view.getPaddingRight(),
                        view.getHeight());
            }
        });

        // Back stack listener
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                refreshNavViewCheckedItem();
            }
        });

        updateWindowColors(Settings.getUserThemeMode());

        final Bundle args = new Bundle();
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

        appUpdateManager = InAppUpdateManager.create(getApplicationContext());

        if (FeatureSettings.isUpdateAvailable()) {
            // Update is available; double check if mandatory
            appUpdateManager.shouldStartImmediateUpdateFlow()
                    .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            if (task.isSuccessful() && task.getResult()) {
                                appUpdateManager.startImmediateUpdateFlow(MainActivity.this, INSTALL_REQUESTCODE);
                            } else {
                                initializeNavFragment(args);
                                initializeNavController();
                            }
                        }
                    });
            return;
        }

        initializeNavFragment(args);
    }

    private void initializeNavFragment(@NonNull Bundle args) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            NavHostFragment hostFragment = NavHostFragment.create(R.navigation.nav_graph, args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!FeatureSettings.isUpdateAvailable()) {
            initializeNavController();
        }

        // Update app shortcuts
        ShortcutCreatorWorker.requestUpdateShortcuts(this);
    }

    private void initializeNavController() {
        mNavController = Navigation.findNavController(this, R.id.fragment_container);

        NavigationUI.setupWithNavController(binding.bottomNavBar, mNavController);
        mNavController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull final NavDestination destination, @Nullable Bundle arguments) {
                refreshNavViewCheckedItem();

                if (destination.getId() == R.id.weatherNowFragment || destination.getId() == R.id.locationsFragment) {
                    binding.bottomNavBar.setVisibility(View.VISIBLE);
                } else {
                    binding.bottomNavBar.postOnAnimationDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (destination.getId() == R.id.locationSearchFragment3 || destination.getId() == R.id.weatherNowFragment) {
                                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());
                            }
                            binding.bottomNavBar.setVisibility(destination.getId() == R.id.locationSearchFragment ? View.GONE : View.VISIBLE);
                        }
                    }, (int) (Constants.ANIMATION_DURATION * 1.5f));
                }
            }
        });

        // Alerts: from weather alert notification
        if (getIntent() != null && WeatherAlertNotificationService.ACTION_SHOWALERTS.equals(getIntent().getAction())) {
            NavDestination destination = mNavController.getCurrentDestination();
            if (destination != null && destination.getId() != R.id.weatherListFragment) {
                LocationData locationData = Settings.getHomeData();
                NavGraphDirections.ActionGlobalWeatherListFragment args =
                        WeatherListFragmentDirections.actionGlobalWeatherListFragment()
                                .setData(JSONParser.serializer(locationData, LocationData.class))
                                .setWeatherListType(WeatherListType.ALERTS);
                mNavController.navigate(args);
            }
        }

        // Check nav item in bottom nav view
        // based on current fragment
        refreshNavViewCheckedItem();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Alerts: from weather alert notification
        if (intent != null && WeatherAlertNotificationService.ACTION_SHOWALERTS.equals(intent.getAction())) {
            Bundle args = new Bundle();
            if (intent.getExtras() != null) {
                args.putAll(intent.getExtras());
            }
            args.putSerializable(Constants.ARGS_WEATHERLISTTYPE, WeatherListType.ALERTS);
            mNavController.navigate(R.id.weatherListFragment, args);
        }
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

        // Checks that the update is not stalled during 'onResume()'.
        // However, you should execute this check at all entry points into the app.
        if (FeatureSettings.isUpdateAvailable()) {
            appUpdateManager.resumeUpdateIfStarted(this, INSTALL_REQUESTCODE);
        }

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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == INSTALL_REQUESTCODE) {
            if (resultCode != RESULT_OK) {
                // Update flow failed; exit
                finishAffinity();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
        if (mNavController != null && mNavController.getCurrentDestination() != null) {
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
    }

    @Override
    public void onThemeChanged(UserThemeMode mode) {
        updateWindowColors(mode);
    }

    private void updateWindowColors(UserThemeMode mode) {
        int color = ContextUtils.getColor(this, android.R.attr.colorBackground);
        if (mode == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }

        ActivityUtils.setTransparentWindow(getWindow(), color, Colors.TRANSPARENT, color);
        binding.getRoot().setBackgroundColor(color);
        binding.bottomNavBar.setBackgroundColor(color);
    }
}
