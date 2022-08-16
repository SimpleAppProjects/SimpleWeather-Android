package com.thewizrd.shared_resources;

public final class DateTimeConstants {
    public static final String CLOCK_FORMAT_24HR = "HH:mm";
    public static final String CLOCK_FORMAT_12HR = "h:mm";
    public static final String CLOCK_FORMAT_12HR_AMPM = "h:mm a";
    public static final String CLOCK_FORMAT_12HR_AMPM_TZ = "h:mm a z";
    public static final String TIMEZONE_NAME = "z";
    public static final String ABBREV_DAY_OF_THE_WEEK = "eee";
    public static final String ABBREV_DAYOFWEEK_AND_12HR_AMPM = "eee h a";
    public static final String ABBREV_DAYOFWEEK_AND_12HR_MIN_AMPM = "eee h:mm a";
    public static final String ABBREV_12HR_AMPM = "h a";
    public static final String ABBREV_12HR_AMPM_SHORT = "ha";
    public static final String DAY_OF_THE_WEEK = "eeee";

    /**
     * Constant for date skeleton with full weekday, month, and numerical day (00).
     */
    public static final String SKELETON_LONG_DATE_FORMAT = "eeeeMMMMdd";
    /**
     * Constant for date skeleton with full weekday, abbreviated month, and numerical day (01).
     */
    public static final String SKELETON_WDAY_ABBR_MONTH_FORMAT = "eeeeMMMdd";
    /**
     * Constant for date skeleton with abbreviated weekday, full month, and numerical day (01).
     */
    public static final String SKELETON_ABBR_WDAY_MONTH_FORMAT = "eeeMMMMdd";
    /**
     * Constant for date skeleton with full weekday, and numerical day (00).
     */
    public static final String SKELETON_WDAY_DATE_FORMAT = "eeeedd";
    /**
     * Constant for date skeleton with abbreviated weekday, month, and numerical day (01).
     */
    public static final String SKELETON_SHORT_DATE_FORMAT = "eeeMMMdd";
    /**
     * Constant for date skeleton with abbreviated weekday, and hour and minute in 24-hour presentation.
     */
    public static final String SKELETON_DAYOFWEEK_AND_24HR = "eeeHm";
    /**
     * Constant for date skeleton with hour and minute in 24-hour presentation.
     */
    public static final String SKELETON_24HR = "Hm";
}
