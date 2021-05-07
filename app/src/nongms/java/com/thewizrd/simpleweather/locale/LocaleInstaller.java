package com.thewizrd.simpleweather.locale;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.thewizrd.shared_resources.utils.LocaleUtils;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class LocaleInstaller implements InstallRequest {
    public static final int CONFIRMATION_REQUEST_CODE = 5;

    private LocaleInstaller() {
        // no-op
    }

    public static InstallRequest installLocale(@NonNull Activity activity, final String langCode) {
        LocaleUtils.setLocaleCode(langCode);
        ActivityCompat.recreate(activity);
        return new LocaleInstaller();
    }

    @Override
    public void cancelRequest() {
        // no-op
    }
}
