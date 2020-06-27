package com.thewizrd.shared_resources.utils;

public class NumberUtils {
    public static Integer tryParseInt(String number) {
        Integer result = null;
        try {
            result = Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
        }

        return result;
    }

    public static Float tryParseFloat(String number) {
        Float result = null;
        try {
            result = Float.parseFloat(number);
        } catch (NumberFormatException ignored) {
        }

        return result;
    }
}
