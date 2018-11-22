package com.thewizrd.simpleweather;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.junit.Test;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.TextStyle;

import java.util.Locale;

import static org.junit.Assert.assertNotNull;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleInstrumentedTest {
    @Test
    public void test() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        AndroidThreeTen.init(appContext);
        ZoneId zId = ZoneId.of("America/New_York");
        System.out.println(zId.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ROOT));
        assertNotNull(zId);
    }
}
