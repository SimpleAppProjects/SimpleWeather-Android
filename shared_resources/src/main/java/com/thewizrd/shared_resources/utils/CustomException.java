package com.thewizrd.shared_resources.utils;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.thewizrd.shared_resources.SimpleLibrary;

public class CustomException extends Exception {
    @StringRes
    int stringResId = Integer.MIN_VALUE;

    public CustomException(@StringRes int stringResId) {
        this.stringResId = stringResId;
    }

    @Nullable
    @Override
    public String getMessage() {
        if (stringResId != Integer.MIN_VALUE) {
            Context context = SimpleLibrary.getInstance().getAppContext();
            return context.getString(stringResId);
        }

        return super.getMessage();
    }
}
