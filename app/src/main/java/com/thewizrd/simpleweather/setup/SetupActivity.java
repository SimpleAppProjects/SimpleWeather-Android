package com.thewizrd.simpleweather.setup;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.stepstone.stepper.adapter.StepAdapter;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.databinding.ActivitySetupBinding;
import com.thewizrd.simpleweather.main.MainActivity;

public class SetupActivity extends AppCompatActivity implements StepperLayout.StepperListener, StepperDataManager {

    private ActivitySetupBinding binding;
    private final String CURRENT_STEP_POSITION_KEY = "position";
    private final String KEY_ARGS = "args";
    private Bundle args;
    private int currentPosition = 0;

    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        View mRootView = binding.getRoot();
        setContentView(mRootView);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mRootView.setFitsSystemWindows(true);

        // Make full transparent statusBar
        ActivityUtils.setTransparentWindow(getWindow(), Colors.SIMPLEBLUE, Colors.TRANSPARENT, Colors.TRANSPARENT,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);

        int startingStepPosition = 0;
        if (savedInstanceState != null) {
            startingStepPosition = savedInstanceState.getInt(CURRENT_STEP_POSITION_KEY, 0);
            if (Settings.isWeatherLoaded() && startingStepPosition > 1) startingStepPosition = 1;
            args = savedInstanceState.getBundle(KEY_ARGS);
        }

        getArguments().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

        binding.stepperLayout.setCompleteButtonColor(Colors.WHITE);
        binding.stepperLayout.setAdapter(new SetupStepperAdapter(getSupportFragmentManager(), this), startingStepPosition);
        binding.stepperLayout.setListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.stepperLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_STEP_POSITION_KEY, binding.stepperLayout.getCurrentStepPosition());
        outState.putBundle(KEY_ARGS, getArguments());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCompleted(View completeButton) {
        // Commit settings changes
        // Units
        if (getArguments().containsKey(Settings.KEY_UNITS)) {
            String value = getArguments().getString(Settings.KEY_UNITS);
            Settings.setTempUnit(value);
        }
        // Interval
        if (getArguments().containsKey(Settings.KEY_REFRESHINTERVAL)) {
            int value = getArguments().getInt(Settings.KEY_REFRESHINTERVAL, Settings.DEFAULTINTERVAL);
            Settings.setRefreshInterval(value);
        }
        // Ongoing Notification
        if (getArguments().containsKey(Settings.KEY_ONGOINGNOTIFICATION)) {
            boolean value = getArguments().getBoolean(Settings.KEY_ONGOINGNOTIFICATION, false);
            Settings.setOngoingNotification(value);
        }
        // Weather Alerts
        if (getArguments().containsKey(Settings.KEY_USEALERTS)) {
            boolean value = getArguments().getBoolean(Settings.KEY_USEALERTS, false);
            Settings.setAlerts(value);
        }

        // Completion
        Settings.setOnBoardingComplete(true);

        mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Start WeatherNow Activity with weather data
            Intent intent = new Intent(this, MainActivity.class);

            if (getArguments().containsKey(Constants.KEY_DATA))
                intent.putExtra(Constants.KEY_DATA, getArguments().getString(Constants.KEY_DATA));

            startActivity(intent);
            finishAffinity();
        } else {
            // Create return intent
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            if (getArguments().containsKey(Constants.KEY_DATA))
                resultValue.putExtra(Constants.KEY_DATA, getArguments().getString(Constants.KEY_DATA));
            setResult(Activity.RESULT_OK, resultValue);
            finish();
        }

    }

    @Override
    public void onBackPressed() {
        StepAdapter adapter = binding.stepperLayout.getAdapter();
        Fragment current = (Fragment) adapter.findStep(currentPosition);

        OnBackPressedFragmentListener fragBackPressedListener = null;
        if (current instanceof OnBackPressedFragmentListener)
            fragBackPressedListener = (OnBackPressedFragmentListener) current;

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onError(VerificationError verificationError) {

    }

    @Override
    public void onStepSelected(int newStepPosition) {
        currentPosition = newStepPosition;
    }

    @Override
    public void onReturn() {

    }

    @Override
    public Bundle getArguments() {
        if (args == null) {
            args = new Bundle();
        }
        return args;
    }
}
