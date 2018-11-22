package com.thewizrd.simpleweather;

import org.junit.Test;

import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void zoning() throws Exception {
        ZoneId zId = ZoneId.of("Asia/Tokyo");
        System.out.println(zId.getDisplayName(TextStyle.SHORT, Locale.ROOT));
        assertTrue(true);
    }
}