package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.simpleweather.widgets.WidgetUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void test() {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getTargetContext();

        ApplicationLib app = new ApplicationLib() {
            @Override
            public Context getAppContext() {
                return appContext.getApplicationContext();
            }

            @Override
            public SharedPreferences getPreferences() {
                return PreferenceManager.getDefaultSharedPreferences(getAppContext());
            }

            @Override
            public SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener() {
                return null;
            }

            @Override
            public AppState getAppState() {
                return null;
            }

            @Override
            public boolean isPhone() {
                return true;
            }
        };

        SimpleLibrary.init(app);
        AndroidThreeTen.init(appContext);

        // Start logger
        Logger.init(appContext);

        WidgetUtils.addWidgetId("NewYork", 10);
        WidgetUtils.addWidgetId("NewYork", 11);
        WidgetUtils.addWidgetId("NewYork", 12);
        WidgetUtils.addWidgetId("NewYork", 13);
        WidgetUtils.addWidgetId("NewYork", 14);
        WidgetUtils.addWidgetId("NewYork", 15);
        WidgetUtils.addWidgetId("NewYork", 16);
        WidgetUtils.addWidgetId("NewYork", 17);

        LocationData loc = new LocationData();
        loc.setQuery("OldYork");

        WidgetUtils.updateWidgetIds("NewYork", loc);
    }
}
