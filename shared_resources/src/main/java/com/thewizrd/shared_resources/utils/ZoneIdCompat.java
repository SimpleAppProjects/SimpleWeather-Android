package com.thewizrd.shared_resources.utils;

import com.ibm.icu.util.TimeZone;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;
import java.util.Objects;

public class ZoneIdCompat {
    public static ZoneId of(String zoneId) {
        Objects.requireNonNull(zoneId, "zoneId");

        ZoneId zid;
        try {
            zid = ZoneId.of(zoneId);
        } catch (ZoneRulesException ignored) {
            /*
             * If time zone is unknown by system, use ICU4J as a fallback
             * ICU4J will have the latest tzdb data
             */
            TimeZone icuTZ = TimeZone.getTimeZone(zoneId, TimeZone.TIMEZONE_ICU);
            final int offsetMs = icuTZ.getOffset(System.currentTimeMillis()/*epochMillis*/); // return offset in milliseconds
            zid = ZoneId.ofOffset("GMT", ZoneOffset.ofTotalSeconds(offsetMs / 1000));
        }

        return zid;
    }
}
