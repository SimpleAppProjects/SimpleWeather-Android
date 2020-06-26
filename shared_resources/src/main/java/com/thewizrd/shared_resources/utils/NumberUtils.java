package com.thewizrd.shared_resources.utils;

public class NumberUtils {
    public static Float tryParse(String number) {
        Float result = null;
        try {
            result = Float.parseFloat(number);
        } catch (NumberFormatException ignored) {
        }

        return result;
    }
}
