package com.thewizrd.shared_resources.utils;

import java.util.Locale;

public class LocationUtils {
    public static boolean isUS(String countryCode) {
        if (StringUtils.isNullOrWhitespace(countryCode))
            return false;
        else {
            return countryCode.equalsIgnoreCase("us") || countryCode.equalsIgnoreCase("usa") || countryCode.toLowerCase(Locale.ROOT).contains("united states");
        }
    }

    public static boolean isUSorCanada(String countryCode) {
        if (StringUtils.isNullOrWhitespace(countryCode))
            return false;
        else {
            return isUS(countryCode) || countryCode.equalsIgnoreCase("CA") || countryCode.toLowerCase(Locale.ROOT).contains("canada");
        }
    }
}
