package com.thewizrd.shared_resources.utils;

import androidx.annotation.NonNull;

public class ExceptionUtils {
    public static <T extends Exception> T copyStackTrace(@NonNull T newException, @NonNull Exception e) {
        newException.setStackTrace(e.getStackTrace());
        return newException;
    }
}
