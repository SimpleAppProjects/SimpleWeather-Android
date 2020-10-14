package com.thewizrd.shared_resources.utils;

import android.os.Build;
import android.text.TextUtils;

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

    public static String removeNonDigitChars(CharSequence s) {
        if (TextUtils.isEmpty(s) || isNullOrWhitespace(s.toString()))
            return "";
        else {
            return s.toString().replaceAll("[^\\d.-]", "").trim();
        }
    }

    public static String removeNonDigitChars(String s) {
        if (isNullOrWhitespace(s))
            return "";
        else {
            return s.replaceAll("[^\\d.-]", "").trim();
        }
    }

    public static String removeDigitChars(String s) {
        if (isNullOrWhitespace(s))
            return "";
        else {
            return s.replaceAll("[0-9]", "").trim();
        }
    }

    public static String lineSeparator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return System.lineSeparator();
        } else {
            return System.getProperty("line.separator");
        }
    }

    public static String unescapeUnicode(String s) {
        if (isNullOrWhitespace(s)) {
            return s;
        } else {
            StringBuilder sb = new StringBuilder();

            final int seqEnd = s.length();
            for (int i = 0; i < s.length(); i++) {
                // Uses -2 to ensure there is something after the &#
                char c = s.charAt(i);
                if (s.charAt(i) == '&' && i < seqEnd - 2 && s.charAt(i + 1) == '#') {
                    int start = i + 2;
                    boolean isHex = false;

                    final char firstChar = s.charAt(start);
                    if (firstChar == 'x' || firstChar == 'X') {
                        start++;
                        isHex = true;

                        if (start == seqEnd) {
                            sb.append(s.substring(i));
                            break;
                        }
                    }

                    int end = start;
                    while (end < seqEnd && s.charAt(end) != ';') {
                        end++;
                    }

                    int value;
                    try {
                        if (isHex) {
                            value = Integer.parseInt(s.substring(start, end), 16);
                        } else {
                            value = Integer.parseInt(s.substring(start, end), 10);
                        }
                    } catch (NumberFormatException nfe) {
                        sb.append(s.substring(i));
                        break;
                    }

                    char[] chars = Character.toChars(value);
                    sb.append(chars);

                    i = end;
                } else {
                    sb.append(s.charAt(i));
                }
            }

            return sb.toString();
        }
    }
}
