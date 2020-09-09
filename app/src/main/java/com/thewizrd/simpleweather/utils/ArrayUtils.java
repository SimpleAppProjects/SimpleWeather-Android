package com.thewizrd.simpleweather.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrayUtils {
    public static boolean contains(int[] array, int item) {
        if (array != null) {
            for (int value : array) {
                if (value == item) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int[] concat(int[]... arrays) {
        if (arrays.length == 0) {
            return new int[0];
        } else {
            int totalLength = 0;

            for (int[] array : arrays) {
                totalLength += array.length;
            }

            int[] resultArray = Arrays.copyOf(arrays[0], totalLength);
            int currentArrLength = arrays[0].length;

            for (int i = 1; i < arrays.length; ++i) {
                int[] currentArray;
                System.arraycopy(currentArray = arrays[i], 0, resultArray, currentArrLength, currentArray.length);
                currentArrLength += currentArray.length;
            }

            return resultArray;
        }
    }

    public static List<Integer> toArrayList(int[] arr) {
        List<Integer> integers = new ArrayList<>(arr.length);

        for (int num : arr) {
            integers.add(num);
        }

        return integers;
    }
}
