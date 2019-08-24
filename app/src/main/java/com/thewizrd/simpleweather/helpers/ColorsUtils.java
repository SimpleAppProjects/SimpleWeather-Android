package com.thewizrd.simpleweather.helpers;

import androidx.annotation.ColorInt;
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
    private static final float MIN_CONTRAST_RATIO = 2f;

    public static boolean isSuperLight(Palette p) {
        return !isLegibleOnWallpaper(Colors.WHITE, p.getSwatches());
    }

    public static boolean isSuperDark(Palette p) {
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
    private static boolean isLegibleOnWallpaper(@ColorInt int color, List<Palette.Swatch> wallpaperSwatches) {
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
}
