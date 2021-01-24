package com.thewizrd.shared_resources.helpers;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

public final class ContextUtils {
    public static Context getThemeContextOverride(@NonNull Context context, boolean isLight) {
        Configuration oldConfig = context.getResources().getConfiguration();
        Configuration newConfig = new Configuration(oldConfig);

        newConfig.uiMode = (isLight ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES)
                | (newConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);

        return context.createConfigurationContext(newConfig);
    }
}
