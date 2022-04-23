package com.thewizrd.common.helpers;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import com.thewizrd.shared_resources.utils.Colors;

import java.util.List;

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Contains helper fields and methods related to extracting colors from the wallpaper.
 */
public class ColorsUtils {
    private static final float MIN_CONTRAST_TITLE_TEXT = 3.0f;
    private static final float MIN_CONTRAST_BODY_TEXT = 4.5f;
    private static final float MIN_CONTRAST_RATIO = MIN_CONTRAST_BODY_TEXT;

    public static boolean isSuperLight(@NonNull Palette p) {
        return !isLegibleOnWallpaper(Colors.WHITE, p.getSwatches());
    }

    public static boolean isSuperDark(@NonNull Palette p) {
        return !isLegibleOnWallpaper(Colors.BLACK, p.getSwatches());
    }

    public static boolean isSuperLight(@ColorInt int color) {
        return !isLegible(Colors.WHITE, color);
    }

    public static boolean isSuperDark(@ColorInt int color) {
        return !isLegible(Colors.BLACK, color);
    }

    /**
     * Given a color, returns true if that color is legible on
     * the given wallpaper color swatches, else returns false.
     */
    private static boolean isLegibleOnWallpaper(@ColorInt int color, @NonNull List<Palette.Swatch> wallpaperSwatches) {
        int legiblePopulation = 0;
        int illegiblePopulation = 0;
        for (Palette.Swatch swatch : wallpaperSwatches) {
            if (isLegible(color, swatch.getRgb())) {
                legiblePopulation += swatch.getPopulation();
            } else {
                illegiblePopulation += swatch.getPopulation();
            }
        }
        return legiblePopulation > illegiblePopulation;
    }

    /**
     * @return Whether the foreground color is legible on the background color.
     */
    private static boolean isLegible(@ColorInt int foreground, @ColorInt int background) {
        background = ColorUtils.setAlphaComponent(background, 255);
        return ColorUtils.calculateContrast(foreground, background) >= MIN_CONTRAST_RATIO;
    }

    public static Palette.Swatch getPreferredSwatch(Palette p) {
        Palette.Swatch swatch = null;

        if (p != null) {
            if (isSuperDark(p)) {
                swatch = p.getDarkMutedSwatch();
                if (swatch == null)
                    swatch = p.getMutedSwatch();
                if (swatch == null)
                    swatch = p.getLightMutedSwatch();
            } else {
                swatch = p.getVibrantSwatch();
                if (swatch == null)
                    swatch = p.getLightVibrantSwatch();
                if (swatch == null)
                    swatch = p.getDarkVibrantSwatch();
            }
        }

        return swatch;
    }

    @ColorInt
    public static int getBodyTextColor(@ColorInt int color) {
        // First check white, as most colors will be dark
        final int lightBodyAlpha = ColorUtils.calculateMinimumAlpha(
                Color.WHITE, color, MIN_CONTRAST_BODY_TEXT);

        if (lightBodyAlpha != -1) {
            // If we found valid light values, use them and return
            return ColorUtils.setAlphaComponent(Color.WHITE, lightBodyAlpha);
        }

        final int darkBodyAlpha = ColorUtils.calculateMinimumAlpha(
                Color.BLACK, color, MIN_CONTRAST_BODY_TEXT);

        if (darkBodyAlpha != -1) {
            // If we found valid dark values, use them and return
            return ColorUtils.setAlphaComponent(Color.BLACK, darkBodyAlpha);
        }

        // If we reach here then we can not find title and body values which use the same
        // lightness, we need to use mismatched values
        return lightBodyAlpha != -1
                ? ColorUtils.setAlphaComponent(Color.WHITE, lightBodyAlpha)
                : ColorUtils.setAlphaComponent(Color.BLACK, darkBodyAlpha);
    }

    @ColorInt
    public static int getTitleTextColor(@ColorInt int color) {
        // First check white, as most colors will be dark
        final int lightTitleAlpha = ColorUtils.calculateMinimumAlpha(
                Color.WHITE, color, MIN_CONTRAST_TITLE_TEXT);

        if (lightTitleAlpha != -1) {
            // If we found valid light values, use them and return
            return ColorUtils.setAlphaComponent(Color.WHITE, lightTitleAlpha);
        }

        final int darkTitleAlpha = ColorUtils.calculateMinimumAlpha(
                Color.BLACK, color, MIN_CONTRAST_TITLE_TEXT);

        if (darkTitleAlpha != -1) {
            // If we found valid dark values, use them and return
            return ColorUtils.setAlphaComponent(Color.BLACK, darkTitleAlpha);
        }

        // If we reach here then we can not find title and body values which use the same
        // lightness, we need to use mismatched values
        return lightTitleAlpha != -1
                ? ColorUtils.setAlphaComponent(Color.WHITE, lightTitleAlpha)
                : ColorUtils.setAlphaComponent(Color.BLACK, darkTitleAlpha);
    }
}
