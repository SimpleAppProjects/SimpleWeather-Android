package com.thewizrd.shared_resources.utils;

public class LocationUtils {
    public static boolean isUS(String countryCode) {
        if (StringUtils.isNullOrWhitespace(countryCode))
            return false;
        else {
            return countryCode.equalsIgnoreCase("us") || countryCode.equalsIgnoreCase("usa");
        }
    }

    public static boolean isUSorCanada(String countryCode) {
        if (StringUtils.isNullOrWhitespace(countryCode))
            return false;
        else {
            return countryCode.equalsIgnoreCase("US") || countryCode.equalsIgnoreCase("CA");
        }
    }
}
