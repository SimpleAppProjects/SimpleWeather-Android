package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;

public class WeatherWidgetConfigActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("WidgetConfig: onCreate");

        // Widget id for ConfigurationActivity
        int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

        // Find the widget id from the intent.
        if (getIntent() != null && getIntent().getExtras() != null) {
            mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If they gave us an intent without the widget id, just bail.
            finish();
        }

        setContentView(R.layout.activity_widget_setup);

        View mRootView = (View) findViewById(R.id.fragment_container).getParent();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mRootView.setFitsSystemWindows(true);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            Bundle args = new Bundle();
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            if (getIntent() != null
                    && !StringUtils.isNullOrWhitespace(getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY))) {
                String locName = getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONNAME);
                String locQuery = getIntent().getStringExtra(WeatherWidgetService.EXTRA_LOCATIONQUERY);
                args.putString(WeatherWidgetService.EXTRA_LOCATIONNAME, locName);
                args.putString(WeatherWidgetService.EXTRA_LOCATIONQUERY, locQuery);
            }

            fragment = WeatherWidgetPreferenceFragment.newInstance(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        OnBackPressedFragmentListener fragBackPressedListener = null;
        if (current instanceof OnBackPressedFragmentListener)
            fragBackPressedListener = (OnBackPressedFragmentListener) current;

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
