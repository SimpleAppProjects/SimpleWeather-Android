package com.thewizrd.shared_resources.controls;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.weatherdata.Beaufort;

public class BeaufortViewModel {
    private DetailItemViewModel beaufort;
    private int progress;
    private int progressMax;
    private @ColorInt
    int progressColor;

    public BeaufortViewModel(Beaufort beaufort) {
        this.beaufort = new DetailItemViewModel(beaufort.getScale(), beaufort.getDescription());
        progressMax = 12;

        switch (beaufort.getScale()) {
            case B0:
                progress = 0;
                progressColor = Colors.DODGERBLUE;
                break;
            case B1:
                progress = 1;
                progressColor = Colors.DEEPSKYBLUE;
                break;
            case B2:
                progress = 2;
                progressColor = Colors.SKYBLUE;
                break;
            case B3:
                progress = 3;
                progressColor = Colors.LIMEGREEN;
                break;
            case B4:
                progress = 4;
                progressColor = Colors.LIME;
                break;
            case B5:
                progress = 5;
                progressColor = Colors.GREENYELLOW;
                break;
            case B6:
                progress = 6;
                progressColor = Colors.LIGHTGREEN;
                break;
            case B7:
                progress = 7;
                progressColor = Colors.LIGHTYELLOW;
                break;
            case B8:
                progress = 8;
                progressColor = Colors.GOLDENROD;
                break;
            case B9:
                progress = 9;
                progressColor = Colors.ORANGE;
                break;
            case B10:
                progress = 10;
                progressColor = Colors.DARKORANGE;
                break;
            case B11:
                progress = 11;
                progressColor = Colors.ORANGERED;
                break;
            case B12:
                progress = 12;
                progressColor = Color.rgb(0xBD, 0x00, 0x35); // FFBD0035
                break;
        }
    }

    public DetailItemViewModel getBeaufort() {
        return beaufort;
    }

    public void setBeaufort(DetailItemViewModel beaufort) {
        this.beaufort = beaufort;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgressMax() {
        return progressMax;
    }

    public void setProgressMax(int progressMax) {
        this.progressMax = progressMax;
    }

    @ColorInt
    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BeaufortViewModel that = (BeaufortViewModel) o;

        if (progress != that.progress) return false;
        if (progressMax != that.progressMax) return false;
        if (progressColor != that.progressColor) return false;
        return beaufort != null ? beaufort.equals(that.beaufort) : that.beaufort == null;
    }

    @Override
    public int hashCode() {
        int result = beaufort != null ? beaufort.hashCode() : 0;
        result = 31 * result + progress;
        result = 31 * result + progressMax;
        result = 31 * result + progressColor;
        return result;
    }
}
