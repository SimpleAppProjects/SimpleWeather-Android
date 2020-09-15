package com.thewizrd.shared_resources;

public final class DateTimeConstants {
    public static final String CLOCK_FORMAT_24HR = "HH:mm";
    public static final String CLOCK_FORMAT_12HR = "h:mm";
    public static final String CLOCK_FORMAT_12HR_AMPM = "h:mm a";
    public static final String TIMEZONE_NAME = "z";
    public static final String ABBREV_DAY_OF_THE_WEEK = "eee";
    public static final String ABBREV_DAYOFWEEK_AND_12HR_AMPM = "eee ha";
    public static final String ABBREV_12HR_AMPM = "ha";

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
     * Constant for date skeleton with abbreviated weekday, month, and numerical day (01).
     */
    public static final String SKELETON_SHORT_DATE_FORMAT = "eeeMMMdd";
    public static final String SKELETON_DAYOFWEEK_AND_24HR = "eeeHm";
    public static final String SKELETON_24HR = "Hm";
}
