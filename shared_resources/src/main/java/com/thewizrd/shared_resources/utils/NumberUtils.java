package com.thewizrd.shared_resources.utils;

import androidx.annotation.Nullable;

public class NumberUtils {
    @Nullable
    public static Integer tryParseInt(String number) {
        Integer result = null;
        try {
            result = Integer.parseInt(number);
        } catch (NumberFormatException | NullPointerException ignored) {
        }

        return result;
    }

    public static Integer tryParseInt(String number, int defaultValue) {
        Integer result = null;
        try {
            result = Integer.parseInt(number);
        } catch (NumberFormatException | NullPointerException ignored) {
        }

        if (result != null)
            return result;
        else
            return defaultValue;
    }

    @Nullable
    public static Float tryParseFloat(String number) {
        Float result = null;
        try {
            result = Float.parseFloat(number);
        } catch (NumberFormatException | NullPointerException ignored) {
        }

        return result;
    }

    public static Float tryParseFloat(String number, float defaultValue) {
        Float result = null;
        try {
            result = Float.parseFloat(number);
        } catch (NumberFormatException | NullPointerException ignored) {
        }

        if (result != null)
            return result;
        else
            return defaultValue;
    }

    @Nullable
    public static Double tryParseDouble(String number) {
        Double result = null;
        try {
            result = Double.parseDouble(number);
        } catch (NumberFormatException | NullPointerException ignored) {
        }

        return result;
    }

    public static Double tryParseDouble(String number, double defaultValue) {
        Double result = null;
        try {
            result = Double.parseDouble(number);
        } catch (NumberFormatException | NullPointerException ignored) {
        }

        if (result != null)
            return result;
        else
            return defaultValue;
    }

    public static String toString(Float number) {
        if (number == null) {
            return null;
        }
        return number.toString();
    }

    public static String toString(Integer number) {
        if (number == null) {
            return null;
        }
        return number.toString();
    }

    public static Integer getValueOrDefault(Integer number, int defaultValue) {
        if (number == null)
            return defaultValue;

        return number;
    }

    public static Float getValueOrDefault(Float number, float defaultValue) {
        if (number == null)
            return defaultValue;

        return number;
    }

    public static Double getValueOrDefault(Double number, double defaultValue) {
        if (number == null)
            return defaultValue;

        return number;
    }
}
