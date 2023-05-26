package com.thewizrd.shared_resources.utils;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Units {
    @StringDef({
            FAHRENHEIT,
            CELSIUS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TemperatureUnits {
    }

    @StringDef({
            MILES_PER_HOUR,
            KILOMETERS_PER_HOUR,
            METERS_PER_SECOND
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SpeedUnits {
    }

    @StringDef({
            INHG,
            MILLIBAR,
            MMHG
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PressureUnits {
    }

    @StringDef({
            MILES,
            KILOMETERS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DistanceUnits {
    }

    @StringDef({
            INCHES,
            MILLIMETERS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PrecipitationUnits {
    }

    public static final String FAHRENHEIT = "F";
    public static final String CELSIUS = "C";
    public static final String MILES_PER_HOUR = "MPH";
    public static final String KILOMETERS_PER_HOUR = "KMPH";
    public static final String METERS_PER_SECOND = "MSEC";
    public static final String INHG = "INMERCURY";
    public static final String MILLIBAR = "MILLIBAR";
    public static final String MMHG = "MMMERCURY";
    public static final String MILES = "MILES";
    public static final String KILOMETERS = "KILOMETERS";
    public static final String INCHES = "INCHES";
    public static final String MILLIMETERS = "MILLIMETERS";
}
