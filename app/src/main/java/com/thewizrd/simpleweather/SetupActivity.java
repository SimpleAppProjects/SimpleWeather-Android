package com.thewizrd.simpleweather;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.stepstone.stepper.adapter.StepAdapter;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

public class SetupActivity extends AppCompatActivity implements StepperLayout.StepperListener, StepperDataManager {

    private StepperLayout mStepperLayout;
    private final String CURRENT_STEP_POSITION_KEY = "position";
    private Bundle args = new Bundle();
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

        setContentView(R.layout.activity_setup);

        // Make full transparent statusBar
        ActivityUtils.setTransparentWindow(getWindow(), Colors.SIMPLEBLUE, Colors.SIMPLEBLUE);

        int startingStepPosition = 0;
        if (savedInstanceState != null) {
            startingStepPosition = savedInstanceState.getInt(CURRENT_STEP_POSITION_KEY, 0);
            args = savedInstanceState.getBundle("args");
        }

        if (args == null) args = new Bundle();
        args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

        mStepperLayout = findViewById(R.id.stepperLayout);
        mStepperLayout.setCompleteButtonColor(Colors.WHITE);
        mStepperLayout.setAdapter(new SetupStepperAdapter(getSupportFragmentManager(), this), startingStepPosition);
        mStepperLayout.setListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_STEP_POSITION_KEY, mStepperLayout.getCurrentStepPosition());
        outState.putBundle("args", args);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCompleted(View completeButton) {
        // Commit settings changes
        // Units
        if (args.containsKey(Settings.KEY_UNITS)) {
            String value = args.getString(Settings.KEY_UNITS);
            Settings.setTempUnit(value);
        }
        // Interval
        if (args.containsKey(Settings.KEY_REFRESHINTERVAL)) {
            int value = args.getInt(Settings.KEY_REFRESHINTERVAL, Settings.DEFAULTINTERVAL);
            Settings.setRefreshInterval(value);
        }
        // Ongoing Notification
        if (args.containsKey(Settings.KEY_ONGOINGNOTIFICATION)) {
            boolean value = args.getBoolean(Settings.KEY_ONGOINGNOTIFICATION, false);
            Settings.setOngoingNotification(value);
        }
        // Weather Alerts
        if (args.containsKey(Settings.KEY_USEALERTS)) {
            boolean value = args.getBoolean(Settings.KEY_USEALERTS, false);
            Settings.setAlerts(value);
        }

        // Completion
        Settings.setOnBoardingComplete(true);

        mAppWidgetId = args.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Start WeatherNow Activity with weather data
            Intent intent = new Intent(this, MainActivity.class);

            if (args.containsKey("data"))
                intent.putExtra("data", args.getString("data"));

            startActivity(intent);
            finishAffinity();
        } else {
            // Create return intent
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            if (args.containsKey("data"))
                resultValue.putExtra("data", args.getString("data"));
            setResult(Activity.RESULT_OK, resultValue);
            finish();
        }

    }

    @Override
    public void onBackPressed() {
        StepAdapter adapter = mStepperLayout.getAdapter();
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
        return args;
    }
}
