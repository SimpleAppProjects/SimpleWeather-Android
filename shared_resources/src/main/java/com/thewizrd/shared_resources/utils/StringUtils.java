package com.thewizrd.shared_resources.utils;

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
        if (isNullOrEmpty(s)) {
            return s;
        }
        final char[] buffer = s.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
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
