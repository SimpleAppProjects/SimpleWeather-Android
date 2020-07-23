package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;

public class WeatherWidgetConfigActivity extends AppCompatActivity {
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("WidgetConfig: onCreate");

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

        int color = ActivityUtils.getColor(this, android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }

        ActivityUtils.setTransparentWindow(getWindow(), color, Colors.TRANSPARENT, Colors.TRANSPARENT, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        View mRootView = (View) findViewById(R.id.fragment_container).getParent();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mRootView.setFitsSystemWindows(true);

        Bundle args = new Bundle();
        args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        if (getIntent() != null && getIntent().getExtras() != null) {
            args.putAll(getIntent().getExtras());
        }

        NavHostFragment hostFragment = NavHostFragment.create(R.navigation.widget_graph, args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, hostFragment)
                .setPrimaryNavigationFragment(hostFragment)
                .commit();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAppWidgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        super.onSaveInstanceState(outState);
    }
}
