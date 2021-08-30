package com.thewizrd.simpleweather.banner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BannerManagerInterface {
    @Nullable
    BannerManager createBannerManager();

    void initBannerManager();

    void showBanner(@NonNull Banner banner);

    void dismissBanner();

    void unloadBannerManager();
}
