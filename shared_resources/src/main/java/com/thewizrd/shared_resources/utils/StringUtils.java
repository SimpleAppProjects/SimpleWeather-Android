package com.thewizrd.shared_resources.utils;

import org.apache.commons.lang3.text.WordUtils;

public class StringUtils {
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isNullOrWhitespace(String s) {
        return s == null || isWhitespace(s);
    }

    private static boolean isWhitespace(String s) {
        if (s == null)
            return true;

        for (int idx = 0; idx < s.length(); ++idx) {
            if (!Character.isWhitespace(s.toCharArray()[idx]))
                return false;
        }

        return true;
    }

    public static String toUpperCase(String s) {
        return WordUtils.capitalize(s);
    }

    public static String toPascalCase(String s) {
        String[] strArray = s.split("\\.", 0);
        StringBuilder sb = new StringBuilder();

        for (String str : strArray) {
            if (str.length() == 0)
                continue;

            sb.append(str.trim().substring(0, 1).toUpperCase())
                    .append(str.trim().substring(1).toLowerCase())
                    .append(". ");
        }

        return sb.toString().trim();
    }
}
