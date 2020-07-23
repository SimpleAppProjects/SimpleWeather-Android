package com.thewizrd.simpleweather.setup;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.SetupGraphDirections;
import com.thewizrd.simpleweather.databinding.ActivitySetupBinding;

public class SetupActivity extends AppCompatActivity {

    private ActivitySetupBinding binding;
    private SetupViewModel viewModel;
    private NavController mNavController;
    private boolean isWeatherLoaded;

    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isWeatherLoaded = Settings.isWeatherLoaded();

        AnalyticsLogger.logEvent("SetupActivity: onCreate");

        // Check if this activity was started from adding a new widget
        if (getIntent() != null && AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(getIntent().getAction())) {
            mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            if (Settings.isWeatherLoaded() || mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                // This shouldn't happen, but just in case
                setResult(RESULT_OK);
                finish();
                // Return if we're finished
                return;
            }

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                // Set the result to CANCELED.  This will cause the widget host to cancel
                // out of the widget placement if they press the back button.
                setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID) &&
                mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            mAppWidgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            // Set the result to CANCELED.  This will cause the widget host to cancel
            // out of the widget placement if they press the back button.
            setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        }

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavBar, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, final WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            binding.getRoot().setFitsSystemWindows(true);

        int color = ContextCompat.getColor(this, R.color.colorPrimaryBackground);
        ActivityUtils.setTransparentWindow(getWindow(), color);

        viewModel = new ViewModelProvider(this).get(SetupViewModel.class);

        NavHostFragment hostFragment = NavHostFragment.create(R.navigation.setup_graph);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, hostFragment)
                .setPrimaryNavigationFragment(hostFragment)
                .commit();

        setupBottomNavBar();

        if (isWeatherLoaded && viewModel.getLocationData() == null) {
            viewModel.setLocationData(Settings.getHomeData());
        }
    }

    private void setupBottomNavBar() {
        binding.bottomNavBar.setItemCount(getItemCount());
        binding.bottomNavBar.setSelectedItem(0);
        binding.bottomNavBar.setVisibility(View.VISIBLE);
        binding.bottomNavBar.setOnBackButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavController.navigateUp();
            }
        });
        binding.bottomNavBar.setOnNextButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavDestination destination = mNavController.getCurrentDestination();
                if (destination != null) {
                    @IdRes int destinationId = destination.getId();
                    if (getPosition(destinationId) >= getItemCount() - 1) {
                        // Complete
                        onCompleted();
                    } else {
                        final int nextDestination = getNextDestination(destinationId);
                        if (nextDestination != R.id.setupSettingsFragment || viewModel.getLocationData() != null) {
                            mNavController.navigate(nextDestination, null,
                                    new NavOptions.Builder()
                                            .setPopUpTo(destinationId, true)
                                            .build());
                        }
                    }
                }
            }
        });
    }

    private int getItemCount() {
        if (isWeatherLoaded) {
            return 2;
        } else {
            return 3;
        }
    }

    private int getPosition(@IdRes int destinationId) {
        switch (destinationId) {
            default:
            case R.id.setupWelcomeFragment:
                return 0;
            case R.id.setupLocationFragment:
            case R.id.locationSearchFragment3:
                return 1;
            case R.id.setupSettingsFragment:
                if (isWeatherLoaded) {
                    return 1;
                } else {
                    return 2;
                }
        }
    }

    private @IdRes
    int getNextDestination(@IdRes int destinationId) {
        switch (destinationId) {
            default:
            case R.id.setupWelcomeFragment:
                if (isWeatherLoaded) {
                    return R.id.setupSettingsFragment;
                } else {
                    return R.id.setupLocationFragment;
                }
            case R.id.setupLocationFragment:
            case R.id.locationSearchFragment3:
                return R.id.setupSettingsFragment;
            case R.id.setupSettingsFragment:
                return R.id.mainActivity;
        }
    }

    private void updateBottomNavigationBarForDestination(@IdRes int destinationId) {
        binding.bottomNavBar.setSelectedItem(getPosition(destinationId));
        binding.bottomNavBar.setVisibility(destinationId == R.id.locationSearchFragment3 ? View.GONE : View.VISIBLE);
        if (destinationId == R.id.setupLocationFragment) {
            binding.bottomNavBar.showBackButton(false);
            binding.bottomNavBar.showNextButton(false);
        } else if (destinationId == R.id.setupSettingsFragment) {
            binding.bottomNavBar.showBackButton(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNavController = Navigation.findNavController(this, R.id.fragment_container);
        mNavController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                updateBottomNavigationBarForDestination(destination.getId());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("SetupActivity: onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        AnalyticsLogger.logEvent("SetupActivity: onPause");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAppWidgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    }

    private void onCompleted() {
        // Completion
        Settings.setWeatherLoaded(true);
        Settings.setOnBoardingComplete(true);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Start WeatherNow Activity with weather data
            NavOptions.Builder opts = new NavOptions.Builder();
            NavDestination currentDestination = mNavController.getCurrentDestination();
            if (currentDestination != null) {
                opts.setPopUpTo(currentDestination.getId(), true);
            }
            mNavController.navigate(
                    SetupGraphDirections.actionGlobalMainActivity()
                            .setData(JSONParser.serializer(viewModel.getLocationData(), LocationData.class)),
                    opts.build());
            finishAffinity();
        } else {
            // Create return intent
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            if (viewModel.getLocationData() != null)
                resultValue.putExtra(Constants.KEY_DATA, JSONParser.serializer(viewModel.getLocationData(), LocationData.class));
            setResult(Activity.RESULT_OK, resultValue);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
